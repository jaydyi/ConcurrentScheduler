package com.jaydyi.concurrenttool.standalone.common;

/**
 * the status of task execute result
 */
public enum TaskExecutionStatus {
    /**
     * task execute success
     */
    SUCCESS,

    /**
     * task execute timeout
     */
    TIMEOUT,

    /**
     * task execute error
     */
    ERROR,

    /**
     * the default status
     */
    DEFAULT
}