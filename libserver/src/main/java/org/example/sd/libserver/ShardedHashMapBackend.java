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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.example.sd.common.KeyValueDB;

public class ShardedHashMapBackend implements KeyValueDB {
    private final int             nShards;
    private Map<String, byte[]>[] shards;
    private ReadWriteLock[]       locks;

    public ShardedHashMapBackend(int nShards) {
        this.nShards = nShards;
        this.shards  = (Map<String, byte[]>[]) new Map[nShards];
        this.locks   = new ReadWriteLock[nShards];

        for (int i = 0; i < nShards; ++i) {
            this.shards[i] = new HashMap<String, byte[]>();
            this.locks[i]  = new ReentrantReadWriteLock(true);
        }
    }

    public ShardedHashMapBackend(ShardedHashMapBackend database) {
        this(database.getNShards());
        this.shards = database.getShards();
    }

    public void put(String key, byte[] value) {
        int shard = Math.abs(key.hashCode()) % this.nShards;

        this.locks[shard].writeLock().lock();
        try {
            this.shards[shard].put(key, value.clone());
        } finally {
            this.locks[shard].writeLock().unlock();
        }
    }

    public byte[] get(String key) {
        int shard = Math.abs(key.hashCode()) % this.nShards;

        this.locks[shard].readLock().lock();
        try {
            byte[] value = this.shards[shard].get(key);
            if (value != null)
                value = value.clone();
            return value;
        } finally {
            this.locks[shard].readLock().unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        Map<Integer, List<String>> shardKeys = this.associateKeysToShards(pairs.keySet());

        int acquiredLocksCount = 0;
        try {
            for (Map.Entry<Integer, List<String>> entry : shardKeys.entrySet()) {
                int shard = entry.getKey();
                this.locks[shard].writeLock().lock();
                acquiredLocksCount++;

                List<String> currentShardKeys = entry.getValue();
                for (String key : currentShardKeys)
                    this.shards[shard].put(key, pairs.get(key).clone());
            }
        } finally {
            Iterator<Integer> i = shardKeys.keySet().iterator();
            while (i.hasNext() && acquiredLocksCount > 0) {
                this.locks[i.next()].writeLock().unlock();
                acquiredLocksCount--;
            }
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        Map<Integer, List<String>> shardKeys = this.associateKeysToShards(keys);
        Map<String, byte[]>        ret       = new HashMap<String, byte[]>();

        int acquiredLocksCount = 0;
        try {
            for (Map.Entry<Integer, List<String>> entry : shardKeys.entrySet()) {
                int shard = entry.getKey();
                this.locks[shard].writeLock().lock();
                acquiredLocksCount++;

                List<String> currentShardKeys = entry.getValue();
                for (String key : currentShardKeys) {
                    byte[] value = this.shards[shard].get(key);
                    if (value != null)
                        ret.put(key, value);
                }
            }

            return ret;
        } finally {
            Iterator<Integer> i = shardKeys.keySet().iterator();
            while (i.hasNext() && acquiredLocksCount > 0) {
                this.locks[i.next()].writeLock().unlock();
                acquiredLocksCount--;
            }
        }
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) {
        throw new UnsupportedOperationException("Choose another backend");
    }

    private Map<Integer, List<String>> associateKeysToShards(Set<String> keys) {
        Map<Integer, List<String>> shardKeys = new TreeMap<Integer, List<String>>();
        for (String key : keys) {
            int shard = Math.abs(key.hashCode()) % this.nShards;

            List<String> currentKeyList = shardKeys.get(shard);
            if (currentKeyList == null) {
                currentKeyList = new ArrayList<String>();
                shardKeys.put(shard, currentKeyList);
            }
            currentKeyList.add(key);
        }

        return shardKeys;
    }

    public int getNShards() {
        return this.nShards;
    }

    private Map<String, byte[]>[] getShards() {
        Map<String, byte[]>[] ret = (Map<String, byte[]>[]) new Map[this.nShards];

        int acquiredLocksCount = 0;
        try {
            for (int i = 0; i < this.nShards; ++i) {
                this.locks[i].readLock().lock();
                acquiredLocksCount++;

                ret[i] = this.shards[i].entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone()));
            }
        } finally {
            for (int i = 0; i < acquiredLocksCount; ++i)
                this.locks[i].readLock().unlock();
        }

        return ret;
    }

    public Object clone() {
        return new ShardedHashMapBackend(this);
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        ShardedHashMapBackend backend     = (ShardedHashMapBackend) o;
        Map<String, byte[]>[] otherShards = backend.getShards();

        if (this.nShards != backend.getNShards())
            return false;

        int acquiredLocksCount = 0;
        try {
            for (int i = 0; i < this.nShards; ++i) {
                this.locks[i].readLock().lock();
                acquiredLocksCount++;

                if (!this.shards[i].equals(otherShards[i]))
                    return false;
            }
        } finally {
            for (int i = 0; i < acquiredLocksCount; ++i)
                this.locks[i].readLock().unlock();
        }

        return true;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        boolean isFirst            = true;
        int     acquiredLocksCount = 0;
        try {
            for (int i = 0; i < this.nShards; ++i) {
                this.locks[i].readLock().lock();
                acquiredLocksCount++;

                for (Map.Entry<String, byte[]> entry : this.shards[i].entrySet()) {
                    if (isFirst)
                        isFirst = false;
                    else
                        builder.append(", ");

                    builder.append(entry.getKey());
                    builder.append(": ");
                    builder.append(Arrays.toString(entry.getValue()));
                }
            }
        } finally {
            for (int i = 0; i < acquiredLocksCount; ++i)
                this.locks[i].readLock().unlock();
        }

        builder.append("}");
        return builder.toString();
    }
}
