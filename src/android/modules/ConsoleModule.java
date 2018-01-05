package ga.oshimin.cordova.module;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Iface;

public class ConsoleModule implements Iface {
	public String TAG = "Console"+JSEngine.TAG;
	private android.content.Context mContext;
    private  Handler mHandler = new Handler(Looper.getMainLooper());
	public ConsoleModule(android.content.Context ctx){
		mContext = ctx;
	}
	public ConsoleModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
		mContext = context;
	}
	public int log(String msg){
		return Log.d(TAG,msg);
	}
	public int error(String msg){
		return Log.e(TAG,msg);
	}
	public int info(String msg){
		return Log.i(TAG,msg);
	}
	public int warn(String msg){
		return Log.w(TAG,msg);
	}
}