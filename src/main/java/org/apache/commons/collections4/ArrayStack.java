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

import java.util.ArrayList;
import java.util.EmptyStackException;

/**
 * An implementation of the {@link java.util.Stack} API that is based on an
 * {@code ArrayList} instead of a {@code Vector}, so it is not
 * synchronized to protect against multithreaded access.  The implementation
 * is therefore operates faster in environments where you do not need to
 * worry about multiple thread contention.
 * <p>
 * The removal order of an {@code ArrayStack} is based on insertion
 * order: The most recently added element is removed first.  The iteration
 * order is <em>not</em> the same as the removal order.  The iterator returns
 * elements from the bottom up.
 * </p>
 * <p>
 * Unlike {@code Stack}, {@code ArrayStack} accepts null entries.
 * <p>
 * <strong>Note:</strong> From version 4.0 onwards, this class does not implement the
 * removed {@code Buffer} interface anymore.
 * </p>
 * <p>
 * <strong>Resilience Features (since 4.6):</strong> This class supports optional
 * request rate limiting and circuit breaker mechanisms to improve system stability
 * under high load or failure conditions. Use {@link ResilienceConfig} to configure
 * these features, or use the convenience factory methods in {@link ArrayStacks}.
 * </p>
 *
 * @param <E> the type of elements in this list
 * @see java.util.Stack
 * @since 1.0
 * @deprecated Use {@link java.util.ArrayDeque} instead (available from Java 1.6)
 */
@Deprecated
public class ArrayStack<E> extends ArrayList<E> {

    private static final long serialVersionUID = 2130079159931574599L;

    private transient RateLimiter rateLimiter;
    private transient CircuitBreaker circuitBreaker;
    private transient boolean resilienceEnabled;

    /**
     * Constructs a new empty {@code ArrayStack}. The initial size
     * is controlled by {@code ArrayList} and is currently 10.
     */
    public ArrayStack() {
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size.
     *
     * @param initialSize  the initial size to use
     * @throws IllegalArgumentException  if the specified initial size
     *  is negative
     */
    public ArrayStack(final int initialSize) {
        super(initialSize);
    }

    /**
     * Constructs a new {@code ArrayStack} with resilience configuration.
     *
     * @param config  the resilience configuration containing rate limiter
     *                and circuit breaker settings
     * @since 4.6
     */
    public ArrayStack(final ResilienceConfig config) {
        this();
        applyResilienceConfig(config);
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size and resilience configuration.
     *
     * @param initialSize  the initial size to use
     * @param config       the resilience configuration
     * @throws IllegalArgumentException  if the specified initial size is negative
     * @since 4.6
     */
    public ArrayStack(final int initialSize, final ResilienceConfig config) {
        super(initialSize);
        applyResilienceConfig(config);
    }

    /**
     * Return {@code true} if this stack is currently empty.
     * <p>
     * This method exists for compatibility with {@link java.util.Stack}.
     * New users of this class should use {@code isEmpty} instead.
     * </p>
     *
     * @return true if the stack is currently empty
     */
    public boolean empty() {
        return isEmpty();
    }

    /**
     * Returns the top item off of this stack without removing it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     * @throws RateLimitExceededException  if the rate limit has been exceeded
     * @throws CircuitBreakerOpenException if the circuit breaker is open
     */
    public E peek() throws EmptyStackException {
        checkResilience();
        try {
            final int n = size();
            if (n <= 0) {
                throw new EmptyStackException();
            }
            final E result = get(n - 1);
            recordSuccess();
            return result;
        } catch (final Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Returns the n'th item down (zero-relative) from the top of this
     * stack without removing it.
     *
     * @param n  the number of items down to go
     * @return the n'th item on the stack, zero relative
     * @throws EmptyStackException  if there are not enough items on the
     *  stack to satisfy this request
     * @throws RateLimitExceededException  if the rate limit has been exceeded
     * @throws CircuitBreakerOpenException if the circuit breaker is open
     */
    public E peek(final int n) throws EmptyStackException {
        checkResilience();
        try {
            final int m = size() - n - 1;
            if (m < 0) {
                throw new EmptyStackException();
            }
            final E result = get(m);
            recordSuccess();
            return result;
        } catch (final Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Pops the top item off of this stack and return it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     * @throws RateLimitExceededException  if the rate limit has been exceeded
     * @throws CircuitBreakerOpenException if the circuit breaker is open
     */
    public E pop() throws EmptyStackException {
        checkResilience();
        try {
            final int n = size();
            if (n <= 0) {
                throw new EmptyStackException();
            }
            final E result = remove(n - 1);
            recordSuccess();
            return result;
        } catch (final Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Pushes a new item onto the top of this stack. The pushed item is also
     * returned. This is equivalent to calling {@code add}.
     *
     * @param item  the item to be added
     * @return the item just pushed
     * @throws RateLimitExceededException  if the rate limit has been exceeded
     * @throws CircuitBreakerOpenException if the circuit breaker is open
     */
    public E push(final E item) {
        checkResilience();
        try {
            add(item);
            recordSuccess();
            return item;
        } catch (final Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Returns the one-based position of the distance from the top that the
     * specified object exists on this stack, where the top-most element is
     * considered to be at distance {@code 1}.  If the object is not
     * present on the stack, return {@code -1} instead.  The
     * {@code equals()} method is used to compare to the items
     * in this stack.
     *
     * @param object  the object to be searched for
     * @return the 1-based depth into the stack of the object, or -1 if not found
     * @throws RateLimitExceededException  if the rate limit has been exceeded
     * @throws CircuitBreakerOpenException if the circuit breaker is open
     */
    public int search(final Object object) {
        checkResilience();
        try {
            int i = size() - 1;
            int n = 1;
            while (i >= 0) {
                final Object current = get(i);
                if (object == null && current == null ||
                    object != null && object.equals(current)) {
                    recordSuccess();
                    return n;
                }
                i--;
                n++;
            }
            recordSuccess();
            return -1;
        } catch (final Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Applies a resilience configuration to this stack.
     *
     * @param config  the resilience configuration to apply
     * @since 4.6
     */
    public void applyResilienceConfig(final ResilienceConfig config) {
        if (config == null) {
            this.resilienceEnabled = false;
            this.rateLimiter = null;
            this.circuitBreaker = null;
            return;
        }
        this.resilienceEnabled = true;
        if (config.getRateLimiterConfig() != null) {
            final RateLimiterConfig rc = config.getRateLimiterConfig();
            this.rateLimiter = new RateLimiter(rc.getMaxPermits(), rc.getWindowMillis());
        }
        if (config.getCircuitBreakerConfig() != null) {
            final CircuitBreakerConfig cc = config.getCircuitBreakerConfig();
            this.circuitBreaker = new CircuitBreaker(
                cc.getFailureThreshold(),
                cc.getMonitoringWindowMillis(),
                cc.getCooldownMillis(),
                cc.getHalfOpenMaxAttempts()
            );
        }
    }

    /**
     * Returns whether resilience features are enabled on this stack.
     *
     * @return true if rate limiting or circuit breaker is enabled
     * @since 4.6
     */
    public boolean isResilienceEnabled() {
        return resilienceEnabled;
    }

    /**
     * Returns the rate limiter instance, or null if not configured.
     *
     * @return the rate limiter or null
     * @since 4.6
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    /**
     * Returns the circuit breaker instance, or null if not configured.
     *
     * @return the circuit breaker or null
     * @since 4.6
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Disables all resilience features on this stack.
     *
     * @since 4.6
     */
    public void disableResilience() {
        this.resilienceEnabled = false;
        this.rateLimiter = null;
        this.circuitBreaker = null;
    }

    private void checkResilience() {
        if (!resilienceEnabled) {
            return;
        }
        if (rateLimiter != null && !rateLimiter.tryAcquire()) {
            throw new RateLimitExceededException(
                "Rate limit exceeded: " + rateLimiter.getMaxPermits() +
                " permits per " + rateLimiter.getWindowMillis() + "ms"
            );
        }
        if (circuitBreaker != null && !circuitBreaker.allowRequest()) {
            throw new CircuitBreakerOpenException(
                "Circuit breaker is open: " + circuitBreaker.getFailureCount() +
                " failures (threshold: " + circuitBreaker.getFailureThreshold() + ")"
            );
        }
    }

    private void recordSuccess() {
        if (circuitBreaker != null) {
            circuitBreaker.recordSuccess();
        }
    }

    private void recordFailure() {
        if (circuitBreaker != null) {
            circuitBreaker.recordFailure();
        }
    }

    /**
     * Configuration holder for resilience features.
     *
     * @since 4.6
     */
    public static class ResilienceConfig {

        private RateLimiterConfig rateLimiterConfig;
        private CircuitBreakerConfig circuitBreakerConfig;

        public ResilienceConfig() {
        }

        public RateLimiterConfig getRateLimiterConfig() {
            return rateLimiterConfig;
        }

        public void setRateLimiterConfig(final RateLimiterConfig rateLimiterConfig) {
            this.rateLimiterConfig = rateLimiterConfig;
        }

        public CircuitBreakerConfig getCircuitBreakerConfig() {
            return circuitBreakerConfig;
        }

        public void setCircuitBreakerConfig(final CircuitBreakerConfig circuitBreakerConfig) {
            this.circuitBreakerConfig = circuitBreakerConfig;
        }

        public ResilienceConfig withRateLimiter(final long maxPermits, final long windowMillis) {
            this.rateLimiterConfig = new RateLimiterConfig(maxPermits, windowMillis);
            return this;
        }

        public ResilienceConfig withCircuitBreaker(final int failureThreshold,
                                                    final long monitoringWindowMillis,
                                                    final long cooldownMillis) {
            this.circuitBreakerConfig = new CircuitBreakerConfig(
                failureThreshold, monitoringWindowMillis, cooldownMillis
            );
            return this;
        }

        public ResilienceConfig withCircuitBreaker(final int failureThreshold,
                                                    final long monitoringWindowMillis,
                                                    final long cooldownMillis,
                                                    final int halfOpenMaxAttempts) {
            this.circuitBreakerConfig = new CircuitBreakerConfig(
                failureThreshold, monitoringWindowMillis, cooldownMillis, halfOpenMaxAttempts
            );
            return this;
        }
    }

    /**
     * Configuration for the rate limiter.
     *
     * @since 4.6
     */
    public static class RateLimiterConfig {

        private final long maxPermits;
        private final long windowMillis;

        public RateLimiterConfig(final long maxPermits, final long windowMillis) {
            this.maxPermits = maxPermits;
            this.windowMillis = windowMillis;
        }

        public long getMaxPermits() {
            return maxPermits;
        }

        public long getWindowMillis() {
            return windowMillis;
        }
    }

    /**
     * Configuration for the circuit breaker.
     *
     * @since 4.6
     */
    public static class CircuitBreakerConfig {

        private final int failureThreshold;
        private final long monitoringWindowMillis;
        private final long cooldownMillis;
        private final int halfOpenMaxAttempts;

        public CircuitBreakerConfig(final int failureThreshold,
                                    final long monitoringWindowMillis,
                                    final long cooldownMillis) {
            this(failureThreshold, monitoringWindowMillis, cooldownMillis, 1);
        }

        public CircuitBreakerConfig(final int failureThreshold,
                                    final long monitoringWindowMillis,
                                    final long cooldownMillis,
                                    final int halfOpenMaxAttempts) {
            this.failureThreshold = failureThreshold;
            this.monitoringWindowMillis = monitoringWindowMillis;
            this.cooldownMillis = cooldownMillis;
            this.halfOpenMaxAttempts = halfOpenMaxAttempts;
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

        public int getHalfOpenMaxAttempts() {
            return halfOpenMaxAttempts;
        }
    }

    /**
     * Exception thrown when the rate limit has been exceeded.
     *
     * @since 4.6
     */
    public static class RateLimitExceededException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public RateLimitExceededException(final String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when the circuit breaker is open.
     *
     * @since 4.6
     */
    public static class CircuitBreakerOpenException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CircuitBreakerOpenException(final String message) {
            super(message);
        }
    }
}
