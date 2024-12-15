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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class TestResults {
    private Map<Operation, Long>    operationSum;
    private Map<Operation, Long>    operationSumSquares;
    private Map<Operation, Integer> operationCount;

    public TestResults() {
        this.operationSum        = new HashMap<Operation, Long>();
        this.operationSumSquares = new HashMap<Operation, Long>();
        this.operationCount      = new HashMap<Operation, Integer>();

        for (Operation operation : Operation.values()) {
            if (operation != Operation.GET_WHEN) {
                this.operationSum.put(operation, (long) 0);
                this.operationSumSquares.put(operation, (long) 0);
                this.operationCount.put(operation, 0);
            }
        }
    }

    public TestResults(TestResults results) {
        this.operationSum        = results.getOperationSum();
        this.operationSumSquares = results.getOperationSumSquares();
        this.operationCount      = results.getOperationCount();
    }

    public void addSample(Operation operation, long time) {
        if (operation == Operation.GET_WHEN)
            return;

        this.operationSum.put(operation, this.operationSum.get(operation) + time);
        this.operationSumSquares.put(operation,
                                     this.operationSumSquares.get(operation) + time * time);
        this.operationCount.put(operation, this.operationCount.get(operation) + 1);
    }

    public void mergeWith(TestResults results) {
        for (Operation operation : Operation.values()) {
            if (operation != Operation.GET_WHEN) {
                Long otherOperationSum = results.getOperationSum().get(operation);
                this.operationSum.put(operation,
                                      this.operationSum.get(operation) + otherOperationSum);

                Long otherOperationSumSquares = results.getOperationSumSquares().get(operation);
                this.operationSumSquares.put(operation,
                                             this.operationSumSquares.get(operation) +
                                                 otherOperationSumSquares);

                Integer otherOperationCount = results.getOperationCount().get(operation);
                this.operationCount.put(operation,
                                        this.operationCount.get(operation) + otherOperationCount);
            }
        }
    }

    public OptionalDouble getAverage(Operation operation) {
        int  sampleCount = this.operationCount.get(operation);
        long sum         = this.operationSum.get(operation);

        if (sampleCount == 0)
            return OptionalDouble.empty();
        else
            return OptionalDouble.of((double) sum / sampleCount);
    }

    public OptionalDouble getStdev(Operation operation) {
        int  sampleCount = this.operationCount.get(operation);
        long sum         = this.operationSum.get(operation);
        long sumSquares  = this.operationSumSquares.get(operation);

        if (sampleCount == 0) {
            return OptionalDouble.empty();
        } else {
            double average  = (double) sum / sampleCount;
            double variance = ((double) sumSquares / sampleCount) - average * average;
            return OptionalDouble.of(Math.sqrt(variance));
        }
    }

    private Map<Operation, Long> getOperationSum() {
        return this.operationSum.entrySet().stream().collect(
            Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private Map<Operation, Long> getOperationSumSquares() {
        return this.operationSumSquares.entrySet().stream().collect(
            Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private Map<Operation, Integer> getOperationCount() {
        return this.operationCount.entrySet().stream().collect(
            Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    public Object clone() {
        return new TestResults(this);
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        TestResults results = (TestResults) o;
        return this.operationSum.equals(results.getOperationSum()) &&
            this.operationSumSquares.equals(results.getOperationSumSquares()) &&
            this.operationCount.equals(results.getOperationCount());
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("TestResults(");

        boolean isFirst = true;
        for (Operation operation : Operation.values()) {
            if (operation != Operation.GET_WHEN) {
                if (isFirst)
                    isFirst = false;
                else
                    builder.append("; ");

                builder.append(operation);
                builder.append(": avg = ");
                builder.append(this.getAverage(operation));
                builder.append(", stdev = ");
                builder.append(this.getStdev(operation));
            }
        }

        builder.append(")");
        return builder.toString();
    }
}
