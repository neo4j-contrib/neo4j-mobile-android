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
 * A holder for an index value. AIDL does not support passing Object, so we wrap
 * it into a Parcelable. The wrapped value needs to be of one of the types
 * supported by {@link android.os.Parcel#writeValue(Object)}.
 * 
 * @see android.os.Parcel#writeValue
 */
public class ParcelableIndexValue implements Parcelable {

    private Object object;

    public ParcelableIndexValue() {
    }

    /**
     * Convenience method for getting a parcelable index value.
     * 
     * @param value
     */
    public static ParcelableIndexValue withValue(Object value) {
        ParcelableIndexValue result = new ParcelableIndexValue();
        result.set(value);
        return result;
    }

    protected ParcelableIndexValue(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        object = in.readValue(getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(object);
    }

    public static final Parcelable.Creator<ParcelableIndexValue> CREATOR = new Parcelable.Creator<ParcelableIndexValue>() {
        public ParcelableIndexValue createFromParcel(Parcel in) {
            return new ParcelableIndexValue(in);
        }

        public ParcelableIndexValue[] newArray(int size) {
            return new ParcelableIndexValue[size];
        }
    };

    /**
     * Set the wrapped value. The type must be one of the types supported by
     * {@link android.os.Parcel#writeValue(Object)}
     * 
     * @param object
     */
    public void set(Object object) {
        this.object = object;
    }

    /**
     * Get the wrapped value.
     */
    public Object get() {
        return object;
    }

}
