package com.jaydyi.concurrenttool.standalone.common;

/**
 * if not set callback object, set this default callback object
 *
 * @param <IN>
 * @param <OUT>
 */
public class DefaultTaskCallback<IN, OUT> implements ITaskCallback<IN, OUT> {

    @Override
    public void result(IN param, TaskResult<OUT> taskResult) {

    }
}
