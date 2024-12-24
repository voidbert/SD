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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import org.example.sd.common.Buffer;
import org.example.sd.common.BufferException;
import org.example.sd.common.GetRequestMessage;
import org.example.sd.common.GetResponseMessage;
import org.example.sd.common.GetWhenRequestMessage;
import org.example.sd.common.KeyValueDB;
import org.example.sd.common.Message;
import org.example.sd.common.MultiGetRequestMessage;
import org.example.sd.common.MultiGetResponseMessage;
import org.example.sd.common.MultiPutRequestMessage;
import org.example.sd.common.PutRequestMessage;
import org.example.sd.common.PutResponseMessage;
import org.example.sd.common.RegisterAuthenticateRequestMessage;
import org.example.sd.common.RegisterAuthenticateResponseMessage;
import org.example.sd.common.RegistrationAuthenticationStatus;

public class DatabaseServer {
    private int            port;
    private SessionManager sessions;
    private ThreadPool     threadPool;
    private KeyValueDB     backend;

    public DatabaseServer(int port, int maxConnections, KeyValueDB backend) {
        this.port       = port;
        this.sessions   = new SessionManager(maxConnections);
        this.threadPool = new ThreadPool();
        this.backend    = backend;
    }

    public void run() throws IOException {
        ServerSocket serverSocket = new ServerSocket(this.port);
        while (true) {
            Socket socket     = serverSocket.accept();
            Buffer sendBuffer = new Buffer();

            Thread readThread = new Thread(() -> {
                String[] username = new String[1];
                try {
                    this.connectionReadLoop(socket, sendBuffer, username);
                } catch (IOException e) {
                    if (!(e instanceof EOFException))
                        System.err.println(e.getMessage());
                }

                sendBuffer.shutdown();
                if (username[0] != null)
                    this.sessions.releaseSession(username[0]);
            });

            Thread writeThread = new Thread(() -> {
                try {
                    this.connectionWriteLoop(socket, sendBuffer);
                } catch (EOFException e) {
                    return;
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            });

            readThread.start();
            writeThread.start();
        }
    }

    private void connectionReadLoop(Socket socket, Buffer sendBuffer, String[] username)
        throws IOException {

        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        // Handle logins
        boolean stop = false;
        while (!stop) {
            Message message = Message.deserialize(in);
            if (message instanceof RegisterAuthenticateRequestMessage) {
                RegisterAuthenticateRequestMessage castedMessage =
                    (RegisterAuthenticateRequestMessage) message;
                RegistrationAuthenticationStatus status;

                try {
                    boolean newUser = this.sessions.acquireSession(castedMessage.getUsername(),
                                                                   castedMessage.getPassword());

                    stop        = true;
                    username[0] = castedMessage.getUsername();
                    if (newUser)
                        status = RegistrationAuthenticationStatus.SUCCESS_NEW_USER;
                    else
                        status = RegistrationAuthenticationStatus.SUCCESS;
                } catch (SessionException e) {
                    if (e.getMessage().contains("authenticated"))
                        status = RegistrationAuthenticationStatus.EXISTING_LOGIN;
                    else
                        status = RegistrationAuthenticationStatus.WRONG_CREDENTIALS;
                }

                try {
                    sendBuffer.send(new RegisterAuthenticateResponseMessage(status));
                } catch (BufferException e) {} // Unreachable
            } else {
                System.err.printf("Invalid message received: %s\n",
                                  message.getClass().getSimpleName());
            }
        }

        // Handle database requests
        while (true) {
            Message message = Message.deserialize(in);
            this.threadPool.addTask(() -> executeMessage(message, sendBuffer));
        }
    }

    private void executeMessage(Message message, Buffer sendBuffer) {
        Message replyMessage;

        if (message instanceof PutRequestMessage) {
            PutRequestMessage castedMessage = (PutRequestMessage) message;
            this.backend.put(castedMessage.getKey(), castedMessage.getValue());
            replyMessage = new PutResponseMessage(castedMessage.getId());

        } else if (message instanceof GetRequestMessage) {
            GetRequestMessage castedMessage = (GetRequestMessage) message;
            byte[]            value         = this.backend.get(castedMessage.getKey());
            replyMessage                    = new GetResponseMessage(castedMessage.getId(), value);

        } else if (message instanceof MultiPutRequestMessage) {
            MultiPutRequestMessage castedMessage = (MultiPutRequestMessage) message;
            this.backend.multiPut(castedMessage.getMap());
            replyMessage = new PutResponseMessage(castedMessage.getId());

        } else if (message instanceof MultiGetRequestMessage) {
            MultiGetRequestMessage castedMessage = (MultiGetRequestMessage) message;
            Map<String, byte[]>    map           = this.backend.multiGet(castedMessage.getKeys());
            replyMessage = new MultiGetResponseMessage(castedMessage.getId(), map);

        } else if (message instanceof GetWhenRequestMessage) {
            GetWhenRequestMessage castedMessage = (GetWhenRequestMessage) message;
            byte[]                value         = this.backend.getWhen(castedMessage.getKey(),
                                                castedMessage.getKeyCond(),
                                                castedMessage.getValueCond());
            replyMessage = new GetResponseMessage(castedMessage.getId(), value);

        } else {
            System.err.printf("Invalid message received: %s\n", message.getClass().getSimpleName());
            return;
        }

        try {
            sendBuffer.send(replyMessage);
        } catch (BufferException e) {} // Unreachable
    }

    private void connectionWriteLoop(Socket socket, Buffer sendBuffer) throws IOException {
        DataOutputStream out =
            new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        while (true) {
            try {
                Message message = sendBuffer.receive();
                message.serialize(out);
                out.flush();
            } catch (BufferException e) {
                return;
            }
        }
    }

    public Object clone() {
        // It's impossible to clone a connection
        return this;
    }

    public boolean equals(Object o) {
        // No two connections different connections can be the same
        return this == o;
    }

    public String toString() {
        return String.format("DatabaseServer(backend=%s, port=%d)",
                             ((Object) this.backend).getClass().getSimpleName(),
                             this.port);
    }
}
