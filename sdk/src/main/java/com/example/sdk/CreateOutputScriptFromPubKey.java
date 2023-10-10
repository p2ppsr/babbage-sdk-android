package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class CreateOutputScriptFromPubKey extends SDKActivity.CallTypes implements Serializable {
  protected CreateOutputScriptFromPubKey(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String derivedPublicKey) {
    paramStr = "\"derivedPublicKey\":\"" + derivedPublicKey + "\"";
  }
  // Dummy methods to allow instantiation of Abstract class
  protected void caller() {
    super.caller("createOutputScriptFromPubKey", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CREATE_OUTPUT_SCR_FROM_PUB_KEY", ">CreateOutputScriptFromPubKey:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("createOutputScriptFromPubKey", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CREATE_OUTPUT_SCR_FROM_PUB_KEY", "CreateOutputScriptFromPubKey:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("createOutputScriptFromPubKey", uuid, result);
    }
    Log.i("D_SDK_CREATE_OUTPUT_SCR_FROM_PUB_KEY", "<CreateOutputScriptFromPubKey:called()");
  }
}