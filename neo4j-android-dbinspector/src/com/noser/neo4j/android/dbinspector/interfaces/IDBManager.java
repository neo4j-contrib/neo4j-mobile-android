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
package com.noser.neo4j.android.dbinspector.interfaces;

import java.util.List;

import org.neo4j.android.client.GraphDatabase;

import com.noser.neo4j.android.dbinspector.base.DBInspectorException;

public interface IDBManager {

    boolean isNeo4jServiceAvailable();

    List<String> listAvailableNeo4jDatabases() throws DBInspectorException;

    void openOrCreateNeo4jDatabase(String databaseName) throws DBInspectorException;

    void shutdownNeo4jDatabase(String databaseName) throws DBInspectorException;

    void deleteNeo4jDatabase(String databaseName) throws DBInspectorException;

    void exportNeo4jDatabase(String databaseName) throws DBInspectorException;

    boolean neo4jDatabaseExists(String databaseName) throws DBInspectorException;

    boolean isNeo4jDatabaseOpen(String databaseName) throws DBInspectorException;

    boolean isCurrentNeo4jDatabaseOpen();

    String getCurrentNeo4jDatabaseName();

    GraphDatabase getCurrentNeo4jDatabase();

}
