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
package org.neo4j.android.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.neo4j.graphdb.Transaction;
import org.neo4j.javax.transaction.TransactionManager;
import org.neo4j.kernel.TopLevelTransaction;

import android.os.IBinder;
import android.util.Log;

/**
 * A helper that associates Neo4j transactions with IBinder instances.
 */
public class TrxManager {

    private static final String TAG = TrxManager.class.getSimpleName();

    private class TrxHolder {

        private Transaction trx;

        private TransactionManager jtaTxManager;

        private long lastModifiedTime;

        private long totalOperationTime;

        private StringBuffer opTimes = new StringBuffer();

        public TrxHolder(Transaction trx, TransactionManager jtaTrxMgr) {
            this.trx = trx;
            this.jtaTxManager = jtaTrxMgr;
            this.lastModifiedTime = System.currentTimeMillis();
        }

        public void startProfiling() {
            lastModifiedTime = System.currentTimeMillis();
        }

        public void suspendProfiling(String operationName) {
            long operationTime = System.currentTimeMillis() - lastModifiedTime;
            totalOperationTime += operationTime;
            opTimes.append(operationName);
            opTimes.append(":");
            opTimes.append(operationTime);
            opTimes.append("ms,");
        }

        public String getProfilingResult() {
            opTimes.append("total:");
            opTimes.append(totalOperationTime);
            opTimes.append("ms");
            return opTimes.toString();
        }

        public long getLastModifiedTime() {
            return lastModifiedTime;
        }
    }

    private ReentrantLock mLock;

    /**
     * Use Binder identity instead of Thread identity for transaction context
     */
    private HashMap<IBinder, TrxHolder> mTrxMap;

    public TrxManager() {
        mTrxMap = new HashMap<IBinder, TrxHolder>();
        mLock = new ReentrantLock(true);
    }

    public Transaction getAssociatedTrx(IBinder binder) {

        try {
            mLock.lock();

            TrxHolder holder = mTrxMap.get(binder);
            if (holder == null) {
                return null;
            } else {
                holder.startProfiling();
                return holder.trx;
            }
        } finally {
            mLock.unlock();
        }

    }

    public void suspendCurrentTrx(IBinder binder, String operationName) {
        try {
            mLock.lock();
            TrxHolder holder = mTrxMap.get(binder);
            if (holder != null) {
                holder.suspendProfiling(operationName);
            }
        } finally {
            mLock.unlock();
        }
    }

    public Transaction disassociateTrx(IBinder binder, String operationName) {

        try {
            mLock.lock();

            TrxHolder holder = mTrxMap.remove(binder);
            if (holder == null) {
                Log.w(TAG, "Attempted to disassociate transaction from Binder, but none found");
                return null;
            } else {
                holder.suspendProfiling(operationName);
                Log.d(TAG, "Disassociated transaction, profiling: " + holder.getProfilingResult());
                return holder.trx;
            }
        } finally {
            mLock.unlock();
        }
    }

    public void associateTrx(IBinder binder, Transaction trx, TransactionManager trxMgr) {

        try {
            mLock.lock();
            if (hasAssociatedTrx(binder)) {
                throw new IllegalStateException("A transaction is already associated with this Binder");
            }

            TrxHolder holder = new TrxHolder(trx, trxMgr);
            holder.startProfiling(); // association at beginning of trx
            mTrxMap.put(binder, holder);
        } finally {
            mLock.unlock();
        }
    }

    public boolean hasAssociatedTrx(IBinder binder) {

        try {
            mLock.lock();
            return mTrxMap.containsKey(binder);
        } finally {
            mLock.unlock();
        }
    }

    /* package */int rollbackZombies(long periodMillis) {

        int numCleaned = 0;

        try {
            mLock.lock();

            final long now = System.currentTimeMillis();

            Iterator<Entry<IBinder, TrxHolder>> entries = mTrxMap.entrySet().iterator();
            while (entries.hasNext()) {
                Entry<IBinder, TrxHolder> entry = entries.next();
                if (now > entry.getValue().getLastModifiedTime() + periodMillis) {

                    entries.remove();
                    numCleaned++;

                    Log.w(TAG, "Reaping zombie (?) transaction after " + (now - entry.getValue().getLastModifiedTime()) / 1000
                            + "s");

                    try {
                        Transaction trx = entry.getValue().trx;
                        TransactionManager trxMgr = entry.getValue().jtaTxManager;
                        // resume the transaction on the reaper thread
                        Log.d(TAG, "Resuming transaction on reaper thread");
                        trxMgr.resume(((TopLevelTransaction) trx).getJtaTransaction());

                        // mark it as failed, and finish it (that will
                        // disassociate it with the thread)
                        trx.failure();
                        trx.finish();
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to clean up zombie transaction", ex);
                    }
                }
            }
        } finally {
            mLock.unlock();
        }

        return numCleaned;
    }

}
