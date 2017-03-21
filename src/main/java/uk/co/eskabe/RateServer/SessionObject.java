package uk.co.eskabe.RateServer;

import java.util.UUID;

/**
 * SessionObject
 * Author : Phil.D
 **/

public class SessionObject {

    public UUID sessionUUID = null;
    public long userID = -1;

    public SessionObject( long userID ) {
        userID = userID;
        sessionUUID = UUID.randomUUID();
    }
    
    public UUID getUUID() {
        return sessionUUID;
    }
    
    public long getUserID() {
        return userID;
    }
}