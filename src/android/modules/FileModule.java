package ga.oshimin.cordova.module;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.AssetUtil;
import ga.oshimin.cordova.Iface;

import org.apache.commons.io.FileUtils;



public class FileModule implements Iface {
	public String TAG = "File"+JSEngine.TAG;
	private android.content.Context mContext;
	public FileModule(android.content.Context context){
		mContext = context;
	}
	public FileModule(android.content.Context context,Context ctx,Scriptable scope,JSEngine engine){
		mContext = context;
	}

	public String read(String file) throws Exception,FileNotFoundException{
		return readFile(file);
	}
	public void write(String file,String data) throws Exception,FileNotFoundException{
		writeFile(file,data);
	}
	private File getFile(String name) throws Exception,FileNotFoundException {
        try{
            File cacheDir = mContext.getExternalCacheDir();
            String sep  = "/";
            int  startStr = 0;
            String directory  = null;
            if (cacheDir == null) {
                Log.e(TAG, "Missing external cache dir");
                throw new FileNotFoundException("Missing external cache dir");
            }
            String cacheDirPath = cacheDir.toString();
            name = AssetUtil.normalizeFileName(name);
            if(name.indexOf(sep) != startStr)
                name = sep + name;
            
            if(name.lastIndexOf(sep) > startStr )
                directory = name.substring(startStr,name.lastIndexOf(sep));

            if(directory != null){
                String[] dirs = directory.substring(sep.length()).split(sep);
                new File(cacheDirPath).mkdir();

                for (String dir : dirs) {
                    cacheDirPath += sep + dir;
                    boolean isCreated = new File(cacheDirPath).mkdir();
                    Log.d(TAG,"Create dir "+ cacheDirPath + " done ? " + ((new File(cacheDirPath).exists()) || isCreated ? "Oui" : "Non"));
                }
            }

            if(directory != null || name.lastIndexOf(sep) == startStr)
                name = name.substring(name.lastIndexOf(sep)+sep.length());

            if(directory == null)
                directory = "";
            Log.d(TAG,cacheDirPath+" "+name);
            return new File(cacheDirPath,name);
        }catch(Exception e){
            throw e;
        }
    }

    private void writeFile(String name,String data) throws Exception,FileNotFoundException {
        try{
            Log.d(TAG,"writeFile "+name);
            File file = getFile(name);
            if(file == null)
                throw new FileNotFoundException("File "+name+" not exists.");
            FileUtils.writeStringToFile(file,data,"UTF-8");
        }catch(Exception e){
            throw e;
        }
    }

    private String readFile(String name) throws Exception,FileNotFoundException {
        try{
            Log.d(TAG,"readFile "+name);
            File file = getFile(name);
            if(file == null) throw new FileNotFoundException("File "+name+" not exists.");
            if(!file.exists())
                throw new FileNotFoundException("File "+file.getAbsolutePath()+" not exists.");
            return FileUtils.readFileToString(file,"UTF-8");
        }catch(Exception e){
            throw e;
        }
    }
}