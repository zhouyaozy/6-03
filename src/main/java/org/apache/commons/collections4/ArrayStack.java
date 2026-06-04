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
 * </p>
 * <p>
 * <strong>Note:</strong> From version 4.0 onwards, this class does not implement the
 * removed {@code Buffer} interface anymore.
 * </p>
 * <p>
 * Since version 4.6, this class supports optional rate limiting and circuit breaker
 * integration. Use {@link #wrap(E, RateLimiter, CircuitBreaker)} to create a protected
 * instance that guards operations against excessive load and cascading failures.
 * </p>
 *
 * @param <E> the type of elements in this list
 * @see java.util.Stack
 * @see RateLimiter
 * @see CircuitBreaker
 * @since 1.0
 * @deprecated Use {@link java.util.ArrayDeque} instead (available from Java 1.6)
 */
@Deprecated
public class ArrayStack<E> extends ArrayList<E> {

    /** Ensure serialization compatibility */
    private static final long serialVersionUID = 2130079159931574599L;

    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final AtomicLong rejectedCount;
    private final AtomicLong throttledCount;
    private final AtomicLong circuitOpenCount;

    /**
     * Constructs a new empty {@code ArrayStack}. The initial size
     * is controlled by {@code ArrayList} and is currently 10.
     */
    public ArrayStack() {
        this.rateLimiter = null;
        this.circuitBreaker = null;
        this.rejectedCount = new AtomicLong(0);
        this.throttledCount = new AtomicLong(0);
        this.circuitOpenCount = new AtomicLong(0);
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
        this.rateLimiter = null;
        this.circuitBreaker = null;
        this.rejectedCount = new AtomicLong(0);
        this.throttledCount = new AtomicLong(0);
        this.circuitOpenCount = new AtomicLong(0);
    }

    /**
     * Constructs a new empty {@code ArrayStack} with rate limiting and circuit breaker protections.
     *
     * @param rateLimiter    the rate limiter to use, may be {@code null}
     * @param circuitBreaker the circuit breaker to use, may be {@code null}
     * @since 4.6
     */
    public ArrayStack(final RateLimiter rateLimiter, final CircuitBreaker circuitBreaker) {
        this.rateLimiter = rateLimiter;
        this.circuitBreaker = circuitBreaker;
        this.rejectedCount = new AtomicLong(0);
        this.throttledCount = new AtomicLong(0);
        this.circuitOpenCount = new AtomicLong(0);
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size and protections.
     *
     * @param initialSize    the initial size to use
     * @param rateLimiter    the rate limiter to use, may be {@code null}
     * @param circuitBreaker the circuit breaker to use, may be {@code null}
     * @throws IllegalArgumentException if the specified initial size is negative
     * @since 4.6
     */
    public ArrayStack(final int initialSize, final RateLimiter rateLimiter, final CircuitBreaker circuitBreaker) {
        super(initialSize);
        this.rateLimiter = rateLimiter;
        this.circuitBreaker = circuitBreaker;
        this.rejectedCount = new AtomicLong(0);
        this.throttledCount = new AtomicLong(0);
        this.circuitOpenCount = new AtomicLong(0);
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
     */
    public E peek() throws EmptyStackException {
        final int n = size();
        if (n <= 0) {
            throw new EmptyStackException();
        }
        return get(n - 1);
    }

    /**
     * Returns the n'th item down (zero-relative) from the top of this
     * stack without removing it.
     *
     * @param n  the number of items down to go
     * @return the n'th item on the stack, zero relative
     * @throws EmptyStackException  if there are not enough items on the
     *  stack to satisfy this request
     */
    public E peek(final int n) throws EmptyStackException {
        final int m = size() - n - 1;
        if (m < 0) {
            throw new EmptyStackException();
        }
        return get(m);
    }

    /**
     * Pops the top item off of this stack and return it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     */
    public E pop() throws EmptyStackException {
        final int n = size();
        if (n <= 0) {
            throw new EmptyStackException();
        }
        return remove(n - 1);
    }

    /**
     * Pushes a new item onto the top of this stack. The pushed item is also
     * returned. This is equivalent to calling {@code add}.
     *
     * @param item  the item to be added
     * @return the item just pushed
     */
    public E push(final E item) {
        add(item);
        return item;
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
     */
    public int search(final Object object) {
        int i = size() - 1;        // Current index
        int n = 1;                 // Current distance
        while (i >= 0) {
            final Object current = get(i);
            if (object == null && current == null ||
                object != null && object.equals(current)) {
                return n;
            }
            i--;
            n++;
        }
        return -1;
    }

    /**
     * Pushes a new item onto the top of this stack with rate limiting and circuit breaker checks.
     * This is equivalent to calling {@link #push(Object)} when no protections are configured.
     * <p>
     * When a {@link RateLimiter} is configured, the call is subject to rate limiting.
     * When a {@link CircuitBreaker} is configured, the call may be rejected if the circuit is open.
     * </p>
     *
     * @param item  the item to be added
     * @return the item just pushed, or {@code null} if the operation was rejected
     * @since 4.6
     */
    public E pushProtected(final E item) {
        if (!allowRequest()) {
            return null;
        }
        return push(item);
    }

    /**
     * Pops the top item off of this stack with rate limiting and circuit breaker checks.
     *
     * @return the top item on the stack, or {@code null} if the operation was rejected
     * @throws EmptyStackException  if the stack is empty
     * @since 4.6
     */
    public E popProtected() throws EmptyStackException {
        if (!allowRequest()) {
            return null;
        }
        return pop();
    }

    /**
     * Returns the top item without removing it, with rate limiting and circuit breaker checks.
     *
     * @return the top item on the stack, or {@code null} if the operation was rejected
     * @throws EmptyStackException  if the stack is empty
     * @since 4.6
     */
    public E peekProtected() throws EmptyStackException {
        if (!allowRequest()) {
            return null;
        }
        return peek();
    }

    /**
     * Returns the n'th item down from the top with rate limiting and circuit breaker checks.
     *
     * @param n  the number of items down to go
     * @return the n'th item on the stack, zero relative, or {@code null} if rejected
     * @throws EmptyStackException  if there are not enough items on the stack
     * @since 4.6
     */
    public E peekProtected(final int n) throws EmptyStackException {
        if (!allowRequest()) {
            return null;
        }
        return peek(n);
    }

    /**
     * Checks whether the current operation is allowed by the configured rate limiter
     * and circuit breaker. Increments the appropriate rejection counters.
     *
     * @return {@code true} if the request is allowed to proceed
     * @since 4.6
     */
    public boolean allowRequest() {
        if (circuitBreaker != null && !circuitBreaker.allowRequest()) {
            circuitOpenCount.incrementAndGet();
            rejectedCount.incrementAndGet();
            return false;
        }
        if (rateLimiter != null && !rateLimiter.tryAcquire()) {
            throttledCount.incrementAndGet();
            rejectedCount.incrementAndGet();
            return false;
        }
        return true;
    }

    /**
     * Notifies the circuit breaker of a successful operation.
     * Call this after a protected operation completes successfully.
     *
     * @since 4.6
     */
    public void recordSuccess() {
        if (circuitBreaker != null) {
            circuitBreaker.recordSuccess();
        }
    }

    /**
     * Notifies the circuit breaker of a failed operation.
     * Call this after a protected operation throws an exception or fails.
     *
     * @since 4.6
     */
    public void recordFailure() {
        if (circuitBreaker != null) {
            circuitBreaker.recordFailure();
        }
    }

    /**
     * Returns the configured rate limiter, or {@code null} if none is configured.
     *
     * @return the rate limiter, or {@code null}
     * @since 4.6
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    /**
     * Returns the configured circuit breaker, or {@code null} if none is configured.
     *
     * @return the circuit breaker, or {@code null}
     * @since 4.6
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Returns the total number of rejected requests due to rate limiting or circuit breaker.
     *
     * @return the rejected request count
     * @since 4.6
     */
    public long getRejectedCount() {
        return rejectedCount.get();
    }

    /**
     * Returns the number of requests throttled by the rate limiter.
     *
     * @return the throttled request count
     * @since 4.6
     */
    public long getThrottledCount() {
        return throttledCount.get();
    }

    /**
     * Returns the number of requests rejected due to an open circuit.
     *
     * @return the circuit-open rejection count
     * @since 4.6
     */
    public long getCircuitOpenCount() {
        return circuitOpenCount.get();
    }

    /**
     * Resets all rejection counters to zero.
     *
     * @since 4.6
     */
    public void resetCounters() {
        rejectedCount.set(0);
        throttledCount.set(0);
        circuitOpenCount.set(0);
    }

    /**
     * Returns a summary of the stack's protection status.
     *
     * @return a string summarizing rate limiter and circuit breaker status
     * @since 4.6
     */
    public String getStatus() {
        final StringBuilder sb = new StringBuilder("ArrayStack[");
        sb.append("size=").append(size());
        sb.append(", rejected=").append(rejectedCount.get());
        sb.append(", throttled=").append(throttledCount.get());
        sb.append(", circuitOpen=").append(circuitOpenCount.get());
        if (rateLimiter != null) {
            sb.append(", rateLimit=").append(rateLimiter.getRate()).append("/s");
            sb.append(", available=").append(String.format("%.1f", rateLimiter.availablePermits()));
        }
        if (circuitBreaker != null) {
            sb.append(", circuitState=").append(circuitBreaker.getState());
            sb.append(", circuitFailures=").append(circuitBreaker.getFailureCount());
            sb.append(", circuitSuccesses=").append(circuitBreaker.getSuccessCount());
        }
        sb.append("]");
        return sb.toString();
    }

}
