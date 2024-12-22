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

public class BidirectionalBuffer {
    // Buffer for sending messages
    private final Queue<Message> sendBuffer   = new ArrayDeque<>();
    private final Lock           sendLock     = new ReentrantLock();
    private final Condition      notEmptySend = sendLock.newCondition();

    // Buffer for receiving messages
    private final Queue<Message> receiveBuffer   = new ArrayDeque<>();
    private final Lock           receiveLock     = new ReentrantLock();
    private final Condition      notEmptyReceive = receiveLock.newCondition();

    private boolean shutdown = false;

    // Adds a message to the send buffer
    public void addToSendBuffer(Message message) {
        sendLock.lock();
        try {
            if (shutdown) {
                throw new IllegalStateException("Buffer is shut down. Cannot add messages.");
            }
            sendBuffer.add(message);
            notEmptySend.signal(); // Notify threads waiting to send
        } finally {
            sendLock.unlock();
        }
    }

    // Removes and returns the next message from the send buffer
    public Message removeFromSendBuffer() throws InterruptedException {
        sendLock.lock();
        try {
            while (sendBuffer.isEmpty() && !shutdown) {
                notEmptySend.await(); // Wait until messages are available
            }
            if (shutdown && sendBuffer.isEmpty()) {
                return null;
            }
            return sendBuffer.poll();
        } finally {
            sendLock.unlock();
        }
    }

    // Adds a message to the receive buffer
    public void addToReceiveBuffer(Message message) {
        receiveLock.lock();
        try {
            if (shutdown) {
                throw new IllegalStateException("Buffer is shut down. Cannot add messages.");
            }
            receiveBuffer.add(message);
            notEmptyReceive.signal(); // Notify threads waiting to receive
        } finally {
            receiveLock.unlock();
        }
    }

    // Removes and returns the next message from the receive buffer
    public Message removeFromReceiveBuffer() throws InterruptedException {
        receiveLock.lock();
        try {
            while (receiveBuffer.isEmpty() && !shutdown) {
                notEmptyReceive.await(); // Wait until messages are available
            }
            if (shutdown && receiveBuffer.isEmpty()) {
                return null; // If shutdown, return null to indicate closure
            }
            return receiveBuffer.poll();
        } finally {
            receiveLock.unlock();
        }
    }

    // Shuts down the buffers and notifies all waiting threads
    public void shutdown() {
        sendLock.lock();
        receiveLock.lock();
        try {
            shutdown = true;
            notEmptySend.signalAll();
            notEmptyReceive.signalAll();
        } finally {
            receiveLock.unlock();
            sendLock.unlock();
        }
    }
}
