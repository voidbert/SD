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

package org.example.sd.tester;

import java.io.IOException;

public class Tester {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: gradle :tester:run --args <output_directory>");
            System.exit(1);
        }

        TestSuite suite = new TestSuite(args[0]);
        suite.run();
    }
}
