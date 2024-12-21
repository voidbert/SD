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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.example.sd.common.KeyValueDB;

public class MultiConditionHashMapBackend implements KeyValueDB {
    private ReadWriteLock          lock;
    private Map<String, Condition> databaseChangedConditions;
    private Condition              triggersDoneCondition;

    private Map<String, byte[]> map;

    private Map<String, Set<Long>> waitingTriggers;
    private Set<Long>              unsignaledTriggers;

    public MultiConditionHashMapBackend() {
        this.lock                      = new ReentrantReadWriteLock(true);
        this.databaseChangedConditions = new HashMap<String, Condition>();
        this.triggersDoneCondition     = this.lock.writeLock().newCondition();

        this.map = new HashMap<>();

        this.waitingTriggers    = new HashMap<String, Set<Long>>();
        this.unsignaledTriggers = new HashSet<Long>();
    }

    public MultiConditionHashMapBackend(MultiConditionHashMapBackend database) {
        this();
        this.map = database.getMap();
    }

    public void put(String key, byte[] value) {
        this.lock.writeLock().lock();
        try {
            while (this.unsignaledTriggers.size() > 0)
                this.triggersDoneCondition.awaitUninterruptibly();

            this.map.put(key, value.clone());

            Set<Long> keyTriggers = this.waitingTriggers.get(key);
            if (keyTriggers != null) {
                this.unsignaledTriggers.addAll(keyTriggers);
                this.databaseChangedConditions.get(key).signalAll();
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public byte[] get(String key) {
        this.lock.readLock().lock();
        try {
            byte[] value = this.map.get(key);
            if (value != null)
                value = value.clone();
            return value;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        this.lock.writeLock().lock();
        try {
            while (this.unsignaledTriggers.size() > 0)
                this.triggersDoneCondition.awaitUninterruptibly();

            for (Map.Entry<String, byte[]> pair : pairs.entrySet())
                this.map.put(pair.getKey(), pair.getValue().clone());

            for (String key : pairs.keySet()) {
                Set<Long> keyTriggers = this.waitingTriggers.get(key);
                if (keyTriggers != null) {
                    this.unsignaledTriggers.addAll(keyTriggers);
                    this.databaseChangedConditions.get(key).signalAll();
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        this.lock.readLock().lock();
        try {
            return keys.stream().collect(Collectors.toMap(k -> k, k -> {
                byte[] value = this.map.get(k);
                if (value != null)
                    value = value.clone();
                return value;
            }));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) {
        long threadId = Thread.currentThread().getId();

        this.lock.writeLock().lock();
        try {
            Set<Long> keyWaitingThreads = this.waitingTriggers.get(keyCond);
            if (keyWaitingThreads == null) {
                keyWaitingThreads = new HashSet<Long>();
                keyWaitingThreads.add(threadId);

                this.waitingTriggers.put(keyCond, keyWaitingThreads);
                this.databaseChangedConditions.put(keyCond, this.lock.writeLock().newCondition());
            } else {
                keyWaitingThreads.add(threadId);
            }

            Condition waitCondition = this.databaseChangedConditions.get(keyCond);
            while (!Arrays.equals(this.map.get(keyCond), valueCond)) {
                waitCondition.awaitUninterruptibly();

                if (this.unsignaledTriggers.contains(threadId)) {
                    this.unsignaledTriggers.remove(threadId);

                    if (this.unsignaledTriggers.size() == 0)
                        this.triggersDoneCondition.signalAll();
                }
            }

            keyWaitingThreads.remove(threadId);
            if (keyWaitingThreads.size() == 0) {
                this.databaseChangedConditions.remove(keyCond);
                this.waitingTriggers.remove(keyCond);
            }

            byte[] value = this.map.get(key);
            if (value != null)
                value = value.clone();
            return value;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private Map<String, byte[]> getMap() {
        this.lock.readLock().lock();
        try {
            return this.map.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone()));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Object clone() {
        return new MultiConditionHashMapBackend(this);
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        MultiConditionHashMapBackend backend = (MultiConditionHashMapBackend) o;
        this.lock.readLock().lock();
        try {
            return this.map.equals(backend.getMap());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String toString() {
        this.lock.readLock().lock();
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("SimpleHashMapBackend({");

            boolean isFirst = true;
            for (Map.Entry<String, byte[]> entry : this.map.entrySet()) {
                if (isFirst)
                    isFirst = false;
                else
                    builder.append(", ");

                builder.append(entry.getKey());
                builder.append(": ");
                builder.append(Arrays.toString(entry.getValue()));
            }

            builder.append("})");
            return builder.toString();
        } finally {
            this.lock.readLock().unlock();
        }
    }
}
