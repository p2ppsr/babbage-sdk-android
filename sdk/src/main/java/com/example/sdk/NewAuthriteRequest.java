package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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
public class NewAuthriteRequest extends SDKActivity.CallTypes implements Serializable {
  protected NewAuthriteRequest(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String params, String requestUrl, String fetchConfig) {
    paramStr = "";
    paramStr += "\"params\":\"" + params + "\",";
    paramStr += "\"requestUrl\":\"" + requestUrl + "\",";
    paramStr += "\"fetchConfig\":\"" + fetchConfig + "\"";
  }
  protected void caller() {
    super.caller("newAuthriteRequest", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_NEW_AUTH_REQ", ">DownloadUHRPFile:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("newAuthriteRequest", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_NEW_AUTH_REQ", "NewAuthriteRequest:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("newAuthriteRequest", uuid, result);
    }
    Log.i("D_SDK_NEW_AUTH_REQ", "<NewAuthriteRequest:called()");
  }
}