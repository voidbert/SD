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

public enum RegistrationAuthenticationStatus {
    SUCCESS,
    SUCCESS_NEW_USER,
    WRONG_CREDENTIALS,
    EXISTING_LOGIN;

    public int toInt() {
        switch (this) {
            case SUCCESS:
                return 0;
            case SUCCESS_NEW_USER:
                return 1;
            case WRONG_CREDENTIALS:
                return 2;
            case EXISTING_LOGIN:
                return 3;
            default:
                return -1; // Unreachable
        }
    }

    public static RegistrationAuthenticationStatus fromInt(int i) {
        switch (i) {
            case 0:
                return SUCCESS;
            case 1:
                return SUCCESS_NEW_USER;
            case 2:
                return WRONG_CREDENTIALS;
            case 3:
                return EXISTING_LOGIN;
            default:
                return null;
        }
    }
}
