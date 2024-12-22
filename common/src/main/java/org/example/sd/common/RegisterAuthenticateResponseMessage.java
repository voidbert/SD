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

public class RegisterAuthenticateResponseMessage extends Message {
    private RegistrationAuthenticationStatus status;

    public RegisterAuthenticateResponseMessage(RegistrationAuthenticationStatus status) {
        this.status = status;
    }

    public RegisterAuthenticateResponseMessage(RegisterAuthenticateResponseMessage message) {
        this(message.getStatus());
    }

    public static RegisterAuthenticateResponseMessage messageDeserialize(DataInputStream in)
        throws IOException {

        RegistrationAuthenticationStatus status =
            RegistrationAuthenticationStatus.fromInt(in.readInt());
        return new RegisterAuthenticateResponseMessage(status);
    }

    protected void messageSerialize(DataOutputStream out) throws IOException {
        out.writeInt(this.status.toInt());
    }

    public RegistrationAuthenticationStatus getStatus() {
        return this.status;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        RegisterAuthenticateResponseMessage message = (RegisterAuthenticateResponseMessage) o;
        return this.status.equals(message.getStatus());
    }

    @Override
    public Object clone() {
        return new RegisterAuthenticateResponseMessage(this);
    }

    @Override
    public String toString() {
        return String.format("RegisterAuthenticateResponseMessage(status=%s)",
                             this.status.toString());
    }
}
