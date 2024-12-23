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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class DatabaseClient implements KeyValueDB {
    private Socket           socket;
    private DataInputStream  in;
    private DataOutputStream out;
    private boolean          brokenConnection;

    private Lock        lock;
    private Condition[] conditions;

    private int                   nextId;
    private Map<Integer, Message> replies;

    public DatabaseClient(String address, int port, int nConditions) throws IOException {
        this.socket = new Socket(address, port);
        this.in     = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        this.out    = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
        this.brokenConnection = false;

        this.lock       = new ReentrantLock();
        this.conditions = new Condition[nConditions];
        for (int i = 0; i < nConditions; ++i)
            this.conditions[i] = this.lock.newCondition();

        this.nextId  = 1;
        this.replies = new HashMap<Integer, Message>();

        Thread connectionReader = new Thread(() -> connectionReaderThreadLoop());
        connectionReader.setDaemon(true);
        connectionReader.start();
    }

    public void put(String key, byte[] value) {
        this.sendAndWaitForReply(i -> new PutRequestMessage(i, key, value));
    }

    public byte[] get(String key) {
        Message reply = this.sendAndWaitForReply(i -> new GetRequestMessage(i, key));
        if (reply instanceof GetResponseMessage) {
            GetResponseMessage getReply = (GetResponseMessage) reply;
            return getReply.getValue();
        }

        throw new DatabaseClientException("Wrong response type from server");
    }

    public void multiPut(Map<String, byte[]> pairs) {
        this.sendAndWaitForReply(i -> new MultiPutRequestMessage(i, pairs));
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        Message reply = this.sendAndWaitForReply(i -> new MultiGetRequestMessage(i, keys));
        if (reply instanceof MultiGetResponseMessage) {
            MultiGetResponseMessage multiGetReply = (MultiGetResponseMessage) reply;
            return multiGetReply.getMap();
        }

        throw new DatabaseClientException("Wrong response type from server");
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) {
        Message reply =
            this.sendAndWaitForReply(i -> new GetWhenRequestMessage(i, key, keyCond, valueCond));
        if (reply instanceof GetResponseMessage) {
            GetResponseMessage getReply = (GetResponseMessage) reply;
            return getReply.getValue();
        }

        throw new DatabaseClientException("Wrong response type from server");
    }

    private Message sendAndWaitForReply(Function<Integer, Message> createMessage) {
        this.lock.lock();
        try {
            int messageId = this.nextId++;
            createMessage.apply(messageId).serialize(this.out);
            this.out.flush();
            Condition waitCondition = this.conditions[messageId % this.conditions.length];

            Message reply = null;
            while (!this.brokenConnection && (reply = this.replies.get(messageId)) == null)
                waitCondition.awaitUninterruptibly();

            this.replies.remove(messageId);

            if (reply == null)
                throw new DatabaseClientException("Unable to receive response from server");
            return reply;
        } catch (IOException e) {
            this.brokenConnection = true;
            for (Condition c : this.conditions)
                c.signalAll();

            throw new DatabaseClientException(e);
        } finally {
            this.lock.unlock();
        }
    }

    private void connectionReaderThreadLoop() {
        try {
            while (true) {
                Message message = Message.deserialize(this.in);
                if (message instanceof ResponseMessage) {
                    ResponseMessage response  = (ResponseMessage) message;
                    int             requestId = response.getRequestId();

                    this.lock.lock();
                    try {
                        this.replies.put(requestId, message);
                        this.conditions[requestId % this.conditions.length].signalAll();
                    } finally {
                        this.lock.unlock();
                    }
                } else {
                    System.err.println("Received request from server");
                }
            }
        } catch (IOException e) {
            this.brokenConnection = true;

            this.lock.lock();
            try {
                for (Condition c : this.conditions)
                    c.signalAll();
            } finally {
                this.lock.unlock();
            }

            System.err.println("Deserialization thread stopping!");
        }
    }

    public boolean isConnectionBroken() {
        this.lock.lock();
        try {
            return this.brokenConnection;
        } finally {
            this.lock.unlock();
        }
    }

    public Object clone() {
        // There's no way to clone an existing connection
        return this;
    }

    public boolean equals(Object o) {
        // No two connections different connections can be the same
        return this == o;
    }

    public String toString() {
        return String.format("DatabaseClient(%s:%d)",
                             this.socket.getInetAddress().toString(),
                             this.socket.getPort());
    }
}
