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
package org.neo4j.android.service.util;

import org.neo4j.android.common.ParcelableNode;
import org.neo4j.android.common.ParcelableRelationship;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * Convert Neo4j objects to parcelable representations.
 */
public class ParcelableFactory {

    public static ParcelableNode makeParcelableNode(Node node) {

        ParcelableNode pNode = new ParcelableNode();
        pNode.setId(node.getId());

        // shallow copy of properties
        for (String key : node.getPropertyKeys()) {
            pNode.setProperty(key, node.getProperty(key));
        }

        // shallow copy of relationships
        for (Relationship rel : node.getRelationships()) {
            ParcelableRelationship pRel = makeParcelableRelationship(rel);
            pNode.getRelationships().add(pRel);
        }

        return pNode;
    }

    public static ParcelableRelationship makeParcelableRelationship(Relationship rel) {

        ParcelableRelationship pRel = new ParcelableRelationship();
        pRel.setId(rel.getId());
        pRel.setName(rel.getType().name());
        pRel.setStartNodeId(rel.getStartNode().getId());
        pRel.setEndNodeId(rel.getEndNode().getId());

        // shallow copy of properties
        for (String key : rel.getPropertyKeys()) {
            pRel.setProperty(key, rel.getProperty(key));
        }
        return pRel;
    }

}
