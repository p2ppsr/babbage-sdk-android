package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class DecryptUsingCryptoKey extends SDKActivity.CallTypes implements Serializable {
  protected DecryptUsingCryptoKey(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String ciphertext, String base64CryptoKey) {
    process(ciphertext, base64CryptoKey, "base64");
  }
  protected void process(String ciphertext, String base64CryptoKey, String returnType) {
    paramStr = "";
    paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
    paramStr += "\"base64CryptoKey\":\"" + base64CryptoKey + "\",";
    paramStr += "\"returnType\":\"" + returnType + "\"";
  }
  protected void caller() {
    Log.i("D_SDK_CRYPT_KEY_DECRYPT", "DecryptUsingCryptoKey:caller()");
    super.caller("decryptUsingCryptoKey", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CRYPT_KEY_DECRYPT", "called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      Log.i("D_SDK_CRYPT_KEY_DECRYPT", "called():result:" + result);
      activity.returnUsingIntent("decryptUsingCryptoKey", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CRYPT_KEY_DECRYPT", "DecryptUsingCryptoKey:called():JSON:ERROR:e=" + e);
      activity.returnUsingIntent("decryptUsingCryptoKey", uuid, result);
    }
  }
}