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

import org.neo4j.android.common.IRelationshipIterator;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.android.common.ParcelableRelationship;
import org.neo4j.android.service.util.ParcelableFactory;
import org.neo4j.graphdb.Relationship;

import android.os.RemoteException;
import android.util.Log;

/**
 * Wrap a relationship iterator in a remotable object.
 */
public class RelationshipIteratorWrapper extends IRelationshipIterator.Stub {

    private static final String TAG = RelationshipIteratorWrapper.class.getSimpleName();

    private Iterator<Relationship> iterator;

    public RelationshipIteratorWrapper(Iterator<Relationship> iterator) {
        this.iterator = iterator;
    }

    @Override
    public ParcelableRelationship next(ParcelableError error) throws RemoteException {

        try {
            Relationship rel = iterator.next();
            return ParcelableFactory.makeParcelableRelationship(rel);
        } catch (Exception e) {
            Log.e(TAG, "Error while iterating over relationships", e);
            error.setError(Errors.RELATIONSHIP_ITERATOR, e.getMessage());
            return null;
        }

    }

    @Override
    public boolean hasNext(ParcelableError error) throws RemoteException {

        try {
            return iterator.hasNext();
        } catch (Exception e) {

            Log.e(TAG, "Error while iterating over relationships", e);
            error.setError(Errors.RELATIONSHIP_ITERATOR, e.getMessage());
            return false;
        }
    }

}
