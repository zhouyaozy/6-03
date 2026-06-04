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
     * Computes the rate per second from a count and elapsed time.
     *
     * @param count      the operation count
     * @param elapsedMs  the elapsed time in milliseconds
     * @return the rate per second, or 0 if elapsedMs is 0
     * @throws IllegalArgumentException if count or elapsedMs is negative
     * @since 4.6
     */
    static double calculateRate(final long count, final long elapsedMs) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (elapsedMs < 0) {
            throw new IllegalArgumentException("elapsedMs must not be negative: " + elapsedMs);
        }
        if (elapsedMs == 0) {
            return 0.0;
        }
        return (double) count / elapsedMs * 1000.0;
    }

    /**
     * Calculates the maximum permitted operation count for a given rate
     * and time window without exceeding the rate limit.
     *
     * @param ratePerSecond  the allowed rate per second
     * @param windowMs       the time window in milliseconds
     * @return the maximum number of operations permitted in the window, at least 0
     * @throws IllegalArgumentException if rate or window is negative
     * @since 4.6
     */
    static long calculatePermittedCount(final double ratePerSecond, final long windowMs) {
        if (ratePerSecond < 0) {
            throw new IllegalArgumentException("ratePerSecond must not be negative: " + ratePerSecond);
        }
        if (windowMs < 0) {
            throw new IllegalArgumentException("windowMs must not be negative: " + windowMs);
        }
        return Math.max(0, (long) (ratePerSecond * windowMs / 1000.0));
    }

    /**
     * Checks whether all elements in the array are non-null.
     *
     * @param array  the array to check, may be {@code null}
     * @param <T>    the type of elements
     * @return {@code true} if the array is non-null and all elements are non-null
     * @since 4.6
     */
    static <T> boolean allNonNull(final T[] array) {
        if (array == null) {
            return false;
        }
        for (final T element : array) {
            if (element == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Don't allow instances.
     */
    private ArrayUtils() {
    }

}
