/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4;

import java.util.concurrent.atomic.AtomicLong;

public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long monitoringWindowMillis;
    private final long cooldownMillis;
    private final int halfOpenMaxAttempts;

    private final AtomicLong failureCount;
    private final AtomicLong successCount;
    private final AtomicLong halfOpenAttempts;
    private volatile State state;
    private volatile long lastFailureTime;
    private volatile long windowStart;

    public CircuitBreaker(final int failureThreshold,
                          final long monitoringWindowMillis,
                          final long cooldownMillis,
                          final int halfOpenMaxAttempts) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be greater than zero");
        }
        if (monitoringWindowMillis <= 0) {
            throw new IllegalArgumentException("monitoringWindowMillis must be greater than zero");
        }
        if (cooldownMillis <= 0) {
            throw new IllegalArgumentException("cooldownMillis must be greater than zero");
        }
        if (halfOpenMaxAttempts <= 0) {
            throw new IllegalArgumentException("halfOpenMaxAttempts must be greater than zero");
        }
        this.failureThreshold = failureThreshold;
        this.monitoringWindowMillis = monitoringWindowMillis;
        this.cooldownMillis = cooldownMillis;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
        this.state = State.CLOSED;
        this.failureCount = new AtomicLong(0);
        this.successCount = new AtomicLong(0);
        this.halfOpenAttempts = new AtomicLong(0);
        this.windowStart = System.currentTimeMillis();
    }

    public CircuitBreaker(final int failureThreshold,
                          final long monitoringWindowMillis,
                          final long cooldownMillis) {
        this(failureThreshold, monitoringWindowMillis, cooldownMillis, 1);
    }

    public synchronized boolean allowRequest() {
        final long now = System.currentTimeMillis();
        switch (state) {
            case CLOSED:
                resetWindowIfExpired(now);
                return true;
            case OPEN:
                if (now - lastFailureTime >= cooldownMillis) {
                    transitionToHalfOpen();
                    return true;
                }
                return false;
            case HALF_OPEN:
                return halfOpenAttempts.getAndIncrement() < halfOpenMaxAttempts;
            default:
                return false;
        }
    }

    public synchronized void recordSuccess() {
        if (state == State.HALF_OPEN) {
            transitionToClosed();
        } else {
            successCount.incrementAndGet();
        }
    }

    public synchronized void recordFailure() {
        final long now = System.currentTimeMillis();
        if (state == State.HALF_OPEN) {
            transitionToOpen(now);
            return;
        }
        if (state == State.CLOSED) {
            resetWindowIfExpired(now);
            final long count = failureCount.incrementAndGet();
            if (count >= failureThreshold) {
                transitionToOpen(now);
            }
        }
    }

    public State getState() {
        final long now = System.currentTimeMillis();
        if (state == State.OPEN && now - lastFailureTime >= cooldownMillis) {
            return State.HALF_OPEN;
        }
        return state;
    }

    public long getFailureCount() {
        return failureCount.get();
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public long getMonitoringWindowMillis() {
        return monitoringWindowMillis;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public synchronized void reset() {
        state = State.CLOSED;
        failureCount.set(0);
        successCount.set(0);
        halfOpenAttempts.set(0);
        windowStart = System.currentTimeMillis();
    }

    private void resetWindowIfExpired(final long now) {
        if (now - windowStart >= monitoringWindowMillis) {
            failureCount.set(0);
            successCount.set(0);
            windowStart = now;
        }
    }

    private void transitionToOpen(final long now) {
        state = State.OPEN;
        lastFailureTime = now;
        halfOpenAttempts.set(0);
    }

    private void transitionToHalfOpen() {
        state = State.HALF_OPEN;
        halfOpenAttempts.set(0);
    }

    private void transitionToClosed() {
        state = State.CLOSED;
        failureCount.set(0);
        successCount.set(0);
        halfOpenAttempts.set(0);
        windowStart = System.currentTimeMillis();
    }
}
