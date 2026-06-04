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

import java.util.List;
import java.util.ListIterator;

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
     * This method returns {@code false} if a {@code null} array is passed in.
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
     * @return the index of the object within the array, {@link CollectionUtils#INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null}
     *         array input.
     */
    static <T> int indexOf(final T[] array, final Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    /**
     * Null-safe comparison of two objects.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return {@code true} if the objects are equal or both are {@code null}
     */
    static boolean equals(final Object o1, final Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }

    /**
     * Finds the 1-based position of the distance from the end of the list
     * that the specified object exists, starting from the last element.
     * Returns -1 if the object is not found.
     *
     * @param list the list to search
     * @param object the object to find
     * @param <E> the type of elements in the list
     * @return 1-based position from the end, or -1 if not found
     */
    static <E> int lastIndexFromEnd(final List<E> list, final Object object) {
        if (list == null) {
            return CollectionUtils.INDEX_NOT_FOUND;
        }
        int position = 1;
        final ListIterator<E> iterator = list.listIterator(list.size());
        while (iterator.hasPrevious()) {
            final E current = iterator.previous();
            if (equals(object, current)) {
                return position;
            }
            position++;
        }
        return CollectionUtils.INDEX_NOT_FOUND;
    }

    /**
     * Don't allow instances.
     */
    private ArrayUtils() {
    }

}
