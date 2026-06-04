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

import java.util.concurrent.atomic.AtomicReference;

/**
 * A circuit breaker that protects against cascading failures by
 * transitioning between CLOSED, OPEN, and HALF_OPEN states based on
 * operation success and failure counts.
 * <p>
 * In the CLOSED state, operations are allowed normally. When the failure
 * threshold is exceeded within the tracking window, the circuit opens.
 * </p>
 * <p>
 * In the OPEN state, operations are rejected immediately for a configurable
 * cooldown period. After the cooldown elapses, the circuit transitions to
 * HALF_OPEN.
 * </p>
 * <p>
 * In the HALF_OPEN state, a limited number of probe operations are allowed.
 * If probes succeed, the circuit closes; if any probe fails, the circuit
 * re-opens.
 * </p>
 * <p>
 * Thread-safe.
 * </p>
 *
 * @since 4.6
 */
public class CircuitBreaker {

    /** Circuit breaker states. */
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    /** Nanoseconds per millisecond. */
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final int failureThreshold;
    private final long cooldownMillis;
    private final int halfOpenMaxProbes;
    private final AtomicReference<Metrics> metrics;

    /**
     * Constructs a new {@code CircuitBreaker} with default half-open probe limit of 1.
     *
     * @param failureThreshold number of failures before opening the circuit
     * @param cooldownMillis   milliseconds to wait before transitioning from OPEN to HALF_OPEN
     * @throws IllegalArgumentException if any parameter is not positive
     */
    public CircuitBreaker(final int failureThreshold, final long cooldownMillis) {
        this(failureThreshold, cooldownMillis, 1);
    }

    /**
     * Constructs a new {@code CircuitBreaker}.
     *
     * @param failureThreshold   number of failures before opening the circuit
     * @param cooldownMillis     milliseconds to wait before transitioning from OPEN to HALF_OPEN
     * @param halfOpenMaxProbes  maximum number of probes allowed in HALF_OPEN state
     * @throws IllegalArgumentException if any parameter is not positive
     */
    public CircuitBreaker(final int failureThreshold, final long cooldownMillis, final int halfOpenMaxProbes) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be positive: " + failureThreshold);
        }
        if (cooldownMillis <= 0) {
            throw new IllegalArgumentException("cooldownMillis must be positive: " + cooldownMillis);
        }
        if (halfOpenMaxProbes <= 0) {
            throw new IllegalArgumentException("halfOpenMaxProbes must be positive: " + halfOpenMaxProbes);
        }
        this.failureThreshold = failureThreshold;
        this.cooldownMillis = cooldownMillis;
        this.halfOpenMaxProbes = halfOpenMaxProbes;
        this.metrics = new AtomicReference<>(new Metrics(0, 0, 0, 0, State.CLOSED));
    }

    /**
     * Checks whether an operation is allowed to proceed.
     *
     * @return {@code true} if the operation can proceed, {@code false} if the circuit is open
     */
    public boolean allowRequest() {
        while (true) {
            final Metrics current = metrics.get();
            switch (current.state) {
            case CLOSED:
                return true;
            case OPEN:
                final long elapsed = (System.nanoTime() - current.openedAtNanos) / NANOS_PER_MILLI;
                if (elapsed >= cooldownMillis) {
                    final Metrics halfOpen = new Metrics(0, 0, 0, System.nanoTime(), State.HALF_OPEN);
                    if (metrics.compareAndSet(current, halfOpen)) {
                        return true;
                    }
                    break;
                }
                return false;
            case HALF_OPEN:
                return current.probeCount < halfOpenMaxProbes;
            default:
                return false;
            }
        }
    }

    /**
     * Records a successful operation.
     */
    public void recordSuccess() {
        while (true) {
            final Metrics current = metrics.get();
            if (current.state == State.CLOSED) {
                final Metrics updated = new Metrics(0, current.successCount + 1, 0, 0, State.CLOSED);
                if (metrics.compareAndSet(current, updated)) {
                    return;
                }
            } else if (current.state == State.HALF_OPEN) {
                final int newProbes = current.probeCount + 1;
                if (newProbes >= halfOpenMaxProbes) {
                    final Metrics closed = new Metrics(0, current.successCount + 1, 0, 0, State.CLOSED);
                    if (metrics.compareAndSet(current, closed)) {
                        return;
                    }
                } else {
                    final Metrics updated = new Metrics(0, current.successCount + 1, newProbes, current.openedAtNanos, State.HALF_OPEN);
                    if (metrics.compareAndSet(current, updated)) {
                        return;
                    }
                }
            } else {
                return;
            }
        }
    }

    /**
     * Records a failed operation. If the failure threshold is reached in CLOSED state,
     * the circuit opens. In HALF_OPEN state, a single failure re-opens the circuit.
     */
    public void recordFailure() {
        while (true) {
            final Metrics current = metrics.get();
            if (current.state == State.CLOSED) {
                final int newFailures = current.failureCount + 1;
                if (newFailures >= failureThreshold) {
                    final Metrics opened = new Metrics(newFailures, current.successCount, 0, System.nanoTime(), State.OPEN);
                    if (metrics.compareAndSet(current, opened)) {
                        return;
                    }
                } else {
                    final Metrics updated = new Metrics(newFailures, current.successCount, 0, 0, State.CLOSED);
                    if (metrics.compareAndSet(current, updated)) {
                        return;
                    }
                }
            } else if (current.state == State.HALF_OPEN) {
                final Metrics opened = new Metrics(current.failureCount + 1, current.successCount, 0, System.nanoTime(), State.OPEN);
                if (metrics.compareAndSet(current, opened)) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    /**
     * Returns the current state of the circuit breaker.
     *
     * @return the current state
     */
    public State getState() {
        final Metrics current = metrics.get();
        if (current.state == State.OPEN) {
            final long elapsed = (System.nanoTime() - current.openedAtNanos) / NANOS_PER_MILLI;
            if (elapsed >= cooldownMillis) {
                return State.HALF_OPEN;
            }
        }
        return current.state;
    }

    /**
     * Returns the current failure count.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return metrics.get().failureCount;
    }

    /**
     * Returns the current success count.
     *
     * @return the success count
     */
    public int getSuccessCount() {
        return metrics.get().successCount;
    }

    /**
     * Immutable snapshot of the circuit breaker metrics.
     */
    private static final class Metrics {
        final int failureCount;
        final int successCount;
        final int probeCount;
        final long openedAtNanos;
        final State state;

        Metrics(final int failureCount, final int successCount, final int probeCount,
                final long openedAtNanos, final State state) {
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.probeCount = probeCount;
            this.openedAtNanos = openedAtNanos;
            this.state = state;
        }
    }
}