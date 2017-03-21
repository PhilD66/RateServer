package uk.co.eskabe.RateServer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.UUID;

/**
 * Unit test for simple App.
 */
public class SessionManagerTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SessionManagerTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SessionManagerTest.class );
    }

    /**
     * Rigorous Test :-)
     */
    public void testGetInstanceOfSessionManager()
    {
        SessionManager sMan = SessionManager.getInstance();
        assertTrue( sMan != null );
    }
    
    public void testConnectOnSessionManagerWithBlankCredentials()
    {
        // Given that we can get a session manager instance...
        SessionManager sMan = SessionManager.getInstance();
        // and connect is invoked with a blank username and password...
        assertNull( sMan.connect("", "") );
        // the result (connection handle) is zero
    }
    
    public void testConnectOnSessionManagerWithTestCredentials()
    {
        // Given that we can get a session manager instance...
        SessionManager sMan = SessionManager.getInstance();
        // and connect is invoked with a test username and password...
        assertNotNull( sMan.connect("test", "password") );
        // the result (connection handle) value is not null.
    }
    
    public void testConnectOnSessionManagerWithBilboCredentials()
    {
        // Given that we can get a session manager instance...
        SessionManager sMan = SessionManager.getInstance();
        // and connect is invoked with a test username and password...
        assertNotNull( sMan.connect("bilbo", "baggins") );
        // the result (connection handle) value is not null.
    }

    public void testConnectOnSessionManagerWithSauronCredentials()
    {
        // Given that we can get a session manager instance...
        SessionManager sMan = SessionManager.getInstance();
        // and connect is invoked with a test username and password...
        assertNotNull( sMan.connect("sauron", "of-mordor") );
        // the result (connection handle) value is not null.
    }

    public void testRateSubscription()
    {
        // Given that we can get a session manager instance...
        SessionManager sMan = SessionManager.getInstance();
        // and connect is invoked with a good test username and password...
        UUID sessionHandle = sMan.connect("sauron", "of-mordor");
        // When we suscribe for an fxRate, no error is returned...
        assertFalse( sMan.subscribe(sessionHandle, "GBPUSD") == -1 );
        // such that the result value is not -1.
    }
        
}
