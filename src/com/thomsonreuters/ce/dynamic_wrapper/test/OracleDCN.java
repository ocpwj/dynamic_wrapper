package com.thomsonreuters.ce.dynamic_wrapper.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleDriver;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.dcn.DatabaseChangeEvent;
import oracle.jdbc.dcn.DatabaseChangeListener;
import oracle.jdbc.dcn.DatabaseChangeRegistration;

public class OracleDCN {

    static final String USERNAME = "mpd2_cnr";
    static final String PASSWORD = "mpd2_cnr";
    static String URL = "jdbc:oracle:thin:@(description = (ADDRESS = (PROTOCOL = TCP)(HOST = c419jvfdw01t.int.thomsonreuters.com)(PORT = 1521))"
            + "(ADDRESS = (PROTOCOL = TCP)(HOST = c569xrtdw02t.int.thomsonreuters.com)(PORT = 1521))(load_balance = on)(connect_data ="
            + "(server = dedicated)(service_name = ord510a.int.thomsonreuters.com)))";

    public static void main(String[] args) {
        OracleDCN oracleDCN = new OracleDCN();
        try {
            oracleDCN.run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void run() throws Exception {
        OracleConnection conn = connect();
        Properties prop = new Properties();
        prop.setProperty(OracleConnection.DCN_NOTIFY_ROWIDS, "true");
        prop.setProperty(OracleConnection.DCN_IGNORE_DELETEOP, "true");
        prop.setProperty(OracleConnection.DCN_IGNORE_UPDATEOP, "true");
        DatabaseChangeRegistration dcr = conn.registerDatabaseChangeNotification(prop);

        try {
            dcr.addListener(new DCNDemoListener(this));

            Statement stmt = conn.createStatement();
            ((OracleStatement) stmt).setDatabaseChangeRegistration(dcr);
            ResultSet rs = stmt.executeQuery("SELECT * FROM universal_config");
            while (rs.next()) {
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            if (conn != null) {
                conn.unregisterDatabaseChangeNotification(dcr);
                conn.close();
            }
            throw ex;
        }
    }

    OracleConnection connect() throws SQLException {
        OracleDriver dr = new OracleDriver();
        Properties prop = new Properties();
        prop.setProperty("user", OracleDCN.USERNAME);
        prop.setProperty("password", OracleDCN.PASSWORD);
        return (OracleConnection) dr.connect(OracleDCN.URL, prop);
    }
}

class DCNDemoListener implements DatabaseChangeListener {
    OracleDCN demo;

    DCNDemoListener(OracleDCN dem) {
        demo = dem;
    }

    public void onDatabaseChangeNotification(DatabaseChangeEvent e) {
        Thread t = Thread.currentThread();
        System.out.println("DCNDemoListener: got an event (" + this + " running on thread " + t + ")");
        System.out.println(e.toString());
        System.out.println("Changed row id : "+e.getTableChangeDescription()[0].getRowChangeDescription()[0].getRowid().stringValue());
        synchronized (demo) {
            demo.notify();
        }
    }
}