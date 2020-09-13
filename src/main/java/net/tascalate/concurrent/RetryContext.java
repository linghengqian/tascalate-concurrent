/**
 * Copyright 2015-2020 Valery Silaev (http://vsilaev.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tascalate.concurrent;

import java.time.Duration;

public final class RetryContext<T> {
    private final int retryCount;
    private final Duration lastCallDuration;
    private final T lastResult;
    private final Throwable lastError;
    
    private RetryContext(int retryCount, Duration lastCallDuration, T lastResult, Throwable lastError) {
        this.retryCount = retryCount;
        this.lastCallDuration = lastCallDuration;
        this.lastResult = lastResult;
        this.lastError  = lastError;
    }

    public int getRetryCount() {
        return retryCount;
    }
    
    public Duration getLastCallDuration() {
        return lastCallDuration;
    }

    public T getLastResult() {
        return lastResult;
    }    
    
    public Throwable getLastError() {
        return lastError;
    }
    
    public RetryContext<T> overrideRetryCount(int newRetryCount) {
        return new RetryContext<>(newRetryCount, lastCallDuration, lastResult, lastError);
    }
    
    public RetryContext<T> overrideLastCallDuration(Duration newDuration) {
        return new RetryContext<>(retryCount, newDuration, lastResult, lastError);
    }

    public RetryContext<T> overrideLastResult(T newResult) {
        return new RetryContext<>(retryCount, lastCallDuration, newResult, lastError);
    }
    
    public RetryContext<T> overrideLastError(Throwable newError) {
        return new RetryContext<>(retryCount, lastCallDuration, lastResult, newError);
    }
    
    static <T> RetryContext<T> initial() {
        return new RetryContext<>(0, Duration.ZERO, null, null);
    }
    
    RetryContext<T> nextRetry(Duration callDuration, T lastResult) {
        return new RetryContext<>(retryCount + 1, callDuration, lastResult, null);
    }
    
    RetryContext<T> nextRetry(Duration callDuration, Throwable lastError) {
        return new RetryContext<>(retryCount + 1, callDuration, null, lastError);
    }

    
    RetryException asFailure() {
        RetryException result = new RetryException(retryCount, lastCallDuration, lastError);
        result.fillInStackTrace();
        return result;
    }

}
