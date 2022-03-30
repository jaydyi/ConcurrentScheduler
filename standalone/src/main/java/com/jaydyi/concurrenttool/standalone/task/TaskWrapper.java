package com.jaydyi.concurrenttool.standalone.task;

import com.jaydyi.concurrenttool.standalone.common.ITask;
import com.jaydyi.concurrenttool.standalone.common.ITaskCallback;

import java.util.List;

/**
 * the wrapper of task and task's callback
 *
 * @param <IN> : the type of task input param
 * @param <OUT> : the type of task output
 */
public class TaskWrapper<IN, OUT> {
    /**
     * the unique id of task wrapper
     */
    private String taskId;

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


    private List<TaskWrapper<?, ?>> nextWrappers;


    private List<PreTaskWrapper> preTaskWrappers;

}
