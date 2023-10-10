package com.example.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
  /*
  // Encrypts data using CWI.decrypt
  @available(iOS 15.0, *)
  public func decrypt(ciphertext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"decrypt",
          "params": [
              "ciphertext": convertToJSONString(param: ciphertext),
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
      let decryptedText:String = (responseObject.objectValue?["result"]?.stringValue)!
      return decryptedText
  }
  */
// public func decrypt(ciphertext: String, protocolID: JSON, keyID: String, counterparty: String? = "self") async throws -> String {

public class Decrypt extends SDKActivity.CallTypes implements Serializable {
  protected Decrypt(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String ciphertext, String protocolID, String keyID) {
    process(ciphertext, protocolID, keyID, "self");
  }
  protected void process(String ciphertext, String protocolID, String keyID, String counterparty) {
    //Log.i("D_SDK_DECRYPT", "Decrypt:process():ciphertext=" + ciphertext);
    paramStr = "";
    paramStr += "\"ciphertext\":\"" + ciphertext + "\",";
    paramStr += "\"protocolID\":" + protocolID + ",";
    paramStr += "\"keyID\":\"" + keyID + "\",";
    paramStr += "\"counterparty\":\"" + counterparty + "\",";
    paramStr += "\"returnType\":\"string\"";

  }
  protected void caller() {
    super.caller("decrypt", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_DECRYPT", ">Decrypt:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("decrypt", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_DECRYPT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("decrypt", uuid, result);
    }
    Log.i("D_SDK_DECRYPT", "<Decrypt:called()");
  }
}