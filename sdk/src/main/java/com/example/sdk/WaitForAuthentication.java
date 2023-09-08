package com.example.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class WaitForAuthentication extends SDKActivity.CallTypes implements Serializable {
  private static SDKActivity activity = null;

  public WaitForAuthentication(SDKActivity activity) {
    //Log.i("D_SDK_WAIT_FOR_AUTHED", ">WaitForAuthentication:constructor()");
    WaitForAuthentication.activity = activity;
    //Log.i("D_SDK_WAIT_FOR_AUTHED", "<WaitForAuthentication:constructor()");
  }
  static String result = "";
  public void caller() {
    //Log.i("D_SDK_WAIT_FOR_AUTHED", "<>WaitForAuthentication:caller()");
    super.caller("waitForAuthentication");
    ////String s = "{\"type\":\"CWI\",\"call\":\"waitForAuthentication\",\"params\":{},\"originator\":\"projectbabbage.com\",\"id\":\"uuid\"}";
  }

  public void called(String returnResult) {
    //Log.i("D_SDK_WAIT_FOR_AUTHED", ">WaitForAuthentication:called()");
    if (!activity.waitingCallType.isEmpty()) {
      // Need to start the child thread to call the waiting run command
      //Log.i("D_SDK_WAIT_FOR_AUTHED", " WaitForAuthentication:called():activity.waitingCallType=" + activity.waitingCallType);
      SDKActivity.WorkerThread workerThread = new SDKActivity.WorkerThread();
      workerThread.start();
    }
    try {
      //Log.i("D_SDK_WAIT_FOR_AUTHED", " WaitForAuthentication:called():1");
      JSONObject jsonReturnResultObject = new JSONObject(returnResult);
      //Log.i("D_SDK_WAIT_FOR_AUTHED", " WaitForAuthentication:called():2");
      //String uuid = jsonReturnResultObject.get("uuid").toString();
      Log.i("D_SDK_WAIT_FOR_AUTHED", " WaitForAuthentication:called():3");
      result = jsonReturnResultObject.get("result").toString();
      if (result.equals("true")) {
        activity.returnUsingIntent("waitForAuthentication", result);
      }
    } catch (JSONException e) {
      Log.e("D_SDK_ENCRYPT", "JSON:ERROR:e=" + e);
      activity.returnUsingIntent("encrypt", result);
    }
    //Log.i("D_SDK_WAIT_FOR_AUTHED", "<WaitForAuthentication:called()");
  }
}