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

    private transient ArrayUtils.RateLimiter rateLimiter;
    private transient ArrayUtils.CircuitBreaker circuitBreaker;

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
     * Enables rate limiting for this stack.
     *
     * @param maxRequests  the maximum number of requests allowed in the time window
     * @param windowMillis the time window in milliseconds
     */
    public void enableRateLimiting(final int maxRequests, final long windowMillis) {
        this.rateLimiter = new ArrayUtils.RateLimiter(maxRequests, windowMillis);
    }

    /**
     * Enables circuit breaker for this stack.
     *
     * @param failureThreshold   the number of consecutive failures before opening the circuit
     * @param resetTimeoutMillis the timeout in milliseconds before attempting to reset the circuit
     */
    public void enableCircuitBreaker(final int failureThreshold, final long resetTimeoutMillis) {
        this.circuitBreaker = new ArrayUtils.CircuitBreaker(failureThreshold, resetTimeoutMillis);
    }

    private void checkRateLimit() {
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }
    }

    private void checkCircuitBreaker() {
        if (circuitBreaker != null) {
            circuitBreaker.check();
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
     * Return {@code true} if this stack is currently empty.
     * <p>
     * This method exists for compatibility with {@link java.util.Stack}.
     * New users of this class should use {@code isEmpty} instead.
     * </p>
     *
     * @return true if the stack is currently empty
     */
    public boolean empty() {
        checkRateLimit();
        checkCircuitBreaker();
        final boolean result = isEmpty();
        recordSuccess();
        return result;
    }

    /**
     * Returns the top item off of this stack without removing it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     */
    public E peek() throws EmptyStackException {
        checkRateLimit();
        checkCircuitBreaker();
        final int n = size();
        if (n <= 0) {
            recordFailure();
            throw new EmptyStackException();
        }
        final E result = get(n - 1);
        recordSuccess();
        return result;
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
        checkRateLimit();
        checkCircuitBreaker();
        final int m = size() - n - 1;
        if (m < 0) {
            recordFailure();
            throw new EmptyStackException();
        }
        final E result = get(m);
        recordSuccess();
        return result;
    }

    /**
     * Pops the top item off of this stack and return it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     */
    public E pop() throws EmptyStackException {
        checkRateLimit();
        checkCircuitBreaker();
        final int n = size();
        if (n <= 0) {
            recordFailure();
            throw new EmptyStackException();
        }
        final E result = remove(n - 1);
        recordSuccess();
        return result;
    }

    /**
     * Pushes a new item onto the top of this stack. The pushed item is also
     * returned. This is equivalent to calling {@code add}.
     *
     * @param item  the item to be added
     * @return the item just pushed
     */
    public E push(final E item) {
        checkRateLimit();
        checkCircuitBreaker();
        add(item);
        recordSuccess();
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
        checkRateLimit();
        checkCircuitBreaker();
        int i = size() - 1;        // Current index
        int n = 1;                 // Current distance
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
    }

}
