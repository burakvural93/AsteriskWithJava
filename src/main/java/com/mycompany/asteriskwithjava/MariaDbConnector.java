
package com.mycompany.asteriskwithjava;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 *
 * @author Burak Vural
 */
public class MariaDbConnector {
    
    private Statement statement;

    public void setStatement(Statement stament) {
        this.statement = stament;
    }

    public Statement getStatement() {
        return statement;
    }
    public MariaDbConnector(String url,String username,String password) throws SQLException, ClassNotFoundException {
        Class.forName("org.mariadb.jdbc.Driver");    
        Connection connection = DriverManager.getConnection("jdbc:mariadb://"+url+"/javaMariaCallReport?user="+username+"&password="+password);
        statement=connection.createStatement();
     
    }
    
    
    
}
