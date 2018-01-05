package ga.oshimin.cordova;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;

import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.Handler;
import android.os.RemoteException;

import android.util.Log;


public class CordovaService extends CordovaPlugin {
    public static final String TAG = CordovaService.class.getSimpleName();
    public static final String ACTION_POST_MESSAGE = "postMessage";
    public static final String ACTION_ON_MESSAGE = "onMessage";
    public static final String ACTION_START = "start";
    public static final String ACTION_IS_START = "isStart";
    public static final String ACTION_STOP = "stop";
    public static final String SERVICE_NAME = "ga.oshimin.cordova.Worker";
    public static final boolean AUTO_START = true;
    private CallbackContext onMessage;
    private boolean started = false;
    private Intent myService;
    protected enum ExecuteStatus {
        OK,
        ERROR,
        INVALID_ACTION
    }
    class MyResultReceiver extends ResultReceiver {
        private Handler mHandler;
        public MyResultReceiver() {
            super(null);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle bundle) {
//            if(0>resultCode) return;
            Log.d(TAG,"Data received");
            Log.d(TAG,"Code : "+resultCode);
            JSONArray args = null;
            if(bundle != null){
                try{
                    args = new JSONArray(bundle.getString("data","[]"));
                    Log.d(TAG,"Data : "+bundle.getString("data",""));
                }catch(JSONException e){
                    Log.d(TAG,"Error on parse json",e);
                }
            }
            switch(resultCode){
                case Worker.constants.POST_MESSAGE:
                    if(onMessage != null){
                        Log.d(TAG,"sendMessage to cordova");
                        if(args != null && args.length() > 0){
                            PluginResult pluginResult;
                            Object normalizedObject = JSObject.normalize(args.opt(0));
                            if(normalizedObject != null){
                                try{
                                    if (normalizedObject instanceof String) {
                                        pluginResult = new PluginResult(PluginResult.Status.OK,(String)normalizedObject);
                                    } else if (normalizedObject instanceof Float) {
                                        pluginResult = new PluginResult(PluginResult.Status.OK,(Float)normalizedObject);
                                    } else if (normalizedObject instanceof Boolean) {
                                        pluginResult = new PluginResult(PluginResult.Status.OK,(Boolean)normalizedObject);
                                    } else if (normalizedObject instanceof JSONObject) {
                                        pluginResult = new PluginResult(PluginResult.Status.OK,(JSONObject)normalizedObject);
                                    } else if (normalizedObject instanceof JSONArray) {
                                        pluginResult = new PluginResult(PluginResult.Status.OK,(JSONArray)normalizedObject);
                                    }else
                                        throw new Exception("No valid type");
                                    pluginResult.setKeepCallback(true);
                                    onMessage.sendPluginResult(pluginResult);
                                }catch(Exception e){}
                            }
                        }
                    }else{
                        Log.d(TAG,"onMessage");
                    }
                    break;
                case Worker.constants.BIND:
                    Log.d(TAG,"Worker bind succefully");
                    break;

            }
        }
    }
    @Override
    protected void pluginInitialize() {
        // Bind to LocalService
        Log.d(TAG,"pluginInitialize");
        initPref(); // start service
    }
    @Override
    public boolean execute(final String action,final JSONArray data,final CallbackContext callbackContext) throws JSONException {
        /*if(!(  action.equals(ACTION_POST_MESSAGE)
            || action.equals(ACTION_ON_MESSAGE)
            || action.equals(ACTION_STOP)
            || action.equals(ACTION_START)
            || action.equals(ACTION_IS_START)
        )) return false;
        */
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"receive "+action);
                if (action.equals(ACTION_POST_MESSAGE)) {
                    callbackContext.success();
                    sendMessage(ACTION_POST_MESSAGE,data);
                } else if (action.equals(ACTION_ON_MESSAGE)) {
                    onMessage = callbackContext;
                } else if (action.equals(ACTION_STOP)) {
                    callbackContext.success();
                } else if (action.equals(ACTION_START)) {
                    callbackContext.success();
                } else if (action.equals(ACTION_IS_START)) {
                    Log.d(TAG,started ? "Worker is started" : "Worker is not started");
                    callbackContext.success(started ? 1 : 0);
                    sendMessage("Action "+action,data);
                }
            }
        });
        return true;
    }
    private void initPref(){
        if(started) return;
        // start automatically if config serviceAutoStart is set to true
         Log.d(TAG,"initPref");
        if(myService == null){
            try{
                Log.d(TAG,"start service");
                // Create an Explicit Intent
                myService = new Intent(this.cordova.getActivity(),Class.forName(SERVICE_NAME));
                myService.putExtra("receiver", new MyResultReceiver());
                myService.putExtra("script", preferences.getString("js-service",""));
                Bundle bundle = new Bundle();
                bundle.putString("cmd", "start");
                bundle.putString("args", "[]");
                myService.putExtra("data", bundle);
                // Start the Service
                //this.cordova.getActivity().bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
                this.cordova.getActivity().startService(myService); started = true;
                Log.d(TAG,"start service DONE");
            } catch(ClassNotFoundException e){
                    Log.d(TAG,"serviceAutoStart fails",e);
            } catch (Exception ex) {
                Log.d(TAG, "serviceAutoStart failed", ex);
            }
        }else
            Log.d(TAG,"serviceAutoStart is bind");
    }

    public void sendMessage() {
        sendMessage(null,null);
    }
    public void sendMessage(String msg) {
        sendMessage(msg,null);
    }
    public void sendMessage(String msg,JSONArray args) {
        if(myService == null){
            initPref();
        }
        Log.d(TAG,"Can sendMessage "+ (started ? "yes" : "no" ));
        if (!started) return;

        Bundle bundle = new Bundle();
        bundle.putString("cmd", msg != null ? msg : "ping");
        bundle.putString("args", args != null ? args.toString() : "[]");
        myService.putExtra("data", bundle);
        try {
            Log.d(TAG,"sendMessage "+(msg != null ? msg : "ping")+"<"+(args != null ? args.toString() : "[]")+">");
            this.cordova.getActivity().startService(myService);
        } catch (Exception e) {
            Log.e(TAG,"sendMessage",e);
        }
    }

}