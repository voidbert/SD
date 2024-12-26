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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.sd.common.KeyValueDB;

import org.apache.commons.lang3.ArrayUtils;

public class DatabasePopulator {
    private final KeyValueDB empty;
    private final int        nKeys, nValues, keyLength, valueLength;
    private final Random     random;

    public DatabasePopulator(KeyValueDB empty,
                             int        nKeys,
                             int        nValues,
                             int        keyLength,
                             int        valueLength) {

        this.empty       = (KeyValueDB) empty.clone();
        this.nKeys       = nKeys;
        this.nValues     = nValues;
        this.keyLength   = keyLength;
        this.valueLength = valueLength;
        this.random      = new Random();
    }

    public DatabasePopulator(DatabasePopulator populator) {
        this(populator.getEmpty(),
             populator.getNKeys(),
             populator.getNValues(),
             populator.getKeyLength(),
             populator.getValueLength());
    }

    private String[] generateValidKeys() {
        Set<String> ret = new HashSet<String>();

        while (ret.size() < this.nKeys) {
            String        chars   = "ABCDEFGHIJKLMONQRSTUVWXYZabcdefghijklmonqrstuvwxyz0123456789";
            StringBuilder builder = new StringBuilder();

            while (builder.length() < this.keyLength)
                builder.append(chars.charAt(this.random.nextInt(chars.length())));

            ret.add(builder.toString());
        }

        return ret.stream().toArray(String[] ::new);
    }

    private byte[][] generateValidValues() {
        // Use List for valid .equals()
        Set<List<Byte>> ret = new HashSet<List<Byte>>();
        while (ret.size() < this.nValues) {
            List<Byte> value = new ArrayList<Byte>(this.nValues);
            for (int i = 0; i < this.valueLength; ++i)
                value.add((byte) this.random.nextInt());

            ret.add(value);
        }

        return ret.stream()
            .map(l -> ArrayUtils.toPrimitive(l.toArray(new Byte[0])))
            .toArray(byte[][] ::new);
    }

    public PopulatedDatabase newDatabase() {
        KeyValueDB database    = (KeyValueDB) this.empty.clone();
        String[]   validKeys   = this.generateValidKeys();
        byte[][]   validValues = this.generateValidValues();

        Map<String, byte[]> databaseContents = Arrays.stream(validKeys).collect(
            Collectors.toMap(k -> k, k -> validValues[this.random.nextInt(validValues.length)]));
        database.multiPut(databaseContents);

        return new PopulatedDatabase(database, validKeys, validValues);
    }

    private KeyValueDB getEmpty() {
        return (KeyValueDB) this.empty.clone();
    }

    public int getNKeys() {
        return this.nKeys;
    }

    public int getNValues() {
        return this.nValues;
    }

    public int getKeyLength() {
        return this.keyLength;
    }

    public int getValueLength() {
        return this.valueLength;
    }

    @Override
    public Object clone() {
        return new DatabasePopulator(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        DatabasePopulator populator = (DatabasePopulator) o;
        return this.empty.equals(populator.getEmpty()) && this.nKeys == populator.getNKeys() &&
            this.nValues == populator.getNValues() && this.keyLength == populator.getKeyLength() &&
            this.valueLength == populator.getValueLength();
    }

    @Override
    public String toString() {
        return String.format(
            "DatabasePopulator(empty = %s, nKeys = %d, nValues = %d, keyLength = %d, valueLength = %d)",
            this.empty.toString(),
            this.nKeys,
            this.nValues,
            this.keyLength,
            this.valueLength);
    }
}
