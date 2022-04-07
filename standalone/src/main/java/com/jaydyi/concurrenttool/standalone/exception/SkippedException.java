package com.jaydyi.concurrenttool.standalone.exception;

public class SkippedException extends RuntimeException{
    public SkippedException() {
        super();
    }

    public SkippedException(String msg) {
        super(msg);
    }
}
