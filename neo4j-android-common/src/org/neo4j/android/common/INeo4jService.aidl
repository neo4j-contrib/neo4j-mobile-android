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

import org.neo4j.android.common.ParcelableError;

import org.neo4j.android.common.IGraphDatabase;

interface INeo4jService {

	String getNeo4jVersion();

	boolean databaseExists(in String name);
	
	boolean isDatabaseOpen(in String name);
	
	IGraphDatabase openOrCreateDatabase(in String name, out ParcelableError err);
	
	boolean deleteDatabase(in String name, out ParcelableError err);
	
	boolean exportDatabase(in String name, out ParcelableError err);
	
	List<String> listAvailableDatabases();
	
	void shutdownDatabase(in String name, out ParcelableError err);
}
