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
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * An implementation of the {@link java.util.Stack} API that is based on an
 * {@code ArrayList} instead of a {@code Vector}. By default, access is
 * serialized with a local reentrant lock, and callers may supply a custom lock
 * implementation to coordinate stack operations across processes.
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

    /**
     * Acquires the lock that protects stack operations.
     */
    @FunctionalInterface
    public interface DistributedLock {

        /**
         * Acquires the lock and returns a handle that releases it.
         *
         * @return the acquired lock handle
         */
        LockHandle lock();
    }

    /**
     * Releases a previously acquired lock.
     */
    @FunctionalInterface
    public interface LockHandle {

        /**
         * Releases the lock.
         */
        void unlock();
    }

    @FunctionalInterface
    interface LockAcquireAction {

        LockHandle lock();
    }

    /** Ensure serialization compatibility */
    private static final long serialVersionUID = 2130079159931574599L;

    private transient DistributedLock distributedLock;
    private transient ThreadLocal<Integer> lockDepth;
    private transient ReentrantLock localLock;

    /**
     * Constructs a new empty {@code ArrayStack}. The initial size
     * is controlled by {@code ArrayList} and is currently 10.
     */
    public ArrayStack() {
    }

    /**
     * Constructs a new empty {@code ArrayStack} that uses the supplied lock.
     *
     * @param distributedLock the lock used to serialize stack operations
     */
    public ArrayStack(final DistributedLock distributedLock) {
        setDistributedLock(distributedLock);
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
     * Constructs a new empty {@code ArrayStack} with an initial size and lock.
     *
     * @param initialSize the initial size to use
     * @param distributedLock the lock used to serialize stack operations
     */
    public ArrayStack(final int initialSize, final DistributedLock distributedLock) {
        super(initialSize);
        setDistributedLock(distributedLock);
    }

    @Override
    public boolean add(final E object) {
        return withLock(() -> super.add(object));
    }

    @Override
    public void add(final int index, final E element) {
        withLock(() -> super.add(index, element));
    }

    @Override
    public boolean addAll(final Collection<? extends E> collection) {
        return withLock(() -> super.addAll(collection));
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> collection) {
        return withLock(() -> super.addAll(index, collection));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        return withLock(() -> {
            final ArrayStack<E> clone = (ArrayStack<E>) super.clone();
            clone.distributedLock = distributedLock;
            clone.lockDepth = null;
            clone.localLock = null;
            return clone;
        });
    }

    @Override
    public void clear() {
        withLock(super::clear);
    }

    @Override
    public boolean contains(final Object object) {
        return withLock(() -> super.contains(object));
    }

    @Override
    public boolean containsAll(final Collection<?> collection) {
        return withLock(() -> super.containsAll(collection));
    }

    public boolean empty() {
        return withLock(super::isEmpty);
    }

    @Override
    public void ensureCapacity(final int minCapacity) {
        withLock(() -> super.ensureCapacity(minCapacity));
    }

    public <R> R executeLocked(final Function<? super ArrayStack<E>, R> action) {
        return withLock(() -> ArrayUtils.requireNonNull(action, "action").apply(this));
    }

    public void executeLocked(final Consumer<? super ArrayStack<E>> action) {
        withLock(() -> ArrayUtils.requireNonNull(action, "action").accept(this));
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        withLock(() -> super.forEach(action));
    }

    @Override
    public E get(final int index) {
        return withLock(() -> super.get(index));
    }

    @Override
    public int indexOf(final Object object) {
        return withLock(() -> super.indexOf(object));
    }

    @Override
    public boolean isEmpty() {
        return withLock(super::isEmpty);
    }

    @Override
    public int lastIndexOf(final Object object) {
        return withLock(() -> super.lastIndexOf(object));
    }

    public E peek() throws EmptyStackException {
        return withLock(() -> {
            final int size = super.size();
            if (size <= 0) {
                throw new EmptyStackException();
            }
            return super.get(size - 1);
        });
    }

    public E peek(final int n) throws EmptyStackException {
        return withLock(() -> {
            final int index = super.size() - n - 1;
            if (index < 0) {
                throw new EmptyStackException();
            }
            return super.get(index);
        });
    }

    public E pop() throws EmptyStackException {
        return withLock(() -> {
            final int size = super.size();
            if (size <= 0) {
                throw new EmptyStackException();
            }
            return super.remove(size - 1);
        });
    }

    public E push(final E item) {
        withLock(() -> super.add(item));
        return item;
    }

    @Override
    public E remove(final int index) {
        return withLock(() -> super.remove(index));
    }

    @Override
    public boolean remove(final Object object) {
        return withLock(() -> super.remove(object));
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
        return withLock(() -> super.removeAll(collection));
    }

    @Override
    public boolean removeIf(final Predicate<? super E> filter) {
        return withLock(() -> super.removeIf(filter));
    }

    @Override
    public void replaceAll(final UnaryOperator<E> operator) {
        withLock(() -> super.replaceAll(operator));
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
        return withLock(() -> super.retainAll(collection));
    }

    public int search(final Object object) {
        return withLock(() -> {
            int i = super.size() - 1;
            int distance = 1;
            while (i >= 0) {
                final Object current = super.get(i);
                if (object == null && current == null || object != null && object.equals(current)) {
                    return distance;
                }
                i--;
                distance++;
            }
            return -1;
        });
    }

    @Override
    public E set(final int index, final E element) {
        return withLock(() -> super.set(index, element));
    }

    public void setDistributedLock(final DistributedLock distributedLock) {
        this.distributedLock = ArrayUtils.requireNonNull(distributedLock, "distributedLock");
    }

    @Override
    public int size() {
        return withLock(super::size);
    }

    @Override
    public void sort(final Comparator<? super E> comparator) {
        withLock(() -> super.sort(comparator));
    }

    @Override
    public Object[] toArray() {
        return withLock(() -> super.toArray());
    }

    @Override
    public <T> T[] toArray(final T[] array) {
        return withLock(() -> super.toArray(array));
    }

    @Override
    public void trimToSize() {
        withLock(super::trimToSize);
    }

    public void useLocalLock() {
        distributedLock = null;
    }

    private LockHandle acquireLock() {
        final ThreadLocal<Integer> depth = lockDepth();
        final Integer currentDepth = depth.get();
        if (currentDepth != null) {
            depth.set(currentDepth + 1);
            return this::releaseNestedLock;
        }
        final LockHandle lockHandle = ArrayUtils.requireNonNull(distributedLock().lock(), "lockHandle");
        depth.set(1);
        return () -> releaseOuterLock(lockHandle);
    }

    private DistributedLock distributedLock() {
        if (distributedLock == null) {
            distributedLock = () -> {
                final ReentrantLock lock = localLock();
                lock.lock();
                return ArrayUtils.asLockHandle(lock);
            };
        }
        return distributedLock;
    }

    private ReentrantLock localLock() {
        if (localLock == null) {
            localLock = new ReentrantLock();
        }
        return localLock;
    }

    private ThreadLocal<Integer> lockDepth() {
        if (lockDepth == null) {
            lockDepth = new ThreadLocal<>();
        }
        return lockDepth;
    }

    private void releaseNestedLock() {
        final ThreadLocal<Integer> depth = lockDepth();
        final int nextDepth = depth.get() - 1;
        if (nextDepth == 0) {
            depth.remove();
        } else {
            depth.set(nextDepth);
        }
    }

    private void releaseOuterLock(final LockHandle lockHandle) {
        releaseNestedLock();
        lockHandle.unlock();
    }

    private <T> T withLock(final Supplier<T> supplier) {
        return ArrayUtils.withLock(this::acquireLock, supplier);
    }

    private void withLock(final Runnable runnable) {
        ArrayUtils.withLock(this::acquireLock, runnable);
    }

}
