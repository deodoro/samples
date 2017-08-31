package teste;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

// midp/cldc API
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import java.util.Hashtable;

public class BTClient implements Runnable, DiscoveryListener {

    private static final UUID PICTURES_SERVER_UUID = new UUID("F0E0D0C0B0A000908070605040302010", false);
    private static final int IMAGES_NAMES_ATTRIBUTE_ID = 0x4321;

    private Hashtable devices = new Hashtable();    
    private DiscoveryAgent discoveryAgent;
    private boolean isClosed;
    private ConnectionHandler handler;
    private int discType;
    private boolean isRunning;
    private int searchId;
    private ServiceRecord serviceRecord;
    
    BTClient(ConnectionHandler parent) {
        isClosed = false;
        handler = parent;
    }
    
    public void searchServers() {
        searchId = -1;
        new Thread(this).start();
    }
    
    public void searchServices(String serverName) {
        RemoteDevice device = (RemoteDevice)devices.get(serverName);
        UUID[] uuidSet;
        int[] attrSet;
        
        uuidSet = new UUID[2];
        uuidSet[0] = new UUID(0x1101);
        uuidSet[1] = PICTURES_SERVER_UUID;
        attrSet = new int[1];
        attrSet[0] = IMAGES_NAMES_ATTRIBUTE_ID;
        
        try {
            searchId = discoveryAgent.searchServices(attrSet, uuidSet, device, this);
        } catch (BluetoothStateException e) {
            System.err.println("Can't search services for: " + device.getBluetoothAddress() +
                " due to " + e);
            searchId = -1;
        }
    }
    
    public void run() {
        // initialize bluetooth first
        boolean isBTReady = false;
        
        try {
            // create/get a local device and discovery agent
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();

            // remember we've reached this point.
            isBTReady = true;
        } catch (Exception e) {
            handler.informSearchError("Can't initialize bluetooth" + e);
        }

        // nothing to do if no bluetooth available
        if (!isBTReady) {
            return;
        }
        
        // search for devices
        if (!searchDevices()) {
            handler.finishedServerSearch(false);
            return;
        }
        else 
            handler.finishedServerSearch(true);

    }
    
    public void cancelSearch() {
        if (searchId == -1)
            discoveryAgent.cancelInquiry(this);
        else
            discoveryAgent.cancelServiceSearch(searchId);
    }
    
    public synchronized boolean getIsRunning() {
        return isRunning;
    }
    
    private boolean searchDevices() {
        devices = new Hashtable();

        try {
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
        } catch (BluetoothStateException e) {
            System.err.println("Can't start inquiry now: " + e);
            handler.informSearchError("Can't start device search");
            return false;
        }

        synchronized(this) {
            isRunning = true;
        }
        
        try {
            synchronized(this) {
                wait(); // until devices are found
            }
        } catch (InterruptedException e) {
            System.err.println("Unexpected interruption: " + e);
            return false;
        }

        
        synchronized(this) {
            isRunning = false;
        }

        // this "wake up" may be caused by 'destroy' call
        if (isClosed) {
            return false;
        }

        // no?, ok, let's check the return code then
        switch (discType) {
        case INQUIRY_ERROR:
            handler.informSearchError("Device discovering error...");

        // fall through
        case INQUIRY_TERMINATED:
            // make sure no garbage in found devices list
            devices = new Hashtable();
            // nothing to report - go to next request
            break;

        case INQUIRY_COMPLETED:

            if (devices.size() == 0) {
                handler.informSearchError("No devices in range");
            }

            // go to service search now
            break;

        default:
            // what kind of system you are?... :(
            System.err.println("system error:" + " unexpected device discovery code: " + discType);
            return false;
        }

        return true;
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        // same device may found several times during single search
        String name;
        try {
            name = btDevice.getFriendlyName(true);
            if (name.length() == 0)
                name = btDevice.getBluetoothAddress();
        } catch (IOException e) {
            name = btDevice.getBluetoothAddress();
        }
        handler.addDevice(name);
        devices.put(name, btDevice);
    }

    public void inquiryCompleted(int discType) {
        this.discType = discType;

        synchronized (this) {
            notify();
        }
    }
    
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        serviceRecord = servRecord[0];
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        try {
            String url = serviceRecord.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            StreamConnection conn = (StreamConnection)Connector.open(url);
            handler.notifyClientConnect(conn);
        } catch (IOException e) {
            handler.informSearchError("Can't connect to service: " + e);
        }
    }   
    
}  