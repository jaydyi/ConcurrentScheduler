package linear;

import com.jaydyi.concurrenttool.standalone.task.TaskWrapper;

public class Test {
  public static void main(String[] args) {
    LinearTask1 task1 = new LinearTask1();
    LinearTask2 task2 = new LinearTask2();
    LinearTask3 task3 = new LinearTask3();
    LinearTask4 task4 = new LinearTask4();

    TaskWrapper<String, String> taskWrapper4 = new TaskWrapper.Builder<String, String>()
            .name("t4")
            .task(task4)
            .param("p4")
            .callback(task4)
            .build();

    TaskWrapper<String, String> taskWrapper3 = new TaskWrapper.Builder<String, String>()
            .name("t3")
            .task(task3)
            .param("p3")
            .callback(task3)
            .next(taskWrapper4)
            .build();

    TaskWrapper<String, String> taskWrapper2 = new TaskWrapper.Builder<String, String>()
            .name("t2")
            .task(task2)
            .param("p2")
            .callback(task2)
            .next(taskWrapper4)
            .build();

    TaskWrapper<String, String> taskWrapper1 = new TaskWrapper.Builder<String, String>()
            .name("t1")
            .task(task1)
            .param("p1")
            .callback(task1)
            .next(taskWrapper2, taskWrapper3)
            .build();

    TaskWrapper.showTaskDAG(taskWrapper1);
    //
  }
}
