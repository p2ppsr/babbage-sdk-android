package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Encrypt extends SDKActivity.CallTypes implements Serializable {
  private static String  uuid = "";
  private static String  result = "";
  private String paramStr = "";
  private static SDKActivity activity = null;
  // Required for polymorphism
  public Encrypt(SDKActivity activity) {
    Encrypt.activity = activity;
  }

  // Default values enforced by overloading constructor
  public void process(String plaintext, String protocolID, String keyID) {
    process(plaintext, protocolID, keyID, "self");
  }
  public void process(String plaintext, String protocolID, String keyID, String counterparty) {
    //Log.i("D_SDK_ENCRYPT", "Encrypt():plaintext=" + plaintext);
    //Log.i("D_SDK_ENCRYPT", "Encrypt():base64 plaintext=" + SDKActivity.convertStringToBase64(plaintext));
    paramStr = "";
    paramStr += "\"plaintext\":\"" + SDKActivity.convertStringToBase64(plaintext) + "\",";
    paramStr += "\"protocolID\":" + protocolID + ",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"returnType\":\"string\"";
  }
  public void caller() {
    super.caller("encrypt", paramStr);
  }
  public void called(String returnResult) {
    //Log.i("D_SDK_ENCRYPT", "called():returnResult:" + returnResult);
    try {
      //Log.i("D_SDK_ENCRYPT", "called():1");
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      //Log.i("D_SDK_ENCRYPT", "called():2");
      uuid = jsonReturnResultObject.get("uuid").toString();
      //Log.i("D_SDK_ENCRYPT", "called():3");
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("encrypt", result);
    } catch (JSONException e) {
      Log.e("D_SDK_ENCRYPT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("encrypt", result);
    }
  }
}