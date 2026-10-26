package hu.elte.ik.thesis.cinegrade.app.managers.concurrency;

import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ThreadPoolManager implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(ThreadPoolManager.class);
    private final ThreadPoolExecutor cpuPool;
    private final ThreadPoolExecutor ioPool;
    private final ThreadPoolExecutor taskPool;
    private final int MAX_THREADS, CPU_THREADS, TASK_THREADS, IO_THREADS;

    public ThreadPoolManager() {
        this(16);
    }

    public ThreadPoolManager(int maxThreads) {
        MAX_THREADS = Math.min(maxThreads, SystemInfo.INSTANCE.getAvailableProcessors() - 2);
        CPU_THREADS = Math.max(1, MAX_THREADS / 2);
        TASK_THREADS = Math.max(1, MAX_THREADS / 4);
        IO_THREADS = Math.max(1, MAX_THREADS / 4);

        ThreadFactory cpuFactory = new DefaultThreadFactory("cinegrade-worker-cpu");
        ThreadFactory ioFactory = new DefaultThreadFactory("cinegrade-worker-io");
        ThreadFactory taskFactory = new DefaultThreadFactory("cinegrade-worker-task");

        this.cpuPool = new DefaultThreadPoolExecutor(cpuFactory, CPU_THREADS);
        this.ioPool = new DefaultThreadPoolExecutor(ioFactory, IO_THREADS);
        this.taskPool = new DefaultThreadPoolExecutor(taskFactory, TASK_THREADS);

        logger.debug("ThreadPoolManager succesfully created, new ExecutorServices with {} threads is created", MAX_THREADS);
    }

    /**
     * Gracefully shuts down the specified threadpool.
     * @param pool the pool to shut down
     */
    public void gracefulShutdown(ThreadPoolExecutor pool) {
        logger.info("Starting graceful pool shutdown! ");
        logger.debug("Active threads: {}", pool.getActiveCount());

        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            pool.shutdownNow();
        }

        logger.debug("{} Executor was succesfully shutdown!", pool.getClass().getName());
    }

    /**
     * Gracefully shuts down all thread pool executors.
     */
    @Override
    public void close() throws Exception {
        gracefulShutdown(taskPool);
        gracefulShutdown(ioPool);
        gracefulShutdown(cpuPool);
    }

    /**
     * A default thread factory for creating threads.
     */
    private static class DefaultThreadFactory implements ThreadFactory {

        private final String threadNamePrefix;
        private final AtomicInteger threadCount = new AtomicInteger(0);

        public DefaultThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, threadNamePrefix + "-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1); // Never ever block the UI thread
            thread.setUncaughtExceptionHandler(ErrorHandler.getInstance());
            return thread;
        }
    }

    /**
     * A default thread pool executor for managing threads.
     */
    private static class DefaultThreadPoolExecutor extends ThreadPoolExecutor {

        public DefaultThreadPoolExecutor(ThreadFactory threadFactory, Integer maxThreads) {
            super(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    threadFactory);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t == null && r instanceof Future<?> && ((Future<?>) r).isDone()) {
                try {
                    ((Future<?>) r).get();
                } catch (CancellationException ce) {
                    t = ce;
                } catch (ExecutionException ee) {
                    t = ee.getCause();
                } catch (InterruptedException ie) {
                    // ignore/reset
                    Thread.currentThread().interrupt();
                }
            }
            if (t != null) {
                ErrorHandler.getInstance().handle(t);
            }
        }
    }
}
