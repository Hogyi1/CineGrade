package hu.elte.ik.thesis.cinegrade.app.managers;

import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager {

    private static final Logger logger = LogManager.getLogger(ThreadPoolManager.class);
    private final ThreadPoolExecutor mainExecutorService;
    private final int MAX_THREADS;

    public ThreadPoolManager() {
        this(2);
    }

    public ThreadPoolManager(int maxThreads) {
        MAX_THREADS = Math.min(maxThreads, SystemInfo.INSTANCE.getAvailableProcessors() / 2);

        AtomicInteger threadCount = new AtomicInteger(0);
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "cinegrade-worker-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1); // Never ever block the UI thread
                thread.setUncaughtExceptionHandler(ErrorHandler.getInstance());
                return thread;
            }
        };

        this.mainExecutorService = new ThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t == null && r instanceof Future<?> && ((Future<?>) r).isDone()) {
                    try {
                        Object result = ((Future<?>) r).get();
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
        };
        logger.debug("ThreadPoolManager succesfully created, new ExecutorService with {} threads is created", MAX_THREADS);
    }

    public Future<?> submitTask(Runnable task) {
        logger.debug("Task: {} - submitted to executor", task.toString());
        return mainExecutorService.submit(task);
    }

    public void setMainExecutorService(Service<?> service) {
        service.setExecutor(mainExecutorService);
    }

    public void shutdown() {
        logger.info("Starting graceful executor shutdown! ");
        logger.debug("Active threads: {}", this::getRunningTasks);

        mainExecutorService.shutdown();
        try {
            if (mainExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                mainExecutorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            mainExecutorService.shutdownNow();
        }

        logger.debug("Executor was succesfully shutdown!");
    }

    public int getMaxThreads() {
        return MAX_THREADS;
    }

    /**
     * Approximate number of threads actively executing tasks.
     */
    public int getRunningTasks() {
        return mainExecutorService.getActiveCount();
    }

    /**
     * Total number of worker threads currently allocated and alive in the pool.
     */
    public int getLiveThreadCount() {
        return mainExecutorService.getPoolSize();
    }

    /**
     * Number of tasks queued and waiting for an available worker thread.
     */
    public int getQueuedTaskCount() {
        return mainExecutorService.getQueue().size();
    }

    public ExecutorService getExecutorService() {
        return mainExecutorService;
    }
}
