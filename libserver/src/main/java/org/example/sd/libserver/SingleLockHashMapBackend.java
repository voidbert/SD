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

public abstract class SingleLockHashMapBackend implements KeyValueDB {
    protected ReadWriteLock       lock;
    protected Condition           triggersDoneCondition;
    protected Set<Long>           unsignaledTriggers;
    protected Map<String, byte[]> map;

    protected SingleLockHashMapBackend() {
        this.lock                  = new ReentrantReadWriteLock(true);
        this.triggersDoneCondition = this.lock.writeLock().newCondition();
        this.unsignaledTriggers    = new HashSet<Long>();
        this.map                   = new HashMap<>();
    }

    public void put(String key, byte[] value) {
        this.lock.writeLock().lock();
        try {
            while (this.unsignaledTriggers.size() > 0)
                this.triggersDoneCondition.awaitUninterruptibly();

            this.map.put(key, value.clone());
            this.summonTriggersAfterPut(key, value);
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

            this.summonTriggersAfterMultiPut(pairs);
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
        this.lock.writeLock().lock();
        try {
            this.getWhenWait(keyCond, valueCond);

            byte[] value = this.map.get(key);
            if (value != null)
                value = value.clone();
            return value;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    protected Map<String, byte[]> getMap() {
        this.lock.readLock().lock();
        try {
            return this.map.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone()));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Object clone() {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        SingleLockHashMapBackend backend = (SingleLockHashMapBackend) o;
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
            builder.append("{");

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

            builder.append("}");
            return builder.toString();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    protected abstract void summonTriggersAfterPut(String key, byte[] value);
    protected abstract void summonTriggersAfterMultiPut(Map<String, byte[]> pairs);
    protected abstract void getWhenWait(String keyCond, byte[] valueCond);
}
