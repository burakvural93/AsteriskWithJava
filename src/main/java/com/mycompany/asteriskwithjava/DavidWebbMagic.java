
package com.mycompany.asteriskwithjava;

import com.goebl.david.Webb;
import org.json.JSONObject;

/**
 *
 * @author Burak Vural
 */
public class DavidWebbMagic {
    public void postJson(JSONObject json){
        Webb webb = Webb.create();
        JSONObject result = webb.post("http://192.168.0.110/crm/dinle.php")
                                    .useCaches(false)
                                    .body(json)
                                    .ensureSuccess()
                                    .asJsonObject()
                                    .getBody();
    }
    
}
