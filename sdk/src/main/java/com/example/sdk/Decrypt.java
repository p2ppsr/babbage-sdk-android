package com.example.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Decrypt extends SDKActivity.CallTypes implements Serializable {
  protected Decrypt(SDKActivity activity) {
    Log.i("D_SDK_DECRYPT", "<>Decrypt():");
    SDKActivity.CallTypes.activity = activity;
  }
  // Default values enforced by overloading constructor
  protected void process(String ciphertext, String protocolID, String keyID) {
    process(ciphertext, protocolID, keyID, "self");
  }
  protected void process(String ciphertext, String protocolID, String keyID, String counterparty) {
    Log.i("D_SDK_DECRYPT", "Decrypt():ciphertext=" + ciphertext);
    paramStr = "";
    paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
    paramStr += "\"protocolID\":" + protocolID + ",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"returnType\":\"string\"";

  }
  protected void caller() {
    super.caller("decrypt", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_DECRYPT", ">Decrypt:called():returnResult:" + returnResult);
    try {
      //Log.i("D_SDK_DECRYPT", "called():1");
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      //Log.i("D_SDK_DECRYPT", "called():2");
      uuid = jsonReturnResultObject.get("uuid").toString();
      //Log.i("D_SDK_DECRYPT", "called():3");
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("decrypt", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_DECRYPT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("decrypt", uuid, result);
    }
    Log.i("D_SDK_DECRYPT", "<Decrypt:called()");
  }
}