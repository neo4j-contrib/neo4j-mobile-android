/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.android.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.neo4j.android.Version;
import org.neo4j.android.common.IGraphDatabase;
import org.neo4j.android.common.INeo4jService;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Service wrapper for Neo4j.
 */
public class Neo4jServiceImpl extends Service {

    private static final String TAG = Neo4jServiceImpl.class.getSimpleName();

    private static final String EXTENSION = ".zip";

    private static final String PROPERTY_PRELOAD_ALL = "preloadAll";

    private static final int BLOCKSIZE = 8192;

    // Transaction reaper interval
    private static final long TRX_REAPER_INTERVAL_MILLIS = 1000;

    private static final long TRX_MAX_LIFETIME_MILLIS = 120 * 1000;

    /**
     * The databases that are currently loaded. Weak referenced, so they may be
     * garbage collected if no clients use them (we can't detect a single client
     * disconnecting)
     */
    private Map<String, WeakReference<EmbeddedGraphDatabase>> mDatabases = new HashMap<String, WeakReference<EmbeddedGraphDatabase>>();

    // this lock protects the above data structure
    private ReentrantLock mDatabaseLock = new ReentrantLock(true);

    /**
     * The core of our transaction association mechanism. It will associate
     * IBinder instances (DbWrapper) with transactions, so that multiple bound
     * clients may perform independent transactions.
     */
    private TrxManager mTrxManager;

    /**
     * Background cleanup of zombie transactions. We don't get notified if a
     * client unbinds (weirdly, only wenn all unbind), so we don't really know
     * if the client has died and trx is going to stay here forever.
     */
    private Reaper mReaper;

    private IBinder mBinder = new INeo4jService.Stub() {

        @Override
        public IGraphDatabase openOrCreateDatabase(String name, ParcelableError err) throws RemoteException {
            if (name.isEmpty()) {
                String message = "given database name is empty.";
                err.setError(Errors.MISSING_DATABASE_NAME, message);
                Log.e(TAG, message);
                return null;
            }
            try {
                return doOpenOrCreateDatabase(name);
            } catch (Exception e) {
                err.setError(Errors.OPEN_CREATE_DATABASE, e.getMessage());
                Log.e(TAG, "Exception while opening/creating database '" + name + "'", e);
                return null;
            }
        }

        @Override
        public String getNeo4jVersion() throws RemoteException {
            return Version.getVersion();
        }

        @Override
        public List<String> listAvailableDatabases() throws RemoteException {
            return doListAvailableDatabases();
        }

        @Override
        public boolean deleteDatabase(String name, ParcelableError err) throws RemoteException {
            if (name.isEmpty()) {
                String message = "given database name is empty.";
                err.setError(Errors.MISSING_DATABASE_NAME, message);
                Log.e(TAG, message);
                return false;
            }

            boolean doDelete = false;

            // check if DB is loaded, if it is, terminate it
            mDatabaseLock.lock();
            try {

                WeakReference<EmbeddedGraphDatabase> dbRef = mDatabases.get(name);
                if (dbRef == null) {
                    doDelete = true;
                } else {
                    if (dbRef.get() != null) {
                        // DB is still in memory and strongly referenced?
                        Log.w(TAG, "Delete requested for database that is still being referenced (HINT: misbehaving clients?)");
                    } else {
                        // object has been cleared, remove the mapping and
                        // delete the DB
                        mDatabases.remove(name);
                        doDelete = true;
                    }
                }

                if (doDelete) {
                    deleteDatabaseDir(name); // I/O in a lock, not good, but
                                             // it's not critical here
                }
                return doDelete;
            } finally {
                mDatabaseLock.unlock();
            }
        }

        @Override
        public boolean exportDatabase(String name, ParcelableError err) throws RemoteException {
            if (name.isEmpty()) {
                String message = "given database name is empty.";
                err.setError(Errors.MISSING_DATABASE_NAME, message);
                Log.e(TAG, message);
                return false;
            }

            boolean doExport = false;

            // check if DB is loaded, if it is, terminate it
            mDatabaseLock.lock();
            try {

                WeakReference<EmbeddedGraphDatabase> dbRef = mDatabases.get(name);
                if (dbRef == null) {
                    doExport = true;
                } else {
                    if (dbRef.get() != null) {
                        // DB is still in memory and strongly referenced?
                        Log.w(TAG, "Export requested for database that is still being referenced (HINT: misbehaving clients?)");
                    } else {
                        // object has been cleared, remove the mapping and
                        // delete the DB
                        mDatabases.remove(name);
                        doExport = true;
                    }
                }

                if (doExport) {
                    exportDatabaseDir(name); // I/O in a lock, not good, but
                                             // it's not critical here
                }
                return doExport;
            } finally {
                mDatabaseLock.unlock();
            }
        }

        @Override
        public void shutdownDatabase(String name, ParcelableError err) throws RemoteException {
            if (name.isEmpty()) {
                String message = "given database name is empty.";
                err.setError(Errors.MISSING_DATABASE_NAME, message);
                Log.e(TAG, message);
                return;
            }

            mDatabaseLock.lock();
            try {
                WeakReference<EmbeddedGraphDatabase> dbRef = mDatabases.get(name);
                if (dbRef != null) {
                    if (dbRef.get() != null) {
                        EmbeddedGraphDatabase db = dbRef.get();
                        db.shutdown();
                    }
                    mDatabases.remove(name);
                } else {
                    Log.w(TAG, "no database found with name '" + name + "'. available databases '" + mDatabases.keySet() + "'.");
                }
            } finally {
                mDatabaseLock.unlock();
            }
        }

        @Override
        public boolean databaseExists(String name) throws RemoteException {
            return databaseDirExists(name);
        }

        public boolean isDatabaseOpen(String name) throws RemoteException {
            if (name == null) {
                return false;
            } else {
                return (mDatabases.containsKey(name) && (mDatabases.get(name).get() != null));
            }
        }
    };

    /**
     * Check if the named DB exists. Under the hood we just check if the
     * database directory exists and assume that it is non empty and filled with
     * valid data.
     * 
     * @param name
     */
    private boolean databaseDirExists(String name) {

        // input checks, no exceptions!
        if (name == null) {
            return false;
        }

        File dbDir = new File(getApplicationContext().getFilesDir(), name);
        return dbDir.exists();
    }

    private void deleteDatabaseDir(String name) {

        File dbDir = new File(getApplicationContext().getFilesDir(), name);
        if (!dbDir.exists()) {
            return;
        }

        // delete all files in directory. Let's hope Neo4j doesn't introduce
        // subdirectories...
        for (File file : dbDir.listFiles()) {
            boolean deleted = file.delete();
            Log.i(TAG, "Deleted file " + file + ": " + deleted);
        }

        boolean dirDeleted = dbDir.delete();
        Log.i(TAG, "Deleting DB directory at " + dbDir + ": " + dirDeleted);
        return;
    }

    private void exportDatabaseDir(String name) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalDir = Environment.getExternalStorageDirectory();

            File dbDir = new File(getApplicationContext().getFilesDir(), name);
            if (!dbDir.exists()) {
                Log.i(TAG, "database with name '" + name + "' does not exist. doing nothing.");
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String zipName = externalDir.getAbsolutePath() + "/" + name + "_" + dateFormat.format(new Date()) + EXTENSION;

            try {
                ZipOutputStream zippingOutputStream = new ZipOutputStream(new FileOutputStream(zipName, false));
                for (File file : dbDir.listFiles()) {
                    zippingOutputStream.putNextEntry(new ZipEntry(file.getName()));
                    zippingOutputStream.setLevel(ZipOutputStream.DEFLATED);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
                    byte[] data = new byte[BLOCKSIZE];
                    int byteCount;
                    while ((byteCount = bufferedInputStream.read(data, 0, BLOCKSIZE)) > -1) {
                        zippingOutputStream.write(data, 0, byteCount);
                    }
                    bufferedInputStream.close();
                    zippingOutputStream.closeEntry();
                }
                zippingOutputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "zipping of '" + dbDir.getAbsolutePath() + "' failed.", e);
            }

            Log.i(TAG, "created zip file '" + zipName + "'");
        } else {
            Log.i(TAG, "external storage not mounted. doing nothing.");
        }
    }

    private Properties loadNeo4jProperties() {
        Properties properties = new Properties();
        try {
            InputStream inputStream = getApplicationContext().getResources().openRawResource(R.raw.neo4j);
            properties.load(inputStream);
            inputStream.close();
        } catch (NotFoundException e) {
            Log.e(TAG, "unable to load neo4j configuration", e);
        } catch (IOException e) {
            Log.e(TAG, "unable to load neo4j configuration", e);
        }
        return properties;
    }

    private List<String> doListAvailableDatabases() {
        List<String> databaseDirs = new ArrayList<String>();
        for (File file : getApplicationContext().getFilesDir().listFiles()) {
            databaseDirs.add(file.getName());
        }
        return databaseDirs;
    }

    private IGraphDatabase doOpenOrCreateDatabase(String name) {
        mDatabaseLock.lock();
        try {
            // TODO: [eRiC] what happens if another application tries to
            // open the same database?

            WeakReference<EmbeddedGraphDatabase> dbRef = mDatabases.get(name);

            // check if the DB is already loaded, or if has been
            // previously loaded but unloaded by the GC
            EmbeddedGraphDatabase db = null;
            if (dbRef != null) {
                db = dbRef.get();
                if (db == null) {
                    Log.d(TAG, "database '" + name + "' was previously loaded, and garbage collected.");
                    // delete the mapping, we will reload and remap it
                    // later
                    mDatabases.remove(name);
                }
            }

            // if we haven't found a DB yet, it wasn't loaded, or has
            // been unloaded by the GC
            if (db == null) {
                File dbDir = new File(getApplicationContext().getFilesDir(), name);
                db = new EmbeddedGraphDatabase(getApplicationContext(), dbDir.getAbsolutePath());
                mDatabases.put(name, new WeakReference<EmbeddedGraphDatabase>(db));

                Log.d(TAG, "database '" + name + "' loaded.");
            }

            DbWrapper dbWrapper = new DbWrapper(db, mTrxManager, getApplicationContext());

            Log.i(TAG, "Returning database '" + name + "' (binder hashcode '" + dbWrapper.asBinder().hashCode() + "'): " + db);
            return dbWrapper;
        } finally {
            mDatabaseLock.unlock();
        }
    }

    private void preloadAllDatabases() {
        List<String> databaseNames = doListAvailableDatabases();
        try {
            for (String databaseName : databaseNames) {
                doOpenOrCreateDatabase(databaseName);
                Log.i(TAG, "preloaded database '" + databaseName + "'");
            }
        } catch (Exception e) {
            Log.e(TAG, "preloading databases aborted.", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Note: Android caches the object returned here, unless there is a
        // variation in the intent.
        Log.i(TAG, "Bound to some client. intent: " + intent);
        return mBinder;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        // supporting infrastructure
        mTrxManager = new TrxManager();
        mReaper = new Reaper();
        mReaper.start();

        Properties properties = loadNeo4jProperties();
        if (properties.containsKey(PROPERTY_PRELOAD_ALL) && Boolean.parseBoolean(properties.getProperty(PROPERTY_PRELOAD_ALL))) {
            preloadAllDatabases();
        }

        Log.i(TAG, "Service created, neo4j version: " + Version.getVersion());
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        mReaper.alive = false;
        try {
            mReaper.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while join reaper", e);
        }

        Log.i(TAG, "Service destroyed");
    }

    private class Reaper extends Thread {

        public Reaper() {
            super("TrxReaper");
            alive = true;
        }

        /* package */volatile boolean alive;

        @Override
        public void run() {

            try {

                while (alive) {
                    Thread.sleep(TRX_REAPER_INTERVAL_MILLIS);

                    mDatabaseLock.lock();
                    try {
                        mTrxManager.rollbackZombies(TRX_MAX_LIFETIME_MILLIS);
                    } finally {
                        mDatabaseLock.unlock();
                    }
                }
            } catch (InterruptedException ex) {
                Log.i(TAG, "Reaper was interrupted, probably because service died.");
            }

            Log.i(TAG, "Reaper terminated");
        }
    }
}
