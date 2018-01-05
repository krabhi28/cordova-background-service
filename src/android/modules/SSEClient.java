// Okhttp3 SSE Client ported from https://github.com/heremaps/oksse.git
// thanks to HERE Technologies 
package ga.oshimin.cordova.module;

import java.net.URISyntaxException;
import java.util.HashMap;

import okhttp3.*;
import okio.BufferedSource;

import java.util.Collections;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mozilla.javascript.Function;

import ga.oshimin.cordova.JSEngine;
import ga.oshimin.cordova.Iface;
import org.mozilla.javascript.Wrapper;

import android.util.Log;


public class SSEClient implements Wrapper,Iface {
    private final OkHttpClient client;
    public String TAG = "SSEClient"+JSEngine.TAG;

    private Function eventListener;
    private Reader mSocket;
    private Call call;
    
    private Request request;
    private JSEngine mEngine;

    private long reconnectTime = TimeUnit.SECONDS.toMillis(10);
    private long maxReconnection = 10;
    private long nbReconnection = 0;
    private long readTimeoutMillis = 0;
    
    public SSEClient(String url) throws URISyntaxException{
        this(url, JSEngine.instance);
    }
    public SSEClient(String url,JSEngine engine) throws URISyntaxException{
        client = new OkHttpClient.Builder()
                            .readTimeout(0, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build()
                            .newBuilder()
                            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                            .build();
        request = new Request.Builder().url(url).build();
        mEngine  = engine;
    }
    public void connect(){
        prepareCall(request);
        enqueue();
    }
    public void disconnect(){
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
        Log.d(TAG,"disconnect");
        callEvent("disconnect",null);
    }
    public void onEvent(final Function cb){
        eventListener = cb;
    }
    public String toString(){
        return "[Object SSE]";
    }
    public String toJSON(){
        return toString();
    }
    @Override
    public Object unwrap(){
    	return (Object) mSocket;
    }
    private void callEvent(Object... args) {
        if(eventListener != null)
            mEngine.call(eventListener,args);
    }

    private void prepareCall(Request request) {
        Request newRequest = request.newBuilder()
                .header("Accept-Encoding", "")
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .build();
        call = client.newCall(newRequest);
    }

    private void enqueue() {
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyFailure(e, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                nbReconnection = 0;
                if (response.isSuccessful()) {
                    openSse(response);
                } else {
                    notifyFailure(new IOException(response.message()), response);
                }
            }
        });
    }
    private void openSse(Response response) {
        mSocket = new Reader(response.body().source());
        mSocket.setTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS);
        callEvent("connect");
        
        //noinspection StatementWithEmptyBody
        while (call != null && !call.isCanceled() && mSocket.read()) {}
    }

    private void notifyFailure(Throwable throwable, Response response) {
        if (!retry(throwable, response)) {
            disconnect();
        }
    }
    private boolean retry(Throwable throwable, Response response) {
        if (!Thread.currentThread().isInterrupted() && !call.isCanceled()) {
            if (request == null) {
                return false;
            }
            prepareCall(request);
            try {
                nbReconnection++;
                Log.d(TAG,"try to reconnect "+nbReconnection);
                Thread.sleep(reconnectTime);
            } catch (InterruptedException ignored) {
                return false;
            }
            if (!Thread.currentThread().isInterrupted() && !call.isCanceled() && nbReconnection < maxReconnection) {
                enqueue();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Internal reader for the SSE channel. This will wait for data being send and will parse it according to the
     * SSE standard.
     *
     * @see Reader#read()
     */
    private class Reader {

        private static final char COLON_DIVIDER = ':';
        private static final String UTF8_BOM = "\uFEFF";

        private static final String DATA = "data";
        private static final String ID = "id";
        private static final String EVENT = "event";
        private static final String RETRY = "retry";
        private static final String DEFAULT_EVENT = "message";
        private static final String EMPTY_STRING = "";

        private final Pattern DIGITS_ONLY = Pattern.compile("^[\\d]+$");

        private final BufferedSource source;

        // Intentionally done to reuse StringBuilder for memory optimization
        @SuppressWarnings("PMD.AvoidStringBufferField")
        private StringBuilder data = new StringBuilder();
        private String lastEventId;
        private String eventName = DEFAULT_EVENT;

        Reader(BufferedSource source) {
            this.source = source;
        }

        /**
         * Blocking call that will try to read a line from the source
         *
         * @return true if the read was successfully, false if an error was thrown
         */
        boolean read() {
            try {
                String line = source.readUtf8LineStrict();
                processLine(line);
            } catch (IOException e) {
                notifyFailure(e, null);
                return false;
            }
            return true;
        }

        /**
         * Close the source
         */
        void close() {
            try {
                source.close();
            } catch (IOException e) {
                // Close quietly
            }
        }

        /**
         * Sets a reading timeout, so the read operation will get unblock if this timeout is reached.
         *
         * @param timeout timeout to set
         * @param unit unit of the timeout to set
         */
        void setTimeout(long timeout, TimeUnit unit) {
            if (source != null) {
                source.timeout().timeout(timeout, unit);
            }
        }

        private void processLine(String line) {
            //log("Sse read line: " + line);
            if (line.isEmpty()) { // If the line is empty (a blank line). Dispatch the event.
                dispatchEvent();
                return;
            }

            int colonIndex = line.indexOf(COLON_DIVIDER);
            if (colonIndex == 0) { // If line starts with COLON dispatch a comment
                if(!line.substring(1).trim().equals(""))
                    callEvent("comment", line.substring(1).trim());
            } else if (colonIndex != -1) { // Collect the characters on the line after the first U+003A COLON character (:), and let value be that string.
                String field = line.substring(0, colonIndex);
                String value = EMPTY_STRING;
                int valueIndex = colonIndex + 1;
                if (valueIndex < line.length()) {
                    if (line.charAt(valueIndex) == ' ') { // If value starts with a single U+0020 SPACE character, remove it from value.
                        valueIndex++;
                    }
                    value = line.substring(valueIndex);
                }
                processField(field, value);
            } else {
                processField(line, EMPTY_STRING);
            }
        }

        private void dispatchEvent() {
            if (data.length() == 0) 
                return;
            String dataString = data.toString();
            if (dataString.endsWith("\n")) {
                dataString = dataString.substring(0, dataString.length() - 1);
            }
            callEvent(eventName, lastEventId, dataString);
            data.setLength(0);
            eventName = DEFAULT_EVENT;
        }

        private void processField(String field, String value) {
            if (DATA.equals(field)) {
                data.append(value).append('\n');
            } else if (ID.equals(field)) {
                lastEventId = value;
            } else if (EVENT.equals(field)) {
                eventName = value;
            } else if (RETRY.equals(field) && DIGITS_ONLY.matcher(value).matches()) {
                long timeout = Long.parseLong(value);
                reconnectTime = timeout;
                
            }
        }
    }
}