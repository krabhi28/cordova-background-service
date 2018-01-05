package ga.oshimin.cordova;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;

import android.util.Log;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;



public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "AssetUtil";
    private Intent myService;
    private Context myContext;
    private int TYPE_WIFI = 1;
    private int TYPE_MOBILE = 2;
    private int TYPE_NOT_CONNECTED = 0;

    private int getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) myContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        } 
        return TYPE_NOT_CONNECTED;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
		try{

	        if(myContext == null)
	    		myContext = context;
	    	if(myService == null)
		        myService = new Intent(context, Class.forName(CordovaService.SERVICE_NAME));
		    String status;
	       	int conn = getConnectivityStatus();
	        if (conn == TYPE_WIFI) {
	            status = "Wifi";
	        } else if (conn == TYPE_MOBILE) {
	            status = "Mobile";
	        } else if (conn == TYPE_NOT_CONNECTED) {
	            status = "offline";
	        } else {
	        	status = "unknow";
	        }
	        Bundle bundle = new Bundle();
	        myService.putExtra("network", status);
	        try {
	            Log.d(TAG,"send network Message : "+status);
	            context.startService(myService);
	        } catch (Exception e) {
	            Log.e(TAG,"sendMessage",e);
	        }
		} catch(ClassNotFoundException e){}
    }
}
