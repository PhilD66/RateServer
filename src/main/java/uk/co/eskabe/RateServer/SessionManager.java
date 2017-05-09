package uk.co.eskabe.RateServer;

import java.util.UUID;
import java.util.Hashtable;

/**
 * SessionManager
 * Author : Phil.D
 **/

public class SessionManager {
    
    public static SessionManager singleSessionManager = null;
    protected Hashtable<UUID, SessionObject> sessionTable = null; 
    protected AuthService theDoorman = null;
    protected RateEventEngine rateEventEngine = null;
    
    private SessionManager(RateEventEngine theEventEngine) {
        sessionTable = new Hashtable<UUID, SessionObject>();
        theDoorman = new AuthService();
        rateEventEngine = theEventEngine;
    }

    public UUID connect( String username, String password, RateUpdateListener rateListener ) {
        long userID = -1;
        if ( (username == null) || (password == null) ) {
            return null;
        } else if ( (userID = theDoorman.isAuthenticated(username, password)) >= 0 ) {
            SessionObject newSession = new SessionObject( userID, rateListener );
            sessionTable.put( newSession.getUUID(), newSession );
            return newSession.getUUID();
        } else {
            return null;
        }
    }
    
    public long disconnect( UUID sessionHandle ) {
        SessionObject sessionObj = sessionTable.remove( sessionHandle );
        if ( sessionObj != null ) {
            return 1;
        } else {
            return -1;
        }
    }
    
    public long subscribe( UUID sessionHandle, String instrument, String fxPair ) {
        SessionObject thisSession = sessionTable.get(sessionHandle);
        if ( thisSession != null ) {
            rateEventEngine.addListener(RateObject.makeRateObjectDefinition(instrument, fxPair), thisSession.getRateUpdateListener());
            return 1;
        } else {
            return -1;
        }
    } 
    
    public long unsubscribe( UUID sessionHandle, String instrument, String fxPair ) {
        SessionObject thisSession = sessionTable.get(sessionHandle);
        if ( thisSession != null ) {
            rateEventEngine.removeListener(RateObject.makeRateObjectDefinition(instrument, fxPair), thisSession.getRateUpdateListener());
            return 1;
        } else {
            return -1;
        }
    } 
    
    public static SessionManager getInstance(RateEventEngine theRateEngine) {
        // Singleton handling...
        if ( singleSessionManager == null ) {
            singleSessionManager = new SessionManager(theRateEngine);
        }
        
        return singleSessionManager;
    }
}