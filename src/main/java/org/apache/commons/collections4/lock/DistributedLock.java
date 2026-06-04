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
package org.apache.commons.collections4.lock;

import java.util.concurrent.TimeUnit;

/**
 * Distributed lock interface for coordinating access to shared resources
 * across multiple nodes in a distributed system.
 *
 * @since 4.6
 */
public interface DistributedLock {

    /**
     * Acquires the lock, waiting indefinitely until it's available.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void lock() throws InterruptedException;

    /**
     * Tries to acquire the lock, returning immediately if it's not available.
     *
     * @return true if the lock was acquired, false otherwise
     */
    boolean tryLock();

    /**
     * Tries to acquire the lock, waiting for the specified time if necessary.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the wait time
     * @return true if the lock was acquired, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.
     */
    void unlock();

}
