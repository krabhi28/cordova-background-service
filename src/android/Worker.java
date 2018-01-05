package ga.oshimin.cordova;


import android.app.Service;
import android.app.PendingIntent;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.Process;
import android.util.Log;

import java.util.HashMap;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class Worker extends Service{
    public static Worker instance;
    public static String VERSION = "0.1.1";
    public static final String TAG = Worker.class.getSimpleName();
    public static class constants {
        public  static final String KEY_SCRIPT = Worker.class.getSimpleName()+"script";
        public  static final int BIND = 1;
        public  static final int POST_MESSAGE = 2;
        public  static final int NETWORK_MESSAGE = 3;
    };
    private static final HashMap<String, JSEngine.ClosureFn> cmdFunction = new HashMap<String, JSEngine.ClosureFn>();
    private ResultReceiver mResultReceiver;
    private class MyMessenger extends ResultReceiver {
        private Handler mHandler;
        public MyMessenger() {
            super(null);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle bundle) {
            switch(resultCode){
                case constants.BIND:
                    Log.d(TAG,"JSEngine binded");
                    break;
                case constants.POST_MESSAGE:
                    Log.d(TAG, "receive Message from worker");
                    if(mResultReceiver != null){
                        mResultReceiver.send(constants.POST_MESSAGE,bundle);
                    }
                    break;
                default:
                    Log.d(TAG,"Data received from JSEngine <" + resultCode+ ">");
                    if(bundle != null){
                        Log.d(TAG,"Data : "+bundle.getString("data"));
                    }
            }
        }
    }
    
    private MyMessenger mMessenger = new MyMessenger();

    private int mExec = 0;
    private  Handler mHandler = new Handler(Looper.getMainLooper());
    private Looper mServiceLooper;
    private JSEngine mEngine;
    private String mScript;
    public Worker() {
        super();
        Log.d(TAG,"Start Worker");
        instance = this;
    }

    @Override
    public IBinder onBind(Intent arg) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate\nMyService Created");
        // try to exec worker script
        mScript = PreferenceManager.getDefaultSharedPreferences(this).getString(constants.KEY_SCRIPT, "");
        Log.d(TAG,"Script : "+mScript);
        if(mScript != ""){
            Log.d(TAG,"Run Script");
            mEngine = new JSEngine(mScript,this);
            mEngine.onMessage(mMessenger);
        }else{
            Log.d(TAG,"Script is empty");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand executed, FLAGS <"+ flags +">, startId <"+ startId +">, Started <"+(mEngine != null ? "oui" : "non")+">");
        try{ // try to handle internal request

            if(intent.hasExtra("network")){
                Log.d(TAG, "receive message from network");
                if(mEngine != null)
                    mEngine.onNetworkMessage(intent.getStringExtra("network"));
                return Service.START_STICKY;
            }
            String key = intent.getStringExtra("from");
            Log.d(TAG,"CMD from me ?");
            if(key != null && key.equals(TAG)){
                key = intent.getStringExtra("functionKey");
                Log.d(TAG,"CMD key is "+key);
                JSEngine.ClosureFn fn = cmdFunction.get(key);
                if(fn != null){
                    try{
                        fn.run();
                    }catch (Exception e) {
                        Log.d(TAG,"Error on exec cmdFunction <"+key+">",e);            
                    }
                    if(intent.getBooleanExtra("autoRemove",true))
                        cmdFunction.remove(key);
                    return Service.START_STICKY;
                }
            }else
                Log.d(TAG,"not from me");
        } catch(NullPointerException e){
            Log.d(TAG,"Error on get cmdFunction",e);
        } catch(Exception e){
            Log.d(TAG,"Error on get cmdFunction",e);
        }
        try{
            setResultReceiver(intent);
            if(mResultReceiver != null){
                final Bundle data = intent.getParcelableExtra("data");
                Log.d(TAG,"cmd "+data.getString("cmd",""));
                Log.d(TAG,"args "+data.getString("args","[]"));

                if(CordovaService.ACTION_POST_MESSAGE.equals(data.getString("cmd",""))){
                    if(mEngine != null){
                        Log.d(TAG,"Send postMessage");
                        mEngine.postMessage(data.getString("args","[]"));
                    } else {
                        Log.d(TAG,"can't send postMessage mEngine is null");
                    }
                }
            } else {
                Log.d(TAG,"can't send postMessage mResultReceiver is null");
            }
        }catch(NullPointerException e){
            Log.d(TAG,"Error onStartCommand",e);
        }
        return Service.START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    private void setResultReceiver(Intent intent){
        if(mResultReceiver == null ){ // init receiver and de JSEngine
           try{
                mResultReceiver = intent.getParcelableExtra("receiver");
                String script;
                Log.d(TAG,"try to get script filename");
                script = intent.getStringExtra("script");
                Log.d(TAG,"Script <"+(mEngine != null ? "" : "no" )+" Started, " + script + ", " + (mScript !=  null ? mScript : "") +">");
                if((mEngine != null && !mScript.equals(script)) || mEngine == null){
                    if(mEngine != null)
                        mEngine.stop();
                    Log.d(TAG,"try to register script filename");
                    // save script name in preference
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    editor.putString(constants.KEY_SCRIPT, script);
                    Log.d(TAG,"register commit");
                    editor.commit(); // Very important
                    Log.d(TAG,"register done");
                    Log.d(TAG,"try to run script");
                    mEngine = new JSEngine(script,this);
                    mEngine.onMessage(mMessenger);
                    Log.d(TAG,"run script done");
                }
                mResultReceiver.send(Worker.constants.BIND,null); // send bind
            }catch(NullPointerException e){
                Log.d(TAG,"Error on get script",e);
            }
        }
    }
    public static PendingIntent addCmdFunction(boolean remove,String key, JSEngine.ClosureFn fn){
        Intent intent = new Intent(Worker.instance, Worker.class);
        cmdFunction.put(key,fn);
        intent.putExtra("from", TAG);
        intent.putExtra("functionKey",key);
        intent.putExtra("autoRemove",remove);
        return PendingIntent.getService(Worker.instance, (int) System.currentTimeMillis() , intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }
    public static void removeCmdFunction(String key){
        cmdFunction.remove(key);
    }
}