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

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

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
@SuppressWarnings("deprecation")
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

    static ArrayStack.LockHandle asLockHandle(final Lock lock) {
        requireNonNull(lock, "lock");
        return lock::unlock;
    }

    static <T> int indexOf(final T[] array, final Object objectToFind) {
        return indexOf(array, objectToFind, 0);
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

    static <T> T requireNonNull(final T object, final String name) {
        if (object == null) {
            throw new NullPointerException(name);
        }
        return object;
    }

    static <T> T withLock(final ArrayStack.LockAcquireAction lockAcquireAction, final Supplier<T> supplier) {
        final ArrayStack.LockHandle lockHandle = requireNonNull(requireNonNull(lockAcquireAction, "lockAcquireAction").lock(), "lockHandle");
        try {
            return requireNonNull(supplier, "supplier").get();
        } finally {
            lockHandle.unlock();
        }
    }

    static void withLock(final ArrayStack.LockAcquireAction lockAcquireAction, final Runnable runnable) {
        final ArrayStack.LockHandle lockHandle = requireNonNull(requireNonNull(lockAcquireAction, "lockAcquireAction").lock(), "lockHandle");
        try {
            requireNonNull(runnable, "runnable").run();
        } finally {
            lockHandle.unlock();
        }
    }

    /**
     * Don't allow instances.
     */
    private ArrayUtils() {
    }

}
