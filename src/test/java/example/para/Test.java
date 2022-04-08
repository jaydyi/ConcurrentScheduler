package example.para;

import com.jaydyi.concurrenttool.StandaloneAsync;
import com.jaydyi.concurrenttool.task.TaskWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {
  private static final Logger logger = LoggerFactory.getLogger(Test.class);
  public static void main(String[] args) {
    testScene6();


    //
  }


  /**
   * 并发无依赖
   */
  private static void testScene1() {
    Task1 task1 = new Task1(20000);
    Task2 task2 = new Task2(20000);
    Task3 task3 = new Task3(20000);

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .build();

    StandaloneAsync.start(1990, wrapper1, wrapper2, wrapper3);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();
  }


  /**
   * 存在依赖关系, DAG构建之前置法
   *
   * 1和2同时开始， 1结束后3开始，2和3都完成后，开始4
   * 1 -> 3 |
   *        | -> 4
   * 2 ->   |
   */
  private static void testScene2() {
    Task1 task1 = new Task1();
    Task2 task2 = new Task2();
    Task3 task3 = new Task3();
    Task4 task4 = new Task4();

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .pre(wrapper1)  // 前置依赖写法
            .build();

    TaskWrapper<String, String> wrapper4 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t4")
            .param("p3")
            .callback(task4)
            .pre(wrapper2, wrapper3)  // 前置依赖写法
            .build();

    TaskWrapper.showTaskDAG(wrapper1, wrapper2);

    StandaloneAsync.start(100000, wrapper1, wrapper2);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();
  }

  /**
   * 存在依赖关系, DAG构建之后置法
   *
   * 1和2同时开始， 1结束后3开始，2和3都完成后，开始4
   * 1 -> 3 |
   *        | -> 4
   * 2 ->   |
   */
  private static void testScene2_1() {
    Task1 task1 = new Task1();
    Task2 task2 = new Task2();
    Task3 task3 = new Task3();
    Task4 task4 = new Task4();

    TaskWrapper<String, String> wrapper4 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t4")
            .param("p3")
            .callback(task4)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .next(wrapper4)  // 后置依赖写法
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .next(wrapper4) // 后置依赖写法
            .build();

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .next(wrapper3) // 后置依赖写法
            .build();

    TaskWrapper.showTaskDAG(wrapper1, wrapper2);

    StandaloneAsync.start(100000, wrapper1, wrapper2);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();
  }

  /**
   * 测试任务链总超时时间
   * 1,2 成功， 3,4超时失败
   *
   * 1 -> 3 |
   *        | -> 4
   * 2 ->   |
   */
  private static void testScene3() {
    Task1 task1 = new Task1();
    Task2 task2 = new Task2();
    Task3 task3 = new Task3();
    Task4 task4 = new Task4();

    TaskWrapper<String, String> wrapper4 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t4")
            .param("p3")
            .callback(task4)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .next(wrapper4)  // 后置依赖写法
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .next(wrapper4) // 后置依赖写法
            .build();

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .next(wrapper3) // 后置依赖写法
            .build();

    TaskWrapper.showTaskDAG(wrapper1, wrapper2);

    StandaloneAsync.start(1000, wrapper1, wrapper2);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();
    /**
     * 15:25:17.363 [pool-1-thread-2] INFO example.para.Task2 - callback task2 success, now is
     * 1649316317363, result is [TaskResult{result=task2 Result: [now: 1649316317363; param = p2],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-2
     *
     * 15:25:17.363 [pool-1-thread-1] INFO example.para.Task1 - callback task1 success, now is 1649316317363,
     * result is [TaskResult{result=task1 Result: [now: 1649316317363; param = p1],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-1
     *
     * 15:25:17.363 [pool-1-thread-2] ERROR example.para.Task4 - callback task4 failed, now is 1649316317363,
     * result is [TaskResult{result=task3 default result, taskExecutionStatus=TIMEOUT, ex=null}],
     * threadName is pool-1-thread-2
     *
     * 15:25:17.363 [pool-1-thread-1] ERROR example.para.Task3 -callback task3 failed, now is 1649316317363,
     * result is [TaskResult{result=task3 default result, taskExecutionStatus=TIMEOUT, ex=null}],
     * threadName is pool-1-thread-1
     */
  }


  /**
   * 应用场景： 1,2,3,4对应的是一个数据的不同来源，任一来源的数据返回了我都可以去进行后面的任务，以缩短总任务时间
   *
   * 1,2,3,4中任意一个结束，5就可以开始
   *
   * 1 -> |
   * 2 -> |
   *      | -> 5
   * 3 -> |
   * 4 -> |
   */
  private static void testScene4() {
    Task1 task1 = new Task1(1000);
    Task2 task2 = new Task2(3000);
    Task3 task3 = new Task3(3000);
    Task4 task4 = new Task4(4000);
    Task5 task5 = new Task5(1000);

    TaskWrapper<String, String> wrapper5 = new TaskWrapper.Builder<String, String>()
            .task(task5)
            .id("t5")
            .param("p5")
            .callback(task5)
            .build();

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .next(wrapper5, false)
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .next(wrapper5, false)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .next(wrapper5, false)
            .build();

    TaskWrapper<String, String> wrapper4 = new TaskWrapper.Builder<String, String>()
            .task(task4)
            .id("t4")
            .param("p4")
            .callback(task4)
            .next(wrapper5, false)
            .build();

    StandaloneAsync.start(100000, wrapper1, wrapper2, wrapper3, wrapper4);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();

    /**
     * 1 先执行，5再执行，其它后面再执行
     *
     * 15:44:23.333 [pool-1-thread-1] INFO example.para.Task1 - callback task1 success, now is
     * 1649317463331, result is [TaskResult{result=task1 Result: [now: 1649317463331; param = p1],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-1
     *
     * 15:44:24.340
     * [pool-1-thread-1] INFO example.para.Task5 - callback task5 success, now is 1649317464340,
     * result is [TaskResult{result=task5 Result: [now: 1649317464340; param = p5],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-1
     *
     * 15:44:25.321
     * [pool-1-thread-2] INFO example.para.Task2 - callback task2 success, now is 1649317465321,
     * result is [TaskResult{result=task2 Result: [now: 1649317465321; param = p2],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-2
     *
     * 15:44:25.321
     * [pool-1-thread-3] INFO example.para.Task3 - callback task3 success, now is 1649317465321,
     * result is [TaskResult{result=task3 Result: [now: 1649317465321; param = p3],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-3
     *
     * 15:44:26.328
     * [pool-1-thread-4] INFO example.para.Task4 - callback task4 success, now is 1649317466328,
     * result is [TaskResult{result=task4 Result: [now: 1649317466328; param = p4],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-4
     */
  }


  /**
   * 这种实际应用中较少，一种可能的场景是：3对应的是一个非核心业务，比如一些度量指标上报，我们希望3任务不影响主业务流程。
   *
   * 1执行完，同时执行2和3,  2必须执行完才能执行4, 3是可选的（即3不影响4的执行时间）
   *
   *    2
   * 1        4
   *    3(o)
   *
   * 最终执行顺序， 1 3 2 4
   */
  public static void testScene5() {
    Task1 task1 = new Task1(1000);
    Task2 task2 = new Task2(2000);
    Task3 task3 = new Task3(1000);
    Task4 task4 = new Task4(1000);

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .pre(wrapper1)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .pre(wrapper1)
            .build();

    TaskWrapper<String, String> wrapper4 = new TaskWrapper.Builder<String, String>()
            .task(task4)
            .id("t4")
            .param("p4")
            .callback(task4)
            .pre(wrapper2)
            .pre(wrapper3, false)
            .build();


    StandaloneAsync.start(100000, wrapper1, wrapper2, wrapper3, wrapper4);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();

    /**
     * 15:53:43.698 [pool-1-thread-1] INFO example.para.Task1 - callback task1 success, now is
     * 1649318023695, result is [TaskResult{result=task1 Result: [now: 1649318023695; param = p1],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-1
     *
     * <p>15:53:44.708 [pool-1-thread-3] INFO example.para.Task3 - callback task3 success, now is
     * 1649318024708, result is [TaskResult{result=task3 Result: [now: 1649318024708; param = p3],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-3
     *
     * <p>15:53:45.719 [pool-1-thread-4] INFO example.para.Task2 - callback task2 success, now is
     * 1649318025719, result is [TaskResult{result=task2 Result: [now: 1649318025719; param = p2],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-4
     *
     * <p>15:53:46.726 [pool-1-thread-4] INFO example.para.Task4 - callback task4 success, now is
     * 1649318026726, result is [TaskResult{result=task4 Result: [now: 1649318026726; param = p4],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-4
     */
  }


  /**
   * 1,4同时执行，如果5已经开始执行，则2和3会被跳过
   *
   * 1 2 3(o)
   *           5
   * 4
   */
  public static void testScene6() {
    Task1 task1 = new Task1(2000);
    Task2 task2 = new Task2(3000);
    Task3 task3 = new Task3(3000);
    Task4 task4 = new Task4(1000);
    Task5 task5 = new Task5(1000);

    TaskWrapper<String, String> wrapper5 = new TaskWrapper.Builder<String, String>()
            .task(task5)
            .id("t5")
            .param("p5")
            .callback(task5)
            .build();

    TaskWrapper<String, String> wrapper4 = new TaskWrapper.Builder<String, String>()
            .task(task4)
            .id("t4")
            .param("p4")
            .callback(task4)
            .next(wrapper5)
            .build();

    TaskWrapper<String, String> wrapper3 = new TaskWrapper.Builder<String, String>()
            .task(task3)
            .id("t3")
            .param("p3")
            .callback(task3)
            .next(wrapper5, false)
            .build();

    TaskWrapper<String, String> wrapper2 = new TaskWrapper.Builder<String, String>()
            .task(task2)
            .id("t2")
            .param("p2")
            .callback(task2)
            .next(wrapper3)
            .build();

    TaskWrapper<String, String> wrapper1 = new TaskWrapper.Builder<String, String>()
            .task(task1)
            .id("t1")
            .param("p1")
            .callback(task1)
            .next(wrapper2)
            .build();


    StandaloneAsync.start(100000, wrapper1, wrapper4);
    logger.info(StandaloneAsync.getDefaultTheadPoolTaskInfo());

    StandaloneAsync.shutDownNow();

    /**
     * 16:23:33.842 [pool-1-thread-2] INFO example.para.Task4 - callback task4 success, now is
     * 1649319813839, result is [TaskResult{result=task4 Result: [now: 1649319813839; param = p4],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-2
     *
     * <p>16:23:34.849 [pool-1-thread-2] INFO example.para.Task5 - callback task5 success, now is
     * 1649319814849, result is [TaskResult{result=task5 Result: [now: 1649319814849; param = p5],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-2
     *
     * <p>16:23:34.849 [pool-1-thread-1] INFO example.para.Task1 - callback task1 success, now is
     * 1649319814849, result is [TaskResult{result=task1 Result: [now: 1649319814849; param = p1],
     * taskExecutionStatus=SUCCESS, ex=null}], threadName is pool-1-thread-1
     *
     * <p>16:23:34.850 [pool-1-thread-1] ERROR example.para.Task2 - callback task2 failed, now is
     * 1649319814850, result is [TaskResult{result=task2 default result, taskExecutionStatus=FAILED,
     * ex=com.jaydyi.concurrenttool.exception.SkippedException}], threadName is
     * pool-1-thread-1
     *
     * <p>16:23:34.850 [pool-1-thread-1] ERROR example.para.Task3 - callback task3 failed, now is
     * 1649319814850, result is [TaskResult{result=task3 default result, taskExecutionStatus=FAILED,
     * ex=com.jaydyi.concurrenttool.exception.SkippedException}], threadName is
     * pool-1-thread-1
     *
     * 16:23:34.850 [main] INFO example.para.Test - activeCount=0, completedCount 2,
     * largestCount 2
     */
  }

  /**
   * 综合的例子
   *
   *           3 -> 4
   *      1                 7
   *           5 -> 6
   *
   * start                             0  -> end
   *
   *      2       ->        8
   */
}
