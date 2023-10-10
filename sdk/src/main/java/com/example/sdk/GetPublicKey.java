package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func getPublicKey(protocolID: JSON?, keyID: String? = nil, priviliged: Bool? = nil, identityKey: Bool? = nil, reason: String? = nil, counterparty: String? = "self", description: String? = nil) async -> String {
      // Construct the expected command to send
      // Added default values for dealing with nil params
      var cmd:JSON = [
          "type":"CWI",
          "call":"getPublicKey",
          "params": [
              "protocolID": protocolID ?? "",
              "keyID": try! JSON(keyID!),
              "priviliged": try! JSON(priviliged ?? false),
              "identityKey": try! JSON(identityKey ?? false),
              "reason": try! JSON(reason ?? ""),
              "counterparty": try! JSON(counterparty ?? ""),
              "description": try! JSON(description ?? "")
          ]
      ]

      // Run the command and get the response JSON object
      let responseObject = await runCommand(cmd: &cmd).value

      // Pull out the expect result string
      let publicKey:String = (responseObject.objectValue?["result"]?.stringValue)!
      return publicKey
  }
  */
// public func getPublicKey(protocolID: JSON?, keyID: String? = nil, privileged: Bool? = nil, identityKey: Bool? = nil, reason: String? = nil, counterparty: String? = "self", description: String? = nil) async -> String {
public class GetPublicKey extends SDKActivity.CallTypes implements Serializable {

  protected GetPublicKey(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String protocolID) {
    process(protocolID, "1");
  }
  protected void process(String protocolID, String keyID) {
    process(protocolID, keyID, false);
  }
  protected void process(String protocolID, String keyID, Boolean privileged) {
    process(protocolID, keyID, privileged, false);
  }
  protected void process(String protocolID, String keyID, Boolean privileged, Boolean identityKey) {
    process(protocolID, keyID, privileged, identityKey, "");
  }
  protected void process(String protocolID, String keyID, Boolean privileged, Boolean identityKey, String reason) {
    process(protocolID, keyID, privileged, identityKey, reason, "");
  }
  protected void process(String protocolID, String keyID, Boolean privileged, Boolean identityKey, String reason, String counterparty) {
    process(protocolID, keyID, privileged, identityKey, reason, counterparty, "");
  }
  protected void process(String protocolID, String keyID, Boolean privileged, Boolean identityKey, String reason, String counterparty, String description) {
    paramStr = "";
    paramStr += "\"protocolID\":" + protocolID + ",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"privileged\":" + privileged + ",";
    paramStr += "\"identityKey\":" + identityKey + ",";
    paramStr += "\"reason\":\"" + reason + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"description\":\"" + description + "\"";
    Log.i("D_SDK_GET_PUBLIC_KEY", "GetPublicKey():paramStr=" + paramStr);
  }
  protected void caller() {
    super.caller("getPublicKey", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_GET_PUB_KEY", ">GetPublicKey:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("getPublicKey", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_ENCRYPT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("getPublicKey", uuid, result);
    }
    Log.i("D_SDK_ENCRYPT", "<GetPublicKey:called()");
  }
}