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

public final class DBInspectorConstants {

    private DBInspectorConstants() {
        // do not instantiate.
    }

    public static final String INTENT_PREFIX = "com.noser.neo4j.android.dbinspector.";

    public static final String TEST_DATABASE = "dbi_test_db";

    public static final long WAIT_FOR_SERVICE_TIME = 1000; // ms

    public static final int TEST_DATABASE_DENSITY = 16;

    public static final int TEST_DATABASE_DEPTH = 2;

    public static final String INTENTEXTRA_MOVE_LEFT = INTENT_PREFIX + "MoveLeft";

    public static final String INTENTEXTRA_NODEID = INTENT_PREFIX + "NodeId";

    public static final String INTENTEXTRA_CENTER_NODEID = INTENT_PREFIX + "CenterNodeId";

    public static final String INTENTEXTRA_PEER_NODEID = INTENT_PREFIX + "PeerNodeId";

    public static final String INTENTEXTRA_RELATIONSHIPID = INTENT_PREFIX + "RelationshipId";

    public static final String INTENTEXTRA_NODE_DELETED = INTENT_PREFIX + "ActivityResultNodeDeleted";

    public static final String INTENTEXTRA_RELATIONSHIP_DELETED = INTENT_PREFIX + "ActivityResultRelationshipDeleted";

    public static final int BUTTON_MINIMUM_HEIGHT = 80; // dp

    public static final int BUTTON_MINIMUM_WIDTH = 80; // dp

    public static final int DEFAULT_REQUESTCODE = 0;

}
