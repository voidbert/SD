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

public class GetRequestMessage extends Message {
    private int    id;
    private String key;

    public GetRequestMessage(int id, String key) {
        this.id  = id;
        this.key = key;
    }

    public static GetRequestMessage messageDeserialize(DataInputStream in) {
        int    id  = in.readInt();
        String key = in.readUTF();
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeInt(id);
        out.writeUTF(key);
    }

    public int getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if ((other == null) || (this.getClass() != other.getClass()))
            return false;
        GetRequestMessage that = (GetRequestMessage) other;
        return this.id == that.id && this.key.equals(that.key);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        String str = "GetRequestMessage(Id= %d, Key= %s)";
        String res = String.format(str, this.id, this.key);
        return res;
    }
}
