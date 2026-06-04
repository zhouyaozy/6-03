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

/**
 * Utility class providing factory methods and helpers for creating
 * {@link ArrayStack} instances with resilience features.
 * <p>
 * This class provides convenient factory methods for creating ArrayStack
 * instances pre-configured with rate limiting and circuit breaker mechanisms.
 * </p>
 *
 * @since 4.6
 */
public final class ArrayStacks {

    private ArrayStacks() {
    }

    /**
     * Creates a new empty ArrayStack with resilience disabled.
     *
     * @param <E> the type of elements
     * @return a new ArrayStack instance
     */
    public static <E> ArrayStack<E> create() {
        return new ArrayStack<>();
    }

    /**
     * Creates a new empty ArrayStack with the specified initial capacity.
     *
     * @param <E>          the type of elements
     * @param initialSize  the initial capacity
     * @return a new ArrayStack instance
     */
    public static <E> ArrayStack<E> create(final int initialSize) {
        return new ArrayStack<>(initialSize);
    }

    /**
     * Creates a new ArrayStack with rate limiting enabled.
     *
     * @param <E>          the type of elements
     * @param maxPermits   the maximum number of operations allowed per window
     * @param windowMillis the window duration in milliseconds
     * @return a new ArrayStack with rate limiting configured
     */
    public static <E> ArrayStack<E> withRateLimiting(final long maxPermits,
                                                     final long windowMillis) {
        final ArrayStack.ResilienceConfig config = new ArrayStack.ResilienceConfig()
            .withRateLimiter(maxPermits, windowMillis);
        return new ArrayStack<>(config);
    }

    /**
     * Creates a new ArrayStack with circuit breaker enabled.
     *
     * @param <E>                    the type of elements
     * @param failureThreshold       the number of failures before opening the circuit
     * @param monitoringWindowMillis the monitoring window duration
     * @param cooldownMillis         the cooldown duration before trying half-open
     * @return a new ArrayStack with circuit breaker configured
     */
    public static <E> ArrayStack<E> withCircuitBreaker(final int failureThreshold,
                                                        final long monitoringWindowMillis,
                                                        final long cooldownMillis) {
        final ArrayStack.ResilienceConfig config = new ArrayStack.ResilienceConfig()
            .withCircuitBreaker(failureThreshold, monitoringWindowMillis, cooldownMillis);
        return new ArrayStack<>(config);
    }

    /**
     * Creates a new ArrayStack with both rate limiting and circuit breaker enabled.
     *
     * @param <E>                    the type of elements
     * @param maxPermits             the maximum number of operations allowed per window
     * @param windowMillis           the window duration in milliseconds
     * @param failureThreshold       the number of failures before opening the circuit
     * @param monitoringWindowMillis the monitoring window duration
     * @param cooldownMillis         the cooldown duration before trying half-open
     * @return a new ArrayStack with both features configured
     */
    public static <E> ArrayStack<E> withResilience(final long maxPermits,
                                                    final long windowMillis,
                                                    final int failureThreshold,
                                                    final long monitoringWindowMillis,
                                                    final long cooldownMillis) {
        final ArrayStack.ResilienceConfig config = new ArrayStack.ResilienceConfig()
            .withRateLimiter(maxPermits, windowMillis)
            .withCircuitBreaker(failureThreshold, monitoringWindowMillis, cooldownMillis);
        return new ArrayStack<>(config);
    }

    /**
     * Creates a new ArrayStack with full resilience configuration.
     *
     * @param <E>                    the type of elements
     * @param maxPermits             the maximum number of operations allowed per window
     * @param windowMillis           the window duration in milliseconds
     * @param failureThreshold       the number of failures before opening the circuit
     * @param monitoringWindowMillis the monitoring window duration
     * @param cooldownMillis         the cooldown duration before trying half-open
     * @param halfOpenMaxAttempts    the maximum probe attempts in half-open state
     * @return a new ArrayStack with full resilience configured
     */
    public static <E> ArrayStack<E> withFullResilience(final long maxPermits,
                                                        final long windowMillis,
                                                        final int failureThreshold,
                                                        final long monitoringWindowMillis,
                                                        final long cooldownMillis,
                                                        final int halfOpenMaxAttempts) {
        final ArrayStack.ResilienceConfig config = new ArrayStack.ResilienceConfig()
            .withRateLimiter(maxPermits, windowMillis)
            .withCircuitBreaker(failureThreshold, monitoringWindowMillis, cooldownMillis, halfOpenMaxAttempts);
        return new ArrayStack<>(config);
    }

    /**
     * Creates a new ArrayStack from the given elements with resilience disabled.
     *
     * @param <E>      the type of elements
     * @param elements the elements to push onto the stack (in order)
     * @return a new ArrayStack containing the given elements
     */
    @SafeVarargs
    public static <E> ArrayStack<E> of(final E... elements) {
        final ArrayStack<E> stack = new ArrayStack<>(elements.length);
        for (final E element : elements) {
            stack.push(element);
        }
        return stack;
    }

    /**
     * Creates a new ArrayStack from the given elements with rate limiting enabled.
     *
     * @param <E>          the type of elements
     * @param maxPermits   the maximum number of operations allowed per window
     * @param windowMillis the window duration in milliseconds
     * @param elements     the elements to push onto the stack (in order)
     * @return a new ArrayStack with rate limiting and the given elements
     */
    @SafeVarargs
    public static <E> ArrayStack<E> ofWithRateLimiting(final long maxPermits,
                                                        final long windowMillis,
                                                        final E... elements) {
        final ArrayStack<E> stack = withRateLimiting(maxPermits, windowMillis);
        for (final E element : elements) {
            stack.push(element);
        }
        return stack;
    }

    /**
     * Creates a pre-configured resilience configuration builder.
     *
     * @return a new ResilienceConfig instance
     */
    public static ArrayStack.ResilienceConfig resilienceConfig() {
        return new ArrayStack.ResilienceConfig();
    }
}
