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
package com.noser.neo4j.android.dbinspector.base;

import java.util.List;

import org.neo4j.android.client.GraphDatabase;
import org.neo4j.android.common.ParcelableNode;

public class DBInspectorUtils {

    private DBInspectorUtils() {
        // do not instantiate.
    }

    public List<ParcelableNode> findOneNodePerCycleInGraph(GraphDatabase database) {
        return null;
        // TODO: [eRiC] find all cycles

        // Traversal.description().uniqueness( Uniqueness.RELATIONSHIP_GLOBAL
        // )
        // .evaluator(new Evaluator() {
        // public Evaluation(Path path) {
        // return path.length() > 0 &&
        // endNodeOccursPreviouslyInPath(
        // path ) ?
        // Evaluation.INCLUDE_AND_CONTINUE :
        // Evaluation.EXCLUDE_AND_CONTINUE;
        // }
        //
        // private boolean endNodeOccursPreviouslyInPath(Path path) {
        // Node endNode = path.endNode();
        // int counter = 0;
        // for ( Node node : path.nodes() ) {
        // if ( counter++ < path.length() && node.equals(
        // endNode ) )
        // return true;
        // }
        // return false;
        // }
        // } ).traverse(...);

    }
}
