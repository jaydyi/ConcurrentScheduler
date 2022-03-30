package com.jaydyi.concurrenttool.standalone.common;

import com.jaydyi.concurrenttool.standalone.task.TaskWrapper;

import java.util.Map;

/**
 * every task should implements this interface
 */
public interface ITask<IN, OUT> {

    /**
     * the task business logic
     *
     * @param param: task input param
     * @param taskWrappers: taskWrapper id -> taskWrapper
     *
     * @return task result
     */
    OUT doTask(IN param, Map<String, TaskWrapper> taskWrappers);
}
