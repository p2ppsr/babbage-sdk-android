package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class GetVersion extends SDKActivity.CallTypes implements Serializable {
  protected GetVersion(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }
  protected void caller() {
    super.caller("getVersion", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_SUB_DIR_TRANS", ">GetVersion:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("getVersion", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_SUB_DIR_TRANS", "GetVersion:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("getVersion", uuid, result);
    }
    Log.i("D_SDK_SUB_DIR_TRANS", "<GetVersion:called()");
  }
}