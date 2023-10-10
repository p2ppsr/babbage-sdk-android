package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
  /*
  @available(iOS 15.0, *)
  public func encrypt(plaintext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {

    // Convert the string to a base64 string
    let base64Encoded = convertStringToBase64(data: plaintext)

    // Construct the expected command to send
    var cmd:JSON = [
    "type":"CWI",
            "call":"encrypt",
            "params": [
    "plaintext": convertToJSONString(param: base64Encoded),
    "protocolID": protocolID,
            "keyID": convertToJSONString(param: keyID),
    "counterparty": convertToJSONString(param: counterparty!),
    "returnType": "string"
            ]
        ]

    // Run the command and get the response JSON object
    var responseObject:JSON = []
    do {
      responseObject = try await runCommand(cmd: &cmd).value
    } catch {
      throw error
    }

    // Pull out the expect result string
    let encryptedText:String = (responseObject.objectValue?["result"]?.stringValue)!
    return encryptedText
  }
  */
//public func encrypt(plaintext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {

public class Encrypt extends SDKActivity.CallTypes implements Serializable {
  protected Encrypt(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String plaintext, String protocolID, String keyID) {
    process(plaintext, protocolID, keyID, "self");
  }
  protected void process(String plaintext, String protocolID, String keyID, String counterparty) {
    //Log.i("D_SDK_ENCRYPT", "Encrypt:process():plaintext=" + plaintext);
    paramStr = "";
    paramStr += "\"plaintext\":\"" + SDKActivity.convertStringToBase64(plaintext) + "\",";
    paramStr += "\"protocolID\":" + protocolID + ",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"returnType\":\"string\"";
  }
  protected void caller() {
    super.caller("encrypt", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_ENCRYPT", ">Encrypt:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("encrypt", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_ENCRYPT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("encrypt", uuid, result);
    }
    Log.i("D_SDK_ENCRYPT", "<Encrypt:called()");
  }
}