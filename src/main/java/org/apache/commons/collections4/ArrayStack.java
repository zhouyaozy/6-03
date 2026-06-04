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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
 *
 * @param <E> the type of elements in this list
 * @see java.util.Stack
 * @since 1.0
 * @deprecated Use {@link java.util.ArrayDeque} instead (available from Java 1.6)
 */
@Deprecated
public class ArrayStack<E> extends ArrayList<E> {

    /** Ensure serialization compatibility */
    private static final long serialVersionUID = 2130079159931574599L;

    /** Default maximum requests per second for rate limiting */
    private static final int DEFAULT_MAX_REQUESTS_PER_SECOND = 100;

    /** Default failure threshold for circuit breaker (in number of failures) */
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;

    /** Default recovery time for circuit breaker (in milliseconds) */
    private static final long DEFAULT_RECOVERY_TIME_MS = 30000;

    /** Rate limiter configuration */
    private final int maxRequestsPerSecond;
    private final AtomicInteger requestCount;
    private final AtomicLong lastResetTime;

    /** Circuit breaker configuration */
    private final int failureThreshold;
    private final long recoveryTimeMs;
    private final AtomicInteger failureCount;
    private final AtomicLong lastFailureTime;
    private volatile CircuitState circuitState;

    /**
     * Circuit breaker states
     */
    private enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    /**
     * Exception thrown when rate limit is exceeded
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when circuit is open
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    /**
     * Constructs a new empty {@code ArrayStack} with default rate limiting and circuit breaker settings.
     */
    public ArrayStack() {
        this(DEFAULT_MAX_REQUESTS_PER_SECOND, DEFAULT_FAILURE_THRESHOLD, DEFAULT_RECOVERY_TIME_MS);
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size and default rate limiting and circuit breaker settings.
     *
     * @param initialSize  the initial size to use
     * @throws IllegalArgumentException  if the specified initial size is negative
     */
    public ArrayStack(final int initialSize) {
        this(initialSize, DEFAULT_MAX_REQUESTS_PER_SECOND, DEFAULT_FAILURE_THRESHOLD, DEFAULT_RECOVERY_TIME_MS);
    }

    /**
     * Constructs a new empty {@code ArrayStack} with custom rate limiting and circuit breaker settings.
     *
     * @param maxRequestsPerSecond maximum requests per second allowed
     * @param failureThreshold number of failures before circuit opens
     * @param recoveryTimeMs time in milliseconds to wait before attempting recovery
     */
    public ArrayStack(final int maxRequestsPerSecond, final int failureThreshold, final long recoveryTimeMs) {
        super();
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.failureThreshold = failureThreshold;
        this.recoveryTimeMs = recoveryTimeMs;
        this.requestCount = new AtomicInteger(0);
        this.lastResetTime = new AtomicLong(System.currentTimeMillis());
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.circuitState = CircuitState.CLOSED;
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size and custom rate limiting and circuit breaker settings.
     *
     * @param initialSize  the initial size to use
     * @param maxRequestsPerSecond maximum requests per second allowed
     * @param failureThreshold number of failures before circuit opens
     * @param recoveryTimeMs time in milliseconds to wait before attempting recovery
     * @throws IllegalArgumentException  if the specified initial size is negative
     */
    public ArrayStack(final int initialSize, final int maxRequestsPerSecond, final int failureThreshold, final long recoveryTimeMs) {
        super(initialSize);
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.failureThreshold = failureThreshold;
        this.recoveryTimeMs = recoveryTimeMs;
        this.requestCount = new AtomicInteger(0);
        this.lastResetTime = new AtomicLong(System.currentTimeMillis());
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.circuitState = CircuitState.CLOSED;
    }

    /**
     * Checks rate limit and circuit breaker state before processing a request.
     *
     * @throws RateLimitExceededException if rate limit is exceeded
     * @throws CircuitBreakerOpenException if circuit is open
     */
    private void checkRateLimitAndCircuitBreaker() {
        final long now = System.currentTimeMillis();

        checkCircuitBreaker(now);
        checkRateLimit(now);
    }

    /**
     * Checks circuit breaker state.
     */
    private void checkCircuitBreaker(final long now) {
        if (circuitState == CircuitState.OPEN) {
            if (now - lastFailureTime.get() > recoveryTimeMs) {
                circuitState = CircuitState.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker is open, try again later");
            }
        }
    }

    /**
     * Checks rate limit.
     */
    private void checkRateLimit(final long now) {
        final long lastReset = lastResetTime.get();
        if (now - lastReset >= TimeUnit.SECONDS.toMillis(1)) {
            requestCount.set(0);
            lastResetTime.set(now);
        }

        if (requestCount.incrementAndGet() > maxRequestsPerSecond) {
            requestCount.decrementAndGet();
            throw new RateLimitExceededException("Rate limit exceeded: " + maxRequestsPerSecond + " requests per second");
        }
    }

    /**
     * Records a successful operation.
     */
    private void recordSuccess() {
        if (circuitState == CircuitState.HALF_OPEN) {
            circuitState = CircuitState.CLOSED;
            failureCount.set(0);
        }
    }

    /**
     * Records a failed operation.
     */
    private void recordFailure() {
        final int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        if (circuitState == CircuitState.HALF_OPEN) {
            circuitState = CircuitState.OPEN;
        } else if (circuitState == CircuitState.CLOSED && failures >= failureThreshold) {
            circuitState = CircuitState.OPEN;
        }
    }

    /**
     * Resets the circuit breaker to CLOSED state.
     */
    public void resetCircuitBreaker() {
        circuitState = CircuitState.CLOSED;
        failureCount.set(0);
    }

    /**
     * Returns the current circuit state.
     *
     * @return current circuit state
     */
    public String getCircuitState() {
        return circuitState.name();
    }

    /**
     * Returns the current request count in this second.
     *
     * @return current request count
     */
    public int getCurrentRequestCount() {
        return requestCount.get();
    }

    /**
     * Returns the current failure count.
     *
     * @return current failure count
     */
    public int getFailureCount() {
        return failureCount.get();
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
        checkRateLimitAndCircuitBreaker();
        try {
            final boolean result = isEmpty();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Returns the top item off of this stack without removing it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     * @throws RateLimitExceededException if rate limit is exceeded
     * @throws CircuitBreakerOpenException if circuit is open
     */
    public E peek() throws EmptyStackException {
        checkRateLimitAndCircuitBreaker();
        try {
            final int n = size();
            if (n <= 0) {
                recordFailure();
                throw new EmptyStackException();
            }
            final E result = get(n - 1);
            recordSuccess();
            return result;
        } catch (EmptyStackException e) {
            recordFailure();
            throw e;
        } catch (Exception e) {
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
     * @throws RateLimitExceededException if rate limit is exceeded
     * @throws CircuitBreakerOpenException if circuit is open
     */
    public E peek(final int n) throws EmptyStackException {
        checkRateLimitAndCircuitBreaker();
        try {
            final int m = size() - n - 1;
            if (m < 0) {
                recordFailure();
                throw new EmptyStackException();
            }
            final E result = get(m);
            recordSuccess();
            return result;
        } catch (EmptyStackException e) {
            recordFailure();
            throw e;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Pops the top item off of this stack and return it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     * @throws RateLimitExceededException if rate limit is exceeded
     * @throws CircuitBreakerOpenException if circuit is open
     */
    public E pop() throws EmptyStackException {
        checkRateLimitAndCircuitBreaker();
        try {
            final int n = size();
            if (n <= 0) {
                recordFailure();
                throw new EmptyStackException();
            }
            final E result = remove(n - 1);
            recordSuccess();
            return result;
        } catch (EmptyStackException e) {
            recordFailure();
            throw e;
        } catch (Exception e) {
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
     * @throws RateLimitExceededException if rate limit is exceeded
     * @throws CircuitBreakerOpenException if circuit is open
     */
    public E push(final E item) {
        checkRateLimitAndCircuitBreaker();
        try {
            add(item);
            recordSuccess();
            return item;
        } catch (Exception e) {
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
     * @throws RateLimitExceededException if rate limit is exceeded
     * @throws CircuitBreakerOpenException if circuit is open
     */
    public int search(final Object object) {
        checkRateLimitAndCircuitBreaker();
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
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

}
