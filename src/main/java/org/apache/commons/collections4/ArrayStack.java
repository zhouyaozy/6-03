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
 * <strong>Thread Safety:</strong> This class is not thread-safe by default. For high-concurrency
 * scenarios, use {@link #withLock(DistributedLock)} to wrap the stack with a distributed lock,
 * or use {@link #withDefaultLock()} for a default ReentrantLock-based solution.
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

    /** The distributed lock for thread-safe operations, null when not enabled */
    private transient volatile DistributedLock lock;

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
     * Constructs a new empty {@code ArrayStack} with a distributed lock.
     *
     * @param lock the distributed lock to use for thread-safe operations
     * @since 4.6
     */
    public ArrayStack(final DistributedLock lock) {
        this.lock = lock;
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size and a distributed lock.
     *
     * @param initialSize  the initial size to use
     * @param lock the distributed lock to use for thread-safe operations
     * @throws IllegalArgumentException  if the specified initial size is negative
     * @since 4.6
     */
    public ArrayStack(final int initialSize, final DistributedLock lock) {
        super(initialSize);
        this.lock = lock;
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
        final DistributedLock currentLock = this.lock;
        if (currentLock != null) {
            currentLock.lock();
            try {
                return isEmpty();
            } finally {
                currentLock.unlock();
            }
        }
        return isEmpty();
    }

    /**
     * Returns the top item off of this stack without removing it.
     *
     * @return the top item on the stack
     * @throws EmptyStackException  if the stack is empty
     */
    public E peek() throws EmptyStackException {
        final DistributedLock currentLock = this.lock;
        if (currentLock != null) {
            currentLock.lock();
            try {
                final int n = size();
                if (n <= 0) {
                    throw new EmptyStackException();
                }
                return get(n - 1);
            } finally {
                currentLock.unlock();
            }
        }
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
        final DistributedLock currentLock = this.lock;
        if (currentLock != null) {
            currentLock.lock();
            try {
                final int m = size() - n - 1;
                if (m < 0) {
                    throw new EmptyStackException();
                }
                return get(m);
            } finally {
                currentLock.unlock();
            }
        }
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
        final DistributedLock currentLock = this.lock;
        if (currentLock != null) {
            currentLock.lock();
            try {
                final int n = size();
                if (n <= 0) {
                    throw new EmptyStackException();
                }
                return remove(n - 1);
            } finally {
                currentLock.unlock();
            }
        }
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
        final DistributedLock currentLock = this.lock;
        if (currentLock != null) {
            currentLock.lock();
            try {
                add(item);
                return item;
            } finally {
                currentLock.unlock();
            }
        }
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
        final DistributedLock currentLock = this.lock;
        if (currentLock != null) {
            currentLock.lock();
            try {
                return doSearch(object);
            } finally {
                currentLock.unlock();
            }
        }
        return doSearch(object);
    }

    private int doSearch(final Object object) {
        int i = size() - 1;
        int n = 1;
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
     * Wraps this stack with the given distributed lock for thread-safe operations.
     *
     * @param lock the distributed lock to use
     * @return this stack instance with lock enabled
     * @since 4.6
     */
    public ArrayStack<E> withLock(final DistributedLock lock) {
        this.lock = lock;
        return this;
    }

    /**
     * Wraps this stack with a default {@link DefaultDistributedLock} for thread-safe operations.
     *
     * @return this stack instance with default lock enabled
     * @since 4.6
     */
    public ArrayStack<E> withDefaultLock() {
        return withDefaultLock(false);
    }

    /**
     * Wraps this stack with a default {@link DefaultDistributedLock} for thread-safe operations.
     *
     * @param fair if {@code true}, lock acquisition favors the longest waiting thread
     * @return this stack instance with default lock enabled
     * @since 4.6
     */
    public ArrayStack<E> withDefaultLock(final boolean fair) {
        this.lock = new DefaultDistributedLock(fair);
        return this;
    }

    /**
     * Returns the distributed lock associated with this stack, or {@code null} if not set.
     *
     * @return the distributed lock, or {@code null}
     * @since 4.6
     */
    public DistributedLock getLock() {
        return lock;
    }

}
