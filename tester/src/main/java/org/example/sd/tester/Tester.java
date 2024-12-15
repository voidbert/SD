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

import org.example.sd.common.KeyValueDB;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;

public class Tester {
    public static void main(String[] args) {
        // TODO - make configurable

        KeyValueDB        empty     = null; // TODO - use actual database when merged
        DatabasePopulator populator = new DatabasePopulator(empty, 128, 128, 8, 8);

        OperationDistribution operationDistribution =
            new OperationDistribution(0.4, 0.39, 0.1, 0.10, 0.01);
        AbstractIntegerDistribution keyDistribution = new ZipfDistribution(populator.getNKeys(), 1);
        AbstractIntegerDistribution valueDistribution =
            new ZipfDistribution(populator.getNValues(), 1);
        AbstractIntegerDistribution multiCountDistribution = new UniformIntegerDistribution(1, 2);

        int nThreads            = 8;
        int nOperations         = 10_000_000;
        int nOperationBlockSize = 10000;

        Test test = new Test(populator,
                             operationDistribution,
                             keyDistribution,
                             valueDistribution,
                             multiCountDistribution,
                             nThreads,
                             nOperations,
                             nOperationBlockSize);

        System.out.println(test.run());
    }
}
