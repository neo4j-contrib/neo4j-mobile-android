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
package org.neo4j.android.client;

import java.util.List;

import org.neo4j.android.common.IGraphDatabase;
import org.neo4j.android.common.INeo4jService;
import org.neo4j.android.common.ParcelableError;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;

/**
 * Client-side helper for working with a remote Neo4j Service.
 */
public class Neo4jService {

    public static final String ACTION_SERVICE = "org.neo4j.android.service.Neo4jService";

    private INeo4jService mProxy;

    public Neo4jService(INeo4jService proxy) {
        mProxy = proxy;
    }

    public String getNeo4jVersion() throws RemoteException {

        String result = mProxy.getNeo4jVersion();
        return result;
    }

    public GraphDatabase openOrCreateDatabase(String name) throws RemoteException, Neo4jServiceException {
        ParcelableError err = new ParcelableError();
        IGraphDatabase proxy = mProxy.openOrCreateDatabase(name, err);
        Util.throwServiceExceptionIfError(err);
        return new GraphDatabase(proxy);
    }

    public boolean deleteDatabase(String name) throws RemoteException, Neo4jServiceException {
        ParcelableError err = new ParcelableError();
        boolean result = mProxy.deleteDatabase(name, err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public boolean exportDatabase(String name) throws RemoteException, Neo4jServiceException {
        ParcelableError err = new ParcelableError();
        boolean result = mProxy.exportDatabase(name, err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public List<String> listAvailableDatabases() throws RemoteException {
        return mProxy.listAvailableDatabases();
    }

    public boolean databaseExists(String name) throws RemoteException {
        boolean result = mProxy.databaseExists(name);
        return result;
    }

    public boolean isDatabaseOpen(String name) throws RemoteException {
        boolean result = mProxy.isDatabaseOpen(name);
        return result;
    }

    public void shutdownDatabase(String name) throws RemoteException, Neo4jServiceException {
        ParcelableError err = new ParcelableError();
        mProxy.shutdownDatabase(name, err);
        Util.throwServiceExceptionIfError(err);
    }

    /**
     * Convenience method to bind to the service.
     * 
     * @param context
     * @param conn
     * @return true if service bound.
     */
    public static boolean bindService(Context context, ServiceConnection conn) {
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(ACTION_SERVICE);
        return context.bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);
    }
}
