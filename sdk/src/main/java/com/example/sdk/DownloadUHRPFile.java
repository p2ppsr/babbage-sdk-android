package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
  @available(iOS 15.0, *)
  public func downloadUHRPFile(url: String, bridgeportResolvers: JSON) async -> Data? {
      // Construct the expected command to send
      var cmd:JSON = [
          "type":"CWI",
          "call":"downloadFile",
          "params": [
              "url": convertToJSONString(param: url),
              "bridgeportResolvers": bridgeportResolvers
          ]
      ]

      // TODO: Determine return type and best way to transfer large bytes of data.
      // Run the command and get the response JSON object
      let result = await runCommand(cmd: &cmd).value

      // Convert the array of JSON objects to an Array of UInt8s and then to a Data object
      // TODO: Optimize further
      if let arrayOfJSONObjects = result.objectValue?["result"]?.objectValue?["data"]?.objectValue?["data"]?.arrayValue {
          let byteArray:[UInt8] = arrayOfJSONObjects.map { UInt8($0.doubleValue!)}
          return Data(byteArray)
      }
      return nil
  }
  */
// public func downloadUHRPFile(url: String, bridgeportResolvers: JSON) async -> Data? {
public class DownloadUHRPFile extends SDKActivity.CallTypes implements Serializable {
  protected DownloadUHRPFile(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String url, String bridgeportResolvers) {
    paramStr = "";
    paramStr += "\"url\":\"" + url + "\",";
    paramStr += "\"bridgeportResolvers\":\"" + bridgeportResolvers + "\"";
  }
  protected void caller() {
    super.caller("downloadUHRPFile", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_DOWN_UHRP", ">DownloadUHRPFile:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("downloadUHRPFile", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_SUB_DIR_TRANS", "DownloadUHRPFile:JSON:ERROR:e=" + e);
      activity.returnUsingIntent("downloadUHRPFile", uuid, result);
    }
    Log.i("D_SDK_DOWN_UHRP", "<DownloadUHRPFile:called()");
  }
}