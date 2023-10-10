package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func createCertificate(certificateType: String, fieldObject: JSON, certifierUrl: String, certifierPublicKey: String) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"createCertificate",
          "params": [
              "certificateType": convertToJSONString(param: certificateType),
              "fieldObject": fieldObject,
              "certifierUrl": convertToJSONString(param: certifierUrl),
              "certifierPublicKey": convertToJSONString(param: certifierPublicKey)
          ]
      ]

      // Run the command and get the response JSON object
      let signedCertificate = await runCommand(cmd: &cmd).value
      return signedCertificate
  }
  */
// public func createCertificate(certificateType: String, fieldObject: JSON, certifierUrl: String, certifierPublicKey: String) async -> JSON {
public class CreateCertificate extends SDKActivity.CallTypes implements Serializable {

  protected CreateCertificate(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  protected void process(String certificateType, String fieldObject, String certifierUrl, String certifierPublicKey) {
    paramStr = "";
    paramStr += "\"certificateType\":\"" + certificateType + "\",";
    paramStr += "\"fieldObject\":\"" + fieldObject + "\",";
    paramStr += "\"certifierUrl\":\"" + certifierUrl + "\",";
    paramStr += "\"certifierPublicKey\":\"" + certifierPublicKey + "\"";
  }
  protected void caller() {
    super.caller("createCertificate", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CREATE_CERTIFICATE", "CreateCertificate:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("createCertificate", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CREATE_CERTIFICATE", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("createCertificate", uuid, result);
    }
  }
}