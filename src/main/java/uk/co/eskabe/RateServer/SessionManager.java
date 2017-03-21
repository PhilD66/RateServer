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
    
    private SessionManager() {
        sessionTable = new Hashtable<UUID, SessionObject>();
        theDoorman = new AuthService();
    }
    
    public UUID connect( String username, String password ) {
        long userID = -1;
        if ( (username == null) || (password == null) ) {
            return null;
        } else if ( (userID = theDoorman.isAuthenticated(username, password)) >= 0 ) {
            SessionObject newSession = new SessionObject( userID );
            sessionTable.put( newSession.getUUID(), newSession );
            return newSession.getUUID();
        } else {
            return null;
        }
    }
    
    public long disconnect( UUID sessionHandle ) {
        return -1;
    }
    
    public long subscribe( UUID sessionHandle, String fxPair ) {
        SessionObject thisSession = sessionTable.get(sessionHandle);
        if ( thisSession != null ) {
            return 1;
        } else {
            return -1;
        }
    } 
    
    public long unsubscribe( UUID sessionHandle, String fxPair ) {
        SessionObject thisSession = sessionTable.get(sessionHandle);
        if ( thisSession != null ) {
            return 1;
        } else {
            return -1;
        }
    } 
    
    public static SessionManager getInstance() {
        // Singleton handling...
        if ( singleSessionManager == null ) {
            singleSessionManager = new SessionManager();
        }
        
        return singleSessionManager;
    }
}