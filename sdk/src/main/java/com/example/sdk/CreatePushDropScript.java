package com.example.sdk;


import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class CreatePushDropScript extends SDKActivity.CallTypes implements Serializable {
  protected CreatePushDropScript(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String fields, String protocolID, String keyID) {
    paramStr = "";
    paramStr += "\"fields\":\"" + fields + "\",";
    paramStr += "\"protocolID\":\"" + protocolID + "\",";
    paramStr += "\"keyID\":\"" + keyID + "\"";
  }
  protected void caller() {
    super.caller("createPushDropScript", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_SUB_DIR_TRANS", ">CreatePushDropScript:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("createPushDropScript", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_SUB_DIR_TRANS", "CreatePushDropScript:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("createPushDropScript", uuid, result);
    }
    Log.i("D_SDK_SUB_DIR_TRANS", "<CreatePushDropScript:called()");
  }
}