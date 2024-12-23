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

public class GetResponseMessage extends Message implements ResponseMessage {
    private int    requestId;
    private byte[] value;

    public GetResponseMessage(int requestId, byte[] value) {
        this.requestId = requestId;
        if (value == null)
            this.value = null;
        else
            this.value = value.clone();
    }

    public GetResponseMessage(GetResponseMessage message) {
        this(message.getRequestId(), message.getValue());
    }

    public static GetResponseMessage messageDeserialize(DataInputStream in) throws IOException {
        int requestId = in.readInt();

        byte[] value  = null;
        int    length = in.readInt();
        if (length > 0) {
            value = new byte[length];
            in.readFully(value);
        }

        return new GetResponseMessage(requestId, value);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(this.requestId);

        if (this.value == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(this.value.length);
            out.write(this.value);
        }
    }

    public int getRequestId() {
        return this.requestId;
    }

    public byte[] getValue() {
        if (this.value == null)
            return null;
        return this.value.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        GetResponseMessage message = (GetResponseMessage) o;
        return this.requestId == message.getRequestId() &&
            Arrays.equals(this.value, message.getValue());
    }

    @Override
    public Object clone() {
        return new GetResponseMessage(this);
    }

    @Override
    public String toString() {
        return String.format("GetResponseMessage(requestId=%d, value=%s)",
                             this.requestId,
                             Arrays.toString(this.value));
    }
}
