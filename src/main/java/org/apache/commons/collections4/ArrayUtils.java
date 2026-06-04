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
 * <p>
 * Operations on arrays, primitive arrays (like {@code int[]}) and primitive wrapper arrays (like {@code Integer[]}).
 * </p>
 * <p>
 * This class tries to handle {@code null} input gracefully. An exception will not be thrown for a {@code null} array input. However, an Object array that
 * contains a {@code null} element may throw an exception. Each method documents its behavior.
 * </p>
 * <p>
 * Package private, might move to an internal package if this needs to be public.
 * </p>
 * <p>
 * #ThreadSafe#
 * </p>
 *
 * @since 4.2 (Copied from Apache Commons Lang.)
 */
final class ArrayUtils {

    /**
     * <p>
     * Checks if the object is in the given array.
     * </p>
     * <p>
     * The method returns {@code false} if a {@code null} array is passed in.
     * </p>
     *
     * @param array        the array to search, may be {@code null}.
     * @param objectToFind the object to find, may be {@code null}.
     * @return {@code true} if the array contains the object.
     */
    static boolean contains(final Object[] array, final Object objectToFind) {
        return indexOf(array, objectToFind) != CollectionUtils.INDEX_NOT_FOUND;
    }

    /**
     * <p>
     * Finds the index of the given object in the array starting at the given index.
     * </p>
     * <p>
     * This method returns {@link CollectionUtils#INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the array length will return {@link CollectionUtils#INDEX_NOT_FOUND} ({@code -1}).
     * </p>
     *
     * @param array        the array to search for the object, may be {@code null}.
     * @param objectToFind the object to find, may be {@code null}.
     * @param startIndex   the index to start searching.
     * @return the index of the object within the array starting at the index, {@link CollectionUtils#INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null}
     *         array input.
     */
    static int indexOf(final Object[] array, final Object objectToFind, int startIndex) {
        if (array == null) {
            return CollectionUtils.INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return CollectionUtils.INDEX_NOT_FOUND;
    }

    /**
     * <p>
     * Finds the index of the given object in the array.
     * </p>
     * <p>
     * This method returns {@link CollectionUtils#INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     * </p>
     *
     * @param array        the array to search for the object, may be {@code null}.
     * @param objectToFind the object to find, may be {@code null}.
     * @return the index of the object within the array, {@link CollectionUtils#INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input.
     */
    static <T> int indexOf(final T[] array, final Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    /**
     * <p>
     * Converts the given array to a stack (ArrayStack) with resilience disabled.
     * Elements are pushed onto the stack in array order, so the last array element
     * will be at the top of the stack.
     * </p>
     *
     * @param <E>    the type of elements in the array
     * @param array  the array to convert, may be {@code null}
     * @return a new ArrayStack containing all array elements, or an empty stack if the array is null
     * @since 4.6
     */
    static <E> ArrayStack<E> toArrayStack(final E[] array) {
        final ArrayStack<E> stack = new ArrayStack<>();
        if (array != null) {
            for (final E element : array) {
                stack.push(element);
            }
        }
        return stack;
    }

    /**
     * <p>
     * Converts the given array to a stack (ArrayStack) with rate limiting enabled.
     * Elements are pushed onto the stack in array order.
     * </p>
     *
     * @param <E>          the type of elements in the array
     * @param array        the array to convert, may be {@code null}
     * @param maxPermits   the maximum number of operations allowed per window
     * @param windowMillis the window duration in milliseconds
     * @return a new ArrayStack with rate limiting and all array elements
     * @since 4.6
     */
    static <E> ArrayStack<E> toArrayStackWithRateLimiting(final E[] array,
                                                           final long maxPermits,
                                                           final long windowMillis) {
        final ArrayStack<E> stack = ArrayStacks.withRateLimiting(maxPermits, windowMillis);
        if (array != null) {
            for (final E element : array) {
                stack.push(element);
            }
        }
        return stack;
    }

    /**
     * <p>
     * Converts the given array to a stack (ArrayStack) with circuit breaker enabled.
     * Elements are pushed onto the stack in array order.
     * </p>
     *
     * @param <E>                    the type of elements in the array
     * @param array                  the array to convert, may be {@code null}
     * @param failureThreshold       the number of failures before opening the circuit
     * @param monitoringWindowMillis the monitoring window duration
     * @param cooldownMillis         the cooldown duration before trying half-open
     * @return a new ArrayStack with circuit breaker and all array elements
     * @since 4.6
     */
    static <E> ArrayStack<E> toArrayStackWithCircuitBreaker(final E[] array,
                                                             final int failureThreshold,
                                                             final long monitoringWindowMillis,
                                                             final long cooldownMillis) {
        final ArrayStack<E> stack = ArrayStacks.withCircuitBreaker(
            failureThreshold, monitoringWindowMillis, cooldownMillis);
        if (array != null) {
            for (final E element : array) {
                stack.push(element);
            }
        }
        return stack;
    }

    /**
     * <p>
     * Converts the given array to a stack (ArrayStack) with full resilience enabled.
     * Elements are pushed onto the stack in array order.
     * </p>
     *
     * @param <E>                    the type of elements in the array
     * @param array                  the array to convert, may be {@code null}
     * @param maxPermits             the maximum number of operations allowed per window
     * @param windowMillis           the window duration in milliseconds
     * @param failureThreshold       the number of failures before opening the circuit
     * @param monitoringWindowMillis the monitoring window duration
     * @param cooldownMillis         the cooldown duration before trying half-open
     * @return a new ArrayStack with full resilience and all array elements
     * @since 4.6
     */
    static <E> ArrayStack<E> toArrayStackWithResilience(final E[] array,
                                                         final long maxPermits,
                                                         final long windowMillis,
                                                         final int failureThreshold,
                                                         final long monitoringWindowMillis,
                                                         final long cooldownMillis) {
        final ArrayStack<E> stack = ArrayStacks.withResilience(
            maxPermits, windowMillis, failureThreshold, monitoringWindowMillis, cooldownMillis);
        if (array != null) {
            for (final E element : array) {
                stack.push(element);
            }
        }
        return stack;
    }

    /**
     * <p>
     * Checks if all elements in the given array are non-null.
     * Returns {@code false} if the array is null or empty.
     * </p>
     *
     * @param array the array to check, may be {@code null}
     * @return {@code true} if all elements are non-null, {@code false} otherwise
     * @since 4.6
     */
    static boolean allNonNull(final Object[] array) {
        if (array == null || array.length == 0) {
            return false;
        }
        for (final Object element : array) {
            if (element == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Counts the number of non-null elements in the given array.
     * Returns 0 if the array is null.
     * </p>
     *
     * @param array the array to count, may be {@code null}
     * @return the count of non-null elements
     * @since 4.6
     */
    static int countNonNull(final Object[] array) {
        if (array == null) {
            return 0;
        }
        int count = 0;
        for (final Object element : array) {
            if (element != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Don't allow instances.
     */
    private ArrayUtils() {
    }

}
