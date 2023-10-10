package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func verifyHmac(data: String, hmac: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: Bool? = nil) async -> Bool {
      // Make sure data and hmac are base64 strings
      var data = data
      var hmac = hmac
      if (!base64StringRegex.matches(hmac)) {
          hmac = convertStringToBase64(data: hmac)
      }
      if (!base64StringRegex.matches(data)) {
          data = convertStringToBase64(data: data)
      }

      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"verifyHmac",
          "params": [
              "data": convertToJSONString(param: data),
              "hmac": convertToJSONString(param: hmac),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false)
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result boolean
      let verified:Bool = (responseObject.objectValue?["result"]?.boolValue)!
      return verified
  }
  */
// Default values enforced by overloading constructor
// public func verifyHmac(data: String, hmac: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: Bool? = nil) async -> Bool {
public class VerifyHmac extends SDKActivity.CallTypes implements Serializable {
  protected VerifyHmac(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String data, String hmac, String protocolID, String keyID) {
    process(data, hmac, protocolID, keyID, "");

  }
  protected void process(String data, String hmac, String protocolID, String keyID, String description) {
    process(data, hmac, protocolID, keyID, description, "self");
  }
  protected void process(String data, String hmac, String protocolID, String keyID, String description, String counterparty) {
    process(data, hmac, protocolID, keyID, description, counterparty, false);
  }
  protected void process(String data, String hmac, String protocolID, String keyID, String description, String counterparty, Boolean privileged) {
    paramStr = "";
    paramStr += "\"data\":\"" + activity.checkIsBase64(data) + "\",";
    paramStr += "\"hmac\":\"" + activity.checkIsBase64(hmac) + "\",";
    paramStr += "\"protocolID\":\"" + protocolID + "\",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"description\":\"" + description + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"privileged\":\"" + privileged + "\"";
  }
  protected void caller() {
    super.caller("verifyHmac", paramStr);  }
  protected void called(String returnResult) {
    Log.i("D_SDK_VERIFY_HMAC", ">VerifyHmac:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("verifyHmac", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_VERIFY_HMAC", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("verifyHmac", uuid, result);
    }
    Log.i("D_SDK_VERIFY_HMAC", "<VerifyHmac:called()");
  }
}