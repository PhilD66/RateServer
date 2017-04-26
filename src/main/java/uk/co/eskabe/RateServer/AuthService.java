package uk.co.eskabe.RateServer;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * AuthService
 * Author : Phil.D
 **/

public class AuthService {

    String testDB = "{ \"users\": [ { \"uniqueID\": \"1\", \"clientID\": \"client1\", \"username\": \"bilbo\", \"password\": \"baggins\" }, { \"uniqueID\": \"4072\", \"clientID\": \"client2\", \"username\": \"sauron\", \"password\": \"of-mordor\" } ] }";

    public AuthService() {
        //
    }

    public long isAuthenticated( String suppliedUsername, String suppliedPassword ) {
        // Basic parameter validation...
        if ( (suppliedUsername != null) && (suppliedUsername.length() > 0) && (suppliedPassword != null) && (suppliedPassword.length() > 0) ) {
            if ( suppliedUsername.compareToIgnoreCase("test") + suppliedPassword.compareToIgnoreCase("password") == 0 ) {
                return 0;
            } else {
                JSONParser parser = new JSONParser();
                JSONObject userDB;
                try {
                    userDB = (JSONObject) parser.parse(testDB);
                } catch (ParseException pEx) {
                    return -1;
                }

                JSONArray arrUsers = (JSONArray)userDB.get("users");
                for (int i = 0; i < arrUsers.size(); i++) {
                    JSONObject userObject = (JSONObject)arrUsers.get(i);
                    String strUsername = userObject.get("username").toString();
                    String strPassword = userObject.get("password").toString();
                    if ( (strUsername.compareTo(suppliedUsername) == 0) && (strPassword.compareTo(suppliedPassword) == 0) ) {
                        return Long.valueOf(userObject.get("uniqueID").toString()).longValue();
                    }
                }
                // TBD - This is where the authentication needs to go.
                return -1;
            }
        } else {
            return -1;
        }
    }
    
}