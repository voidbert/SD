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
import java.util.Arrays;

public class PutRequestMessage extends Message {
    private int    id;
    private String key;
    private byte[] value;

    public PutRequestMessage(int id, String key, byte[] value) {
        this.id    = id;
        this.key   = key;
        this.value = value.clone();
    }

    public PutRequestMessage(PutRequestMessage message) {
        this(message.getId(), message.getKey(), message.getValue());
    }

    public static PutRequestMessage messageDeserialize(DataInputStream in) throws IOException {
        int    id    = in.readInt();
        String key   = in.readUTF();
        byte[] value = new byte[in.readInt()];
        in.readFully(value);

        return new PutRequestMessage(id, key, value);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeUTF(key);
        out.writeInt(value.length);
        out.write(value);
    }

    public int getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    public byte[] getValue() {
        return this.value.clone();
    }

    @Override
    public Object clone() {
        return new PutRequestMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        PutRequestMessage message = (PutRequestMessage) o;
        return this.id == message.getId() && this.key.equals(message.getValue()) &&
            Arrays.equals(this.value, message.getValue());
    }

    @Override
    public String toString() {
        return String.format("PutRequestMessage(id=%d, key=%s, value=%s)",
                             this.id,
                             this.key,
                             Arrays.toString(this.value));
    }
}
