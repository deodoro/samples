package teste;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.Connector;
import java.util.Vector;

public class ProtocolHandler {

    final static byte CMD_MOVE = 0;
    final static byte CMD_START = 1;
    final static byte CMD_CHECKMATE = 2;
    final static byte CMD_PROMOTION = 3;
    final static byte CMD_REQUESTTAKEBACK = 4;
    final static byte CMD_REPLYTAKEBACK = 5;
    final static byte CMD_REQUESTDRAW = 6;
    final static byte CMD_REPLYDRAW = 7;
    
    private byte [] bytes;
    private Game game;
    private StreamConnection connection;
    private Thread readThread, writeThread;
    private Vector sendQueue = new Vector();
    private boolean isOpen;
    
    public ProtocolHandler() {
    }
    
    public ProtocolHandler(StreamConnection _conn) {
        connection = _conn;
        readThread = new Thread(new ReadThread());
        writeThread = new Thread(new WriteThread());
        isOpen = true;
        readThread.start();
        writeThread.start();
    }
    
    public void setGame(Game _game) {
        game = _game;
    }
    
    public void sendMove(String move) {
        byte [] bytes = new byte[5];
        byte [] aux = move.getBytes();
        bytes[0] = CMD_MOVE;
        for (int i = 0; i < aux.length; i++)
            bytes[1 + i] = aux[i];
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendMove(String move, int type) {
        byte [] bytes = new byte[6];
        byte [] aux = move.getBytes();
        bytes[0] = CMD_PROMOTION;
        for (int i = 0; i < aux.length; i++)
            bytes[1 + i] = aux[i];
        bytes[5] = (byte)type;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendStart(int your_color) {
        byte [] bytes = new byte[2];
        bytes[0] = CMD_START;
        bytes[1] = (byte)your_color;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendCheckMate() {
        byte [] bytes = new byte[1];
        bytes[0] = CMD_CHECKMATE;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendRequestTakeback() {
        byte [] bytes = new byte[1];
        bytes[0] = CMD_REQUESTTAKEBACK;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendTakebackReply(boolean value) {
        byte [] bytes = new byte[2];
        bytes[0] = CMD_REPLYTAKEBACK;
        bytes[1] = value ? (byte)1 : (byte)0;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendDrawRequest() {
        byte [] bytes = new byte[1];
        bytes[0] = CMD_REQUESTDRAW;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    public void sendDrawReply(boolean value) {
        byte [] bytes = new byte[2];
        bytes[0] = CMD_REPLYDRAW;
        bytes[1] = value ? (byte)1 : (byte)0;
        sendQueue.addElement(bytes);
        synchronized(sendQueue) {
            sendQueue.notify();
        }
    }
    
    private class ReadThread implements Runnable {
        public void run() {
            try {
                InputStream in;
                int length;
                byte[] read_buffer;
                synchronized (connection) {
                    in = connection.openInputStream();
                }
                while (isOpen) {
                    length = in.read();
                    read_buffer = new byte[length];
                    for (int i = 0; i < length; i++) 
                        read_buffer[i] = (byte)(in.read() & 0xff);
                    switch(read_buffer[0]) {
                    case CMD_MOVE: {
                            String move;
                            StringBuffer coord = new StringBuffer();
                            move = new String(read_buffer, 1, 4);
                            game.doMove(move);
                        }
                        break;
                    case CMD_PROMOTION: {
                            String move;
                            int type;
                            StringBuffer coord = new StringBuffer();
                            move = new String(read_buffer, 1, 4);
                            type = read_buffer[5];
                            game.doPromotionMove(move, type);
                        }
                        break;
                    case CMD_START:
                        game.changeColor(read_buffer[1]);
                        break;
                    case CMD_CHECKMATE:
                        game.winGame();
                        break;
                    case CMD_REQUESTTAKEBACK:
                        game.requestTakeback();
                        break;
                    case CMD_REPLYTAKEBACK:                     
                        game.replyTakeback(read_buffer[1] == 1);
                        break;
                    case CMD_REQUESTDRAW:
                        game.requestDraw();
                        break;
                    case CMD_REPLYDRAW:
                        game.replyDraw(read_buffer[1] == 1);
                        break;
                    }
                }
            }
            catch (IOException e) {
                System.out.println("Exception at read: " + e);
            }
        }
    }
    
    private class WriteThread implements Runnable {
        public void run() {
            try {
                OutputStream out;
                int length;
                byte[] bytes;
                synchronized (connection) {
                    out = connection.openOutputStream();
                }
                
                while (isOpen) {
                    synchronized(sendQueue) {
                        if (sendQueue.size() == 0) {
                            try {
                                sendQueue.wait();
                            }
                            catch (InterruptedException e) {
                                System.out.println("Protocol thread going out");
                            }
                        }
                    }
                    if (!isOpen)
                        return;
                    bytes = (byte[])sendQueue.firstElement();
                    sendQueue.removeElementAt(0);
                    out.write(bytes.length);
                    out.write(bytes);
                    out.flush();
                }
            }
            catch (IOException e) {
                System.out.println("Exception at read: " + e);
            }
        }
    }
    
}