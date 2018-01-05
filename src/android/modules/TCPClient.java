package ga.oshimin.cordova.module;

import org.mozilla.javascript.Function;
import ga.oshimin.cordova.JSEngine;
import java.lang.Thread;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;

import java.net.InetAddress;
import java.net.Socket;

import android.util.Log;

import org.json.JSONObject;

public class TCPClient {
    public String TAG = "TCPClient";

    private String myServer = null; //server IP address
    private int myPort = 0;
    // message to send to the server
    private String mServerMessage;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;
    private JSEngine mEngine;
    private Thread myThread;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */

    public TCPClient(int port) {
        this(port,"127.0.0.1",JSEngine.instance);
    }
    public TCPClient(int port,JSEngine engine) {
        this(port,"127.0.0.1",engine);
    }
    public TCPClient(int port, String host) {
        this(port,host,JSEngine.instance);
    }
    public TCPClient(int port, String host,JSEngine engine) {
        mEngine = engine;
        myServer = host;
        myPort = port;
    }
    public boolean isClosed(){
        return !mRun;
    }
    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message) {
        if (mBufferOut != null && !mBufferOut.checkError()) {
            mBufferOut.println(message);
            mBufferOut.flush();
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
        if(!myThread.interrupted())
            myThread.interrupt();
    }

    public void run(final Function onConnect,final Function onMessage,final Function onError) {
        mRun = true;
        if(myThread != null && !myThread.interrupted())
            myThread.interrupt();

        myThread = new Thread() {
            @Override
            public void run() {
                try {
                    //here you must put your computer's IP address.
                    InetAddress serverAddr = InetAddress.getByName(myServer);
                    Log.e(TAG,"TCP Client - Connecting...");
                    //create a socket to make the connection with the server
                    Socket socket = new Socket(serverAddr, myPort);
                    Log.e(TAG,"TCP Client - Connected.");
                    mEngine.call(onConnect,new Object[0]);                    
                    try {
                        //sends the message to the server
                        mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                        //receives the message which the server sends back
                        mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //in this while the client listens for the messages sent by the server
                        while (mRun) {
                            mServerMessage = mBufferIn.readLine();
                            if(socket.isClosed() || mServerMessage == null){
                                stopClient();
                                break;
                            }
                            Object[] args = {mServerMessage};
                            Log.e(TAG,"CALL onMessage for '" + mServerMessage + "'");
                            mEngine.call(onMessage,args);
                        }
                        Log.e(TAG,"RESPONSE FROM SERVER Received Message: '" + mServerMessage + "'");
                        stopClient();
                    } catch (Exception e) {
                        Log.e(TAG,"TCP Error", e);
                        Object[] args = {e.getMessage()};
                        mEngine.call(onError,args);
                        stopClient();
                    } finally {
                        //the socket must be closed. It is not possible to reconnect to this socket
                        // after it is closed, which means a new socket instance has to be created.
                        socket.close();
                        stopClient();
                    }
                } catch (Exception e) {
                    Log.e(TAG,"TCP Error", e);
                    Object[] args = {e.getMessage()};
                    mEngine.call(onError,args);
                    stopClient();
                }
            }
        };
        myThread.start();
    }
}