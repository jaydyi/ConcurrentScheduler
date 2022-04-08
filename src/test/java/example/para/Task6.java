package example.para;

import com.jaydyi.concurrenttool.common.ITask;
import com.jaydyi.concurrenttool.common.ITaskCallback;
import com.jaydyi.concurrenttool.common.TaskExecutionStatus;
import com.jaydyi.concurrenttool.common.TaskResult;
import com.jaydyi.concurrenttool.task.TaskWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Task6 implements ITask<String, String>, ITaskCallback<String, String> {
    private static final Logger logger = LoggerFactory.getLogger(Task6.class);

    private static final String TASK_NAME = "task6";

    private long timeout = 1000;

    public Task6() {

    }

    public Task6(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String doTask(String param, Map<String, TaskWrapper> taskWrappers) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            logger.error("sleep interrupted");
        }
        return TASK_NAME + " Result: [now: " + System.currentTimeMillis() + "; param = " + param + "]";
    }

    @Override
    public void begin() {

    }

    @Override
    public String defaultReturn() {
        return TASK_NAME + " default result";
    }

    @Override
    public void result(String param, TaskResult<String> taskResult) {
        if (taskResult.getTaskExecutionStatus() == TaskExecutionStatus.SUCCESS) {
            logger.info("callback {} success, now is {}, result is [{}], threadName is {}",
                    TASK_NAME, System.currentTimeMillis(), taskResult, Thread.currentThread().getName());
        } else {
            logger.error("callback {} failed, now is {}, result is [{}], threadName is {}",
                    TASK_NAME, System.currentTimeMillis(), taskResult, Thread.currentThread().getName());
        }
    }
}
