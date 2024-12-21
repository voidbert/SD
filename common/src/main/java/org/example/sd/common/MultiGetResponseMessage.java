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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MultiGetResponseMessage extends Message {
    private int                 requestId;
    private Map<String, byte[]> map;

    public MultiGetResponseMessage(int requestId, Map<String, byte[]> map) {
        this.requestId = requestId;
        this.map       = map;
    }

    public static MultiGetResponseMessage messageDeserialize(DataInputStream in) {
        int                 requestId = in.readInt();
        int                 mapSize   = in.readInt();
        Map<String, byte[]> map       = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            String key         = in.readUTF();
            int    valueLength = in.readInt();
            byte[] value       = new byte[valueLength];
            in.readFully(value);
            map.put(key, value);
        }
        return new MultiGetResponseMessage(requestId, map);
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeInt(requestId);
        out.writeInt(map.size());
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
    }

    public int getRequestId() {
        return this.requestId;
    }

    public Map<String, byte[]> getMap() {
        return this.map;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        MultiGetResponseMessage that = (MultiGetResponseMessage) other;
        return requestId == that.requestId && map.equals(that.map);
    }

    @Override
    public Object clone() {
        try {
            MultiGetResponseMessage cloned = (MultiGetResponseMessage) super.clone();
            cloned.map                     = new HashMap<>(this.map.size());
            for (Map.Entry<String, byte[]> entry : this.map.entrySet()) {
                cloned.map.put(entry.getKey(), entry.getValue().clone());
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        StringBuilder mapString = new StringBuilder();
        mapString.append("{");
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            mapString.append(entry.getKey())
                .append("=")
                .append(Arrays.toString(entry.getValue()))
                .append(", ");
        }
        if (mapString.length() > 1) {
            mapString.setLength(mapString.length() - 2);
        }
        mapString.append("}");

        String str = "MultiGetResponseMessage(RequestId= %d, Map= %s)";
        String res = String.format(str, this.requestId, mapString.toString());
        return res;
    }
}
