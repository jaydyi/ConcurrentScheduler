package linear;

import com.jaydyi.concurrenttool.standalone.common.ITask;
import com.jaydyi.concurrenttool.standalone.common.ITaskCallback;
import com.jaydyi.concurrenttool.standalone.common.TaskExecutionStatus;
import com.jaydyi.concurrenttool.standalone.common.TaskResult;
import com.jaydyi.concurrenttool.standalone.task.TaskWrapper;

import java.util.Map;

public class LinearTask2 implements ITask<String, String>, ITaskCallback<String, String> {
    @Override
    public String doTask(String param, Map<String, TaskWrapper> taskWrappers) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "now: " + System.currentTimeMillis() + "; param = " + param + " from 2";
    }

    @Override
    public void begin() {

    }

    @Override
    public String defaultReturn() {
        return "task2 default result";
    }

    @Override
    public void result(String param, TaskResult<String> taskResult) {
        if (taskResult.getTaskExecutionStatus() == TaskExecutionStatus.SUCCESS) {
            System.out.println("callback task2 success, now is " + System.currentTimeMillis() + ", " + taskResult.getResult()
                    + ", threadName is " + Thread.currentThread().getName());
        } else {
            System.err.println("callback task2 error, now is " + System.currentTimeMillis() + ", " + taskResult.getResult()
                    + ", threadName is " + Thread.currentThread().getName());
        }
    }
}
