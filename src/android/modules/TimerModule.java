package ga.oshimin.cordova.module;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Iface;

import android.util.Log;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

import java.util.HashMap;

import java.io.IOException;

import android.os.Looper;
import android.os.Handler;

public class TimerModule implements Iface {
    public String TAG = "Timer"+JSEngine.TAG;
    public int mIndex = 0;
    private HashMap<String, Runnable> mTask = new HashMap<String, Runnable>();
    private final JSEngine mEngine;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    public static interface Closure {
        public void run();
    }
    public TimerModule(){
        mEngine = null;
    }
    public TimerModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
        mEngine = engine;
    }
    public int runTask(int milliseconds,final JSEngine.ClosureFn cb){
        return runTask(milliseconds,cb,false);
    }
    public int runTask(int milliseconds,final JSEngine.ClosureFn cb,final boolean repeat){
        //if(milliseconds == null) milliseconds  = 1;
        if(milliseconds < 0 ) milliseconds = 0;
        if(milliseconds > 86400000 ) milliseconds = 86400000;// max on day
        final int timeRepeat = milliseconds; 
        //if(repeat) repeat = false;
        mTask.put(String.valueOf(mIndex),new Runnable(){
            private int myIndex = mIndex;
            @Override
            public void run() {
                try{cb.run();}catch(Exception e){e.printStackTrace();}
                if(repeat)
                    mHandler.postDelayed(this, timeRepeat); // Optional, to repeat the task.
                else
                    removeTask(myIndex); // remove renable
            }
        });
        if(milliseconds > 0)
            mHandler.postDelayed(mTask.get(String.valueOf(mIndex)),milliseconds);
        else
            mHandler.post(mTask.get(String.valueOf(mIndex)));

        return mIndex++;
    }
    
    public int runTask(final Function cb,int milliseconds){
        return runTask(cb,milliseconds,false);
    }
    public int runTask(final Function cb,int milliseconds,final boolean repeat){
        if(mEngine == null) return -1;
        //if(milliseconds == null) milliseconds  = 1;
        if(milliseconds < 0 ) milliseconds = 0;
        if(milliseconds > 86400000 ) milliseconds = 86400000;// max on day 24h
        final int timeRepeat = milliseconds; 
        //if(repeat) repeat = false;
        Log.d(TAG, "add task  "+String.valueOf(mIndex));
        mTask.put(String.valueOf(mIndex),new Runnable(){
            private int myIndex = mIndex;
            @Override
            public void run() {
                if(mTask.containsKey(String.valueOf(myIndex))){
                    Log.d(TAG, "run task  "+String.valueOf(myIndex));
                    try{mEngine.call(cb,Context.emptyArgs);}catch(Exception e){e.printStackTrace();}
                    if(repeat)
                        mHandler.postDelayed(this, timeRepeat); // Optional, to repeat the task.
                    else
                        removeTask(myIndex); // remove renable
                }
            }
        });
        if(milliseconds > 0)
            mHandler.postDelayed(mTask.get(String.valueOf(mIndex)),milliseconds);
        else
            mHandler.post(mTask.get(String.valueOf(mIndex)));

        return mIndex++;
    }
    public void removeTask(final int taskKey){
        if(taskKey > -1 && mTask.containsKey(String.valueOf(taskKey))){
            Log.d(TAG, "Clear "+taskKey);
            mHandler.removeCallbacks(mTask.get(String.valueOf(taskKey)));
            mTask.remove(String.valueOf(taskKey));
        }
    }
}