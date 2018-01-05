package ga.oshimin.cordova;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.mozilla.javascript.*;

import android.util.Log;


import java.util.Iterator;

public class JSObject { 
    public static String TAG = "JSObject"+JSEngine.TAG;
	public static Object convert(final Object obj) {
        if (obj instanceof NativeObject)
            return convertObject((NativeObject) obj);
        else if (obj instanceof NativeArray)
            return convertArray((NativeArray) obj);
        return obj;
    }
	public static <T> T convert(final Object obj, final Class<T> clazz) {
        return (T) Context.jsToJava(obj, clazz);
    }
    public static Object convertObject(final NativeObject obj) {
        final JSONObject result = new JSONObject();

        for (final Object id : obj.getIds()) {
            String key;
            Object value;
            if (id instanceof String) {
                key = (String) id;
                value = obj.get(key, obj);
            } else if (id instanceof Integer) {
                key = id.toString();
                value = obj.get(((Integer) id).intValue(), obj);
            } else {
                throw new IllegalArgumentException();
            }
            if (value instanceof NativeObject)
                value = convertObject((NativeObject) value);
            if (value instanceof NativeArray)
                value = convertArray((NativeArray) value);
            try{result.put(key, value);} catch(JSONException e){}
        }

        return result;
    }
    public static Object convertArray(final NativeArray arr) {
        final int len = (int) arr.getLength();
        final JSONArray result = new JSONArray();

        for (int i = 0; i < len; i++) {
            Object value = arr.get(i, null);
            if (value instanceof NativeObject)
                value = convertObject((NativeObject) value);
            if (value instanceof NativeArray)
                value = convertArray((NativeArray) value);
            result.put(value);
			
        }
        return result;
    }

    public static Object objectToJs(final Object obj,Context ctx,Scriptable scope) {
    	if (obj == null)
            return Context.getUndefinedValue();
        else if (obj instanceof String || obj instanceof Number
            || obj instanceof Boolean || obj instanceof Scriptable)
            return obj;
       	else if (obj instanceof Character)
            return String.valueOf(((Character)obj).charValue());
        else if (obj instanceof Iface)
        	return Context.javaToJS(obj,scope);
        else if (obj instanceof JSONArray)
            return objectToJsArray((JSONArray) obj,ctx,scope);
        else if (obj instanceof JSONObject)
            return objectToJsObject((JSONObject) obj,ctx,scope);
        else if(obj instanceof ConsString)
            return obj.toString();
        Log.d("objectToJs","Get class name : "+obj.getClass().getName());
        return obj;
    }

    public static Object objectToJsObject(final JSONObject obj,Context ctx, Scriptable scope) {
        final Scriptable result = ctx.newObject(scope);
        for (final Iterator<String> i = obj.keys(); i.hasNext();) {
        	final String key = i.next();
            result.put(key, result, objectToJs(obj.opt(key),ctx,scope));
        }
        return result;
    }
    public static Object objectToJsArray(final JSONArray arr,Context ctx, Scriptable scope) {
        final int len = (int) arr.length();
        final Scriptable result = ctx.newArray(scope,len);
        for (int key = 0; key < len; key++) {
        	result.put(key, result, objectToJs(arr.opt(key),ctx,scope));
        }
        return result;
    }
    public static Object normalize(Object obj){
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Number) {
            return (Number) obj;
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof JSONObject) {
            return normalizeObject((JSONObject)obj);
        } else if (obj instanceof JSONArray) {
            return normalizeArray((JSONArray)obj);
        } else {
            Log.d("Normalize","Get class name : "+obj.getClass().getName());
            return null;
        }
    }
    public static JSONObject normalizeObject(final JSONObject obj) {

        final JSONObject result = new JSONObject();
        for (final Iterator<String> i = obj.keys(); i.hasNext();) {
            final String key = i.next();
            Object value  = normalize(obj.opt(key));
            if(value != null){
                try{result.put(key, value);} catch(JSONException e){}
            }
        }
        return result;
    }
    public static JSONArray normalizeArray(final JSONArray arr) {
        final int len = (int) arr.length();
        final JSONArray result = new JSONArray();
        for (int key = 0; key < len; key++) {
            Object value = normalize(arr.opt(key));
            if(value != null){
                result.put(value);
            }        
        }
        return result;
    }
}