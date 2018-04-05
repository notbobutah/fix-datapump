package com.neovest.fix.datapump.model;

import com.neovest.fix.datapump.quickfix.Application;
import com.neovest.fix.datapump.resource.FixDataPumpWebController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Group;
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.component.Parties;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class TestManager {
    private static final Logger log = LoggerFactory.getLogger(FixDataPumpWebController.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static TestManager instance = null;
    HashMap<String, TestRequestEntity> testRequests = new HashMap<String, TestRequestEntity>();
    Application app=null;

    protected TestManager() {}

    public static TestManager getInstance(){
        if(instance == null)
            instance = new TestManager();
        return instance;
    }


    public HashMap<String, TestRequestEntity> getTestRequests() {
        return testRequests;
    }
    public void setTestRequests(HashMap<String, TestRequestEntity> testrequests) {
        this.testRequests = testrequests;
    }

    public TestRequestEntity getTestRequest(String testrequestid){
        TestRequestEntity tr = testRequests.get(testrequestid);
        return tr;
    }

    public MarketDataRequest getMDRForTest(String testrequestid) throws Exception {
        TestRequestEntity tr = testRequests.get(testrequestid);
        return subscribeToMarketDataInFullAmountMode(new Symbol(tr.symbol),tr.requestId,1, tr.getProviders(), tr.getAmounts());
    }

    private MarketDataRequest subscribeToMarketDataInFullAmountMode(Symbol pair, String uniqueId, int depth, ArrayList<String>  partyList, ArrayList<Integer> amountList) throws Exception {
        final MarketDataRequest mdr = new MarketDataRequest();
        if(partyList.size() == 1) {
            String provSingle = partyList.get(0);
            mdr.set(new MDReqID(pair.getValue() + "_" + uniqueId + "~" + provSingle));
        } else {
            mdr.set(new MDReqID(pair.getValue() + "_" + uniqueId));
        }
//        mdr.setBoolean(266,true);
        mdr.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
        mdr.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
//        mdr.set(new MDUpdateType(MDUpdateType.INCREMENTAL_REFRESH));
        // FSS custom fids for FullAmount

        final MarketDataRequest.NoRelatedSym symbol = new MarketDataRequest.NoRelatedSym();
        symbol.set(pair);
//        mdr.addGroup(symbol);
        mdr.setField(55,pair);
        mdr.set(new MarketDepth(depth));

        if(partyList != null) {
            addPartyGroup(partyList, mdr);
        }
        // quantities
        int[] quantities = new int[amountList.size()];
        Iterator intlist = amountList.iterator();
        int i = 0;
        mdr.setInt(9000, quantities.length);
        while(intlist.hasNext()){
            final Group group = new Group(9000, 9001);
            group.setInt(9001, (int)intlist.next());
            // RequestedSize (tag 9001)
            mdr.addGroup(group);
        }
       log.info("************  rtn MarketDataRequest str : " + mdr.toString());
        return mdr;
    }
    public Message addPartyGroup(ArrayList<String> partyList, Message message) throws Exception {

        int cnt = 0;
        PartyIDSource prtysrc = new PartyIDSource('D');
        PartyRole prtyrole = new PartyRole(35);
        Parties parties = new Parties();
        Iterator iter = partyList.iterator();
        int fldord[] = new int[]{453,447,448,452};
        while(iter.hasNext()){

            String lp = (String)iter.next();
            Group wrkgroup = new Group(453,1,fldord);
            PartyID prtyid = new PartyID(lp);
            wrkgroup.setField(prtyid);
            wrkgroup.setField(prtysrc);
            wrkgroup.setField(prtyrole);
            message.addGroup(wrkgroup);
            cnt++;
        }
        message.setField(new NoPartyIDs(cnt));

        log.info("************** inside addpartygroup hasgroup  = :" + message.hasGroup(453));
        return message;
    }

    public boolean isTestRequest(String ReqId){
        TestRequestEntity tre = testRequests.get(ReqId);
        if(tre == null) return false;
        return true;
    }
    public Application getApp() {
        return app;
    }

    public void setApp(Application app) {
        this.app = app;
    }

}
