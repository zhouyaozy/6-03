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

import org.apache.commons.collections4.lock.DistributedLock;
import org.apache.commons.collections4.lock.LocalDistributedLock;

import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.Objects;

/**
 * Decorates another ArrayStack to use a distributed lock for safe
 * concurrent access in both single-JVM and distributed environments.
 *
 * @param <E> the type of elements in this stack
 * @since 4.6
 */
public class SynchronizedDistributedArrayStack<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ArrayStack<E> stack;
    private final DistributedLock lock;

    /**
     * Factory method to create a synchronized distributed array stack.
     *
     * @param <E> the type of elements in this stack
     * @param stack the stack to decorate, must not be null
     * @return a new synchronized distributed array stack
     * @throws NullPointerException if stack is null
     */
    public static <E> SynchronizedDistributedArrayStack<E> synchronizedDistributedArrayStack(final ArrayStack<E> stack) {
        return new SynchronizedDistributedArrayStack<>(stack, new LocalDistributedLock());
    }

    /**
     * Factory method to create a synchronized distributed array stack
     * using a custom distributed lock.
     *
     * @param <E> the type of elements in this stack
     * @param stack the stack to decorate, must not be null
     * @param lock the distributed lock to use, must not be null
     * @return a new synchronized distributed array stack
     * @throws NullPointerException if stack or lock is null
     */
    public static <E> SynchronizedDistributedArrayStack<E> synchronizedDistributedArrayStack(final ArrayStack<E> stack,
                                                                                             final DistributedLock lock) {
        return new SynchronizedDistributedArrayStack<>(stack, lock);
    }

    /**
     * Constructor that wraps an ArrayStack with a distributed lock.
     *
     * @param stack the stack to decorate, must not be null
     * @param lock the distributed lock to use, must not be null
     * @throws NullPointerException if stack or lock is null
     */
    protected SynchronizedDistributedArrayStack(final ArrayStack<E> stack, final DistributedLock lock) {
        this.stack = Objects.requireNonNull(stack, "stack");
        this.lock = Objects.requireNonNull(lock, "lock");
    }

    /**
     * Checks if the stack is empty.
     *
     * @return true if the stack is empty
     */
    public boolean empty() {
        try {
            lock.lock();
            return stack.empty();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the top element without removing it.
     *
     * @return the top element
     * @throws EmptyStackException if the stack is empty
     */
    public E peek() throws EmptyStackException {
        try {
            lock.lock();
            return stack.peek();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the element at the specified distance from the top without removing it.
     *
     * @param n the distance from the top
     * @return the element
     * @throws EmptyStackException if there are not enough elements
     */
    public E peek(final int n) throws EmptyStackException {
        try {
            lock.lock();
            return stack.peek(n);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the top element.
     *
     * @return the top element
     * @throws EmptyStackException if the stack is empty
     */
    public E pop() throws EmptyStackException {
        try {
            lock.lock();
            return stack.pop();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Pushes an element onto the top of the stack.
     *
     * @param item the element to push
     * @return the pushed element
     */
    public E push(final E item) {
        try {
            lock.lock();
            return stack.push(item);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the 1-based position of an object on the stack.
     *
     * @param object the object to search for
     * @return the 1-based position from the top, or -1 if not found
     */
    public int search(final Object object) {
        try {
            lock.lock();
            return stack.search(object);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the size of the stack.
     *
     * @return the size
     */
    public int size() {
        try {
            lock.lock();
            return stack.size();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the underlying stack (for advanced usage).
     *
     * @return the decorated stack
     */
    protected ArrayStack<E> decorated() {
        return stack;
    }

}
