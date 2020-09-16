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

import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

class SharedFunctions {
    
    private SharedFunctions() {}
    
    static Throwable unwrapCompletionException(Throwable ex) {
        Throwable nested = ex;
        while (nested instanceof CompletionException) {
            nested = nested.getCause();
        }
        return null == nested ? ex : nested;
    }

    static CompletionException wrapCompletionException(Throwable e) {
        if (e instanceof CompletionException) {
            return (CompletionException) e;
        } else {
            return new CompletionException(e);
        }
    }
    
    static Throwable unwrapExecutionException(Throwable ex) {
        Throwable nested = ex;
        while (nested instanceof ExecutionException) {
            nested = nested.getCause();
        }
        return null == nested ? ex : nested;
    }
    
    static ExecutionException wrapExecutionException(Throwable e) {
        if (e instanceof ExecutionException) {
            return (ExecutionException) e;
        } else {
            return new ExecutionException(e);
        }
    }
    
    
    static boolean cancelPromise(CompletionStage<?> promise, boolean mayInterruptIfRunning) {
        if (promise instanceof Future) {
            Future<?> future = (Future<?>) promise;
            return future.cancel(mayInterruptIfRunning);
        } else {
            Stream<Function<Class<?>, ExceptionalCancellation>> options = Stream.of(
               SharedFunctions::cancelInterruptibleMethodOf,     
               SharedFunctions::cancelMethodOf,
               SharedFunctions::completeExceptionallyMethodOf
            );
            
            return options.map(f -> tryCancellation(f, promise, mayInterruptIfRunning))  
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .findFirst()
                          .orElse(Boolean.FALSE);
        }
    }

    @SuppressWarnings("unchecked")
    static <U, V> BiFunction<U, V, U> selectFirst() {
        return (BiFunction<U, V, U>)SELECT_FIRST;
    }
    
    @SuppressWarnings("unchecked")
    static <U, V> BiFunction<U, V, V> selectSecond() {
        return (BiFunction<U, V, V>)SELECT_SECOND;
    }
    
    @SuppressWarnings("unchecked")
    static <T, R> Function<T, R> nullify() {
        return (Function<T, R>)NULLIFY;
    }
    
    static <T> Supplier<T> supply(T value) {
        return () -> value;
    }
    
    static void iif(boolean v) {}
    static <T> void voided(T v) {}
    
    private static Optional<Boolean> tryCancellation(Function<Class<?>, ExceptionalCancellation> option, 
                                                     CompletionStage<?> promise,
                                                     boolean mayInterruptIfRunning) {
        

        return Optional.ofNullable( option.apply(promise.getClass()) )
                       .map(SharedFunctions::uncheckedReflectionException) // TODO: False for reflective operation or runtime exception?
                       .map(bf -> bf.apply(promise, mayInterruptIfRunning ? Boolean.TRUE : Boolean.FALSE))
        ;      
    }

    private static ExceptionalCancellation completeExceptionallyMethodOf(Class<?> clazz) {
        try {
            Method m = clazz.getMethod("completeExceptionally", Throwable.class);
            return (p, b) -> (Boolean)m.invoke(p, new CancellationException());
        } catch (ReflectiveOperationException | SecurityException ex) {
            return null;
        }
    }
    
    private static ExceptionalCancellation cancelInterruptibleMethodOf(Class<?> clazz) {
        try {
            Method m = clazz.getMethod("cancel", boolean.class);
            return (p, b) -> (Boolean)m.invoke(p, b);
        } catch (ReflectiveOperationException | SecurityException ex) {
            return null;
        }
    }
    
    private static ExceptionalCancellation cancelMethodOf(Class<?> clazz) {
        try {
            Method m = clazz.getMethod("cancel");
            return (p, b) -> (Boolean)m.invoke(p);
        } catch (ReflectiveOperationException | SecurityException ex) {
            return null;
        }
    }
    
    private static <T, U> Cancellation uncheckedReflectionException(ExceptionalCancellation original ) {
        return (a, b) -> { 
            try {
                return original.apply(a, b);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    static final Function<Object, Throwable> NO_SUCH_ELEMENT = t -> new NoSuchElementException("Result rejected by filter: " + t);
    
    private static final BiFunction<Object, Object, Object> SELECT_FIRST  = (u, v) -> u;
    private static final BiFunction<Object, Object, Object> SELECT_SECOND = (u, v) -> v;
    private static final Function<Object, Object> NULLIFY = v -> null;
    
    @FunctionalInterface
    private static interface Cancellation extends BiFunction<CompletionStage<?>, Boolean, Boolean> { }
    
    @FunctionalInterface
    public interface ExceptionalCancellation {
        Boolean apply(CompletionStage<?> p, Boolean b) throws ReflectiveOperationException;
    }
}
