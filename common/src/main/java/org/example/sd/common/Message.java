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

public abstract class Message {
    public static Message deserialize(DataInputStream in) throws IOException {
        // TODO - implement Message.deserialize
        throw new UnsupportedOperationException();
    }

    public void serialize(DataOutputStream out) throws IOException {
        // TODO - implement Message.serialize
        throw new UnsupportedOperationException();
    }

    protected abstract void messageSerialize(DataOutputStream out) throws IOException;
}
