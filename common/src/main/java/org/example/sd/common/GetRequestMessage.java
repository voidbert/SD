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

public class GetRequestMessage extends Message {
    private final int    id;
    private final String key;

    public GetRequestMessage(int id, String key) {
        this.id  = id;
        this.key = key;
    }

    public GetRequestMessage(GetRequestMessage message) {
        this(message.getId(), message.getKey());
    }

    public static GetRequestMessage messageDeserialize(DataInputStream in) throws IOException {
        int    id  = in.readInt();
        String key = in.readUTF();
        return new GetRequestMessage(id, key);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.key);
    }

    public int getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        GetRequestMessage message = (GetRequestMessage) o;
        return this.id == message.getId() && this.key.equals(message.getKey());
    }

    @Override
    public Object clone() {
        return new GetRequestMessage(this);
    }

    @Override
    public String toString() {
        return String.format("GetRequestMessage(id=%d, key=%s)", this.id, this.key);
    }
}
