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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Message {
    private static Map<String, Byte> classToTypeInteger;
    private static Map<Byte, String> typeIntegerToClass;

    static {
        Message.classToTypeInteger =
            Map.ofEntries(Map.entry("RegisterAuthenticateRequestMessage", (byte) 0),
                          Map.entry("RegisterAuthenticateResponseMessage", (byte) 1),

                          Map.entry("PutRequestMessage", (byte) 2),
                          Map.entry("GetRequestMessage", (byte) 3),
                          Map.entry("MultiPutRequestMessage", (byte) 4),
                          Map.entry("MultiGetRequestMessage", (byte) 5),
                          Map.entry("GetWhenRequestMessage", (byte) 6),

                          Map.entry("PutResponseMessage", (byte) 7),
                          Map.entry("GetResponseMessage", (byte) 8),
                          Map.entry("MultiPutResponseMessage", (byte) 9),
                          Map.entry("MultiGetResponseMessage", (byte) 10));

        Message.typeIntegerToClass = Message.classToTypeInteger.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public static Message deserialize(DataInputStream in) throws IOException {
        String messageClassName = Message.typeIntegerToClass.get(in.readByte());
        if (messageClassName == null)
            throw new RuntimeException("Message class not supported");

        try {
            Class<?> messageClass = Class.forName("org.example.sd.common." + messageClassName);
            Method   deserializeMethod =
                messageClass.getMethod("messageDeserialize", DataInputStream.class);
            return (Message) deserializeMethod.invoke(null, in);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void serialize(DataOutputStream out) throws IOException {
        Byte typeInteger = Message.classToTypeInteger.get(this.getClass().getSimpleName());
        if (typeInteger == null)
            throw new RuntimeException("Message class not supported");

        out.writeByte(typeInteger);
        this.messageSerialize(out);
    }

    public abstract Object  clone();
    protected abstract void messageSerialize(DataOutputStream out) throws IOException;
}
