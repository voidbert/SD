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

package org.example.sd.server;

import java.io.IOException;

import org.example.sd.common.KeyValueDB;
import org.example.sd.libserver.DatabaseServer;
import org.example.sd.libserver.MultiConditionHashMapBackend;
import org.example.sd.libserver.ShardedHashMapBackend;
import org.example.sd.libserver.SimpleHashMapBackend;

public class Server {
    public static void main(String[] args) throws IOException {
        // Parse command-line arguments
        int        port           = 0;
        int        maxConnections = 0;
        KeyValueDB backend        = null;
        try {
            port           = Integer.valueOf(args[0]);
            maxConnections = Integer.valueOf(args[1]);

            int argCount = 3;
            switch (args[2].toLowerCase()) {
                case "simplehashmapbackend":
                    backend = new SimpleHashMapBackend();
                    break;
                case "multiconditionhashmapbackend":
                    backend = new MultiConditionHashMapBackend();
                    break;
                case "shardedhashmapbackend":
                    backend = new ShardedHashMapBackend(Integer.valueOf(args[3]));
                    argCount++;
                    break;
                default:
                    throw new Exception();
            }

            if (args.length != argCount)
                throw new Exception();
        } catch (Exception e) {
            System.err.println(
                "Usage: gradle :server:run --args \"<port> <max_connections> <backend>\"");
            System.err.println(
                "         backend = SimpleHashMapBackend | MultiConditionHashMapBackend | ShardedHashMapBackend nShards");
            System.exit(1);
        }

        // Serve requests
        DatabaseServer server = new DatabaseServer(port, maxConnections, backend);
        server.run();
    }
}
