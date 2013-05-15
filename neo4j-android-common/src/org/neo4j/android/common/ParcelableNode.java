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

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Parcelable representation of a Neo4j node, to be used on the client side. DO
 * NOT introduce Neo4j dependencies in here.
 * 
 * @author ahs
 */
public class ParcelableNode extends ParcelablePropertyContainer {

    private long id;

    private List<ParcelableRelationship> relationships;

    @Override
    public void writeToParcel(Parcel out, int flags) {

        super.writeToParcel(out, flags);

        out.writeLong(id);
        out.writeList(relationships);
    }

    public ParcelableNode() {

        this.relationships = new ArrayList<ParcelableRelationship>(4 /*
                                                                      * magic
                                                                      * number
                                                                      */);
    }

    protected ParcelableNode(Parcel in) {

        super(in);

        id = in.readLong();

        relationships = new ArrayList<ParcelableRelationship>(4 /* magic number */);
        in.readList(relationships, ParcelableRelationship.class.getClassLoader());
    }

    public static final Parcelable.Creator<ParcelableNode> CREATOR = new Parcelable.Creator<ParcelableNode>() {
        public ParcelableNode createFromParcel(Parcel in) {
            return new ParcelableNode(in);
        }

        public ParcelableNode[] newArray(int size) {
            return new ParcelableNode[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<ParcelableRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<ParcelableRelationship> relationships) {
        this.relationships = relationships;
    }

    /**
     * Convenience method that indicates if the node has a relationship in the
     * given direction.
     * 
     * @param dir
     */
    public boolean hasRelationship(Direction dir) {

        if (dir == null) {
            throw new IllegalArgumentException("Need to specify a direction (HINT: use Direction.BOTH if direction is irrelevant)");
        }

        for (ParcelableRelationship rel : this.relationships) {
            if (dir == Direction.INCOMING && rel.getEndNodeId() == this.id) {
                return true;
            } else if (dir == Direction.OUTGOING && rel.getStartNodeId() == this.id) {
                return true;
            } else if (dir == Direction.BOTH) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the single relationship that matches the criteria.<br/>
     * TODO: in Neo4j this method behaves differently, it will throw an
     * exception if multiple relationships match the given criteria.
     */
    public ParcelableRelationship getSingleRelationship(String type, Direction dir) {

        if (type == null) {
            throw new IllegalArgumentException("Need to specify a type");
        }
        if (dir == null) {
            throw new IllegalArgumentException("Need to specify a direction (HINT: use Direction.BOTH if direction is irrelevant)");
        }

        for (ParcelableRelationship rel : this.relationships) {

            if (rel.getName().equals(type)) {

                if (dir == Direction.INCOMING && rel.getEndNodeId() == this.id) {
                    return rel;
                } else if (dir == Direction.OUTGOING && rel.getStartNodeId() == this.id) {
                    return rel;
                } else {
                    return rel;
                }
            }
        }

        return null;
    }

    @Override
    public String toString() {

        StringBuilder tmp = new StringBuilder(512);
        tmp.append("Node[id=");
        tmp.append(id);
        tmp.append(" properties=");
        tmp.append(super.toString());
        tmp.append(" relationships=[");
        for (ParcelableRelationship rel : relationships) {
            tmp.append(rel.toString());
            tmp.append(", ");
        }
        tmp.append("]");
        return tmp.toString();
    }
}
