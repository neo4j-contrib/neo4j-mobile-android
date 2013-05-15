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
 * A parcelable representation of a Neo4j property container (node or
 * relationship).
 */
public class ParcelablePropertyContainer implements Parcelable {

    private Map<String, Object> properties;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(properties);
    }

    public ParcelablePropertyContainer() {
        this.properties = new HashMap<String, Object>(10 /* magic number */);
    }

    protected ParcelablePropertyContainer(Parcel in) {
        properties = new HashMap<String, Object>(10 /* magic number */);
        in.readMap(properties, null);
    }

    public static final Parcelable.Creator<ParcelablePropertyContainer> CREATOR = new Parcelable.Creator<ParcelablePropertyContainer>() {
        public ParcelablePropertyContainer createFromParcel(Parcel in) {
            return new ParcelablePropertyContainer(in);
        }

        public ParcelablePropertyContainer[] newArray(int size) {
            return new ParcelablePropertyContainer[size];
        }
    };

    public Iterable<String> getPropertyKeys() {
        return properties.keySet();
    }

    /**
     * Return the named property, or null if no value was associated with the
     * key.
     * 
     * @param key
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Set a property on this container. Only types supported by
     * android.os.Bundle are supported.
     * 
     * @param key
     * @param value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Remove the named property.
     * 
     * @param key
     */
    public void removeProperty(String key) {
        properties.remove(key);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public String toString() {

        StringBuilder tmp = new StringBuilder();
        tmp.append("{");
        for (String key : properties.keySet()) {
            tmp.append(key);
            tmp.append("='");
            tmp.append(properties.get(key));
            tmp.append("', ");
        }
        tmp.append("}");
        return tmp.toString();
    }

}
