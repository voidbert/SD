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

import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SessionManager {
    private final int maxSessions;
    private final Queue<Socket> waitingQueue;
    private final Map<String, Socket> activeClients;
    private final ReentrantLock       lock;
    private final Condition           sessionAvailable;

    public SessionManager(int maxSessions) {
        this.maxSessions      = maxSessions;
        this.waitingQueue     = new LinkedList<>();
        this.activeClients    = new HashMap<>();
        this.lock             = new ReentrantLock();
        this.sessionAvailable = lock.newCondition();
    }

    public Socket acquireSession(Socket clientSocket, String username)
        throws InterruptedException, SessionException {
        lock.lock();
        try {
            if (activeClients.containsKey(username)) {
                throw new SessionException("User '" + username + "' is already authenticated.");
            }

            while (activeClients.size() >= maxSessions) {
                if (!waitingQueue.contains(clientSocket)) {
                    waitingQueue.add(clientSocket);
                }
                sessionAvailable.awaitUninterruptibly();
            }

            waitingQueue.remove(clientSocket);
            activeClients.put(username, clientSocket);
            return clientSocket;
        } finally {
            lock.unlock();
        }
    }

    public void releaseSession(String username) {
        lock.lock();
        try {
            if (activeClients.containsKey(username)) {
                activeClients.remove(username);

                if (!waitingQueue.isEmpty()) {
                    sessionAvailable.signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public int getActiveSessions() {
        lock.lock();
        try {
            return activeClients.size();
        } finally {
            lock.unlock();
        }
    }
}

class SessionException extends Exception {
    public SessionException(String message) {
        super(message);
    }
}
