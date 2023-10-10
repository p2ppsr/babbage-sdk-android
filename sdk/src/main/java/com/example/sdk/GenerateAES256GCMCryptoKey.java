package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class GenerateAES256GCMCryptoKey extends SDKActivity.CallTypes implements Serializable {
  protected GenerateAES256GCMCryptoKey(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }
  protected void process() {
    paramStr = "";
  }
  protected void caller() {
    Log.i("D_SDK_GEN_CRYPT", "GenerateAES256GCMCryptoKey:caller()");
    super.caller("generateAES256GCMCryptoKey", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_GEN_CRYPT", " >called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("generateAES256GCMCryptoKey", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_GEN_CRYPT", "GenerateAES256GCMCryptoKey:called():JSON:ERROR:e=" + e);
      activity.returnUsingIntent("generateAES256GCMCryptoKey", uuid, result);
    }
    Log.i("D_SDK_GEN_CRYPT", "<GenerateAES256GCMCryptoKey:called()");
  }
}