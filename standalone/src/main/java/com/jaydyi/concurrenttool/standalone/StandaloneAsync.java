package com.jaydyi.concurrenttool.standalone;

import com.jaydyi.concurrenttool.standalone.task.TaskWrapper;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static boolean start(long timeout, ExecutorService executorService, List<TaskWrapper> taskWrappers) {
        if (CollectionUtils.isNotEmpty(taskWrappers)) {
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
            if (CollectionUtils.isEmpty(taskWrapper.getNextWrappers())) {
                continue;
            }
            traversalTotalTask(taskWrapper.getNextWrappers(), taskWrapperSet);
        }
    }

    public static void shutDown() {
        shutDown(executorService);
    }



    public static String getTheadPoolTaskInfo() {
        if (executorService == null) {
            return "activeCount=" + DEFAULT_POOL.getActiveCount() +
                    ", completedCount " + DEFAULT_POOL.getCompletedTaskCount() +
                    ", largestCount " + DEFAULT_POOL.getLargestPoolSize();
        }
        return "";
    }


    private static void shutDown(ExecutorService executorService) {
        if (executorService != null) {
            executorService.shutdown();
        } else {
            DEFAULT_POOL.shutdown();
        }
    }
}
