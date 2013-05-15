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

import java.util.Iterator;

import org.neo4j.android.common.INodeIterator;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.service.util.ParcelableFactory;
import org.neo4j.graphdb.Node;

import android.os.RemoteException;
import android.util.Log;

/**
 * Wrap a node iterator in a remotable object.
 */
public class NodeIteratorWrapper extends INodeIterator.Stub {

    private static final String TAG = NodeIteratorWrapper.class.getSimpleName();

    private Iterator<Node> iterator;

    public NodeIteratorWrapper(Iterator<Node> iterator) {
        this.iterator = iterator;
    }

    @Override
    public ParcelableNode next(ParcelableError error) throws RemoteException {

        try {
            Node node = iterator.next();
            return ParcelableFactory.makeParcelableNode(node);
        } catch (Exception e) {
            Log.e(TAG, "Error while iterating over nodes", e);
            error.setError(Errors.NODE_ITERATOR, e.getMessage());
            return null;
        }

    }

    @Override
    public boolean hasNext(ParcelableError error) throws RemoteException {

        try {
            return iterator.hasNext();
        } catch (Exception e) {
            Log.e(TAG, "Error while iterating over nodes", e);
            error.setError(Errors.NODE_ITERATOR, e.getMessage());
            return false;
        }
    }

}
