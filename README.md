# FIX DataPump Project
a FIX engine intended to create a load test envrionemtn for the the fxdataserverproject and associated ecosystem.

### based on QuickFixJ executor example code
Modifications made to reflect FIX data in response to standard requests

Using the FIX 44 protocol based on FXSpotStream ROE 
Implementing message types
   
   MarketDataRequest 35=W   
   MarketDataReject 35=v

### Test Request
Posting to /api/v1/fixdatapump/testrequest/ requires several parameters
* requestid - unique id for each request used to track the messages associated with it
* iterations - total number of messages sent for this requestID
* sleepms - the number of ms to sleep on each thread rotation
* delaybeforestartms - a time delay before the loop begins to execute
* fixmessageid - combined from server, symbol, volume and provider
* server - unique server name taken from HOSTNAME of the machine
* symbol - symbol/currency pair being requested. i.e. EUR/USD
* volume - number to be quoted
* provider - comes from a list of brokers/providers. BAML, JPMC, GS, CS, etc.
* sellprice - sets sell price
* buyprice - sets buy price
