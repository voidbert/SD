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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;

import org.example.sd.common.KeyValueDB;

public class SimpleHashMapBackend extends SingleLockHashMapBackend {
    private Condition databaseChangedCondition;
    private Set<Long> waitingTriggers;

    public SimpleHashMapBackend() {
        super();
        this.databaseChangedCondition = this.lock.writeLock().newCondition();
        this.waitingTriggers          = new HashSet<Long>();
    }

    public SimpleHashMapBackend(SimpleHashMapBackend database) {
        this();
        this.map = database.getMap();
    }

    protected void summonTriggersAfterPut(String key, byte[] value) {
        if (this.waitingTriggers.size() > 0) {
            this.unsignaledTriggers.addAll(this.waitingTriggers);
            this.databaseChangedCondition.signalAll();
        }
    }

    protected void summonTriggersAfterMultiPut(Map<String, byte[]> pairs) {
        if (this.waitingTriggers.size() > 0) {
            this.unsignaledTriggers.addAll(this.waitingTriggers);
            this.databaseChangedCondition.signalAll();
        }
    }

    protected void getWhenWait(String keyCond, byte[] valueCond) {
        long threadId = Thread.currentThread().threadId();
        this.waitingTriggers.add(threadId);

        while (!Arrays.equals(this.map.get(keyCond), valueCond)) {
            this.databaseChangedCondition.awaitUninterruptibly();

            // Signal end of trigger execution
            if (this.unsignaledTriggers.contains(threadId)) {
                this.unsignaledTriggers.remove(threadId);

                if (this.unsignaledTriggers.size() == 0)
                    this.triggersDoneCondition.signalAll();
            }
        }

        this.waitingTriggers.remove(threadId);
    }

    @Override
    public Object clone() {
        return new SimpleHashMapBackend(this);
    }

    @Override
    public String toString() {
        return "SimpleHashMapBackend(" + super.toString() + ")";
    }
}
