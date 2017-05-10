package uk.co.eskabe.RateServer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;

/**
 * Created by Phil on 10/05/2017.
 */
public class MessageRateUpdateTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MessageRateUpdateTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( MessageRateUpdateTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testRateUpdateMessage() {

        MessageRateUpdate updateMsg = new MessageRateUpdate("SESSION-ID", "SPOT", "EURGBP", 10.99 );
        String strMsg = updateMsg.writeOut();

        assertEquals( strMsg, "{\"verb\": \"update\", \"params\": {\"sessionId\": \"SESSION-ID\", \"instrument\": \"SPOT\", \"fxPair\": \"EURGBP\", \"price\": \"10.99\"}}" );
    }
}
