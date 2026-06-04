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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link DistributedLock} based on {@link ReentrantLock}.
 * <p>
 * This implementation provides a local reentrant lock suitable for
 * single-JVM high-concurrency scenarios. For true distributed environments,
 * consider implementing {@link DistributedLock} with Redis, ZooKeeper, or
 * other distributed coordination services.
 * </p>
 *
 * @since 4.6
 */
public class DefaultDistributedLock implements DistributedLock {

    private final ReentrantLock lock;
    private final String name;

    /**
     * Constructs a new {@code DefaultDistributedLock} with a default name.
     */
    public DefaultDistributedLock() {
        this("DefaultDistributedLock");
    }

    /**
     * Constructs a new {@code DefaultDistributedLock} with the given name.
     *
     * @param name the name of this lock
     */
    public DefaultDistributedLock(final String name) {
        this.name = name;
        this.lock = new ReentrantLock();
    }

    /**
     * Constructs a new {@code DefaultDistributedLock} with the given fairness policy.
     *
     * @param fair if {@code true}, lock acquisition favors the longest waiting thread
     */
    public DefaultDistributedLock(final boolean fair) {
        this("DefaultDistributedLock", fair);
    }

    /**
     * Constructs a new {@code DefaultDistributedLock} with the given name and fairness policy.
     *
     * @param name the name of this lock
     * @param fair if {@code true}, lock acquisition favors the longest waiting thread
     */
    public DefaultDistributedLock(final String name, final boolean fair) {
        this.name = name;
        this.lock = new ReentrantLock(fair);
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public boolean tryLock(final long timeout, final TimeUnit unit) throws InterruptedException {
        return lock.tryLock(timeout, unit);
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Condition newCondition() {
        return lock.newCondition();
    }

    @Override
    public String toString() {
        return "DefaultDistributedLock[name=" + name + ", " + lock.toString() + "]";
    }

}
