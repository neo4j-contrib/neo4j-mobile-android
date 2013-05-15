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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable representation of a Neo4j relationship.
 */
public class ParcelableRelationship extends ParcelablePropertyContainer {

    private long id;

    private String name;

    private long startNodeId;

    private long endNodeId;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {

        super.writeToParcel(out, flags);

        out.writeLong(id);
        out.writeString(name);
        out.writeLong(startNodeId);
        out.writeLong(endNodeId);
    }

    public ParcelableRelationship() {
    }

    private ParcelableRelationship(Parcel in) {

        super(in);

        id = in.readLong();
        name = in.readString();
        startNodeId = in.readLong();
        endNodeId = in.readLong();
    }

    public static final Parcelable.Creator<ParcelableRelationship> CREATOR = new Parcelable.Creator<ParcelableRelationship>() {
        public ParcelableRelationship createFromParcel(Parcel in) {
            return new ParcelableRelationship(in);
        }

        public ParcelableRelationship[] newArray(int size) {
            return new ParcelableRelationship[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(long startNodeId) {
        this.startNodeId = startNodeId;
    }

    public long getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(long endNodeId) {
        this.endNodeId = endNodeId;
    }

    /**
     * Convenience method that indicates if this relationship is outgoing with
     * regards to the given node.
     * 
     * @param nodeId
     */
    public boolean isOutgoing(long nodeId) {
        return this.startNodeId == nodeId;
    }

    /**
     * Convenience method that indicates if this relationship is incoming with
     * regards to the given node.
     * 
     * @param nodeId
     */
    public boolean isIncoming(long nodeId) {
        return this.endNodeId == nodeId;
    }

    @Override
    public String toString() {

        StringBuilder tmp = new StringBuilder(512);
        tmp.append("Relationship[id=");
        tmp.append(id);
        tmp.append(" name=");
        tmp.append(name);
        tmp.append(" startNodeId=");
        tmp.append(startNodeId);
        tmp.append(" endNodeId=");
        tmp.append(endNodeId);
        tmp.append(" properties=");
        tmp.append(super.toString());
        tmp.append("]");
        return tmp.toString();
    }

}
