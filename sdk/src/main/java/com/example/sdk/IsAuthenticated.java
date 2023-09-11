package com.example.sdk;

import android.content.Intent;
import android.telecom.Call;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class IsAuthenticated extends SDKActivity.CallTypes implements Serializable {
  protected IsAuthenticated(SDKActivity activity) {
    //Log.i("D_SDK_IS_AUTH", ">IsAuthenticated:constructor()");
    SDKActivity.CallTypes.activity = activity;
    //Log.i("D_SDK_IS_AUTH", "<IsAuthenticated:constructor()");
  }
  protected void caller() {
    //Log.i("D_SDK_IS_AUTH", "<>IsAuthenticated:caller()");
    super.caller("isAuthenticated");
  }
  protected void called(String returnResult) {
    Log.i("D_SDK_IS_AUTH", ">IsAuthenticated:called():returnResult:" + returnResult);
    String result = "";
    String callTypeStr = "";
    try {
      returnResult = returnResult.replaceAll(":true,", ":\"true\",");
      returnResult = returnResult.replaceAll(":false,", ":\"false\",");
      //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():after replace returnResult:" + returnResult);
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      String uuid = jsonReturnResultObject.get("uuid").toString();
      result = (String)jsonReturnResultObject.get("result").toString();
      //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():before result:" + result);
      //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():activity.waitingCallType=" + activity.waitingCallType);
      //03Sep2023
      /*** set experimental */
      //result = experimentalResult;
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():returnResult:" + result);
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():activity.nextCallTypes.get()=" + activity.nextCallTypes.get());
      if (result.equals("true") && activity.nextCallTypes != null) {
      //if (result.equals("true") && !activity.waitingCallType.isEmpty()) {
        // Need to start the child thread to call the waiting run command
        //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():activity.waitingCallType=" + activity.waitingCallType);
        //09Sep2023-2307
        Log.i("D_SDK_IS_AUTH", "  IsAuthenticated:called():call runCommand()" + activity.nextCallTypes.get());
        /*** 10Sep2023-1851 ***/
        //activity.runCommand(activity.nextCallTypes.get());
        callTypeStr = SDKActivity.setInstance(activity.nextCallTypes);
        activity.returnUsingWaitingIntent(callTypeStr, uuid,"");
      } else {
        Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():EXPERIMENT set return to false");
        //Log.i("D_SDK_AUTH", " IsAuthenticated:called():after result:" + result);
        if (result.equals("false")) {
          //Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():return:false to App");
          activity.callTypes = new WaitForAuthentication(activity);
          activity.callTypes.caller();
          callTypeStr = SDKActivity.setInstance(activity.callTypes);
          if (activity.nextCallTypes != null) {
            activity.returnUsingWaitingIntent(callTypeStr, SDKActivity.setInstance(activity.nextCallTypes), uuid,  "openBabbage");
          }
        }
      }
    } catch (JSONException e) {
      Log.e("D_SDK_IS_AUTH", "JSON:ERROR:e=" + e);
      activity.returnUsingWaitingIntent(callTypeStr, uuid,"");
    }
    Log.i("D_SDK_IS_AUTH", "<IsAuthenticated:called()");
  }
}
