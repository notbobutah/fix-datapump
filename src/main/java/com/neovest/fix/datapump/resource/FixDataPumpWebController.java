package com.neovest.fix.datapump.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovest.fix.datapump.config.AppConfig;
import com.neovest.fix.datapump.model.SymbolList;
import com.neovest.fix.datapump.model.TestManager;
import com.neovest.fix.datapump.model.TestRequestEntity;
import com.neovest.fix.datapump.quickfix.Application;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.fix44.MarketDataRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/fixdatapump/")
@Api(value="neovest", description="Operations for controlling the data pump")
public class FixDataPumpWebController {

	private static final Logger log = LoggerFactory.getLogger(FixDataPumpWebController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	ObjectMapper mapper = new ObjectMapper();

	SymbolList symbollist = SymbolList.getInstance();
	TestManager testManager = TestManager.getInstance();

	@Autowired
	AppConfig appConfig;


	@RequestMapping(value = "quoteconfig/", produces = "application/json", method = RequestMethod.POST)
	@ApiOperation(value = "Set datapump operating parameters", hidden = false)
	@CrossOrigin
	public AppConfig setDataPumpConfig(@RequestBody AppConfig cfg  )
			throws InterruptedException, Exception {

		appConfig.setThreadcount(cfg.getThreadcount());
		appConfig.setIterations(cfg.getIterations());
		appConfig.setThreadsleepms(cfg.getThreadsleepms());
		appConfig.setSymbolCount(cfg.getSymbolCount());

	return appConfig;
	}

	@RequestMapping(value = "datapumpstats/", produces = "application/json", method = RequestMethod.POST)
	@ApiOperation(value = "Get datapump operating stats", hidden = false)
	@CrossOrigin
	public HashMap getDataPumpStats()
			throws InterruptedException, Exception {
		    Runtime runtime = Runtime.getRuntime();
			HashMap stats = new HashMap();

		stats.put("totalThreads",Thread.getAllStackTraces().keySet().size());
		int nbRunning = 0;
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (t.getState()==Thread.State.RUNNABLE) nbRunning++;
		}
		stats.put("totalThreadsRunning", nbRunning);
		stats.put("totalmemory", runtime.totalMemory());
		stats.put("totalfree", runtime.freeMemory());
		stats.put("systemProperties",System.getProperties());
		return stats;
	}

	@RequestMapping(value = "securitylist/{count}", produces = "application/json", method = RequestMethod.GET)
	@ApiOperation(value = "retrieve all supported fx symbols as json", hidden = false)
	@CrossOrigin
	public ArrayList<String> getSecurityCountList(@PathVariable int count )
			throws InterruptedException, Exception {

		appConfig.setSymbolCount(count);
		log.error("symbollist is endpoint : "+ symbollist.hashCode());

		return symbollist.getFlattenedSymbolArraytoCount(count);
	}
	@RequestMapping(value = "securitylist/", produces = "application/json", method = RequestMethod.GET)
	@ApiOperation(value = "retrieve all supported fx symbols as json", hidden = false)
	@CrossOrigin
	public ArrayList<String> getSecurityList()	throws InterruptedException, Exception {

		log.error("symbollist is endpoint : "+ symbollist.hashCode());
		ArrayList<String> slist = symbollist.getFlattenedSymbolArray();
		return slist;
	}
	@RequestMapping(value = "securitylist/", produces = "application/json", method = RequestMethod.POST)
	@ApiOperation(value = "Set the supported fx symbols as json", hidden = false)
	@CrossOrigin
	public ArrayList<String> setSecurityList(@RequestBody SymbolList symlst )
			throws InterruptedException, Exception {
		symbollist.replaceAllSymbolsinArray(symlst.getFlattenedSymbolArray());
		return symbollist.getFlattenedSymbolArray();
	}

	@RequestMapping(value = "testrequest/", produces = "application/json", method = RequestMethod.POST)
	@ApiOperation(value = "defined test parameters for execution", hidden = false)
	@CrossOrigin
	public TestRequestEntity setFeedRequest(@RequestBody TestRequestEntity testrqst)
			throws InterruptedException, Exception {
			MarketDataRequest marketDataRequest = testManager.getMDRForTest(testrqst.getRequestId());
			Application appInstance = testManager.getApp();
			SessionID sessid = new SessionID("STR.UT.SIM.NEOV.9999");
			if(Session.doesSessionExist(sessid))
				appInstance.onMessage(marketDataRequest, sessid);

		return testrqst;
	}


} // end of class
