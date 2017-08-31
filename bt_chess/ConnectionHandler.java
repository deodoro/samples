package teste;

import javax.microedition.lcdui.*;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectionHandler implements CommandListener {

    // commands
    static final int CMD_EXIT = 1;
    static final int CMD_START = 2;
    static final int CMD_OPTIONS = 3;
    static final int CMD_DRAW = 4;
    static final int CMD_QUIT = 5;
    static final int CMD_UNDO = 6;
    static final int CMD_ZLAST = 7; // must be ze last, of course

    Command [] cmds;
    
    Display dpy;
    Game parent;
    
    Form connectionForm = new Form("Connection");
    List pickServerForm;

    ChoiceGroup connType;
    
    private Command ok;
    private Command cancel;
    private Command back;
    
    boolean connected;
    int color;

    private BTClient btClient;
    private BTServer btServer;
    
    public ConnectionHandler(Display _dpy, Game _parent) {
        
        dpy = _dpy;
        parent = _parent;

        connType = new ChoiceGroup(null, Choice.EXCLUSIVE);
        connType.append("Client", null);
        connType.append("Server Whites", null);
        connType.append("Server Blacks", null);

        ok = new Command("OK", Command.OK, 0);
        cancel = new Command("Cancel", Command.CANCEL, 1);
        back = new Command("Back", Command.BACK, 2);

        connectionForm.append(connType);
        connectionForm.addCommand(ok);
        connectionForm.addCommand(cancel);
        connectionForm.setCommandListener(this);
                                            
        connected = false;
        color = Piece.WHITE;
    }
    
    public void commandAction(Command c, Displayable d) {
        if (d == connectionForm) {
            if (c == ok) {
                switch(connType.getSelectedIndex()) {
                    case 0:
                        color = Piece.BLACK;
                        connectClient();
                        break;
                    case 1:
                        color = Piece.WHITE;
                        connectServer();
                        break;
                    case 2:
                        color = Piece.BLACK;
                        connectServer();
                        break;
                }
            }
            else
                parent.cancelGame();
        }
        else {
            if (d == pickServerForm) {
                if (c == ok) 
                    btClient.searchServices(pickServerForm.getString(pickServerForm.getSelectedIndex()));
                else {
                    if (btClient.getIsRunning())
                        btClient.cancelSearch();
                    btClient = null;
                    parent.cancelGame();
                }
            }
            else {          
                if (c == back) {
                    if (btServer != null) {
                        btServer.cancelConnection();
                        btServer = null;
                    }
                    parent.cancelGame();
                }
            }
        }
    }   
    
    public void connect() {
        dpy.setCurrent(connectionForm);
    }
    
    public boolean getConnected() {
        return connected;
    }
    
    private void connectClient() {
        pickServerForm = new List("Pick server", List.EXCLUSIVE);
        pickServerForm.addCommand(cancel);
        pickServerForm.setCommandListener(this);
        dpy.setCurrent(pickServerForm);
        btClient = new BTClient(this);
        btClient.searchServers(); 
    }

    private void connectServer() {
        Form waitForm = new Form("Connection");
        waitForm.append("Waiting for a connection");
        waitForm.addCommand(back);
        waitForm.setCommandListener(this);
        dpy.setCurrent(waitForm);
        btServer = new BTServer(this);
        btServer.waitForConnection();
    }
    
    public void addDevice(String device) {
        pickServerForm.append(device, null);
    }
    
    public void finishedServerSearch(boolean result) {
        if (result)
            pickServerForm.addCommand(ok);
        else
            parent.cancelGame();
    }
    
    public void informSearchError(String msg) {
        Alert alert = new Alert("Bluetooth Error");
        alert.setString(msg);
        alert.setTimeout(Alert.FOREVER);
        alert.addCommand(back);
        alert.setCommandListener(this);
        dpy.setCurrent(alert);
    }   
    
    public void notifyClientConnect(StreamConnection connection) {
        parent.startClientGame(color, new ProtocolHandler(connection));     
    }
    
    public void notifyServerConnect(StreamConnection connection) {
        parent.startServerGame(color, new ProtocolHandler(connection));
    }
    
}        