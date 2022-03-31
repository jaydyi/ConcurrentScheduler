package com.jaydyi.concurrenttool.standalone.task;

public class PreTaskWrapper {
    private TaskWrapper<?, ?> preTaskWrapper;

    /**
     * used to identify the pre dependency is required or optional.
     *
     * one task depend on one more task,
     * the task will launch when any one of the dependency task which isRequired is set to false to execute finish.
     * otherwise,
     * the task will launch when all of the dependency task which isRequired is set to true to execute finish.
     *
     * eg: two scene:
     * a
     * ---c
     * b
     * a and b finished, c begin
     *
     *
     * a---c
     * b---c
     * a or b finished, c begin
     */
    private boolean isRequired = true;

    public PreTaskWrapper(TaskWrapper<?, ?> preTaskWrapper, boolean isRequired) {
        this.preTaskWrapper = preTaskWrapper;
        this.isRequired = isRequired;
    }

    public TaskWrapper<?, ?> getPreTaskWrapper() {
        return preTaskWrapper;
    }

    public void setPreTaskWrapper(TaskWrapper<?, ?> preTaskWrapper) {
        this.preTaskWrapper = preTaskWrapper;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    @Override
    public String toString() {
        return "PreTaskWrapper{" +
                "preTaskWrapper=" + preTaskWrapper +
                ", isRequired=" + isRequired +
                '}';
    }
}
