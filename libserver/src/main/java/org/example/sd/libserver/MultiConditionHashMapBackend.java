/*
 * Copyright 2024 Carolina Pereira, Diogo Costa, Humberto Gomes, Sara Lopes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.sd.libserver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;

import org.example.sd.common.KeyValueDB;

public class MultiConditionHashMapBackend extends SingleLockHashMapBackend {
    private Map<String, Condition> databaseChangedConditions;
    private Map<String, Set<Long>> waitingTriggers;

    public MultiConditionHashMapBackend() {
        super();
        this.databaseChangedConditions = new HashMap<String, Condition>();
        this.waitingTriggers           = new HashMap<String, Set<Long>>();
    }

    public MultiConditionHashMapBackend(MultiConditionHashMapBackend database) {
        this();
        this.map = database.getMap();
    }

    protected void summonTriggersAfterPut(String key, byte[] value) {
        Set<Long> keyTriggers = this.waitingTriggers.get(key);
        if (keyTriggers != null) {
            this.unsignaledTriggers.addAll(keyTriggers);
            this.databaseChangedConditions.get(key).signalAll();
        }
    }

    protected void summonTriggersAfterMultiPut(Map<String, byte[]> pairs) {
        for (String key : pairs.keySet()) {
            Set<Long> keyTriggers = this.waitingTriggers.get(key);
            if (keyTriggers != null) {
                this.unsignaledTriggers.addAll(keyTriggers);
                this.databaseChangedConditions.get(key).signalAll();
            }
        }
    }

    protected void getWhenWait(String keyCond, byte[] valueCond) {
        long threadId = Thread.currentThread().threadId();

        // Add current thread to set of waiting threads
        Set<Long> keyWaitingThreads = this.waitingTriggers.get(keyCond);
        if (keyWaitingThreads == null) {
            keyWaitingThreads = new HashSet<Long>();
            this.waitingTriggers.put(keyCond, keyWaitingThreads);
            this.databaseChangedConditions.put(keyCond, this.lock.writeLock().newCondition());
        }
        keyWaitingThreads.add(threadId);

        // Wait for the database to change
        Condition waitCondition = this.databaseChangedConditions.get(keyCond);
        while (!Arrays.equals(this.map.get(keyCond), valueCond)) {
            waitCondition.awaitUninterruptibly();

            // Signal end of trigger execution
            if (this.unsignaledTriggers.contains(threadId)) {
                this.unsignaledTriggers.remove(threadId);

                if (this.unsignaledTriggers.size() == 0)
                    this.triggersDoneCondition.signalAll();
            }
        }

        // Waiting threads cleanup
        keyWaitingThreads.remove(threadId);
        if (keyWaitingThreads.size() == 0) {
            this.databaseChangedConditions.remove(keyCond);
            this.waitingTriggers.remove(keyCond);
        }
    }

    @Override
    public Object clone() {
        return new MultiConditionHashMapBackend(this);
    }

    @Override
    public String toString() {
        return "MultiConditionHashMapBackend(" + super.toString() + ")";
    }
}
