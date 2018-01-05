 package ga.oshimin.cordova;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {  
	
	/*
	 ************************************************************************************************
	 * Overriden Methods 
	 ************************************************************************************************
	 */
	@Override  
	public void onReceive(Context context, Intent intent) {
		try{
			context.startService(new Intent(context, Class.forName(CordovaService.SERVICE_NAME)));
		} catch(ClassNotFoundException e){}
	}
} 
