package com.neovest.fix.datapump.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.TimerTask;

public class TestRequestEntity {
    // operational parameters
    public String requestId;
    public long iterations;
    public long sleepms;
    public long delaybeforestartms;
    public String fixsessionid;
    // requestid parameters
    public String server;
    public String symbol;
    public String volume;
    public String provider;
    // quote parameters
    public Double sellprice;
    public Double buyprice;


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Double getSellprice() {
        return sellprice;
    }

    public void setSellprice(Double sellprice) {
        this.sellprice = sellprice;
    }

    public Double getBuyprice() {
        return buyprice;
    }

    public void setBuyprice(Double buyprice) {
        this.buyprice = buyprice;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getIterations() {
        return iterations;
    }

    public void setIterations(long iterations) {
        this.iterations = iterations;
    }

    public long getSleepms() {
        return sleepms;
    }

    public void setSleepms(long sleepms) {
        this.sleepms = sleepms;
    }
    public long getDelaybeforestartms() {
        return delaybeforestartms;
    }

    public void setDelaybeforestartms(long delaybeforestartms) {
        this.delaybeforestartms = delaybeforestartms;
    }

    public String getFixsessionid() {
        return fixsessionid;
    }

    public void setFixsessionid(String fixsessionid) {
        this.fixsessionid = fixsessionid;
    }

    public ArrayList<String> getProviders(){
        ArrayList local = new ArrayList();
            local.add(provider);
        return local;
    }
    public ArrayList<Integer> getAmounts(){
        ArrayList local = new ArrayList();
        local.add(volume);
        return local;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

}
