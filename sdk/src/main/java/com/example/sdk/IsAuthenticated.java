package com.example.sdk;

import android.content.Intent;
import android.telecom.Call;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class IsAuthenticated extends SDKActivity.CallBaseTypes implements Serializable {
  private static SDKActivity activity = null;
  private static String waitingTypeInstance = "";

  public IsAuthenticated(SDKActivity activity) {
    Log.i("D_SDK_IS_AUTH", ">IsAuthenticated:constructor()");
    IsAuthenticated.activity = activity;
    Log.i("D_SDK_IS_AUTH", "<IsAuthenticated:constructor()");
  }
  public String caller() {
    Log.i("D_SDK_IS_AUTH", "<>IsAuthenticated:caller()");
    return "{\"type\":\"CWI\",\"call\":\"isAuthenticated\",\"params\":{},\"originator\":\"projectbabbage.com\",\"id\":\"uuid\"}";
  }
  public void called(String returnResult) {
    Log.i("D_SDK_IS_AUTH", ">IsAuthenticated:called():returnResult:" + returnResult);
    String result = "";
    try {
      returnResult = returnResult.replaceAll(":true,", ":\"true\",");
      returnResult = returnResult.replaceAll(":false,", ":\"false\",");
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():after replace returnResult:" + returnResult);
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      String uuid = jsonReturnResultObject.get("uuid").toString();
      result = (String)jsonReturnResultObject.get("result").toString();
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():before result:" + result);
      Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():activity.waitingCallType=" + activity.waitingCallType);
      //03Sep2023
      /*** set experimental */
      result = "false";
      if (result.equals("true") && !activity.waitingCallType.isEmpty()) {
        // Need to start the child thread to call the waiting run command
        Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():activity.waitingCallType=" + activity.waitingCallType);
        SDKActivity.WorkerThread workerThread = new SDKActivity.WorkerThread();
        workerThread.start();
      } else {
        Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():EXPERIMENT set return to false");
        Log.i("D_SDK_AUTH", " IsAuthenticated:called():after result:" + result);
        if (result.equals("false")) {
          Log.i("D_SDK_IS_AUTH", " IsAuthenticated:called():return:false to App");
          waitingTypeInstance = SDKActivity.setInstance(new WaitForAuthentication(activity));
          if (!activity.waitingCallType.isEmpty()) {
            activity.returnUsingWaitingIntent(waitingTypeInstance, SDKActivity.setInstance(activity.waitingCallType.get(0)), "openBabbage");
          }
        }
      }
    } catch (JSONException e) {
      activity.returnUsingWaitingIntent(waitingTypeInstance, "openBabbage");
    }
    Log.i("D_SDK_IS_AUTH", "<IsAuthenticated:called()");
  }
}
