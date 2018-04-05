package com.neovest.fix.datapump.config;
/********************
 * This is the main bean configuration file for the FX Appia server implmentation
 * the interdependcies of beans are defined here allowing for proper singleton
 * emitter manager maps to be properly injected into the other beans.
 *
 * The singleton emittermap is shared across all threads in the app.
 * The emitterMap being a ConcurrentHashMap is thread safe.
 */

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
public class AppConfig {

    long threadcount = 100;
    long iterations = 1000;
    Double randomseed = 50d;
    long threadsleepms = 1500;
    long symbolCount = 209;

    public long getSymbolCount() {
        return symbolCount;
    }

    public void setSymbolCount(long symbolCount) {
        System.setProperty("DATAPUMP_SYMBOLCOUNT",String.valueOf(symbolCount));
        this.symbolCount = symbolCount;
    }


    public long getThreadcount() {
        return threadcount;
    }

    public void setThreadcount(long threadcount) {
        System.setProperty("DATAPUMP_THREADCOUNT",String.valueOf(threadcount));
        this.threadcount = threadcount;
    }

    public long getIterations() {
        return iterations;
    }

    public void setIterations(long iterations) {
        System.setProperty("DATAPUMP_ITERATIONS",String.valueOf(iterations));
        this.iterations = iterations;
    }

    public Double getRandomseed() {
        return randomseed;
    }

    public void setRandomseed(Double randomseed) {
        System.setProperty("DATAPUMP_RANDOMSEED",String.valueOf(randomseed));
        this.randomseed = randomseed;
    }

    public long getThreadsleepms() {
        return threadsleepms;
    }

    public void setThreadsleepms(long threadsleepms) {
        System.setProperty("DATAPUMP_SLEEPTIME",String.valueOf(threadsleepms));
        this.threadsleepms = threadsleepms;
    }

}
