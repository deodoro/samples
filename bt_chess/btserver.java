package teste;

import java.io.IOException;

// jsr082 API
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

// midp/cldc API
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class BTServer implements Runnable {

    ConnectionHandler handler;
    private static final UUID PICTURES_SERVER_UUID =
        new UUID("F0E0D0C0B0A000908070605040302010", false);
    private static final int IMAGES_NAMES_ATTRIBUTE_ID = 0x4321;
    /** Keeps the local device reference. */
    private LocalDevice localDevice;
    private StreamConnectionNotifier notifier;
    private ServiceRecord record;
    StreamConnection conn;

    BTServer(ConnectionHandler parent) {
        handler = parent;
    }
    
    public void waitForConnection() {
        new Thread(this).start();
    }
    
    public void cancelConnection() {
        try {
            notifier.close();
        } catch (IOException e) {
            return;
        }       
    }
    
    public void run() {
        boolean isBTReady = false;
        conn = null;

        try {
            // create/get a local device
            localDevice = LocalDevice.getLocalDevice();

            // set we are discoverable
            if (!localDevice.setDiscoverable(DiscoveryAgent.GIAC)) {
                // Some implementations always return false, even if
                // setDiscoverable successful
                // throw new IOException("Can't set discoverable mode...");
            }

            // prepare a URL to create a notifier
            StringBuffer url = new StringBuffer("btspp://");

            // indicate this is a server
            url.append("localhost").append(':');

            // add the UUID to identify this service
            url.append(PICTURES_SERVER_UUID.toString());

            // add the name for our service
            url.append(";name=Picture Server");

            // request all of the client not to be authorized
            // some devices fail on authorize=true
            url.append(";authorize=false");

            // create notifier now
            notifier = (StreamConnectionNotifier)Connector.open(url.toString());

            // and remember the service record for the later updates
            record = localDevice.getRecord(notifier);

            // remember we've reached this point.
            isBTReady = true;
        } catch (Exception e) {
            handler.informSearchError("Can't initialize bluetooth" + e);
            return;
        }

        // nothing to do if no bluetooth available
        if (!isBTReady) {
            handler.informSearchError("Can't initialize bluetooth");
            return;
        }

        try {
            conn = notifier.acceptAndOpen();
            handler.notifyServerConnect(conn);
        } catch (IOException e) {
            // wrong client or interrupted - continue anyway
            return;
        }       
    }

}