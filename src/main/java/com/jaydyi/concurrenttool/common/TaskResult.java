package com.jaydyi.concurrenttool.common;

/**
 * the wrapper of task execution result
 *
 * @param <OUT> : the type of task execution result
 */
public class TaskResult<OUT> {

    /**
     * task execution result
     */
    private OUT result;

    /**
     * task execute result status
     */
    private TaskExecutionStatus taskExecutionStatus;

    /**
     * task execution exception
     */
    private Exception ex;


    public TaskResult(OUT result, TaskExecutionStatus taskExecutionStatus) {
        this(result, taskExecutionStatus, null);
    }

    public TaskResult(OUT result, TaskExecutionStatus taskExecutionStatus, Exception ex) {
        this.result = result;
        this.taskExecutionStatus = taskExecutionStatus;
        this.ex = ex;
    }

    public static <OUT> TaskResult<OUT> defaultResult() {
        return new TaskResult<>(null, TaskExecutionStatus.DEFAULT);
    }

    public OUT getResult() {
        return result;
    }

    public void setResult(OUT result) {
        this.result = result;
    }

    public TaskExecutionStatus getTaskExecutionStatus() {
        return taskExecutionStatus;
    }

    public void setTaskExecutionStatus(TaskExecutionStatus taskExecutionStatus) {
        this.taskExecutionStatus = taskExecutionStatus;
    }

    public Exception getEx() {
        return ex;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "result=" + result +
                ", taskExecutionStatus=" + taskExecutionStatus +
                ", ex=" + ex +
                '}';
    }
}
