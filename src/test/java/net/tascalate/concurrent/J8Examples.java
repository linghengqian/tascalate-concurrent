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

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.tascalate.concurrent.decorators.ExtendedPromiseDecorator;
import net.tascalate.concurrent.util.AsyncLock;

import static net.tascalate.concurrent.PromiseOperations.partitionedItems;
import static net.tascalate.concurrent.PromiseOperations.partitionedStream;
import static net.tascalate.concurrent.PromiseOperations.tryCompose;

public class J8Examples {
    
    public static void main(final String[] argv) throws InterruptedException, ExecutionException {
        Promise<Long> eleOrigin = Promises.success(10L);
        Promise<Promise<Number>> eleDone1 = PromiseOperations.lift(eleOrigin);
        Promise<Promise<Number>> eleDone2 = eleOrigin.as(PromiseOperations::lift);
        eleDone2.as(PromiseOperations::drop).whenComplete((v, e) -> {
           System.out.println("Elevated-then-narrowed: " + v); 
        });
        
        final ThreadFactory tf = TaskExecutors.newThreadFactory()
            .withNameFormat("CTX-MY-THREAD-%d-OF-%s")
            .withThreadGroup(
                TaskExecutors.newThreadGroup()
                    .withName("Tascalate-Tasks")
                    .withMaxPriority(Thread.NORM_PRIORITY)
                .build()
            )
            .withContextClassLoader(J8Examples.class.getClassLoader())
        .build();
        
        final TaskExecutorService executorService = TaskExecutors.newFixedThreadPool(6, tf);
        AsyncLock lock = AsyncLock.create();
        for (int i = 0; i < 10; i++) {
            int idx  = i;
            lock.acquire()
                .orTimeout(Duration.ofMillis(450), true)
                .as(tryCompose(token -> CompletableTask.submit(() -> {
                    System.out.println("Current i " + idx  + " = " + Thread.currentThread().getName());
                    Thread.sleep(100);
                    return "Value" + idx;
                    }, executorService)                    
                )); 
        }
        /*
        Promise<?> timeout = CompletableTask.submit(() -> pollingMethod(
            RetryContext.initial(RetryPolicy.DEFAULT.withMaxRetries(1))
        ), executorService)
        .orTimeout(Duration.ofMillis(1000));
        timeout.whenComplete((r, e) -> {
            System.out.println(r);
            System.out.println(e);
        });
        */
        //Thread.sleep(7000); 
        //timeout.cancel(true);
        //System.exit(0);
        
        
        Promises.all(IntStream.range(1, 5)
                              .mapToObj(i -> CompletableTask.supplyAsync(() -> awaitAndProduce1(i, 100), executorService))
                              .collect(Collectors.toList()) 
                      )
                .thenApply(Collection::stream)
                .as(partitionedStream(2, i -> CompletableTask.supplyAsync(() -> awaitAndProduce1(i, 1000), executorService), Collectors.toList()))
                .whenComplete((r, e) -> {
                    System.out.println("PARTITIONED: " + r + " ON " + Thread.currentThread());
                    //System.exit(0);
                });
        
        IntFunction<Promise<Integer>> makeNewValue = v -> CompletableTask.supplyAsync(() -> awaitAndProduce2(v), executorService);
        // MUST be Promises.streamCompletions(..., false) -- while the original stream is generator-base rather than collection based 
        try (Stream<Number> s = AsyncCompletions.stream(IntStream.range(0, 100).mapToObj(makeNewValue), 16, AsyncCompletions.Cancel.ENLISTED)) {
            s.filter(v -> v.intValue() % 20 != 0)
             .limit(4)
             .forEach(v -> System.out.println("By completion stream:: " + v));
             
        }
        
        @SuppressWarnings("unused")
        Promise<Void>   t1 = Promises.retry(() -> System.out.println("Hello!"), executorService, RetryPolicy.DEFAULT);
        
        @SuppressWarnings("unused")
        Promise<String> t2 = Promises.retry(() -> "Hello!", executorService, RetryPolicy.DEFAULT);
        
        // Must be a block of code in next sample -- otherwise ambiguity
        @SuppressWarnings("unused")
        Promise<Void>   t3 = Promises.retry(ctx -> {System.out.println("Hello!");}, executorService, RetryPolicy.DEFAULT);
        
        @SuppressWarnings("unused")
        Promise<String> t4 = Promises.retry(ctx -> "Hello!", executorService, RetryPolicy.DEFAULT);
        
        @SuppressWarnings("unused")
        Promise<Void> t5 = Promises.retry(J8Examples::nop, executorService, RetryPolicy.DEFAULT);
        
        @SuppressWarnings("unused")
        Promise<Void> t6 = Promises.retry(J8Examples::nopCtx, executorService, RetryPolicy.DEFAULT);

        
        Promise<BigInteger> tryTyping = Promises.retry(
            J8Examples::tryCalc, executorService, 
            new RetryPolicy<Number>().withResultValidator(v -> v.intValue() > 0).withMaxRetries(2)
        );
        System.out.println( tryTyping.get() );

        Promise<Object> k = CompletableTask.supplyAsync(() -> produceStringSlow("-ABC"), executorService);
        //Promise<Object> k = CompletableTask.complete("ABC", executorService);
        //Promise<Object> k = CompletableTask.supplyAsync(() -> {throw new RuntimeException();}, executorService);
        //Promise<Object> k = Promises.success("ABC");
        //Promise<Object> k = Promises.failure(new RuntimeException());
        k.dependent().delay(Duration.ofMillis(1), true).whenComplete((r, e) -> System.out.println(Thread.currentThread() + " ==> " + r + ", " + e));
        

        
        Promise<Object> k1 = CompletableTask.supplyAsync(() -> produceStringSlow("-onTimeout1"), executorService);
        k1.onTimeout("ALTERNATE1", Duration.ofMillis(50))
            .whenComplete((r, e) -> System.out.println(Thread.currentThread() + " - onTimeout(value) -> " + r));
        
        Promise<Object> k2 = CompletableTask.supplyAsync(() -> produceStringSlow("-onTimeout2"), executorService);
        k2.onTimeout(() -> "ALTERNATE2", Duration.ofMillis(50))
            .whenComplete((r, e) -> System.out.println(Thread.currentThread() + " - onTimeout(supplier) -> " + r));
        
        Promise<Object> k3 = CompletableTask.supplyAsync(() -> produceStringSlow("-orTimeout"), executorService);
        k3.orTimeout(Duration.ofMillis(30))
            .whenComplete((r, e) -> System.out.println(Thread.currentThread() + " - orTimeout -> " + e));

        
        Thread.sleep(150);

        final Promise<Number> p = Promises.any(
            CompletableTask.supplyAsync(() -> awaitAndProduce1(20, 100), executorService),
            CompletableTask.supplyAsync(() -> awaitAndProduce1(-10, 50), executorService)
        );
        p.whenComplete((r,e) -> {
           System.out.println("Result = " + r + ", Error = " + e); 
        });
        //p.cancel(true);
        p.get();
        
        Promise<String> pollerFuture = Promises.retryFuture( 
            ctx -> pollingFutureMethod(ctx, executorService),
            RetryPolicy.DEFAULT
                       .withMaxRetries(10)
                       .withBackoff(DelayPolicy.fixedInterval(200))
        );
        System.out.println("Poller (future): " + pollerFuture.get());
        
        Promise<String> pollerPlain = Promises.retry(
            J8Examples::pollingMethod, executorService, 
            RetryPolicy.DEFAULT
                       .rejectNullResult()
                       .withMaxRetries(10)
                       .withTimeout(DelayPolicy.fixedInterval(3200))
                       .withBackoff(DelayPolicy.fixedInterval(200).withMinDelay(100).withFirstRetryNoDelay())
        );
        
        System.out.println("Poller (plain): " + pollerPlain.get());

        CompletableTask
            .delay( Duration.ofMillis(100), executorService )
            .thenRun(() -> System.out.println("After initial delay"));

        
        CompletableTask
            .supplyAsync(() -> awaitAndProduceN(73), executorService)
            .as(ExtendedPromiseDecorator<Integer>::new)
            .dependent()
            .thenApply(Function.identity(), true)
            .delay( Duration.ofMillis(100), true, true )
            .thenApply(v -> {
                System.out.println("After delay: " + v);
                return v;
            }, true)
            .onTimeout(() -> 123456789, Duration.ofMillis(2000))
            .thenAcceptAsync(J8Examples::onComplete)
            .get(); 
        
        for (int i : Arrays.asList(5, -5, 10, 4)) {
            final Promise<Integer> task1 = executorService.submit(() -> awaitAndProduce1(i, 1500));
            final Promise<Integer> task2 = executorService.submit(() -> awaitAndProduce2(i + 1));
            task1.thenCombineAsync(
                     task2, 
                     (a,b) -> a + b
                 )
                 .thenAcceptAsync(J8Examples::onComplete)
                 .exceptionally(J8Examples::onError)
                ;
            if (i == 10) {
                Thread.sleep(200);
                task1.cancel(true);
            }
        }

        Promise<Integer> intermidiate;
        Promises.atLeast(
            4, //Change to 5 or 6 to see the difference -- will end up exceptionally
            executorService.submit(() -> awaitAndProduceN(2)),
            intermidiate = 
            executorService.submit(() -> awaitAndProduceN(3)).thenAcceptAsync(J8Examples::multByX).thenApply((v) -> 1234),
            executorService.submit(() -> awaitAndProduceN(5)),
            executorService.submit(() -> awaitAndProduceN(6)),
            executorService.submit(() -> awaitAndProduceN(7)),                
            executorService.submit(() -> awaitAndProduceN(8)),
            executorService.submit(() -> awaitAndProduceN(11))
        )
        .defaultAsyncOn(executorService)
        .thenApplyAsync(
                l -> l.stream().filter(v -> v != null).collect(Collectors.summingInt((Integer i) -> i.intValue()))
        )
        .thenAcceptAsync(J8Examples::onComplete)
        .exceptionally(J8Examples::onError);
        
        
        executorService.submit(() -> awaitAndProduceN(789)).whenComplete((r, e) -> {
        	if (null == e) {
        		System.out.println("On complete result: " + r);
        	} else {
        		System.out.println("On complete error: " + e);
        	}
        }).thenAccept(System.out::println);
        
        System.out.println("Intermediate result: " + intermidiate.toCompletableFuture().get());

        Promise<?> xt = CompletableTask
            .supplyAsync(() -> "" + awaitAndProduceN(10), executorService)
            .defaultAsyncOn(executorService)
            .onCancel(() -> System.out.println("CANCELLED!!!"))
            .exceptionallyComposeAsync(e -> CompletableTask.supplyAsync(() -> "<ERROR> " + e.getMessage(), executorService))
            .dependent()
            .onTimeout("XYZ", Duration.ofMillis(2000), true, true)
            .thenAccept(v -> System.out.println("Value produced via timeout: " + v))
            ;
        //xt.cancel(true);
        System.out.println(xt);

        // Suicidal task to close gracefully
        executorService.submit(() -> {
            try {
                Thread.sleep(15000);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
            executorService.shutdown();
        });
        
    }
    
    
    private static int multByX(int v) {
    	return v * 100;
    }
    
    private static int awaitAndProduce1(int i, long delay) {
        try {
            System.out.println("Delay I in " + Thread.currentThread());
            Thread.sleep(delay);
            if (i < 0) {
                throw new RuntimeException("Negative value: " + i);
            }
            return i * 10;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    
    private static int awaitAndProduce2(int i) {
        try {
            System.out.println("Delay II in " + Thread.currentThread());
            Thread.sleep(150);
            return i * 10;
        } catch (final InterruptedException ex) {
            System.out.println("Interrupted awaitAndProduce2 for " + i);
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    
    private static int awaitAndProduceN(int i) {
        try {
            System.out.println("Delay N + " + i + " in " + Thread.currentThread());
            Thread.sleep(1500);
            if (i % 2 == 0) {
                throw new RuntimeException("Even value: " + i);
            }
            return i * 1000;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.println("awaitAndProduceN interrupted, requested value " + i);
            return -1;
        }
    }
    
    
    static String produceStringSlow(String suffix) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("Interrupted" + suffix + "!!!");
            return "INTERRUPTED" + suffix;
        }
        return "PRODUCED" + suffix;
    }
    
    private static String pollingMethod(RetryContext<String> ctx) throws InterruptedException {
        System.out.println("Enter Polling method #" + ctx.getRetryCount());
        try {
            if (ctx.getRetryCount() < 5) {
                Thread.sleep((5 - ctx.getRetryCount()) * 1000);
            }
            if (ctx.getRetryCount() < 7) {
                throw new IllegalStateException();
            }
            return "Result " + ctx.getRetryCount();
        } catch (final InterruptedException ex) {
            System.out.println("Polling method, #" + ctx.getRetryCount() + ", interrupted!");
            Thread.currentThread().interrupt();
            throw ex;
        }
    }
    
    private static CompletionStage<String> pollingFutureMethod(RetryContext<String> ctx, Executor executor) throws InterruptedException {
        System.out.println("Polling future, #" + ctx.getRetryCount());
        if (ctx.getRetryCount() < 3) {
            throw new RuntimeException("Fail to start future");
        }
        if (ctx.getRetryCount() < 5) {
            return CompletableTask.supplyAsync(() -> {
                throw new RuntimeException("Fail to complete future");
            }, executor);
        }
        return CompletableTask.supplyAsync(() -> "42", executor);
    }
    
    static BigInteger tryCalc(RetryContext<Number> ctx) {
        return BigInteger.ONE;
    }
    
    static void nop() {
        
    }
    
    static void nopCtx(RetryContext<?> ctx) {
        
    }
    
    private static void onComplete(int i) {
        System.out.println(">>> Result " + i + ", " + Thread.currentThread());
    }
    
    private static Void onError(Throwable i) {
        System.out.println(">>> Error " + i + ", " + Thread.currentThread());
        return null;
    }
}
