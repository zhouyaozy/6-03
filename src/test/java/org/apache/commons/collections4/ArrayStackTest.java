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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EmptyStackException;

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
    void testUserWorkflowFromPushToDrain() {
        final ArrayStack<String> stack = new ArrayStack<>(4);

        assertTrue(stack.empty(), "Workflow starts with an empty stack");

        stack.push("browse");
        stack.push(null);
        stack.push("checkout");
        stack.push("pay");

        assertEquals("pay", stack.peek(), "Latest user action stays on top");
        assertEquals("checkout", stack.peek(1), "Can inspect a previous action without removing it");
        assertEquals(1, stack.search("pay"), "Top action is one step away");
        assertEquals(2, stack.search("checkout"), "Previous action is two steps away");
        assertEquals(3, stack.search(null), "Null actions participate in stack searches");
        assertEquals(4, stack.search("browse"), "Earliest action is deepest in the stack");

        final Object[] snapshot = stack.toArray();
        assertArrayEquals(new Object[] { "browse", null, "checkout", "pay" }, snapshot,
                "Stack snapshot keeps bottom-up iteration order");
        assertTrue(ArrayUtils.contains(snapshot, null), "Snapshot still exposes null entries");
        assertEquals(0, ArrayUtils.indexOf(snapshot, "browse"), "Snapshot keeps the first action at index zero");
        assertEquals(3, ArrayUtils.indexOf(snapshot, "pay"), "Snapshot keeps the latest action at the end");
        assertEquals(CollectionUtils.INDEX_NOT_FOUND, ArrayUtils.indexOf(snapshot, "refund"),
                "Snapshot lookup reports missing actions consistently");

        assertEquals("pay", stack.pop(), "Workflow drains in reverse order");
        assertEquals("checkout", stack.pop(), "Second latest action comes next");
        assertEquals(null, stack.pop(), "Null values can be popped like regular entries");
        assertEquals("browse", stack.pop(), "Earliest action is removed last");
        assertTrue(stack.empty(), "Workflow ends with an empty stack");
        assertThrows(EmptyStackException.class, stack::peek, "Cannot inspect an empty workflow");
        assertThrows(EmptyStackException.class, stack::pop, "Cannot pop from an empty workflow");
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

//    void testCreate() throws Exception {
//        resetEmpty();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/ArrayStack.emptyCollection.version4.obj");
//        resetFull();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/ArrayStack.fullCollection.version4.obj");
//    }

}
