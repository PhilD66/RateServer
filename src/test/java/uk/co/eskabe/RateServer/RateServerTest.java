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

        boolean runsOK = false;

        RateServer rateServer = new RateServer();
        try {
            System.out.println("Rate server spinning up...");

            (new Thread() {
                public void run() {
                    try {
                        rateServer.run(6789);
                    } catch (Exception ex) {
                        System.out.println("Serious exception encountered launching the server: " + ex.toString());
                        assertTrue(false);
                    }
                }
            }).start();

            RateClient rateClient = new RateClient();

            System.out.println("Prepping to run a series of client tests messaging the server...");
            rateClient.run(6789);

            // Once the 'run' thread of RateCLient terminates we can shutdown its receiverThread.
            rateClient.terminate();

            System.out.println("All looks good.");
            runsOK = true;
        } catch (Exception ex) {
            System.out.println("Something went wrong during the test: " + ex.toString());
        }

        assertTrue( runsOK );
    }
}
