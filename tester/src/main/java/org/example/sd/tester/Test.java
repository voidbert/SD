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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.example.sd.common.KeyValueDB;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;

public class Test {
    private final DatabasePopulator           populator;
    private final OperationDistribution       operationDistribution;
    private final AbstractIntegerDistribution keyDistribution, valueDistribution;
    private final AbstractIntegerDistribution multiCountDistribution;
    private final int                         nThreads, nOperations, operationBlockSize;

    public Test(DatabasePopulator           populator,
                OperationDistribution       operationDistribution,
                AbstractIntegerDistribution keyDistribution,
                AbstractIntegerDistribution valueDistribution,
                AbstractIntegerDistribution multiCountDistribution,
                int                         nThreads,
                int                         nOperations,
                int                         operationBlockSize) {

        this.populator = populator;

        this.operationDistribution  = operationDistribution;
        this.keyDistribution        = keyDistribution;
        this.valueDistribution      = valueDistribution;
        this.multiCountDistribution = multiCountDistribution;

        this.nThreads           = nThreads;
        this.nOperations        = nOperations;
        this.operationBlockSize = operationBlockSize;
    }

    public Test(Test test) {
        this(test.getPopulator(),
             test.getOperationDistribution(),
             test.getKeyDistribution(),
             test.getValueDistribution(),
             test.getMultiCountDistribution(),
             test.getNThreads(),
             test.getNOperations(),
             test.getOperationBlockSize());
    }

    public TestResults run() {
        PopulatedDatabase database       = this.populator.newDatabase();
        AtomicInteger     operationCount = new AtomicInteger();

        Thread[] regularThreads        = new Thread[this.nThreads];
        Thread   getWhenUnlockerThread = null;

        AtomicBoolean                                regularThreadsDone = new AtomicBoolean();
        AtomicReference<Map.Entry<String, byte[]>>[] currentConditions =
            (AtomicReference<Map.Entry<String, byte[]>>[]) new AtomicReference[this.nThreads];

        TestResults[] threadResults = new TestResults[this.nThreads];

        // Prepare threads and their state
        for (int i = 0; i < this.nThreads; ++i) {
            final int lambda_i = i;

            currentConditions[i] = new AtomicReference<Map.Entry<String, byte[]>>();
            threadResults[i]     = new TestResults();
            regularThreads[i] =
                new Thread(()
                               -> this.regularThreadLoop(database,
                                                         operationCount,
                                                         currentConditions[lambda_i],
                                                         threadResults[lambda_i]));
        }

        long startTime = System.nanoTime();

        // Run all threads (including getWhen unlocker thread, if needed)
        if (this.operationDistribution.getGetWhen() > 0) {
            getWhenUnlockerThread = new Thread(
                () -> this.getWhenThreadLoop(database, currentConditions, regularThreadsDone));
            getWhenUnlockerThread.start();
        }

        for (int i = 0; i < this.nThreads; ++i)
            regularThreads[i].start();

        // Wait for all threads (and, in the end, for the getWhen unlocker thread)
        for (int i = 0; i < this.nThreads; ++i) {
            try {
                regularThreads[i].join();
            } catch (InterruptedException e) {}
        }

        if (getWhenUnlockerThread != null) {
            regularThreadsDone.set(true);
            try {
                getWhenUnlockerThread.join();
            } catch (InterruptedException e) {}
        }

        long endTime = System.nanoTime();

        // Prepare final test results
        for (int i = 1; i < this.nThreads; ++i)
            threadResults[0].mergeWith(threadResults[i]);

        if (this.operationDistribution.getGetWhen() == 0)
            threadResults[0].setTestTime(endTime - startTime);
        return threadResults[0];
    }

    private void getWhenThreadLoop(PopulatedDatabase                            populatedDatabase,
                                   AtomicReference<Map.Entry<String, byte[]>>[] currentConditions,
                                   AtomicBoolean regularThreadsDone) {

        while (!regularThreadsDone.get()) {
            Map<String, byte[]> unlocker =
                Arrays.stream(currentConditions)
                    .map(AtomicReference::get)
                    .filter(e -> e != null)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e1));

            if (unlocker.size() > 0)
                populatedDatabase.getDatabase().multiPut(unlocker);
        }
    }

    private void regularThreadLoop(PopulatedDatabase                          populatedDatabase,
                                   AtomicInteger                              operationCount,
                                   AtomicReference<Map.Entry<String, byte[]>> currentCondition,
                                   TestResults                                results) {

        // Creater local copies of the distributions and seed their random generators
        Random reseeder = new Random();

        OperationDistribution threadOperationDistribution =
            (OperationDistribution) this.operationDistribution.clone();

        IntegerDistribution threadKeyDistribution =
            (IntegerDistribution) SerializationUtils.clone(this.keyDistribution);
        threadKeyDistribution.reseedRandomGenerator(reseeder.nextLong());

        IntegerDistribution threadValueDistribution =
            (IntegerDistribution) SerializationUtils.clone(this.valueDistribution);
        threadValueDistribution.reseedRandomGenerator(reseeder.nextLong());

        IntegerDistribution threadMultiCountDistribution =
            (IntegerDistribution) SerializationUtils.clone(this.multiCountDistribution);
        threadMultiCountDistribution.reseedRandomGenerator(reseeder.nextLong());

        // Variable copies for easier access
        KeyValueDB database    = populatedDatabase.getDatabase();
        String[]   validKeys   = populatedDatabase.getValidKeys();
        byte[][]   validValues = populatedDatabase.getValidValues();

        while (true) {
            // Determine number of operations
            int counter = operationCount.getAndAdd(this.operationBlockSize);
            if (counter >= nOperations)
                break;

            int threadOperationCount =
                Math.min(this.operationBlockSize, this.nOperations - counter);

            // Generate and run operations
            for (int i = 0; i < threadOperationCount; ++i) {
                Operation operation = threadOperationDistribution.sample();
                long      startTime = 0, endTime = 0;

                if (operation == Operation.PUT) {
                    String key   = this.generateKey(validKeys, threadKeyDistribution);
                    byte[] value = this.generateValue(validValues, threadValueDistribution);

                    startTime = System.nanoTime();
                    database.put(key, value);
                    endTime = System.nanoTime();
                } else if (operation == Operation.GET) {
                    String key = this.generateKey(validKeys, threadKeyDistribution);

                    startTime = System.nanoTime();
                    database.get(key);
                    endTime = System.nanoTime();
                } else if (operation == Operation.MULTI_PUT) {
                    int multiOperationCount =
                        this.generateMultiOperationCount(validKeys, threadMultiCountDistribution);

                    startTime = System.nanoTime();
                    database.multiPut(this.generateKeyValuePairs(multiOperationCount,
                                                                 validKeys,
                                                                 validValues,
                                                                 threadKeyDistribution,
                                                                 threadValueDistribution));
                    endTime = System.nanoTime();
                } else if (operation == Operation.MULTI_GET) {
                    int multiOperationCount =
                        this.generateMultiOperationCount(validKeys, threadMultiCountDistribution);

                    startTime = System.nanoTime();
                    database.multiGet(this.generateMultipleKeys(multiOperationCount,
                                                                validKeys,
                                                                threadKeyDistribution));
                    endTime = System.nanoTime();
                } else if (operation == Operation.GET_WHEN) {
                    String key       = this.generateKey(validKeys, threadKeyDistribution);
                    String keyCond   = this.generateKey(validKeys, threadKeyDistribution);
                    byte[] valueCond = this.generateValue(validValues, threadValueDistribution);

                    currentCondition.set(new SimpleImmutableEntry(keyCond, valueCond));

                    startTime = System.nanoTime();
                    database.getWhen(key, keyCond, valueCond);
                    endTime = System.nanoTime();

                    currentCondition.set(null);
                }

                results.addSample(operation, endTime - startTime);
            }
        }
    }

    private String generateKey(String[] validKeys, IntegerDistribution distribution) {
        return validKeys[Math.clamp(distribution.sample(), 0, validKeys.length - 1)];
    }

    private byte[] generateValue(byte[][] validValues, IntegerDistribution distribution) {
        return validValues[Math.clamp(distribution.sample(), 0, validValues.length - 1)];
    }

    private Set<String>
        generateMultipleKeys(int count, String[] validKeys, IntegerDistribution distribution) {

        Set<String> keys = new HashSet<String>();
        while (keys.size() < count)
            keys.add(this.generateKey(validKeys, distribution));
        return keys;
    }

    private Map<String, byte[]> generateKeyValuePairs(int                 count,
                                                      String[]            validKeys,
                                                      byte[][]            validValues,
                                                      IntegerDistribution keyDistribution,
                                                      IntegerDistribution valueDistribution) {

        return this.generateMultipleKeys(count, validKeys, keyDistribution)
            .stream()
            .collect(
                Collectors.toMap(k -> k, k -> this.generateValue(validValues, valueDistribution)));
    }

    private int generateMultiOperationCount(String[] validKeys, IntegerDistribution distribution) {
        return Math.clamp(distribution.sample(), 0, validKeys.length - 1);
    }

    public DatabasePopulator getPopulator() {
        return this.populator;
    }

    public OperationDistribution getOperationDistribution() {
        return this.operationDistribution;
    }

    public AbstractIntegerDistribution getKeyDistribution() {
        return this.keyDistribution;
    }

    public AbstractIntegerDistribution getValueDistribution() {
        return this.valueDistribution;
    }

    public AbstractIntegerDistribution getMultiCountDistribution() {
        return this.multiCountDistribution;
    }

    public int getNThreads() {
        return this.nThreads;
    }

    public int getNOperations() {
        return this.nOperations;
    }

    public int getOperationBlockSize() {
        return this.operationBlockSize;
    }

    public Object clone() {
        return new Test(this);
    }

    // Cannot safely override equals() due to lack of equals() method in AbstractIntegerDistribution

    public String toString() {
        return String.format(
            "Test(populator = %s, operationDistribution = %s, keyDistribution = %s, valueDistribution = %s, multiCountDistribution = %s, nThreads = %d, nOperations = %d, operationBlockSize = %d)",
            this.populator.toString(),
            this.operationDistribution.toString(),
            (new ReflectionToStringBuilder(this.keyDistribution)).toString(),
            (new ReflectionToStringBuilder(this.valueDistribution)).toString(),
            (new ReflectionToStringBuilder(this.multiCountDistribution)).toString(),
            this.nThreads,
            this.nOperations,
            this.operationBlockSize);
    }
}
