package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  // Creates an Hmac using CWI.createHmac
  @available(iOS 15.0, *)
  public func createHmac(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = "self", privileged: Bool? = nil) async -> String {
      // Construct the expected command to send with default values for nil params
      var cmd:JSON = [
          "type":"CWI",
          "call":"createHmac",
          "params": [
              "data": convertToJSONString(param: convertStringToBase64(data: data)),
              "protocolID": convertToJSONString(param: protocolID),
              "keyID": convertToJSONString(param: keyID),
              "description": try! JSON(description ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "privileged": try! JSON(privileged ?? false)
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result string
      let decryptedText:String = (responseObject.objectValue?["result"]?.stringValue)!
      return decryptedText
  }
  */
// public func createHmac(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = "self", privileged: Bool? = nil) async -> String {

public class CreateHmac extends SDKActivity.CallTypes implements Serializable {
  protected CreateHmac(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String data, String protocolID, String keyID) {
    process(data, protocolID, keyID, "");
  }
  protected void process(String data, String protocolID, String keyID, String description) {
    process(data, protocolID, keyID, description, "self");
  }
  protected void process(String data, String protocolID, String keyID, String description, String counterparty) {
    process(data, protocolID, keyID, description, counterparty, false);
  }
  protected void process(String data, String protocolID, String keyID, String description, String counterparty, Boolean privileged) {
    paramStr = "";
    paramStr += "\"data\":\"" + SDKActivity.convertStringToBase64(data) + "\",";
    paramStr += "\"protocolID\":\"" + protocolID + "\",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"description\":\"" + description + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"privileged\":\"" + privileged + "\"";
  }
  protected void caller() {
    super.caller("createHmac", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CREATE_HMAC", "called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("createHmac", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CREATE_HMAC", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("createHmac", uuid, result);
    }
    Log.i("D_SDK_CREATE_HMAC", "called()");
  }
}