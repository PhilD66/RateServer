package uk.co.eskabe.RateServer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;

/**
 * Created by Phil on 21/04/2017.
 */
public class RateServerTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RateServerTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RateServerTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testSocketConnection() throws IOException {

        //RateClient rateClient = new RateClient();
        assertTrue( true );
    }
}
