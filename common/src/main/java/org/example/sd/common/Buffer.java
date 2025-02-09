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

package org.example.sd.common;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Buffer {
    private Queue<Message>  buffer;
    private final Lock      lock;
    private final Condition condition;
    private boolean         isShutdown;

    public Buffer() {
        this.buffer     = new ArrayDeque<Message>();
        this.lock       = new ReentrantLock();
        this.condition  = this.lock.newCondition();
        this.isShutdown = false;
    }

    public Buffer(Buffer buffer) {
        this();
        this.buffer = buffer.getBuffer();
    }

    public void send(Message message) throws BufferException {
        this.lock.lock();
        try {
            if (this.isShutdown)
                throw new BufferException("Buffer was shutdown");

            if (this.buffer.isEmpty()) {
                this.buffer.add(message);
                this.condition.signal();
            } else {
                this.buffer.add(message);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public Message receive() throws BufferException {
        this.lock.lock();
        try {
            while (!this.isShutdown && this.buffer.isEmpty())
                this.condition.awaitUninterruptibly();

            if (this.isShutdown)
                throw new BufferException("Buffer was shutdown");

            return this.buffer.poll();
        } finally {
            this.lock.unlock();
        }
    }

    public void shutdown() {
        this.lock.lock();
        try {
            if (this.isShutdown)
                return;

            this.isShutdown = true;
            this.condition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    public Queue<Message> getBuffer() {
        this.lock.lock();
        try {
            return new ArrayDeque<Message>(
                this.buffer.stream().map(m -> (Message) m.clone()).collect(Collectors.toList()));
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Object clone() {
        return new Buffer(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass())
            return false;

        Buffer buffer = (Buffer) o;
        this.lock.lock();
        try {
            return this.buffer.equals(buffer.getBuffer());
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public String toString() {
        this.lock.lock();
        try {
            return "Buffer(" + this.buffer.toString() + ")";
        } finally {
            this.lock.unlock();
        }
    }
}
