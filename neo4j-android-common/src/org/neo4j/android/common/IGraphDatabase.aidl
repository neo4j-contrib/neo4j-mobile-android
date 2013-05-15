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
package org.neo4j.android.common;

import android.os.Bundle;

import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;
import org.neo4j.android.common.ParcelableTraversalDescription;
import org.neo4j.android.common.ParcelableError;
import org.neo4j.android.common.ParcelableIndexValue;

import org.neo4j.android.common.INodeIterator;
import org.neo4j.android.common.IRelationshipIterator;

interface IGraphDatabase {

	// Top-level retrieval functions
	ParcelableNode getReferenceNode(out ParcelableError err);
	ParcelableNode getNodeById(long id, out ParcelableError err);
	INodeIterator getAllNodes(out ParcelableError err);
	List<String> getRelationshipTypes(out ParcelableError err);
	
	// Node modification
	long createNode(in ParcelableNode node, out ParcelableError err);
	void updateNode(in ParcelableNode node, out ParcelableError err);
	void deleteNode(long id, out ParcelableError err);
	
	// Relationships
	ParcelableRelationship getRelationshipById(long id, out ParcelableError err);
	long createRelationship(in ParcelableRelationship rel, out ParcelableError err);
	void updateRelationship(in ParcelableRelationship rel, out ParcelableError err);
	void deleteRelationship(long id, out ParcelableError err);
	
	// Traversal
	INodeIterator traverse(
		in ParcelableTraversalDescription desc,
		long startNodeId,
		out ParcelableError err
		);
	
	// Transactions
	void beginTx(out ParcelableError err);
	void txSuccess(out ParcelableError err);
	void txFailure(out ParcelableError err);
	void txFinish(out ParcelableError err);
	
	// Node Indices
	void createNodeIndex(String name, out ParcelableError err);
	boolean nodeIndexExists(String name, out ParcelableError err);
	void deleteNodeIndex(String name, out ParcelableError err);
	
	// Working with indices
	void addNodeToIndex(
		in String name, long nodeId,
		in String key, in ParcelableIndexValue value,
		out ParcelableError err
		);
		
	void updateNodeInIndex(
		in String name, long nodeId,
		in String key, in ParcelableIndexValue value,
		out ParcelableError err
		);
	
	void removeNodeFromIndex(
		in String name, long nodeId,
		out ParcelableError err
		);
		
	void removeNodeKeyFromIndex(
		in String name, long nodeId, in String key,
		out ParcelableError err
		);
		
	void removeNodeKeyValueFromIndex(
		in String name, long nodeId, in String key, in ParcelableIndexValue value,
		out ParcelableError err
		);
	
	// TODO: we should use something more specialized, since we have to call close() on an iterator that was not used completely
	INodeIterator getNodesFromIndex(
		in String name, in String key, in ParcelableIndexValue value,
		out ParcelableError err
		);
	
	// Relationship Indices
	void createRelationshipIndex(String name, out ParcelableError err);
	boolean relationshipIndexExists(String name, out ParcelableError err);
	void deleteRelationshipIndex(String name, out ParcelableError err);
	
	void addRelationshipToIndex(
		in String name,
		long relationshipId,
		in String key,
		in ParcelableIndexValue value,
		out ParcelableError err
		);
		
	void updateRelationshipInIndex(
		in String name,
		long relationshipId,
		in String key,
		in ParcelableIndexValue value,
		out ParcelableError err
		);
	
	void removeRelationshipFromIndex(
		in String name,
		long relationshipId,
		out ParcelableError err
		);
		
	void removeRelationshipKeyFromIndex(
		in String name,
		long relationshipId,
		in String key,
		out ParcelableError err
		);
		
	void removeRelationshipKeyValueFromIndex(
		in String name,
		long relationshipId,
		in String key,
		in ParcelableIndexValue value,
		out ParcelableError err
		);
		
	IRelationshipIterator getRelationshipsFromIndex(
		in String name,
		in String key,
		in ParcelableIndexValue value,
		out ParcelableError err
		);
}
