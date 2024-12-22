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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

public class MultiPutRequestMessage extends Message {
    private int                 id;
    private Map<String, byte[]> map;

    public MultiPutRequestMessage(int id, Map<String, byte[]> map) {
        this.id  = id;
        this.map = map.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone()));
    }

    public MultiPutRequestMessage(MultiPutRequestMessage message) {
        this(message.getId(), message.getMap());
    }

    public static MultiPutRequestMessage messageDeserialize(DataInputStream in) throws IOException {
        int                 id     = in.readInt();
        int                 length = in.readInt();
        Map<String, byte[]> map    = new HashMap<String, byte[]>();
        for (int i = 0; i < length; i++) {
            String key   = in.readUTF();
            byte[] value = new byte[in.readInt()];
            in.readFully(value);
            map.put(key, value);
        }

        return new MultiPutRequestMessage(id, map);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(id);

        out.writeInt(map.size());
        for (Map.Entry<String, byte[]> entry : this.map.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
    }

    public int getId() {
        return this.id;
    }

    public Map<String, byte[]> getMap() {
        return this.map.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone()));
    }

    private Map<String, List<Byte>> getComparableMap() {
        return this.map.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey,
                             e -> Arrays.asList(ArrayUtils.toObject(e.getValue()))));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        MultiPutRequestMessage message = (MultiPutRequestMessage) o;
        return this.id == message.getId() &&
            this.getComparableMap().equals(message.getComparableMap());
    }

    @Override
    public Object clone() {
        return new MultiPutRequestMessage(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MultiPutRequestMessage(id = ");
        builder.append(this.id);
        builder.append(", map = {");

        boolean isFirst = true;
        for (Map.Entry<String, byte[]> entry : this.map.entrySet()) {
            if (isFirst)
                isFirst = false;
            else
                builder.append(", ");

            builder.append(entry.getKey());
            builder.append(": ");
            builder.append(Arrays.toString(entry.getValue()));
        }

        builder.append("})");
        return builder.toString();
    }
}
