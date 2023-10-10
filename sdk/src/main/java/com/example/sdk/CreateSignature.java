package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func createSignature(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil) async -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"createSignature",
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
      let signature:String = (responseObject.objectValue?["result"]?.stringValue)!
      return signature
  }
  */
//   public func createSignature(data: String, protocolID: String, keyID: String, description: String? = nil, counterparty: String? = nil, privileged: String? = nil) async -> String {
// Default values enforced by overloading constructor
public class CreateSignature extends SDKActivity.CallTypes implements Serializable {
  protected CreateSignature(SDKActivity activity) {
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
    paramStr += "\"data\":\"" + activity.checkIsBase64(data) + "\",";
    paramStr += "\"protocolID\":\"" + protocolID + "\",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"description\":\"" + description + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"privileged\":\"" + privileged + "\"";
  }
  protected void caller() {
    super.caller("createSignature", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CREATE_SIGNATURE", "CreateSignature:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("createSignature", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CREATE_SIGNATURE", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("createSignature", uuid, result);
    }
  }
}