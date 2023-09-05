package com.example.sdk;

import com.example.sdk.SDKActivity;
import static org.apache.commons.codec.binary.Base64.isBase64;
import static android.util.Base64.DEFAULT;
import static android.util.Base64.decode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Call;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

// TODO Sparse logging is left since we are adding additional commands in the future and to help
// TODO team view what is happening on a physical device

public class SDKActivity extends AppCompatActivity {
        //implements Serializable {
  public  static String counterparty = "";
  private transient boolean pageFinished = false;
  private transient boolean openBabbage = false;
  private transient String url;
  //= "https://staging-mobile-portal.babbage.systems";
  public static Object classObject; // Used for Intent callback
  private WebView webview;
  private CallTypes callTypes = null; // Used as intermediate concrete class to give polymorphism
  private Stack<CallBaseTypes> babbageWaitingCallType = new Stack<CallBaseTypes>(); // Stores the waiting call type while authentication is performed
  Stack<CallBaseTypes> waitingCallType = new Stack<CallBaseTypes>(); // Stores the waiting call type while authentication is performed
  private static Handler mainThreadHandler; // Needed to keep run commands calls in a queue
  private String uuid = ""; // Has to be available to run the queued run command
  private static String identityKey = ""; //

  /*** High-level API Middleware ***/

  public static Intent encrypt(Context activityContext, Object instance, TextView messageText, String url) {
    return encrypt(activityContext, instance, messageText, "self", url, "");
  }
  public static Intent encrypt(Context activityContext, Object instance, TextView messageText, String counterparty, String url, String portal) {
    SDKActivity.counterparty = counterparty;
    Log.i("D_SDK_ENCRYPT", "encrypt():returnResult:counterparty=" + counterparty);
    Log.i("D_SDK", "encrypt():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "encrypt");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("plaintext", messageText.getText().toString());
    intent.putExtra("protocolID", "[1, 'crypton']");
    //intent.putExtra("protocolID", "crypton");
    intent.putExtra("keyID", "1");
    intent.putExtra("counterparty", counterparty);
    intent.putExtra("returnType", "string");
    intent.putExtra("url", url);
    Log.i("D_SDK", "<encrypt():" + intent);
    return intent;
  }
  public static Intent decrypt(Context activityContext, Object instance, TextView cipherText, String url) {
    return decrypt(activityContext, instance, cipherText, "self", url, "");
  }
  public static Intent decrypt(Context activityContext, Object instance, TextView cipherText, String counterparty, String url, String portal) {
    Log.i("D_SDK", "decrypt():portal:" + portal);
    SDKActivity.counterparty = counterparty;
    Log.i("D_SDK_INTENT_DECRYPT", "decrypt():counterparty=" + counterparty);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "decrypt");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("ciphertext", cipherText.getText().toString());
    intent.putExtra("protocolID", "[1, 'crypton']");
    intent.putExtra("keyID", "1");
    intent.putExtra("counterparty", counterparty);
    intent.putExtra("returnType", "string");
    intent.putExtra("url", url);
    Log.i("D_SDK", "<decrypt():" + intent);
    return intent;
  }
  public static Intent doWaitingType(Context activityContext, Object instance, String waitingInstance, String url, String portal) {
    Log.i("D_SDK", ">doWaitingType():waitingInstance=" + waitingInstance);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("type", "waitingInstance");
    intent.putExtra("waitingInstance", waitingInstance);
    intent.putExtra("url", url);
    intent.putExtra("portal", portal);
    Log.i("D_SDK", "<doWaitingType():" + intent);
    return intent;
  }
  public static Intent doWaitingType(Context activityContext, Object instance, String waitingInstance, String waitingNextInstance, String url, String portal) {
    Log.i("D_SDK", ">doWaitingType():waitingInstance=" + waitingInstance);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("type", "waitingInstance");
    intent.putExtra("waitingInstance", waitingInstance);
    intent.putExtra("waitingNextInstance", waitingNextInstance);
    intent.putExtra("url", url);
    intent.putExtra("portal", portal);
    Log.i("D_SDK", "<doWaitingType():" + intent);
    return intent;
  }
  public static Intent portal(Context activityContext, Object instance, String url) {
    Log.i("D_SDK", ">portal()");
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("type", "portal");
    intent.putExtra("url", url);
    Log.i("D_SDK", "<portal():" + intent);
    return intent;
  }

  //25Aug2023
  public static Intent waitForAuthentication(Context activityContext, Object instance, String url) {
    return openPortal(activityContext, instance, url, "");
  }
  public static Intent waitForAuthentication(Context activityContext, Object instance, String url, String portal) {
    return openPortal(activityContext, instance, url, "");
  }
  public static Intent waitForAuthentication(Context activityContext, Object instance, String url, String portal, String waitingTypeInstance) {
    Log.i("D_SDK", "waitForAuthentication():portal:0" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    //Log.i("D_SDK", "waitForAuthentication():portal:1");
    intent.putExtra("callingClass", setInstance(instance));
    //Log.i("D_SDK", "waitForAuthentication():portal:2");
    intent.putExtra("portal", portal);
    intent.putExtra("type", "waitForAuthentication");
    //String waitingInstance = waitingCallType.get(0);
    //Log.i("D_SDK", "waitForAuthentication():waitingInstance=" + waitingInstance);
    //intent.putExtra("waitingInstance", );
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    intent.putExtra("waitingInstance", waitingTypeInstance);
    return intent;
  }
  public static Intent generateAES256GCMCryptoKey(Context activityContext, Object instance, String url) {
    Log.i("D_SDK", "intent generateAES256GCMCryptoKey()");
    return generateAES256GCMCryptoKey(activityContext, instance, url,"");
  }

  public static Intent generateAES256GCMCryptoKey(Context activityContext, Object instance, String url, String portal) {
    Log.i("D_SDK", "generateAES256GCMCryptoKey():portal:0" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    Log.i("D_SDK", "generateAES256GCMCryptoKey():portal:1");
    intent.putExtra("callingClass", setInstance(instance));
    Log.i("D_SDK", "generateAES256GCMCryptoKey():portal:2");
    intent.putExtra("portal", portal);
    intent.putExtra("type", "generateAES256GCMCryptoKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    return intent;
  }
  public static Intent openPortal(Context activityContext, Object instance, String url) {
    return openPortal(activityContext, instance, url, "");
  }
  public static Intent openPortal(Context activityContext, Object instance, String url, String portal) {
    Log.i("D_SDK", "openPortal():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "portal");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    return intent;
  }
  public static Intent getIdentityKey(Context activityContext, Object instance, String url) {
    Log.i("D_SDK", "getIdentityKey():constructor()");
    return getIdentityKey(activityContext, instance, url,"");
  }
  public static Intent getIdentityKey(Context activityContext, Object instance, String url, String portal) {
    Log.i("D_SDK", "getIdentityKey():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "getIdentityKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    intent.putExtra("key", identityKey);
    return intent;
  }
  public static Intent encryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url) {
    return encryptUsingCryptoKey(activityContext, instance, cryptoKeyText, messageText, url,"");
  }
  public static Intent encryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url, String portal) {
    Log.i("D_SDK", "encryptUsingCryptoKey()portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "encryptUsingCryptoKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("plaintext", messageText.getText().toString());
    intent.putExtra("base64CryptoKey", cryptoKeyText.getText().toString());
    intent.putExtra("returnType", "base64");
    intent.putExtra("url", url);
    return intent;
  }
  public static Intent decryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url) {
    return decryptUsingCryptoKey(activityContext, instance, cryptoKeyText, messageText, url,"");
  }
  public static Intent decryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url, String portal) {
    Log.i("D_SDK", "decryptUsingCryptoKey():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "decryptUsingCryptoKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("ciphertext", messageText.getText().toString());
    intent.putExtra("base64CryptoKey", cryptoKeyText.getText().toString());
    intent.putExtra("returnType", "base64");
    intent.putExtra("url", url);
    return intent;
  }

  /*** High-level API Middleware ***/

  // This is required due to Android restrictions placed on Webview
  // All run commands have to be placed in a queue (except authentication commands)
  // Because we check for user authentication before any command is run, we have to use the queue
  public static class WorkerThread extends Thread {

    @Override
    public void run() {
      // Create a message in child thread.
      Message childThreadMessage = new Message();
      childThreadMessage.what = 1;
      // Put the message in Webview thread message queue.
      mainThreadHandler.sendMessage(childThreadMessage);
    }
  }

  abstract static class CallBaseTypes {

    abstract String caller();

    abstract void called(String returnResult);
  }

  class CallTypes extends CallBaseTypes implements Serializable {
    public CallBaseTypes type;
    public String actualType;
    public String portal;

    public CallTypes() {}

    public void update(CallBaseTypes type, String actualType, String portal) {
      Log.i("D_SDK", ">update():type=" + type + ",actualType=" + actualType + ",portal=" + portal);
      this.type = type;
      this.actualType = actualType;
      this.portal = portal;
      Log.i("D_SDK", "<update()");
    }
    public String caller() {
      return type.caller();
    }
    public void called(String returnResult) {
      type.called(returnResult);
    }
  }

  // portal callbacks defined
  public class WebAppInterface {

    public CallBaseTypes type = null;

    // These methods are called from JavaScript, so are given compile time warnings as they are never used in this class
    // @JavascriptInterface
    // public void dbg(String returnResult) {
    //   Log.i("D_SDK_INTERFACE_DBG", returnResult);
    // }
    /*
    @JavascriptInterface
    public void openBabbage() {
      Log.i("D_SDK_INTERFACE", "called openBabbage():callTypes.type:" + callTypes.actualType);
      Log.i("D_SDK_INTERFACE", "called openBabbage():callTypes.portal:" + callTypes.portal);
      Intent intent = new Intent(SDKActivity.this, SDKActivity.class);
      intent.putExtra("callingClass", classObject.getClass());
      intent.putExtra("portal","portal");
      intent.putExtra("type", callTypes.actualType);
      intent.putExtra("uuid", UUID.randomUUID().toString());
      intent.putExtra("url", url);
      Log.i("D_SDK_INTERFACE", "called openBabbage():startActivity(intent)");
      startActivity(intent);
    }
    */
    @JavascriptInterface
    public void closeBabbage() {
      Log.i("D_SDK_INTERFACE", "called closeBabbage():waitingCallType:" + waitingCallType);
      Log.i("D_SDK_INTERFACE", "called closeBabbage():babbageWaitingCallType:" + babbageWaitingCallType);
      openBabbage = false;
      if (babbageWaitingCallType.isEmpty()) {
        finish();
      } else {
        Log.i("D_SDK_INTERFACE", "called closeBabbage():babbageWaitingCallType not empty");
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("waitingInstance", setInstance(waitingCallType.pop()));
        intent.putExtra("uuid", UUID.randomUUID().toString());
        startActivity(intent);
      }
      // Process the waiting command(s)
      WorkerThread workerThread = new WorkerThread();
      workerThread.start();
    }
    @JavascriptInterface
    public void isFocused() {
      Log.i("D_SDK_INTERFACE_IS_FOCUSED", "called isFocused()");
    }
    @JavascriptInterface
    public void waitForAuthentication(String returnResult) {
      callTypes.called(returnResult);
    }
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
    @JavascriptInterface
    public void generateAES256GCMCryptoKey(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void encryptUsingCryptoKey(String returnResult) {
       callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void decryptUsingCryptoKey(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createAction(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createHmac(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void verifyHmac(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createSignature(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void verifySignature(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createCertificate(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void getCertificates(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void proveCertificate(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void submitDirectTransaction(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void getPublicKey(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void getVersion(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createPushDropScript(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void parapetRequest(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void downloadUHRPFile(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void newAuthriteRequest(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createOutputScriptFromPubKey(String returnResult) { callTypes.called(returnResult); }
  }




  /*
  @available(iOS 15.0, *)
  public func encrypt(plaintext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {

    // Convert the string to a base64 string
    let base64Encoded = convertStringToBase64(data: plaintext)

    // Construct the expected command to send
    var cmd:JSON = [
    "type":"CWI",
            "call":"encrypt",
            "params": [
    "plaintext": convertToJSONString(param: base64Encoded),
    "protocolID": protocolID,
            "keyID": convertToJSONString(param: keyID),
    "counterparty": convertToJSONString(param: counterparty!),
    "returnType": "string"
            ]
        ]

    // Run the command and get the response JSON object
    var responseObject:JSON = []
    do {
      responseObject = try await runCommand(cmd: &cmd).value
    } catch {
      throw error
    }

    // Pull out the expect result string
    let encryptedText:String = (responseObject.objectValue?["result"]?.stringValue)!
    return encryptedText
  }
  */
  //public func encrypt(plaintext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {

  /*** 03Sep2023-1608 ***/
  /*
  public class Encrypt extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public Encrypt() {}

    // Default values enforced by overloading constructor
    public Encrypt(String plaintext, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"plaintext\":\"" + convertStringToBase64(plaintext) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
      paramStr += "\"counterparty\":\"self\",";
      paramStr += "\"returnType\":\"string\"";
    }
    public Encrypt(String plaintext, String protocolID, String keyID, String counterparty) {
      Log.i("D_SDK_ENCRYPT", "Encrypt():plaintext=" + plaintext);
      Log.i("D_SDK_ENCRYPT", "Encrypt():base64 plaintext=" + convertStringToBase64(plaintext));
      paramStr = "";
      paramStr += "\"plaintext\":\"" + convertStringToBase64(plaintext) + "\",";
      paramStr += "\"protocolID\":" + protocolID + ",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"returnType\":\"string\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"encrypt\",";
      cmdJSONString += "\"params\":{" + paramStr;
      cmdJSONString += "},";
      cmdJSONString += "\"originator\":\"projectbabbage.com\",";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_ENCRYPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "encrypt");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        intent.putExtra("counterparty", counterparty);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "encrypt", "invalid JSON", "result");
      }
    }
  }
  */
  /*
  // Encrypts data using CWI.decrypt
  @available(iOS 15.0, *)
  public func decrypt(ciphertext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"decrypt",
          "params": [
              "ciphertext": convertToJSONString(param: ciphertext),
              "protocolID": protocolID,
              "keyID": convertToJSONString(param: keyID),
              "counterparty": convertToJSONString(param: counterparty!),
              "returnType": "string"
          ]
      ]

      // Run the command and get the response JSON object
      var responseObject:JSON = []
      do {
          responseObject = try await runCommand(cmd: &cmd).value
      } catch {
          throw error
      }

      // Pull out the expect result string
      let decryptedText:String = (responseObject.objectValue?["result"]?.stringValue)!
      return decryptedText
  }
  */
  // public func decrypt(ciphertext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {
  public class Decrypt extends CallBaseTypes implements Serializable {
    private String paramStr;

    // Required for polymorphism
    public Decrypt() {}

    // Default values enforced by overloading constructor
    public Decrypt(String ciphertext, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
      paramStr += "\"protocolID\":" + protocolID + ",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"counterparty\":\"self\",";
      paramStr += "\"returnType\":\"string\"";
    }
    public Decrypt(String ciphertext, String protocolID, String keyID, String counterparty) {
      paramStr = "";
      paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
      paramStr += "\"protocolID\":" + protocolID + ",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"returnType\":\"string\"";
    }
    public String caller() {
      Log.i("D_SDK_DECRYPT", "caller():returnResult:counterparty=" + counterparty);
      String cmdJSONString = "";
      cmdJSONString += "{\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"decrypt\",";
      cmdJSONString += "\"params\":{" + paramStr;
      cmdJSONString += "},";
      cmdJSONString += "\"originator\":\"projectbabbage.com\",";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_DECRYPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "decrypt");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        Log.i("D_SDK_DECRYPT", "called():returnResult:counterparty=" + counterparty);
        intent.putExtra("counterparty", counterparty);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "decrypt", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func generateAES256GCMCryptoKey() async -> String {
    // Construct the expected command to send
    var cmd:JSON = [
        "type":"CWI",
        "call":"generateAES256GCMCryptoKey",
        "params": []
    ]

    // Run the command and get the response JSON object
    let responseObject = await runCommand(cmd: &cmd).value

    // Pull out the expect result string
    let cryptoKey:String = (responseObject.objectValue?["result"]?.stringValue)!
    return cryptoKey
  }
  */
  // public func generateAES256GCMCryptoKey() async -> String {
  public class GenerateAES256GCMCryptoKey extends CallBaseTypes implements Serializable {

    public String caller() {
      Log.i("D_SDK_GEN_CRYPT", "caller()");
      return "{\"type\":\"CWI\",\"call\":\"generateAES256GCMCryptoKey\",\"params\":{},\"id\":\"uuid\"}";
    }
    public void called(String returnResult) {
      Log.i("D_SDK_GEN_CRYPT", " >called():returnResult:" + returnResult);
      JSONObject jsonReturnResultObject = null;
      // String result = "";
      try {
        jsonReturnResultObject = new JSONObject(returnResult);
        Log.i("D_SDK_GEN_CRYPT", "  called():jsonReturnResultObject=" + jsonReturnResultObject);
        /*
        if (jsonReturnResultObject.get("code").equals("ERR_UNKNOWN")) {
          // returnError(returnResult,"generateAES256GCMCryptoKey", "invalid JSON", "result");
          Log.i("D_SDK_GEN_CRYPT", "  called():12");
          String result = jsonReturnResultObject.get("result").toString();
          Log.i("D_SDK_GEN_CRYPT", "  called():13");
          Intent intent = new Intent(SDKActivity.this, classObject.getClass());
          Log.i("D_SDK_GEN_CRYPT", "  called():14");
          intent.putExtra("type", "generateAES256GCMCryptoKey");
          Log.i("D_SDK_GEN_CRYPT", "  called():15");
          intent.putExtra("uuid", uuid);
          Log.i("D_SDK_GEN_CRYPT", "  called():16");
          intent.putExtra("result", result);
          Log.i("D_SDK_GEN_CRYPT", "called():return:intent=" + intent);
          startActivity(intent);
          //return;
        }
        */
        String uuid = jsonReturnResultObject.get("uuid").toString();
        Log.i("D_SDK_GEN_CRYPT", "  called():2");
        String result = jsonReturnResultObject.get("result").toString();
        Log.i("D_SDK_GEN_CRYPT", "  called():result=" + result);
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        Log.i("D_SDK_GEN_CRYPT", "  called():4");
        intent.putExtra("type", "generateAES256GCMCryptoKey");
        Log.i("D_SDK_GEN_CRYPT", "  called():5");
        intent.putExtra("uuid", uuid);
        Log.i("D_SDK_GEN_CRYPT", "  called():6");
        intent.putExtra("result", result);
        Log.i("D_SDK_GEN_CRYPT", "called():return:intent=" + intent);
        startActivity(intent);
      } catch (JSONException e) {
        Log.i("D_SDK_GEN_CRYPT", "called():error:" + e);
        // String result = jsonReturnResultObject.get("result").toString();
        // returnError(returnResult,"generateAES256GCMCryptoKey", "invalid JSON", "result");
        Log.i("D_SDK_GEN_CRYPT", "  called():2");
        Log.i("D_SDK_GEN_CRYPT", "  called():3");
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        Log.i("D_SDK_GEN_CRYPT", "  called():4");
        intent.putExtra("type", "generateAES256GCMCryptoKey");
        Log.i("D_SDK_GEN_CRYPT", "  called():5");
        intent.putExtra("uuid", uuid);
        Log.i("D_SDK_GEN_CRYPT", "  called():6");
        intent.putExtra("result", "");
        Log.i("D_SDK_GEN_CRYPT", "called():return:intent=" + intent);
        startActivity(intent);
      }
      Log.i("D_SDK_GEN_CRYPT", " <called()");
    }
   }

  /*
  @available(iOS 15.0, *)
  public func encryptUsingCryptoKey(plaintext: String, base64CryptoKey: String, returnType: String? = "base64") async -> String {
    // Construct the expected command to send
    var cmd:JSON = [
    "type":"CWI",
      "call":"encryptUsingCryptoKey",
      "params": [
    "plaintext": convertToJSONString(param: plaintext),
    "base64CryptoKey": convertToJSONString(param: base64CryptoKey),
    "returnType": convertToJSONString(param: returnType ?? "base64")
            ]
        ]

    // Run the command and get the response JSON object
    let responseObject = await runCommand(cmd: &cmd).value

    // Pull out the expect result string
    // TODO: Support buffer return type
    if (returnType == "base64") {
      return (responseObject.objectValue?["result"]?.stringValue)!
    }
    return "Error: Unsupported type!"
  }
  */
  // public func encryptUsingCryptoKey(plaintext: String, base64CryptoKey: String, returnType: String? = "base64") async -> String {
  // Default values enforced by overloading constructor
  public class EncryptUsingCryptoKey extends CallBaseTypes implements Serializable {
    private String paramStr;
    private String base64CryptoKey;

    // Required for polymorphism
    public EncryptUsingCryptoKey() {}

    // Default values enforced by overloading constructor
    public EncryptUsingCryptoKey(String plaintext, String base64CryptoKey) {
      this.base64CryptoKey = base64CryptoKey;
      paramStr = "";
      paramStr += "\"plaintext\":\"" + plaintext + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"base64\"";
    }
    public EncryptUsingCryptoKey(String plaintext, String base64CryptoKey, String returnType) {
      this.base64CryptoKey = base64CryptoKey;
      paramStr = "";
      paramStr += "\"plaintext\":\"" + plaintext + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"" + returnType + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"encryptUsingCryptoKey\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CRYPT_KEY_ENCRYPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Log.i("D_SDK_CRYPT_KEY_ENCRYPT", "called():result:" + result);
        // TODO returnType is missing?
        // String returnType = jsonReturnResultObject.get("returnType").toString();
        // if (!returnType.equals("base64")) {
        //   result = "Error: Unsupported type!";
        // }
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "encryptUsingCryptoKey");
        intent.putExtra("uuid", uuid);
        intent.putExtra("cryptoKey", base64CryptoKey);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "encryptUsingCryptoKey", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func decryptUsingCryptoKey(ciphertext: String, base64CryptoKey: String, returnType: String? = "base64") async -> String {
    // Construct the expected command to send
    var cmd:JSON = [
    "type":"CWI",
      "call":"decryptUsingCryptoKey",
      "params": [
    "ciphertext": convertToJSONString(param: ciphertext),
    "base64CryptoKey": convertToJSONString(param: base64CryptoKey),
    "returnType": convertToJSONString(param: returnType ?? "base64")
            ]
        ]

    // Run the command and get the response JSON object
    let responseObject = await runCommand(cmd: &cmd).value

    // Pull out the expect result string
    // TODO: Support buffer return type
    if (returnType == "base64") {
      return (responseObject.objectValue?["result"]?.stringValue)!
    }
    return "Error: Unsupported type!"
  }
  */
  // public func decryptUsingCryptoKey(ciphertext: String, base64CryptoKey: String, returnType: String? = "base64") async -> String {
  public class DecryptUsingCryptoKey extends CallBaseTypes implements Serializable {
    private String paramStr = "";
    private String base64CryptoKey;

    // Required for polymorphism
    public DecryptUsingCryptoKey() {}

    // Default values enforced by overloading constructor
    public DecryptUsingCryptoKey(String ciphertext, String base64CryptoKey) {
      this.base64CryptoKey = base64CryptoKey;
      paramStr = "";
      paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"base64\"";
    }
    public DecryptUsingCryptoKey(String ciphertext, String base64CryptoKey, String returnType) {
      this.base64CryptoKey = base64CryptoKey;
      paramStr = "";
      paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"" + returnType + "\"";
    }
    public String caller() {
      String cmdJSONString = "";
      cmdJSONString += "{\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"decryptUsingCryptoKey\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CRYPT_KEY_DECRYPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Log.i("D_SDK_CRYPT_KEY_DECRYPT", "called():result:" + result);
        // TODO returnType is missing?
        // String returnType = jsonReturnResultObject.get("returnType").toString();
        // if (!returnType.equals("base64")) {
        //   result = "Error: Unsupported type!";
        // }
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "decryptUsingCryptoKey");
        intent.putExtra("uuid", uuid);
        intent.putExtra("cryptoKey", base64CryptoKey);
        intent.putExtra("result", convertBase64ToString(result));
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "decryptUsingCryptoKey", "invalid JSON", "result");
      }
    }
  }

  /*
    // Creates a new action using CWI.createAction
    @available(iOS 15.0, *)
    public func createAction(inputs: JSON? = nil, outputs: JSON, description: String, bridges: JSON? = nil, labels: JSON? = nil) async -> JSON {

        let params:[String:JSON] = [
            "inputs": inputs ?? nil,
            "outputs": outputs,
            "description": convertToJSONString(param: description),
            "bridges": bridges ?? nil,
            "labels": labels ?? nil
        ]
        let paramsAsJSON:JSON = getValidJSON(params: params)

        // Construct the expected command to send
        var cmd:JSON = [
            "type":"CWI",
            "call":"createAction",
            "params": paramsAsJSON
        ]

        // Run the command and get the response JSON object
        let responseObject = await runCommand(cmd: &cmd).value

        return responseObject
    }
  */
  // public func createAction(inputs: JSON? = nil, outputs: JSON, description: String, bridges: JSON? = nil, labels: JSON? = nil) async -> JSON {
  public class CreateAction extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public CreateAction() {}

    // Default values enforced by overloading constructor
    public CreateAction(String outputs, String description) {
      paramStr = "";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + description + "\"";
     }
    public CreateAction(String inputs, String outputs, String description) {
      paramStr = "";
      paramStr += "\"inputs\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "inputs") + "\",";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + description + "\"";
    }
    public CreateAction(String inputs, String outputs, String description, String bridges) {
      paramStr = "";
      paramStr += "\"inputs\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "inputs") + "\",";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"bridges\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "bridges") + "\"";
    }
    public CreateAction(String inputs, String outputs, String description, String bridges, String labels) {
      paramStr = "";
      paramStr += "\"inputs\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "inputs") + "\",";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createAction", "description") + "\",";
      paramStr += "\"bridges\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "bridges") + "\",";
      paramStr += "\"labels\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "labels") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createAction\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CREATE_ACTION", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "createAction");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "createAction", "invalid JSON", "result");
      }
    }
  }

  /*
  // Creates an Hmac using CWI.createHmac
  @available(iOS 15.0, *)
  public func createHmac(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = "self", privileged: Bool? = nil) async -> String {
      // Construct the expected command to send with default values for nil params
      var cmd:JSON = [
          "type":"CWI",
          "call":"createHmac",
          "params": [
              "data": convertToJSONString(param: convertStringToBase64(data: data)),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false)
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result string
      let decryptedText:String = (responseObject.objectValue?["result"]?.stringValue)!
      return decryptedText
  }
  */
  // public func createHmac(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = "self", privileged: Bool? = nil) async -> String {
  // Default values enforced by overloading constructor
  public class CreateHmac extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public CreateHmac() {}

    // Default values enforced by overloading constructor
    public CreateHmac(String data, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"counterparty\":\"self\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"self\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description, String counterparty, String privileged) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\"";
   }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createHmac\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CREATE_HMAC", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "createHmac");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "createHmac", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func verifyHmac(data: String, hmac: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: Bool? = nil) async -> Bool {
      // Make sure data and hmac are base64 strings
      var data = data
      var hmac = hmac
      if (!base64StringRegex.matches(hmac)) {
          hmac = convertStringToBase64(data: hmac)
      }
      if (!base64StringRegex.matches(data)) {
          data = convertStringToBase64(data: data)
      }

      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"verifyHmac",
          "params": [
              "data": convertToJSONString(param: data),
              "hmac": convertToJSONString(param: hmac),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false)
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result boolean
      let verified:Bool = (responseObject.objectValue?["result"]?.boolValue)!
      return verified
  }
  */
  // Default values enforced by overloading constructor
  // public func verifyHmac(data: String, hmac: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: Bool? = nil) async -> Bool {
  public class VerifyHmac extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public VerifyHmac() {}

    // Default values enforced by overloading constructor
    public VerifyHmac(String data, String hmac, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description, String counterparty, String privileged) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"verifyHmac\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_VERIFY_HMAC", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "verifyHmac");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "verifyHmac", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func createSignature(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil) async -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"createSignature",
          "params": [
              "data": convertToJSONString(param: convertStringToBase64(data: data)),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false)
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result string
      let signature:String = (responseObject.objectValue?["result"]?.stringValue)!
      return signature
  }
  */
  //   public func createSignature(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil) async -> String {
  // Default values enforced by overloading constructor
  public class CreateSignature extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public CreateSignature() {}

    // Default values enforced by overloading constructor
    public CreateSignature(String data, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public CreateSignature(String data, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public CreateSignature(String data, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public CreateSignature(String data, String protocolID, String keyID, String description, String counterparty, String privileged) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createSignature\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CREATE_SIG", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "createSignature");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "createSignature", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func verifySignature(data: String, signature: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil, reason: String? = nil) async -> Bool{
      // Make sure data and signature are base64 strings
      var data = data
      var signature = signature
      if (!base64StringRegex.matches(data)) {
          data = convertStringToBase64(data: data)
      }
      if (!base64StringRegex.matches(signature)) {
          signature = convertStringToBase64(data: signature)
      }

      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"verifySignature",
          "params": [
              "data": convertToJSONString(param: data),
              "signature": convertToJSONString(param: signature),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false),
              "reason": try! JSON(reason ?? "")
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result boolean
      let verified:Bool = (responseObject.objectValue?["result"]?.boolValue)!
      return verified
  }
  */
  // public func verifySignature(data: String, signature: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil, reason: String? = nil) async -> Bool{
  // Default values enforced by overloading constructor
  public class VerifySignature extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public VerifySignature() {}

    // Default values enforced by overloading constructor
    public VerifySignature(String data, String signature, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public VerifySignature(String data, String signature, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public VerifySignature(String data, String signature, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"false\"";
    }
    public VerifySignature(String data, String signature, String protocolID, String keyID, String description, String counterparty, String privileged) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\"";
    }
    public VerifySignature(String data, String signature, String protocolID, String keyID, String description, String counterparty, String privileged, String reason) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"reason\":\"" + reason + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"verifySignature\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_VERIFY_HMAC", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "verifySignature");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "verifySignature", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func createCertificate(certificateType: String, fieldObject: JSON, certifierUrl: String, certifierPublicKey: String) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"createCertificate",
          "params": [
              "certificateType": convertToJSONString(param: certificateType),
              "fieldObject": fieldObject,
              "certifierUrl": convertToJSONString(param: certifierUrl),
              "certifierPublicKey": convertToJSONString(param: certifierPublicKey)
          ]
      ]

      // Run the command and get the response JSON object
      let signedCertificate = await runCommand(cmd: &cmd).value
      return signedCertificate
  }
  */
  // public func createCertificate(certificateType: String, fieldObject: JSON, certifierUrl: String, certifierPublicKey: String) async -> JSON {
  public class CreateCertificate extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public CreateCertificate() {}

    public CreateCertificate(String certificateType, String fieldObject, String certifierUrl, String certifierPublicKey) {
      paramStr = "";
      paramStr += "\"certificateType\":\"" + certificateType + "\",";
      paramStr += "\"fieldObject\":\"" + checkForJSONErrorAndReturnToApp(fieldObject, "createCertificate", "fieldObject") + "\",";
      paramStr += "\"certifierUrl\":\"" + certifierUrl + "\",";
      paramStr += "\"certifierPublicKey\":\"" + certifierPublicKey + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createCertificate\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CREATE_CERT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "createCertificate");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "createCertificate", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func getCertificates(certifiers: JSON, types: JSON) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"ninja.findCertificates",
          "params": [
              "certifiers": certifiers,
              "types": types
          ]
      ]

      // Run the command and get the response JSON object
      let certificates = await runCommand(cmd: &cmd).value
      return certificates
  }
  */
  // public func getCertificates(certifiers: JSON, types: JSON) async -> JSON {
  public class GetCertificates extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public GetCertificates() {}

    public GetCertificates(String certifiers, String types) {
      paramStr = "";
      paramStr += "\"certifiers\":\"" + checkForJSONErrorAndReturnToApp(certifiers, "ninja.findCertificates", "certifiers") + "\",";
      paramStr += "\"types\":\"" + checkForJSONErrorAndReturnToApp(types, "ninja.findCertificates", "types") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"ninja.findCertificates\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_GET_CERTS", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "ninja.findCertificates");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "ninja.findCertificates", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func proveCertificate(certificate: JSON, fieldsToReveal: JSON? = nil, verifierPublicIdentityKey: String) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"proveCertificate",
          "params": [
              "certificate": certificate,
              "fieldsToReveal": fieldsToReveal ?? nil,
              "verifierPublicIdentityKey": convertToJSONString(param: verifierPublicIdentityKey)
          ]
      ]


      // Run the command and get the response JSON object
      let provableCertificate = await runCommand(cmd: &cmd).value
      return provableCertificate
  }
  */
  // public func proveCertificate(certificate: JSON, fieldsToReveal: JSON? = nil, verifierPublicIdentityKey: String) async -> JSON {
  // Default values enforced by overloading constructor
  public class ProveCertificate extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public ProveCertificate() {}

    // Default values enforced by overloading constructor
    public ProveCertificate(String certificate, String verifierPublicIdentityKey) {
      paramStr = "";
      paramStr += "\"certificate\":\"" + checkForJSONErrorAndReturnToApp(certificate, "proveCertificate", "certificate") + "\",";
      paramStr += "\"verifierPublicIdentityKey\":\"" + verifierPublicIdentityKey + "\"";
    }
    public ProveCertificate(String certificate, String fieldsToReveal, String verifierPublicIdentityKey) {
      paramStr = "";
      paramStr += "\"certificate\":\"" + checkForJSONErrorAndReturnToApp(certificate, "proveCertificate", "certificate") + "\",";
      paramStr += "\"fieldsToReveal\":\"" + checkForJSONErrorAndReturnToApp(fieldsToReveal, "proveCertificate", "fieldsToReveal") + "\",";
      paramStr += "\"verifierPublicIdentityKey\":\"" + verifierPublicIdentityKey + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"proveCertificate\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_PROVE_CERT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "proveCertificate");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "proveCertificate", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func submitDirectTransaction(protocolID: String, transaction: JSON, senderIdentityKey: String, note: String, amount: Int, derivationPrefix: String? = nil) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"ninja.submitDirectTransaction",
          "params": [
              "protocol": convertToJSONString(param: protocolID),
              "transaction": transaction,
              "senderIdentityKey": convertToJSONString(param: senderIdentityKey),
              "note": convertToJSONString(param: note),
              "amount": try! JSON(amount),
              "derivationPrefix": try! JSON(derivationPrefix ?? "")
          ]
      ]

      // Run the command and get the response JSON object
      let provableCertificate = await runCommand(cmd: &cmd).value
      return provableCertificate
  }
  */
  //   public func submitDirectTransaction(protocolID: String, transaction: JSON, senderIdentityKey: String, note: String, amount: Int, derivationPrefix: String? = nil) async -> JSON {
  public class SubmitDirectTransaction extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public SubmitDirectTransaction() {}

    // Default values enforced by overloading constructor
    public SubmitDirectTransaction(String protocolID, String transaction, String senderIdentityKey, String note, String amount) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"transaction\":\"" + checkForJSONErrorAndReturnToApp(transaction, "submitDirectTransaction", "transaction") + "\",";
      paramStr += "\"senderIdentityKey\":\"" + senderIdentityKey + "\",";
      paramStr += "\"note\":\"" + note + "\",";
      paramStr += "\"amount\":\"" + amount + "\"";
    }
    public SubmitDirectTransaction(String protocolID, String transaction, String senderIdentityKey, String note, String amount, String derivationPrefix) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"transaction\":\"" + checkForJSONErrorAndReturnToApp(transaction, "submitDirectTransaction", "transaction") + "\",";
      paramStr += "\"senderIdentityKey\":\"" + senderIdentityKey + "\",";
      paramStr += "\"note\":\"" + note + "\",";
      paramStr += "\"amount\":\"" + amount + "\",";
      paramStr += "\"derivationPrefix\":\"" + derivationPrefix + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"submitDirectTransaction\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_SUBMIT_DIRECT_TX", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "submitDirectTransaction");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "submitDirectTransaction", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func getPublicKey(protocolID: JSON?, keyID: String? = nil, priviliged: Bool? = nil, identityKey: Bool? = nil, reason: String? = nil, counterparty: String? = "self", description: String? = nil) async -> String {
      // Construct the expected command to send
      // Added default values for dealing with nil params
      var cmd:JSON = [
          "type":"CWI",
          "call":"getPublicKey",
          "params": [
              "protocolID": protocolID ?? "",
              "keyID": try! JSON(keyID!),
              "priviliged": try! JSON(priviliged ?? false),
              "identityKey": try! JSON(identityKey ?? false),
              "reason": try! JSON(reason ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "description": try! JSON(description ?? "")
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result string
      let publicKey:String = (responseObject.objectValue?["result"]?.stringValue)!
      return publicKey
  }
  */
  // public func getPublicKey(protocolID: JSON?, keyID: String? = nil, privileged: Bool? = nil, identityKey: Bool? = nil, reason: String? = nil, counterparty: String? = "self", description: String? = nil) async -> String {
   public class GetPublicKey extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public GetPublicKey() {}

    // Default values enforced by overloading constructor
    public GetPublicKey(String protocolID) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"privileged\":\"false\",";
      paramStr += "\"identityKey\":\"false\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"false\",";
      paramStr += "\"identityKey\":\"false\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"false\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey) {
      Log.i("D_SDK_GET_PUBLIC_KEY", "called():identityKey=" + identityKey);
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey, String reason) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"reason\":\"" + reason + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey, String reason, String counterparty) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"reason\":\"" + reason + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey, String reason, String counterparty, String description) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      //paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      if (privileged != null && privileged.equals("true")) {
        paramStr += "\"privileged\":true,";
      } else {
        paramStr += "\"privileged\":false,";
      }
      if (identityKey != null && identityKey.equals("true")) {
        paramStr += "\"identityKey\":true,";
      } else {
        paramStr += "\"identityKey\":false,";
      }
      paramStr += "\"reason\":\"" + reason + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"description\":\"" + description + "\"";
      Log.i("D_SDK_GET_PUBLIC_KEY", "GetPublicKey():paramStr=" + paramStr);
    }
    public String caller() {
      Log.i("D_SDK_GET_PUBLIC_KEY", "caller()");
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"getPublicKey\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      Log.i("D_SDK_GET_PUBLIC_KEY", "caller():cmdJSONString=" + cmdJSONString);
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_GET_PUBLIC_KEY", "called()::returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        Log.i("D_SDK_GET_PUBLIC_KEY", "called():create intent");
        String uuid = jsonReturnResultObject.get("uuid").toString();
        Log.i("D_SDK_GET_PUBLIC_KEY", "called():uuid=" + uuid);
        String result = jsonReturnResultObject.get("result").toString();
        Log.i("D_SDK_GET_PUBLIC_KEY", "called():result=" + result);
        // Intent intent = new Intent(SDKActivity.this, new SDKActivity.CryptonActivity.getClass());
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        Log.i("D_SDK_GET_PUBLIC_KEY", "called():created intent:" + intent);
        intent.putExtra("type", "getPublicKey");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        Log.i("D_SDK_GET_PUBLIC_KEY", "called():call startActivity():intent=" + intent);
        startActivity(intent);
        finish();
      } catch (JSONException e) {
        Log.i("D_SDK_GET_PUBLIC_KEY", "called():ERROR raised");
        // returnError(returnResult, "getPublicKey", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func getVersion() async -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"getVersion",
          "params": []
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result string
      let version:String = (responseObject.objectValue?["result"]?.stringValue)!
      return version
  }
  */
  // public func getVersion() async -> String {
  public class GetVersion extends CallBaseTypes implements Serializable {

    public String caller() {
      return "{\"type\":\"CWI\",\"call\":\"getVersion\",\"params\":{},\"id\":\"uuid\"}";
    }
    public void called(String returnResult) {
      Log.i("D_SDK_GET_VERSION", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "generateCryptoKey");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "generateCryptoKey", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func createPushDropScript(fields: JSON, protocolID: String, keyID: String) async -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"pushdrop.create",
          "params": [
              "fields": fields,
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID)
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value
      let script:String = (responseObject.objectValue?["result"]?.stringValue)!
      return script
  }
  */
  // public func createPushDropScript(fields: JSON, protocolID: String, keyID: String) async -> String {
  public class CreatePushDropScript extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public CreatePushDropScript() {}

    public CreatePushDropScript(String fields, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"fields\":\"" + checkForJSONErrorAndReturnToApp(fields, "createPushDropScript", "fields") + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createPushDropScript\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CREATE_PUSH_DROP", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "createPushDropScript");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "createPushDropScript", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func parapetRequest(resolvers: JSON, bridge: String, type: String, query: JSON) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"parapet",
          "params": [
              "resolvers": resolvers,
              "bridge": convertToJSONString(param: bridge),
              "type": convertToJSONString(param: type),
              "query": query
            ]
          ]

      // Run the command and get the response JSON object
      let result = await runCommand(cmd: &cmd).value
      return result
  }
  */
  // public func parapetRequest(resolvers: JSON, bridge: String, type: String, query: JSON) async -> JSON {
  public class ParapetRequest extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public ParapetRequest() {}

    public ParapetRequest(String resolvers, String bridge, String type, String query) {
      paramStr = "";
      paramStr += "\"resolvers\":\"" + checkForJSONErrorAndReturnToApp(resolvers, "parapetRequest", "resolvers") + "\",";
      paramStr += "\"bridge\":\"" + bridge + "\",";
      paramStr += "\"type\":\"" + type + "\",";
      paramStr += "\"query\":\"" + checkForJSONErrorAndReturnToApp(query, "parapetRequest", "query") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"parapetRequest\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_PARA_REQUEST", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "parapetRequest");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "parapetRequest", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func downloadUHRPFile(url: String, bridgeportResolvers: JSON) async -> Data? {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"downloadFile",
          "params": [
              "url": convertToJSONString(param: url),
              "bridgeportResolvers": bridgeportResolvers
          ]
      ]

      // TODO: Determine return type and best way to transfer large bytes of data.
      // Run the command and get the response JSON object
      let result = await runCommand(cmd: &cmd).value

      // Convert the array of JSON objects to an Array of UInt8s and then to a Data object
      // TODO: Optimize further
      if let arrayOfJSONObjects = result.objectValue?["result"]?.objectValue?["data"]?.objectValue?["data"]?.arrayValue {
          let byteArray:[UInt8] = arrayOfJSONObjects.map { UInt8($0.doubleValue!)}
          return Data(byteArray)
      }
      return nil
  }
  */
  // public func downloadUHRPFile(url: String, bridgeportResolvers: JSON) async -> Data? {
  public class DownloadUHRPFile extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public DownloadUHRPFile() {}

    public DownloadUHRPFile(String url, String bridgeportResolvers) {
      paramStr = "";
      paramStr += "\"url\":\"" + url + "\",";
      paramStr += "\"bridgeportResolvers\":\"" + checkForJSONErrorAndReturnToApp(bridgeportResolvers, "downloadUHRPFile", "bridgeportResolvers") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"downloadUHRPFile\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_PARA_REQUEST", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "downloadUHRPFile");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "downloadUHRPFile", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func newAuthriteRequest(params: JSON, requestUrl: String, fetchConfig: JSON) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"newAuthriteRequest",
          "params": [
              "params": params,
              "requestUrl": convertToJSONString(param: requestUrl),
              "fetchConfig": fetchConfig
          ]
      ]

      // TODO: Determine return type and best way to transfer large bytes of data.
      // Run the command and get the response JSON object
      let result = await runCommand(cmd: &cmd).value
      return result
  }
  */
  // public func newAuthriteRequest(params: JSON, requestUrl: String, fetchConfig: JSON) async -> JSON {
  public class NewAuthriteRequest extends CallBaseTypes implements Serializable {

    private String paramStr = "";

    // Required for polymorphism
    public NewAuthriteRequest() {}

    public NewAuthriteRequest(String params, String requestUrl, String fetchConfig) {
      paramStr = "";
      paramStr += "\"params\":\"" + checkForJSONErrorAndReturnToApp(params, "newAuthriteRequest", "params") + "\",";
      paramStr += "\"requestUrl\":\"" + requestUrl + "\",";
      paramStr += "\"fetchConfig\":\"" + checkForJSONErrorAndReturnToApp(fetchConfig, "newAuthriteRequest", "fetchConfig") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"newAuthriteRequest\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_NEW_AUTHRITE", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "newAuthriteRequest");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "newAuthriteRequest", "invalid JSON", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func createOutputScriptFromPubKey(derivedPublicKey: String) async -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"createOutputScriptFromPubKey",
          "params": [
              "derivedPublicKey": convertToJSONString(param: derivedPublicKey)
          ]
      ]

      // Run the command and get the response as a string
      let responseObject = await runCommand(cmd: &cmd).value
      return (responseObject.objectValue?["result"]?.stringValue)!
  }
  */
  // public func createOutputScriptFromPubKey(derivedPublicKey: String) async -> String {
  public class CreateOutputScriptFromPubKey extends CallBaseTypes implements Serializable {
    private String paramStr = "";

    // Required for polymorphism
    public CreateOutputScriptFromPubKey() {}

    public CreateOutputScriptFromPubKey(String derivedPublicKey) {
      paramStr = "\"derivedPublicKey\":\"" + derivedPublicKey + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createOutputScriptFromPubKey\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("D_SDK_CREATE_OUTPUT_SCRIPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "createOutputScriptFromPubKey");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        returnError(returnResult, "createOutputScriptFromPubKey", "invalid JSON", "result");
      }
    }
  }

  /*** Helper methods ***/
  private String checkIsBase64(String str) {
    String returnStr = str;
    if (!isBase64(str)) {
      returnStr = convertStringToBase64(str);
    }
    return returnStr;
  }
  public void returnError(String str, String type, String message, String field){
    Intent intent = new Intent(SDKActivity.this, classObject.getClass());
    intent.putExtra("result", str);
    intent.putExtra("type", type);
    intent.putExtra("uuid", uuid);
    intent.putExtra("error", message);
    intent.putExtra("field", field);
    startActivity(intent);
  }
  private String checkForJSONErrorAndReturnToApp(String str, String type, String field) {
    String resultStr = "";
    try {
      new JSONObject(str);
      resultStr = str;
    } catch (JSONException e) {
      returnError(str, type, "invalid JSON", field);
    }
    return resultStr;
  }

  /*
  // Returns a JSON object with non-null values
  func getValidJSON(params: [String: JSON]) -> JSON {
    var paramsAsJSON:JSON = []
    for param in params {
      if (param.value != nil) {
        paramsAsJSON = paramsAsJSON.merging(with: [param.key: param.value])
      }
    }
    return paramsAsJSON
  }
  // TODO Not needed as JSON has to be passed as strings by the App
  private JSONObject getValidJSON(Stack<JSONObject> params) {
    while(!params.isEmpty()) {
      if (params.pop() != null) {
        JSONFormatStr
      }
    }
  }
  // Helper function which returns a JSON type string
  func convertToJSONString(param: String) -> JSON {
    return try! JSON(param)
  }
  */
  private JSONObject convertToJSONString(String param) {
    try {
      return new JSONObject(param);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO Additional functionality as per iOS version
  /*
  // Generates a secure random base64 string base on provided byte length
  public func generateRandomBase64String(byteCount: Int) -> String {
    var bytes = [UInt8](repeating: 0, count: byteCount)
    let status = SecRandomCopyBytes(
      kSecRandomDefault,
      byteCount,
      &bytes
        )
    // A status of errSecSuccess indicates success
    if status != errSecSuccess {
      return "Error"
    }
    let data = Data(bytes)
    return data.base64EncodedString()
  }
  */
  public static String generateRandomBase64String(int length) {
    int byteLength = ((length + 3) / 4) * 3; // base 64: 3 bytes = 4 chars
    byte[] byteVal = new byte[byteLength];
    new Random(System.currentTimeMillis()).nextBytes(byteVal);

    // Change '/' and '\' with '$' in case the string is used as file name
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new String(Base64.getEncoder().encode(byteVal), 0, length).replace('/', '$');
    }
    return "Error";
  }
  public static String convertBase64ToString(String codedStr){
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      Log.i("D_SDK", "convertBase64ToString()");
      byte[] decodedStr = Base64.getUrlDecoder().decode(codedStr);
      return new String(decodedStr);
    }
    return "";
  }
  public void doJavaScript(WebView webview, String javascript) {
    Log.i("D_SDK", "doJavaScript():javascript:" + javascript);
    webview.evaluateJavascript(javascript, null);
  }

  // Generic 'run' command
  public void runCommand(CallBaseTypes type, String uuid, String actualType, String portal) {
    Log.i("D_SDK", ">runCommand():type:" + type + ",uuid=" + uuid + ",actualType=" + actualType + ",portal=" + portal);
    callTypes.update(type, actualType, portal);
    String s = "window.postMessage(" + callTypes.caller().replace("uuid", uuid) + ")";
    Log.i("D_SDK", " runCommand():s=" + s);
    doJavaScript(
      webview,
      s
    );
    Log.i("D_SDK", "<runCommand()");
  }

  // Convert the string to base64 so it can be passed over the net
  public static String convertStringToBase64(String str) {
    byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
    if (
      android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
    ) {
      return new String(Base64.getEncoder().encodeToString(byteArray).getBytes());
    }
    return "Error";
  }

  // TODO (Not currently required) Helper to format complex JSON objects into an appropriate string
  public static String JSONFormatStr(String name, String key) {
    try {
      return new JSONObject("{\"" + name + "\":\"" + key + "\"}").toString();
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }
  private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
    Object ret = null;
    ObjectInputStream in = null;
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      in = new ObjectInputStream(bis);
      ret = in.readObject();
    } catch(IOException e) {
      throw e;
    } catch(ClassNotFoundException e) {
      throw e;
    }
    return ret;
  }
  private static byte[] convertToBytes(Object object) {
    //Log.i("D_SDK", ">convertToBytes():");
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      //Log.i("D_SDK", " convertToBytes():1");
      ObjectOutputStream out = new ObjectOutputStream(bos);
      //Log.i("D_SDK", " convertToBytes():2");
      out.writeObject(object);
      //Log.i("D_SDK", " convertToBytes():3");
    } catch (IOException e) {
      Log.i("D_SDK", "ERROR:convertToBytes():" + e);
      Log.i("D_SDK", "<convertToBytes():" + bos.toByteArray());
      return bos.toByteArray();
    }
    //Log.i("D_SDK", "<convertToBytes():" + bos.toByteArray());
    return bos.toByteArray();
  }

  // Called by App to pass over calling class to be used by Intent callback
  // Raises compile time warning as not used
  public static String setInstance(Object instance) {
    //Log.i("D_SDK", ">setInstance():instance=" + instance);
    byte[] bytes = {};
    try {
      bytes = convertToBytes(instance);
      //Log.i("D_SDK", " setInstance():bytes=" + bytes);
    } catch (Exception e) {
      Log.e("D_SDK", " setInstance():ERROR:e=" + e);
    }
    byte[] base64Encoded = null;
    // Convert the string to base64 so it can be passed over the net
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      base64Encoded = Base64.getEncoder().encodeToString(bytes).getBytes();
    }
    byte[] finalBase64Encoded = base64Encoded;
    //Log.i("D_SDK", "<setInstance():finalBase64Encoded=" + finalBase64Encoded);
    return new String(finalBase64Encoded);
  }
  public static Object getInstance(String instanceStr) {
    //Log.i("D_SDK", ">getInstance()");
    byte[] bytes = decode(instanceStr, DEFAULT);
    //Log.i("D_SDK", " getInstance():created bytes=" + bytes.toString());
    try {
      //Log.i("D_SDK", " getInstance():create classObject");
      Object o = convertFromBytes(bytes);
      //Log.i("D_SDK", " getInstance():created instance=" + o);
      //Log.i("D_SDK", "<getInstance()");
      return o;
    } catch (IOException e) {
      Log.e("D_SDK", " getInstance():throw new RuntimeException:e=" + e);
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      Log.e("D_SDK", " getInstance():throw new ClassNotFoundException:e=" + e);
      throw new RuntimeException(e);
    }
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i("D_SDK", ">onCreate()");
    super.onCreate(savedInstanceState);
    Log.i("D_SDK", " onCreate():openBabbage:" + openBabbage);
    if (openBabbage) {
      Log.i("D_SDK", " onCreate():openBabbage:return");
      return;
    }
    getSupportActionBar().hide();
    Intent intent = getIntent();
    final String[] type = {intent.getStringExtra("type")};
    String portal = intent.getStringExtra("portal");
    Log.i("D_SDK", " onCreate():type:" + type[0]);
    Log.i("D_SDK", " onCreate():portal:" + portal);
    String url = intent.getStringExtra("url");
    Log.i("D_SDK", " onCreate():url:" + url);
    String instanceStr = intent.getStringExtra("waitingInstance");
    // if (!type.equals("portal")) {
    if (openBabbage || type[0].equals("waitingInstance") || type[0].equals("generateAES256GCMCryptoKey") || type[0].equals("getIdentityKey") || type[0].equals("portal") || type[0].equals("waitForAuthentication") || portal.equals("portal") || portal.equals("waitForAuthentication")) {
      // if (openBabbage || type.equals("generateAES256GCMCryptoKey") ||  type.equals("portal") || type.equals("waitForAuthentication") || portal.equals("portal") || portal.equals("waitForAuthentication")) {
      Log.i("D_SDK", " onCreate():display Webview");
      Log.i("D_SDK_STACK", " onCreate():portal:" + portal);
      if (instanceStr != null) {
        //05Sep2023-2039
        //type[0] = "portal";
        openBabbage = true;
      }
      if (portal.equals("portal")) {
        Log.i("D_SDK", " onCreate():delayed while webview displayed");
        Log.i("D_SDK_STACK", " onCreate():waitingCallTypes:" + waitingCallType);
        // Need to start the child thread to call IsAuthenticated run command
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            WorkerThread workerThread = new WorkerThread();
            workerThread.start();
          }
        }, 20000);
      }
      Log.i("D_SDK", " onCreate():COMMENT OUT openBabbage = true");
    } else {
      Log.i("D_SDK", " onCreate():display App");
      finish();
    }
    callTypes = new CallTypes();
    setContentView(R.layout.activity_sdk);
    webview = findViewById(R.id.web_html);
    webview.setWebChromeClient (
            new WebChromeClient() {
              @Override
              public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Log.i("D_SDK", "onJsConfirm():result:" + result);
                return super.onJsConfirm(view, url, message, result);
              }
              @Override
              public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.i("D_SDK", "onJsAlert():result" + result);
                return super.onJsAlert(view, url, message, result);
              }
            }
    );
    webview.setWebViewClient (
            new WebViewClient() {
              @Override
              public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.i("D_SDK", ">onPageStarted():type=" + type[0]);
                super.onPageStarted(view, url, favicon);
                String callingClass = intent.getStringExtra("callingClass");
                Log.i("D_SDK", " onPageStarted():created callingClass=" + callingClass);
                classObject = getInstance(callingClass);
                if (instanceStr != null || type[0].equals("portal") || type[0].equals("waitForAuthentication")) {
                  Log.i("D_SDK", " onPageStarted():set openBabbage=true");
                  openBabbage = true;
                  return;
                } else {
                  final Handler handler = new Handler(Looper.getMainLooper());
                  handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      Log.i("D_SDK", " onPageStarted():pageFinished=" + pageFinished);
                      if (!pageFinished) {
                        Log.i("D_SDK", " onPageStarted():call onPageFinished()");
                        onPageFinished(view, url);
                      }
                    }
                  }, 5000);
                  finish();
                }
                Log.i("D_SDK", "<onPageStarted()");
              }

              @Override
              public void onPageFinished(WebView view, String url) {
                Log.i("D_SDK", ">onPageFinished():type=" + type[0]);
                pageFinished = true;
                super.onPageFinished(view, url);
                if (type[0].equals("portal")) {
                  Log.i("D_SDK", "<onPageFinished()");
                  return;
                }
                if (instanceStr != null) {
                  Log.i("D_SDK", " onCreate():waitingInstance is not null");
                  CallBaseTypes instance = (CallBaseTypes)getInstance(instanceStr);
                  Log.i("D_SDK_WAITING", " onPageFinished():call runCommand():instance=" + instance);
                  String className = instance.getClass().getName();
                  Log.i("D_SDK_WAITING", " onPageFinished():call runCommand():className=" + className);
                  String[] classNames  = className.split("[.]", 0);
                  Log.i("D_SDK_WAITING", " onPageFinished():call runCommand():classNames.length=" + classNames.length + ",classNames=" + classNames);
                  type[0] = classNames[3].substring(0, 1).toLowerCase() + classNames[3].substring(1);
                  Log.i("D_SDK_WAITING", " onPageFinished():call runCommand():type[0]=" + type[0]);
                  String s = "window.postMessage(" + instance.caller().replace("uuid", UUID.randomUUID().toString()) + ")";
                  Log.i("D_SDK_WAITING", " onPageFinished():s=" + s);
                  doJavaScript(webview, s);
                  Log.i("D_SDK_WAITING", " onPageFinished():called runCommand():type=" + type[0]);
                  String waitingInstanceStr = intent.getStringExtra("waitingNextInstance");
                  if (waitingInstanceStr != null) {
                    Log.i("D_SDK", " onCreate():waitingNextInstance is not null");
                    CallBaseTypes waitingInstance = (CallBaseTypes) getInstance(waitingInstanceStr);
                    Log.i("D_SDK_WAITING", " onPageFinished():call runCommand():waitingInstance=" + waitingInstance);
                    Log.i("D_SDK", " onPageFinished():waitingInstance before waitingCallType=" + waitingCallType);
                    waitingCallType.push(waitingInstance);
                    Log.i("D_SDK", " onPageFinished():waitingInstance after waitingCallType=" + waitingCallType);
                  }
                  type[0] = "portal";
                  openBabbage = true;
                }
                //25Aug2023
                //String waitingInstance = "";
                uuid = intent.getStringExtra("uuid");
                if(!type[0].equals("portal")) {
                  if (type[0].equals("waitForAuthentication")) {
                    runCommand(new WaitForAuthentication(SDKActivity.this), uuid, type[0], portal);
                  }
                  if (portal.equals("waitForAuthentication")) {
                    runCommand(new WaitForAuthentication(SDKActivity.this), uuid, type[0], "");
                  }
                  if (type[0].equals("isAuthenticated")) {
                    runCommand(new IsAuthenticated(SDKActivity.this), uuid, type[0], portal);
                  }
                  Log.i("D_SDK", " onPageFinished():created openBabbage=" + openBabbage);
                  if (!openBabbage) {
                    // Process calling class
                  }
                //} else {
                  Log.i("D_SDK", " onPageFinished():type=" + type[0]);
                  if (type[0].equals("encrypt")) {
                    Encrypt encrypt = null;
                    String counterparty = intent.getStringExtra("counterparty");
                    SDKActivity.counterparty = counterparty;
                    Log.i("D_SDK", " onPageFinished():encrypt:SDKActivity.counterparty=" + SDKActivity.counterparty);
                    if (counterparty == null) {
                      encrypt =new Encrypt(SDKActivity.this);
                      encrypt.process(
                              intent.getStringExtra("plaintext"),
                              intent.getStringExtra("protocolID"),
                              intent.getStringExtra("keyID")
                      );
                    } else {
                      //03Sep2023-1402
                      //*** Added Experimental Test ***//
                      encrypt =new Encrypt(SDKActivity.this);
                      encrypt.process(
                              intent.getStringExtra("plaintext"),
                              intent.getStringExtra("protocolID"),
                              intent.getStringExtra("keyID"),
                              intent.getStringExtra("counterparty")
                      );
                    }
                    Log.i("D_SDK", " onPageFinished():encrypt before waitingCallType=" + waitingCallType);
                    waitingCallType.push(encrypt);
                    Log.i("D_SDK", " onPageFinished():encrypt after waitingCallType=" + waitingCallType);
                    //03Sep2023-1402
                    //*** Added Experimental Test ***//
                    //babbageWaitingCallType.push(waitingCallType.get(0));
                  }
                  if (type[0].equals("decrypt")) {
                    String counterparty = intent.getStringExtra("counterparty");
                    SDKActivity.counterparty = counterparty;
                    Log.i("D_SDK", " onPageFinished():decrypt:SDKActivity.counterparty=" + SDKActivity.counterparty);
                    if (counterparty == null) {
                      waitingCallType.push(
                              new Decrypt(
                                      intent.getStringExtra("ciphertext"),
                                      intent.getStringExtra("protocolID"),
                                      intent.getStringExtra("keyID")
                              )
                      );
                    } else {
                      waitingCallType.push(
                              new Decrypt(
                                      intent.getStringExtra("ciphertext"),
                                      intent.getStringExtra("protocolID"),
                                      intent.getStringExtra("keyID"),
                                      intent.getStringExtra("counterparty")
                              )
                      );
                    }
                  }
                  if (type[0].equals("generateAES256GCMCryptoKey")) {
                    Log.i("D_SDK", "call push GenerateAES256GCMCryptoKey()");
                    waitingCallType.push(new GenerateAES256GCMCryptoKey());
                    Log.i("D_SDK", "called push GenerateAES256GCMCryptoKey()");
                  }
                  if (type[0].equals("encryptUsingCryptoKey")) {
                    // Log.i("D_SDK_ENCRYPT_KEY", "push EncryptUsingCryptoKey()");
                    String base64CryptoKey = intent.getStringExtra("base64CryptoKey");
                    if (!isBase64(base64CryptoKey)) {
                      returnError(base64CryptoKey, "encryptUsingCryptoKey", "invalid base64 crypto key", "base64CryptoKey");
                    }
                    waitingCallType.push(
                            new EncryptUsingCryptoKey(
                                    intent.getStringExtra("plaintext"),
                                    base64CryptoKey,
                                    !intent.getStringExtra("returnType").equals("") ? intent.getStringExtra("returnType") : "base64"
                            )
                    );
                  }
                  if (type[0].equals("decryptUsingCryptoKey")) {
                    // Log.i("D_SDK", "push DecryptUsingCryptoKey()");
                    String base64CryptoKey = intent.getStringExtra("base64CryptoKey");
                    if (!isBase64(base64CryptoKey)) {
                      returnError(base64CryptoKey, "decryptUsingCryptoKey", "invalid base64 crypto key", "base64CryptoKey");
                    }
                    waitingCallType.push(
                            new DecryptUsingCryptoKey(
                                    intent.getStringExtra("ciphertext"),
                                    base64CryptoKey,
                                    !intent.getStringExtra("returnType").equals("") ? intent.getStringExtra("returnType") : "base64"
                            )
                    );
                  }
                  if (type[0].equals("createAction")) {
                    waitingCallType.push(
                            new CreateAction(
                                    intent.getStringExtra("inputs"),
                                    intent.getStringExtra("outputs"),
                                    intent.getStringExtra("description"),
                                    intent.getStringExtra("bridges"),
                                    intent.getStringExtra("labels")
                            )
                    );
                  }
                  if (type[0].equals("createHmac")) {
                    waitingCallType.push(
                            new CreateHmac(
                                    intent.getStringExtra("data"),
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("keyID"),
                                    intent.getStringExtra("description"),
                                    !intent.getStringExtra("counterparty").equals("") ? intent.getStringExtra("counterparty") : "self",
                                    intent.getStringExtra("privileged")
                            )
                    );
                  }
                  if (type[0].equals("verifyHmac")) {
                    waitingCallType.push(
                            new VerifyHmac(
                                    intent.getStringExtra("data"),
                                    intent.getStringExtra("hmac"),
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("keyID"),
                                    intent.getStringExtra("description"),
                                    intent.getStringExtra("counterparty"),
                                    intent.getStringExtra("privileged")
                            )
                    );
                  }
                  if (type[0].equals("createSignature")) {
                    waitingCallType.push(
                            new CreateSignature(
                                    intent.getStringExtra("data"),
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("keyID"),
                                    intent.getStringExtra("description"),
                                    intent.getStringExtra("counterparty"),
                                    intent.getStringExtra("privileged")
                            )
                    );
                  }
                  if (type[0].equals("verifySignature")) {
                    waitingCallType.push(
                            new VerifySignature(
                                    intent.getStringExtra("data"),
                                    intent.getStringExtra("signature"),
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("keyID"),
                                    intent.getStringExtra("description"),
                                    intent.getStringExtra("counterparty"),
                                    intent.getStringExtra("privileged"),
                                    intent.getStringExtra("reason")
                            )
                    );
                  }
                  if (type[0].equals("createCertificate")) {
                    waitingCallType.push(
                            new CreateCertificate(
                                    intent.getStringExtra("certificateType"),
                                    intent.getStringExtra("fieldObject"),
                                    intent.getStringExtra("certifierUrl"),
                                    intent.getStringExtra("certifierPublicKey")
                            )
                    );
                  }
                  if (type[0].equals("getCertificates")) {
                    waitingCallType.push(
                            new GetCertificates(
                                    intent.getStringExtra("certifiers"),
                                    intent.getStringExtra("types")
                            )
                    );
                  }
                  if (type[0].equals("proveCertificate")) {
                    waitingCallType.push(
                            new ProveCertificate(
                                    intent.getStringExtra("certificate"),
                                    intent.getStringExtra("fieldsToReveal"),
                                    intent.getStringExtra("verifierPublicIdentityKey")
                            )
                    );
                  }
                  if (type[0].equals("submitDirectTransaction")) {
                    waitingCallType.push(
                            new SubmitDirectTransaction(
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("transaction"),
                                    intent.getStringExtra("senderIdentityKey"),
                                    intent.getStringExtra("note"),
                                    intent.getStringExtra("amount"),
                                    intent.getStringExtra("derivationPrefix")
                            )
                    );
                  }
                  if (type[0].equals("getIdentityKey")) {
                    Log.i("D_SDK", "push getIdentityKey()");
                    waitingCallType.push(
                            new GetPublicKey(
                                    "identity key",
                                    intent.getStringExtra(""),
                                    intent.getStringExtra(""),
                                    "true",
                                    "",
                                    "",
                                    ""
                            )
                    );
                  }
                  if (type[0].equals("getPublicKey")) {
                    waitingCallType.push(
                            new GetPublicKey(
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("keyID"),
                                    intent.getStringExtra("privileged"),
                                    intent.getStringExtra("identityKey"),
                                    intent.getStringExtra("reason"),
                                    !intent.getStringExtra("counterparty").equals("") ? intent.getStringExtra("counterparty") : "self",
                                    intent.getStringExtra("description")
                            )
                    );
                  }
                  if (type[0].equals("getVersion")) {
                    waitingCallType.push(new GetVersion());
                  }
                  if (type[0].equals("createPushDropScript")) {
                    waitingCallType.push(
                            new CreatePushDropScript(
                                    intent.getStringExtra("fields"),
                                    intent.getStringExtra("protocolID"),
                                    intent.getStringExtra("keyID")
                            )
                    );
                  }
                  if (type[0].equals("parapetRequest")) {
                    waitingCallType.push(
                            new ParapetRequest(
                                    intent.getStringExtra("resolvers"),
                                    intent.getStringExtra("bridge"),
                                    intent.getStringExtra("type"),
                                    intent.getStringExtra("query")
                            )
                    );
                  }
                  if (type[0].equals("downloadUHRPFile")) {
                    waitingCallType.push(
                            new DownloadUHRPFile(
                                    intent.getStringExtra("url"),
                                    intent.getStringExtra("bridgeportResolvers")
                            )
                    );
                  }
                  if (type[0].equals("newAuthriteRequest")) {
                    waitingCallType.push(
                            new NewAuthriteRequest(
                                    intent.getStringExtra("params"),
                                    intent.getStringExtra("requestUrl"),
                                    intent.getStringExtra("fetchConfig")
                            )
                    );
                  }
                  if (type[0].equals("createOutputScriptFromPubKey")) {
                    waitingCallType.push(
                            new CreateOutputScriptFromPubKey(
                                    intent.getStringExtra("derivedPublicKey")
                            )
                    );
                  }
                  Log.i("D_SDK", " onPageFinished():before end point:waitingCallType.isEmpty()=" + waitingCallType.isEmpty());
                  if (type[0].equals("portal")) {
                    if (!waitingCallType.isEmpty()) {
                      babbageWaitingCallType.push(waitingCallType.get(0));
                    }
                    Log.i("D_SDK_BABBAGE_STACK", "onPageFinished():after push():babbageWaitingCallType=" + babbageWaitingCallType);
                    Log.i("D_SDK", "<onPageFinished():type=" + type[0]);
                    finish();
                  }
                }
                Log.i("D_SDK_STACK", " onPageFinished():waitingCallTypes:" + waitingCallType);
                Log.i("D_SDK", " onPageFinished():before queue type=" + type[0]);
                if (!type[0].equals("waitForAuthentication") && !type[0].equals("portal") && !portal.equals("waitForAuthentication")) {
                  //24Aug2023-18:59
                  if (type[0].equals("portal") || portal.equals("openBabbage")) {
                    Log.i("D_SDK", " onPageFinished():don't queue as portal type=" + type[0]);
                    //25Aug2023
                    //waitingCallType.push(waitingCallType.get(0));
                  } else {
                    if (!waitingCallType.isEmpty()) {
                      babbageWaitingCallType.push(waitingCallType.get(0));
                    }
                    Log.i("D_SDK_BABBAGE_STACK", "onPageFinished():after push():babbageWaitingCallType=" + babbageWaitingCallType);
                    waitingCallType.push(new IsAuthenticated(SDKActivity.this));
                  }
                  // Need to start the child thread to call IsAuthenticated run command
                  WorkerThread workerThread = new WorkerThread();
                  workerThread.start();
                }
                Log.i("D_SDK_STACK", " onPageFinished():waitingCallTypes:" + waitingCallType);
                Log.i("D_SDK", "<onPageFinished():type=" + type[0]);
              }
            }
    );

    // Process waiting command call
    mainThreadHandler =
            new Handler(Looper.myLooper()) {
              @Override
              public void handleMessage(Message msg) {
                if (!waitingCallType.isEmpty() && msg.what == 1) {
                  runCommand(waitingCallType.pop(), uuid, type[0], portal);
                }
              }
            };

    // required for most of our React modules (Hades, Prosperity etc.)
    webview.getSettings().setDomStorageEnabled(true);
    webview.getSettings().setJavaScriptEnabled(true);
    webview.getSettings().setSupportMultipleWindows(true);
    webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    webview.getSettings().setUserAgentString("babbage-webview-inlay");
    webview.addJavascriptInterface(new WebAppInterface(),"androidMessageHandler");
    webview.loadUrl(url);
  }
  public void returnUsingWaitingIntent(String waitingTypeInstance, String waitingNextTypeInstance, String portal) {
    Log.i("D_SDK_RETURN_WAITING_INTENT", ">returnUsingWaitingIntent():portal=" + portal + ",waitingTypeInstance=" + waitingTypeInstance +  ",waitingNextTypeInstance=" + waitingNextTypeInstance);
    Intent intent = null;
    try {
      intent = new Intent(this, classObject.getClass());
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    intent.putExtra("waitingInstance", waitingTypeInstance);
    intent.putExtra("waitingNextInstance", waitingNextTypeInstance);
    intent.putExtra("portal", portal);
    try {
      startActivity(intent);
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    Log.i("D_SDK_RETURN_WAITING_INTENT", "<returnUsingWaitingIntent():called startActivity()");
  }
  public void returnUsingWaitingIntent(String waitingTypeInstance, String portal) {
    Log.i("D_SDK_RETURN_WAITING_INTENT", ">returnUsingWaitingIntent():portal=" + portal +",waitingTypeInstance=" + waitingTypeInstance);
    Intent intent = null;
    try {
      intent = new Intent(this, classObject.getClass());
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    intent.putExtra("waitingInstance", waitingTypeInstance);
    intent.putExtra("portal", portal);
    try {
      startActivity(intent);
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    Log.i("D_SDK_RETURN_WAITING_INTENT", "<returnUsingWaitingIntent():called startActivity()");
  }
  public void returnUsingIntent(String type, String result) {
    Log.i("D_SDK_RETURN_INTENT", ">returnUsingIntent():type=" + type + ",result=" + result);
    returnUsingIntent(type, result, "");
  }
  public void returnUsingIntent(String type, String result, String portal) {
    Log.i("D_SDK_RETURN_INTENT", ">returnUsingIntent():type=" + type + ",portal=" + portal + ",result=" + result);
    Intent intent = null;
    try {
      intent = new Intent(this, classObject.getClass());
    } catch (Exception e) {
      Log.i("D_SDK_RETURN_INTENT", " returnUsingIntent():ERROR:e=" + e);
    }
    //Log.i("D_SDK_RETURN_INTENT", "returnUsingIntent():2");
    intent.putExtra("type", type);
    //Log.i("D_SDK_RETURN_INTENT", "returnUsingIntent():3");
    intent.putExtra("uuid", uuid);
    //Log.i("D_SDK_RETURN_INTENT", "returnUsingIntent():4");
    intent.putExtra("result", result);
    //Log.i("D_SDK_RETURN_INTENT", "returnUsingIntent():5");
    intent.putExtra("counterparty", counterparty);
    //Log.i("D_SDK_RETURN_INTENT", "returnUsingIntent():6");
    intent.putExtra("portal", portal);

    try {
      startActivity(intent);
      Log.i("D_SDK_RETURN_INTENT", "<returnUsingIntent():called startActivity()");
    } catch (Exception e) {
      Log.i("D_SDK_RETURN_INTENT", " returnUsingIntent():ERROR:e=" + e);
    }
  }
}
