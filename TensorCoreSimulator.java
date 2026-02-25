import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class TensorCoreSimulator {

    static final int N = 1000;

    static int[][] A = new int[N][N];
    static int[][] B = new int[N][N];
    static int[][] C = new int[N][N];

    static long totalOps = 0;
    static ReentrantLock lock = new ReentrantLock();
    static Semaphore semaphore = new Semaphore(2);

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Tensor Core Simulator (N=" + N + ") ===");
        System.out.println("Generating data...");

        generateMatrices();

        // -------- Part A --------
        System.out.println("\n--- Part A: Sequential Execution ---");
        long start = System.currentTimeMillis();
        sequentialMultiply();
        long end = System.currentTimeMillis();
        System.out.println("Sequential Time: " + (end - start) + " ms");

        // -------- Part B --------
        System.out.println("\n--- Part B: Parallel Execution (4 Threads) ---");
        totalOps = 0;
        start = System.currentTimeMillis();
        parallelMultiply(4, false);
        end = System.currentTimeMillis();
        System.out.println("Parallel Time: " + (end - start) + " ms");

        // -------- Part C & D --------
        System.out.println("\n--- Part C & D: Parallel Execution with Audit (Billing) ---");
        totalOps = 0;
        start = System.currentTimeMillis();
        parallelMultiply(8, true);
        end = System.currentTimeMillis();
        System.out.println("Safe Parallel Time: " + (end - start) + " ms");

        System.out.println("\nTotal Operations Logged: " + totalOps);
        System.out.println("Expected Operations: " + (long) N * N * N);
    }

    static void generateMatrices() {
        Random rand = new Random();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = rand.nextInt(10);
                B[i][j] = rand.nextInt(10);
            }
        }
    }

    static void sequentialMultiply() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int sum = 0;
                for (int k = 0; k < N; k++) {
                    sum += A[i][k] * B[k][j];
                }
                C[i][j] = sum;
            }
        }
    }

    static void parallelMultiply(int threadCount, boolean safeMode) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];
        int rowsPerThread = N / threadCount;

        for (int t = 0; t < threadCount; t++) {
            int startRow = t * rowsPerThread;
            int endRow = (t == threadCount - 1) ? N : startRow + rowsPerThread;

            threads[t] = new Thread(new MatrixWorker(startRow, endRow, safeMode));
            threads[t].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    static class MatrixWorker implements Runnable {

        int startRow, endRow;
        boolean safeMode;

        MatrixWorker(int startRow, int endRow, boolean safeMode) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.safeMode = safeMode;
        }

        @Override
        public void run() {
            try {
                if (safeMode) {
                    semaphore.acquire();
                }

                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < N; j++) {
                        int sum = 0;
                        for (int k = 0; k < N; k++) {
                            sum += A[i][k] * B[k][j];

                            if (safeMode) {
                                lock.lock();
                                try {
                                    totalOps++;
                                } finally {
                                    lock.unlock();
                                }
                            } else {
                                totalOps++;
                            }
                        }
                        C[i][j] = sum;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (safeMode) {
                    semaphore.release();
                }
            }
        }
    }
}
