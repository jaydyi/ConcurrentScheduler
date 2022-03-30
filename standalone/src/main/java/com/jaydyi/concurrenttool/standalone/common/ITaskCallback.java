package com.jaydyi.concurrenttool.standalone.common;

/**
 * every task will callback this interface when it execute end.
 *
 * so if you want fetch task execution result, please to implements this interface.
 */
@FunctionalInterface
public interface ITaskCallback<IN, OUT> {

    /**
     * call begin task execute
     */
    default void begin() {

    }

    /**
     * set result value after task execute finish
     *
     * @param param: any param which want to transfer outer
     * @param taskResult: task execution result
     */
    void result(IN param, TaskResult<OUT> taskResult);
}
