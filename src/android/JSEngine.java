package ga.oshimin.cordova;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;


import android.os.ResultReceiver;

import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;

import org.mozilla.javascript.*;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.util.Log;

import android.content.Intent;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import java.lang.reflect.Method;

public class JSEngine{
	public final static String TAG = JSEngine.class.getSimpleName();
	public static String appID;
	public static JSEngine instance;
	public static android.content.Context mContext;
	private static String mFile;
	private static String mRootDir = "";
	private static Scriptable mScope;
	private static Scriptable mScopePrototype;
	private static ResultReceiver mMessenger;
	private HashMap<String, InputStream> cacheFile = new HashMap<String, InputStream>();
	private HashMap<String, Object> cacheObject = new HashMap<String, Object>();
	public static interface Closure {
		public Object run(Object[] args,Scriptable scope, Scriptable thisObj);
	}
	public static interface ClosureFn {
		public void run();
	}
	private class JSFunction extends BaseFunction {
		private Closure mClosure;
		private Scriptable mThisScope;
	    public JSFunction(Closure cb) throws RuntimeException {
	    	if(cb == null) throw new RuntimeException("Runnable is not defined");
			mClosure = cb;
	    }
	    public JSFunction(Closure cb, Scriptable lScope) throws RuntimeException {
	    	if(cb == null) throw new RuntimeException("Runnable is not defined");
			mClosure = cb;
			mThisScope = lScope;
	    }

	    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
	        // implementation of my function
	        //transfor all args to JS
	        Log.d(TAG,"get arguments");
	        for(int i =0; i < args.length;i++){
	        	args[i] = JSObject.objectToJs(args[i],cx,scope);
	        	Log.d(TAG,"arg["+i+"] = "+args[i]);
	        }
	        Log.d(TAG,"Call Closure");
	        return mClosure.run(args,scope,mThisScope != null ? mThisScope : thisObj);
	    }

	    public int getArity() {
	        return 1;
	    }
	}
	public JSEngine(String fileName,android.content.Context context){
		Log.d(TAG, "init JSEngine");
		if(mScope != null){
			Log.d(TAG, "JSEngine already init");
			return;
		}
		mContext = context;
		// get the root dir
		int index = fileName.lastIndexOf("/");
		if(index != -1)
			mRootDir = fileName.substring(0,index+1);
		mFile = (String) includeFile(fileName);
		if(mFile != null){
			// start rhino here
			Log.d(TAG,mFile);
			Log.d(TAG,"Start script "+fileName);
			startScript(fileName);
		}
		String tag = "widget";
		try{
			// get config.xml
			XmlResourceParser xpp = mContext.
								getResources().
								getAssets().
								openXmlResourceParser("res/xml/config.xml");
			try{
				int eventType = xpp.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if(eventType == XmlPullParser.START_TAG && tag.equals(xpp.getName())) {
						appID = xpp.getAttributeValue(null, "id");
						Log.d(TAG,"["+(tag.equals(xpp.getName()) ? "oui" : "non")+"] "+xpp.getName()+".id = "+xpp.getAttributeValue(null, "id"));
					  	break;
					}
					eventType = xpp.next();
		        }
		    }catch(XmlPullParserException e){
		    	Log.d(TAG,"Error on parse xml",e);
		    }catch(IOException e){
		    	Log.d(TAG,"Error on parse xml",e);
		    }catch(NullPointerException e){
		    	Log.d(TAG,"Error on parse xml",e);
		    }finally{
		    	if(xpp != null)
		    		xpp.close();
		    }
		}catch(IOException e){
	    	Log.d(TAG,"Error on parse xml",e);
	    }
		Log.d(TAG,tag+".id = "+appID);
		if(instance == null)
			instance = this;
	}
	public InputStream readFile(String lFile){
		lFile = AssetUtil.normalizeFileName(lFile);
		try{
			Log.d(TAG, "Try to load from cache "+lFile);
			if(cacheFile.containsKey(lFile)){
				return cacheFile.get(lFile);
			}
		}catch(Exception e){
			Log.d(TAG, "Cache load fails",e);
			return null;
		}
		Log.d(TAG, "Try to load "+lFile);
		AssetFileDescriptor assetFd = null;
		InputStream inputStream;
		long length = -1;
		try{
			try {
				assetFd = mContext.getAssets().openFd(lFile);
				inputStream = assetFd.createInputStream();
				length = assetFd.getLength();
				Log.d(TAG,lFile+" is read");
			} catch (FileNotFoundException e) {
				// Will occur if the file is compressed.
				Log.d(TAG,lFile+" is compressed try to reload");
				inputStream = mContext.getAssets().open(lFile);
			}
			if(inputStream != null){
				Log.d(TAG,lFile+" is load");
				cacheFile.put(lFile,inputStream);
				return inputStream;
			}else{
				cacheFile.put(lFile,null);
				Log.d(TAG,lFile+" is null");
			}
		} catch (Exception e){
			inputStream = null;
			Log.d(TAG, "readFile failed : " + e.getMessage());
		}
		return null;
	}
	private String getFile(String lFile){
		InputStream stream = readFile(lFile);
		if(stream != null){
			try{
				return IOUtils.toString(stream, "UTF-8");
			}catch(IOException e){
				Log.d(TAG, "getFile failed : " + e.getMessage());
			}
		}
		return null;
	}
	public Object includeFile(String lFile){
		return includeFile(lFile,null,null);
	}
	public Object includeFile(String lFile, Scriptable replaceScope){
		return includeFile(lFile,replaceScope,null);
	}
	public Object includeFile(String lFile, String moduleFileName){
		return includeFile(lFile,null,moduleFileName);
	}
	public Object includeFile(String lFile, Scriptable replaceScope,String moduleFileName){
		if(mContext == null) return null;
		lFile = AssetUtil.normalizeFileName(lFile);
		if(cacheObject.containsKey(lFile)) return cacheObject.get(lFile);
		String file = lFile;
		boolean internalModule = false;
		int index = file.lastIndexOf("/");
		String lRootDir = "";
		if(index != -1){
			lRootDir = file.substring(0,index+1);
			file = file.substring(index+1);
		}
		Log.d(TAG,"My root dir is "+lRootDir+" my file is "+ file );
		lFile = getFile("www/"+lRootDir+file); // force to read asset
		if(lFile == null)
			lFile = getFile("www/"+lRootDir+file+".js");
		if(lFile == null && moduleFileName != null){
			lFile = getFile("www/cordovaService/"+moduleFileName+".js"); // try to  load in module directory
			internalModule = (lFile != null);
		}
		if(lFile == null){
			return null;
		}
		if(mScope == null && replaceScope == null){
			cacheObject.put(file,null); // never require worker service
			return lFile; // return content
		}
		// add Script to mScope
		Log.d(TAG,"include start");
		final Context ctx;
		try{
			Log.d(TAG,"init context");
			ctx = Context.enter();
			initContext(ctx);
			final Scriptable lScope;
			lScope = initScope(lRootDir,ctx,false);
			//lScope.setParentScope(mScope == null ? replaceScope : mScope);
			try {
				addObject("global",mScope == null ? replaceScope : mScope,lScope);
				Log.d(TAG,"execute script");
				Object res = ctx.evaluateString(lScope, "(function(){\n"+
				" 	var module = {exports:null},exports={};\n"+
				"	(function(module,exports){ "+ lFile+"\n; })(module,exports);\n"+
				"	return module.exports || exports;\n"+
				" })()",file,1,null);
				if(res != null){
					return res;
				}

			} catch(RuntimeException e){
				Log.d(TAG,"Failled to run script",e);
			}
		} finally {
			Context.exit();
		}
		return null;
	}
	public void stop(){
		mScope = null;
		mContext = null;
		mFile = null;
		mMessenger = null;

	}
	public void onMessage(ResultReceiver receiver){
		if(mMessenger == null)
			mMessenger = receiver;
		mMessenger.send(Worker.constants.BIND,null); // send bind code
	}

	public void onNetworkMessage(String type){
		if(type.equals("offline")){
			call("offline");
		}else{
			Object[] arguments = new Object[1];
	        arguments[0] = (Object) type;
			call("online",arguments);
		}
	}

	public void postMessage(String json){
		try{
			Log.d(TAG,"postMessage retreive arguments");
			JSONArray  args = new JSONArray(json);
			Log.d(TAG,"json["+ (args.length()) +"]<"+json+">");
			final int len = (int) args.length();
			Object[] arguments = new Object[len];
			for (int key = 0; key < len; key++) {
	        	arguments[key] = (Object) args.opt(key);
	        }
			Log.d(TAG,"run onMessage");
			call("onMessage",arguments);
		} catch(JSONException e){
			Log.d(TAG,"onMessage error",e);
		}
	}

	public  Object call(String f){
		return call(f,Context.emptyArgs);
	}
	public Object call(String fn, Object[] args) {
		JSONObject result = new JSONObject();
		if(fn == null || mScope == null){
			Log.d(TAG,(fn == null ? "fn" : "mScope")+" is null");
			return result;
		}
		Log.d(TAG,"try to exec "+fn);
		Object fObj = mScope.get(fn, mScope);
		if (fObj == Scriptable.NOT_FOUND) {
			return null;
		};
		Log.d(TAG, fn + " is "+(fObj instanceof Function ? "Function" : "???"));
		if (!(fObj instanceof Function)) {
			Log.d(TAG,fn+" is not a function.");
			return JSObject.convert(fObj);
		} else {
			return call((Object) fObj,args);
		}
	}
	public Object call(Function f){
		return call((Object)f,Context.emptyArgs);
	}
	public Object call(Function f,Object[] args){
		return call((Object)f,args);
	}
	public Object call(Object f){
		return call(f,Context.emptyArgs);
	}
	public Object call(Object f,Object[] args){
		/* reflexion api */
		JSONObject result = new JSONObject();
		Method method;
		try {
		  	method = f.getClass().getMethod("call", Context.class, Scriptable.class, Scriptable.class, Object[].class);
		} catch (SecurityException e) {
		  // exception handling omitted for brevity
			try{
				Log.d(TAG,"SecurityException execFn",e);
				result.put("errorType","SecurityException");
				result.put("error",e.getMessage());
			} catch(JSONException err){
				Log.d(TAG,"JSONException execFn",err);
			} finally{
				return result;
			}
		} catch (NoSuchMethodException e) {
		  // exception handling omitted for brevity
			try{
				Log.d(TAG,"NoSuchMethodException execFn",e);
				result.put("errorType","NoSuchMethodException");
				result.put("error",e.getMessage());
			} catch(JSONException err){
				Log.d(TAG,"JSONException execFn",err);
			} finally{
				return result;
			}
		}

		if(f == null || mScope == null){
			Log.d(TAG,(f == null ? "function" : "mScope")+" is null");
			return result;
		}
		Context ctx;
		try {
			ctx = Context.enter();
			initContext(ctx);
			try {
				if(mScope == null)
					Log.d(TAG, "execFn call mScope is null");
				if(f == null)
					Log.d(TAG, "execFn call f is null");
				try {
					if(args != null){
						int i = 0;
						for (Object arg : args) {
							args[i++] = JSObject.objectToJs(arg,ctx, mScope);
						}
					}
					Object res = method.invoke(f,ctx, mScope, mScope, args != null ? args : Context.emptyArgs );
					String report = "execFn result => " + (res != null ? Context.toString( res) : "null");
					Log.d(TAG,report);
					return JSObject.convert(res);
				} catch (IllegalArgumentException e) { // exception handling omitted for brevity
					try{
						Log.d(TAG,"IllegalArgumentException execFn",e);
						result.put("errorType","IllegalArgumentException");
						result.put("error",e.getMessage());
					} catch(JSONException err){
						Log.d(TAG,"JSONException execFn",err);
					}
				} catch (IllegalAccessException e) { // exception handling omitted for brevity
					try{
						Log.d(TAG,"IllegalAccessException execFn",e);
						result.put("errorType","IllegalAccessException");
						result.put("error",e.getMessage());
					} catch(JSONException err){
						Log.d(TAG,"JSONException execFn",err);
					}
				} catch (InvocationTargetException e) {
					try{
						Log.d(TAG,"InvocationTargetException execFn",e);
						result.put("errorType","InvocationTargetException");
						result.put("error",e.getMessage());
					} catch(JSONException err){
						Log.d(TAG,"JSONException execFn",err);
					}
				}
			} catch (EvaluatorException e) {
				try{
					Log.d(TAG,"EvaluatorException execFn",e);
					result.put("errorType","EvaluatorException");
					result.put("error",e.getMessage());
				} catch(JSONException err){
					Log.d(TAG,"JSONException execFn",err);
				}
			} catch(RuntimeException errExec){
				try{
					Log.d(TAG,"RuntimeException execFn",errExec);
					result.put("errorType","RuntimeException");
					result.put("error",errExec.getMessage());
				} catch(JSONException err){
					Log.d(TAG,"JSONException execFn",err);
				}
			}
		}finally{
			Context.exit();
			ctx = null;
		}
		return result;
	}
	protected void addObject(String objName, Object obj,Scriptable lScope){
		Context ctx = Context.enter();
		try {
			initContext(ctx);	
			ScriptableObject.putProperty(lScope, objName, Context.javaToJS(obj, lScope));
		} finally {
			Context.exit();
			ctx = null;
		}
	}
	protected void addFunction(String objName, Closure obj,Scriptable lScope){
		Context ctx = Context.enter();
		try {
			initContext(ctx);
			ScriptableObject.putProperty(lScope, objName, new JSFunction(obj));
		} finally {
			Context.exit();
			ctx = null;
		}
	}
	private static void initContext(Context ctx){
		ctx.setOptimizationLevel(-1);
		if(ctx.isValidLanguageVersion(ctx.VERSION_ES6))
			ctx.setLanguageVersion(ctx.VERSION_ES6);
		else if(ctx.isValidLanguageVersion(ctx.VERSION_1_8))
			ctx.setLanguageVersion(ctx.VERSION_1_8);
		else
			ctx.setLanguageVersion(ctx.VERSION_DEFAULT);
	}

	protected Scriptable initScope(final String parent,final Context ctx,boolean global){
		final Scriptable lScope = ctx.initSafeStandardObjects(null,true);
		if(mScopePrototype == null){
			mScopePrototype = ctx.initSafeStandardObjects(null,true);
			try{
				Log.d(TAG,parent+" is a internalModule set processBinding");
				addFunction("processBinding",new Closure(){
					@Override
				    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
				    	// set object to JSONArray
				    	if(args.length < 1) {
				    		throw new EvaluatorException("processBinding need class name");
				    	}
				    	if(!(args[0] instanceof String)){
				    		throw new EvaluatorException("Class name must be a string");
				    	}
				    	try{
							Class t = Class.forName("ga.oshimin.cordova.module."+args[0].toString());
			    			Constructor c = t.getConstructor(android.content.Context.class,Context.class,Scriptable.class,JSEngine.class);
			    			Object o = c.newInstance(mContext,ctx,mScopePrototype,JSEngine.this);

			    			if(o instanceof Iface){
			    				return Context.javaToJS(o, mScopePrototype);
			    			} else if(o instanceof Closure){
			    				try{
			    				// load function
									return new JSFunction((Closure)o,mScopePrototype);
				    			}catch(RuntimeException e){
				    				Log.e(TAG,"processBinding Error",e);
				    				throw new EvaluatorException(e.getMessage());
				    			}
			    			} else {
			    				Log.e(TAG,"Class "+args[0]+" not loadable");
				    			throw new EvaluatorException("Class "+args[0]+" not loadable");
			    			}
			    		}catch(ClassNotFoundException e){
			    			Log.e(TAG,"processBinding Error",e);
				    		throw new EvaluatorException(e.getMessage());
			    		} catch(NoSuchMethodException e){
			    			Log.e(TAG,"processBinding Error",e);
				    		throw new EvaluatorException(e.getMessage());
			    		} catch(InstantiationException e){
							Log.e(TAG,"processBinding Error",e);
				    		throw new EvaluatorException(e.getMessage());
			    		}catch(IllegalArgumentException e){
			    			Log.e(TAG,"processBinding Error",e);
				    		throw new EvaluatorException(e.getMessage());
			    		}catch(IllegalAccessException e){
			    			Log.e(TAG,"processBinding Error",e);
				    		throw new EvaluatorException(e.getMessage());
			    		}catch(InvocationTargetException e){
			    			Log.e(TAG,"processBinding Error",e);
				    		throw new EvaluatorException(e.getMessage());
			    		}
				    }
				},mScopePrototype);
			}catch(RuntimeException e){
		    	Log.d(TAG,"error on add processBinding",e);
			}
			ctx.evaluateString(mScopePrototype ,
				"(function(root){// init global object\n"+
					"Object.defineProperty(root, 'isAndroid', {\n"+
					"	enumerable: false,\n"+
					"	configurable: false,\n"+
					"	writable: false,\n"+
					"	value: true\n"+
					"});\n"+
					"Object.defineProperty(root, 'isWindows', {\n"+
					"	enumerable: false,\n"+
					"	configurable: false,\n"+
					"	writable: false,\n"+
					"	value: false\n"+
					"});\n"+
					"Object.defineProperty(root, 'isIos', {\n"+
					"	enumerable: false,\n"+
					"	configurable: false,\n"+
					"	writable: false,\n"+
					"	value: false\n"+
					"});\n"+
					"Object.defineProperty(root, 'hasE4x', {\n"+
					"	enumerable: false,\n"+
					"	configurable: false,\n"+
					"	writable: false,\n"+
					"	value: "+(ctx.hasFeature(ctx.FEATURE_E4X) ? "true" : "false")+"\n"+
					"});\n"+
					"var Timer = processBinding('TimerModule');\n"+
					"Timer.TAG = 'WorkerTimer';\n"+
					"root.setTimeout = function(fn,time){\n"+
					"	if(typeof fn == 'function' && typeof time == 'number')\n"+
					"		return Timer.runTask(fn,0+time,false);\n"+
					"}\n"+
					"root.setImmediate = function(fn){\n"+
					"	if(typeof fn == 'function')\n"+
					"		return Timer.runTask(fn,0,false);\n"+
					"}\n"+
					"root.setInterval = function(fn,time){\n"+
					"	if(typeof fn == 'function' && typeof time == 'number')\n"+
					"		return Timer.runTask(fn,0+time,true);\n"+
					"}\n"+
					"root.clearImmediate = root.clearTimeout = root.clearInterval = function(timerVariable){\n"+
					"	if(typeof timerVariable == 'number')\n"+
					"		Timer.removeTask(timerVariable);\n"+
					"}\n"+
				"})(this)", "<lazyLoad>", 0, null);
			try{
				addFunction("postMessage",new Closure(){
					@Override
				    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
				    	// set object to JSONArray
				    	if(args.length < 1 || mMessenger == null) return Context.getUndefinedValue();
				    	JSONArray arguments = new JSONArray();
				    	arguments.put(args[0]);
				    	Bundle data = new Bundle();
				    	data.putString("data",arguments.toString());
						mMessenger.send(Worker.constants.POST_MESSAGE,data); // send bind code
					    return Context.getUndefinedValue();
				    }
				},mScopePrototype);
			}catch(RuntimeException e){
		    	Log.d(TAG,"error on add postMessage",e);
			}
			//addObject("Timer",new TimerModule(this),mScopePrototype);

			try{
				addFunction("openApp",new Closure(){
					@Override
				    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
				    	Log.d(TAG,"widget.id = "+JSEngine.appID);
				    	if(JSEngine.appID == null)  return Context.getUndefinedValue();
				    	try{
							Intent dialogIntent = new Intent(mContext, Class.forName(JSEngine.appID+".MainActivity"));
							dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							mContext.startActivity(dialogIntent);
						}catch(ClassNotFoundException e){
							Log.d(TAG,"Error on call openApp",e);
						}
						return Context.getUndefinedValue();
					}
				},mScopePrototype);
			}catch(RuntimeException e){
				Log.d(TAG,"error on openApp",e);
			}

			try{
				addFunction("openApp",new Closure(){
					@Override
				    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
				    	Log.d(TAG,"widget.id = "+JSEngine.appID);
				    	if(JSEngine.appID == null)  return Context.getUndefinedValue();
				    	try{
							Intent dialogIntent = new Intent(mContext, Class.forName(JSEngine.appID+".MainActivity"));
							dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							mContext.startActivity(dialogIntent);
						}catch(ClassNotFoundException e){
							Log.d(TAG,"Error on call openApp",e);
						}
						return Context.getUndefinedValue();
					}
				},mScopePrototype);
			}catch(RuntimeException e){
				Log.d(TAG,"error on openApp",e);
			}

			try{
				addFunction("openApp",new Closure(){
					@Override
				    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
				    	Log.d(TAG,"widget.id = "+JSEngine.appID);
				    	if(JSEngine.appID == null)  return Context.getUndefinedValue();
				    	try{
							Intent dialogIntent = new Intent(mContext, Class.forName(JSEngine.appID+".MainActivity"));
							dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							mContext.startActivity(dialogIntent);
						}catch(ClassNotFoundException e){
							Log.d(TAG,"Error on call openApp",e);
						}
						return Context.getUndefinedValue();
					}
				},mScopePrototype);
			}catch(RuntimeException e){
				Log.d(TAG,"error on openApp",e);
			}
		}
		lScope.setPrototype(mScopePrototype);
		lScope.setParentScope(null);
		if(global){
			ctx.evaluateString(lScope ,
				"(function(root){// init global object\n"+
					"root.configuration={};\n"+
					"Object.defineProperty(root, 'global', {\n"+
					"	enumerable: false,\n"+
					"	configurable: false,\n"+
					"	get: function(){ return root;}\n"+
					"});\n"+
				"})(this)", "<lazyLoad>", 0, null);
		}
		try{
			addFunction("require",new Closure(){
				@Override
			    public Object run(Object[] args,Scriptable scope, Scriptable thisObj) {
			    	if(args.length < 1) throw new EvaluatorException("require need module name");
			    	if(!(args[0] instanceof String)) throw new EvaluatorException("Module name must be a string");
			    	String lFile = (String)args[0];
			    	Object res;
			    	String moduleFileName = lFile;
			    	lFile = AssetUtil.normalizeFileName(parent+"/"+lFile);
			    	Log.d(TAG,"require('"+lFile+"')");

			    	if(cacheObject.containsKey(lFile)){
			    		Log.d(TAG,"require('"+lFile+"') is in cache");
			    		res = cacheObject.get(lFile);
			    	} else {
			    		Log.d(TAG,"require('"+lFile+"') isn't in cache");
				    	res = includeFile(lFile,moduleFileName);
				    	cacheObject.put(lFile,res);
			    	}
			    	if(res == null){
			    		throw new EvaluatorException("Module '"+moduleFileName+"' not found");
			    	}else{
			    		return res;
			    	}
			    }
			},lScope);
		}catch(RuntimeException e){
	    	Log.d(TAG,"error on add processBinding",e);
		}
		return lScope;
	}

	protected void startScript(String fileName){
		if(mFile == null ){
			Log.d(TAG,"Cant read script");
			return;
		}
		if(mScope != null ){
			Log.d(TAG,"script already started");
			return;
		}
		Log.d(TAG,"script start");
		Context ctx = Context.enter();;
		try{
			Log.d(TAG,"init context");
			initContext(ctx);
			Log.d(TAG,"init scope");
			mScope =  initScope(mRootDir, ctx,true);
			// load service.js
			try {
				Log.d(TAG,"execute script");
				ctx.evaluateString(mScope, mFile,"<"+fileName+">",1,null);
			} catch(RuntimeException e){
				Log.d(TAG,"Failled to run script",e);
				mScope = null;
			}
		} finally {
			if(mScope != null)
				Log.d(TAG,"script started");
			Context.exit();
			ctx = null;
		}
	};
}