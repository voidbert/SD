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

public class PutResponseMessage extends Message {
    private int    requestId;
    private byte[] value;

    public PutResponseMessage(int requestId, byte[] value) {
        this.requestId = requestId;
        this.value     = value.clone();
    }

    public PutResponseMessage(PutResponseMessage message) {
        this(message.getRequestId(), message.getValue());
    }

    public static PutResponseMessage messageDeserialize(DataInputStream in) throws IOException {
        int    requestId = in.readInt();
        byte[] value     = new byte[in.readInt()];
        in.readFully(value);

        return new PutResponseMessage(requestId, value);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(requestId);
        out.write(value.length);
        out.write(value);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public byte[] getValue() {
        return this.value.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        PutResponseMessage message = (PutResponseMessage) o;
        return this.requestId == message.getRequestId() &&
            Arrays.equals(this.value, message.getValue());
    }

    @Override
    public Object clone() {
        return new PutResponseMessage(this);
    }

    @Override
    public String toString() {
        return String.format("PutResponseMessage(id=%d, value=%s)",
                             this.requestId,
                             Arrays.toString(this.value));
    }
}
