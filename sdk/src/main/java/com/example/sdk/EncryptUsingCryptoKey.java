package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class EncryptUsingCryptoKey extends SDKActivity.CallTypes implements Serializable {
  protected EncryptUsingCryptoKey(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String plaintext, String base64CryptoKey) {
    process(plaintext, base64CryptoKey, "base64");
  }
  protected void process(String plaintext, String base64CryptoKey, String returnType) {
    paramStr = "";
    paramStr += "\"plaintext\":\"" + plaintext + "\",";
    paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
    paramStr += "\"returnType\":\"" + returnType + "\"";
  }
  protected void caller() {
    Log.i("D_SDK_CRYPT_KEY_ENCRYPT", "EncryptUsingCryptoKey:caller()");
    super.caller("encryptUsingCryptoKey", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CRYPT_KEY_ENCRYPT", "called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      Log.i("D_SDK_CRYPT_KEY_ENCRYPT", "called():result:" + result);
      activity.returnUsingIntent("encryptUsingCryptoKey", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CRYPT_KEY_ENCRYPT", "EncryptUsingCryptoKey:called():JSON:ERROR:e=" + e);
      activity.returnUsingIntent("encryptUsingCryptoKey", uuid, result);
    }
  }
}