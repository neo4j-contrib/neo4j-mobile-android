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
package com.noser.neo4j.android.dbinspector.implementation;

import java.util.List;

import org.neo4j.android.client.GraphDatabase;
import org.neo4j.android.client.Neo4jService;
import org.neo4j.android.client.Neo4jServiceException;
import org.neo4j.android.common.INeo4jService;

import roboguice.util.Ln;
import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.noser.neo4j.android.dbinspector.base.DBInspectorException;
import com.noser.neo4j.android.dbinspector.interfaces.IDBManager;

/**
 * needs to be a @Singleton. @ContextSingleton would lead to a singleton per
 * activity, which is not that what we want.
 */
@Singleton
public class DBManager implements IDBManager {

    private Neo4jService neo4jService;

    private GraphDatabase database;

    private String databaseName;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {

            neo4jService = new Neo4jService(INeo4jService.Stub.asInterface(binder));
        }
    };

    @Inject
    public DBManager(Application application) {
        Neo4jService.bindService(application, serviceConnection);
        Ln.i("Neo4j service bound.");
    }

    @Override
    public boolean isNeo4jServiceAvailable() {
        return (neo4jService != null);
    }

    @Override
    public void openOrCreateNeo4jDatabase(String databaseName) throws DBInspectorException {
        try {
            doShutdownNeo4jDatabase(databaseName);
            this.database = neo4jService.openOrCreateDatabase(databaseName);
            this.databaseName = databaseName;
        } catch (Neo4jServiceException e) {
            throw new DBInspectorException(e);
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public void shutdownNeo4jDatabase(String databaseName) throws DBInspectorException {
        try {
            doShutdownNeo4jDatabase(databaseName);
        } catch (Neo4jServiceException e) {
            throw new DBInspectorException(e);
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public boolean neo4jDatabaseExists(String databaseName) throws DBInspectorException {
        try {
            return neo4jService.databaseExists(databaseName);
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public boolean isNeo4jDatabaseOpen(String databaseName) throws DBInspectorException {
        try {
            return neo4jService.isDatabaseOpen(databaseName);
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public void deleteNeo4jDatabase(String databaseName) throws DBInspectorException {
        try {
            neo4jService.deleteDatabase(databaseName);
        } catch (Neo4jServiceException e) {
            throw new DBInspectorException(e);
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public void exportNeo4jDatabase(String databaseName) throws DBInspectorException {
        try {
            neo4jService.exportDatabase(databaseName);
        } catch (Neo4jServiceException e) {
            throw new DBInspectorException(e);
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public List<String> listAvailableNeo4jDatabases() throws DBInspectorException {
        try {
            return neo4jService.listAvailableDatabases();
        } catch (RemoteException e) {
            throw new DBInspectorException(e);
        }
    }

    @Override
    public GraphDatabase getCurrentNeo4jDatabase() {
        assertDatabaseState();
        return database;
    }

    @Override
    public boolean isCurrentNeo4jDatabaseOpen() {
        assertDatabaseState();
        return (database != null);
    }

    @Override
    public String getCurrentNeo4jDatabaseName() {
        assertDatabaseState();
        return databaseName;
    }

    private void doShutdownNeo4jDatabase(String databaseName) throws RemoteException, Neo4jServiceException {
        if ((this.databaseName != null) && this.databaseName.equals(databaseName)) {
            this.database = null;
            this.databaseName = null;
        }
        neo4jService.shutdownDatabase(databaseName);
    }

    private void assertDatabaseState() {
        boolean databaseIsOpen = (database != null) && (databaseName != null);
        boolean databaseIsClosed = (database == null) && (databaseName == null);
        if (databaseIsOpen == databaseIsClosed) {
            throw new IllegalStateException("database state not consistent.");
        }
    }
}
