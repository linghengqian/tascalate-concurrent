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
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

abstract class Try<R> {

    abstract R done();
    abstract boolean isSuccess();
    abstract boolean isCancel();
    abstract Promise<R> asPromise();
    
    static final class Success<R> extends Try<R> {
        
        private final R result;
        
        Success(R result) {
            this.result = result;
        }
        
        @Override
        R done() {
            return result;
        }
        
        @Override
        Promise<R> asPromise() {
            return Promises.success(result);
        }
        
        @Override
        boolean isSuccess() {
            return true;
        }
        
        @Override
        boolean isCancel() {
            return false;
        }        
    }
    
    static final class Failure<R> extends Try<R> {
        
        private final Throwable error;
        
        Failure(Throwable error) {
            this.error = error;
        }
        
        @Override
        R done() {
            if (error instanceof Error) {
                throw (Error)error;
            } else if (error instanceof CancellationException) {
                throw (CancellationException)error;
            } else {
                throw SharedFunctions.wrapCompletionException(error); 
            }
        }
        
        @Override
        Promise<R> asPromise() {
            return Promises.failure(error);
        }

        @Override
        boolean isSuccess() {
            return false;
        }
        
        @Override
        boolean isCancel() {
            Throwable ex = SharedFunctions.unwrapCompletionException(error);
            return ex instanceof CancellationException;
        }        

    }
    
    static <R> Try<R> success(R result) {
        return new Success<R>(result);
    }

    static <R> Try<R> failure(Throwable error) {
        return new Failure<R>(error);
    }

    static <R> Try<R> handle(R result, Throwable error, Promise<?> timeout) {
        if (null != timeout) {
            timeout.cancel(true);
        }
        return null == error ? Try.success(result) : Try.failure(error);
    }
    
    static <T> Try<T> doneOrTimeout(Try<T> result, Duration duration) {
        return null != result ? result : Try.failure(new TimeoutException("Timeout after " + duration));
    }
    
    static <R> Supplier<Try<R>> call(Supplier<? extends R> supplier) {
        return () -> {
            try {
                return Try.success(supplier.get());
            } catch (Throwable ex) {
                return Try.failure(ex);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <R> Try<R> nothing() {
        return (Try<R>)NOTHING;
    }
    
    private static final Try<Object> NOTHING = success(null); 
}