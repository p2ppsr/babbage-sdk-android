package com.example.sdk;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static android.util.Base64.DEFAULT;
import static android.util.Base64.decode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

// TODO Sparse logging is left since we are adding additional commands in the future and to help
// TODO team view what is happening on a physical device

public class SDKActivity extends AppCompatActivity {
  private static String uuid = "";
  private  static String counterparty = "";
  private static Object classObject = null; // Used for Intent callback
  private WebView webview = null; // Used for CWI-SDK
  protected CallTypes callTypes = null; // Current command
  protected CallTypes nextCallTypes = null; // Next command

  /*** High-level API Middleware ***/
  public static Intent encrypt(Context activityContext, Object instance, TextView messageText, String url) {
    return encrypt(activityContext, instance, messageText, "self", url, "");
  }
  public static Intent encrypt(Context activityContext, Object instance, TextView messageText, String counterparty, String url, String portal) {
    SDKActivity.counterparty = counterparty;
    Log.i("D_SDK_ENCRYPT", ">Intent:encrypt():returnResult:counterparty=" + counterparty);
    Log.i("D_SDK", " Intent:encrypt():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "encrypt");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("plaintext", messageText.getText().toString());
    intent.putExtra("protocolID", "[1, 'crypton']");
    intent.putExtra("keyID", "1");
    intent.putExtra("counterparty", counterparty);
    intent.putExtra("returnType", "string");
    intent.putExtra("url", url);
    Log.i("D_SDK", "<Intent:encrypt():" + intent);
    return intent;
  }
  public static Intent decrypt(Context activityContext, Object instance, TextView cipherText, String url) {
    return decrypt(activityContext, instance, cipherText, "self", url, "");
  }
  public static Intent decrypt(Context activityContext, Object instance, TextView cipherText, String counterparty, String url, String portal) {
    Log.i("D_SDK", "decrypt():portal:" + portal);
    SDKActivity.counterparty = counterparty;
    Log.i("D_SDK_INTENT_DECRYPT", "decrypt():counterparty=" + counterparty);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "decrypt");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("ciphertext", cipherText.getText().toString());
    intent.putExtra("protocolID", "[1, 'crypton']");
    intent.putExtra("keyID", "1");
    intent.putExtra("counterparty", counterparty);
    intent.putExtra("returnType", "string");
    intent.putExtra("url", url);
    Log.i("D_SDK", "<decrypt():" + intent);
    return intent;
  }
  public static Intent doWaitingType(Context activityContext, Object instance, String waitingInstance, String url, String portal) {
    Log.i("D_SDK", ">doWaitingType():waitingInstance=" + waitingInstance);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("type", "waitingInstance");
    intent.putExtra("waitingInstance", waitingInstance);
    intent.putExtra("url", url);
    intent.putExtra("portal", portal);
    Log.i("D_SDK", "<doWaitingType():" + intent);
    return intent;
  }
  public static Intent doWaitingType(Context activityContext, Object instance, String waitingInstance, String waitingNextInstance, String url, String portal) {
    Log.i("D_SDK", ">doWaitingType():waitingInstance=" + waitingInstance);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("type", "waitingInstance");
    intent.putExtra("waitingInstance", waitingInstance);
    intent.putExtra("waitingNextInstance", waitingNextInstance);
    intent.putExtra("url", url);
    intent.putExtra("portal", portal);
    Log.i("D_SDK", "<doWaitingType():" + intent);
    return intent;
  }
  public static Intent portal(Context activityContext, Object instance, String url) {
    Log.i("D_SDK", ">portal()");
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("type", "portal");
    intent.putExtra("url", url);
    Log.i("D_SDK", "<portal():" + intent);
    return intent;
  }
  //30Sep2023
  public static Intent isAuthenticated(Context activityContext, Object instance, String url) {
    return isAuthenticated(activityContext, instance, url, "");
  }
  public static Intent isAuthenticated(Context activityContext, Object instance, String url, String portal) {
    return isAuthenticated(activityContext, instance, url, "","");
  }
  public static Intent isAuthenticated(Context activityContext, Object instance, String url, String portal, String waitingTypeInstance) {
    Log.i("D_SDK", ">isAuthenticated():portal=" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "isAuthenticated");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    intent.putExtra("waitingInstance", waitingTypeInstance);
    Log.i("D_SDK", "<isAuthenticated():portal");
    return intent;
  }
  //25Aug2023
  public static Intent waitForAuthentication(Context activityContext, Object instance, String url) {
    return waitForAuthentication(activityContext, instance, url, "");
  }
  public static Intent waitForAuthentication(Context activityContext, Object instance, String url, String portal) {
    return waitForAuthentication(activityContext, instance, url, "", "");
  }
  public static Intent waitForAuthentication(Context activityContext, Object instance, String url, String portal, String waitingTypeInstance) {
    Log.i("D_SDK", "waitForAuthentication():portal:0" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "waitForAuthentication");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    intent.putExtra("waitingInstance", waitingTypeInstance);
    return intent;
  }
  public static Intent generateAES256GCMCryptoKey(Context activityContext, Object instance, String url) {
    Log.i("D_SDK", "intent generateAES256GCMCryptoKey()");
    return generateAES256GCMCryptoKey(activityContext, instance, url,"");
  }

  public static Intent generateAES256GCMCryptoKey(Context activityContext, Object instance, String url, String portal) {
    Log.i("D_SDK", "generateAES256GCMCryptoKey():portal:0" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    Log.i("D_SDK", "generateAES256GCMCryptoKey():portal:1");
    intent.putExtra("callingClass", setInstance(instance));
    Log.i("D_SDK", "generateAES256GCMCryptoKey():portal:2");
    intent.putExtra("portal", portal);
    intent.putExtra("type", "generateAES256GCMCryptoKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    return intent;
  }
  public static Intent openPortal(Context activityContext, Object instance, String url) {
    return openPortal(activityContext, instance, url, "");
  }
  public static Intent openPortal(Context activityContext, Object instance, String url, String portal) {
    Log.i("D_SDK", "openPortal():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "portal");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    return intent;
  }
  public static Intent getIdentityKey(Context activityContext, Object instance, String url) {
    Log.i("D_SDK", "getIdentityKey():constructor()");
    return getIdentityKey(activityContext, instance, url,"");
  }
  public static Intent getIdentityKey(Context activityContext, Object instance, String url, String portal) {
    Log.i("D_SDK", "getIdentityKey():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "getIdentityKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("url", url);
    //
    String identityKey = "";
    intent.putExtra("key", identityKey);
    return intent;
  }
  public static Intent encryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url) {
    return encryptUsingCryptoKey(activityContext, instance, cryptoKeyText, messageText, url,"");
  }
  public static Intent encryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url, String portal) {
    Log.i("D_SDK", "encryptUsingCryptoKey()portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "encryptUsingCryptoKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("plaintext", messageText.getText().toString());
    intent.putExtra("base64CryptoKey", cryptoKeyText.getText().toString());
    intent.putExtra("returnType", "base64");
    intent.putExtra("url", url);
    return intent;
  }
  public static Intent decryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url) {
    return decryptUsingCryptoKey(activityContext, instance, cryptoKeyText, messageText, url,"");
  }
  public static Intent decryptUsingCryptoKey(Context activityContext, Object instance, TextView cryptoKeyText, EditText messageText, String url, String portal) {
    Log.i("D_SDK", "decryptUsingCryptoKey():portal:" + portal);
    Intent intent = new Intent(activityContext, SDKActivity.class);
    intent.putExtra("callingClass", setInstance(instance));
    intent.putExtra("portal", portal);
    intent.putExtra("type", "decryptUsingCryptoKey");
    intent.putExtra("uuid", UUID.randomUUID().toString());
    intent.putExtra("ciphertext", messageText.getText().toString());
    intent.putExtra("base64CryptoKey", cryptoKeyText.getText().toString());
    intent.putExtra("returnType", "base64");
    intent.putExtra("url", url);
    return intent;
  }
  public abstract static class CallBaseTypes {
    String experimentalResult = "false";
    abstract void caller();
    abstract void caller(String type);
    abstract void caller(String type, String paramStr);
    abstract void called(String returnResult);
    abstract String get();
  }
  protected static class CallTypes extends CallBaseTypes implements Serializable {
    @SuppressLint("StaticFieldLeak")
    protected static SDKActivity activity = null;
    protected String paramStr = "";
    //protected String waitingTypeInstance = "";
    protected String result = "";
    protected String uuid = "";
    private String command = "";
    private CallBaseTypes type = null;
    protected CallTypes() {}
    protected  void caller(){}
    protected void caller(String type) {
      caller(type, "");
    }
    protected void caller(String type, String paramStr) {
      //Log.i("D_SDK", ">CallTypes:caller():type=" + type + ",paramStr=" + paramStr);
      command = "{";
      command += "\"type\":\"CWI\",";
      command += "\"call\":\"" +  type + "\",";
      command += "\"params\":{" + paramStr;
      command += "},";
      command += "\"originator\":\"projectbabbage.com\",";
      uuid = UUID.randomUUID().toString();
      command += "\"id\":\"" + uuid + "\"";
      command += "}";
      //Log.i("D_SDK", "<CallTypes:caller():command=" + command);
    }
    protected String get() {
      //Log.i("D_SDK", "<>CallTypes:get():" + command);
      return command;
    }
    protected void called(String returnResult) {
      type.called(returnResult);
    }
  }
  public class WebAppInterface {
    @JavascriptInterface
    public void closeBabbage() {
      Log.i("D_SDK_INTERFACE", "called closeBabbage():callType:" + callTypes + ",waitingCallType:" + nextCallTypes);
      returnUsingIntent("isAuthenticated", uuid, "true");
    }
    @JavascriptInterface
    public void isFocused() {
      Log.i("D_SDK_INTERFACE_IS_FOCUSED", "called isFocused()");
    }
    @JavascriptInterface
    public void waitForAuthentication(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void isAuthenticated(String returnResult) {
      Log.i("D_SDK_INTERFACE_IS_AUTH", ">isAuthenticated():returnResult=" + returnResult);
      Log.i("D_SDK_INTERFACE_IS_AUTH", " isAuthenticated():callTypes=" + callTypes);
      callTypes.called(returnResult);
      Log.i("D_SDK_INTERFACE_IS_AUTHENTICATED", "<isAuthenticated()");
    }
    @JavascriptInterface
    public void encrypt(String returnResult) {
      Log.i("D_SDK_INTERFACE_ENCRYPT", ">encrypt():returnResult=" + returnResult);
      Log.i("D_SDK_INTERFACE_ENCRYPT", " encrypt():callTypes=" + callTypes);
      callTypes.called(returnResult);
      Log.i("D_SDK_INTERFACE_ENCRYPT", "<encrypt()");
    }
    @JavascriptInterface
    public void decrypt(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void generateAES256GCMCryptoKey(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void encryptUsingCryptoKey(String returnResult) {
       callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void decryptUsingCryptoKey(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createAction(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createHmac(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void verifyHmac(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createSignature(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void verifySignature(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createCertificate(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void getCertificates(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void proveCertificate(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void submitDirectTransaction(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void getPublicKey(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void getVersion(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createPushDropScript(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void parapetRequest(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void downloadUHRPFile(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void newAuthriteRequest(String returnResult) {
      callTypes.called(returnResult);
    }
    @JavascriptInterface
    public void createOutputScriptFromPubKey(String returnResult) { callTypes.called(returnResult); }
  }


  /*** Helper methods ***/
  protected String checkIsBase64(String str) {
    String returnStr = str;
    if (!isBase64(str)) {
      returnStr = convertStringToBase64(str);
    }
    return returnStr;
  }
  protected void returnError(String str, String type, String message, String field){
    Intent intent = new Intent(SDKActivity.this, classObject.getClass());
    intent.putExtra("result", str);
    intent.putExtra("type", type);
    // Has to be available to run the queued run command
    //String uuid = "";
    intent.putExtra("uuid", uuid);
    intent.putExtra("error", message);
    intent.putExtra("field", field);
    startActivity(intent);
  }
  protected String checkForJSONErrorAndReturnToApp(String str, String type, String field) {
    String resultStr = "";
    try {
      new JSONObject(str);
      resultStr = str;
    } catch (JSONException e) {
      returnError(str, type, "invalid JSON", field);
    }
    return resultStr;
  }

  /*
  // Returns a JSON object with non-null values
  func getValidJSON(params: [String: JSON]) -> JSON {
    var paramsAsJSON:JSON = []
    for param in params {
      if (param.value != nil) {
        paramsAsJSON = paramsAsJSON.merging(with: [param.key: param.value])
      }
    }
    return paramsAsJSON
  }
  // TODO Not needed as JSON has to be passed as strings by the App
  private JSONObject getValidJSON(Stack<JSONObject> params) {
    while(!params.isEmpty()) {
      if (params.pop() != null) {
        JSONFormatStr
      }
    }
  }
  // Helper function which returns a JSON type string
  func convertToJSONString(param: String) -> JSON {
    return try! JSON(param)
  }
  */
  private JSONObject convertToJSONString(String param) {
    try {
      return new JSONObject(param);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO Additional functionality as per iOS version
  /*
  // Generates a secure random base64 string base on provided byte length
  public func generateRandomBase64String(byteCount: Int) -> String {
    var bytes = [UInt8](repeating: 0, count: byteCount)
    let status = SecRandomCopyBytes(
      kSecRandomDefault,
      byteCount,
      &bytes
        )
    // A status of errSecSuccess indicates success
    if status != errSecSuccess {
      return "Error"
    }
    let data = Data(bytes)
    return data.base64EncodedString()
  }
  */
  private static String generateRandomBase64String(int length) {
    int byteLength = ((length + 3) / 4) * 3; // base 64: 3 bytes = 4 chars
    byte[] byteVal = new byte[byteLength];
    new Random(System.currentTimeMillis()).nextBytes(byteVal);

    // Change '/' and '\' with '$' in case the string is used as file name
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new String(Base64.getEncoder().encode(byteVal), 0, length).replace('/', '$');
    }
    return "Error";
  }
  protected static String convertBase64ToString(String codedStr){
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      //Log.i("D_SDK", "convertBase64ToString()");
      byte[] decodedStr = Base64.getUrlDecoder().decode(codedStr);
      return new String(decodedStr);
    }
    return "";
  }
  private void doJavaScript(WebView webview, String javascript) {
    Log.i("D_SDK", "doJavaScript():javascript:" + javascript);
    webview.evaluateJavascript(javascript, null);
  }

  // Generic 'run' command

  protected void runCommand(String s) {
    //Log.i("D_SDK", ">runCommand():s:" + s);
    s = "window.postMessage(" + s + ")";
    //Log.i("D_SDK", " runCommand():s:" + s);
    doJavaScript(webview, s);
    //Log.i("D_SDK", "<runCommand()");
  }

  // Convert the string to base64 so it can be passed over the net
  protected static String convertStringToBase64(String str) {
    byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
    if (
      android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
    ) {
      return new String(Base64.getEncoder().encodeToString(byteArray).getBytes());
    }
    return "Error";
  }

  // TODO (Not currently required) Helper to format complex JSON objects into an appropriate string
  private static String JSONFormatStr(String name, String key) {
    try {
      return new JSONObject("{\"" + name + "\":\"" + key + "\"}").toString();
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }
  private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
    Object ret = null;
    ObjectInputStream in = null;
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      in = new ObjectInputStream(bis);
      ret = in.readObject();
    } catch(IOException e) {
      throw e;
    } catch(ClassNotFoundException e) {
      throw e;
    }
    return ret;
  }
  private static byte[] convertToBytes(Object object) {
    //Log.i("D_SDK_CONV_TO_BYTES", ">convertToBytes():object=" + object);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(object);
    } catch (IOException e) {
      Log.e("D_SDK_CONV_TO_BYTES", "convertToBytes():ERROR:" + e);
      Log.e("D_SDK_CONV_TO_BYTES", "<convertToBytes():" + Arrays.toString(bos.toByteArray()));
    }
    //Log.i("D_SDK_CONV_TO_BYTES", "<convertToBytes()");
    return bos.toByteArray();
  }

  // Called by App to pass over calling class to be used by Intent callback
  // Raises compile time warning as not used
  protected static String setInstance(Object instance) {
    //Log.i("D_SDK", ">setInstance():instance=" + instance);
    byte[] bytes = {};
    try {
      bytes = convertToBytes(instance);
      //Log.i("D_SDK", " setInstance():bytes=" + bytes);
    } catch (Exception e) {
      Log.e("D_SDK", " setInstance():ERROR:e=" + e);
    }
    byte[] base64Encoded = null;
    // Convert the string to base64 so it can be passed over the net
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      base64Encoded = Base64.getEncoder().encodeToString(bytes).getBytes();
    }
    byte[] finalBase64Encoded = base64Encoded;
    //Log.i("D_SDK", "<setInstance():finalBase64Encoded=" + finalBase64Encoded);
    return new String(finalBase64Encoded);
  }
  private static Object getInstance(String instanceStr) {
    Log.i("D_SDK", ">getInstance()");
    byte[] bytes = decode(instanceStr, DEFAULT);
    Log.i("D_SDK", " getInstance():created bytes=" + bytes.toString());
    try {
      Log.i("D_SDK", " getInstance():create classObject");
      Object o = convertFromBytes(bytes);
      Log.i("D_SDK", " getInstance():created instance=" + o);
      Log.i("D_SDK", "<getInstance()");
      return o;
    } catch (IOException e) {
      Log.e("D_SDK", " getInstance():throw new RuntimeException:e=" + e);
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      Log.e("D_SDK", " getInstance():throw new ClassNotFoundException:e=" + e);
      throw new RuntimeException(e);
    }
  }
  @SuppressLint("SetJavaScriptEnabled")
  private void setWebview(String url) {
    webview.getSettings().setDomStorageEnabled(true);
    webview.getSettings().setJavaScriptEnabled(true);
    webview.getSettings().setSupportMultipleWindows(true);
    webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    webview.getSettings().setUserAgentString("babbage-webview-inlay");
    webview.addJavascriptInterface(new WebAppInterface(), "androidMessageHandler");
    webview.loadUrl(url);
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i("D_SDK", ">onCreate()");
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();
    Intent intent = getIntent();
    String type = intent.getStringExtra("type");
    Log.i("D_SDK", " onCreate():type:" + type);
    String portal = intent.getStringExtra("portal");
    Log.i("D_SDK", " onCreate():portal:" + portal);
    setContentView(R.layout.activity_sdk);
    webview = findViewById(R.id.web_html);
    // Set up class object to build return intent
    String callingClass = intent.getStringExtra("callingClass");
    Log.i("D_SDK", " onPageStarted():created callingClass=" + callingClass);
    classObject = getInstance(callingClass);
    String url = intent.getStringExtra("url");
    Log.i("D_SDK", " onCreate():url:" + url);
    if (type.equals("portal") || (portal != null && ((portal.equals("openBabbage"))))) {
      Log.i("D_SDK", " onCreate():open portal");
      webview.setWebChromeClient (
        new WebChromeClient() {
          @Override
          public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            Log.i("D_SDK", "onJsConfirm():result:" + result);
            return super.onJsConfirm(view, url, message, result);
          }
          @Override
          public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.i("D_SDK", "onJsAlert():result" + result);
            return super.onJsAlert(view, url, message, result);
          }
        }
      );
      webview.setWebViewClient (new WebViewClient() {});
      // required for most of our React modules (Hades, Prosperity etc.)
      setWebview(url);
      Log.i("D_SDK", " onCreate():opened portal");
     } else {
      Log.i("D_SDK", " onCreate():call finish()");
      finish();
      String instanceStr = intent.getStringExtra("waitingInstance");
      String nextInstanceStr = intent.getStringExtra("waitingNextInstance");
      Log.i("D_SDK", " onCreate():instanceStr=" + instanceStr);
      Log.i("D_SDK", " onCreate():nextInstanceStr=" + nextInstanceStr);
      if (instanceStr != null && !instanceStr.equals("")) {
        // Need to run waiting command
        Log.i("D_SDK", " onCreate():instanceStr is not null");
        callTypes = (CallTypes) getInstance(instanceStr);
        Log.i("D_SDK", " onCreate():nextInstanceStr=" + nextInstanceStr);
        if (nextInstanceStr != null) {
          nextCallTypes = (CallTypes) getInstance(nextInstanceStr);
          Log.i("D_SDK", " onCreate():nextCallTypes=" + nextCallTypes);
          Log.i("D_SDK", " onCreate():portal=" + portal);
        }
      } else if (type.equals("waitForAuthentication")) {
        callTypes = new WaitForAuthentication(SDKActivity.this);
        callTypes.caller();
      } else if (type.equals("isAuthenticated")) {
        callTypes = new IsAuthenticated(SDKActivity.this);
        callTypes.caller();
        finish();
      }
      else {
        // Need to run isAuthenticated first
        Log.i("D_SDK", " onCreate():type=" + type);
        if (type.equals("encrypt")) {
          SDKActivity.counterparty  = intent.getStringExtra("counterparty");
          Log.i("D_SDK", " onCreate():encrypt:SDKActivity.counterparty=" + SDKActivity.counterparty);
          nextCallTypes = new Encrypt(SDKActivity.this);
          if (counterparty == null) {
            ((Encrypt) nextCallTypes).process(
              intent.getStringExtra("plaintext"),
              intent.getStringExtra("protocolID"),
              intent.getStringExtra("keyID")
            );
          } else {
            ((Encrypt) nextCallTypes).process(
              intent.getStringExtra("plaintext"),
              intent.getStringExtra("protocolID"),
              intent.getStringExtra("keyID"),
              intent.getStringExtra("counterparty")
            );
          }
        } else if (type.equals("decrypt")) {
          SDKActivity.counterparty  = intent.getStringExtra("counterparty");
          Log.i("D_SDK", " onPageFinished():decrypt:SDKActivity.counterparty=" + SDKActivity.counterparty);
          nextCallTypes = new Decrypt(SDKActivity.this);
          if (counterparty == null) {
            ((Decrypt)nextCallTypes).process(
              intent.getStringExtra("ciphertext"),
              intent.getStringExtra("protocolID"),
              intent.getStringExtra("keyID")
            );
          } else {
            ((Decrypt)nextCallTypes).process(
              intent.getStringExtra("ciphertext"),
              intent.getStringExtra("protocolID"),
              intent.getStringExtra("keyID"),
              intent.getStringExtra("counterparty")
            );
          }
        } else if (type.equals("getIdentityKey")) {
          Log.i("D_SDK", " onPageFinished():getIdentityKey");
          nextCallTypes = new GetPublicKey(SDKActivity.this);
          ((GetPublicKey)nextCallTypes).process(
            "\"identity key\"",
            "null",
            false,
            true,
            "",
            ""
          );
        } else if (type.equals("getPublicKey")) {
          SDKActivity.counterparty  = intent.getStringExtra("counterparty");
          Log.i("D_SDK", " onPageFinished():getPublicKey:SDKActivity.counterparty=" + SDKActivity.counterparty);
          nextCallTypes = new GetPublicKey(SDKActivity.this);
          ((GetPublicKey)nextCallTypes).process(
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("keyID"),
            Objects.equals(intent.getStringExtra("privileged"), "true"),
            Objects.equals(intent.getStringExtra("identityKey"), "true"),
            intent.getStringExtra("reason"),
            intent.getStringExtra("counterparty").equals("") ? intent.getStringExtra("counterparty") : "self",
            intent.getStringExtra("description")
          );
        } else if (type.equals("generateAES256GCMCryptoKey")) {
          Log.i("D_SDK", "call push GenerateAES256GCMCryptoKey()");
          nextCallTypes = new GenerateAES256GCMCryptoKey(SDKActivity.this);
          ((GenerateAES256GCMCryptoKey) nextCallTypes).process();
          Log.i("D_SDK", "called push GenerateAES256GCMCryptoKey()");
        } else if (type.equals("encryptUsingCryptoKey")) {
          String base64CryptoKey = intent.getStringExtra("base64CryptoKey");
          if (!isBase64(base64CryptoKey)) {
            returnError(base64CryptoKey, "encryptUsingCryptoKey", "invalid base64 crypto key", "base64CryptoKey");
          }
          nextCallTypes = new EncryptUsingCryptoKey(SDKActivity.this);
          ((EncryptUsingCryptoKey) nextCallTypes).process(
            intent.getStringExtra("plaintext"),
            base64CryptoKey,
            !intent.getStringExtra("returnType").equals("") ? intent.getStringExtra("returnType") : "base64"
          );
        } else if (type.equals("decryptUsingCryptoKey")) {
          String base64CryptoKey = intent.getStringExtra("base64CryptoKey");
          if (!isBase64(base64CryptoKey)) {
            returnError(base64CryptoKey, "decryptUsingCryptoKey", "invalid base64 crypto key", "base64CryptoKey");
          }
          nextCallTypes = new DecryptUsingCryptoKey(SDKActivity.this);
          ((DecryptUsingCryptoKey) nextCallTypes).process(
            intent.getStringExtra("ciphertext"),
            base64CryptoKey,
            !intent.getStringExtra("returnType").equals("") ? intent.getStringExtra("returnType") : "base64"
          );
        } else if (type.equals("createAction")) {
          nextCallTypes = new CreateAction(SDKActivity.this);
          ((CreateAction) nextCallTypes).process(
            intent.getStringExtra("inputs"),
            intent.getStringExtra("outputs"),
            intent.getStringExtra("description"),
            intent.getStringExtra("bridges"),
            intent.getStringExtra("labels")
          );
        } else if (type.equals("createHmac")) {
          nextCallTypes = new CreateHmac(SDKActivity.this);
          ((CreateHmac) nextCallTypes).process(
            intent.getStringExtra("data"),
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("keyID"),
            intent.getStringExtra("description"),
            !intent.getStringExtra("counterparty").equals("") ? intent.getStringExtra("counterparty") : "self",
                  !Objects.equals(intent.getStringExtra("privileged"), "false")
          );
        } else if (type.equals("verifyHmac")) {
          nextCallTypes = new VerifyHmac(SDKActivity.this);
          ((VerifyHmac) nextCallTypes).process(
            intent.getStringExtra("data"),
            intent.getStringExtra("hmac"),
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("keyID"),
            intent.getStringExtra("description"),
            intent.getStringExtra("counterparty"),
                  !Objects.equals(intent.getStringExtra("privileged"), "false")
          );
        } else if (type.equals("createSignature")) {
          nextCallTypes = new CreateSignature(SDKActivity.this);
          ((CreateSignature) nextCallTypes).process(
            intent.getStringExtra("data"),
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("keyID"),
            intent.getStringExtra("description"),
            intent.getStringExtra("counterparty"),
                  !Objects.equals(intent.getStringExtra("privileged"), "false")
          );
        } else if (type.equals("verifySignature")) {
          nextCallTypes = new VerifySignature(SDKActivity.this);
          ((VerifySignature) nextCallTypes).process(
            intent.getStringExtra("data"),
            intent.getStringExtra("signature"),
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("keyID"),
            intent.getStringExtra("description"),
            intent.getStringExtra("counterparty"),
                  !Objects.equals(intent.getStringExtra("privileged"), "false"),
            intent.getStringExtra("reason")
          );
        } else if (type.equals("createCertificate")) {
          nextCallTypes = new CreateCertificate(SDKActivity.this);
          ((CreateCertificate) nextCallTypes).process(
            intent.getStringExtra("certificateType"),
            intent.getStringExtra("fieldObject"),
            intent.getStringExtra("certifierUrl"),
            intent.getStringExtra("certifierPublicKey")
          );
        } else if (type.equals("getCertificates")) {
          nextCallTypes = new GetCertificates(SDKActivity.this);
          ((GetCertificates) nextCallTypes).process(
            intent.getStringExtra("certifiers"),
            intent.getStringExtra("types")
          );
        } else if (type.equals("proveCertificate")) {
          nextCallTypes = new ProveCertificate(SDKActivity.this);
          ((ProveCertificate) nextCallTypes).process(
            intent.getStringExtra("certificate"),
            intent.getStringExtra("fieldsToReveal"),
            intent.getStringExtra("verifierPublicIdentityKey")
          );
        } else if (type.equals("submitDirectTransaction")) {
          nextCallTypes = new SubmitDirectTransaction(SDKActivity.this);
          ((SubmitDirectTransaction) nextCallTypes).process(
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("transaction"),
            intent.getStringExtra("senderIdentityKey"),
            intent.getStringExtra("note"),
            intent.getStringExtra("amount"),
            intent.getStringExtra("derivationPrefix")
          );
        } else if (type.equals("getVersion")) {
          nextCallTypes = new GetVersion(SDKActivity.this);
        } else if (type.equals("createPushDropScript")) {
          nextCallTypes = new CreatePushDropScript(SDKActivity.this);
          ((CreatePushDropScript) nextCallTypes).process(
            intent.getStringExtra("fields"),
            intent.getStringExtra("protocolID"),
            intent.getStringExtra("keyID")
          );
        } else if (type.equals("downloadUHRPFile")) {
          nextCallTypes = new DownloadUHRPFile(SDKActivity.this);
          ((DownloadUHRPFile) nextCallTypes).process(
            intent.getStringExtra("url"),
            intent.getStringExtra("bridgeportResolvers")
          );
        } else if (type.equals("newAuthriteRequest")) {
          nextCallTypes = new NewAuthriteRequest(SDKActivity.this);
          ((NewAuthriteRequest) nextCallTypes).process(
            intent.getStringExtra("params"),
            intent.getStringExtra("requestUrl"),
            intent.getStringExtra("fetchConfig")
          );
        }
        if (type.equals("createOutputScriptFromPubKey")) {
          nextCallTypes = new CreateOutputScriptFromPubKey(SDKActivity.this);
          ((CreateOutputScriptFromPubKey) nextCallTypes).process(
            intent.getStringExtra("derivedPublicKey")
          );
        }
        nextCallTypes.caller();

        // Run isAuthenticated command first
        callTypes = new IsAuthenticated(SDKActivity.this);
        callTypes.caller();
      }
      webview.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageFinished(WebView view, String url) {
            runCommand(callTypes.get());
          }
      });
      setWebview(url);
    }
  }
  public void returnUsingWaitingIntent(String waitingTypeInstance, String waitingNextTypeInstance, String uuid, String portal) {
    Log.i("D_SDK_RETURN_WAITING_INTENT", ">returnUsingWaitingIntent():portal=" + portal + ",uuid=" + uuid + ",waitingTypeInstance=" + waitingTypeInstance +  ",waitingNextTypeInstance=" + waitingNextTypeInstance);
    Intent intent = null;
    try {
      intent = new Intent(this, classObject.getClass());
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    assert intent != null;
    intent.putExtra("uuid", uuid);
    intent.putExtra("waitingInstance", waitingTypeInstance);
    intent.putExtra("waitingNextInstance", waitingNextTypeInstance);
    intent.putExtra("portal", portal);
    try {
      startActivity(intent);
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    Log.i("D_SDK_RETURN_WAITING_INTENT", "<returnUsingWaitingIntent():called startActivity()");
  }
  public void returnUsingWaitingIntent(String waitingTypeInstance, String uuid, String portal) {
    Log.i("D_SDK_RETURN_WAITING_INTENT", ">returnUsingWaitingIntent():portal=" + portal + ",uuid=" + uuid + ",waitingTypeInstance=" + waitingTypeInstance);
    Intent intent = null;
    try {
      intent = new Intent(this, classObject.getClass());
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    assert intent != null;
    intent.putExtra("uuid", uuid);
    intent.putExtra("waitingInstance", waitingTypeInstance);
    intent.putExtra("portal", portal);
    try {
      startActivity(intent);
    } catch (Exception e) {
      Log.e("D_SDK_RETURN_WAITING_INTENT", " returnUsingWaitingIntent():ERROR:e=" + e);
    }
    Log.i("D_SDK_RETURN_WAITING_INTENT", "<returnUsingWaitingIntent():called startActivity()");
  }
  public void returnUsingIntent(String type, String uuid, String result) {
    Log.i("D_SDK_RETURN_INTENT", ">returnUsingIntent():type=" + type + ",uuid=" + uuid + ",result=" + result);
    returnUsingIntent(type, uuid, result, "");
  }
  public void returnUsingIntent(String type, String uuid, String result, String portal) {
    Log.i("D_SDK_RETURN_INTENT", ">returnUsingIntent():type=" + type + ",uuid=" + uuid + ",portal=" + portal + ",result=" + result);
    Intent intent = null;
    try {
      intent = new Intent(this, classObject.getClass());
    } catch (Exception e) {
      Log.i("D_SDK_RETURN_INTENT", " returnUsingIntent():ERROR:e=" + e);
    }
    assert intent != null;
    intent.putExtra("type", type);
    intent.putExtra("uuid", uuid);
    intent.putExtra("result", result);
    intent.putExtra("counterparty", counterparty);
    intent.putExtra("portal", portal);
    try {
      startActivity(intent);
      Log.i("D_SDK_RETURN_INTENT", "<returnUsingIntent():called startActivity()");
    } catch (Exception e) {
      Log.i("D_SDK_RETURN_INTENT", " returnUsingIntent():ERROR:e=" + e);
    }
  }
}
