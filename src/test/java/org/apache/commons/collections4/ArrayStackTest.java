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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EmptyStackException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Tests ArrayStack.
 */
@SuppressWarnings("deprecation") // we test a deprecated class
public class ArrayStackTest<E> extends AbstractArrayListTest<E> {

    @Override
    public String getCompatibilityVersion() {
        return "4";
    }

    @Override
    public ArrayStack<E> makeObject() {
        return new ArrayStack<>();
    }

    @Test
    void testNewStack() {
        final ArrayStack<E> stack = makeObject();
        assertTrue(stack.empty(), "New stack is empty");
        assertEquals(0, stack.size(), "New stack has size zero");

        assertThrows(EmptyStackException.class, () -> stack.peek());

        assertThrows(EmptyStackException.class, () -> stack.pop());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPushPeekPop() {
        final ArrayStack<E> stack = makeObject();

        stack.push((E) "First Item");
        assertFalse(stack.empty(), "Stack is not empty");
        assertEquals(1, stack.size(), "Stack size is one");
        assertEquals("First Item", stack.peek(),
                "Top item is 'First Item'");
        assertEquals(1, stack.size(), "Stack size is one");

        stack.push((E) "Second Item");
        assertEquals(2, stack.size(), "Stack size is two");
        assertEquals("Second Item", stack.peek(),
                "Top item is 'Second Item'");
        assertEquals(2, stack.size(), "Stack size is two");

        assertEquals("Second Item", stack.pop(),
                "Popped item is 'Second Item'");
        assertEquals("First Item", stack.peek(),
                "Top item is 'First Item'");
        assertEquals(1, stack.size(), "Stack size is one");

        assertEquals("First Item", stack.pop(),
                "Popped item is 'First Item'");
        assertEquals(0, stack.size(), "Stack size is zero");
    }

    @Test
    @Override
    @SuppressWarnings("unchecked")
    public void testSearch() {
        final ArrayStack<E> stack = makeObject();

        stack.push((E) "First Item");
        stack.push((E) "Second Item");
        assertEquals(1, stack.search("Second Item"),
                "Top item is 'Second Item'");
        assertEquals(2, stack.search("First Item"),
                "Next Item is 'First Item'");
        assertEquals(-1, stack.search("Missing Item"),
                "Cannot find 'Missing Item'");
    }

@Test
    @SuppressWarnings("unchecked")
    void testDistributedLockConstructor() {
        final DistributedLock lock = ArrayUtils.reentrantLock("testLock");
        final ArrayStack<E> stack = new ArrayStack<>(lock);
        assertSame(lock, stack.getLock());
        assertTrue(stack.empty());

        stack.push((E) "A");
        assertEquals(1, stack.size());
        assertEquals("A", stack.peek());
        assertEquals("A", stack.pop());
        assertTrue(stack.empty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDistributedLockConstructorWithInitialSize() {
        final DistributedLock lock = ArrayUtils.reentrantLock("testLock");
        final ArrayStack<E> stack = new ArrayStack<>(20, lock);
        assertSame(lock, stack.getLock());
        assertTrue(stack.empty());

        stack.push((E) "B");
        assertEquals(1, stack.size());
        assertEquals("B", stack.pop());
    }

    @Test
    void testNullLockThrowsException() {
        assertThrows(NullPointerException.class, () -> new ArrayStack<E>((DistributedLock) null));
        assertThrows(NullPointerException.class, () -> new ArrayStack<E>(10, null));
    }

    @Test
    void testUnlockedStackHasNullLock() {
        final ArrayStack<E> stack = new ArrayStack<>();
        assertTrue(stack.getLock() == null);
    }

    @Test
    void testLockIsAcquiredDuringOperations() {
        final AtomicInteger lockCount = new AtomicInteger(0);
        final AtomicInteger unlockCount = new AtomicInteger(0);
        final DistributedLock countingLock = new DistributedLock() {
            @Override
            public void lock() {
                lockCount.incrementAndGet();
            }

            @Override
            public void unlock() {
                unlockCount.incrementAndGet();
            }

            @Override
            public boolean tryLock() {
                lockCount.incrementAndGet();
                return true;
            }
        };

        final ArrayStack<String> stack = new ArrayStack<>(countingLock);
        stack.push("test");
        stack.peek();
        stack.pop();
        stack.size();
        stack.isEmpty();

        assertTrue(lockCount.get() >= 5, "Lock should be acquired at least 5 times");
        assertTrue(unlockCount.get() >= 5, "Unlock should be called at least 5 times");
    }

    @Test
    void testConcurrentPushAndPop() throws InterruptedException {
        final DistributedLock lock = ArrayUtils.reentrantLock("concurrentLock");
        final ArrayStack<Integer> stack = new ArrayStack<>(lock);
        final int threads = 4;
        final int iterations = 100;
        final CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int start = t * iterations;
            new Thread(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        stack.push(start + i);
                        stack.peek();
                        stack.pop();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertTrue(stack.empty());
        assertEquals(0, stack.size());
    }

//    void testCreate() throws Exception {
//        resetEmpty();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/ArrayStack.emptyCollection.version4.obj");
//        resetFull();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/ArrayStack.fullCollection.version4.obj");
//    }

}
