/*
 * Created on Oct 31, 2009
 */
package org.knowceans.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ParallelFor implements what in OpenMP is called a parallel for loop, i.e., a
 * loop that executes in parallel on several threads with a barrier (join) at
 * the end. Implementors simply subclass and implement the process() method and
 * make sure the loop() method is not called while a loop of one instance is
 * running. Execution can be stopped using stop(), which completes the
 * iterations currently running. After the last usage of the class, it should be
 * shut down properly using static function shutdown() or the hard way using
 * System.exit();
 * 
 * @author gregor
 */
public abstract class ParallelFor {

    /**
     * Worker is one pooled thread
     */
    private class Worker implements Runnable {

        public void run() {
            while (!isStopping) {
                int i = 0;
                synchronized (ParallelFor.this) {
                    i = iter++;
                }
                if (i >= niter) {
                    break;
                }
                process(i);
            }
            synchronized (ParallelFor.this) {
                ParallelFor.this.activeWorkers--;
                ParallelFor.this.notifyAll();
            }
        }
    }

    /**
     * number of threads = number of processors / cores
     */
    protected static int nthreads = Runtime.getRuntime().availableProcessors();

    /**
     * pool of threads spread over processors
     */
    protected static ExecutorService threadpool;

    /**
     * current iteration (synchronized access)
     */
    protected int iter = 0;

    /**
     * worker thread
     */
    protected final Worker worker;

    /**
     * stop flag
     */
    protected boolean isStopping;

    /**
     * loop iterations
     */
    protected int niter = 0;

    /**
     * active workers (for the barrier)
     */
    protected int activeWorkers = 0;

    /**
     * instantiate a parallel for implementation
     */
    public ParallelFor() {
        if (threadpool == null) {
            threadpool = Executors.newFixedThreadPool(nthreads);
        }
        worker = new Worker();
    }

    /**
     * Start worker threads and loop through the iterations. Should never be
     * called while the loop instance is still running.
     * 
     * @param N
     */
    public void loop(int N) {
        isStopping = false;
        niter = N;
        iter = 0;

        // start worker threads
        for (int i = 0; i < nthreads; i++) {
            threadpool.execute(worker);
            synchronized (this) {
                activeWorkers++;
            }
        }

        // wait until all worker threads are done
        while (activeWorkers > 0) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * payload for the for loop
     * 
     * @param idx
     */
    abstract public void process(int idx);

    /**
     * stop loop execution
     */
    public void stop() {
        isStopping = true;
    }

    /**
     * shut down the thread pool after final usage
     */
    public static void shutdown() {
        // immediately terminate threadpool
        try {
            threadpool.shutdown();
            threadpool.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
