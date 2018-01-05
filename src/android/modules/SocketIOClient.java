package ga.oshimin.cordova.module;

import java.net.URISyntaxException;
import java.util.HashMap;

import io.socket.client.Ack;
import io.socket.emitter.Emitter;
import io.socket.client.IO;
import io.socket.client.Socket;

import org.mozilla.javascript.Function;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Iface;
import org.mozilla.javascript.Wrapper;

public class SocketIOClient implements Wrapper,Iface {
    private final HashMap<String, Function> cookieStore = new HashMap<String, Function>();
    private Socket mSocket;
    private JSEngine mEngine;
    public SocketIOClient(String url) throws URISyntaxException{
    	mSocket = IO.socket(url);
    	mEngine = JSEngine.instance;
    }
    public SocketIOClient(String url,JSEngine engine) throws URISyntaxException{
        mSocket = IO.socket(url);
        mEngine  = engine;
    }
    public void connect(){
        mSocket.connect();
    }
    public void connect(final Function cb){
 		mSocket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
          @Override
          public void call(Object... args) {
            mEngine.call(cb,args);
          }
        });
        mSocket.connect();
    }
    public void disconnect(final Function cb){
 		mSocket.once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
          @Override
          public void call(Object... args) {
            mEngine.call(cb,args);
          }
        });
        mSocket.disconnect();
    }
    public void disconnect(){
        mSocket.disconnect();
    }
    
    public void on(String event,final Function cb){
        mSocket.on(event, new Emitter.Listener() {
          @Override
          public void call(Object... args) {
            mEngine.call(cb,args);
          }
        });
    }
    public void once(String event,final Function cb){
        mSocket.once(event, new Emitter.Listener() {
          @Override
          public void call(Object... args) {
            mEngine.call(cb,args);
          }
        });
    }
    public void emit(String event,Object... args){
    	if(args[args.length-1] instanceof Function){
        	final Function cb = (Function) args[args.length-1];
        	Object[] newArgs = new Object[args.length - 1];
        	System.arraycopy(args, 0, newArgs, 0, args.length - 1);
        	mSocket.emit(event,newArgs, new Ack() {
			  @Override
			  public void call(Object... args) {
			  	mEngine.call(cb,args);
			  }
			});
    	} else {
	        mSocket.emit(event,args);
    	}
    }
    public void onConnect(Function cb){
        on(Socket.EVENT_CONNECT,cb);
    }
    public void onDisconnect(Function cb){
        on(Socket.EVENT_DISCONNECT,cb);

    }
    public String toString(){
        return String.valueOf(mSocket);
    }
    public String toJSON(){
        return toString();
    }
    @Override
    public Object unwrap(){
    	return (Object) mSocket;
    }
}