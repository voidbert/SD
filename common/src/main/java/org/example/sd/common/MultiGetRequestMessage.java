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
import java.util.HashSet;
import java.util.Set;

public class MultiGetRequestMessage extends Message {
    private final int id;
    private final Set<String> keys;

    public MultiGetRequestMessage(int id, Set<String> keys) {
        this.id   = id;
        this.keys = new HashSet(keys);
    }

    public MultiGetRequestMessage(MultiGetRequestMessage message) {
        this(message.getId(), message.getKeys());
    }

    public static MultiGetRequestMessage messageDeserialize(DataInputStream in) throws IOException {
        int         id     = in.readInt();
        int         length = in.readInt();
        Set<String> keys   = new HashSet<String>();
        for (int i = 0; i < length; i++)
            keys.add(in.readUTF());

        return new MultiGetRequestMessage(id, keys);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeInt(keys.size());
        for (String key : keys)
            out.writeUTF(key);
    }

    public int getId() {
        return this.id;
    }

    public Set<String> getKeys() {
        return new HashSet(keys);
    }

    @Override
    public Object clone() {
        return new MultiGetRequestMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass())
            return false;

        MultiGetRequestMessage message = (MultiGetRequestMessage) o;
        return this.id == message.getId() && this.keys.equals(message.getKeys());
    }

    @Override
    public String toString() {
        return String.format("MultiGetRequestMessage(id=%d, keys=%s)",
                             this.id,
                             this.keys.toString());
    }
}
