package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func proveCertificate(certificate: JSON, fieldsToReveal: JSON? = nil, verifierPublicIdentityKey: String) async -> JSON {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"proveCertificate",
          "params": [
              "certificate": certificate,
              "fieldsToReveal": fieldsToReveal ?? nil,
              "verifierPublicIdentityKey": convertToJSONString(param: verifierPublicIdentityKey)
          ]
      ]


      // Run the command and get the response JSON object
      let provableCertificate = await runCommand(cmd: &cmd).value
      return provableCertificate
  }
  */
// public func proveCertificate(certificate: JSON, fieldsToReveal: JSON? = nil, verifierPublicIdentityKey: String) async -> JSON {
// Default values enforced by overloading constructor
public class ProveCertificate extends SDKActivity.CallTypes implements Serializable {
  protected ProveCertificate(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }
  protected void process(String certificate, String verifierPublicIdentityKey) {
    process(certificate, "", verifierPublicIdentityKey);
  }
  protected void process(String certificate, String fieldsToReveal, String verifierPublicIdentityKey) {
    paramStr = "";
    paramStr += "\"certificate\":\"" + certificate + "\",";
    paramStr += "\"fieldsToReveal\":\"" + fieldsToReveal + "\",";
    paramStr += "\"verifierPublicIdentityKey\":\"" + verifierPublicIdentityKey + "\"";
  }
  protected void caller() {
    super.caller("proveCertificate", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_PROVE_CERT", ">ProveCertificate:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("proveCertificate", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_PROVE_CERT", "ProveCertificate:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("proveCertificate", uuid, result);
    }
    Log.i("D_SDK_PROVE_CERT", "<ProveCertificate:called()");
  }
}