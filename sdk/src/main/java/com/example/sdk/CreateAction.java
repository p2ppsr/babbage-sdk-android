package com.example.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/*
    // Creates a new action using CWI.createAction
    @available(iOS 15.0, *)
    public func createAction(inputs: JSON? = nil, outputs: JSON, description: String, bridges: JSON? = nil, labels: JSON? = nil) async -> JSON {

        let params:[String:JSON] = [
            "inputs": inputs ?? nil,
            "outputs": outputs,
            "description": convertToJSONString(param: description),
            "bridges": bridges ?? nil,
            "labels": labels ?? nil
        ]
        let paramsAsJSON:JSON = getValidJSON(params: params)

        // Construct the expected command to send
        var cmd:JSON = [
            "type":"CWI",
            "call":"createAction",
            "params": paramsAsJSON
        ]

        // Run the command and get the response JSON object
        let responseObject = await runCommand(cmd: &cmd).value

        return responseObject
    }
  */
// public func createAction(inputs: JSON? = nil, outputs: JSON, description: String, bridges: JSON? = nil, labels: JSON? = nil) async -> JSON {
public class CreateAction extends SDKActivity.CallTypes implements Serializable {
  protected CreateAction(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }

  // Default values enforced by overloading constructor
  protected void process(String outputs, String description) {
    process(null, outputs, description);
  }
  protected void process(String inputs, String outputs, String description) {
    process(inputs, outputs, description, "");
  }
  protected void process(String inputs, String outputs, String description, String bridges) {
    process(inputs, outputs, description, bridges, "");
  }
  protected void process(String inputs, String outputs, String description, String bridges, String labels) {
    paramStr = "";
    paramStr += "\"inputs\":\"" + inputs + "\",";
    paramStr += "\"outputs\":\"" + outputs + "\",";
    paramStr += "\"description\":\"" + description + "\",";
    paramStr += "\"bridges\":\"" + bridges + "\",";
    paramStr += "\"labels\":\"" + labels + "\"";
  }
  protected void caller() {
    super.caller("createAction", paramStr);
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_CREATE_ACTION", "CreateAction:called():returnResult:" + returnResult);
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      activity.returnUsingIntent("createAction", uuid, result);
    } catch (JSONException e) {
      Log.e("D_SDK_CREATE_ACTION", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("createAction", uuid, result);
    }
  }
}