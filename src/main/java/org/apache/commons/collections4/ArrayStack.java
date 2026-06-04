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
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

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

    public static class CircuitBreakerOpenException extends RejectedExecutionException {

        private static final long serialVersionUID = 1L;

        CircuitBreakerOpenException(final String message) {
            super(message);
        }
    }

    public static class RequestRateLimitException extends RejectedExecutionException {

        private static final long serialVersionUID = 1L;

        RequestRateLimitException(final String message) {
            super(message);
        }
    }

    /** Ensure serialization compatibility */
    private static final long serialVersionUID = 2130079159931574599L;

    private transient int maxRequestsPerWindow;
    private transient long requestWindowMillis;
    private transient long[] requestTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
    private transient int failureThreshold;
    private transient long failureWindowMillis;
    private transient long circuitOpenMillis;
    private transient long[] failureTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
    private transient long circuitOpenUntil = Long.MIN_VALUE;

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

    public void configureCircuitBreaker(final int requestFailureThreshold, final long requestFailureWindowMillis,
            final long requestCircuitOpenMillis) {
        validatePositive(requestFailureThreshold, "requestFailureThreshold");
        validatePositive(requestFailureWindowMillis, "requestFailureWindowMillis");
        validatePositive(requestCircuitOpenMillis, "requestCircuitOpenMillis");
        failureThreshold = requestFailureThreshold;
        failureWindowMillis = requestFailureWindowMillis;
        circuitOpenMillis = requestCircuitOpenMillis;
        failureTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
        circuitOpenUntil = Long.MIN_VALUE;
    }

    public void configureRequestRateLimit(final int requestMaxRequests, final long requestWindowMillis) {
        validatePositive(requestMaxRequests, "requestMaxRequests");
        validatePositive(requestWindowMillis, "requestWindowMillis");
        maxRequestsPerWindow = requestMaxRequests;
        this.requestWindowMillis = requestWindowMillis;
        requestTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public void disableCircuitBreaker() {
        failureThreshold = 0;
        failureWindowMillis = 0;
        circuitOpenMillis = 0;
        failureTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
        circuitOpenUntil = Long.MIN_VALUE;
    }

    public void disableRequestRateLimit() {
        maxRequestsPerWindow = 0;
        requestWindowMillis = 0;
        requestTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
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

    public <T> T executeRequest(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        final long now = currentTimeMillis();
        beforeRequest(now);
        try {
            final T value = supplier.get();
            onRequestSuccess();
            return value;
        } catch (final RuntimeException ex) {
            onRequestFailure(now);
            throw ex;
        } catch (final Error ex) {
            onRequestFailure(now);
            throw ex;
        }
    }

    public boolean isCircuitBreakerOpen() {
        return isCircuitBreakerOpen(currentTimeMillis());
    }

    /**
     * Returns the top item off of this stack without removing it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     */
    public E peek() throws EmptyStackException {
        return executeRequest(this::peekInternal);
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
        return executeRequest(() -> peekInternal(n));
    }

    /**
     * Pops the top item off of this stack and return it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     */
    public E pop() throws EmptyStackException {
        return executeRequest(this::popInternal);
    }

    /**
     * Pushes a new item onto the top of this stack. The pushed item is also
     * returned. This is equivalent to calling {@code add}.
     *
     * @param item  the item to be added
     * @return the item just pushed
     */
    public E push(final E item) {
        return executeRequest(() -> pushInternal(item));
    }

    public void resetResilience() {
        requestTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
        failureTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
        circuitOpenUntil = Long.MIN_VALUE;
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
        return executeRequest(() -> searchInternal(object));
    }

    private void beforeRequest(final long now) {
        if (isCircuitBreakerOpen(now)) {
            throw new CircuitBreakerOpenException("Circuit breaker is open");
        }
        if (circuitOpenUntil != Long.MIN_VALUE) {
            failureTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
            circuitOpenUntil = Long.MIN_VALUE;
        }
        if (maxRequestsPerWindow > 0) {
            requestTimestamps = ArrayUtils.removeValuesBefore(requestTimestamps, now - requestWindowMillis + 1);
            if (requestTimestamps.length >= maxRequestsPerWindow) {
                throw new RequestRateLimitException("Request rate limit exceeded");
            }
            requestTimestamps = ArrayUtils.add(requestTimestamps, now);
        }
    }

    private boolean isCircuitBreakerOpen(final long now) {
        return failureThreshold > 0 && circuitOpenUntil > now;
    }

    private void onRequestFailure(final long now) {
        if (failureThreshold <= 0) {
            return;
        }
        failureTimestamps = ArrayUtils.removeValuesBefore(failureTimestamps, now - failureWindowMillis + 1);
        failureTimestamps = ArrayUtils.add(failureTimestamps, now);
        if (failureTimestamps.length >= failureThreshold) {
            circuitOpenUntil = now + circuitOpenMillis;
        }
    }

    private void onRequestSuccess() {
        if (failureThreshold > 0) {
            failureTimestamps = ArrayUtils.EMPTY_LONG_ARRAY;
            circuitOpenUntil = Long.MIN_VALUE;
        }
    }

    private E peekInternal() throws EmptyStackException {
        final int n = size();
        if (n <= 0) {
            throw new EmptyStackException();
        }
        return super.get(n - 1);
    }

    private E peekInternal(final int n) throws EmptyStackException {
        final int m = size() - n - 1;
        if (m < 0) {
            throw new EmptyStackException();
        }
        return super.get(m);
    }

    private E popInternal() throws EmptyStackException {
        final int n = size();
        if (n <= 0) {
            throw new EmptyStackException();
        }
        return super.remove(n - 1);
    }

    private E pushInternal(final E item) {
        super.add(item);
        return item;
    }

    private int searchInternal(final Object object) {
        int i = size() - 1;
        int n = 1;
        while (i >= 0) {
            final Object current = super.get(i);
            if (object == null && current == null ||
                object != null && object.equals(current)) {
                return n;
            }
            i--;
            n++;
        }
        return -1;
    }

    private void validatePositive(final long value, final String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }

}
