# babbage-sdk-android

Build Babbage Android apps in Android Studio

## Introduction

This package provides a client-side authentication solution that will let your Android application access a few cryptographic keys that belong to your users. You can use these for client-side encryption/decryption, authentication, signing, or even creating Bitcoin transactions.

See the Javascript repo for the full capabilities of building apps with Babbage: [@babbage/sdk](https://github.com/p2ppsr/babbage-sdk)

## Installation

To start creating apps with Babbage, you first need to install this package using either the Android Studio or similar IDE.

### Using Android Studio

![Screenshot from 2022-10-11 20-32-00](https://user-images.githubusercontent.com/27419107/195178274-4d5f524e-bb87-427a-8ddb-818891fc9138.png)


![Screenshot from 2022-10-11 20-37-54](https://user-images.githubusercontent.com/27419107/195176212-8e751017-4d5f-4760-8139-53605d40dec3.png)

Update your activity ```build.gradle``` file to include ```'com.babbage:sdk:0.0.9'``` as shown in the image above.

Here is an example file:

```Java
plugins {
    id 'com.android.application'
}

android {
    compileSdk 33

    defaultConfig {
        applicationId "com.example.androidSDK"
        minSdk 24
        targetSdk 33
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
}
allprojects {
    tasks.withType(JavaCompile) {
        options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"
    }
}
dependencies {
    implementation 'androidx.webkit:webkit:1.6.0-alpha01'
    implementation 'androidx.appcompat:appcompat:1.5.1'
    implementation 'com.google.android.material:material:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'org.chromium.net:cronet-embedded:98.4758.101'
    implementation 'com.babbage:sdk:0.0.9'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
}
```

## Example Usage

Here is an example usage from the example app [Crypton](https://github.com/p2ppsr/crypton-android).

MainActivity.java - Splash screen(calls your main activity)
```Java
package com.example.androidSDK;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.main_activity);
    getSupportActionBar().hide();
    new Handler().postDelayed(() -> {
      Intent intent = new Intent(MainActivity.this, MainActivity1.class);
      startActivity(intent);
      finish();
    }, 2000);
  }
}
```

MainActivity1.java - calls the babbage sdk
```Java
import com.example.sdk.SDKActivity;
import static com.example.sdk.SDKActivity.passActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import java.io.Serializable;
import java.util.UUID;

// Needs to be Serializable so we can pass over the class to the SDK
public class MainActivity1 extends AppCompatActivity implements Serializable {

  @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity1);
    Button encryptBtn = findViewById(R.id.encryptBtn);
    Button decryptBtn = findViewById(R.id.decryptBtn);
    TextView textView = findViewById(R.id.textView);
    String result = getIntent().getStringExtra("result");
    Log.i("MAIN_ON_CREATE", "onCreate():result:" + result);
    if (result != null) {
      String type = getIntent().getStringExtra("type");
      String uuid = getIntent().getStringExtra("uuid");
      Log.i("MAIN_ON_CREATE", "onCreate():type:" + type + ",uuid:" + uuid);
      if (type.equals("encrypt")) {
        textView.setText(result);
      }
      if (type.equals("decrypt")) {
        textView.setText(result);
      }
    }
    encryptBtn.setOnClickListener(
      v -> {
        Intent intent = new Intent(MainActivity1.this, SDKActivity.class);
        intent.putExtra("callingClass", passActivity(new MainActivity1()));
        intent.putExtra("type", "encrypt");
        intent.putExtra("uuid", UUID.randomUUID().toString());
        intent.putExtra("plaintext", (String) textView.getText());
        String PROTOCOL_ID = "crypton";
        String KEY_ID = "1";
        intent.putExtra("protocolID", PROTOCOL_ID);
        intent.putExtra("keyID", KEY_ID);
        startActivity(intent);
      }
    );
    decryptBtn.setOnClickListener(
      v -> {
        Intent intent = new Intent(MainActivity1.this, SDKActivity.class);
        intent.putExtra("callingClass", passActivity(new MainActivity1()));
        intent.putExtra("type", "decrypt");
        intent.putExtra("uuid", UUID.randomUUID().toString());
        intent.putExtra("ciphertext",(String)textView.getText());
        String PROTOCOL_ID = "crypton";
        String KEY_ID = "1";
        intent.putExtra("protocolID", PROTOCOL_ID);
        intent.putExtra("keyID", KEY_ID);
        startActivity(intent);
      }
    );
  }
}
```

## API

**Note:** All SDK functions must be called within an ```Intent``` which must include the ```callingClass``` being set to your main activity (```new MainActivity1()```) instance and passed using the ```passActivity()``` method, as shown above.

### encrypt

Encrypts data with a key belonging to the user. If a counterparty is provided, also allows the counterparty to decrypt the data. The same protocolID, and keyID parameters must be used when decrypting.

#### Parameters

*   `args.plaintext` **([string]()** The data to encrypt. If given as a string, it will be automatically converted to a base64 format.
*   `args.protocolID` **([string]()** Specify an identifier for the protocol under which this operation is being performed.
*   `args.keyID` **[string]()** An identifier for the message being encrypted. When decrypting, the same message ID will be required. This can be used to prevent key re-use, even when the same two users are using the same protocol to encrypt multiple messages. It can be randomly-generated, sequential, or even fixed.
*   `args.description` **[string]()?** Describe the high-level operation being performed, so that the user can make an informed decision if permission is needed. (optional, default `''`)

Returns **[String]()>** The encrypted ciphertext.

### decrypt

Decrypts data with a key belonging to the user. The same protocolID, keyID, counterparty and privileged parameters that were used during encryption must be used to successfully decrypt.

#### Parameters

*   `args.ciphertext` **([string]()** The encrypted data to decipher. If given as a string, it must be in base64 format.
*   `args.protocolID` **([Array]())** Specify an identifier for the protocol under which this operation is being performed. It should be the same protocol ID used during encryption.
*   `args.keyID` **[string]()** This should be the same message ID used during encryption.
*   `args.description` **[string]()?** Describe the high-level operation being performed, so that the user can make an informed decision if permission is needed. (optional, default `''`)

Returns **[String]())>** The decrypted plaintext.

More capabilities such as createAction, and createHmac coming soon!

## License

The license for this library, which is a wrapper for the proprietary Babbage API, is the Open BSV License. It can only be used on the BSV blockchain. The Babbage API itself, including the rights to create and host Babbage software or any other related infrastructure, is not covered by the Open BSV License and remains proprietary and restricted. The Open BSV License only extends to the code in this repository, and you are not permitted to host Babbage software, servers or create copies or alternative implementations of the proprietary Babbage API without other permission.
