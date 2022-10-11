package com.example.sdk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class SDKActivity extends AppCompatActivity {
    private Object classObject;
    private WebView webview;
    private CallBaseTypes waitingCallType = null;
    private CallTypes callTypes = null; // Used as intermediate concrete class to give polymorphism
    private Handler mainThreadHandler;
    private String uuid = "";


    private class WorkerThread extends Thread{
        @Override
        public void run() {
            // Create a message in child thread.
            Message childThreadMessage = new Message();
            childThreadMessage.what = 1;
            // Put the message in main thread message queue.
            mainThreadHandler.sendMessage(childThreadMessage);
        }
    }

    static abstract class CallBaseTypes {
        abstract String caller();
        abstract void called(String returnResult);
    }

    class CallTypes extends CallBaseTypes {
        public CallBaseTypes type;

        public CallTypes () {
        }
        public void update (CallBaseTypes type) {
            this.type = type;
        }
        public String caller(){
            return type.caller();
        }
        public void called(String returnResult){ type.called(returnResult); }
    }

    public class WebAppInterface {
        public CallBaseTypes type = null;

        @JavascriptInterface
        public void isAuthenticated(String returnResult) {
            Log.i("WEBVIEW_AUTH_INTERFACE", returnResult);
            callTypes.called(returnResult);
        }
        @JavascriptInterface
        public void encrypt(String returnResult) {
            Log.i("WEBVIEW_ENC_INTERFACE", returnResult);
            callTypes.called(returnResult);
        }
        @JavascriptInterface
        public void decrypt(String returnResult) {
            Log.i("WEBVIEW_DEC_INTERFACE", returnResult);
            callTypes.called(returnResult);
        }
        @JavascriptInterface
        public void dbg(String str) {
            Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            Log.i("WEBVIEW_DBG_INTERFACE", str);
        }
    }

    public class IsAuthenticated extends CallBaseTypes {

        public String caller() {
            return "{\"type\":\"CWI\",\"call\":\"isAuthenticated\",\"params\": [],\"id\":\"uuid\"}";
        }
        public void called(String returnResult) {
            try {
                JSONObject jsonReturnResultObject = new JSONObject(returnResult);
                String uuid = jsonReturnResultObject.get("uuid").toString();
                String result = jsonReturnResultObject.get("result").toString();
                if (result.equals("false")) {
                    runCommand(new WaitForAuthenticated(), uuid);
                }
                else {
                    Intent intent = new Intent(SDKActivity.this, classObject.getClass());
                    intent.putExtra("type", "isAuthenticated");
                    intent.putExtra("uuid", uuid);
                    intent.putExtra("result", result);
                    if (waitingCallType != null) {
                        // Start a child thread when button is clicked.
                        WorkerThread workerThread = new WorkerThread();
                        workerThread.start();
                    } else {
                        startActivity(intent);
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class WaitForAuthenticated extends CallBaseTypes {

        public String caller() {
            return "{\"type\":\"CWI\",\"call\":\"waitForAuthenticated\",\"params\": [],\"id\":\"uuid\"}";
        }
        public void called(String returnResult) {
            try {
                JSONObject jsonReturnResultObject = new JSONObject(returnResult);
                String uuid = jsonReturnResultObject.get("uuid").toString();
                if (waitingCallType != null) {
                    // Start a child thread when button is clicked.
                    WorkerThread workerThread = new WorkerThread();
                    workerThread.start();
                } else {
                    finish();
                }
             } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class Encrypt extends CallBaseTypes {
        private byte[] base64Encoded;
        private String protocolID;
        private String keyID;

        // Required for polymorphism
        public Encrypt(){
        }
        public Encrypt(String plaintext, String protocolID, String keyID) {
            this.protocolID = protocolID;
            this.keyID = keyID;

            // Convert the string to base64 so it can be passed over the net
            try {
                byte[] byteArray = plaintext.getBytes("UTF-8");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    base64Encoded = Base64.getEncoder().encodeToString(byteArray).getBytes();
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        public String caller() {
            String cmdJSONString = "{";
            cmdJSONString += "\"type\":\"CWI\",";
            cmdJSONString += "\"call\":\"encrypt\",";
            cmdJSONString += "\"params\":{";
            cmdJSONString += "\"plaintext\":\"" + new String(base64Encoded) + "\",";
            cmdJSONString += "\"protocolID\":\"" + protocolID + "\",";
            cmdJSONString += "\"keyID\":\"" + keyID + "\",";
            cmdJSONString += "\"returnType\":\"string\"";
            cmdJSONString += "},";
            cmdJSONString += "\"id\":\"uuid\"";
            cmdJSONString += "}";
            return cmdJSONString;
        }
        public void called(String returnResult) {
            try {
                JSONObject jsonReturnResultObject = new JSONObject(returnResult);
                String uuid = jsonReturnResultObject.get("uuid").toString();
                String result = jsonReturnResultObject.get("result").toString();
                Intent intent = new Intent(SDKActivity.this, classObject.getClass());
                intent.putExtra("type", "encrypt");
                intent.putExtra("uuid", uuid);
                intent.putExtra("result", result);
                startActivity(intent);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class Decrypt extends CallBaseTypes {
        private String ciphertext;
        private String protocolID;
        private String keyID;

        // Required for polymorphism
        public Decrypt(){
        }
        public Decrypt(String ciphertext, String protocolID, String keyID) {
            this.ciphertext = ciphertext;
            this.protocolID = protocolID;
            this.keyID = keyID;

        }
        public String caller() {
            String cmdJSONString = "";
            cmdJSONString += "{\"type\":\"CWI\",";
            cmdJSONString += "\"call\":\"decrypt\",";
            cmdJSONString += "\"params\":{";
            cmdJSONString += "\"ciphertext\":\"" + ciphertext + "\",";
            cmdJSONString += "\"protocolID\":\"" + protocolID + "\",";
            cmdJSONString += "\"keyID\":\"" + keyID + "\",";
            cmdJSONString += "\"returnType\":\"string\"";
            cmdJSONString += "},";
            cmdJSONString += "\"id\":\"uuid\"";
            cmdJSONString += "}";
            return cmdJSONString;
        }
        public void called(String returnResult) {
            try {
                JSONObject jsonReturnResultObject = new JSONObject(returnResult);
                String uuid = jsonReturnResultObject.get("uuid").toString();
                String result = jsonReturnResultObject.get("result").toString();
                Intent intent = new Intent(SDKActivity.this, classObject.getClass());
                intent.putExtra("type", "decrypt");
                intent.putExtra("uuid", uuid);
                intent.putExtra("result", result);
                startActivity(intent);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void doJavaScript(WebView webview, String javascript) {
        Log.i("WEBVIEW_JAVASCRIPT", javascript);
        webview.evaluateJavascript(javascript, null);
        // finish();
    }
    // Generic 'run' command
    public void runCommand(CallBaseTypes type, String uuid) {
        callTypes.update(type);
        // String cmdJSONString = callTypes.caller();
        doJavaScript(webview, "window.postMessage(" + callTypes.caller().replace("uuid", uuid) + ")");
    }
    // Helper to format complex JSON objects into an appropriate string
    public static String JSONFormatStr(String name, String key){
        try {
            return new JSONObject("{\"" + name + "\":\"" + key + "\"}").toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }
    private static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }
    public static String passActivity(Object activity){
        byte[] bytes;
        try {
            bytes = convertToBytes(activity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] base64Encoded = null;
        // Convert the string to base64 so it can be passed over the net
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            base64Encoded = Base64.getEncoder().encodeToString(bytes).getBytes();
        }
        byte[] finalBase64Encoded = base64Encoded;
        return new String(finalBase64Encoded);
    }
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        callTypes = new CallTypes();
        setContentView(R.layout.activity_sdk);
        webview = findViewById(R.id.web_html);
        webview.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        // need to store the calling class to be used as the callback class in the returning intent
                        Intent intent = getIntent();
                        String callingClass = intent.getStringExtra("callingClass");
                        byte[] bytes = android.util.Base64.decode(callingClass, android.util.Base64.DEFAULT);
                        try {
                            classObject = convertFromBytes(bytes);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        // allow app developer to inject javascript using the script key value
                        String script = intent.getStringExtra("script");
                        webview.loadUrl("javascript:document.getElementsByTagName('body')[0].innerHTML = document.getElementsByTagName('body')[0].innerHTML" + script);
                        String type = intent.getStringExtra("type");
                        uuid = getIntent().getStringExtra("uuid");
                        if (type.equals("isAuthenticated")) {
                            runCommand(new IsAuthenticated(), uuid);
                        }
                        if (type.equals("waitForAuthenticated")) {
                            runCommand(new WaitForAuthenticated(), uuid);
                        }
                        if (type.equals("encrypt")) {
                            waitingCallType = new Encrypt(
                              intent.getStringExtra("plaintext"),
                              intent.getStringExtra("protocolID"),
                              intent.getStringExtra("keyID")
                            );
                            // Start a child thread when button is clicked.
                            WorkerThread workerThread = new WorkerThread();
                            workerThread.start();
                            runCommand(new WaitForAuthenticated(), uuid);
                        }
                        if (type.equals("decrypt")) {
                            waitingCallType = new Decrypt(
                                intent.getStringExtra("ciphertext"),
                                intent.getStringExtra("protocolID"),
                                intent.getStringExtra("keyID")
                            );
                            WorkerThread workerThread = new WorkerThread();
                            workerThread.start();
                            runCommand(new WaitForAuthenticated(), uuid);
                        }
                    }
                }
        );
        // This handler is used to handle child thread message from main thread message queue.
        mainThreadHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 1)
                {
                    // Update view component text, this is allowed.
                    runCommand(waitingCallType, uuid);
                }
            }
        };

        // required for most of our React modules (Hades, Prosperity etc.)
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setUserAgentString("babbage-webview-inlay");
        webview.addJavascriptInterface(new WebAppInterface(), "androidMessageHandler");
        webview.loadUrl("https://staging-mobile-portal.babbage.systems");
    }
}