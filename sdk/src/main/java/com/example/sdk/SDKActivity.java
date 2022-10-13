package com.example.sdk;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static android.util.Base64.DEFAULT;
import static android.util.Base64.decode;

import android.content.Intent;
import android.os.Build;
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
import java.util.Random;
import java.util.Stack;

import org.json.JSONException;
import org.json.JSONObject;

// TODO Sparse logging is left since we are adding additional commands in the future and to help
// TODO team view what is happening on a physical device

public class SDKActivity extends AppCompatActivity {

  private Object classObject; // Used for Intent callback
  private WebView webview;
  private CallTypes callTypes = null; // Used as intermediate concrete class to give polymorphism
  private Stack<CallBaseTypes> waitingCallType = new Stack<CallBaseTypes>(); // Stores the waiting call type while authentication is performed
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
      return "{\"type\":\"CWI\",\"call\":\"isAuthenticated\",\"params\":{},\"id\":\"uuid\"}";
    }

    public void called(String returnResult) {
      try {
        Log.i("WEBVIEW_AUTH", "called():returnResult:" + returnResult);
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = (String)jsonReturnResultObject.get("result").toString();
        if (result.equals("false")) {
          Intent intent = new Intent(SDKActivity.this, classObject.getClass());
          intent.putExtra("result", result);
          intent.putExtra("type", "isAuthenticated");
          intent.putExtra("uuid", uuid);
          startActivity(intent);
        }
      } catch (JSONException e) {
        checkForJSONErrorAndReturnToApp(returnResult,"isAuthenticated", "result");
      }
      if (!waitingCallType.isEmpty()) {
        // Need to start the child thread to call the waiting run command
        WorkerThread workerThread = new WorkerThread();
        workerThread.start();
      }
    }
  }

  public class WaitForAuthenticated extends CallBaseTypes {

    public String caller() {
      return "{\"type\":\"CWI\",\"call\":\"waitForAuthenticated\",\"params\":{},\"id\":\"uuid\"}";
    }

    public void called(String returnResult) {
      Log.i("WEBVIEW_AUTHED", "called()");
      finish();

      // Once authenticated the waiting command is processed
      if (!waitingCallType.isEmpty()) {
        // Need to start the child thread to call the waiting run command
        WorkerThread workerThread = new WorkerThread();
        workerThread.start();
      }
    }
  }

  public class Encrypt extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public Encrypt() {}

    public Encrypt(String plaintext, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"plaintext\":\"" + convertStringToBase64(plaintext) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
    }

    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"encrypt\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
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
        checkForJSONErrorAndReturnToApp(returnResult,"encrypt", "result");
      }
    }
  }

  public class Decrypt extends CallBaseTypes {

    private String paramStr;

    // Required for polymorphism
    public Decrypt() {}

    public Decrypt(String ciphertext, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
    }

    public String caller() {
      String cmdJSONString = "";
      cmdJSONString += "{\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"decrypt\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
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
        checkForJSONErrorAndReturnToApp(returnResult,"decrypt", "result");
      }
    }
  }

  /*
  @available(iOS 15.0, *)
  public func generateCryptoKey() async -> String {
    // Construct the expected command to send
    var cmd:JSON = [
        "type":"CWI",
        "call":"generateCryptoKey",
        "params": []
    ]

    // Run the command and get the response JSON object
    let responseObject = await runCommand(cmd: &cmd).value

    // Pull out the expect result string
    let cryptoKey:String = (responseObject.objectValue?["result"]?.stringValue)!
    return cryptoKey
  }
  */
  public class GenerateCryptoKey extends CallBaseTypes {

    public String caller() {
      return "{\"type\":\"CWI\",\"call\":\"generateCryptoKey\",\"params\":{},\"id\":\"uuid\"}";
    }

    public void called(String returnResult) {
      Log.i("WEBVIEW_GEN_CRYPT", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"generateCryptoKey", "result");
      }
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
  public class EncryptUsingCryptoKey extends CallBaseTypes {

    private String paramStr;
    
    // Required for polymorphism
    public EncryptUsingCryptoKey() {}

    public EncryptUsingCryptoKey(String plaintext, String base64CryptoKey) {
      paramStr = "";
      paramStr += "\"plaintext\":\"" + convertStringToBase64(plaintext) + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"base64\"";
    }
    public EncryptUsingCryptoKey(String plaintext, String base64CryptoKey, String returnType) {
      paramStr = "";
      paramStr += "\"plaintext\":\"" + convertStringToBase64(plaintext) + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"" + returnType + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"encryptUsingCryptoKey\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("WEBVIEW_CRYPT_KEY_ENCRYPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        String returnType = jsonReturnResultObject.get("returnType").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "encryptUsingCryptoKey");
        intent.putExtra("uuid", uuid);
        if (!returnType.equals("base64")) {
          intent.putExtra("result", result);
        } else {
          intent.putExtra("result","Error: Unsupported type!");
        }
        startActivity(intent);
      } catch (JSONException e) {
        checkForJSONErrorAndReturnToApp(returnResult,"encryptUsingCryptoKey", "result");
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
  public class DecryptUsingCryptoKey extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public DecryptUsingCryptoKey() {}

    public DecryptUsingCryptoKey(String ciphertext, String base64CryptoKey) {
      paramStr = "";
      paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
      paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      paramStr += "\"returnType\":\"base64\"";
    }
    public DecryptUsingCryptoKey(String ciphertext, String base64CryptoKey, String returnType) {
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
      Log.i("WEBVIEW_CRYPT_KEY_DECRYPT", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        String returnType = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "decryptUsingCryptoKey");
        intent.putExtra("uuid", uuid);
        if (returnType.equals("base64")) {
          intent.putExtra("result", result);
        } else {
          intent.putExtra("result","Error: Unsupported type!");
        }
        startActivity(intent);
      } catch (JSONException e) {
        checkForJSONErrorAndReturnToApp(returnResult,"decryptUsingCryptoKey", "result");
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
  public class CreateAction extends CallBaseTypes {

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
      Log.i("WEBVIEW_CREATE_ACTION", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"createAction", "result");
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
  public class CreateHmac extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public CreateHmac() {}

    public CreateHmac(String data, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\"";
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
      Log.i("WEBVIEW_CREATE_HMAC", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"createHmac", "result");
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
  public class VerifyHmac extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public VerifyHmac() {}

    public VerifyHmac(String data, String hmac, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\"";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"hmac\":\"" + checkIsBase64(hmac) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\"";
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
      Log.i("WEBVIEW_VERIFY_HMAC", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"verifyHmac", "result");
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
  public class CreateSignature extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public CreateSignature() {}

    public CreateSignature(String data, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + convertStringToBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
    }
    public CreateSignature(String data, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\"";
    }
    public CreateSignature(String data, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\"";
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
      Log.i("WEBVIEW_CREATE_SIG", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"createSignature", "result");
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
  public class VerifySignature extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public VerifySignature() {}

    public VerifySignature(String data, String signature, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
    }
    public VerifySignature(String data, String signature, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\"";
    }
    public VerifySignature(String data, String signature, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkIsBase64(data) + "\",";
      paramStr += "\"signature\":\"" + checkIsBase64(signature) + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"description\":\"" + description + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\"";
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
      Log.i("WEBVIEW_VERIFY_HMAC", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"verifySignature", "result");
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
  // Default values enforced by overloading constructor
  public class CreateCertificate extends CallBaseTypes {

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
      Log.i("WEBVIEW_CREATE_CERT", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"createCertificate", "result");
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
  // Default values enforced by overloading constructor
  public class GetCertificates extends CallBaseTypes {

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
      Log.i("WEBVIEW_GET_CERTS", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"ninja.findCertificates", "result");
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
  public class ProveCertificate extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public ProveCertificate() {}

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
      Log.i("WEBVIEW_PROVE_CERT", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"proveCertificate", "result");
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
  // Default values enforced by overloading constructor
  public class SubmitDirectTransaction extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public SubmitDirectTransaction() {}

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
      Log.i("WEBVIEW_SUBMIT_DIRECT_TX", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"submitDirectTransaction", "result");
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
  //   public func getPublicKey(protocolID: JSON?, keyID: String? = nil, privileged: Bool? = nil, identityKey: Bool? = nil, reason: String? = nil, counterparty: String? = "self", description: String? = nil) async -> String {
  public class GetPublicKey extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public GetPublicKey() {}

    // Default values enforced by overloading constructor
    public GetPublicKey(String protocolID) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey, String reason) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"reason\":\"" + reason + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey, String reason, String counterparty) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"reason\":\"" + reason + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
    }
    public GetPublicKey(String protocolID, String keyID, String privileged, String identityKey, String reason, String counterparty, String description) {
      paramStr = "";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "getPublicKey", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\"";
      paramStr += "\"privileged\":\"" + privileged + "\",";
      paramStr += "\"identityKey\":\"" + identityKey + "\",";
      paramStr += "\"reason\":\"" + reason + "\",";
      paramStr += "\"counterparty\":\"" + counterparty + "\",";
      paramStr += "\"description\":\"" + description + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"getPublicKey\",";
      cmdJSONString += "\"params\":{" + paramStr + "},";
      cmdJSONString += "\"id\":\"uuid\"";
      cmdJSONString += "}";
      return cmdJSONString;
    }
    public void called(String returnResult) {
      Log.i("WEBVIEW_CREATE_ACTION", "called():returnResult:" + returnResult);
      try {
        JSONObject jsonReturnResultObject = new JSONObject(returnResult);
        String uuid = jsonReturnResultObject.get("uuid").toString();
        String result = jsonReturnResultObject.get("result").toString();
        Intent intent = new Intent(SDKActivity.this, classObject.getClass());
        intent.putExtra("type", "getPublicKey");
        intent.putExtra("uuid", uuid);
        intent.putExtra("result", result);
        startActivity(intent);
      } catch (JSONException e) {
        checkForJSONErrorAndReturnToApp(returnResult,"getPublicKey", "result");
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
  public class GetVersion extends CallBaseTypes {

    public String caller() {
      return "{\"type\":\"CWI\",\"call\":\"getVersion\",\"params\":{},\"id\":\"uuid\"}";
    }

    public void called(String returnResult) {
      Log.i("WEBVIEW_GET_VERSION", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"getVersion", "result");
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
  public class CreatePushDropScript extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public CreatePushDropScript() {}

    // Default values enforced by overloading constructor
    public CreatePushDropScript(String fields, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"fields\":\"" + checkForJSONErrorAndReturnToApp(fields, "createPushDropScript", "fields") + "\",";
      paramStr += "\"keyID\":\"" + keyID + "\",";
      paramStr += "\"protocolID\":\"" + protocolID + "\"";
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
      Log.i("WEBVIEW_CREATE_PUSH_DROP", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"createPushDropScript", "result");
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
  public class ParapetRequest extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public ParapetRequest() {}

    // Default values enforced by overloading constructor
    public ParapetRequest(String resolvers, String bridge, String type, String query) {
      paramStr = "";
      paramStr += "\"resolvers\":\"" + checkForJSONErrorAndReturnToApp(resolvers, "parapetRequest", "resolvers") + "\",";
      paramStr += "\"bridge\":\"" + bridge + "\",";
      paramStr += "\"type\":\"" + type + "\"";
      paramStr += "\"query\":\"" + checkForJSONErrorAndReturnToApp(query, "parapetRequest", "query") + "\",";
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
      Log.i("WEBVIEW_PARA_REQUEST", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"parapetRequest", "result");
      }
    }
  }
  /*
  @available(iOS 15.0, *)
  public func downloadUHRPFile(URL: String, bridgeportResolvers: JSON) async -> Data? {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"downloadFile",
          "params": [
              "URL": convertToJSONString(param: URL),
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
  // public func downloadUHRPFile(URL: String, bridgeportResolvers: JSON) async -> Data? {
  public class DownloadUHRPFile extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public DownloadUHRPFile() {}

    // Default values enforced by overloading constructor
    public DownloadUHRPFile(String URL, String bridgeportResolvers) {
      paramStr = "";
      paramStr += "\"URL\":\"" + URL + "\"";
      paramStr += "\"bridgeportResolvers\":\"" + checkForJSONErrorAndReturnToApp(bridgeportResolvers, "downloadUHRPFile", "bridgeportResolvers") + "\",";
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
      Log.i("WEBVIEW_PARA_REQUEST", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"downloadUHRPFile", "result");
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
  public class NewAuthriteRequest extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public NewAuthriteRequest() {}

    public NewAuthriteRequest(String params, String requestUrl, String fetchConfig) {
      paramStr = "";
      paramStr += "\"params\":\"" + checkForJSONErrorAndReturnToApp(params, "newAuthriteRequest", "params") + "\",";
      paramStr += "\"requestUrl\":\"" + requestUrl + "\"";
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
      Log.i("WEBVIEW_NEW_AUTHRITE", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"newAuthriteRequest", "result");
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
  public class CreateOutputScriptFromPubKey extends CallBaseTypes {

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
      Log.i("WEBVIEW_CREATE_OUTPUT_SCRIPT", "called():returnResult:" + returnResult);
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
        checkForJSONErrorAndReturnToApp(returnResult,"createOutputScriptFromPubKey", "result");
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

  private String checkForJSONErrorAndReturnToApp(String str, String type, String field) {
    String resultStr = "";
    try {
      new JSONObject(str);
      resultStr = str;
    } catch (JSONException e) {
      Intent intent = new Intent(SDKActivity.this, classObject.getClass());
      intent.putExtra("result", str);
      intent.putExtra("type", type);
      intent.putExtra("uuid", uuid);
      intent.putExtra("error", "invalid JSON");
      intent.putExtra("field", field);
      startActivity(intent);
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

  // Convert the string to base64 so it can be passed over the net
  private String convertStringToBase64(String str) {
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
    Intent intent = getIntent();
    String type = intent.getStringExtra("type");
    if (!type.equals("waitForAuthenticated")) {
      finish();
    }
    callTypes = new CallTypes();
    setContentView(R.layout.activity_sdk);
    webview = findViewById(R.id.web_html);
    webview.setWebViewClient(
      new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        uuid = intent.getStringExtra("uuid");
        if (type.equals("isAuthenticated")) {
          runCommand(new IsAuthenticated(), uuid);
        }
        if (type.equals("waitForAuthenticated")) {
          runCommand(new WaitForAuthenticated(), uuid);
        }
        String callingClass = intent.getStringExtra("callingClass");
        byte[] bytes = decode(
          callingClass,
          DEFAULT
        );
        try {
          classObject = convertFromBytes(bytes);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
        if (type.equals("encrypt")) {
          waitingCallType.push(
            new Encrypt(
              intent.getStringExtra("plaintext"),
              intent.getStringExtra("protocolID"),
              intent.getStringExtra("keyID")
            )
          );
          waitingCallType.push(new IsAuthenticated());

          // Need to start the child thread to call waitForAuthenticated run command
          WorkerThread workerThread = new WorkerThread();
          workerThread.start();
        }
        if (type.equals("decrypt")) {
          waitingCallType.push(
            new Decrypt(
              intent.getStringExtra("ciphertext"),
              intent.getStringExtra("protocolID"),
              intent.getStringExtra("keyID")
            )
          );
          waitingCallType.push(new IsAuthenticated());
          // Need to start the child thread to call IsAuthenticated run command
          WorkerThread workerThread = new WorkerThread();
          workerThread.start();
        }
        }
      }
    );
    mainThreadHandler =
      new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
          if (msg.what == 1) {
            runCommand(waitingCallType.pop(), uuid);
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
