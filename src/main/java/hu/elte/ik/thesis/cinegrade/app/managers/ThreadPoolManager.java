package hu.elte.ik.thesis.cinegrade.app.managers;

import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager {

    private final ThreadPoolExecutor mainExecutorService;
    private final int MAX_THREADS;

    public ThreadPoolManager(int maxThreads) {
        MAX_THREADS = Math.min(maxThreads, SystemInfo.INSTANCE.getAvailableProcessors() / 2);

        AtomicInteger threadCount = new AtomicInteger(0);
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "cinegrade-worker-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1); // Never ever block the UI thread
                return thread;
            }
        };

        // TODO add ErrorHandler to this executor service
        this.mainExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS, threadFactory);
    }

    public ThreadPoolManager() {
        this(2);
    }

    public void submitTask(Runnable task) {
        mainExecutorService.submit(task);
    }

    public void setMainExecutorService(Service<?> service) {
        service.setExecutor(mainExecutorService);
    }

    public void shutdown(){
        mainExecutorService.shutdown();
        try {
            if (mainExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                mainExecutorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            mainExecutorService.shutdownNow();
        }
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
