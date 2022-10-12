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

    private byte[] base64Encoded;
    private String protocolID;
    private String keyID;

    // Required for polymorphism
    public Encrypt() {}

    public Encrypt(String plaintext, String protocolID, String keyID) {
      this.protocolID = protocolID;
      this.keyID = keyID;

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
        checkForJSONErrorAndReturnToApp(returnResult,"encrypt", "result");
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

    private String plaintext;
    private String base64CryptoKey;
    private String  returnType = "base64";

    // Required for polymorphism
    public EncryptUsingCryptoKey() {}

    public EncryptUsingCryptoKey(String plaintext, String base64CryptoKey) {
      this.plaintext = plaintext;
      this.base64CryptoKey = base64CryptoKey;
    }
    public EncryptUsingCryptoKey(String plaintext, String base64CryptoKey, String returnType) {
      this.plaintext = plaintext;
      this.base64CryptoKey = base64CryptoKey;
      this.returnType = returnType;
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"encryptUsingCryptoKey\",";
      cmdJSONString += "\"params\":{";
      cmdJSONString += "\"plaintext\":\"" + convertStringToBase64(plaintext) + "\",";
      cmdJSONString += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      cmdJSONString += "\"returnType\":\"" + returnType + "\",";
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
        if (returnType.equals("base64")) {
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
  public class DecryptUsingCryptoKey extends CallBaseTypes {

    private String ciphertext;
    private String base64CryptoKey;
    private String returnType = "base64";

    // Required for polymorphism
    public DecryptUsingCryptoKey() {}

    public DecryptUsingCryptoKey(String ciphertext, String base64CryptoKey) {
      this.ciphertext = ciphertext;
      this.base64CryptoKey = base64CryptoKey;
    }
    public DecryptUsingCryptoKey(String ciphertext, String base64CryptoKey, String returnType) {
      this.ciphertext = ciphertext;
      this.base64CryptoKey = base64CryptoKey;
      this.returnType = returnType;
    }
    public String caller() {
      String cmdJSONString = "";
      cmdJSONString += "{\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"decryptUsingCryptoKey\",";
      cmdJSONString += "\"params\":{";
      cmdJSONString += "\"ciphertext\":\"" + ciphertext + "\",";
      cmdJSONString += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
      cmdJSONString += "\"returnType\":\"" + returnType + "\",";
      cmdJSONString += "},";
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
  public class CreateAction extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public CreateAction() {}

    // Default values enforced by overloading constructor
    public CreateAction(String outputs, String description) {
      paramStr = "";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createAction", "description") + "\"";
     }
    public CreateAction(String inputs, String outputs, String description) {
      paramStr = "";
      paramStr += "\"inputs\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "inputs") + "\",";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createAction", "description") + "\"";
    }
    public CreateAction(String inputs, String outputs, String description, String bridges) {
      paramStr = "";
      paramStr += "\"inputs\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "inputs") + "\",";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createAction", "description") + "\"";
      paramStr += "\"bridges\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "bridges") + "\"";
    }
    public CreateAction(String inputs, String outputs, String description, String bridges, String labels) {
      paramStr = "";
      paramStr += "\"inputs\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "inputs") + "\",";
      paramStr += "\"outputs\":\"" + checkForJSONErrorAndReturnToApp(outputs, "createAction", "outputs") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createAction", "description") + "\"";
      paramStr += "\"bridges\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "bridges") + "\",";
      paramStr += "\"labels\":\"" + checkForJSONErrorAndReturnToApp(inputs, "createAction", "labels") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createAction\",";
      cmdJSONString += "\"params\":\"{" + paramStr + "\"},";
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
  // Default values enforced by overloading constructor
  public class CreateHmac extends CallBaseTypes {

    private String paramStr = "";

    // Required for polymorphism
    public CreateHmac() {}

    public CreateHmac(String data, String protocolID, String keyID) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(data, "createHmac", "data") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "createHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "createHmac", "keyID") + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(data, "createHmac", "data") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "createHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "createHmac", "keyID") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createHmac", "description") + "\",";
      paramStr += "\"counterparty\":\"self\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(data, "createHmac", "data") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "createHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "createHmac", "keyID") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createHmac", "description") + "\",";
      paramStr += "\"counterparty\":\"" + checkForJSONErrorAndReturnToApp(counterparty, "createHmac", "counterparty") + "\"";
    }
    public CreateHmac(String data, String protocolID, String keyID, String description, String counterparty, String privileged) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(data, "createHmac", "data") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "createHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "createHmac", "keyID") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "createHmac", "description") + "\",";
      paramStr += "\"counterparty\":\"" + checkForJSONErrorAndReturnToApp(counterparty, "createHmac", "counterparty") + "\",";
      paramStr += "\"privileged\":\"" + checkForJSONErrorAndReturnToApp(privileged, "createHmac", "privileged") + "\"";
   }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"createHmac\",";
      cmdJSONString += "\"params\":\"{" + paramStr + "\"},";
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
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(data), "verifyHmac", "data") + "\",";
      paramStr += "\"hmac\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(hmac), "verifyHmac", "hmac") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "verifyHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "verifyHmac", "keyID") + "\",";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(data), "verifyHmac", "data") + "\",";
      paramStr += "\"hmac\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(hmac), "verifyHmac", "hmac") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "verifyHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "verifyHmac", "keyID") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "verifyHmac", "description") + "\",";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description, String counterparty) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(data), "verifyHmac", "data") + "\",";
      paramStr += "\"hmac\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(hmac), "verifyHmac", "hmac") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "verifyHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "verifyHmac", "keyID") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "verifyHmac", "description") + "\",";
      paramStr += "\"counterparty\":\"" + checkForJSONErrorAndReturnToApp(counterparty, "verifyHmac", "counterparty") + "\"";
    }
    public VerifyHmac(String data, String hmac, String protocolID, String keyID, String description, String counterparty, String privileged) {
      paramStr = "";
      paramStr += "\"data\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(data), "verifyHmac", "data") + "\",";
      paramStr += "\"hmac\":\"" + checkForJSONErrorAndReturnToApp(checkIsBase64(hmac), "verifyHmac", "hmac") + "\",";
      paramStr += "\"protocolID\":\"" + checkForJSONErrorAndReturnToApp(protocolID, "verifyHmac", "protocolID") + "\",";
      paramStr += "\"keyID\":\"" + checkForJSONErrorAndReturnToApp(keyID, "verifyHmac", "keyID") + "\",";
      paramStr += "\"description\":\"" + checkForJSONErrorAndReturnToApp(description, "verifyHmac", "description") + "\",";
      paramStr += "\"counterparty\":\"" + checkForJSONErrorAndReturnToApp(counterparty, "verifyHmac", "counterparty") + "\",";
      paramStr += "\"privileged\":\"" + checkForJSONErrorAndReturnToApp(privileged, "verifyHmac", "privileged") + "\"";
    }
    public String caller() {
      String cmdJSONString = "{";
      cmdJSONString += "\"type\":\"CWI\",";
      cmdJSONString += "\"call\":\"verifyHmac\",";
      cmdJSONString += "\"params\":\"{" + paramStr + "\"},";
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
