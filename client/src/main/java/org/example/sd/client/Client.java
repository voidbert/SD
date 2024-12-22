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

package org.example.sd.client;

import java.util.Scanner;

import org.example.sd.common.KeyValueDB;

public class Client {
    public static void main(String[] args) {
        KeyValueDB    database = null; // TODO - replace with client abstraction
        Scanner       scanner  = new Scanner(System.in);
        CommandRunner runner   = new LoggerCommandRunner(database, "> ");

        System.out.print("> ");
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();

            try {
                runner.parseAndRun(command);
            } catch (CommandException e) {
                System.err.printf("%s\n> ", e.getMessage());
            }
        }
        scanner.close();
    }
}
