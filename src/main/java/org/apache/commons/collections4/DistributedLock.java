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
import java.util.concurrent.locks.Lock;

/**
 * A distributed lock interface for high-concurrency scenarios.
 * <p>
 * This interface provides a lock abstraction that can be implemented
 * by various distributed lock mechanisms (e.g., Redis, ZooKeeper, etc.)
 * to protect shared data structures in distributed environments.
 * </p>
 *
 * @since 4.6
 */
public interface DistributedLock extends Lock {

    /**
     * Acquires the lock.
     * <p>
     * If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     * </p>
     */
    @Override
    void lock();

    /**
     * Acquires the lock unless the current thread is interrupted.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    @Override
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    @Override
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time.
     *
     * @param timeout the maximum time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     * @throws InterruptedException if the current thread is interrupted
     */
    @Override
    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.
     */
    @Override
    void unlock();

    /**
     * Returns the name of this lock for identification purposes.
     *
     * @return the lock name
     */
    String getName();

    /**
     * Returns a {@link Condition} instance for use with this {@link Lock} instance.
     *
     * @return a new {@link Condition} instance
     */
    @Override
    Condition newCondition();

}
