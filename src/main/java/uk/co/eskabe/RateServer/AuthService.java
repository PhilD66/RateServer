package uk.co.eskabe.RateServer;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * AuthService
 * Author : Phil.D
 **/

public class AuthService {

    String testDB = "{ \"users\": [ { \"uniqueID\": \"1\", \"clientID\": \"client1\", \"username\": \"bilbo\", \"password\": \"baggins\" }, { \"uniqueID\": \"4072\", \"clientID\": \"client2\", \"username\": \"sauron\", \"password\": \"of-mordor\" } ] }";

    public AuthService() {
        //
    }

    public long isAuthenticated( String username, String password ) {
        // Basic parameter validation...
        if ( (username != null) && (username.length() > 0) && (password != null) && (password.length() > 0) ) {
            if ( username.compareToIgnoreCase("test") + password.compareToIgnoreCase("password") == 0 ) {
                return 0;
            } else {
                JSONObject userDB = new JSONObject(testDB);
                JSONArray arrUsers = userDB.getJSONArray("users");
                for (int i = 0; i < arrUsers.length(); i++) {
                    JSONObject userObject = arrUsers.getJSONObject(i);
                    if ( userObject.getString("username").compareTo(username) + userObject.getString("password").compareTo(password) == 0 ) {
                        return userObject.getLong("uniqueID");
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