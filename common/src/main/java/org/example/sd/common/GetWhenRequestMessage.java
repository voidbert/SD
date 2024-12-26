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

public class GetWhenRequestMessage extends Message {
    private final int    id;
    private final String key;
    private final String keyCond;
    private final byte[] valueCond;

    public GetWhenRequestMessage(int id, String key, String keyCond, byte[] valueCond) {
        this.id        = id;
        this.key       = key;
        this.keyCond   = keyCond;
        this.valueCond = valueCond.clone();
    }

    public GetWhenRequestMessage(GetWhenRequestMessage message) {
        this(message.getId(), message.getKey(), message.getKeyCond(), message.getValueCond());
    }

    public static GetWhenRequestMessage messageDeserialize(DataInputStream in) throws IOException {
        int    id      = in.readInt();
        String key     = in.readUTF();
        String keyCond = in.readUTF();

        byte[] valueCond = new byte[in.readInt()];
        in.readFully(valueCond);

        return new GetWhenRequestMessage(id, key, keyCond, valueCond);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeUTF(key);
        out.writeUTF(keyCond);
        out.writeInt(valueCond.length);
        out.write(valueCond);
    }

    public int getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    public String getKeyCond() {
        return this.keyCond;
    }

    public byte[] getValueCond() {
        return this.valueCond.clone();
    }

    @Override
    public Object clone() {
        return new GetWhenRequestMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        GetWhenRequestMessage message = (GetWhenRequestMessage) o;
        return this.id == message.getId() && this.key.equals(message.getKey()) &&
            this.keyCond.equals(message.getKeyCond()) &&
            Arrays.equals(this.valueCond, message.getValueCond());
    }

    @Override
    public String toString() {
        return String.format("GetWhenRequestMessage(id=%d, key=%s, keyCond=%s, valueCond=%s)",
                             this.id,
                             this.key,
                             this.keyCond,
                             Arrays.toString(this.valueCond));
    }
}
