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
 * A container for an error code and message. Used to signal server errors over
 * IPC (exceptions are not supported across processes)
 */
public class ParcelableError implements Parcelable {

    private int errorCode;

    private String errorMessage;

    public ParcelableError() {
        errorCode = 0;
    }

    protected ParcelableError(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        errorCode = in.readInt();
        errorMessage = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(errorCode);
        out.writeString(errorMessage);
    }

    public static final Parcelable.Creator<ParcelableError> CREATOR = new Parcelable.Creator<ParcelableError>() {
        public ParcelableError createFromParcel(Parcel in) {
            return new ParcelableError(in);
        }

        public ParcelableError[] newArray(int size) {
            return new ParcelableError[size];
        }
    };

    public boolean isError() {
        return errorCode != 0;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("Error[code: ");
        tmp.append(errorCode);
        if (errorMessage != null) {
            tmp.append(" message: ");
            tmp.append(errorMessage);
        }
        tmp.append("]");
        return tmp.toString();
    }
}
