package com.ib.client.examples;

import com.ib.client.*;
import java.sql.*;
import java.text.*;
import java.lang.Math;


/**
 * Simple example which will pull the last price for a given symbol. 
 * 
 * API requests:
 *  eConnect
 *  reqMktData (snapshot)
 *  eDisconnect
 * 
 * API callbacks:
 *  tickPrice
 * 
 * $Id$
 */
public class Example1 extends ExampleBase {

    private String symbol = null;
    private int requestId = 0;
    private double lastPrice = 0.0;
    private double askPrice = 0.0;
    private double bidPrice = 0.0;
    private double futLastPrice = 0.0;
    private double etfLastPrice = 0.0;
    private long startStrike = 0;
    private int endStrike = 0;
    private int numMonths = 3; //(set to 3 for now), later to be read in from db
    private String myOptionSymbol;
    private int numCur = 2;
    

    public Example1(String symbol) {
        this.symbol = symbol;
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++){
            String dbDriver = "org.sqlite.JDBC";            
            String dbUrl = "jdbc:sqlite:" + args[i] + "options.db";    
            Mydb db = new Mydb(dbDriver, dbUrl);
            
            try{
                db.executeStmt("drop table if exists options");
                db.executeStmt("create table options (id int, symbol String, lastPrice float, bidPrice float, askPrice float)");                           
            }
            catch (SQLException e){
                e.printStackTrace();
            }
            finally {            
            }         
            new Example1(args[i]).start();
            try{
                sleep(10000);
            }
            catch (Throwable t){
                
            }
            
        }

        
    }
                      
    public void run() {   
        String dbDriver = "org.sqlite.JDBC";
        String dbUrl = "jdbc:sqlite:" + symbol + "options.db";
        System.out.println(symbol);
        
        Mydb db = new Mydb(dbDriver, dbUrl);
        
        boolean isSuccess = false;
        int waitCount = 0;
        int intStartStrike = 0;
        String contractClass = null;
        String contractMonth = null;
        String expStrike = null;
        String rightCP = null;
        String stmt = null;
        String strike = null;
        
   
        if (symbol.equals("EUR")){
            contractClass = "6E";
        }

        if (symbol.equals("CAD")){
            contractClass = "6C";
        }
        
        if (symbol.equals("FXE")){
            contractClass = "FXE";
        }
        if (symbol.equals("FXC")){
            contractClass = "FXC";
        }
            
        if (contractClass.length() == 2){
            getFuturesOptionsData(contractClass);
        }
        else if (contractClass.length() == 3){
            getETFOptionsData(contractClass);
        }                    
    }           

    private void getFuturesOptionsData(String contractClass){
        String dbDriver = "org.sqlite.JDBC";
        String dbUrl = "jdbc:sqlite:" + symbol + "options.db";
        System.out.println(symbol);
        
        Mydb db = new Mydb(dbDriver, dbUrl);
        
        boolean isSuccess = false;
        int waitCount = 0;
        int intStartStrike = 0;   
        String contractMonth = null;
        String expStrike = null;
        String rightCP = null;
        String stmt = null;
        String strike = null;        
        
        System.out.println("ContractClass = " + contractClass);
        DecimalFormat decFor = new DecimalFormat ("00.00000");        
        
        try{
           connectToTWS();
           Contract contract = createContract(symbol, "FUT", "GLOBEX", "USD", "201209", null, 0);
           eClientSocket.reqMktData(requestId++, contract, "", false);
           while (!isSuccess && waitCount < MAX_WAIT_COUNT) {             
                // Check if last price loaded
                if (lastPrice != 0.0) {
                    isSuccess = true;
                    futLastPrice = lastPrice;
                }

                if (!isSuccess) {
                    sleep(WAIT_TIME); // Pause for 1 second
                    waitCount++;
                }
            }      
            eClientSocket.cancelMktData(requestId-1);
        
            startStrike = Math.round(lastPrice*1000);
            intStartStrike = (int)startStrike;              
            intStartStrike = intStartStrike - (intStartStrike % 5);            
            
            endStrike = intStartStrike + 5;
            int numOpt = 0;
            System.out.println(intStartStrike);

            for (int i = 1; i <= numMonths; i++){
                switch (i){
                    case 1: contractMonth = "U2";
                        break;
                    case 2: contractMonth = "V2";
                        break;
                    case 3: contractMonth = "X2";
                }          
                for (int j = intStartStrike - 5; j <= endStrike; j = j+5){
                    for (int k = 1; k<=2; k++){
                        switch (k){
                            case 1: rightCP = "C";
                                break;
                            case 2: rightCP = "P";
                        }
                        if (j >= 1000){
                            strike = String.valueOf(j);
                        }
                        else {
                            strike = "0" + String.valueOf(j);
                        }
                            
                        myOptionSymbol = contractClass + contractMonth + " " + rightCP + strike;                      
                        isSuccess = false;
                        waitCount = 0;   
                        lastPrice = askPrice = bidPrice = 0;

                    // Create a contract, with defaults...
                        Contract optionContract = createFutOptionContract(myOptionSymbol, "FOP", "GLOBEX", "USD", requestId+1);
                    // Requests snapshot market data
                        eClientSocket.reqMktData(requestId++, optionContract, "", false);            
 
                        //get offer price
                 //      tickPrice(requestId, 2, askPrice, 0);            
                    //get bid price
                   //     tickPrice(requestId, 1, bidPrice, 0);            
                    //get last price
                     //   tickPrice(requestId, 4, lastPrice, 0);   

                        while (!isSuccess && waitCount < MAX_WAIT_COUNT) {             
                        // Check if last price loaded
                            if ((lastPrice != 0.0) || (askPrice != 0.0 && bidPrice != 0.0)) {
                                isSuccess = true;
                            }

                            if (!isSuccess) {
                                sleep(WAIT_TIME); // Pause for 1 second
                                waitCount++;
                            }
                        }
                        eClientSocket.cancelMktData(requestId-1);
                        numOpt++;
                    // Display results
                        if (isSuccess) {
                            stmt = "Insert into options values(" + requestId + ", '" + myOptionSymbol + "', " + lastPrice + ", " + bidPrice + ", " + askPrice + ")";
                            //System.out.println(stmt);
                            
                            db.executeStmt(stmt);
                            //System.out.println(" [Info] Ask price for " + myOptionSymbol + " was: " + decFor.format(askPrice));
                            //System.out.println(" [Info] Bid price for " + myOptionSymbol + " was: " + decFor.format(bidPrice));
                            //System.out.println(" [Info] Last price for " + myOptionSymbol + " was: " + decFor.format(lastPrice));
                        } else {
                            stmt = "Insert into options values(" + requestId + ", '" + myOptionSymbol + "', 0, 0, 0)";
                            db.executeStmt(stmt);
                            //System.out.println(" [Error] Failed to retrieve mkt data " + myOptionSymbol);
                        }
                    }
                } 
                
            }
            
            
            ResultSet rs = db.executeQry("select * from options");            
            int fail =0;
            int success = 0;
            int total = 0;
            
            while (rs.next()){
                total++;
                if (rs.getDouble("askPrice")==0 && rs.getDouble("bidPrice")==0 && rs.getDouble("lastPrice")==0){
                    fail++;
                   // System.out.println(rs.getString("symbol"));
                }
                else{
                    success++;
                }
                System.out.println(rs.getString("symbol") + ": " + rs.getDouble("lastPrice") + "  " + rs.getDouble("bidPrice") + "  " + rs.getDouble("askPrice"));
            }
            System.out.println("success: " + success);
            System.out.println("fail: " + fail);
            System.out.println("total: " + total);
            System.out.println("numOpt: " + numOpt);
        }
        catch (Throwable t) {
            System.out.println("Example1.run() :: Problem occurred during processing: " + t.getMessage());
        } 
        finally {
            disconnectFromTWS();
        }                           
    }
    
   private void getETFOptionsData(String contractClass){
        String dbDriver = "org.sqlite.JDBC";
        String dbUrl = "jdbc:sqlite:" + symbol + "options.db";
        System.out.println(symbol);
        
        Mydb db = new Mydb(dbDriver, dbUrl);
        
        boolean isSuccess = false;
        int waitCount = 0;
        int intStartStrike = 0;   
        String contractMonth = null;
        String expMonth = null;
        String rightCP = null;
        String stmt = null;
        String strike = null;
        double doubleStrike = 0.0;
 
        
        try{
           connectToTWS();
           Contract contract = createContract(symbol, "STK", "SMART", "USD");
           eClientSocket.reqMktData(requestId++, contract, "", false);
           while (!isSuccess && waitCount < MAX_WAIT_COUNT) {             
                // Check if last price loaded
                if (lastPrice != 0.0) {
                    isSuccess = true;
                    etfLastPrice = lastPrice;
                }

                if (!isSuccess) {
                    sleep(WAIT_TIME); // Pause for 1 second
                    waitCount++;
                }
            }      
            eClientSocket.cancelMktData(requestId-1);
            System.out.println(symbol + ": " + etfLastPrice);
            
            startStrike = Math.round(etfLastPrice);
            intStartStrike = (int)startStrike;              
            endStrike = intStartStrike + 1;
            
            int numOpt = 0;
            System.out.println(intStartStrike);

            for (int i = 1; i <= numMonths; i++){
                switch (i){
                    case 1: contractMonth = "120818";
                        expMonth = "20120817";
                        break;
                    case 2: contractMonth = "120922";
                        expMonth = "20120921";
                        break;
                    case 3: contractMonth = "121222";
                        expMonth = "20121221";
                        break;
                }
                
                
                for (int j = intStartStrike - 2; j <= endStrike; j++){
                    for (int k = 1; k<=2; k++){
                        switch (k){
                            case 1: rightCP = "C";
                                break;
                            case 2: rightCP = "P";
                        }
                        strike = String.valueOf(j);
                        switch (strike.length()){
                            case 1: strike = "0000" + strike + "000";
                                break;
                            case 2: strike = "000" + strike + "000";
                                break;
                            case 3: strike = "00" + strike + "000";
                                break;
                            case 4: strike = "0" + strike + "000";
                                break;                                
                        }
                        
                        myOptionSymbol = contractClass + "   " + contractMonth + rightCP + strike;
                        System.out.println(myOptionSymbol);
                        isSuccess = false;
                        waitCount = 0;   
                        lastPrice = askPrice = bidPrice = 0;
                        doubleStrike = Double.valueOf(j);
                        
                    // Create a contract, with defaults...
                        Contract optionContract = createEtfOptionContract(symbol, myOptionSymbol, "OPT", "SMART", "USD", expMonth, doubleStrike, rightCP, requestId+1);
                    // Requests snapshot market data
                        eClientSocket.reqMktData(requestId++, optionContract, "", false);            
 
                        //get offer price
                 //      tickPrice(requestId, 2, askPrice, 0);            
                    //get bid price
                   //     tickPrice(requestId, 1, bidPrice, 0);            
                    //get last price
                     //   tickPrice(requestId, 4, lastPrice, 0);   

                        while (!isSuccess && waitCount < MAX_WAIT_COUNT) {             
                        // Check if last price loaded
                            if ((lastPrice != 0.0) || (askPrice != 0.0 && bidPrice != 0.0)) {
                                isSuccess = true;
                            }

                            if (!isSuccess) {
                                sleep(WAIT_TIME); // Pause for 1 second
                                waitCount++;
                            }
                        }
                        eClientSocket.cancelMktData(requestId-1);
                        numOpt++;
                    // Display results
                        if (isSuccess) {
                            stmt = "Insert into options values(" + requestId + ", '" + myOptionSymbol + "', " + lastPrice + ", " + bidPrice + ", " + askPrice + ")";
                            //System.out.println(stmt);
                            
                            db.executeStmt(stmt);
                            //System.out.println(" [Info] Ask price for " + myOptionSymbol + " was: " + decFor.format(askPrice));
                            //System.out.println(" [Info] Bid price for " + myOptionSymbol + " was: " + decFor.format(bidPrice));
                            //System.out.println(" [Info] Last price for " + myOptionSymbol + " was: " + decFor.format(lastPrice));
                        } else {
                            stmt = "Insert into options values(" + requestId + ", '" + myOptionSymbol + "', 0, 0, 0)";
                            db.executeStmt(stmt);
                            //System.out.println(" [Error] Failed to retrieve mkt data " + myOptionSymbol);
                        }
                    }
                } 
                
            }
            
            
            ResultSet rs = db.executeQry("select * from options");            
            int fail =0;
            int success = 0;
            int total = 0;
            
            while (rs.next()){
                total++;
                if (rs.getDouble("askPrice")==0 && rs.getDouble("bidPrice")==0 && rs.getDouble("lastPrice")==0){
                    fail++;
                   // System.out.println(rs.getString("symbol"));
                }
                else{
                    success++;
                }
                System.out.println(rs.getString("symbol") + ": " + rs.getDouble("lastPrice") + "  " + rs.getDouble("bidPrice") + "  " + rs.getDouble("askPrice"));
            }
            System.out.println("success: " + success);
            System.out.println("fail: " + fail);
            System.out.println("total: " + total);
            System.out.println("numOpt: " + numOpt);
        }
        catch (Throwable t) {
            System.out.println("Example1.run() :: Problem occurred during processing: " + t.getMessage());
        } 
        finally {
            disconnectFromTWS();
        }                           
    }    
    
    
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        if (field == TickType.LAST) {
            lastPrice = price;
        }
        if (field == TickType.ASK) {
            askPrice = price;
        }
        if (field == TickType.BID){
            bidPrice = price;
        }
    }
}
