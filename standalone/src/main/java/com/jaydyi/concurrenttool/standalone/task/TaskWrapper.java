package com.jaydyi.concurrenttool.standalone.task;

import com.jaydyi.concurrenttool.standalone.common.DefaultTaskCallback;
import com.jaydyi.concurrenttool.standalone.common.ITask;
import com.jaydyi.concurrenttool.standalone.common.ITaskCallback;
import com.jaydyi.concurrenttool.standalone.common.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

        public Builder<BIN, BOUT> next(TaskWrapper<?, ?> wrapper, boolean isRequired) {
            if (wrapper == null) {
                return this;
            }
            if (nextTaskWrappers == null) {
                nextTaskWrappers = new ArrayList<>();
            }
            nextTaskWrappers.add(wrapper);

            if (isRequired) {
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




    public void run(ExecutorService executorService, long remainTime, Map<String, TaskWrapper> taskWrapperMap) {

    }

    public void stopNow() {

    }


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void showTaskDAG(TaskWrapper taskWrapper) {
        if (null == taskWrapper) {
            return;
        }
        logger.info("begin to show task DAG:");
        Deque<TaskWrapper> deque = new ArrayDeque<>();
        deque.offer(taskWrapper);

        while (!deque.isEmpty()) {
            int num = deque.size();
            while (num --> 0) {
                TaskWrapper wrapper = deque.poll();
                if (wrapper.getNextWrappers() != null) {
                    for (Object next: wrapper.getNextWrappers()) {
                        if (next != null) {
                            deque.offer((TaskWrapper)next);
                            logger.info("{} -> {}", wrapper.getName(), ((TaskWrapper)next).getName());
                        }
                    }
                }
            }
        }
        logger.info("end show task DAG.");
    }


}
