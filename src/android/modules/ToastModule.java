package ga.oshimin.cordova.module;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

import ga.oshimin.cordova.JSEngine;



public class ToastModule implements JSEngine.Closure {
	public String TAG = "ToastModule"+JSEngine.TAG;
	private android.content.Context mContext;
    private  Handler mHandler = new Handler(Looper.getMainLooper());
	public ToastModule(android.content.Context context){
		mContext = context;
	}
	// signature obligatoire pour le lazy loading (processBinding('ToastModule'));
	public ToastModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
		mContext = context;
	}
	public void toast(final String msg){
		Log.i(TAG,msg);
		mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext,msg,Toast.LENGTH_LONG).show();
            }
        });
	}
	@Override
    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
    	try{
	        toast(args[0].toString());
	    }catch(Exception e){
	    	Log.d(TAG,"error on Exec Toast",e);
	    }
	    return Context.getUndefinedValue();
    }
}