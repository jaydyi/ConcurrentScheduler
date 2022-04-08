package com.jaydyi.concurrenttool.exception;

public class SkippedException extends RuntimeException{
    public SkippedException() {
        super();
    }

    public SkippedException(String msg) {
        super(msg);
    }
}
