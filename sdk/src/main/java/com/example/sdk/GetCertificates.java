package com.example.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func getCertificates(certifiers: JSON, types: JSON) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"ninja.findCertificates",
          "params": [
              "certifiers": certifiers,
              "types": types
          ]
      ]

      // Run the command and get the response JSON object
      let certificates = await runCommand(cmd: &cmd).value
      return certificates
  }
  */
// public func getCertificates(certifiers: JSON, types: JSON) async -> JSON {
public class GetCertificates extends SDKActivity.CallTypes implements Serializable {
  protected GetCertificates(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  protected void process(String certifiers, String types) {
    paramStr = "";
    paramStr += "\"certifiers\":\"" + certifiers + "\",";
    paramStr += "\"types\":\"" + types + "\"";
  }
  protected void caller() {
    super.caller("getCertificates", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_GET_CERT", "GetCertificates:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("getCertificates", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_GET_CERT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("getCertificates", uuid, result);
    }
  }
}
