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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SessionManager {
    private final int           maxSessions;
    private Map<String, String> passwords;
    private Queue<String>       waitingQueue;
    private Set<String>         activeClients;
    private final ReentrantLock lock;
    private final Condition     sessionAvailable;

    public SessionManager(int maxSessions) {
        this.maxSessions      = maxSessions;
        this.passwords        = new HashMap<String, String>();
        this.waitingQueue     = new LinkedList<String>();
        this.activeClients    = new HashSet<String>();
        this.lock             = new ReentrantLock();
        this.sessionAvailable = lock.newCondition();
    }

    public SessionManager(SessionManager manager) {
        this(manager.getMaxSessions());
        this.passwords = manager.getPasswords();
    }

    public boolean acquireSession(String username, String password) throws SessionException {
        boolean newUser = false;

        this.lock.lock();
        try {
            if (this.activeClients.contains(username))
                throw new SessionException("User '" + username + "' is already authenticated.");

            String truePassword = this.passwords.get(username);
            if (truePassword == null) {
                this.passwords.put(username, password);
                newUser = true;
            } else if (!truePassword.equals(password)) {
                throw new SessionException("Wrong password");
            }

            if (!this.waitingQueue.contains(username))
                this.waitingQueue.add(username);

            while (this.activeClients.size() >= this.maxSessions ||
                   !username.equals(this.waitingQueue.peek())) {

                this.sessionAvailable.awaitUninterruptibly();
            }

            this.waitingQueue.poll();
            this.activeClients.add(username);
            return newUser;
        } finally {
            this.lock.unlock();
        }
    }

    public void releaseSession(String username) {
        this.lock.lock();
        try {
            if (this.activeClients.contains(username)) {
                this.activeClients.remove(username);

                if (!this.waitingQueue.isEmpty())
                    this.sessionAvailable.signalAll();
            }
        } finally {
            this.lock.unlock();
        }
    }

    public int getMaxSessions() {
        return this.maxSessions;
    }

    public Map<String, String> getPasswords() {
        this.lock.lock();
        try {
            return new HashMap<String, String>(this.passwords);
        } finally {
            this.lock.unlock();
        }
    }

    public Object clone() {
        return new SessionManager(this);
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        SessionManager sessions = (SessionManager) o;
        return this.maxSessions == sessions.getMaxSessions() &&
            this.passwords.equals(sessions.getPasswords());
    }

    public String toString() {
        return String.format("SessionManager(maxSessions=%d, passwords=%s)",
                             this.maxSessions,
                             this.passwords.toString());
    }
}
