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

import org.neo4j.android.common.INodeIterator;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.android.common.ParcelableNode;

import android.os.RemoteException;

public class NodeIterator {

    private INodeIterator mProxy;

    public NodeIterator(INodeIterator proxy) {
        mProxy = proxy;
    }

    public boolean hasNext() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        boolean result = mProxy.hasNext(err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public ParcelableNode next() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        ParcelableNode result = mProxy.next(err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }
}
