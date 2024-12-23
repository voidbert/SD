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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.sd.common.DatabaseClientException;
import org.example.sd.common.KeyValueDB;
import org.example.sd.common.Message;

import org.apache.commons.lang3.StringUtils;

public abstract class CommandRunner {
    private final KeyValueDB database;

    protected CommandRunner(KeyValueDB database) {
        this.database = database;
    }

    public void parseAndRun(String str) throws CommandException {
        String trimmed = str.trim();
        String upper   = trimmed.toUpperCase();

        boolean isBackground = false;
        if (trimmed.endsWith("&")) {
            isBackground = true;
            trimmed      = trimmed.substring(0, trimmed.length() - 1);
            upper        = upper.substring(0, upper.length() - 1);
        }

        if (upper.startsWith("GETWHEN")) {
            this.parseAndRunGetWhen(trimmed.substring(7).trim(), isBackground);
        } else if (upper.startsWith("PUT")) {
            this.parseAndRunPut(trimmed.substring(3).trim(), isBackground);
        } else if (upper.startsWith("GET")) {
            this.parseAndRunGet(trimmed.substring(3).trim(), isBackground);
        } else if (upper.startsWith("MULTIPUT")) {
            this.parseAndRunMultiPut(trimmed.substring(8).trim(), isBackground);
        } else if (upper.startsWith("MULTIGET")) {
            this.parseAndRunMultiGet(trimmed.substring(8).trim(), isBackground);
        } else {
            throw new CommandException("Unknown command");
        }
    }

    private void parseAndRunGet(String argument, boolean isBackground) throws CommandException {
        if (!StringUtils.isAlphanumeric(argument))
            throw new CommandException("Non-alphanumeric GET argument: " + argument);

        chooseForegroundOrBackground(isBackground, () -> {
            try {
                byte[] value = this.database.get(argument);
                this.reactToGetResult(argument, value);
            } catch (DatabaseClientException e) {
                this.reactToException(e);
            }
        });
    }

    private void parseAndRunPut(String arguments, boolean isBackground) throws CommandException {
        int keyEnd = arguments.indexOf(' ');
        if (keyEnd == -1)
            throw new CommandException("Wrong number of PUT arguments");

        String key = arguments.substring(0, keyEnd);
        if (!StringUtils.isAlphanumeric(key))
            throw new CommandException("Non-alphanumeric PUT key: " + key);

        byte[] value = this.parseValueArray(arguments.substring(keyEnd).trim(), "PUT");

        chooseForegroundOrBackground(isBackground, () -> {
            try {
                this.database.put(key, value);
                this.reactToPutResult(key, value);
            } catch (DatabaseClientException e) {
                this.reactToException(e);
            }
        });
    }

    private void parseAndRunMultiPut(String arguments, boolean isBackground)
        throws CommandException {

        Map<String, byte[]> pairs = new HashMap<String, byte[]>();
        while (arguments.length() > 0) {
            int keyEnd = arguments.indexOf(' ');
            if (keyEnd == -1)
                throw new CommandException("Wrong number of MULTIPUT arguments");

            String key = arguments.substring(0, keyEnd);
            if (!StringUtils.isAlphanumeric(key))
                throw new CommandException("Non-alphanumeric MULTIPUT key: " + key);

            int valueEnd = arguments.indexOf(']', keyEnd);
            if (valueEnd == -1)
                throw new CommandException("Wrong number of MULTIPUT arguments");

            String valueStr = arguments.substring(keyEnd, valueEnd + 1).trim();
            byte[] value    = this.parseValueArray(valueStr, "MULTIPUT");

            pairs.put(key, value);
            arguments = arguments.substring(valueEnd + 1).trim();
        }

        if (pairs.size() == 0)
            throw new CommandException("Wrong number of MULTIPUT arguments");

        chooseForegroundOrBackground(isBackground, () -> {
            try {
                this.database.multiPut(pairs);
                this.reactToMultiPutResult(pairs);
            } catch (DatabaseClientException e) {
                this.reactToException(e);
            }
        });
    }

    private void parseAndRunMultiGet(String argument, boolean isBackground)
        throws CommandException {

        Set<String> keys = Arrays.stream(argument.split(" "))
                               .filter(k -> !k.equals(""))
                               .collect(Collectors.toSet());
        if (keys.size() == 0)
            throw new CommandException("Wrong number of MULTIGET arguments");

        for (String key : keys)
            if (!StringUtils.isAlphanumeric(key))
                throw new CommandException("Non-alphanumeric MULTIGET key: " + key);

        chooseForegroundOrBackground(isBackground, () -> {
            try {
                Map<String, byte[]> result = this.database.multiGet(keys);
                this.reactToMultiGetResult(keys, result);
            } catch (DatabaseClientException e) {
                this.reactToException(e);
            }
        });
    }

    private void parseAndRunGetWhen(String arguments, boolean isBackground)
        throws CommandException {
        int keyEnd = arguments.indexOf(' ');
        if (keyEnd == -1)
            throw new CommandException("Wrong number of GETWHEN arguments");

        String key = arguments.substring(0, keyEnd);
        if (!StringUtils.isAlphanumeric(key))
            throw new CommandException("Non-alphanumeric GETWHEN key: " + key);

        String remainingArguments = arguments.substring(keyEnd).trim();

        int keyCondEnd = remainingArguments.indexOf(' ');
        if (keyCondEnd == -1)
            throw new CommandException("Wrong number of GETWHEN arguments");

        String keyCond = remainingArguments.substring(0, keyCondEnd);
        if (!StringUtils.isAlphanumeric(keyCond))
            throw new CommandException("Non-alphanumeric GETWHEN conditional key: " + key);

        byte[] valueCond =
            this.parseValueArray(remainingArguments.substring(keyCondEnd).trim(), "GETWHEN");

        chooseForegroundOrBackground(isBackground, () -> {
            try {
                byte[] value = this.database.getWhen(key, keyCond, valueCond);
                this.reactToGetWhenResult(key, keyCond, valueCond, value);
            } catch (DatabaseClientException e) {
                this.reactToException(e);
            }
        });
    }

    private String[] parseArray(String array, String commandName) throws CommandException {
        if (!array.startsWith("[") || !array.endsWith("]"))
            throw new CommandException(
                String.format("Invalid array in %s command: %s", commandName, array));

        String[] stringValues = array.substring(1, array.length() - 1).split(",");
        if (stringValues.length == 1 && stringValues[0].equals(""))
            return new String[0];

        return Arrays.stream(stringValues).map(String::trim).toArray(String[] ::new);
    }

    private byte[] parseValueArray(String array, String commandName) throws CommandException {
        String[] stringValues = this.parseArray(array, commandName);
        byte[]   ret          = new byte[stringValues.length];
        for (int i = 0; i < stringValues.length; ++i) {
            try {
                ret[i] = Byte.valueOf(stringValues[i]);
            } catch (NumberFormatException e) {
                throw new CommandException(String.format("Invalid byte in array in %s command: %s",
                                                         commandName,
                                                         stringValues[i]));
            }
        }
        return ret;
    }

    private void chooseForegroundOrBackground(boolean isBackground, Runnable r) {
        if (isBackground) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.start();
        } else {
            r.run();
        }
    }

    protected abstract void reactToException(DatabaseClientException e);
    protected abstract void reactToGetResult(String key, byte[] value);
    protected abstract void reactToPutResult(String key, byte[] value);
    protected abstract void reactToMultiGetResult(Set<String> keys, Map<String, byte[]> values);
    protected abstract void reactToMultiPutResult(Map<String, byte[]> values);
    protected abstract void
        reactToGetWhenResult(String key, String keyCond, byte[] valueCond, byte[] value);

    protected KeyValueDB getDatabase() {
        return this.database;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        // Check if the database is the same (not equal)
        CommandRunner runner = (CommandRunner) o;
        return this.database == runner.getDatabase();
    }
}
