/**
 * Original work: copyright 2009-2015 Lukáš Křečan
 * https://github.com/lukas-krecan/completion-stage
 * 
 * This class is based on the work create by Lukáš Křečan 
 * under the Apache License, Version 2.0. Please see 
 * https://github.com/lukas-krecan/completion-stage/blob/completion-stage-0.0.9/src/main/java/net/javacrumbs/completionstage/CompletionStageAdapter.java
 * 
 * Modified work: copyright 2015-2020 Valery Silaev (http://vsilaev.com)
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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Helper class to create a concrete {@link Promise} subclass as an
 * implementation from scratch.
 * @author vsilaev
 *
 * @param <T>
 *   a type of the successfully resolved promise value   
 */
abstract public class PromiseAdapter<T> implements Promise<T> {
    protected static final Executor SAME_THREAD_EXECUTOR = new Executor() {
        public void execute(Runnable command) {
            command.run();
        }

        public String toString() {
            return "SAME_THREAD_EXECUTOR";
        }
    };
    private final Executor defaultExecutor;

    protected PromiseAdapter(Executor defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Override
    public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public <U> Promise<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, this.defaultExecutor);
    }

    @Override
    public Promise<Void> thenAccept(Consumer<? super T> action) {
        return thenAcceptAsync(action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public Promise<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAcceptAsync(action, this.defaultExecutor);
    }

    @Override
    public Promise<Void> thenRun(Runnable action) {
        return thenRunAsync(action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public Promise<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, this.defaultExecutor);
    }

    @Override
    public <U, V> Promise<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombineAsync(other, fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public <U, V> Promise<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombineAsync(other, fn, this.defaultExecutor);
    }
    
    @Override
    public <U> Promise<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public <U> Promise<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, this.defaultExecutor);
    }

    @Override
    public Promise<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public Promise<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, this.defaultExecutor);
    }
    
    @Override
    public <U> Promise<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public <U> Promise<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, this.defaultExecutor);
    }
    
    @Override
    public Promise<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return exceptionallyAsync(fn, SAME_THREAD_EXECUTOR);
    }
    
    @Override
    public Promise<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        return exceptionallyAsync(fn, this.defaultExecutor);
    }

    @Override
    public Promise<T> exceptionallyCompose(Function<Throwable, ? extends CompletionStage<T>> fn) {
        return exceptionallyComposeAsync(fn, SAME_THREAD_EXECUTOR);
    }
    
    @Override
    public Promise<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn) {
        return exceptionallyComposeAsync(fn, this.defaultExecutor);
    }

    @Override
    public Promise<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public Promise<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, this.defaultExecutor);
    }

    @Override
    public Promise<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public Promise<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, this.defaultExecutor);
    }
    
    @Override
    public <U> Promise<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public <U> Promise<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, this.defaultExecutor);
    }
    
    @Override
    public Promise<T> thenFilter(Predicate<? super T> predicate) {
        return thenFilterAsync(predicate, SAME_THREAD_EXECUTOR);
    }
    
    @Override
    public Promise<T> thenFilter(Predicate<? super T> predicate, Function<? super T, Throwable> errorSupplier) {
        return thenFilterAsync(predicate, errorSupplier, SAME_THREAD_EXECUTOR);
    }
    
    @Override
    public Promise<T> thenFilterAsync(Predicate<? super T> predicate) {
        return thenFilterAsync(predicate, this.defaultExecutor);
    }
    
    @Override
    public Promise<T> thenFilterAsync(Predicate<? super T> predicate, Function<? super T, Throwable> errorSupplier) {
        return thenFilterAsync(predicate, errorSupplier, this.defaultExecutor);
    }
    
    /* AS IS (definitions in Promise are OK)
    @Override
    public Promise<T> thenFilterAsync(Predicate<? super T> predicate, Executor executor) {
        return thenFilterAsync(predicate, NO_SUCH_ELEMENT, executor);
    }
    
    @Override
    public Promise<T> thenFilterAsync(Predicate<? super T> predicate, Function<? super T, Throwable> errorSupplier, Executor executor) {
        return thenComposeAsync(v -> predicate.test(v) ? this : failure(errorSupplier, v), executor);
    }
    */

    @Override
    public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public Promise<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, this.defaultExecutor);
    }

    @Override
    public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public <U> Promise<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, this.defaultExecutor);
    }

    protected final Executor getDefaultExecutor() {
        return this.defaultExecutor;
    }
}
