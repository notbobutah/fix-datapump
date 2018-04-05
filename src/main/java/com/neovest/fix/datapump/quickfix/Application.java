package com.neovest.fix.datapump.quickfix;

import com.neovest.fix.datapump.config.AppConfig;
import com.neovest.fix.datapump.executor.AsyncCustomExecutor;
import com.neovest.fix.datapump.model.SymbolList;
import com.neovest.fix.datapump.model.TestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import quickfix.*;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.MessageFactory;
import quickfix.field.*;
import quickfix.field.Currency;
import quickfix.fix44.*;
import quickfix.fix50sp2.component.TrdSessLstGrp;
import quickfix.fix50sp2.TradingSessionList;
import quickfix.fix50sp2.TradingSessionListRequest;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadFactory;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;


@EnableAsync
public class Application extends MessageCracker implements quickfix.Application {
    private static final String DEFAULT_MARKET_PRICE_KEY = "DefaultMarketPrice";
    private static final String ALWAYS_FILL_LIMIT_KEY = "AlwaysFillLimitOrders";
    private static final String VALID_ORDER_TYPES_KEY = "ValidOrderTypes";
    private AsyncCustomExecutor aThreadExec = new AsyncCustomExecutor();

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final boolean alwaysFillLimitOrders;
    private final HashSet<String> validOrderTypes = new HashSet<>();
    private MarketDataProvider marketDataProvider;
    private MessageFactory messageFactory;
    private DataDictionary dd;
    private Session session;
    private AppConfig ap = new AppConfig();
    SymbolList symbollist = SymbolList.getInstance();
    TestManager testManager = TestManager.getInstance();


    public Application(SessionSettings settings) throws ConfigError, FieldConvertError {
        initializeValidOrderTypes(settings);
        initializeMarketDataProvider(settings);
        messageFactory = new DefaultMessageFactory();
        dd = new DataDictionary("FSS_FIX44.xml");

        testManager.setApp(this);
        alwaysFillLimitOrders = settings.isSetting(ALWAYS_FILL_LIMIT_KEY) && settings.getBool(ALWAYS_FILL_LIMIT_KEY);
    }

    private void initializeMarketDataProvider(SessionSettings settings) throws ConfigError, FieldConvertError {
        if (settings.isSetting(DEFAULT_MARKET_PRICE_KEY)) {
            if (marketDataProvider == null) {
                final double defaultMarketPrice = settings.getDouble(DEFAULT_MARKET_PRICE_KEY);
                marketDataProvider = new MarketDataProvider() {
                    public double getAsk(String symbol) {
                        return defaultMarketPrice;
                    }

                    public double getBid(String symbol) {
                        return defaultMarketPrice;
                    }
                };
            } else {
                log.warn("Ignoring " + DEFAULT_MARKET_PRICE_KEY + " since provider is already defined.");
            }
        }
    }

    private void initializeValidOrderTypes(SessionSettings settings) throws ConfigError, FieldConvertError {
        if (settings.isSetting(VALID_ORDER_TYPES_KEY)) {
            List<String> orderTypes = Arrays
                    .asList(settings.getString(VALID_ORDER_TYPES_KEY).trim().split("\\s*,\\s*"));
            validOrderTypes.addAll(orderTypes);
        } else {
            validOrderTypes.add(OrdType.LIMIT + "");
        }
    }

    public void onCreate(SessionID sessionID) {
        Session.lookupSession(sessionID).getLog().onEvent("Valid order types: " + validOrderTypes);
    }

    public void onLogon(SessionID sessionID) {
        log.debug("received logon request message type:" + sessionID);
    }

    public void onLogout(SessionID sessionID) {
        log.debug("received logout request message type:" + sessionID);
    }

    public void toAdmin(Message message, SessionID sessionID) {
        Session session = Session.lookupSession(sessionID);
        DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();

        log.debug("received admin message type:" + message.toString());
    }

    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, RejectLogon {
        Session session = Session.lookupSession(sessionID);
        DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
        log.info("received fromAdmin message type:" + message.toString());
    }


    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        log.error("received toApp message type:" + message.toString());
        Session session = Session.lookupSession(sessionID);
        DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
    }

    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType {
        try {
            log.error("received fromApp message type:" + message.toString());
            Session session = Session.lookupSession(sessionID);
            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            MsgType mtype = new MsgType();
            message.getHeader().getField(mtype);
            if (mtype.getValue().equals("V")) {
                onMessage((MarketDataRequest) message, sessionID);
            } else if (mtype.getValue().equals("BI")) {
                log.error("Caught TradingSessionListRequest MessageType in fromApp" + message.getHeader().getField(mtype));
                onTSLRMessage(message, sessionID);
            } else
                crack(message, sessionID);
        } catch (UnsupportedMessageType umte) {
            log.error("Caught UnsupportedMessageType in fromApp" + umte);
        } catch (Exception e) {
            log.error("Caught UnsupportedMessageType in fromApp" + e);
        }
    }


    private boolean isOrderExecutable(Message order, Price price) throws FieldNotFound {
        if (order.getChar(OrdType.FIELD) == OrdType.LIMIT) {
            BigDecimal limitPrice = new BigDecimal(order.getString(Price.FIELD));
            char side = order.getChar(Side.FIELD);
            BigDecimal thePrice = new BigDecimal("" + price.getValue());

            return (side == Side.BUY && thePrice.compareTo(limitPrice) <= 0)
                    || ((side == Side.SELL || side == Side.SELL_SHORT) && thePrice.compareTo(limitPrice) >= 0);
        }
        return true;
    }

    private Price getPrice(Message message) throws FieldNotFound {
        Price price;
        if (message.getChar(OrdType.FIELD) == OrdType.LIMIT && alwaysFillLimitOrders) {
            price = new Price(message.getDouble(Price.FIELD));
        } else {
            if (marketDataProvider == null) {
                throw new RuntimeException("No market data provider specified for market order");
            }
            char side = message.getChar(Side.FIELD);
            if (side == Side.BUY) {
                price = new Price(marketDataProvider.getAsk(message.getString(Symbol.FIELD)));
            } else if (side == Side.SELL || side == Side.SELL_SHORT) {
                price = new Price(marketDataProvider.getBid(message.getString(Symbol.FIELD)));
            } else {
                throw new RuntimeException("Invalid order side: " + side);
            }
        }
        return price;
    }

    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) {
                throw new SessionNotFound(sessionID.toString());
            }

            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            if (dataDictionaryProvider != null) {
                try {
                    dataDictionaryProvider.getApplicationDataDictionary(
                            getApplVerID(session, message)).validate(message, true);
                } catch (Exception e) {
                    LogUtil.logThrowable(sessionID, "Outgoing message failed validation: "
                            + e.getMessage(), e);
                    return;
                }
            }

            session.send(message);
        } catch (SessionNotFound e) {
            log.error(e.getMessage(), e);
        }
    }

    private ApplVerID getApplVerID(Session session, Message message) {
        String beginString = session.getSessionID().getBeginString();
        if (FixVersions.BEGINSTRING_FIXT11.equals(beginString)) {
            return new ApplVerID(ApplVerID.FIX50);
        } else {
            return MessageUtils.toApplVerID(beginString);
        }
    }


    /**
     * Allows a custom market data provider to be specified.
     *
     * @param marketDataProvider
     */
    public void setMarketDataProvider(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    ///////////////// message cracker functions ///////////////////
    @Handler
    public void onMessage(Message message, Session sessionI) throws Exception {
        log.info("inside generic message handler - message =: " + message.toString());
        MsgType msgType = new MsgType();
        message.getHeader().getField(msgType);
        log.info("inside generic message handler - msgtype =: " + msgType.toString());
//        log.info("inside generic message handler - message =: "+ message.toString());
    }

    @Handler
    public void onMessage(QuoteRequest message, SessionID sessionID) throws Exception {
        log.error("Inside QuoteRequest :"+message);
        QuoteResponse quoteResponse = new QuoteResponse();
        String targetCompId = message.getHeader().getString(56);
        String senderCompId = message.getHeader().getString(49);
        quoteResponse.getHeader().setString(49, targetCompId);
        quoteResponse.getHeader().setString(56, senderCompId);
        long sleeptime = ap.getThreadsleepms();
        RFQReqID rfqReqID = new RFQReqID();
        message.get(rfqReqID);
        quoteResponse.setString(644,rfqReqID.getValue());
        // run background lambda thread to send out responses
        // BidPx <132>, OfferPx <133> or both must be specified.

        Runnable quotetask = () -> {
            int i = 100;

            try {
                while (i > 0) {
                    sleep(sleeptime);
                    quoteResponse.removeGroup(268);
//                    addQuoteGroup(partyId, 1, quoteResponse, message, mdEntrySize);
//                    addQuoteGroup(partyId, 0, quoteResponse, message, mdEntrySize);
                    log.info("fixmsg in loop ="+quoteResponse.toString());
                    Session.sendToTarget(quoteResponse);
                    i--;
                }
            }catch(InterruptedException e){
                log.error("Exiting thread "+currentThread().getName());
                return;
            } catch (Exception e){
                log.error("Thread operation exception: "+e);
            }
        };
//        ThreadFactory namedThreadFactory =
//                new ThreadFactoryBuilder().setNameFormat(mdReqId.toString()).build();
        Thread temp = new Thread(quotetask);
        temp.setName(rfqReqID.getValue());
        temp.start();
    }

//    @Handler
//    public void onMessage(QuoteResponse message, SessionID sessionID) throws Exception {
//        log.error("Inside QuoteResponse :"+message);
//    }

    //  8=FIX.4.4|9=387|35=W|34=606494538|
    // 49=FSS|52=20180109-16:46:00.364|56=STR.NY.SIM.NEOV.5270|
    // 55=AUD/EUR|64=SP|
    // 262=rob-ubuntu_8009_AUDEUR_1000000,2000000,5000000,10000000_GS_2114095319|
    // 268=2|269=0|278=1baB-1#tkh+NX+/1M|270=0.67152|271=1000000|272=20180109|273=16:46:00.200|15=AUD|282=GS|64=20180112|
    //       269=1|278=1baO-1#tkh+NX+/1M|270=0.67164|271=1000000|272=20180109|273=16:46:00.200|15=AUD|282=GS|64=20180112|
    // 10=179|
    // creating this repeating field group
    // 268=2|269=0|278=1baB-1#tkh+NX+/1M|270=0.67152|271=1000000|272=20180109|273=16:46:00.200|15=AUD|282=GS|64=20180112|
    //       269=1|278=1baO-1#tkh+NX+/1M|270=0.67164|271=1000000|272=20180109|273=16:46:00.200|15=AUD|282=GS|64=20180112|

    @Handler
    public void onMessage(MarketDataRequest message, SessionID sessionID) throws Exception {
        MDReqID mdReqId = message.getMDReqID();
        final MarketDataRequest msg = message;
        // unsubscribe message
        if(msg.getSubscriptionRequestType().getValue() ==
                SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST) {
            aThreadExec.interrupt(mdReqId.getValue());
            return;
        }


        MarketDataSnapshotFullRefresh mdrsfr = new MarketDataSnapshotFullRefresh();
        MarketDataSnapshotFullRefresh working = new MarketDataSnapshotFullRefresh();
        ArrayList<String> partyList = new ArrayList<String>();
        NoPartyIDs noPartyIDs = new NoPartyIDs();

        log.error("Inside MarketDataRequest handler:" + msg.toString());

        Group partyGroup = new Group(NoPartyIDs.FIELD, PartyID.FIELD);
        msg.getGroup(1, partyGroup);
        PartyID partyId = new PartyID();
        partyGroup.getField(partyId);
        String targetCompId = msg.getHeader().getString(56);
        String senderCompId = msg.getHeader().getString(49);

        log.error("process response for msg :" + msg.toString());

        MDEntrySize mdEntrySize = new MDEntrySize();
        try{
            String locfixstr = msg.toString();
            int index9001 = locfixstr.indexOf("9001");
            locfixstr = locfixstr.substring(index9001);
            locfixstr = locfixstr.substring(locfixstr.indexOf("=")+1,locfixstr.indexOf(0x0001));
            log.error("volume = "+locfixstr +" parsed "+Long.parseLong(locfixstr));
            mdEntrySize.setValue(Long.parseLong(locfixstr));
        } catch(Exception e){
            mdEntrySize.setValue(1000000);
            log.error("Exception getting group (hardcoding volume)"+e);
        }
        Symbol symbol = new Symbol();
        msg.getField(symbol);
        long sleeptime = ap.getThreadsleepms();
//        working.getHeader().setString(8,"FIX.4.4");
        working.getHeader().setString(49,targetCompId);
        working.getHeader().setString(56,senderCompId);
        working.setField(symbol);
        working.set(mdReqId);

        // run background lambda thread to send out responses
        Runnable quotetask = () -> {
            long i = 1000;
            long lasttime = System.nanoTime();
            long locsleeptime = Long.parseLong(System.getProperty("DATAPUMP_SLEEPTIME",String.valueOf(sleeptime) ));
//            long lociterations = Long.parseLong(System.getProperty("DATAPUMP_ITERATIONS"));

            try {
                log.info("starting timestamp:"+lasttime);
                while (i > 0) {
                    locsleeptime = Long.parseLong(System.getProperty("DATAPUMP_SLEEPTIME",String.valueOf(sleeptime)));
                    lasttime = System.nanoTime();
                    log.info("timestamp before sleeping:"+lasttime);
                    sleep(locsleeptime);
                    log.info("sleeptime :"+locsleeptime);
                    log.info("timestamp "+System.nanoTime()+" diff after sleeping:"+(System.nanoTime()-lasttime));
                    working.removeGroup(268);
                    addQuoteGroup(partyId, 1, working, msg, mdEntrySize);
                    addQuoteGroup(partyId, 0, working, msg, mdEntrySize);
                    log.info("fixmsg in loop ="+working.toString());
                    Session.sendToTarget(working);
                    i--;
                }
            }catch(InterruptedException e){
                log.info("Exiting timestamp:"+System.nanoTime());
                log.error("Exiting thread "+currentThread().getName());
                return;
            } catch (Exception e){
                log.error("Thread operation exception: "+e);
            }
        };
//        ThreadFactory namedThreadFactory =
//                new ThreadFactoryBuilder().setNameFormat(mdReqId.toString()).build();
        Thread temp = new Thread(quotetask);
        temp.setName(mdReqId.getValue());
        temp.start();
//        aThreadExec.getAsyncExecutor().execute(temp);
    }


    //@Handler
    // custom directly called handler to output response message
    //8=FIX.4.4|9=0302|35=BJ|49=FSS|56=STR.NY.SIM.NEOV.569806|52=20180124-18:53:42.040|34=3752|
    // 335=rob-ubuntu_1daf90d9-eef6-4637-8963-8c154bc40ae9|
    // 386=12|336=BAML|340=2|336=CITI|340=2|336=BNP|340=2|336=BTMU|340=2|336=MS|340=2|336=UBS|340=2|336=GS|340=2|336=HSBC|340=2|336=CS|340=2|336=SCB|340=2|336=COBA|340=2|336=JPMC|340=2|10=191||]

    public void onTSLRMessage(Message message, SessionID sessionID) throws Exception {
        SubscriptionRequestType subtype = new SubscriptionRequestType();
        TradingSessionList tsl = new TradingSessionList();
        TradSesReqID trReqID = new TradSesReqID();
        trReqID.setValue(message.getString(335));
        tsl.setField(trReqID);

        message.getField(subtype);
        if(subtype.getValue() == SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST) return;
        log.info("Request Message for providers list:" + message);
        tsl.getHeader().setString(49, sessionID.getSenderCompID());
        tsl.getHeader().setString(56, sessionID.getTargetCompID());
        tsl.set(new TradSesReqID(message.getString(335)));
        TrdSessLstGrp tslg = new TrdSessLstGrp();
        TrdSessLstGrp.NoTradingSessions tgrp = new TrdSessLstGrp.NoTradingSessions();
//        tgrp.set(new NoTradingSessions(12)); // 386=12

        tgrp.setString(336,"BAML");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"CITI");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"BNP");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"BTMU");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"MS");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"UBS");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"GS");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"HSBC");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"CS");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"SCB");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"COBA");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tgrp.setString(336,"JPMC");
        tgrp.setString(340,"2");
        tslg.addGroup(tgrp);
        tsl.set(tslg);
        tsl.getHeader().setField(new BeginString("FIX.4.4"));
        Session.sendToTarget(tsl);

    }

//    @Handler
//    public void onMessage(TradingSessionListRequest message, SessionID sessionID) throws Exception {
//        log.info("Request Message for providers list:" + message);
//    }
    @Handler
    public void onMessage(TradingSessionList message, SessionID sessionID) throws Exception {
        log.info("Response Message for providers list:" + message);
    }

    //  8=FIX.4.4|9=124|35=x|34=2|49=STR.NY.SIM.NEOV.5270|52=20180110-14:34:03.870|56=FSS|320=rob-ubuntu_21b701b8-1a65-4c71-8ad2-4988675fb82c|559=4|10=190|
    @Handler
    public void onMessage(SecurityListRequest message, SessionID sessionID) throws Exception {
        log.info("Request Message for symbol list:"+message);
        // loop through first dimension of array of symbols
        // send as fix message
//        SecurityResponseID secrespid = new SecurityResponseID(secid.toString());
        SecurityReqID secid = new SecurityReqID();
        message.get(secid);
        int r1= symbollist.getSymbolarraySize();
        for(ArrayList<String> symbolrow: symbollist.getSymbolarray()){
            SecurityList secResp = new SecurityList();
            secResp.setField(secid);
            secResp.setInt(560,0);
            secResp.getHeader().setString(49,"FSS");
            secResp.getHeader().setString(56,"STR.UT.SIM.NEOV.9999");
            log.debug("Request Message for symbol list:"+r1);
            addSecurityGroup(secResp, message, symbolrow);
            if(r1 <= 0) {
                log.debug(" ******* last fragement "+r1);
                secResp.set(new LastFragment(true));
                r1--;
            } else {
                secResp.set(new LastFragment(false));
                r1--;
            }

            Session.sendToTarget(secResp);
        }

    }

    //    The SecurityList response:8=FIX.4.49=104035=y34=316849=FSS52=20180110-16:13:15.57156=STR.UT.SIM.NEOV.9999320=rob-ubuntu_42278854-f0f7-4566-a3c3-e983134535a7560=0893=N146=5055=CAD/NOK167=FX55=CAD/NZD167=FX55=CAD/PHP167=FX55=CAD/PLN167=FX55=CAD/SEK167=FX55=CAD/SGD167=FX55=CAD/THB167=FX55=CAD/TWD167=FX55=CAD/USD167=FX55=CAD/ZAR167=FX55=CAD/ZMW167=FX55=CHF/AUD167=FX55=CHF/CAD167=FX55=CHF/CNH167=FX55=CHF/CNY167=FX55=CHF/CZK167=FX55=CHF/DKK167=FX55=CHF/EUR167=FX55=CHF/GBP167=FX55=CHF/GHS167=FX55=CHF/HKD167=FX55=CHF/HUF167=FX55=CHF/IDR167=FX55=CHF/INR167=FX55=CHF/JPY167=FX55=CHF/KES167=FX55=CHF/KRW167=FX55=CHF/MXN167=FX55=CHF/MYR167=FX55=CHF/NOK167=FX55=CHF/NZD167=FX55=CHF/PHP167=FX55=CHF/PLN167=FX55=CHF/RON167=FX55=CHF/SEK167=FX55=CHF/SGD167=FX55=CHF/THB167=FX55=CHF/TRY167=FX55=CHF/TWD167=FX55=CHF/USD167=FX55=CHF/ZAR167=FX55=CHF/ZMW167=FX55=CLP/USD167=FX55=CNH/CNY167=FX55=CNH/HKD167=FX55=CNH/IDR167=FX55=CNH/INR167=FX55=CNH/JPY167=FX55=CNH/KRW167=FX55=CNH/PHP167=FX10=068
    @Handler
    @Async
    public void onMessage(SecurityList message, SessionID sessionID) throws Exception {
        log.debug("Message for symbol list"+message);
    }
    //    request: 8=FIX.4.4|9=125|35=BI|34=2|49=STR.NY.SIM.NEOV.5270|52=20180110-14:34:03.893|56=FSS|263=1|335=rob-ubuntu_b9bc6c1a-f1bd-410c-bdaf-b18a68aed9b2|10=154|
    //    response: 8=FIX.4.4|9=0300|35=BJ|49=FSS|56=STR.NY.SIM.NEOV.5555|52=20180110-14:34:03.956|34=1999|335=rob-ubuntu_91ba9e97-2939-4b0d-b91c-67081b9f4a61|386=12|336=BAML|340=2|336=CITI|340=2|336=BNP|340=2|336=BTMU|340=2|336=MS|340=2|336=UBS|340=2|336=GS|340=2|336=HSBC|340=2|336=CS|340=2|336=SCB|340=2|336=COBA|340=2|336=JPMC|340=2|10=248|
    @Handler
    public void onMessage(quickfix.fix50sp2.TradingSessionListRequest message, SessionID sessionID) throws Exception {
        quickfix.fix50sp2.TradingSessionList tsl = new quickfix.fix50sp2.TradingSessionList();
        tsl.getHeader().setString(49,"FSS");
        tsl.getHeader().setString(56,"STR.UT.SIM.NEOV.9999");
        tsl.set(message.getTradSesReqID());
        addProviderGroup(tsl,message,new String[]{"BAML","JPMC","GS","CS"});
        Session.sendToTarget(tsl);
        log.debug("Message for trading list"+message);
    }
    /////////////////////  support functions for message cracker functions ///////////
    // creates MessageSpecific class from FIX message
    // Useful for routing to Quickfix onMessage Handlers.
    public <T extends Message> T getMessageClass(String fixmsg) throws Exception {

        Message message = MessageUtils.parse(messageFactory, dd, fixmsg);
        String msgType = message.getHeader().getField(new StringField(35)).getValue();
        if(msgType.equals("BJ") || msgType.equals("BS")) {
            log.debug("Handling a TradingSessionList Response -- " + fixmsg);
            message.getHeader().setField(new BeginString("FIXT.1.19"));
        }
        Message factorymessage = messageFactory.create(message.getHeader().getString(8), message.getHeader().getString(35));
        factorymessage.fromString(fixmsg,dd, false);
        return (T) factorymessage;
    }

    //386=12|336=BAML|340=2|336=CITI|340=2|336=BNP|340=2|336=BTMU|340=2|336=MS|340=2|336=UBS|340=2|336=GS|340=2|336=HSBC|340=2|336=CS|340=2|336=SCB|340=2|336=COBA|340=2|336=JPMC|340=2|10=248|
    public Message  addProviderGroup(Message respMsg, Message message, String[] providersrow) throws Exception {

        int cnt = 0;
        log.debug("Request symbol list length: "+providersrow.length);
        TradSesStatus st = new TradSesStatus(2);
        Group wrkgroup = new Group(386,1);
        // loop through second dimension of symbols array
        // add as symbols to group
        for(int c1 =0; c1 < providersrow.length; c1++){
            wrkgroup.setField(new TradingSessionID(providersrow[c1]));
            wrkgroup.setField(st);
            respMsg.addGroup(wrkgroup);
        }

        log.debug("************** inside addprovidergroup hasgroup  = :" + message.hasGroup(386));
        return respMsg;
    }
    public Message  addSecurityGroup(Message respMsg, Message message, ArrayList<String> symbolsrow) throws Exception {

        int cnt = 0;
        log.debug("Request symbol list length: "+symbolsrow.size());
        SecurityType st = new SecurityType("FX");
        Group wrkgroup = new Group(146,1);
        // loop through second dimension of symbols array
        // add as symbols to group
        for(String symbol: symbolsrow){
            wrkgroup.setField(new Symbol(symbol));
            wrkgroup.setField(st);
            respMsg.addGroup(wrkgroup);
        }

        log.debug("************** inside addquotegroup hasgroup  = :" + message.hasGroup(268));
        return respMsg;
    }
    // creating this repeating field group
    // json looks like {"settledate":"20180115","price":"0.67176","volume":"1000000","altvolume":671760,"date":"20180110","time":"19:34:55.111","mdorderid":"1aaUBxnQD.2M+/1M","provider":"BAML"}
    // 268=2|269=0|278=1baB-1#tkh+NX+/1M|270=0.67152|271=1000000|272=20180109|273=16:46:00.200|15=AUD|282=GS|64=20180112|
    //       269=1|278=1baO-1#tkh+NX+/1M|270=0.67164|271=1000000|272=20180109|273=16:46:00.200|15=AUD|282=GS|64=20180112|
//    Random ran = new Random(Long.parseLong(System.getProperty("DATAPUMP_RANDOMSEED", String.valueOf(50))));
    Random ran = new Random(50);

    public Message  addQuoteGroup(PartyID partyId,  int BidOffer, Message respMsg, Message message, MDEntrySize mdEntrySize) throws Exception {

        int fldord[] = new int[]{269,278,270,271,272,273,15,282,64};
        int cnt = 0;
        DecimalFormat df = new DecimalFormat("##.#####");
        Symbol symbol = new Symbol();
        message.getField(symbol);
        Group wrkgroup = new Group(268,1,fldord);

        MDEntryType mdentry = new MDEntryType(); //269
        if(BidOffer == 0)
            mdentry.setValue('0');
        else
            mdentry.setValue('1');
        wrkgroup.setField(mdentry);

        MDEntryID mdEntryId = new MDEntryID(); // 278
        mdEntryId.setValue("fssorderid");
        wrkgroup.setField(mdEntryId);

        MDEntryPx mdEntryPx = new MDEntryPx(); // 270

        Double resultprice = Double.parseDouble(df.format(1.0+(ran.nextDouble()/4)));
        mdEntryPx.setValue(resultprice);
        wrkgroup.setField(mdEntryPx);


//        param instead of local MDEntrySize mdEntrySize = new MDEntrySize(); // 271
        wrkgroup.setField(mdEntrySize);

        MDEntryDate mdEntryDate = new MDEntryDate(); // 272
        mdEntryDate.setValue(LocalDate.now());
        wrkgroup.setField(mdEntryDate);

        MDEntryTime mdEntryTime = new MDEntryTime(); // 273
        mdEntryTime.setValue(LocalTime.now());
        wrkgroup.setField(mdEntryTime);

        Currency currency = new Currency(symbol.getValue().substring(0,3)); // 15
        wrkgroup.setField(currency);

        MDEntryOriginator mdEntryOriginator = new MDEntryOriginator(partyId.getValue()); // 282
        wrkgroup.setField(mdEntryOriginator);

        SettlDate settlDate =new SettlDate(); // 64
        settlDate.setValue(LocalDate.now().plusDays(2L).toString());
        wrkgroup.setField(settlDate);

        respMsg.addGroup(wrkgroup);

        log.debug("************** inside addquotegroup hasgroup  = :" + message.hasGroup(268));
        return message;
    }

}
