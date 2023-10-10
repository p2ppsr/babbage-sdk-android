package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func verifySignature(data: String, signature: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil, reason: String? = nil) async -> Bool{
      // Make sure data and signature are base64 strings
      var data = data
      var signature = signature
      if (!base64StringRegex.matches(data)) {
          data = convertStringToBase64(data: data)
      }
      if (!base64StringRegex.matches(signature)) {
          signature = convertStringToBase64(data: signature)
      }

      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"verifySignature",
          "params": [
              "data": convertToJSONString(param: data),
              "signature": convertToJSONString(param: signature),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false),
              "reason": try! JSON(reason ?? "")
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result boolean
      let verified:Bool = (responseObject.objectValue?["result"]?.boolValue)!
      return verified
  }
  */
// public func verifySignature(data: String, signature: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil, reason: String? = nil) async -> Bool{
// Default values enforced by overloading constructor
public class VerifySignature extends SDKActivity.CallTypes implements Serializable {
  protected VerifySignature(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String data, String signature, String protocolID, String keyID) {
    process(data, signature, protocolID, keyID, "");
  }
  protected void process(String data, String signature, String protocolID, String keyID, String description) {
    process(data, signature, protocolID, keyID, description, "self");
  }
  protected void process(String data, String signature, String protocolID, String keyID, String description, String counterparty) {
    process(data, signature, protocolID, keyID, description, counterparty, false);
  }
  protected void process(String data, String signature, String protocolID, String keyID, String description, String counterparty, Boolean privileged) {
    process(data, signature, protocolID, keyID, description, counterparty, privileged, "");
  }
  protected void process(String data, String signature, String protocolID, String keyID, String description, String counterparty, Boolean privileged, String reason) {
    paramStr = "";
    paramStr += "\"data\":\"" + activity.checkIsBase64(data) + "\",";
    paramStr += "\"signature\":\"" + activity.checkIsBase64(data) + "\",";
    paramStr += "\"protocolID\":\"" + protocolID + "\",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"description\":\"" + description + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"privileged\":\"" + privileged + "\",";
    paramStr += "\"reason\":\"" + reason + "\"";
  }
  protected void caller() {
    super.caller("verifySignature", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_VERIFY_SIGNATURE", ">VerifySignature:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("verifySignature", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_VERIFY_SIGNATURE", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("verifySignature", uuid, result);
    }
    Log.i("D_SDK_VERIFY_SIGNATURE", "<VerifySignature:called()");
  }}