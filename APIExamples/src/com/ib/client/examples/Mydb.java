/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ib.client.examples;

import java.sql.*;
/**
 *
 * @author Trader
 */
public class Mydb extends DatabaseInterface{
    
    public Mydb (String sDriverKey, String sUrlKey) {
        try{
            init (sDriverKey, sUrlKey);
            
        }
        catch (Exception e){         
            e.printStackTrace();
        }
        finally {
            
        }
        
    }
    
    public void init(String sDriverVar, String sUrlVar) throws Exception {
        setDriver(sDriverVar);
        setUrl(sUrlVar);
        setConnection();
        setStatement();       
        System.out.println("End of init");
    }

    private void setDriver(String sDriverVar)
    {
    sDriver = sDriverVar;
    }

    private void setUrl(String sUrlVar)
    {
    sUrl = sUrlVar;
    }

    public  void setConnection() throws Exception {
        Class.forName(sDriver);    
        conn = DriverManager.getConnection(sUrl);
    }


    public  Connection getConnection() {
    return conn;
    }

    public  void setStatement() throws Exception {
    if (conn == null) {
    setConnection();
    }
    statement = conn.createStatement();
    statement.setQueryTimeout(iTimeout);  // set timeout to 30 sec.
    }

    public  Statement getStatement() {
    return statement;
    }

    public  void executeStmt(String instruction) throws SQLException {
    statement.executeUpdate(instruction);
    }

    // processes an array of instructions e.g. a set of SQL command strings passed from a file
    //NB you should ensure you either handle empty lines in files by either removing them or parsing them out 
    // since they will generate spurious SQLExceptions when they are encountered during the iteration....
    public void executeStmt(String[] instructionSet) throws SQLException {
    for (int i = 0; i < instructionSet.length; i++) {
    executeStmt(instructionSet[i]);
    }
    }

    public ResultSet executeQry(String instruction) throws SQLException {
    return statement.executeQuery(instruction);
    } 

    public void closeConnection() {
    try { conn.close(); } catch (Exception ignore) {}
    }
 
}
