package com.mycompany.asteriskwithjava;

import com.goebl.david.Webb;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.asteriskjava.live.OriginateCallback;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.action.StatusAction;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.HangupRequestEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.SoftHangupRequestEvent;
import org.asteriskjava.manager.response.ManagerResponse;
import org.asteriskjava.manager.event.AgentCalledEvent;
import org.asteriskjava.manager.event.AgentCompleteEvent;
import org.asteriskjava.manager.event.BridgeEvent;
import org.asteriskjava.manager.event.CdrEvent;
import org.asteriskjava.manager.event.DialEvent;
import org.asteriskjava.manager.event.ExtensionStatusEvent;
import org.asteriskjava.manager.event.NewAccountCodeEvent;
import org.asteriskjava.manager.event.NewExtenEvent;
import org.asteriskjava.manager.event.NewStateEvent;
import org.asteriskjava.manager.event.QueueMemberAddedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.asteriskjava.manager.event.TransferEvent;
import org.asteriskjava.manager.event.VarSetEvent;
import org.json.JSONException;
import org.json.JSONObject;

public class HelloEvents implements ManagerEventListener {

    //   Map<String,List<String>> hmap4ami= new HashMap<String,List<String>>();
    //   HashMap declaration to take sip
    Map<String, Object> hmap4ami = new HashMap();
    Object4Ami o4am;
    JSONObject json1;
    DavidWebbMagic dwm;
    String url = "192.168.0.43:3306";
    String username = "root";
    String password = "kaynakas";
    public MariaDbConnector mariadb;
    private ManagerConnection managerConnection;
    private static final Logger logger = Logger.getLogger(HelloEvents.class);

    public HelloEvents() throws IOException, SQLException, ClassNotFoundException {

        this.mariadb = new MariaDbConnector(url, username, password);
        ManagerConnectionFactory factory = new ManagerConnectionFactory(
                "192.168.0.43", "admin", "kaynakas");

        this.managerConnection = factory.createManagerConnection();
    }

    public void run() throws IOException, AuthenticationFailedException,
            TimeoutException, InterruptedException {

        // register for events
        managerConnection.addEventListener(this);

        // connect to Asterisk and log in
        managerConnection.login();

        // request channel state
        managerConnection.sendAction(new StatusAction());

        // wait 10 seconds for events to come in
        Thread.sleep(1000000);

        // and finally log off and disconnect
        managerConnection.logoff();

    }

    public void onManagerEvent(ManagerEvent event) {
        //  System.out.println(event.toString());
        //  logger.debug(event.toString());

        if (event instanceof DialEvent) {
            try {
                // System.out.println(event.toString());
                String src = ((DialEvent) event).getSrc().toString();
                String dst = ((DialEvent) event).getDestination().toString();
                String uniqueid = ((DialEvent) event).getUniqueId().toString();
                String source = src.split("\\-", 2)[0];
                String destination = dst.split("\\-", 2)[0];
                source = source.split("\\/", 2)[1];
                destination = destination.split("\\/", 2)[1];
                if (destination.contains("buraksiptrunkproviderout")) {
                    destination = ((DialEvent) event).getDialString().split("\\/", 2)[1];

                }
                if (source.contains("buraksiptrunkproviderout")) {
                    source = ((DialEvent) event).getCallerIdNum();
                }
                // ((DialEvent)event).getDialStatus()

                dwm = new DavidWebbMagic();
                Object4Ami o4am = new Object4Ami(uniqueid);
                json1 = new JSONObject();

                o4am.setCaller(source);
                o4am.setCalled(destination);
                o4am.setUniqueid(uniqueid);
                o4am.setStatus("dial");
                //   hmap4ami.put(uniqueid,requiredParameters);
                hmap4ami.put(uniqueid, o4am);
                // System.out.println(Arrays.asList(hmap4ami));
                System.out.println("[" + uniqueid + "] " + source + " ile " + destination + " Arasında çağrı başlatma girişimi bulunmaktadır ");

                mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status,scenario,date) "
                        + "VALUES ('" + uniqueid + "', '" + source + "', '" + destination + "','Ringing','" + destination + " Has rung" + "','" + event.getDateReceived() + "')");

                json1.put("UniqueId", uniqueid);
                json1.put("Caller", source);
                json1.put("Called", destination);
                json1.put("Status", "Ringing");
                json1.put("Scenario", destination + " Has rung");
                json1.put("Date", event.getDateReceived().toString());
                System.out.println(json1);
                dwm.postJson(json1);
                //System.out.println(json1);
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        if (event instanceof TransferEvent) {
            try {
                //System.out.println(event.toString());
                //System.out.println(((TransferEvent) event).getTransferExten());
                String unique4transfer = ((TransferEvent) event).getTargetUniqueId();
                Object4Ami o4am = (Object4Ami) hmap4ami.get(json1.get("UniqueId"));
                o4am.setStatus("dial");
                System.out.println("[" + unique4transfer + "] " + o4am.getCaller()
                        + " ile "
                        + o4am.getCalled()
                        + " arasındaki çağrı "
                        + ((TransferEvent) event).getTransferExten() + " e transfer edildi ");
                mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status,scenario,date) "
                        + "VALUES ('" + unique4transfer + "', '" + o4am.getCaller() + "', '" + o4am.getCalled() + "','Transferred','" + o4am.getCalled() + " Transferred Call to " + ((TransferEvent) event).getTransferExten() + "','"
                        + event.getDateReceived() + "')");
                json1.put("Status", "Transferred");
                json1.put("Scenario", o4am.getCalled() + " Transferred Call to " + ((TransferEvent) event).getTransferExten());
                json1.put("Date", event.getDateReceived().toString());
                System.out.println(json1);
                dwm.postJson(json1);

            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        if (event instanceof NewStateEvent) {
            //String unique4Ringing = ((NewStateEvent) event).getUniqueId().toString();
            if (((NewStateEvent) event).getState().equalsIgnoreCase("Up") && ((NewStateEvent) event).getConnectedlinenum() != null) {
                //System.out.println(event.toString());
                // System.out.println(((NewStateEvent) event).getCallerIdNum());
                // System.out.println(o4am.getCalled());
                Object4Ami o4am = (Object4Ami) hmap4ami.get(json1.get("UniqueId"));
                if (!(o4am.getStatus().equalsIgnoreCase("up"))) {
                    if ((o4am.getCaller().toString()).equalsIgnoreCase(((NewStateEvent) event).getCallerIdNum().toString()) || (o4am.getCalled().toString()).equalsIgnoreCase(((NewStateEvent) event).getCallerIdNum().toString())) {
                        try {
                            o4am.setStatus("up");
                            System.out.println("[" + json1.get("UniqueId") + "] " + ((NewStateEvent) event).getConnectedlinenum() + " Ahizeyi Kaldırdı ve Çağrı Başladı ");
                            mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status ,scenario,date) "
                                    + "VALUES ('" + json1.get("UniqueId") + "', '" + o4am.getCaller() + "', '" + o4am.getCalled() + "','Up','" + o4am.getCalled() + " Up the Phone" + "','" + event.getDateReceived() + "')");
                            json1.put("Status", "Up");
                            json1.put("Scenario", o4am.getCalled() + " Up the Phone");
                            //  ((NewStateEvent) event).getConnectedlinenum()
                            json1.put("Date", event.getDateReceived().toString());
                            System.out.println(json1);
                            dwm.postJson(json1);

                        } catch (SQLException ex) {
                            java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }

        if (event instanceof VarSetEvent) {
            //  System.out.println(event.toString());
              Object4Ami o4am = (Object4Ami) hmap4ami.get(json1.get("UniqueId"));
            if (((VarSetEvent) event).getVariable().equalsIgnoreCase("ANSWEREDTIME")) {

                try {
                    String unique4Answer = ((VarSetEvent) event).getUniqueId().toString();
                    o4am.setStatus("finished");
                    if (((VarSetEvent) event).getValue() != null) {
                        System.out.println("[" + unique4Answer + "] " + o4am.getCaller().toString()
                                + " ile "
                                + o4am.getCalled().toString()
                                + " arasında çağrı " + ((VarSetEvent) event).getValue() + " saniye sürdü ve bitti");
                        mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status,scenario,date) "
                                + "VALUES ('" + unique4Answer + "', '" + o4am.getCaller() + "', '" + o4am.getCalled() + "','Finished','" + ((VarSetEvent) event).getValue() + " second is talking time" + "','" + event.getDateReceived() + "')");
                        json1.put("Status", "Finished");
                        json1.put("Scenario", ((VarSetEvent) event).getValue() + " second is talking time");
                        json1.put("Date", event.getDateReceived().toString());
                        System.out.println(json1);
                        dwm.postJson(json1);
                    }

                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            if (((VarSetEvent) event).getValue().equalsIgnoreCase("BUSY") && ((VarSetEvent) event).getVariable().equalsIgnoreCase("DIALSTATUS")) {
                System.out.println((VarSetEvent) event);
                if (!(o4am.getStatus().equalsIgnoreCase("busy"))) {
                    try {
                        o4am.setStatus("busy");
                        //  System.out.println(event.toString());
                        String unique4Busy = ((VarSetEvent) event).getUniqueId();
                        //   List<String> sipValues =hmap4ami.get(unique4Busy);
                        System.out.println("[" + unique4Busy + "] " + o4am.getCaller().toString()
                                + " ile "
                                + o4am.getCalled().toString()
                                + " arasındaki çağrıyı "
                                + o4am.getCalled() + " meşgule attı ");
                        mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status,scenario,date) "
                                + "VALUES ('" + unique4Busy + "', '" + o4am.getCaller() + "', '" + o4am.getCalled() + "','Busy','" + o4am.getCalled() + " is Busy" + "','" + event.getDateReceived() + "')");
                        json1.put("Status", "Busy");
                        json1.put("Scenario", o4am.getCalled() + " is Busy");
                        json1.put("Date", event.getDateReceived().toString());
                        System.out.println(json1);
                        dwm.postJson(json1);
                        
                    } catch (SQLException ex) {
                        java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
            if (((VarSetEvent) event).getValue().equalsIgnoreCase("CANCEL") && ((VarSetEvent) event).getVariable().equalsIgnoreCase("DIALSTATUS")) {
                try {
                    //System.out.println(event.toString());
                    String unique4Cancel = ((VarSetEvent) event).getUniqueId().toString();
                    //   List<String> sipValues4cancel =hmap4ami.get(unique4Cancel);
                   // Object4Ami o4am = (Object4Ami) hmap4ami.get(unique4Cancel);
                    o4am.setStatus("cancel");
                    System.out.println("[" + unique4Cancel + "] " + o4am.getCaller().toString()
                            + " ile "
                            + o4am.getCalled().toString()
                            + " arasındaki çağrıyı "
                            + o4am.getCaller() + " iptal etti ");
                    mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status,scenario,date) "
                            + "VALUES ('" + unique4Cancel + "', '" + o4am.getCaller() + "', '" + o4am.getCalled() + "','Cancel','" + o4am.getCaller() + " has cancelled attempt to talk" + "','" + event.getDateReceived() + "')");
                    json1.put("Status", "Cancelled");
                    json1.put("Scenario", o4am.getCaller() + " has cancelled attempt to talk");
                    json1.put("Date", event.getDateReceived().toString());
                    System.out.println(json1);
                    dwm.postJson(json1);

                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);

                }

            }
        }
//        if (event instanceof ExtensionStatusEvent) {
//            if (((ExtensionStatusEvent) event).getStatus() == 0) {
//
//                System.out.println(event.toString());
//                Object4Ami o4am = (Object4Ami) hmap4ami.get(json1.get("UniqueId"));
//
//                if (!(o4am.getStatus().equalsIgnoreCase("busy"))) {
//                    if ((((ExtensionStatusEvent) event).getExten().toString()).equalsIgnoreCase(o4am.getCaller().toString()) || (((ExtensionStatusEvent) event).getExten().toString()).equalsIgnoreCase(o4am.getCalled().toString())) {
//                        try {
//                            o4am.setStatus("busy");
//                            System.out.println("[" + o4am.getUniqueid() + "] " + o4am.getCaller().toString()
//                                    + " ile "
//                                    + o4am.getCalled().toString()
//                                    + " arasındaki çağrıyı "
//                                    + o4am.getCalled() + " meşgule attı ");
//
//                            mariadb.getStatement().executeUpdate("INSERT INTO java2maria (uniqueid,caller, called, status,scenario,date) "
//                                    + "VALUES ('" + o4am.getUniqueid().toString() + "', '" + o4am.getCaller() + "', '" + o4am.getCalled() + "','Busy','" + o4am.getCalled() + " is Busy" + "','" + event.getDateReceived() + "')");
//                            json1.put("Status", "Busy");
//                            json1.put("Scenario", o4am.getCalled() + " is Busy");
//                            json1.put("Date", event.getDateReceived().toString());
//                            System.out.println(json1);
//                            dwm.postJson(json1);
//
//                        } catch (SQLException ex) {
//                            java.util.logging.Logger.getLogger(HelloEvents.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                }
//            }
        else {
        System.out.print("");
    }
    //   System.out.println(Arrays.asList(hmap4ami));
}


    public static void main(String[] args) throws Exception {
        HelloEvents helloEvents;
        helloEvents = new HelloEvents();
        helloEvents.run();

    }
}
