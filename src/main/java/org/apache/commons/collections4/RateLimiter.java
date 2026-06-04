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

/**
 * A thread-safe token bucket rate limiter that controls the rate of operations.
 * <p>
 * This implementation uses a sliding window approach to track request rates
 * and allows configuration of both the maximum permits per window and the
 * window duration.
 * </p>
 *
 * @since 4.6
 */
public class RateLimiter {

    private final long maxPermits;
    private final long windowMillis;
    private final AtomicLong counter;
    private volatile long windowStart;

    /**
     * Constructs a RateLimiter with the specified maximum permits and window duration.
     *
     * @param maxPermits    the maximum number of permits allowed per window
     * @param windowMillis  the window duration in milliseconds
     * @throws IllegalArgumentException if maxPermits is less than or equal to zero
     *                                  or windowMillis is less than or equal to zero
     */
    public RateLimiter(final long maxPermits, final long windowMillis) {
        if (maxPermits <= 0) {
            throw new IllegalArgumentException("maxPermits must be greater than zero");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis must be greater than zero");
        }
        this.maxPermits = maxPermits;
        this.windowMillis = windowMillis;
        this.counter = new AtomicLong(0);
        this.windowStart = System.currentTimeMillis();
    }

    /**
     * Attempts to acquire a permit.
     *
     * @return true if a permit was acquired, false if the rate limit has been exceeded
     */
    public synchronized boolean tryAcquire() {
        final long now = System.currentTimeMillis();
        if (now - windowStart >= windowMillis) {
            counter.set(0);
            windowStart = now;
        }
        final long currentCount = counter.get();
        if (currentCount < maxPermits) {
            counter.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Acquires a permit, blocking until one is available.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            Thread.sleep(1);
        }
    }

    /**
     * Returns the maximum number of permits allowed per window.
     *
     * @return the max permits
     */
    public long getMaxPermits() {
        return maxPermits;
    }

    /**
     * Returns the window duration in milliseconds.
     *
     * @return the window duration in millis
     */
    public long getWindowMillis() {
        return windowMillis;
    }

    /**
     * Returns the current number of used permits in the current window.
     *
     * @return the current permit count
     */
    public long getCurrentCount() {
        return counter.get();
    }

    /**
     * Returns the number of remaining permits in the current window.
     *
     * @return the remaining permits
     */
    public long getRemainingPermits() {
        final long now = System.currentTimeMillis();
        if (now - windowStart >= windowMillis) {
            return maxPermits;
        }
        return Math.max(0, maxPermits - counter.get());
    }

    /**
     * Resets the rate limiter, clearing the current window.
     */
    public synchronized void reset() {
        counter.set(0);
        windowStart = System.currentTimeMillis();
    }
}
