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
import org.neo4j.android.common.INodeIterator;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;
import org.neo4j.android.common.ParcelableTraversalDescription;

import android.os.RemoteException;

/**
 * Client-side helper for working with a remote Neo4j graph database. Main duty
 * is to free the user from the duty of handling the ParcelableError objects and
 * providing exceptions instead. Wow, I really wish I had a generator framework
 * here.
 */
public class GraphDatabase {

    private IGraphDatabase mProxy;

    public GraphDatabase(IGraphDatabase proxy) {
        mProxy = proxy;
    }

    public ParcelableNode getReferenceNode() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        ParcelableNode result = mProxy.getReferenceNode(err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public ParcelableNode getNodeById(long id) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        ParcelableNode result = mProxy.getNodeById(id, err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public NodeIterator getAllNodes() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        INodeIterator it = mProxy.getAllNodes(err);
        Util.throwServiceExceptionIfError(err);
        return new NodeIterator(it);
    }

    public long createNode(ParcelableNode node) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        long result = mProxy.createNode(node, err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public void updateNode(ParcelableNode node) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.updateNode(node, err);
        Util.throwServiceExceptionIfError(err);
    }

    public void deleteNode(long id) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.deleteNode(id, err);
        Util.throwServiceExceptionIfError(err);
    }

    public List<String> getRelationshipTypes() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        List<String> result = mProxy.getRelationshipTypes(err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public ParcelableRelationship getRelationshipById(long id) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        ParcelableRelationship result = mProxy.getRelationshipById(id, err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public long createRelationship(ParcelableRelationship relationship) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        long result = mProxy.createRelationship(relationship, err);
        Util.throwServiceExceptionIfError(err);
        return result;
    }

    public void updateRelationship(ParcelableRelationship relationship) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.updateRelationship(relationship, err);
        Util.throwServiceExceptionIfError(err);
    }

    public void deleteRelationship(long id) throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.deleteRelationship(id, err);
        Util.throwServiceExceptionIfError(err);
    }

    public void beginTx() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.beginTx(err);
        Util.throwServiceExceptionIfError(err);
    }

    public void txSuccess() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.txSuccess(err);
        Util.throwServiceExceptionIfError(err);
    }

    public void txFailure() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.txFailure(err);
        Util.throwServiceExceptionIfError(err);
    }

    public void txFinish() throws RemoteException, Neo4jServiceException {

        ParcelableError err = new ParcelableError();
        mProxy.txFinish(err);
        Util.throwServiceExceptionIfError(err);
    }

    public NodeIterator traverse(ParcelableTraversalDescription desc, long startNodeId) throws RemoteException,
            Neo4jServiceException {
        ParcelableError err = new ParcelableError();
        INodeIterator result = mProxy.traverse(desc, startNodeId, err);
        Util.throwServiceExceptionIfError(err);
        return new NodeIterator(result);
    }
}
