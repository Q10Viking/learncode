package org.hzz.interfacee.array;

public class TestMain {
    public static void main(String[] args) {
        // 声明大小
        Executor[] executors = new Executor[3];
        // 实例化
        for(int i=0;i< executors.length;i++){
            executors[i] = new ExecutorImpl();
        }
    }
}
