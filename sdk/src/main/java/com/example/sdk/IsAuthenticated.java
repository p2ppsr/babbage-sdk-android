package com.example.sdk;

import android.content.Intent;
import android.telecom.Call;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class IsAuthenticated extends SDKActivity.CallTypes implements Serializable {
  protected IsAuthenticated(SDKActivity activity) {
    SDKActivity.CallTypes.activity = activity;
  }
  protected void caller() {
    //Log.i("D_SDK_IS_AUTH", "<>IsAuthenticated:caller()");
    super.caller("isAuthenticated");
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_IS_AUTH", ">IsAuthenticated:called():returnResult:" + returnResult);
    String callTypeStr = "";
    try {

      // Needs to be JSON strings
      returnResult = returnResult.replaceAll(":true,", ":\"true\",");
      returnResult = returnResult.replaceAll(":false,", ":\"false\",");
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():after replace returnResult:" + returnResult);
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      uuid = jsonReturnResultObject.get("uuid").toString();
      result = jsonReturnResultObject.get("result").toString();
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():before result:" + result);
      //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():activity.waitingCallType=" + activity.waitingCallType);
      //03Sep2023
      /*** set experimental ***/
      //result = experimentalResult;
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():return" + result);
      if (result.equals("true") || activity.nextCallTypes == null) {
        if (activity.nextCallTypes == null) {
          if (result.equals("true")) {
            Log.i("D_SDK_IS_AUTH", " IsAuthenticated:return direct with true");
            activity.returnUsingIntent("isAuthenticated", uuid, result);
          } else {
            Log.i("D_SDK_IS_AUTH", " IsAuthenticated:false so set WaitForAuthentication as waiting type");
            activity.callTypes = new WaitForAuthentication(activity);
            activity.callTypes.caller();
            callTypeStr = SDKActivity.setInstance(activity.callTypes);
            activity.returnUsingWaitingIntent(callTypeStr, uuid, "openBabbage");
          }
        } else {
          Log.i("D_SDK_IS_AUTH", "  IsAuthenticated:called():call runCommand()" + activity.nextCallTypes.get());
          /*** 10Sep2023-1851 ***/
          callTypeStr = SDKActivity.setInstance(activity.nextCallTypes);
          activity.returnUsingWaitingIntent(callTypeStr, uuid, "");
        }
      } else {
        Log.i("D_SDK_AUTH", " IsAuthenticated:called():after result:" + result);
        //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():return:false to App");
        activity.callTypes = new WaitForAuthentication(activity);
        activity.callTypes.caller();
        callTypeStr = SDKActivity.setInstance(activity.callTypes);
        if (activity.nextCallTypes != null) {
          activity.returnUsingWaitingIntent(callTypeStr, SDKActivity.setInstance(activity.nextCallTypes), uuid,  "openBabbage");
        }
      }
    } catch (JSONException e) {
      Log.e("D_SDK_IS_AUTH", "JSON:ERROR:e=" + e);
      activity.returnUsingWaitingIntent(callTypeStr, uuid,"");
    }
    Log.i("D_SDK_IS_AUTH", "<IsAuthenticated:called()");
  }
}
