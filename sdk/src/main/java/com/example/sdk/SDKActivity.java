package com.example.sdk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

// TODO Sparse logging is left since we are adding additional commands in the future and to help
// TODO team view what is happening on a physical device

public class SDKActivity extends AppCompatActivity {

  private Object classObject; // Used for Intent callback
  private WebView webview;
  private CallTypes callTypes = null; // Used as intermediate concrete class to give polymorphism
  private CallBaseTypes waitingCallType = null; // Stores the waiting call type while authentication is performed
  private Handler mainThreadHandler; // Needed to keep run commands calls in a queue
  private String uuid = ""; // Has to be available to run the queued run command

  // This is required due to Android restrictions placed on webview
  // All run commands have to be placed in a queue (except authentication commands)
  // Because we check for user authentication before any command is run, we have to use the queue
  private class WorkerThread extends Thread {

    @Override
    public void run() {
      // Create a message in child thread.
      Message childThreadMessage = new Message();
      childThreadMessage.what = 1;
      // Put the message in webview thread message queue.
      mainThreadHandler.sendMessage(childThreadMessage);
    }
  }

  abstract static class CallBaseTypes {

    abstract String caller();

    abstract void called(String returnResult);
  }

  class CallTypes extends CallBaseTypes {

    public CallBaseTypes type;

    public CallTypes() {}

    public void update(CallBaseTypes type) {
      this.type = type;
    }

    public String caller() {
      return type.caller();
    }

    public void called(String returnResult) {
      type.called(returnResult);
    }
  }

  public class WebAppInterface {

    public CallBaseTypes type = null;

    // These methods are called from JaScript, so are given compile time warnings as never used
    @JavascriptInterface
    public void isAuthenticated(String returnResult) {
      callTypes.called(returnResult);
    }

    @JavascriptInterface
    public void encrypt(String returnResult) {
      callTypes.called(returnResult);
    }

    @JavascriptInterface
    public void decrypt(String returnResult) {
      callTypes.called(returnResult);
    }
  }

  public class IsAuthenticated extends CallBaseTypes {

    public String caller() {
      return "{\"type\":\"CWI\",\"call\":\"isAuthenticated\",\"params\": [],\"id\":\"uuid\"}";
    }

    public void called(String returnResult) {
      try {
        Log.i("WEBVIEW_AUTH", "called():returnResult:" + returnResult);
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        if (result.equals("false")) {
          /* TODO Should we call waitForAuthenticated() or return the boolean?
          waitingCallType = new WaitForAuthenticated();
          // Need to start the child thread to call waiting run command
          WorkerThread workerThread = new WorkerThread();
          workerThread.start();
          */
        }
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "isAuthenticated");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
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
      Log.i("WEBVIEW_AUTHED", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        uuid = jsonReturnResultObject.get("uuid").toString();
        if (waitingCallType != null) {
          // Need to start the child thread to call the waiting run command
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
    public Encrypt() {}

    public Encrypt(String plaintext, String protocolID, String keyID) {
      this.protocolID = protocolID;
      this.keyID = keyID;

      // Convert the string to base64 so it can be passed over the net
      byte[] byteArray = plaintext.getBytes(StandardCharsets.UTF_8);
      if (
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
      ) {
        base64Encoded =
          Base64.getEncoder().encodeToString(byteArray).getBytes();
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
      Log.i("WEBVIEW_ENCRYPT", "called():returnResult:" + returnResult);
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
    public Decrypt() {}

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
      Log.i("WEBVIEW_DECRYPT", "called():returnResult:" + returnResult);
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
    Log.i("WEBVIEW_JAVASCRIPT", "doJavaScript():javascript:" + javascript);
    webview.evaluateJavascript(javascript, null);
  }

  // Generic 'run' command
  public void runCommand(CallBaseTypes type, String uuid) {
    callTypes.update(type);
    doJavaScript(
      webview,
      "window.postMessage(" + callTypes.caller().replace("uuid", uuid) + ")"
    );
  }

  // TODO (Not currently required) Helper to format complex JSON objects into an appropriate string
  public static String JSONFormatStr(String name, String key) {
    try {
      return new JSONObject("{\"" + name + "\":\"" + key + "\"}").toString();
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  private static Object convertFromBytes(byte[] bytes)
    throws IOException, ClassNotFoundException {
    try (
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInputStream in = new ObjectInputStream(bis)
    ) {
      return in.readObject();
    }
  }

  private static byte[] convertToBytes(Object object) throws IOException {
    try (
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos)
    ) {
      out.writeObject(object);
      return bos.toByteArray();
    }
  }

  // Called by App to pass over calling class to be used by Intent callback
  // Raises compile time warning as not used
  public static String passActivity(Object activity) {
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
          Intent intent = getIntent();
          String callingClass = intent.getStringExtra("callingClass");
          byte[] bytes = android.util.Base64.decode(
            callingClass,
            android.util.Base64.DEFAULT
          );
          try {
            classObject = convertFromBytes(bytes);
          } catch (IOException e) {
            throw new RuntimeException(e);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
          String type = intent.getStringExtra("type");
          uuid = getIntent().getStringExtra("uuid");
          if (type.equals("isAuthenticated")) {
            runCommand(new IsAuthenticated(), uuid);
          }
          if (type.equals("waitForAuthenticated")) {
            runCommand(new WaitForAuthenticated(), uuid);
          }
          if (type.equals("encrypt")) {
            waitingCallType =
              new Encrypt(
                intent.getStringExtra("plaintext"),
                intent.getStringExtra("protocolID"),
                intent.getStringExtra("keyID")
              );
            // Need to start the child thread to call waitForAuthenticated run command
            WorkerThread workerThread = new WorkerThread();
            workerThread.start();
            runCommand(new WaitForAuthenticated(), uuid);
          }
          if (type.equals("decrypt")) {
            waitingCallType =
              new Decrypt(
                intent.getStringExtra("ciphertext"),
                intent.getStringExtra("protocolID"),
                intent.getStringExtra("keyID")
              );
            // Need to start the child thread to call waitForAuthenticated run command
            WorkerThread workerThread = new WorkerThread();
            workerThread.start();
            runCommand(new WaitForAuthenticated(), uuid);
          }
        }
      }
    );
    mainThreadHandler =
      new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
          if (msg.what == 1) {
            // Update view component text, this is allowed.
            runCommand(waitingCallType, uuid);
          }
        }
      };
    // required for most of our React modules (Hades, Prosperity etc.)
    webview.getSettings().setDomStorageEnabled(true);
    webview.getSettings().setJavaScriptEnabled(true);
    webview.getSettings().setUserAgentString("babbage-webview-inlay");
    webview.addJavascriptInterface(
      new WebAppInterface(),
      "androidMessageHandler"
    );
    webview.loadUrl("https://staging-mobile-portal.babbage.systems");
  }
}
