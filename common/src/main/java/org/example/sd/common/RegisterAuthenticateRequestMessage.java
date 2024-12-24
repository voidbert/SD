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

public class RegisterAuthenticateRequestMessage extends Message {
    private String username;
    private String password;

    public RegisterAuthenticateRequestMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public RegisterAuthenticateRequestMessage(RegisterAuthenticateRequestMessage message) {
        this(message.getUsername(), message.getPassword());
    }

    public static RegisterAuthenticateRequestMessage messageDeserialize(DataInputStream in)
        throws IOException {

        String username = in.readUTF();
        String password = in.readUTF();
        return new RegisterAuthenticateRequestMessage(username, password);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeUTF(username);
        out.writeUTF(password);
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public Object clone() {
        return new RegisterAuthenticateRequestMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        RegisterAuthenticateRequestMessage message = (RegisterAuthenticateRequestMessage) o;
        return this.username.equals(message.getUsername()) &&
            this.password.equals(message.getPassword());
    }

    @Override
    public String toString() {
        return String.format("RegisterAuthenticateRequestMessage(username=%s, password=%s)",
                             this.username,
                             this.password);
    }
}
