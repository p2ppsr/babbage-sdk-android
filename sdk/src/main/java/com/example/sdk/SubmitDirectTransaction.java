package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func submitDirectTransaction(protocolID: String, transaction: JSON, senderIdentityKey: String, note: String, amount: Int, derivationPrefix: String? = nil) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"ninja.submitDirectTransaction",
          "params": [
              "protocol": convertToJSONString(param: protocolID),
              "transaction": transaction,
              "senderIdentityKey": convertToJSONString(param: senderIdentityKey),
              "note": convertToJSONString(param: note),
              "amount": try! JSON(amount),
              "derivationPrefix": try! JSON(derivationPrefix ?? "")
          ]
      ]

      // Run the command and get the response JSON object
      let provableCertificate = await runCommand(cmd: &cmd).value
      return provableCertificate
  }
  */
//   public func submitDirectTransaction(protocolID: String, transaction: JSON, senderIdentityKey: String, note: String, amount: Int, derivationPrefix: String? = nil) async -> JSON {
public class SubmitDirectTransaction extends SDKActivity.CallTypes implements Serializable {
  protected SubmitDirectTransaction(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String protocolID, String transaction, String senderIdentityKey, String note, String amount) {
    process(protocolID, transaction, senderIdentityKey, note, amount, "");
  }
  protected void process(String protocolID, String transaction, String senderIdentityKey, String note, String amount, String derivationPrefix) {
    paramStr = "";
    paramStr += "\"protocolID\":\"" + protocolID + "\",";
    paramStr += "\"transaction\":\"" + transaction + "\",";
    paramStr += "\"senderIdentityKey\":\"" + senderIdentityKey + "\",";
    paramStr += "\"note\":\"" + note + "\",";
    paramStr += "\"amount\":\"" + amount + "\",";
    paramStr += "\"derivationPrefix\":\"" + derivationPrefix + "\"";
  }
  protected void caller() {
    super.caller("submitDirectTransaction", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_SUB_DIR_TRANS", ">SubmitDirectTransaction:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("submitDirectTransaction", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_SUB_DIR_TRANS", "SubmitDirectTransaction:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("submitDirectTransaction", uuid, result);
    }
    Log.i("D_SDK_SUB_DIR_TRANS", "<SubmitDirectTransaction:called()");
  }
}