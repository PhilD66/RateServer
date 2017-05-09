package uk.co.eskabe.RateServer;

import java.util.UUID;

/**
 * SessionObject
 * Author : Phil.D
 **/

public class SessionObject {

    public UUID sessionUUID = null;
    public long userID = -1;
    private RateUpdateListener rateUpdateListener = null;

    public SessionObject( long userID, RateUpdateListener rateListener ) {
        userID = userID;
        sessionUUID = UUID.randomUUID();
        rateUpdateListener = rateListener;
    }
    
    public UUID getUUID() {
        return sessionUUID;
    }
    
    public long getUserID() {
        return userID;
    }

    public RateUpdateListener getRateUpdateListener() { return rateUpdateListener; }
}