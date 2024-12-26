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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.example.sd.common.DatabaseClientException;
import org.example.sd.common.KeyValueDB;

public class LoggerCommandRunner extends CommandRunner {
    private Lock         outputLock;
    private final String prompt;

    public LoggerCommandRunner(KeyValueDB database, String prompt) {
        super(database);
        this.outputLock = new ReentrantLock();
        this.prompt     = prompt;
    }

    public LoggerCommandRunner(LoggerCommandRunner runner) {
        this(runner.getDatabase(), runner.getPrompt());
    }

    @Override
    protected void reactToBackgrounding() {
        this.outputLock.lock();
        try {
            System.out.printf("Request backgrounded\n%s", this.prompt);
        } finally {
            this.outputLock.unlock();
        }
    }

    @Override
    protected void reactToException(DatabaseClientException e) {
        // No need to lock stderr
        System.err.printf("%s\n%s", e.getMessage(), this.prompt);
    }

    @Override
    protected void reactToGetResult(String key, byte[] value) {
        this.outputLock.lock();
        try {
            System.out.printf("GET %s completed: %s\n%s", key, Arrays.toString(value), this.prompt);
        } finally {
            this.outputLock.unlock();
        }
    }

    @Override
    protected void reactToPutResult(String key, byte[] value) {
        this.outputLock.lock();
        try {
            System.out.printf("PUT %s %s completed\n%s", key, Arrays.toString(value), prompt);
        } finally {
            this.outputLock.unlock();
        }
    }

    @Override
    protected void reactToMultiGetResult(Set<String> keys, Map<String, byte[]> values) {
        this.outputLock.lock();
        try {
            System.out.printf("MULTIGET %s completed:\n", String.join(" ", keys));
            for (String key : keys)
                System.out.printf("  %s: %s\n", key, Arrays.toString(values.get(key)));
            System.out.print(this.prompt);
        } finally {
            this.outputLock.unlock();
        }
    }

    @Override
    protected void reactToMultiPutResult(Map<String, byte[]> values) {
        String valuesString =
            String.join(" ",
                        values.entrySet()
                            .stream()
                            .map(e -> e.getKey() + " " + Arrays.toString(e.getValue()))
                            .toArray(String[] ::new));

        this.outputLock.lock();
        try {
            System.out.printf("MULTIPUT %s completed\n%s", valuesString, this.prompt);
        } finally {
            this.outputLock.unlock();
        }
    }

    @Override
    protected void
        reactToGetWhenResult(String key, String keyCond, byte[] valueCond, byte[] value) {

        this.outputLock.lock();
        try {
            System.out.printf("GETWHEN %s %s %s completed: %s\n%s",
                              key,
                              keyCond,
                              Arrays.toString(valueCond),
                              Arrays.toString(value),
                              this.prompt);
        } finally {
            this.outputLock.unlock();
        }
    }

    public String getPrompt() {
        return this.prompt;
    }

    @Override
    public Object clone() {
        return new LoggerCommandRunner(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        LoggerCommandRunner runner = (LoggerCommandRunner) o;
        return super.equals(o) && this.prompt.equals(runner.getPrompt());
    }

    @Override
    public String toString() {
        return String.format("LoggerCommandRunner(prompt=%s)", this.prompt);
    }
}
