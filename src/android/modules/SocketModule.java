package ga.oshimin.cordova.module;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Iface;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

public class SocketModule implements Iface {
    public String TAG = "Socket"+JSEngine.TAG;
    private final JSEngine mEngine;
    public SocketModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
        mEngine = engine;
    }
    public TCPClient socket(int port,String host) {
        return new TCPClient(port,host,mEngine);
    }
    public TCPClient socket(String host,int port) {
        return new TCPClient(port,host,mEngine);
    }
    public TCPClient socket(int port) {
        return new TCPClient(port,mEngine);
    }
}