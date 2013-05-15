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

import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable representation of a Neo4j traversal description. We do not
 * support all features of the new traversal framework. Especially evaluators
 * (these could be Binder objects on the client) would be interesting.
 */
public class ParcelableTraversalDescription implements Parcelable {

    private Order order;

    private Uniqueness uniqueness;

    private Map<String, Direction> relationships;

    public enum Order {
        BREADTH_FIRST, DEPTH_FIRST
    }

    public enum Uniqueness {
        NONE, NODE_GLOBAL, RELATIONSHIP_GLOBAL, NODE_PATH, RELATIONSHIP_PATH, NODE_RECENT, RELATIONSHIP_RECENT
    }

    public enum Direction {
        OUTGOING, INCOMING, BOTH
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ParcelableTraversalDescription() {
        order = Order.DEPTH_FIRST;
        uniqueness = Uniqueness.NODE_GLOBAL;
        // empty means traverse all relationships:
        relationships = new HashMap<String, Direction>();
    }

    protected ParcelableTraversalDescription(Parcel in) {

        order = Order.valueOf(in.readString());
        uniqueness = Uniqueness.valueOf(in.readString());

        int numRelationships = in.readInt();
        relationships = new HashMap<String, Direction>(numRelationships);

        for (int i = 0; i < numRelationships; i++) {
            String name = in.readString();
            Direction direction = Direction.valueOf(in.readString());
            relationships.put(name, direction);
        }
    }

    public static final Parcelable.Creator<ParcelableTraversalDescription> CREATOR = new Parcelable.Creator<ParcelableTraversalDescription>() {
        public ParcelableTraversalDescription createFromParcel(Parcel in) {
            return new ParcelableTraversalDescription(in);
        }

        public ParcelableTraversalDescription[] newArray(int size) {
            return new ParcelableTraversalDescription[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(order.name());
        dest.writeString(uniqueness.name());

        dest.writeInt(relationships.size());

        for (String key : relationships.keySet()) {
            dest.writeString(key);
            dest.writeString(relationships.get(key).name());
        }
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Uniqueness getUniqueness() {
        return uniqueness;
    }

    public void setUniqueness(Uniqueness uniqueness) {
        this.uniqueness = uniqueness;
    }

    public Map<String, Direction> getRelationships() {
        return relationships;
    }

}
