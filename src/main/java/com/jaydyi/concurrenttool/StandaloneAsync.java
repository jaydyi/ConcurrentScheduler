package com.jaydyi.concurrenttool;

import com.jaydyi.concurrenttool.task.TaskWrapper;

import java.util.*;
import java.util.concurrent.*;

/**
 * entrypoint
 */
public class StandaloneAsync {
    /**
     * default cached thread pool.
     */
    private static final ThreadPoolExecutor DEFAULT_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    /**
     * user define thread pool
     */
    private static ExecutorService executorService;

    /**
     *
     * @param timeout : 这个超时时间，会存在一定的误差，在几十毫秒左右;
     *                超时后，会将任务快速失败，但是处于执行中的线程不会自动退出，会继续执行，只是结果已设置为超时;
     *                要想执行中的线程也立即结束，只能在main线程，调用StandaloneAsync.shutDownNow()。
     * @param taskWrappers: 封装的待执行任务
     * @return
     */
    public static boolean start(long timeout, TaskWrapper... taskWrappers) {
        return start(timeout, DEFAULT_POOL, taskWrappers);
    }

    public static boolean start(long timeout, ExecutorService executorService, TaskWrapper... taskWrappers) {
        if (taskWrappers == null || taskWrappers.length == 0) {
            return false;
        }
        return start(timeout, executorService, Arrays.asList(taskWrappers));
    }

    public static boolean start(long timeout, ExecutorService executorService, List<TaskWrapper> taskWrappers) {
        if (taskWrappers == null || taskWrappers.size() == 0) {
            return false;
        }
        StandaloneAsync.executorService = executorService;

        // task wrapper id -> task wrapper,  user can get wrapper result by wrapper id
        Map<String, TaskWrapper> taskWrapperMap = new ConcurrentHashMap<>();

        CompletableFuture[] futures = new CompletableFuture[taskWrappers.size()];

        for (int i = 0; i < taskWrappers.size(); i++) {
            TaskWrapper taskWrapper = taskWrappers.get(i);
            futures[i] = CompletableFuture.runAsync(() -> taskWrapper.run(executorService, timeout, taskWrapperMap),
                    executorService);
        }

        try {
            CompletableFuture.allOf(futures).get(timeout, TimeUnit.MILLISECONDS);
            return true;
        }  catch (TimeoutException | InterruptedException | ExecutionException e) {
            Set<TaskWrapper> allTaskWrapperSet = new HashSet<>();
            traversalTotalTask(taskWrappers, allTaskWrapperSet);
            allTaskWrapperSet.forEach(TaskWrapper::stopNow);
            return false;
        }
    }

    /**
     * get all task wrapper by task dependency relation
     *
     * @param taskWrappers: initial task wrappers
     * @param taskWrapperSet: add task wrappers to set
     */
    private static void traversalTotalTask(List<TaskWrapper> taskWrappers, Set<TaskWrapper> taskWrapperSet) {
        taskWrapperSet.addAll(taskWrappers);
        for (TaskWrapper taskWrapper: taskWrappers) {
            if (taskWrapper.getNextWrappers() == null || taskWrapper.getNextWrappers().size() == 0) {
                continue;
            }
            traversalTotalTask(taskWrapper.getNextWrappers(), taskWrapperSet);
        }
    }

    public static void shutDown() {
        shutDown(executorService);
    }

    public static void shutDownNow() {
        shutDownNow(executorService);
    }

    public static String getDefaultTheadPoolTaskInfo() {
        return "activeCount=" + DEFAULT_POOL.getActiveCount() +
                ", completedCount " + DEFAULT_POOL.getCompletedTaskCount() +
                ", largestCount " + DEFAULT_POOL.getLargestPoolSize();
    }


    private static void shutDown(ExecutorService executorService) {
        if (executorService != null) {
            executorService.shutdown();
        } else {
            DEFAULT_POOL.shutdown();
        }
    }

    private static void shutDownNow(ExecutorService executorService) {
        if (executorService != null) {
            executorService.shutdownNow();
        } else {
            DEFAULT_POOL.shutdownNow();
        }
    }
}
