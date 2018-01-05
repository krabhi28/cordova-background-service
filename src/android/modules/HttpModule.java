package ga.oshimin.cordova.module;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Iface;

import android.util.Log;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.HttpUrl;
import okhttp3.Headers;
import okhttp3.Callback;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Cookie;
import okhttp3.CookieJar;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import java.net.URISyntaxException;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

public class HttpModule implements Iface {
    public String TAG = "Http"+JSEngine.TAG;
    public String USER_AGENT = "Cordova Worker Service "+ga.oshimin.cordova.Worker.VERSION+"/OkHttpClient";
    private final JSEngine mEngine;
    private final OkHttpClient client;
    private final String mCookiesFile;
    private final HashMap<String, List<Cookie>> cookieStore;
    private final TimerModule timer = new TimerModule();
    private int lastTimer = -1;
    public HttpModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
        mEngine = engine;
        File cacheDir = context.getExternalCacheDir();
        if(cacheDir != null){
            mCookiesFile = cacheDir.toString()+"/cookies.jar";
            touch(cacheDir.toString(),"cookies.jar");
        } else
            mCookiesFile = null;

        cookieStore = loadCookieStore();
        client = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                    saveCookieStore();
                }
                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    boolean rm = false;
                    if(cookies != null){
                        for (Cookie cookie : cookies) {
                            if(System.currentTimeMillis() >= cookie.expiresAt()){
                                cookies.remove(cookie);
                                rm = true;
                            }
                        }
                        if(rm) saveCookieStore();
                        return cookies;
                    }
                    return new ArrayList<Cookie>();
                }
            })
            .build();
    }
    void touch(String dir,String filename) {
        try
        {
            File file = new File(dir,filename);
            if (!file.exists())
                new FileOutputStream(file).close();
            file.setLastModified(System.currentTimeMillis());
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    private void saveCookieStore(){
        timer.removeTask(lastTimer);
        lastTimer = timer.runTask(100,new JSEngine.ClosureFn(){
            @Override
            public void run() {
                ObjectOutputStream oos = null;
                try{
                    FileOutputStream fout = new FileOutputStream(mCookiesFile);
                    oos = new ObjectOutputStream(fout);
                    HashMap<String, List<String>> tmp = new HashMap<String, List<String>>();
                    for (String key : cookieStore.keySet()) {
                        try{
                            List<Cookie> cookies = cookieStore.get(key);
                            List<String> cookiesJSON = new ArrayList<String>();
                            for (Cookie cookie : cookies) {
                                if(System.currentTimeMillis() < cookie.expiresAt()){
                                    JSONObject cookieJSON = new JSONObject();
                                    cookieJSON.put("expiresAt",cookie.expiresAt());
                                    cookieJSON.put("value",cookie.value());
                                    cookieJSON.put("name",cookie.name());
                                    cookieJSON.put("domain",cookie.domain());
                                    cookieJSON.put("path",cookie.path());
                                    cookieJSON.put("httpOnly",cookie.httpOnly());
                                    cookieJSON.put("secure",cookie.secure());
                                    cookiesJSON.add(cookieJSON.toString());
                                }
                            }
                            tmp.put(key,cookiesJSON);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    oos.writeObject(tmp);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(oos  != null){
                        try{oos.close();}catch(IOException e){e.printStackTrace();}
                    } 
                }
            }
        });
    }

    private HashMap<String, List<Cookie>> loadCookieStore(){
        ObjectInputStream objectinputstream = null;
        HashMap<String, List<Cookie>> tmp = new HashMap<String, List<Cookie>>();
        try {
            FileInputStream streamIn = new FileInputStream(mCookiesFile);
            objectinputstream = new ObjectInputStream(streamIn);
            HashMap<String, List<String>> cookieJSONStore = (HashMap<String, List<String>>) objectinputstream.readObject();
            for (String key : cookieJSONStore.keySet()) {
                try{
                    List<String> cookiesJSON = cookieJSONStore.get(key);
                    List<Cookie> cookies = new ArrayList<Cookie>();;
                    for (String cookieJSONData : cookiesJSON) {
                        JSONObject cookieJSON = new JSONObject(cookieJSONData);
                        if(System.currentTimeMillis() < cookieJSON.optLong("expiresAt")) {
                            Cookie.Builder cookie = new Cookie.Builder();
                            cookie.expiresAt(cookieJSON.optLong("expiresAt"));
                            cookie.value(cookieJSON.optString("value"));
                            cookie.name(cookieJSON.optString("name"));
                            cookie.domain(cookieJSON.optString("domain"));
                            cookie.path(cookieJSON.optString("path"));
                            if(cookieJSON.optBoolean("httpOnly"))
                                cookie.httpOnly();
                            if(cookieJSON.optBoolean("secure"))
                                cookie.secure();
                            cookies.add(cookie.build());
                        }
                    }
                    tmp.put(key,cookies);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(objectinputstream != null){
                try{objectinputstream.close();}catch(IOException e){e.printStackTrace();}
            }
            return tmp;
        }
    }
    public void get(String url,final Function cb, Object lHeaders){
        try{
            Request.Builder request = new Request.Builder().url(url);
            request.addHeader("user-agent",USER_AGENT);
            JSONObject headers = (JSONObject) ga.oshimin.cordova.JSObject.convert(lHeaders);
            for (final Iterator<String> i = headers.keys(); i.hasNext();) {
                final String key = i.next();
                try{
                   request.addHeader(key,String.valueOf(headers.opt(key)));
               }catch(Exception e){}
            }

            client.newCall(request.build()).enqueue(new Callback() {
              @Override public void onFailure(Call call, IOException e) {
                Log.d(TAG,"onFailure "+e.getMessage());
                Object[] args = {e.getMessage()};
                mEngine.call(cb,args);
              }
              @Override public void onResponse(Call call, Response response) {
                final JSONObject resp = new JSONObject();
                final JSONObject headers = new JSONObject();
                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    try{
                        headers.put(responseHeaders.name(i).toLowerCase(),responseHeaders.value(i));
                    }catch(Exception e){}
                }
                try{
                    resp.put("headers",headers);
                }catch(Exception e){}
                try{
                    resp.put("body",response.body().string());
                }catch(Exception e){}
                try{
                    resp.put("status",response.code());
                }catch(Exception e){}
                try{
                    resp.put("message",response.message());
                }catch(Exception e){}
                try{
                    resp.put("success",response.isSuccessful());
                }catch(Exception e){}
                try{
                    resp.put("method",response.request().method() );
                }catch(Exception e){}

                Object[] args = {null,resp};
                mEngine.call(cb,args);
              }
            });
        }catch(Exception e){
            Log.d(TAG,"Error on http request",e);
            Object[] args = {e.getMessage()};
            mEngine.call(cb,args);
        }
    }
    public void post(String method,String url,String data,String type,final Function cb,Object lHeaders){
        try{
            Request.Builder request = new Request.Builder()
                .url(url)
                .method(method,RequestBody.create(MediaType.parse(type), data));
            //request.addHeader("user-agent",USER_AGENT);
            JSONObject headers = (JSONObject) ga.oshimin.cordova.JSObject.convert(lHeaders);
            for (final Iterator<String> i = headers.keys(); i.hasNext();) {
                final String key = i.next();
                try{
                   request.addHeader(key,String.valueOf(headers.opt(key)));
               }catch(Exception e){}
            }
            client.newCall(request.build()).enqueue(new Callback() {
              @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                //Object[] args = {e.getMessage()};
                //mEngine.call(cb,args);
              }

              @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                final JSONObject resp = new JSONObject();
                final JSONObject headers = new JSONObject();
                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    try{
                        headers.put(responseHeaders.name(i).toLowerCase(),responseHeaders.value(i));
                    }catch(Exception e){}
                }
                try{
                    resp.put("headers",headers);
                }catch(Exception e){}
                try{
                    resp.put("body",response.body().string());
                }catch(Exception e){}
                try{
                    resp.put("status",response.code());
                }catch(Exception e){}
                try{
                    resp.put("message",response.message());
                }catch(Exception e){}
                try{
                    resp.put("success",response.isSuccessful());
                }catch(Exception e){}
                try{
                    resp.put("method",response.request().method() );
                }catch(Exception e){}

                Object[] args = {null,resp};
                mEngine.call(cb,args);
              }
            });
        }catch(Exception e){
            Log.d(TAG,"Error on http request",e);
            Object[] args = {e.getMessage()};
            mEngine.call(cb,args);
        }
    }
    public SocketIOClient io(String url) throws URISyntaxException{
        return new SocketIOClient(url,mEngine);
    }
    public SSEClient sse(String url) throws URISyntaxException{
        return new SSEClient(url,mEngine);
    }
}