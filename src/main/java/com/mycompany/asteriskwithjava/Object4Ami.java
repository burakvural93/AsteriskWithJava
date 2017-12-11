
package com.mycompany.asteriskwithjava;

/**
 *
 * @author Burak Vural
 */
public class Object4Ami {
    
       // private String caller,called;
        
    private String uniqueid;
    private String caller;
    private String called;
    private String status;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
     public Object4Ami(String uniqueId) {
        uniqueid=uniqueId;
    }

    public String getUniqueid() {
        return uniqueid;
    }

    public void setUniqueid(String uniqueid) {
        this.uniqueid = uniqueid;
    }
    
    public  void setCalled(String ced) {
       called= ced;
    }
    public  void setCaller(String cer) {
        caller = cer;
    }
    public String getCaller() {
        return caller;
    }
    public String getCalled() {
        return called;
    }
   
    
    
    
}
