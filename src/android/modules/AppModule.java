

package ga.oshimin.cordova.module;

import android.util.Log;
import android.net.Uri;
import android.content.Intent;

import  android.os.Build;
import java.lang.reflect.Method;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.AssetUtil;
import ga.oshimin.cordova.Iface;
import java.util.HashMap;
import android.os.IBinder;


public class AppModule implements Iface {
	public String TAG = "AppModule"+JSEngine.TAG;
	private android.content.Context mContext;
    private static final HashMap<String, IBinder> mIcon = new HashMap<String, IBinder>();
    private static final HashMap<String, String> mIcon17 = new HashMap<String, String>();
    private final AssetUtil assets;
	// signature obligatoire pour le lazy loading (processBinding('ToastModule'));
	public AppModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
		mContext = context;
        assets = AssetUtil.getInstance(mContext);
	}
    public void openApp(){
        try{
            Intent dialogIntent = new Intent(mContext, Class.forName(JSEngine.appID+".MainActivity"));
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(dialogIntent);
        }catch(ClassNotFoundException e){
            Log.d(TAG,"Error on call openApp",e);
        }
    }
    public int getIcon (String icon,String def) {
        int resId = assets.getResIdForDrawable(icon);

        if (resId == 0) {
            android.content.res.Resources res   = mContext.getResources();
            String pkgName  = mContext.getPackageName();
            resId = res.getIdentifier(def, "drawable", pkgName);
        }

        return resId;
    }
    /*
    public void setVisibleStatusBarIcon(int id,boolean visible){
        try {
            Object sbservice = mContext.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method method;
            if (Build.VERSION.SDK_INT >= 17) {
                method = statusbarManager.getMethod("setIcon",String.class, Integer.class, Integer.class, String.class);
            } else {
                method = statusbarManager.getMethod("addIcon",String.class, Integer.class, Integer.class);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error on get statusbar service",e);
            
        }
        try{
            if (Build.VERSION.SDK_INT >= 17) {
                // TTY status
                if(!mIcon17.containsKey(String.valueOf(id)))
                    return ;
                mService.setIconVisibility("tty", visible);
            } else {
                showsb = statusbarManager.getMethod("expand");
            }
        }catch(Exception e){
            Log.d(TAG, "Error on set visible icon on statusbar",e);
            return -1;
        }
    }
    */
    public int addStatusBarIcon(String slot,String icon){
        int notifIcon = getIcon(icon,"cordova_service_def_action");
        if(notifIcon == 0) return -1;
        Object sbservice;
        Method method;
        Class<?> statusbarManager;
        try{
            sbservice = mContext.getSystemService("statusbar");
            statusbarManager = Class.forName("android.app.StatusBarManager");
        } catch (Exception e) {
            Log.d(TAG, "Error on get statusbar class",e);
            return -1;
            
        }
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                method = statusbarManager.getMethod("setIcon",String.class, int.class, int.class, String.class);
            } else {
                method = statusbarManager.getMethod("addIcon",String.class, int.class, int.class);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error on get statusbar service",e);
            return -1;
        }
        try{
            if (Build.VERSION.SDK_INT >= 17) {
                // TTY status
                method.invoke(sbservice,slot,notifIcon , 0, null);
                mIcon17.put(String.valueOf(mIcon17.size()),slot);
                return mIcon17.size() - 1;
            } else {
                IBinder bind = (IBinder)method.invoke(sbservice,slot,notifIcon , 0);
                mIcon.put(String.valueOf(mIcon.size()),bind);
                return mIcon.size() - 1;
            }
        }catch(Exception e){
            Log.d(TAG, "Error on add icon to statusbar",e);
            return -1;
        }
    }
	public void openStatusBar(){
		try {
            Object sbservice = mContext.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showsb;
            if (Build.VERSION.SDK_INT >= 17) {
                showsb = statusbarManager.getMethod("expandNotificationsPanel");
            } else {
                showsb = statusbarManager.getMethod("expand");
            }
            showsb.invoke(sbservice);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	public void closeStatusBar(){
		try {
            Object sbservice = mContext.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showsb;
            if (Build.VERSION.SDK_INT >= 17) {
                showsb = statusbarManager.getMethod("collapsePanels");
            } else {
                showsb = statusbarManager.getMethod("collapse");
            }

            showsb.invoke(sbservice);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	public void sendSMS(String msg){
		sendSMS(msg,null);
	}
	public void sendSMS(String msg, String to){
		 Uri smsUri = Uri.parse("smsto:"+(to != null ? to : "")); 
         Intent smsIntent = new Intent(Intent.ACTION_SENDTO, smsUri); 
         smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         smsIntent.putExtra("sms_body", msg);
         mContext.startActivity(smsIntent); 
	}

    public void dial(String to){
        if(to != null){
             Uri telUri = Uri.parse("tel:"+(to != null ? to : "")); 
             Intent telIntent = new Intent(Intent.ACTION_DIAL, telUri); 
             telIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             mContext.startActivity(telIntent); 
         }
    }
    public void web(String to){
        if(to != null){
            Uri webpage = Uri.parse(to);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             mContext.startActivity(webIntent); 
         }
    }

}