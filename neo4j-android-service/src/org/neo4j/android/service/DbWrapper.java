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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.android.common.IGraphDatabase;
import org.neo4j.android.common.INodeIterator;
import org.neo4j.android.common.IRelationshipIterator;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.android.common.ParcelableIndexValue;
import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;
import org.neo4j.android.common.ParcelableTraversalDescription;
import org.neo4j.android.service.util.ParcelableFactory;
import org.neo4j.android.service.util.SimpleRelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.javax.transaction.InvalidTransactionException;
import org.neo4j.javax.transaction.SystemException;
import org.neo4j.javax.transaction.TransactionManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.TopLevelTransaction;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

/**
 * An IPC-wrapper around a Neo4j graph database.
 */
public class DbWrapper extends IGraphDatabase.Stub {

    private static final String TAG = DbWrapper.class.getSimpleName();

    /**
     * Our transaction Binder-to-Transaction mapper
     */
    private TrxManager mTrxManager;

    /**
     * The graph database we are exposing via IPC
     */
    private EmbeddedGraphDatabase mDb;

    /**
     * The service context, required for permission checks.
     */
    private Context mContext;

    public DbWrapper(EmbeddedGraphDatabase db, TrxManager mgr, Context context) {
        mDb = db;
        mTrxManager = mgr;
        mContext = context;
    }

    @Override
    public INodeIterator getAllNodes(ParcelableError err) throws RemoteException {

        try {
            resumeTrxIfExists();
            try {
                return new NodeIteratorWrapper(mDb.getAllNodes().iterator());
            } finally {
                suspendCurrentTrx("getAllNodes");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing nodes", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> getRelationshipTypes(ParcelableError err) throws RemoteException {

        try {
            resumeTrxIfExists();
            try {
                ArrayList<String> names = new ArrayList<String>();
                for (RelationshipType type : mDb.getRelationshipTypes()) {
                    names.add(type.name());
                }
                return names;
            } finally {
                suspendCurrentTrx("getRelationshipTypes");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing relationship types", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    @Override
    public ParcelableNode getReferenceNode(ParcelableError err) throws RemoteException {

        try {
            resumeTrxIfExists();
            try {
                Node refNode = mDb.getReferenceNode();
                return ParcelableFactory.makeParcelableNode(refNode);
            } finally {
                suspendCurrentTrx("getReferenceNode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing reference node node", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    @Override
    public ParcelableNode getNodeById(long id, ParcelableError err) throws RemoteException {

        try {
            resumeTrxIfExists();
            try {
                Node node = mDb.getNodeById(id);
                return ParcelableFactory.makeParcelableNode(node);
            } finally {
                suspendCurrentTrx("getNodeById");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing node", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    @Override
    public ParcelableRelationship getRelationshipById(long id, ParcelableError err) throws RemoteException {

        try {
            resumeTrxIfExists();
            try {
                Relationship rel = mDb.getRelationshipById(id);
                return ParcelableFactory.makeParcelableRelationship(rel);
            } finally {
                suspendCurrentTrx("getRelationshipById");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing relationship", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    @Override
    public long createNode(ParcelableNode node, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {

                Node newNode = mDb.createNode();
                for (String key : node.getPropertyKeys()) {
                    newNode.setProperty(key, node.getProperty(key));
                }
                return newNode.getId();
            } finally {
                suspendCurrentTrx("createNode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating node", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return -1; // we need to return something, caller checks error
                       // object
        }
    }

    @Override
    public void deleteNode(long id, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Node node = mDb.getNodeById(id); // will throw NotFoundException
                                                 // if node does not exist
                node.delete();
            } finally {
                suspendCurrentTrx("deleteNode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting node", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void updateNode(ParcelableNode node, ParcelableError err) throws RemoteException {

        try {

            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Node target = mDb.getNodeById(node.getId());
                if (target != null) {
                    // remove existing properties
                    for (String key : target.getPropertyKeys()) {
                        target.removeProperty(key);
                    }

                    // set new properties
                    for (String key : node.getPropertyKeys()) {
                        target.setProperty(key, node.getProperty(key));
                    }
                } else {
                    String message = "node not found. node '" + node + "'";
                    Log.e(TAG, message);
                    err.setError(Errors.TRANSACTION, message);
                }
            } finally {
                suspendCurrentTrx("updateNode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update node", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public long createRelationship(ParcelableRelationship rel, ParcelableError err) throws RemoteException {

        try {

            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Node startNode = mDb.getNodeById(rel.getStartNodeId());
                Node endNode = mDb.getNodeById(rel.getEndNodeId());
                Relationship newRel = startNode.createRelationshipTo(endNode, new SimpleRelationshipType(rel.getName()));
                for (String key : rel.getPropertyKeys()) {
                    newRel.setProperty(key, rel.getProperty(key));
                }
                return newRel.getId();
            } finally {
                suspendCurrentTrx("createRelationship");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating relationship", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return -1; // we need to return something, caller checks error
                       // object
        }
    }

    @Override
    public void updateRelationship(ParcelableRelationship rel, ParcelableError err) throws RemoteException {

        try {

            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship target = mDb.getRelationshipById(rel.getId());

                // remove existing properties
                for (String key : target.getPropertyKeys()) {
                    target.removeProperty(key);
                }

                // set new properties
                for (String key : rel.getPropertyKeys()) {
                    target.setProperty(key, rel.getProperty(key));
                }
            } finally {
                suspendCurrentTrx("updateRelationship");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating relationship", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void deleteRelationship(long id, ParcelableError err) throws RemoteException {

        try {

            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship rel = mDb.getRelationshipById(id);
                rel.delete();
            } finally {
                suspendCurrentTrx("deleteRelationship");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting relationship", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public INodeIterator traverse(ParcelableTraversalDescription desc, long startNodeId, ParcelableError err)
            throws RemoteException {
        try {
            resumeTrxIfExists();

            try {
                Node startNode = mDb.getNodeById(startNodeId);
                if (startNode == null) {
                    throw new IllegalArgumentException("Illegal start node for traversal");
                }

                TraversalDescription traversalDesc = Traversal.description();

                // map order
                switch (desc.getOrder()) {
                    case BREADTH_FIRST:
                        traversalDesc = traversalDesc.breadthFirst();
                        break;
                    case DEPTH_FIRST:
                        traversalDesc = traversalDesc.depthFirst();
                        break;
                }

                // map uniqueness
                switch (desc.getUniqueness()) {
                    case NONE:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.NONE);
                        break;
                    case NODE_GLOBAL:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.NODE_GLOBAL);
                        break;
                    case NODE_RECENT:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.NODE_RECENT);
                        break;
                    case NODE_PATH:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.NODE_PATH);
                        break;
                    case RELATIONSHIP_GLOBAL:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
                        break;
                    case RELATIONSHIP_PATH:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.RELATIONSHIP_PATH);
                        break;
                    case RELATIONSHIP_RECENT:
                        traversalDesc = traversalDesc.uniqueness(Uniqueness.RELATIONSHIP_RECENT);
                        break;
                }

                // map relationships
                for (String name : desc.getRelationships().keySet()) {
                    RelationshipType type = new SimpleRelationshipType(name);
                    Direction direction = Direction.valueOf(desc.getRelationships().get(name).name());
                    traversalDesc = traversalDesc.relationships(type, direction);
                }

                // TODO: support paths
                Iterator<Node> nodeIterator = traversalDesc.traverse(startNode).nodes().iterator();

                return new NodeIteratorWrapper(nodeIterator);
            } finally {
                suspendCurrentTrx("traverse");
            }
        } catch (Exception e) {
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }

    }

    // -------------------------------------------------------------------------
    // Transactions
    // -------------------------------------------------------------------------

    @Override
    public void beginTx(ParcelableError err) throws RemoteException {
        Log.i(TAG, "BEGIN TRANSACTION '" + this.asBinder().hashCode() + "'");
        try {
            // cannot start transactions without write permission
            checkCallerHasWritePermission();

            if (mTrxManager.hasAssociatedTrx(this.asBinder())) {
                throw new IllegalStateException("A transaction is already associated with this Binder");
            }

            // acquire and associate transaction
            try {
                Transaction trx = mDb.beginTx();
                TransactionManager trxMgr = mDb.getConfig().getTxModule().getTxManager();
                mTrxManager.associateTrx(this.asBinder(), trx, trxMgr);
            } finally {
                suspendCurrentTrx("beginTx");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark trx as failure", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void txFailure(ParcelableError err) throws RemoteException {
        try {
            Transaction trx = resumeTrx();
            try {
                trx.failure();
            } finally {
                suspendCurrentTrx("txFailure");
                Log.i(TAG, "END TRANSACTION - FAIL '" + this.asBinder().hashCode() + "'");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark trx as failure, will disassociate trx", e);
            mTrxManager.disassociateTrx(this.asBinder(), "txFailure");
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void txSuccess(ParcelableError err) throws RemoteException {
        try {
            Transaction trx = resumeTrx();
            try {
                trx.success();
            } finally {
                suspendCurrentTrx("txSuccess");
                Log.i(TAG, "END TRANSACTION - SUCCESS '" + this.asBinder().hashCode() + "'");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark trx as success, will disassociate trx", e);
            mTrxManager.disassociateTrx(this.asBinder(), "txSuccess");
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void txFinish(ParcelableError err) throws RemoteException {
        try {
            Transaction trx = resumeTrx();
            trx.finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark trx as finished, will disassociate trx", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        } finally {
            mTrxManager.disassociateTrx(this.asBinder(), "txFinish");
            Log.i(TAG, "END TRANSACTION - FINISH '" + this.asBinder().hashCode() + "'");
        }
    }

    // -------------------------------------------------------------------------
    // Node Indexing support
    // -------------------------------------------------------------------------

    @Override
    public void createNodeIndex(String name, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                IndexManager index = mDb.index();
                index.forNodes(name); // this will create the index
            } finally {
                suspendCurrentTrx("createNodeIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create/access node index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public boolean nodeIndexExists(String name, ParcelableError err) throws RemoteException {

        try {
            resumeTrxIfExists();
            try {
                IndexManager index = mDb.index();
                return index.existsForNodes(name);
            } finally {
                suspendCurrentTrx("nodeIndexExists");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to determine if node index '" + name + "' exists", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return false; // dummy return value
        }
    }

    @Override
    public void deleteNodeIndex(String name, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                IndexManager index = mDb.index();
                index.forNodes(name); // this will create the index
            } finally {
                suspendCurrentTrx("deleteNodeIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete node index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void addNodeToIndex(String name, long nodeId, String key, ParcelableIndexValue value, ParcelableError err)
            throws RemoteException {
        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Index<Node> nodeIndex = mDb.index().forNodes(name);
                Node node = mDb.getNodeById(nodeId); // will throw
                                                     // NotFoundException if id
                                                     // is invalid
                nodeIndex.add(node, key, value.get());
            } finally {
                suspendCurrentTrx("addNodeToIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add node to index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void updateNodeInIndex(String name, long nodeId, String key, ParcelableIndexValue value, ParcelableError err)
            throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Index<Node> nodeIndex = mDb.index().forNodes(name);
                Node node = mDb.getNodeById(nodeId); // will throw
                                                     // NotFoundException if id
                                                     // is invalid

                // See http://docs.neo4j.org/chunked/stable/indexing-update.html
                nodeIndex.remove(node, key, value.get());
                nodeIndex.add(node, key, value.get());
            } finally {
                suspendCurrentTrx("updateNodeInIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update node in index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }

    }

    @Override
    public void removeNodeFromIndex(String name, long nodeId, ParcelableError err) throws RemoteException {
        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Index<Node> nodeIndex = mDb.index().forNodes(name);
                Node node = mDb.getNodeById(nodeId); // will throw
                                                     // NotFoundException if id
                                                     // is invalid
                nodeIndex.remove(node);
            } finally {
                suspendCurrentTrx("removeNodeFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove node from index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }

    }

    @Override
    public void removeNodeKeyFromIndex(String name, long nodeId, String key, ParcelableError err) throws RemoteException {
        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Index<Node> nodeIndex = mDb.index().forNodes(name);
                Node node = mDb.getNodeById(nodeId); // will throw
                                                     // NotFoundException if id
                                                     // is invalid
                nodeIndex.remove(node, key);
            } finally {
                suspendCurrentTrx("removeNodeKeyFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove node from index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void removeNodeKeyValueFromIndex(String name, long nodeId, String key, ParcelableIndexValue value, ParcelableError err)
            throws RemoteException {
        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Index<Node> nodeIndex = mDb.index().forNodes(name);
                Node node = mDb.getNodeById(nodeId); // will throw
                                                     // NotFoundException if id
                                                     // is invalid
                nodeIndex.remove(node, key, value.get());
            } finally {
                suspendCurrentTrx("removeNodeKeyValueFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove node from index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public INodeIterator getNodesFromIndex(String name, String key, ParcelableIndexValue value, ParcelableError err)
            throws RemoteException {
        try {
            resumeTrxIfExists();
            try {
                Index<Node> nodeIndex = mDb.index().forNodes(name);
                IndexHits<Node> hits = nodeIndex.get(key, value.get());
                return new NodeIteratorWrapper(hits.iterator());
            } finally {
                suspendCurrentTrx("getNodesFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove node from index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Relationship Indexing support
    // -------------------------------------------------------------------------

    @Override
    public void createRelationshipIndex(String name, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                mDb.index().forRelationships(name); // this will create the
                                                    // index
            } finally {
                suspendCurrentTrx("createRelationshipIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create/access relationship index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public boolean relationshipIndexExists(String name, ParcelableError err) throws RemoteException {
        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                boolean indexExists = mDb.index().existsForRelationships(name);
                return indexExists;
            } finally {
                suspendCurrentTrx("relationshipIndexExists");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to access relationship index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return false; // have to return something
        }
    }

    @Override
    public void deleteRelationshipIndex(String name, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index
                index.delete();
            } finally {
                suspendCurrentTrx("deleteRelationshipIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete relationship index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void addRelationshipToIndex(String name, long relationshipId, String key, ParcelableIndexValue value, ParcelableError err)
            throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship rel = mDb.getRelationshipById(relationshipId);
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index
                index.add(rel, key, value.get());
            } finally {
                suspendCurrentTrx("addRelationshipToIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add relationship to index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void updateRelationshipInIndex(String name, long relationshipId, String key, ParcelableIndexValue value,
            ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship rel = mDb.getRelationshipById(relationshipId);
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index

                // See http://docs.neo4j.org/chunked/stable/indexing-update.html
                index.remove(rel, key, value.get());
                index.add(rel, key, value.get());
            } finally {
                suspendCurrentTrx("updateRelationshipInIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update relationship in index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }
    }

    @Override
    public void removeRelationshipFromIndex(String name, long relationshipId, ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship rel = mDb.getRelationshipById(relationshipId);
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index
                index.remove(rel);
            } finally {
                suspendCurrentTrx("removeRelationshipFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add relationship to index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }

    }

    @Override
    public void removeRelationshipKeyFromIndex(String name, long relationshipId, String key, ParcelableError err)
            throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship rel = mDb.getRelationshipById(relationshipId);
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index
                index.remove(rel, key);
            } finally {
                suspendCurrentTrx("removeRelationshipKeyFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add relationship to index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }

    }

    @Override
    public void removeRelationshipKeyValueFromIndex(String name, long relationshipId, String key, ParcelableIndexValue value,
            ParcelableError err) throws RemoteException {

        try {
            checkCallerHasWritePermission();
            resumeTrx();

            try {
                Relationship rel = mDb.getRelationshipById(relationshipId);
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index
                index.remove(rel, key, value.get());
            } finally {
                suspendCurrentTrx("removeRelationshipKeyValueFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add relationship to index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
        }

    }

    @Override
    public IRelationshipIterator getRelationshipsFromIndex(String name, String key, ParcelableIndexValue value, ParcelableError err)
            throws RemoteException {

        try {
            resumeTrxIfExists();

            try {
                RelationshipIndex index = mDb.index().forRelationships(name); // this
                                                                              // will
                                                                              // create
                                                                              // the
                                                                              // index
                IndexHits<Relationship> hits = index.get(key, value.get());
                return new RelationshipIteratorWrapper(hits);
            } finally {
                suspendCurrentTrx("getRelationshipsFromIndex");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add relationship to index '" + name + "'", e);
            err.setError(Errors.TRANSACTION, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertTransactionExists() {

        boolean hasTransaction = mTrxManager.hasAssociatedTrx(this.asBinder());
        // log.d("hasTransaction: " + hasTransaction);
        if (!hasTransaction) {
            throw new IllegalStateException("No transaction associated with this Binder");
        }
    }

    /**
     * Resume the transaction that is associated with this Binder.
     * 
     * @param required if a transaction is required
     * @return the transaction
     */
    private Transaction resumeTrx() throws InvalidTransactionException, SystemException {

        assertTransactionExists();

        Transaction trx = mTrxManager.getAssociatedTrx(this.asBinder());
        jtaResume(trx);

        return trx;
    }

    /**
     * Resume a transaction, if one is associated with this Binder instance.
     * 
     * @return the transaction object, or null if no transaction was associated
     *         with the binder.
     * @throws InvalidTransactionException
     * @throws SystemException
     */
    private Transaction resumeTrxIfExists() throws InvalidTransactionException, SystemException {

        Transaction trx = mTrxManager.getAssociatedTrx(this.asBinder());
        if (trx != null) {
            jtaResume(trx);
        }
        return trx;
    }

    /**
     * Resume the JTA transaction.
     * 
     * @param trx
     * @throws SystemException
     * @throws InvalidTransactionException
     * @throws IllegalStateException
     */
    private void jtaResume(Transaction trx) throws SystemException, InvalidTransactionException, IllegalStateException {

        if (trx == null) {
            throw new IllegalArgumentException("Null transaction passed");
        }

        org.neo4j.javax.transaction.Transaction jtaTrx = ((TopLevelTransaction) trx).getJtaTransaction();
        TransactionManager trxManager = mDb.getConfig().getTxModule().getTxManager();

        trxManager.resume(jtaTrx); // this will throw an exception if an
                                   // exception is already associated with this
                                   // thread
    }

    /**
     * Suspend this thread's current transaction, if one exists.
     * 
     * @throws SystemException
     */
    private void suspendCurrentTrx(String operationName) throws SystemException {

        mTrxManager.suspendCurrentTrx(this.asBinder(), operationName);
        TransactionManager trxManager = mDb.getConfig().getTxModule().getTxManager();
        if (trxManager.getTransaction() != null) {
            // log.d("Found running transaction associated with this thread, suspending it");
            trxManager.suspend();
        }
    }

    /**
     * Check if the caller has permission to write data.
     * 
     * @throws SecurityException if the caller does not have the permission to
     *             write data
     */
    private void checkCallerHasWritePermission() throws SecurityException {

        int access = mContext.checkCallingPermission(Permissions.PERM_WRITE);
        if (access == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.w(TAG, "IPC caller lacks required permission: " + Permissions.PERM_WRITE);
        throw new SecurityException("IPC caller lacks required permission: " + Permissions.PERM_WRITE);
    }
}
