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
 * A token bucket based rate limiter providing configurable throughput control.
 * <p>
 * This implementation uses the token bucket algorithm: tokens are added at a fixed
 * rate up to a maximum capacity. Each operation attempt consumes one token; if no
 * token is available, the attempt is rejected.
 * </p>
 * <p>
 * Thread-safe. Suitable for guarding resource access in concurrent environments.
 * </p>
 *
 * @since 4.6
 */
public class RateLimiter {

    /** Nanoseconds per second, used for time calculations. */
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final double permitsPerSecond;
    private final double maxPermits;
    private final AtomicReference<State> state;

    /**
     * Constructs a new {@code RateLimiter} with the specified rate.
     *
     * @param permitsPerSecond the steady-state rate at which permits are generated
     * @throws IllegalArgumentException if {@code permitsPerSecond} is not positive
     */
    public RateLimiter(final double permitsPerSecond) {
        this(permitsPerSecond, permitsPerSecond);
    }

    /**
     * Constructs a new {@code RateLimiter} with the specified steady rate
     * and burst capacity.
     *
     * @param permitsPerSecond the steady-state rate at which permits are generated
     * @param maxBurstPermits  the maximum number of permits that can be stored
     * @throws IllegalArgumentException if either parameter is not positive
     */
    public RateLimiter(final double permitsPerSecond, final double maxBurstPermits) {
        if (permitsPerSecond <= 0.0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive: " + permitsPerSecond);
        }
        if (maxBurstPermits <= 0.0) {
            throw new IllegalArgumentException("maxBurstPermits must be positive: " + maxBurstPermits);
        }
        this.permitsPerSecond = permitsPerSecond;
        this.maxPermits = Math.max(permitsPerSecond, maxBurstPermits);
        this.state = new AtomicReference<>(new State(this.maxPermits, System.nanoTime()));
    }

    /**
     * Attempts to acquire a permit. Returns {@code true} if a permit was
     * granted immediately, {@code false} otherwise.
     *
     * @return {@code true} if the operation is allowed, {@code false} if rate-limited
     */
    public boolean tryAcquire() {
        while (true) {
            final State current = state.get();
            final long now = System.nanoTime();
            final double refill = (now - current.lastRefillNanos) / (double) NANOS_PER_SECOND * permitsPerSecond;
            final double newPermits = Math.min(maxPermits, current.permits + refill);
            if (newPermits < 1.0) {
                return false;
            }
            final State updated = new State(newPermits - 1.0, now);
            if (state.compareAndSet(current, updated)) {
                return true;
            }
        }
    }

    /**
     * Returns the number of permits currently available.
     *
     * @return the current available permits
     */
    public double availablePermits() {
        final State current = state.get();
        final long now = System.nanoTime();
        final double refill = (now - current.lastRefillNanos) / (double) NANOS_PER_SECOND * permitsPerSecond;
        return Math.min(maxPermits, current.permits + refill);
    }

    /**
     * Returns the configured steady-state permit rate.
     *
     * @return permits per second
     */
    public double getRate() {
        return permitsPerSecond;
    }

    /**
     * Immutable snapshot of the rate limiter internal state.
     */
    private static final class State {
        final double permits;
        final long lastRefillNanos;

        State(final double permits, final long lastRefillNanos) {
            this.permits = permits;
            this.lastRefillNanos = lastRefillNanos;
        }
    }
}