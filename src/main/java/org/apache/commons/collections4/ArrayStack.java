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
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

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
 * <strong>Distributed Lock Support (since 4.6):</strong>
 * When constructed with a {@link DistributedLock}, all mutable and read
 * operations acquire the lock before proceeding, eliminating data races
 * in high-concurrency scenarios. The lock can be backed by technologies
 * such as Redis, ZooKeeper, or a local {@code ReentrantLock}. Use
 * {@link ArrayUtils#reentrantLock(String)} for a simple single-JVM lock.
 * </p>
 * <p>
 * <strong>Note:</strong> From version 4.0 onwards, this class does not implement the
 * removed {@code Buffer} interface anymore.
 * </p>
 *
 * @param <E> the type of elements in this list
 * @see java.util.Stack
 * @see DistributedLock
 * @see ArrayUtils#reentrantLock(String)
 * @since 1.0
 * @deprecated Use {@link java.util.ArrayDeque} instead (available from Java 1.6)
 */
@Deprecated
public class ArrayStack<E> extends ArrayList<E> {

    /** Ensure serialization compatibility */
    private static final long serialVersionUID = 2130079159931574599L;

    /** The distributed lock for thread safety; {@code null} means no locking. */
    private final DistributedLock lock;

    /**
     * Constructs a new empty {@code ArrayStack}. The initial size
     * is controlled by {@code ArrayList} and is currently 10.
     * <p>
     * No locking is applied; operations are not thread-safe.
     * </p>
     */
    public ArrayStack() {
        this.lock = null;
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size.
     * <p>
     * No locking is applied; operations are not thread-safe.
     * </p>
     *
     * @param initialSize  the initial size to use
     * @throws IllegalArgumentException  if the specified initial size
     *  is negative
     */
    public ArrayStack(final int initialSize) {
        super(initialSize);
        this.lock = null;
    }

    /**
     * Constructs a new empty {@code ArrayStack} with the given
     * {@link DistributedLock} for thread-safe operations.
     * <p>
     * All mutable and read operations will acquire the lock before
     * proceeding, solving data races in high-concurrency scenarios.
     * </p>
     *
     * @param lock  the distributed lock to use, must not be null
     * @throws NullPointerException if lock is null
     * @since 4.6
     */
    public ArrayStack(final DistributedLock lock) {
        this.lock = Objects.requireNonNull(lock, "lock");
    }

    /**
     * Constructs a new empty {@code ArrayStack} with an initial size
     * and a {@link DistributedLock} for thread-safe operations.
     *
     * @param initialSize  the initial size to use
     * @param lock  the distributed lock to use, must not be null
     * @throws IllegalArgumentException  if the specified initial size
     *  is negative
     * @throws NullPointerException if lock is null
     * @since 4.6
     */
    public ArrayStack(final int initialSize, final DistributedLock lock) {
        super(initialSize);
        this.lock = Objects.requireNonNull(lock, "lock");
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
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return peekLocked();
            } finally {
                l.unlock();
            }
        }
        return peekLocked();
    }

    private E peekLocked() {
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
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return peekLocked(n);
            } finally {
                l.unlock();
            }
        }
        return peekLocked(n);
    }

    private E peekLocked(final int n) {
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
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return popLocked();
            } finally {
                l.unlock();
            }
        }
        return popLocked();
    }

    private E popLocked() {
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
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                add(item);
                return item;
            } finally {
                l.unlock();
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
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return searchLocked(object);
            } finally {
                l.unlock();
            }
        }
        return searchLocked(object);
    }

    private int searchLocked(final Object object) {
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

    // -- Override ArrayList mutating methods with lock protection --

    @Override
    public boolean add(final E e) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.add(e);
            } finally {
                l.unlock();
            }
        }
        return super.add(e);
    }

    @Override
    public void add(final int index, final E element) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.add(index, element);
            } finally {
                l.unlock();
            }
        } else {
            super.add(index, element);
        }
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.addAll(c);
            } finally {
                l.unlock();
            }
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.addAll(index, c);
            } finally {
                l.unlock();
            }
        }
        return super.addAll(index, c);
    }

    @Override
    public void clear() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.clear();
            } finally {
                l.unlock();
            }
        } else {
            super.clear();
        }
    }

    @Override
    public E remove(final int index) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.remove(index);
            } finally {
                l.unlock();
            }
        }
        return super.remove(index);
    }

    @Override
    public boolean remove(final Object o) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.remove(o);
            } finally {
                l.unlock();
            }
        }
        return super.remove(o);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.removeAll(c);
            } finally {
                l.unlock();
            }
        }
        return super.removeAll(c);
    }

    @Override
    public boolean removeIf(final Predicate<? super E> filter) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.removeIf(filter);
            } finally {
                l.unlock();
            }
        }
        return super.removeIf(filter);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.retainAll(c);
            } finally {
                l.unlock();
            }
        }
        return super.retainAll(c);
    }

    @Override
    public E set(final int index, final E element) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.set(index, element);
            } finally {
                l.unlock();
            }
        }
        return super.set(index, element);
    }

    @Override
    public void replaceAll(final UnaryOperator<E> operator) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.replaceAll(operator);
            } finally {
                l.unlock();
            }
        } else {
            super.replaceAll(operator);
        }
    }

    @Override
    public void sort(final java.util.Comparator<? super E> c) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.sort(c);
            } finally {
                l.unlock();
            }
        } else {
            super.sort(c);
        }
    }

    // -- Override ArrayList read/query methods with lock protection --

    @Override
    public boolean contains(final Object o) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.contains(o);
            } finally {
                l.unlock();
            }
        }
        return super.contains(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.containsAll(c);
            } finally {
                l.unlock();
            }
        }
        return super.containsAll(c);
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.forEach(action);
            } finally {
                l.unlock();
            }
        } else {
            super.forEach(action);
        }
    }

    @Override
    public E get(final int index) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.get(index);
            } finally {
                l.unlock();
            }
        }
        return super.get(index);
    }

    @Override
    public int indexOf(final Object o) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.indexOf(o);
            } finally {
                l.unlock();
            }
        }
        return super.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.isEmpty();
            } finally {
                l.unlock();
            }
        }
        return super.isEmpty();
    }

    @Override
    public int lastIndexOf(final Object o) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.lastIndexOf(o);
            } finally {
                l.unlock();
            }
        }
        return super.lastIndexOf(o);
    }

    @Override
    public int size() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.size();
            } finally {
                l.unlock();
            }
        }
        return super.size();
    }

    @Override
    public Object[] toArray() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.toArray();
            } finally {
                l.unlock();
            }
        }
        return super.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.toArray(a);
            } finally {
                l.unlock();
            }
        }
        return super.toArray(a);
    }

    @Override
    public void ensureCapacity(final int minCapacity) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.ensureCapacity(minCapacity);
            } finally {
                l.unlock();
            }
        } else {
            super.ensureCapacity(minCapacity);
        }
    }

    @Override
    public void trimToSize() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                super.trimToSize();
            } finally {
                l.unlock();
            }
        } else {
            super.trimToSize();
        }
    }

    @Override
    public Object clone() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.clone();
            } finally {
                l.unlock();
            }
        }
        return super.clone();
    }

    @Override
    public boolean equals(final Object o) {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.equals(o);
            } finally {
                l.unlock();
            }
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.hashCode();
            } finally {
                l.unlock();
            }
        }
        return super.hashCode();
    }

    @Override
    public String toString() {
        final DistributedLock l = lock;
        if (l != null) {
            l.lock();
            try {
                return super.toString();
            } finally {
                l.unlock();
            }
        }
        return super.toString();
    }

    /**
     * Returns the {@link DistributedLock} used by this stack, or {@code null}
     * if no lock is configured.
     *
     * @return the distributed lock, or null
     * @since 4.6
     */
    public DistributedLock getLock() {
        return lock;
    }
}