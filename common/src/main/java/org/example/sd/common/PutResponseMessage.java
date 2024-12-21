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

public class PutResponseMessage extends Message {
    private int    requestId;
    private byte[] value;

    public PutResponseMessage(int requestId, byte[] value) {
        this.requestId = requestId;
        this.value     = value;
    }

    public static PutResponseMessage messageDeserialize(DataInputStream in) {
        int    requestId = in.readInt();
        byte[] value     = in.readAllBytes();
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeInt(requestId);
        out.write(value);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public byte[] getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if ((other == null) || (this.getClass() != other.getClass()))
            return false;
        PutResponseMessage that = (PutResponseMessage) other;
        return this.requestId == that.requestId && this.value.equals(that.value);
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
        String str = "PutResponseMessage(Id= %d, Value= %s)";
        String res = String.format(str, this.requestId, Arrays.toString(this.value));
        return res;
    }
}
