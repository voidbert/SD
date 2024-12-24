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

public class DatabaseServer {
    private int        port;
    private int        maxConnections;
    private ThreadPool threadPool;
    private KeyValueDB backend;

    public DatabaseServer(int port, int maxConnections, KeyValueDB backend) {
        this.port           = port;
        this.maxConnections = maxConnections;
        this.threadPool     = new ThreadPool();
        this.backend        = backend;
    }

    public void run() throws IOException {
        ServerSocket serverSocket = new ServerSocket(this.port);
        while (true) {
            Socket socket = serverSocket.accept();

            Buffer sendBuffer = new Buffer();

            Thread readThread = new Thread(() -> {
                try {
                    this.connectionReadLoop(socket, sendBuffer);
                } catch (IOException e) {
                    if (!(e instanceof EOFException))
                        System.err.println(e.getMessage());
                }

                sendBuffer.shutdown();
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

    private void connectionReadLoop(Socket socket, Buffer sendBuffer) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        // TODO - handle login here

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
        } catch (BufferException e) {} // Can only happen if we shutdown the buffer
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
