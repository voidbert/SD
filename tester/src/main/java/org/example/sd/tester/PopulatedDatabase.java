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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.sd.common.KeyValueDB;

import org.apache.commons.lang3.ArrayUtils;

public class PopulatedDatabase {
    private final KeyValueDB database;
    private final String[] validKeys;
    private final byte[][] validValues;

    public PopulatedDatabase(KeyValueDB database, String[] validKeys, byte[][] validValues) {
        this.database    = database;
        this.validKeys   = validKeys;
        this.validValues = validValues;
    }

    public PopulatedDatabase(PopulatedDatabase database) {
        this((KeyValueDB) database.getDatabase().clone(),
             database.getValidKeys().clone(),
             Arrays.stream(database.getValidValues()).map(byte[] ::clone).toArray(byte[][] ::new));
    }

    public KeyValueDB getDatabase() {
        return this.database;
    }

    public String[] getValidKeys() {
        return this.validKeys;
    }

    private Set<String> getValidKeysSet() {
        return Arrays.stream(this.validKeys).collect(Collectors.toSet());
    }

    public byte[][] getValidValues() {
        return this.validValues;
    }

    // Use List<Byte> for valid equals
    private Set<List<Byte>> getValidValuesSet() {
        return Arrays.stream(this.validValues)
            .map(a -> Arrays.asList(ArrayUtils.toObject(a)))
            .collect(Collectors.toSet());
    }

    @Override
    public Object clone() {
        return new PopulatedDatabase(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        PopulatedDatabase database = (PopulatedDatabase) o;
        return this.database.equals(database.getDatabase()) &&
            this.getValidKeysSet().equals(database.getValidKeysSet()) &&
            this.getValidValuesSet().equals(database.getValidValuesSet());
    }

    @Override
    public String toString() {
        return String.format("PopulatedDatabase(database = %s, validKeys = %s, validValues = %s)",
                             this.database.toString(),
                             Arrays.toString(this.validKeys),
                             Arrays.deepToString(this.validValues));
    }
}
