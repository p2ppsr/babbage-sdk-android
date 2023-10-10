package com.example.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class WaitForAuthentication extends SDKActivity.CallTypes implements Serializable {
  protected WaitForAuthentication(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }
  public void caller() {
    //Log.i("D_SDK_WAIT_FOR_AUTHED", "<>WaitForAuthentication:caller()");
    super.caller("waitForAuthentication");
  }

  public void called(String returnResult) {
    //Log.i("D_SDK_WAIT_FOR_AUTHED", ">WaitForAuthentication:called():returnResult=" + returnResult);
    //Log.i("D_SDK_WAIT_FOR_AUTHED", " WaitForAuthentication:called():1 activity.waitingCallType=" + activity.waitingCallType);
    String callTypeStr = "";
    experimentalResult = "true";
    try {
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      result = jsonReturnResultObject.get("result").toString();
      uuid = jsonReturnResultObject.get("uuid").toString();
      Log.i("D_SDK_WAIT_FOR_AUTHED", " WaitForAuthentication:called():result=" + result);
      if (result.equals("true")) {
        if (activity.nextCallTypes != null) {

          // return next type to be called
          callTypeStr = SDKActivity.setInstance(activity.nextCallTypes);
          activity.returnUsingWaitingIntent(callTypeStr, uuid, "");
        }
      } else {
        // return result
        activity.returnUsingIntent("waitForAuthentication", uuid, result);
      }
    } catch (JSONException e) {
      Log.e("D_SDK_WAIT_FOR_AUTHED", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("waitForAuthentication", uuid, result);
    }
    Log.i("D_SDK_WAIT_FOR_AUTHED", "<WaitForAuthentication:called()");
  }
}