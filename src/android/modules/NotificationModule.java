package ga.oshimin.cordova.module;


import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeFunction;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Worker;
import ga.oshimin.cordova.AssetUtil;
import ga.oshimin.cordova.Iface;

import android.R;
import android.graphics.drawable.Icon;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;

import android.app.PendingIntent;
import android.app.Notification;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ga.oshimin.cordova.JSObject;

import android.content.res.Resources;
import java.util.HashMap;


public class NotificationModule implements Iface {
	public String TAG = "NotificationModule"+JSEngine.TAG;
	public String KEY = JSEngine.appID;
	private NotificationManagerCompat notificationManager;
	private int id = 1;
	private long[] vibrate = {1000,500,1000,400,1000,300,1000,200,10000,100,10000};
	private android.content.Context mContext;
	private JSEngine mEngine;
    private  Handler mHandler = new Handler(Looper.getMainLooper());
    private void init(){
    	notificationManager = NotificationManagerCompat.from(mContext);
    }
	public NotificationModule(android.content.Context context){
		Log.d(TAG,"init NotificationModule short");
		mContext = context;
		init();
	}
	// signature obligatoire pour le lazy loading (processBinding('NotificationModule'));
	public NotificationModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
		Log.d(TAG,"init NotificationModule");
		mContext = context;
		mEngine = engine;
		init();
	}
	public int notification(Object options) throws JavaScriptException{
		options = JSObject.convert(options);
		if(options instanceof JSONObject){
			return notification((JSONObject)options);
		}else if(options instanceof String){
			try{
				JSONObject obj = new JSONObject();
				obj.put("text",(String) options);
				return notification((JSONObject)options);
			}catch(JSONException e){
				Log.d(TAG,"Error on put text",e);
			}
		}
		throw new JavaScriptException("Bad Options");
	}
	public void cancel(int id){
		notificationManager.cancel(id);
	}
	public void cancelAll(){
		notificationManager.cancelAll();
	}
	public int notification(JSONObject opts) throws JavaScriptException{
		Options options = new Options(mContext,opts);
		/*Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(),
        R.drawable.sym_def_app_icon);*/

		notificationManager.notify(options.getId() != -1 ? options.getId() : id, builder(options));
		/*
		// Build the notification, setting the group appropriately
		Notification notif = new NotificationCompat.Builder(mContext)
			.setTicker(title)
			.setContentTitle(title)
			.setContentText(text)
			//.setSmallIcon(R.drawable.new_mail)
			//.setLargeIcon(R.drawable.new_mail)
			.setStyle(new Notification.BigTextStyle()
				.bigText(body))
			.setGroup(KEY)
	        .setGroupSummary(true)
	        .setAutoCancel(true)
	        .setVibrate(vibrate)
	        .setLights(Color.WHITE,500,1000)
			.build();
		notificationManager.notify(id, notif);
		*/
		Log.d(TAG,"show notification <"+(options.getId() != -1 ? options.getId() :id)+">");
		return options.getId() != -1 ? options.getId() : (id++);
	}
	private Notification builder(Options options){
		if(options.getText().equals("")) throw new JavaScriptException("no Text");
		Log.d(TAG,"new notification <"+options.getTitle()+">");
		Uri sound     = options.getSoundUri();
        int smallIcon = options.getSmallIcon();
        int ledColor  = options.getLedColor();
        String body   = options.getBigText();

        NotificationCompat.Builder builder;

        builder = new NotificationCompat.Builder(mContext)
                .setDefaults(0)
                .setContentTitle(options.getTitle())
                .setContentText(options.getText())
                .setNumber(options.getBadgeNumber())
                .setTicker(options.getText())
                .setAutoCancel(options.isAutoClear())
                .setOngoing(options.isOngoing())
                .setColor(options.getColor())
		        .setSmallIcon(options.getIcon())
		        .setLargeIcon(options.getIconBitmap());

        if (ledColor != 0) {
            builder.setLights(ledColor, 100, 100);
        }

        if (sound != null) {
            builder.setSound(sound);
        }

        if(!body.equals(""))
        	builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(body));

        if(options.isAutoClear()){
			Log.d(TAG,"Group clearable notification");
			builder = builder.setGroup(KEY)
						.setGroupSummary(true);
		}
		Log.d(TAG,"Total actions "+options.actions.size());
		for(String key:options.actions.keySet()){
			Log.d(TAG,"addAction "+key+" to notification "+id);
			//String key = options.actions.next();
			Action action = options.actions.get(key);
			builder.addAction(action.icon,action.title,action.closure);
		}

        builder = applyClickReceiver(builder,options);

		return builder.build();
	}
	public NotificationCompat.Builder applyClickReceiver(NotificationCompat.Builder builder, final Options options){
		builder.setDeleteIntent(options.closureFn("onclose",id)).setContentIntent(options.closureFn("onclick",id));
		return builder;
	}

	private class Action{
			public String title;
			public PendingIntent closure;
			public int icon;
			public Action(android.content.Context context, JSONObject options) throws Exception{
				try{
					if(options instanceof JSONObject){
			        	Options o = new Options(context,options);
			        	icon = o.getIcon("cordova_service_def_action");
			        	int id = (int) System.currentTimeMillis();
			        	closure =  o.closureFn("onclick","Action-"+id);
			        	title = !options.optString("title","").equals("") ? o.getTitle() : (options.has("icon") && !options.optString("icon","").equals("") ? options.optString("icon","") : "");
			        	Log.d(TAG,"Action title "+title);
			        	//NotificationCompat.Action act = NotificationCompat.Action.Builder(o.getIcon(),( CharSequence )title,closure).build();
			        }else
			        	throw new Exception("options is not an JSONObject");
			    }catch(Exception e){
			    	throw e;
			    }
			}
		}
	// parse notification options from JSONObject
	private class Options {

	    // Key name for bundled extras
	    static final String EXTRA = "NOTIFICATION_OPTIONS";

	    // The original JSON object
	    private JSONObject options = new JSONObject();

	    // Application context
	    private final android.content.Context context;

	    // Asset util instance
	    private final AssetUtil assets;
    	public final HashMap<String, Action> actions = new HashMap<String, Action>();
	    /**
	     * Constructor
	     *
	     * @param context
	     *      Application context
	     */
	    public Options(android.content.Context context,JSONObject options){
	    	this.context = context;
	        this.assets  = AssetUtil.getInstance(context);
	        parse(options);
	    }

	    /**
	     * Parse given JSON properties.
	     *
	     * @param options
	     *      JSON properties
	     */
	    public Options parse (JSONObject options) {
	        this.options = options;
	        parseAssets();
	        //try{
	        if(options.has("actions"))
			    Log.d(TAG,"options.actions is "+options.opt("actions").getClass().getName());
	        Object a = options.opt("actions");
	        if(a != null && a instanceof JSONArray){
	        	try{
		        	JSONArray arr = (JSONArray) a;
					final int len = (int) arr.length();
			        final JSONArray result = new JSONArray();
			        Log.d(TAG,"size of action "+String.valueOf(len));
			        int i = 0;
			        for (int key = 0; key < len; key++) {
			        	JSONObject val;
			        	try{
				        	Log.d(TAG,"getAction at "+i+"/"+len+" ; loop "+key);
				        	val = arr.optJSONObject(key);
					        Log.d(TAG,"type action : "+(val == null ? "null" : (val.getClass().getName())));
		        			actions.put(String.valueOf(key),new Action(context,val));
				        }catch(Exception e){
			    			Log.d(TAG,"Error on parse actions index "+key,e);
				        }
			        }
			    }catch(Exception e){
			    	Log.d(TAG,"Error on parse actions",e);
			    }
	        }
		    //}catch(JSONException e){
		    //	Log.d()
		    //}
	        return this;
	    }

	    /**
	     * Parse asset URIs.
	     */
	    private void parseAssets() {

	        if (options.has("iconUri") && !options.optBoolean("updated"))
	            return;

	        try {
	        	Uri iconUri  = assets.parse(options.optString("icon", "res://icon"));
	            options.put("iconUri", iconUri.toString());
	        } catch (JSONException e) {
	            Log.d(TAG,"Error on put iconUri",e);
	        }
	        try {
	        	Uri soundUri = assets.parseSound(options.optString("sound", "res://platform_default"));
	            options.put("soundUri", soundUri.toString());
	        } catch (JSONException e) {
	            Log.d(TAG,"Error on put soundUri",e);
	        }
	    }
	    public PendingIntent closureFn(final String name, final int id){
	    	return closureFn(name,String.valueOf(id));
	   	}
	    public PendingIntent closureFn(final String name, final String id){
	    	return Worker.addCmdFunction(false, name+"Fn"+id,new JSEngine.ClosureFn(){
				private String LOGTAG = name+"ClosureFn";
				@Override
				public void run(){
					Log.d(LOGTAG, "run "+name+"Fn"+id);
					Options.this.run(name);
				}
			});
	    }

	    /**
	     * Application context.
	     */
	    public android.content.Context getContext () {
	        return context;
	    }

	    /**
	     * Wrapped JSON object.
	     */
	    JSONObject getDict () {
	        return options;
	    }

	    /**
	     * Text for the local notification.
	     */
	    public String getText() {
	        return options.optString("text", "");
	    }

		/**
	     * Big Text for the local notification.
	     */
	    public String getBigText() {
	        return options.optString("body", "");
	    }
	    /**
	     * Badge number for the local notification.
	     */
	    public int getBadgeNumber() {
	        return options.optInt("badge", 0);
	    }

	    /**
	     * ongoing flag for local notifications.
	     */
	    public Boolean isOngoing() {
	    	Log.d(TAG,"ongoing : "+(options.optBoolean("ongoing", false) ? "Oui" : "Non" ));
	        return options.optBoolean("ongoing", false);
	    }

	    /**
	     * autoClear flag for local notifications.
	     */
	    public Boolean isAutoClear() {
	        return options.optBoolean("autoClear", true);
	    }

	    /**
	     * ID for the local notification as a number.
	     */
	    public Integer getId() {
	        return options.optInt("id", -1);
	    }

	    /**
	     * ID for the local notification as a string.
	     */
	    public String getIdStr() {
	        return getId().toString();
	    }

	    /**
	     * Title for the local notification.
	     */
	    public String getTitle() {
	        String title = options.optString("title", "");

	        if (title.isEmpty()) {
	            title = context.getApplicationInfo().loadLabel(
	                    context.getPackageManager()).toString();
	        }

	        return title;
	    }

	    /**
	     * @return
	     *      The notification color for LED
	     */
	    public int getLedColor() {
	        String hex = options.optString("led", null);

	        if (hex == null) {
	            return 0;
	        }

	        int aRGB = Integer.parseInt(hex, 16);

	        return aRGB + 0xFF000000;
	    }

	    /**
	     * @return
	     *      The notification background color for the small icon
	     *      Returns null, if no color is given.
	     */
	    public int getColor() {
	        String hex = options.optString("color", null);

	        if (hex == null) {
	            return NotificationCompat.COLOR_DEFAULT;
	        }

	        int aRGB = Integer.parseInt(hex, 16);

	        return aRGB + 0xFF000000;
	    }

	    /**
	     * Sound file path for the local notification.
	     */
	    public Uri getSoundUri() {
	        Uri uri = null;
	        try{
	        	if(options.optBoolean("mute",false)) return uri;
	            uri = Uri.parse(options.optString("soundUri"));
	        } catch (Exception ex){
	            try{
		            uri = Uri.parse("res://platform_default");
		        } catch (Exception e){
		            e.printStackTrace();
		        }
	        }

	        return uri;
	    }

	    /**
	     * Icon bitmap for the local notification.
	     */
	    public Bitmap getIconBitmap() {
	        Bitmap bmp;
	        try {
		        Log.d(TAG,"Uri BMP "+options.optString("iconUri"));
	            Uri uri = Uri.parse(options.optString("iconUri"));
	            bmp = assets.getIconFromUri(uri);
	        } catch (Exception e){
	        		//e.printStackTrace();
			        Log.d(TAG,"Icon BMP "+options.optString("icon"));
	        		bmp = getBitmap(options.optString("icon"));
	        }
	        Log.d(TAG,"BMP "+options.optString("icon")+String.valueOf(bmp));
	        return Bitmap.createScaledBitmap(bmp,getDensitySize(),getDensitySize(),true);
	    }

	    /**
	     * Icon resource ID for the local notification.
	     */
	    public int getIcon () {
	    	return getIcon("icon");
	    }
	    public int getIcon (String def) {
	        String icon = options.optString("icon", "");

	        int resId = assets.getResIdForDrawable(icon);

	        if (resId == 0) {
	            resId = getSmallIcon();
	        }

	        if (resId == 0) {
		        Resources res   = context.getResources();
		        String pkgName  = context.getPackageName();
		        resId = res.getIdentifier(def, "drawable", pkgName);
	        }

	        return resId;
	    }

	    /**
	     * Small icon resource ID for the local notification.
	     */
	    public int getSmallIcon () {
	        String icon = options.optString("smallIcon", "");

	        return assets.getResIdForDrawable(icon);
	    }

	    /**
	     * JSON object as string.
	     */
	    public String toString() {
	        return options.toString();
	    }

	    /*
	     * Run function
	     */
	    public void run(String fnName){
	    	if(options.has(fnName)){
	    		try{
	    			Log.d(TAG,"typeof "+fnName+" is "+options.get(fnName).getClass().getName());
	    			mEngine.call(options.get(fnName));
	    		} catch(JSONException e){
	    			Log.d(TAG,"get typeof "+fnName+" trigger an error ",e);
	    		} catch(Exception e){
	    			Log.d(TAG,"get typeof "+fnName+" trigger an error ",e);
	    		}
	    	} else
	    		Log.d(TAG,"typeof "+fnName+" is undefined");
	    }
	    private int getDensitySize() {
		    float density = context.getResources().getDisplayMetrics().density;
		    Log.d(TAG,"density is "+String.valueOf(density));
		    String size = options.optString("iconSize", "small");
		    if( size.equals("big")){
			    if (density >= 4.0)
			        return 192;
			    else if (density >= 3.0)
			        return 144;
			    else if (density >= 2.0)
			        return 96;
			    else if (density >= 1.5)
			        return 72;
			    else if (density >= 1.0)
			        return 48;
			    else return 36;
			} else {
				if (density >= 4.0)
			        return 96;
			    else if (density >= 3.0)
			        return 72;
			    else if (density >= 2.0)
			        return 48;
			    else if (density >= 1.5)
			        return 36;
			    else if (density >= 1.0)
			        return 24;
			    else return 22;
			}
		}
		private Bitmap getBitmap(String name){
			return getBitmap(name,options);
		}
		private Bitmap getBitmap(String name, JSONObject options) {
	        Bitmap bmp;
        	try {
        		bmp = BitmapFactory.decodeStream(mEngine.readFile("www/"+options.optString(name)));
		        if(bmp == null) throw new Exception("no BMP");
        	} catch(Exception ex) {
	        	try{
	        		//ex.printStackTrace();
		            bmp = assets.getIconFromDrawable(name);
	        		Log.d(TAG,"BMP "+String.valueOf(bmp));
		            if(bmp == null) throw new Exception("no BMP");
	        	} catch(Exception e) {
	        		//e.printStackTrace();
		            bmp = assets.getIconFromDrawable("icon");
	        	}
        	}
	        Log.d(TAG,"BMP "+String.valueOf(bmp));
	        return bmp;
	    }
	}
}