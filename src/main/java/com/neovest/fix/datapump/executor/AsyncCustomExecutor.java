package com.neovest.fix.datapump.executor;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Set;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@ComponentScan(basePackages = "com.neovest.fix.datapump.quickfix")
public class AsyncCustomExecutor extends AsyncConfigurerSupport {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(25);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(10000);
//        executor.setThreadNamePrefix("FXAppiaServer");
        executor.initialize();
        return executor;
    }

    public boolean interrupt(String threadname) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            System.out.println("looking for threadname:"+ threadname +" thread name:"+thread.getName());
            if (thread.getName().contains(threadname)) {
                thread.interrupt();
                return true;
            }
        }
        return false;
    }
}