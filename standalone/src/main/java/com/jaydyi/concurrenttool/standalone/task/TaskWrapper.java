package com.jaydyi.concurrenttool.standalone.task;

import com.jaydyi.concurrenttool.standalone.common.*;
import com.jaydyi.concurrenttool.standalone.exception.SkippedException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * the wrapper of task and task's callback
 *
 * @param <IN> : the type of task input param
 * @param <OUT> : the type of task output
 */
public class TaskWrapper<IN, OUT> {
    private static final Logger logger = LoggerFactory.getLogger(TaskWrapper.class);

    /**
     * the unique id of task wrapper
     */
    private String id;

    private String name;

    /**
     * the input param of task
     */
    private IN param;

    /**
     * task
     */
    private ITask<IN, OUT> task;

    /**
     * task callback
     */
    private ITaskCallback<IN, OUT> callback;


    private List<TaskWrapper<?, ?>> nextTaskWrappers;


    private List<PreTaskWrapper> preTaskWrappers;

    private Map<String, TaskWrapper> taskWrapperMap;

    private volatile TaskResult<OUT> taskResult = TaskResult.defaultResult();

    private volatile boolean validNextWrapperResultFlag = true;


    private static final int INIT = 0;
    private static final int FINISH = 1;
    private static final int ERROR = 2;
    private static final int WORKING = 3;
    private AtomicInteger state = new AtomicInteger(0);


    private TaskWrapper(String id, String name, IN param, ITask<IN, OUT> task, ITaskCallback<IN, OUT> callback) {
        this.id = id;
        this.name = name;
        this.param = param;
        this.task = task;
        if (callback == null) {
            // allow callback set null to fit that don't care task result scene
            this.callback = new DefaultTaskCallback<IN, OUT>();
        } else {
            this.callback = callback;
        }
    }

    private void addPre(TaskWrapper<?, ?> taskWrapper, boolean isRequired) {
        addPre(new PreTaskWrapper(taskWrapper, isRequired));
    }

    private void addPre(PreTaskWrapper taskWrapper) {
        if (preTaskWrappers == null) {
            preTaskWrappers = new ArrayList<>();
        }
        if (preTaskWrappers.contains(taskWrapper)) {
            return;
        }
        preTaskWrappers.add(taskWrapper);
    }

    private void addNext(TaskWrapper<?, ?> taskWrapper) {
        if (nextTaskWrappers == null) {
            nextTaskWrappers = new ArrayList<>();
        }
        if (nextTaskWrappers.contains(taskWrapper)) {
            // avoid repeat add
            return;
        }
        nextTaskWrappers.add(taskWrapper);
    }


    public void run(ExecutorService executorService, long remainTime, Map<String, TaskWrapper> taskWrapperMap) {
        run(executorService, remainTime, taskWrapperMap, null);
    }

    public void run(ExecutorService executorService, long remainTime, Map<String, TaskWrapper> taskWrapperMap, TaskWrapper fromTaskWrapper) {
        this.taskWrapperMap = taskWrapperMap;
        taskWrapperMap.put(id, this);

        long now = System.currentTimeMillis();

        // 任务链总时间已超时
        if (remainTime <= 0) {
            // 快速失败
            fastFail(INIT, null);
            // 传递下去
            beginNext(executorService, now ,remainTime);
            return;
        }

        // 存在多条optional pre task时，可能任意一个pre task已触发过了，则直接跳过
        if (getState() == FINISH || getState() == ERROR) {
            beginNext(executorService, now, remainTime);
            return;
        }

        // 如果子节点，已经开始执行了，则跳过，不要再执行了
        if (validNextWrapperResultFlag) {
            if (!isNextHasBegin()) {
                fastFail(INIT, new SkippedException());
                beginNext(executorService, now, remainTime);
                return;
            }
        }

        // 没有任何依赖，立即开始执行自己的任务
        if (CollectionUtils.isEmpty(preTaskWrappers)) {
            noPreTaskCondition(executorService, now, remainTime);
            return;
        }

        // 有依赖, 按序检查前置依赖的完成情况
        hasPreTasksCondition(executorService, fromTaskWrapper, now, remainTime);
    }


    private boolean isNextHasBegin() {
        if (CollectionUtils.isEmpty(nextTaskWrappers) || nextTaskWrappers.size() != 1) {
            // 自己就是最后一个节点或者后面有并行的多个，就返回true
            return getState() == INIT;
        }
        TaskWrapper nextTask = nextTaskWrappers.get(0);
        return nextTask.getState() == INIT && nextTask.isNextHasBegin();
    }

    /**
     * handle no dependency task scene
     *
     * @param executorService thread pool
     * @param start  self task begin time.
     * @param remainTime remain time
     */
    private void noPreTaskCondition(ExecutorService executorService, long start, long remainTime) {
        beginSelf();
        beginNext(executorService, start, remainTime);
    }

    /**
     * handle has dependency task scene
     *
     * @param executorService
     * @param fromTaskWrapper
     * @param start
     * @param remainTime
     */
    private synchronized void hasPreTasksCondition(ExecutorService executorService, TaskWrapper fromTaskWrapper, long start, long remainTime) {
        // it means fromTaskWrapper is task's unique pre task
        if (preTaskWrappers.size() == 1) {
            // 失败的话，延续依赖任务失败的状态
            if (fromTaskWrapper.getTaskResult().getTaskExecutionStatus() == TaskExecutionStatus.TIMEOUT) {
                defaultTimeOutResult();
                fastFail(INIT, null);
            } else if (fromTaskWrapper.getTaskResult().getTaskExecutionStatus() == TaskExecutionStatus.FAILED) {
                defaultExResult(fromTaskWrapper.getTaskResult().getEx());
                fastFail(INIT, null);
            } else {
                beginSelf();
                beginNext(executorService, start, remainTime);
            }
        } else {
            // has multiple pre task
            // pre task has two dependency condition: required or optional
            // for required tasks, we must wait for all task to be succeed before we can continue.
            // for optional tasks, we just need to wait for any one of them to be succeed before wo can continue.

            // find out all required pre task wrapper
            Set<PreTaskWrapper> requiredPreTaskWrapper = new HashSet<>();
            boolean fromIsRequired = false;

            for (PreTaskWrapper preTaskWrapper: preTaskWrappers) {
                if (preTaskWrapper.isRequired()) {
                    requiredPreTaskWrapper.add(preTaskWrapper);
                }
                if (preTaskWrapper.getPreTaskWrapper().equals(fromTaskWrapper)) {
                    fromIsRequired = true;
                }
            }

            if (requiredPreTaskWrapper.size() == 0) {
                if (fromTaskWrapper.getTaskResult().getTaskExecutionStatus() == TaskExecutionStatus.TIMEOUT) {
                    fastFail(INIT, null);
                } else {
                    beginSelf();
                }
                beginNext(executorService, start, remainTime);
                return;
            }

            // besides fromTaskWrapper, there is required task wrapper, so wait an return
            if (!fromIsRequired) {
                return;
            }

            // check required pre task
            boolean hasFailed = false;
            boolean hasNotFinished = false;

            for (PreTaskWrapper requiredPreTask: requiredPreTaskWrapper) {
                TaskWrapper taskWrapper = requiredPreTask.getPreTaskWrapper();
                TaskResult taskWrapperResult = taskWrapper.getTaskResult();

                if (taskWrapper.getState() == INIT || taskWrapper.getState() == WORKING) {
                    hasNotFinished = true;
                    break;
                }
                if (taskWrapperResult.getTaskExecutionStatus() == TaskExecutionStatus.TIMEOUT) {
                    defaultTimeOutResult();
                    hasFailed = true;
                    break;
                }
                if (taskWrapperResult.getTaskExecutionStatus() == TaskExecutionStatus.FAILED) {
                    defaultExResult(taskWrapperResult.getEx());
                    hasFailed = true;
                    break;
                }
            }

            // has required pre task failed, fast fail and transfer
            if (hasFailed) {
                fastFail(INIT, null);
                beginNext(executorService, start, remainTime);
                return;
            }

            // has not finished pre task, return
            if (hasNotFinished) {
                return;
            }

            beginSelf();
            beginNext(executorService, start, remainTime);
        }
    }

    public void beginNext(ExecutorService executorService, long start, long remainTime) {
        long costTime  = System.currentTimeMillis() - start;
        if (CollectionUtils.isEmpty(nextTaskWrappers)) {
            return;
        }
        if (nextTaskWrappers.size() == 1) {
            // 后续只有一个任务待执行，复用当前线程
            nextTaskWrappers.get(0).run(executorService, remainTime - costTime, taskWrapperMap, this);
            return;
        }

        // 有多个后续任务，往线程池里丢，去并发执行
        CompletableFuture[] futures = new CompletableFuture[nextTaskWrappers.size()];
        for (int i = 0; i < nextTaskWrappers.size(); i++) {
            final int fi = i;
            futures[i] = CompletableFuture.runAsync(
                    () -> nextTaskWrappers.get(fi)
                            .run(executorService, remainTime - costTime, taskWrapperMap, this),
                    executorService);
        }
        try {
            CompletableFuture.allOf(futures).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("beginNext wait all next task execute exception.", e);
        }
    }


    // 存在多线程的竞争，需要避免重复执行
    public void beginSelf() {
        if (!verifyIsNullResult()) {
            // if taskResult is not null, it means task has executed, should directly return, avoid repeat execution.
            return;
        }

        try {
            if (!compareAndSetState(INIT, WORKING)) {
                // taskResult is null and task state is not init, it means task is executing, should directly return, avoid repeat execution.
                return;
            }

            //
            callback.begin();

            OUT result = task.doTask(param, taskWrapperMap);

            if (!compareAndSetState(WORKING, FINISH)) {
                // 加一重保护，避免重复执行
                return;
            }

            taskResult.setTaskExecutionStatus(TaskExecutionStatus.SUCCESS);
            taskResult.setResult(result);

            callback.result(param, taskResult);
        } catch (Exception e) {
            if (!verifyIsNullResult()) {
                // 有其它线程执行完毕，直接使用结果
                return;
            }
            fastFail(WORKING, e);
        }
    }

    // 默认是超时快速失败
    private boolean fastFail(int fromState, Exception e) {
        if (!compareAndSetState(fromState, ERROR)) {
            return false;
        }
        if (verifyIsNullResult()) {
            if (e == null) {
                // 非异常的快速退出，只对应超时场景
                defaultTimeOutResult();
            } else {
                defaultExResult(e);
            }
        }

        callback.result(param, taskResult);
        return true;
    }

    private void defaultTimeOutResult() {
        taskResult.setResult(task.defaultReturn());
        taskResult.setTaskExecutionStatus(TaskExecutionStatus.TIMEOUT);
    }

    private void defaultExResult(Exception e) {
        taskResult.setResult(task.defaultReturn());
        taskResult.setTaskExecutionStatus(TaskExecutionStatus.FAILED);
        taskResult.setEx(e);
    }




    private boolean verifyIsNullResult() {
        return taskResult.getTaskExecutionStatus() == TaskExecutionStatus.DEFAULT;
    }

    public void stopNow() {
        if (getState() == INIT || getState() == WORKING) {
            fastFail(getState(), null);
        }
    }

    public static void showTaskDAG(TaskWrapper... taskWrapper) {
        if (null == taskWrapper) {
            return;
        }
        logger.info("begin to show task DAG:");
        Deque<TaskWrapper> deque = new ArrayDeque<>();
        for (TaskWrapper tmp: taskWrapper) {
            deque.offer(tmp);
        }

        while (!deque.isEmpty()) {
            int num = deque.size();
            while (num --> 0) {
                TaskWrapper wrapper = deque.poll();
                if (wrapper.getNextWrappers() != null) {
                    for (Object next: wrapper.getNextWrappers()) {
                        if (next != null) {
                            deque.offer((TaskWrapper)next);
                            logger.info("{} -> {}", wrapper.getId(), ((TaskWrapper)next).getId());
                        }
                    }
                }
            }
        }
        logger.info("end show task DAG.");
    }


    // ==============================================================
    //  builder define
    // ==============================================================

    public static class Builder<BIN, BOUT> {
        /**
         * the unique id of task wrapper
         */
        private String id = UUID.randomUUID().toString();

        private String name;

        /**
         * the input param of task
         */
        private BIN param;

        /**
         * task
         */
        private ITask<BIN, BOUT> task;

        /**
         * task callback
         */
        private ITaskCallback<BIN, BOUT> callback;


        private List<TaskWrapper<?, ?>> nextTaskWrappers;


        private List<PreTaskWrapper> preTaskWrappers;


        private Set<TaskWrapper<?, ?>> selfIsRequiredSet;

        private volatile boolean validNextWrapperResultFlag = true;

        public Builder<BIN, BOUT> id (String id) {
            this.id = id;
            return this;
        }

        public Builder<BIN, BOUT> name (String name) {
            this.name = name;
            return this;
        }

        public Builder<BIN, BOUT> task(ITask<BIN, BOUT> task) {
            this.task = task;
            return this;
        }

        public Builder<BIN, BOUT> param(BIN param) {
            this.param = param;
            return this;
        }

        public Builder<BIN, BOUT> callback(ITaskCallback<BIN, BOUT> callback) {
            this.callback = callback;
            return this;
        }

        public Builder<BIN, BOUT> pre(TaskWrapper<?, ?>... wrappers) {
            if (wrappers == null) {
                return this;
            }
            for (TaskWrapper<?, ?> wrapper: wrappers) {
                pre(wrapper);
            }
            return this;
        }

        public Builder<BIN, BOUT> pre(TaskWrapper<?, ?> wrapper) {
            if (wrapper == null) {
                return this;
            }
            return pre(wrapper, true);
        }

        public Builder<BIN, BOUT> pre(TaskWrapper<?, ?> wrapper, boolean isRequired) {
            if (wrapper == null) {
                return this;
            }
            PreTaskWrapper preTaskWrapper = new PreTaskWrapper(wrapper, isRequired);
            if (preTaskWrappers == null) {
                preTaskWrappers = new ArrayList<>();
            }
            preTaskWrappers.add(preTaskWrapper);
            return this;
        }

        public Builder<BIN, BOUT> next(TaskWrapper<?, ?> wrapper) {
            if (wrapper == null) {
                return this;
            }
            return next(wrapper, true);
        }

        public Builder<BIN, BOUT> next(TaskWrapper<?, ?> wrapper, boolean selfIsRequired) {
            if (wrapper == null) {
                return this;
            }
            if (nextTaskWrappers == null) {
                nextTaskWrappers = new ArrayList<>();
            }
            nextTaskWrappers.add(wrapper);

            if (selfIsRequired) {
                if (selfIsRequiredSet == null) {
                    selfIsRequiredSet = new HashSet<>();
                }
                selfIsRequiredSet.add(wrapper);
            }

            return this;
        }


        public Builder<BIN, BOUT> next(TaskWrapper<?, ?>... wrappers) {
            if (wrappers == null) {
                return this;
            }
            for (TaskWrapper<?, ?> wrapper: wrappers) {
                next(wrapper);
            }
            return this;
        }

        public TaskWrapper<BIN, BOUT> build() {
            TaskWrapper<BIN, BOUT> wrapper = new TaskWrapper<>(id, name, param, task, callback);
            // default to check next tasks result
            wrapper.setValidNextWrapperResultFlag(true);
            if (preTaskWrappers != null) {
                for (PreTaskWrapper preTaskWrapper: preTaskWrappers) {
                    // pre -> task ---
                    preTaskWrapper.getPreTaskWrapper().addNext(wrapper);
                    // pre <- task ---
                    wrapper.addPre(preTaskWrapper);
                }
            }

            if (nextTaskWrappers != null) {
                for (TaskWrapper nextTaskWrapper: nextTaskWrappers) {
                    boolean required = false;
                    if (selfIsRequiredSet != null && selfIsRequiredSet.contains(nextTaskWrapper)) {
                        required = true;
                    }
                    // --- task <- next
                    nextTaskWrapper.addPre(wrapper, required);
                    // --- task -> next
                    wrapper.addNext(nextTaskWrapper);
                }
            }

            return wrapper;
        }

    }



    // ==============================================================
    //  bean define
    // ==============================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IN getParam() {
        return param;
    }

    public void setParam(IN param) {
        this.param = param;
    }

    public ITask<IN, OUT> getTask() {
        return task;
    }

    public void setTask(ITask<IN, OUT> task) {
        this.task = task;
    }

    public ITaskCallback<IN, OUT> getCallback() {
        return callback;
    }

    public void setCallback(ITaskCallback<IN, OUT> callback) {
        this.callback = callback;
    }

    public List<TaskWrapper<?, ?>> getNextWrappers() {
        return nextTaskWrappers;
    }

    public void setNextWrappers(List<TaskWrapper<?, ?>> nextWrappers) {
        this.nextTaskWrappers = nextWrappers;
    }

    public List<PreTaskWrapper> getPreTaskWrappers() {
        return preTaskWrappers;
    }

    public void setPreTaskWrappers(List<PreTaskWrapper> preTaskWrappers) {
        this.preTaskWrappers = preTaskWrappers;
    }

    public boolean isValidNextWrapperResultFlag() {
        return validNextWrapperResultFlag;
    }

    public void setValidNextWrapperResultFlag(boolean validNextWrapperResultFlag) {
        this.validNextWrapperResultFlag = validNextWrapperResultFlag;
    }

    public int getState() {
        return state.get();
    }

    public boolean compareAndSetState(int expect, int update) {
        return state.compareAndSet(expect, update);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskResult<OUT> getTaskResult() {
        return taskResult;
    }
}
