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
    private final int maxSessions;                // Maximum number of active sessions
    private int       activeSessions         = 0; // Current active session count
    private final Queue<Socket> waitingQueue = new LinkedList<>(); // Queue for waiting clients
    private final Map<String, Socket> activeClients =
        new HashMap<>();                                           // Active authenticated clients
    private final ReentrantLock lock = new ReentrantLock();        // Lock for synchronization
    private final Condition     sessionAvailable =
        lock.newCondition(); // Condition to wait for available sessions

    public SessionManager(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public Socket acquireSession(Socket clientSocket, String username) throws InterruptedException {
        lock.lock();
        try {
            // Check if the client is already authenticated
            if (activeClients.containsKey(username)) {
                System.out.println("User already authenticated: " + username);
                return null;
            }

            // If no sessions are available, add the client to the waiting queue and wait
            while (activeSessions >= maxSessions) {
                System.out.println("No sessions available. Adding " + username +
                                   " to the waiting queue.");
                if (!waitingQueue.contains(clientSocket)) {
                    waitingQueue.add(clientSocket);
                }
                try {
                    sessionAvailable.await();
                } catch (InterruptedException e) {
                    waitingQueue.remove(clientSocket); // Ensure client is removed on interruption
                    System.out.println("Thread interrupted. Removed client " + username +
                                       " from the waiting queue.");
                    throw e;
                }
            }

            // Remove the client from the waiting queue and add to active sessions
            waitingQueue.remove(clientSocket);
            activeSessions++;
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
                activeSessions--;

                // Notify waiting clients if the queue is not empty
                if (!waitingQueue.isEmpty()) {
                    System.out.println("Releasing session and notifying the next client in queue.");
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
            return activeSessions;
        } finally {
            lock.unlock();
        }
    }
}
