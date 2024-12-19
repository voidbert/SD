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

import java.util.Random;

public class OperationDistribution {
    private final double put, get, multiPut, multiGet, getWhen;
    private final Random random;

    public OperationDistribution(double put,
                                 double get,
                                 double multiPut,
                                 double multiGet,
                                 double getWhen) {

        if (Math.abs(1.0 - put - get - multiPut - multiGet - getWhen) > 0.001)
            throw new IllegalArgumentException("Probabilities do not add up to one!");

        // Make values commulative for better performance
        this.put      = put;
        this.get      = get + this.put;
        this.multiPut = multiPut + this.get;
        this.multiGet = multiGet + this.multiPut;
        this.getWhen  = getWhen + this.multiGet;
        this.random   = new Random();
    }

    public OperationDistribution(OperationDistribution dist) {
        this(dist.getPut(),
             dist.getGet(),
             dist.getMultiPut(),
             dist.getMultiGet(),
             dist.getGetWhen());
    }

    public double getPut() {
        return this.put;
    }

    public double getGet() {
        return this.get - this.put;
    }

    public double getMultiPut() {
        return this.multiPut - this.get;
    }

    public double getMultiGet() {
        return this.multiGet - this.multiPut;
    }

    public double getGetWhen() {
        return this.getWhen - this.multiGet;
    }

    public Operation sample() {
        double random = this.random.nextDouble();

        if (random < this.put)
            return Operation.PUT;
        else if (random < this.get)
            return Operation.GET;
        else if (random < this.multiPut)
            return Operation.MULTI_PUT;
        else if (random < this.multiGet)
            return Operation.MULTI_GET;
        else if (this.getWhen != 0.0) // Do not return getWhen due to floating point errors
            return Operation.GET_WHEN;
        else
            return Operation.GET;
    }

    public Object clone() {
        return new OperationDistribution(this);
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        OperationDistribution dist = (OperationDistribution) o;
        return this.put == dist.getPut() && this.get == dist.getGet() &&
            this.multiPut == dist.getMultiPut() && this.multiGet == dist.getMultiGet() &&
            this.getWhen == dist.getGetWhen();
    }

    public String toString() {
        return String.format(
            "OperationDistribution(put = %f, get = %f, multiPut = %f, multiGet = %f, getWhen = %f)",
            this.getPut(),
            this.getGet(),
            this.getMultiPut(),
            this.getMultiGet(),
            this.getGetWhen());
    }
}
