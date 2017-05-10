package uk.co.eskabe.RateServer;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Phil on 02/05/2017.
 */
public class RateEventEngine extends Thread {

    private boolean keepRunning = true;
    private boolean bPrintRates = false;
    protected long updateProbability = 5;   // 5 = likely, 50 = unlikely
    protected long updateIntervalMs = 1000;

    private ArrayList<RateObject> rates = new ArrayList<RateObject>();

    public RateEventEngine() {
        rates.add( new RateObject("SPOT", "GBPEUR") );
        rates.add( new RateObject("SPOT", "GBPUSD") );
        rates.add( new RateObject("SPOT", "GBPYEN") );
        rates.add( new RateObject("SPOT", "GBPAUD") );
        rates.add( new RateObject("SPOT", "GBPNZD") );

        rates.add( new RateObject("SPOT", "EURGBP") );
        rates.add( new RateObject("SPOT", "EURUSD") );
        rates.add( new RateObject("SPOT", "EURYEN") );
        rates.add( new RateObject("SPOT", "EURAUD") );
        rates.add( new RateObject("SPOT", "EURNZD") );

        rates.add( new RateObject("SPOT", "USDEUR") );
        rates.add( new RateObject("SPOT", "USDGBP") );
        rates.add( new RateObject("SPOT", "USDYEN") );
        rates.add( new RateObject("SPOT", "USDAUD") );
        rates.add( new RateObject("SPOT", "USDNZD") );

        rates.add( new RateObject("SPOT", "YENEUR") );
        rates.add( new RateObject("SPOT", "YENUSD") );
        rates.add( new RateObject("SPOT", "YENGBP") );
        rates.add( new RateObject("SPOT", "YENAUD") );
        rates.add( new RateObject("SPOT", "YENNZD") );

        rates.add( new RateObject("SPOT", "AUDEUR") );
        rates.add( new RateObject("SPOT", "AUDUSD") );
        rates.add( new RateObject("SPOT", "AUDYEN") );
        rates.add( new RateObject("SPOT", "AUDGBP") );
        rates.add( new RateObject("SPOT", "AUDNZD") );

        rates.add( new RateObject("SPOT", "NZDEUR") );
        rates.add( new RateObject("SPOT", "NZDUSD") );
        rates.add( new RateObject("SPOT", "NZDYEN") );
        rates.add( new RateObject("SPOT", "NZDAUD") );
        rates.add( new RateObject("SPOT", "NZDGBP") );
    }

    public void setUpdateIntervalMs( long newInterval ) { updateIntervalMs = newInterval; }

    public long getUpdateIntervalMs() { return updateIntervalMs; }

    public void kill() { keepRunning = false; }

    public void updatePrices() {
        Iterator iRates = rates.iterator();
        while ( iRates.hasNext() ) {
            RateObject thisRateObject = (RateObject)iRates.next();
            if ( ((long)(Math.random() * updateProbability) % (updateProbability - 1)) == 0 ) {
                thisRateObject.setPrice( Math.random() );
            }
            if ( bPrintRates ) {
                System.out.println(thisRateObject.getInfo() + " = " + String.valueOf(thisRateObject.getPriceAsString()));
            }
        }
    }

    public boolean addListener( String strRateObjectDef, RateUpdateListener addListener ) {
        Iterator iRates = rates.iterator();
        while ( iRates.hasNext() ) {
            RateObject thisRateObject = (RateObject)iRates.next();
            if ( thisRateObject.rateObjectEquals(strRateObjectDef)  ) {
                thisRateObject.addListener(addListener);
                return true;
            }
        }

        return false;
    }

    public boolean removeListener( String strRateObject, RateUpdateListener removeListener ) {
        Iterator iRates = rates.iterator();
        while ( iRates.hasNext() ) {
            RateObject thisRateObject = (RateObject)iRates.next();
            if ( thisRateObject.rateObjectEquals(strRateObject)  ) {
                thisRateObject.removeListener(removeListener);
                return true;
            }
        }

        return false;
    }

    public void run() {
        System.out.println("RateEventEngine started to create price updates.");
        while ( keepRunning ) {
            updatePrices();
            try {
                Thread.sleep(updateIntervalMs);
            } catch(InterruptedException intEx) {
                keepRunning = false;
            }
        }
        System.out.println("RateEventEngine killed. Price updates stopped.");
    }

}
