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

import java.io.IOException;
import java.util.Scanner;

import org.example.sd.common.DatabaseClient;
import org.example.sd.common.KeyValueDB;

public class Client {
    public static final int N_CONDITIONS = 32;

    public static void main(String[] args) {
        String address = "";
        int    port    = 0;
        try {
            if (args.length != 1)
                throw new Exception();

            String[] addressPort = args[0].split(":");
            if (addressPort.length != 2)
                throw new Exception();

            address = addressPort[0];
            port    = Integer.valueOf(addressPort[1]);
        } catch (Exception e) {
            System.err.println("Usage: gradle :client:run --args <address>:<port>");
            System.exit(1);
        }

        DatabaseClient database = null;
        try {
            database = new DatabaseClient(address, port, Client.N_CONDITIONS);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        Scanner       scanner = new Scanner(System.in);
        CommandRunner runner  = new LoggerCommandRunner(database, "> ");

        System.out.print("> ");
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();

            if (database.isConnectionBroken())
                System.exit(1); // Something should have been printed by now

            try {
                runner.parseAndRun(command);
            } catch (CommandException e) {
                System.err.printf("%s\n> ", e.getMessage());
            }
        }
        scanner.close();
    }
}
