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

/**
 * Abstraction for a distributed lock mechanism that can be used to protect
 * shared data structures against concurrent access in high-concurrency scenarios.
 * <p>
 * Implementations may use technologies such as Redis, ZooKeeper, or database-backed
 * locks to coordinate across multiple JVM instances. For single-JVM use cases,
 * {@link ArrayUtils#reentrantLock(String)} provides a convenient local implementation
 * via {@link java.util.concurrent.locks.ReentrantLock}.
 * </p>
 * <p>
 * Typical usage pattern:
 * </p>
 * <pre>{@code
 * DistributedLock lock = ...;
 * lock.lock();
 * try {
 *     // critical section
 * } finally {
 *     lock.unlock();
 * }
 * }</pre>
 *
 * @since 4.6
 */
public interface DistributedLock {

    /**
     * Acquires the lock, blocking until available.
     * <p>
     * Implementations should ensure that the lock is reentrant when possible,
     * and should document their specific semantics.
     * </p>
     */
    void lock();

    /**
     * Releases the lock.
     * <p>
     * This method must be called exactly once per successful {@link #lock()}
     * or {@link #tryLock()} call. Implementations should be safe to call
     * from the same thread that acquired the lock.
     * </p>
     */
    void unlock();

    /**
     * Attempts to acquire the lock without blocking.
     *
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    boolean tryLock();
}