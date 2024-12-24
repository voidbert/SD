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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.example.sd.common.Buffer;

public class ThreadPool {
    private static final long TIME_BETWEEN_SHRINKS = 30_000_000_000L; // 30 seconds (in ns)

    private Lock                 lock;
    private Condition            condition;
    private ArrayDeque<Runnable> buffer;
    private Map<Long, Thread>    threads;
    private Set<Long>            killedThreads;
    private int                  usedThreads;
    private long                 lastShrink;

    public ThreadPool() {
        this.lock          = new ReentrantLock();
        this.condition     = this.lock.newCondition();
        this.buffer        = new ArrayDeque<Runnable>();
        this.threads       = new HashMap<Long, Thread>();
        this.killedThreads = new HashSet<Long>();
        this.usedThreads   = 0;
        this.lastShrink    = 0;
    }

    public void addTask(Runnable r) {
        this.lock.lock();
        try {
            if (this.usedThreads >= this.threads.size()) {
                Thread thread = new Thread(() -> this.threadLoop());
                thread.setDaemon(true);
                thread.start();
                this.threads.put(thread.threadId(), thread);
            }

            this.buffer.add(r);
            this.usedThreads++;
            if (this.buffer.size() == 1)
                this.condition.signal();
        } finally {
            this.lock.unlock();
        }
    }

    private void threadLoop() {
        long    threadId           = Thread.currentThread().threadId();
        boolean processedOperation = false;

        while (true) {
            Runnable runnable;

            // Get task to run
            this.lock.lock();
            try {
                if (processedOperation)
                    this.usedThreads--;

                while (!this.killedThreads.contains(threadId) && this.buffer.isEmpty()) {
                    if (this.usedThreads < this.threads.size() / 2 && this.threads.size() == 0) {
                        try {
                            this.condition.awaitNanos(ThreadPool.TIME_BETWEEN_SHRINKS);
                            this.shrinkIfNeeded();
                        } catch (InterruptedException e) {}
                    } else {
                        this.condition.awaitUninterruptibly();
                    }
                }

                if (this.killedThreads.contains(threadId)) {
                    this.killedThreads.remove(threadId);
                    return;
                }

                runnable           = this.buffer.poll();
                processedOperation = false;
            } finally {
                this.lock.unlock();
            }

            // Run task
            try {
                if (runnable != null) {
                    processedOperation = true;
                    runnable.run();
                }
            } catch (Exception e) {
                // Don't let a bad task crash the pool's thread
                System.err.printf("Threadpool exception: %s\n", e.getMessage());
            }
        }
    }

    private void shrinkIfNeeded() {
        long time = System.nanoTime();
        int  half = this.threads.size() / 2;
        if (time - this.lastShrink >= ThreadPool.TIME_BETWEEN_SHRINKS && this.usedThreads < half) {
            int              i  = 0;
            Iterator<Thread> it = this.threads.values().iterator();
            while (it.hasNext() && i < half) {
                this.killedThreads.add(it.next().threadId());
                it.remove();
                i++;
            }

            this.lastShrink = time;
            this.condition.signalAll();
        }
    }

    @Override
    public String toString() {
        return String.format("ThreadPool(nThreads=%d)", this.threads.size());
    }
}
