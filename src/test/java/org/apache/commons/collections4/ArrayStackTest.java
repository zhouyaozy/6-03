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
    public void testUserOperationFlow() {
        final ArrayStack<E> stack = makeObject();

        // Simulate pushing multiple items in a batch
        stack.pushAll((E) "Item1", (E) "Item2", (E) "Item3");
        assertFalse(stack.empty(), "Stack should not be empty");
        assertEquals(3, stack.size(), "Stack size should be 3");

        // Simulate peeking at the top item
        assertEquals("Item3", stack.peek(), "Top item should be 'Item3'");

        // Simulate searching for an item
        assertEquals(2, stack.search("Item2"), "Distance from top to 'Item2' should be 2");
        assertEquals(3, stack.search("Item1"), "Distance from top to 'Item1' should be 3");

        // Simulate popping a single item
        assertEquals("Item3", stack.pop(), "Popped item should be 'Item3'");
        assertEquals(2, stack.size(), "Stack size should be 2 after popping one item");

        // Simulate pushing another item
        stack.push((E) "Item4");
        assertEquals("Item4", stack.peek(), "Top item should now be 'Item4'");

        // Simulate popping all remaining items
        final Object[] remainingItems = stack.popAll();
        assertEquals(3, remainingItems.length, "Should have popped 3 items");
        assertEquals("Item4", remainingItems[0], "First popped item should be 'Item4'");
        assertEquals("Item2", remainingItems[1], "Second popped item should be 'Item2'");
        assertEquals("Item1", remainingItems[2], "Third popped item should be 'Item1'");

        // Verify stack is empty at the end
        assertTrue(stack.empty(), "Stack should be empty after popAll");
        assertEquals(0, stack.size(), "Stack size should be 0");
        assertThrows(EmptyStackException.class, () -> stack.peek());
    }

//    void testCreate() throws Exception {
//        resetEmpty();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/ArrayStack.emptyCollection.version4.obj");
//        resetFull();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/ArrayStack.fullCollection.version4.obj");
//    }

}
