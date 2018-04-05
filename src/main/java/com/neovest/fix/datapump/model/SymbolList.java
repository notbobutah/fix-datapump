package com.neovest.fix.datapump.model;

import com.neovest.fix.datapump.resource.FixDataPumpWebController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Component
public class SymbolList {
    private static final Logger log = LoggerFactory.getLogger(FixDataPumpWebController.class);
    private String[][] symbols = {{"CAD/NOK","CAD/NZD","CAD/PHP","CAD/PLN","CAD/SEK","CAD/SGD","CAD/THB","CAD/TWD","CAD/USD","CAD/ZAR","CAD/ZMW","CHF/AUD","CHF/CAD","CHF/CNH","CHF/CNY","CHF/CZK","CHF/DKK","CHF/EUR","CHF/GBP","CHF/GHS","CHF/HKD","CHF/HUF","CHF/IDR","CHF/INR","CHF/JPY","CHF/KES","CHF/KRW","CHF/MXN","CHF/MYR","CHF/NOK","CHF/NZD","CHF/PHP","CHF/PLN","CHF/RON","CHF/SEK","CHF/SGD","CHF/THB","CHF/TRY","CHF/TWD","CHF/USD","CHF/ZAR","CHF/ZMW","CLP/USD","CNH/CNY","CNH/HKD","CNH/IDR","CNH/INR","CNH/JPY","CNH/KRW","CNH/PHP"},
            {"AUD/BRL","AUD/CAD","AUD/CHF","AUD/CNY","AUD/CZK","AUD/DKK","AUD/EUR","AUD/GBP","AUD/GHS","AUD/HKD","AUD/HUF","AUD/IDR","AUD/ILS","AUD/INR","AUD/JPY","AUD/KES","AUD/KRW","AUD/MXN","AUD/MYR","AUD/NGN","AUD/NOK","AUD/NZD","AUD/PHP","AUD/PLN","AUD/SEK","AUD/SGD","AUD/THB","AUD/TRY","AUD/TWD","AUD/USD","AUD/ZAR","AUD/ZMW","BRL/USD","CAD/AUD","CAD/BRL","CAD/CHF","CAD/CNH","CAD/CNY","CAD/CZK","CAD/DKK","CAD/EUR","CAD/GBP","CAD/HKD","CAD/HUF","CAD/IDR","CAD/INR","CAD/JPY","CAD/KRW","CAD/MXN","CAD/MYR"},
            {"CNH/SGD","CNH/TWD","CNH/USD","CNY/USD","COP/USD","CZK/HUF","CZK/JPY","DKK/AUD","DKK/CHF","DKK/EUR","DKK/GBP","DKK/HKD","DKK/HUF","DKK/JPY","DKK/NOK","DKK/PLN","DKK/SEK","DKK/SGD","DKK/THB","DKK/USD","DKK/ZAR","DKK/ZMW","EUR/AED","EUR/AUD","EUR/BRL","EUR/CAD","EUR/CHF","EUR/CNH","EUR/CNY","EUR/CZK","EUR/DKK","EUR/GBP","EUR/GHS","EUR/HKD","EUR/HUF","EUR/IDR","EUR/ILS","EUR/INR","EUR/JPY","EUR/KES","EUR/KRW","EUR/KWD","EUR/MXN","EUR/MYR","EUR/NOK","EUR/NZD","EUR/OMR","EUR/PHP","EUR/PLN","EUR/RON"},
            {"EUR/RUB","EUR/SAR","EUR/SEK","EUR/SGD","EUR/THB","EUR/TRY","EUR/TWD","EUR/TWO","EUR/USD","EUR/ZAR","EUR/ZMW","GBP/AED","GBP/AUD","GBP/BRL","GBP/CAD","GBP/CHF","GBP/CNH","GBP/CNY","GBP/CZK","GBP/DKK","GBP/EUR","GBP/GHS","GBP/HKD","GBP/HUF","GBP/IDR","GBP/ILS","GBP/INR","GBP/JPY","GBP/KES","GBP/KRW","GBP/KWD","GBP/MXN","GBP/MYR","GBP/NOK","GBP/NZD","GBP/OMR","GBP/PHP","GBP/PLN","GBP/RON","GBP/SAR","GBP/SEK","GBP/SGD","GBP/THB","GBP/TRY","GBP/TWD","GBP/USD","GBP/ZAR","GBP/ZMW","HKD/CNH","HKD/CNY"},
            {"HKD/IDR","HKD/INR","HKD/JPY","HKD/KRW","HKD/PHP","HKD/SEK","HKD/THB","HKD/TWD","HKD/USD","IDR/USD","ILS/JPY","INR/USD","JPY/AUD","JPY/CAD","JPY/CHF","JPY/CNH","JPY/DKK","JPY/EUR","JPY/GBP","JPY/HKD","JPY/HUF","JPY/MXN","JPY/NOK","JPY/NZD","JPY/SEK","JPY/SGD","JPY/TRY","JPY/TWD","JPY/USD","JPY/ZAR","JPY/ZMW","KRW/USD","MXN/AUD","MXN/CHF","MXN/EUR","MXN/GBP","MXN/JPY","MXN/USD","MYR/USD","NOK/AUD","NOK/CHF","NOK/CZK","NOK/DKK","NOK/EUR","NOK/GBP","NOK/HKD","NOK/HUF","NOK/JPY","NOK/SEK","NOK/USD"},
            {"NOK/ZAR","NOK/ZMW","NZD/AUD","NZD/BRL","NZD/CAD","NZD/CHF","NZD/CNY","NZD/CZK","NZD/DKK","NZD/EUR","NZD/GBP","NZD/HKD","NZD/HUF","NZD/IDR","NZD/INR","NZD/JPY","NZD/KRW","NZD/MYR","NZD/NOK","NZD/PHP","NZD/PLN","NZD/SEK","NZD/SGD","NZD/THB","NZD/TRY","NZD/TWD","NZD/USD","NZD/ZAR","NZD/ZMW","OMR/JPY","PEN/USD","PHP/USD","PLN/CZK","PLN/DKK","PLN/HUF","PLN/JPY","PLN/NOK","PLN/SEK","PLN/ZAR","RON/NOK","RUB/JPY","RUB/USD","SEK/AUD","SEK/CHF","SEK/CZK","SEK/DKK","SEK/EUR","SEK/GBP","SEK/HKD","SEK/HUF"},
            {"SEK/JPY","SEK/NOK","SEK/USD","SEK/ZMW","SGD/AUD","SGD/CHF","SGD/CNH","SGD/CNY","SGD/EUR","SGD/GBP","SGD/HUF","SGD/IDR","SGD/INR","SGD/JPY","SGD/KRW","SGD/MYR","SGD/NOK","SGD/PHP","SGD/PLN","SGD/RUB","SGD/SEK","SGD/THB","SGD/TWD","SGD/USD","SGD/ZAR","THB/IDR","THB/INR","THB/JPY","THB/KRW","THB/PHP","THB/TWD","TRY/AUD","TRY/CHF","TRY/DKK","TRY/EUR","TRY/GBP","TRY/JPY","TRY/MXN","TRY/NZD","TRY/USD","TWD/USD","USD/AED","USD/ARS","USD/AUD","USD/BRL","USD/CAD","USD/CHF","USD/CLP","USD/CNH","USD/CNY"},
            {"XPT/ZAR","XTS/XTS","ZAR/AUD","ZAR/CHF","ZAR/EUR","ZAR/GBP","ZAR/JPY","ZAR/MXN","ZAR/USD"}};


    ArrayList<ArrayList<String>> symbolarray = new ArrayList<ArrayList<String>>();
    private static SymbolList instance = null;

    protected SymbolList() {}

    public static SymbolList getInstance(){
        if(instance == null )
            instance = new SymbolList();
        return instance;
    }

    public void loadSymbols(){
        log.error("symbollist constructor  : "+ this.hashCode());

            for(String[] lst: symbols){
                ArrayList<String> strlist = new ArrayList<String>();
                for(String item: lst) {
                    strlist.add(item);
                }
                symbolarray.add(strlist);
            }
        }

    public ArrayList<ArrayList<String>> getSymbolarray() {
        return symbolarray;
    }
    public int getSymbolarraySize() {
        return symbolarray.size();
    }

    public void setSymbolarray(ArrayList<ArrayList<String>> symbolarray) {
        this.symbolarray = symbolarray;
    }
    public void addInnerSymbolarray(ArrayList<String> p_symbolarray) {
        this.symbolarray.add(p_symbolarray);
    }
    public int replaceAllSymbolsinArray(ArrayList<String> p_symbolarray) {
        symbolarray = new ArrayList<ArrayList<String>>();

        if (symbolarray.isEmpty()) {
            symbolarray.add(p_symbolarray);
        }
        return symbolarray.size();
    }
    public ArrayList<String> getFlattenedSymbolArray(){

        ArrayList<String> strlist = new ArrayList<String>();
        for(ArrayList<String> lst: symbolarray){
            for(String item: lst) {
                strlist.add(item);
            }
        }
        return strlist;
    }
    public ArrayList<String> getFlattenedSymbolArraytoCount(int cnt){

        int index=cnt;
        ArrayList<String> strlist = new ArrayList<String>();
        for(ArrayList<String> lst: symbolarray){
            for(String item: lst) {
                strlist.add(item);
                if(index <= 0) break;
                cnt--;
            }
            if(index <= 0) break;
        }
        return strlist;
    }

}
