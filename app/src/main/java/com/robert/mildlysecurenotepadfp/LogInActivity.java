package com.robert.mildlysecurenotepadfp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class LogInActivity extends AppCompatActivity {

    Integer mode = 0;
    byte[] subKey;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_in);

        TextView textView = (TextView)findViewById(R.id.logInTextView);
        textView.setText(R.string.login_pass);
        Intent intent = getIntent();
        try {
            mode = intent.getIntExtra("change", 0);
            Log.i("MODE", "LogInActivity onCreate()" + Integer.toString(mode));
            if (mode == -1) {
                textView.setText(R.string.log_in_to_change_pass);
            } else {
                textView.setText(R.string.login_pass);
            }
        } catch (Exception e) {
            Log.i("ERROR", "LogInActivity");
            e.printStackTrace();
        }

    }

    private byte[] getNumberOfBytes(byte[] byteArray, int offset, int length) throws ArrayIndexOutOfBoundsException {

        byte[] targetArray = new byte[length];
        if (length >= 0) System.arraycopy(byteArray, offset, targetArray, 0, length);
        return targetArray;

    }

    private boolean validatePassword(String check, String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {

        byte[] generatedCheckSecret = Crypto.generateKey(check, salt);
        byte[] checkToHash = getNumberOfBytes(generatedCheckSecret, 0, 16);
        byte[] checkSecret = getNumberOfBytes(generatedCheckSecret, 16, 32);
        String checkToHashString = new String(checkToHash);
        String hashCheck = Crypto.hashFunc(checkToHashString, new String(salt));
        if (password.equals(hashCheck)) {
            subKey = checkSecret;
            Log.i("Validation", "Successful");
            return true;
        } else {
            Log.i("Validation", "Failed");
            return false;
        }

    }

    public void logInPassword(View view) {

        EditText logInEditText = (EditText)findViewById(R.id.logInEditText);
        if (logInEditText.getText().toString().equals("")) {
            makeToast(R.string.cannot_empty);
        } else {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
            try {
                String password = sharedPreferences.getString("password", null);
                Log.i("Password", password);
                byte[] salt = Base64.getDecoder().decode(sharedPreferences.getString("salt", null));
                Log.i("Salt", Arrays.toString(salt));
                if (validatePassword(logInEditText.getText().toString(), password, salt)) {
                    if (mode == -1) {
                        Intent intentChange = new Intent(getApplicationContext(), CreatePasswordActivity.class);
                        intentChange.putExtra("change", mode);
                        startActivity(intentChange);
                    } else {
                        Intent intentMain = new Intent(getApplicationContext(), MainActivity.class);
                        intentMain.putExtra("secretKey", subKey);
                        startActivity(intentMain);
                    }
                    LogInActivity.this.finish();
                } else {
                    makeToast(R.string.invalid_pass);
                }
            } catch (Exception e) {
                Log.i("ERROR","LOGIN");
                e.printStackTrace();
            }
        }

    }

    public void logInBiometric(View view) {

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LogInActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                makeToast(errString.toString());
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                makeToast("Authentication successful!");

                String data = null;

                // Decrypt Master Key
                Log.i("Authentication successful", "next run");
                data = sharedPreferences.getString("encryptedMasterKey", null);

                try {
                    subKey = Objects.requireNonNull(Objects.requireNonNull(result.getCryptoObject()).getCipher()).doFinal(Base64.getDecoder().decode(data));
                    Log.i("Master Key", "decrypted");
                    //Log.i("Plaintext Master Key", masterKey.toString());
                    //Log.i("Encrypted Master Key", data);
                    Log.i("Master Key Size", Integer.toString(subKey.length * 8) + " bits");
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }

                if (mode == -1) {
                    Intent intentChange = new Intent(getApplicationContext(), CreatePasswordActivity.class);
                    intentChange.putExtra("change", mode);
                    startActivity(intentChange);
                } else {
                    Intent intentMain = new Intent(getApplicationContext(), MainActivity.class);
                    intentMain.putExtra("secretKey", subKey);
                    startActivity(intentMain);
                }
                LogInActivity.this.finish();

            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                makeToast("Authentication failed");
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Use your finger to unlock the app")
                .setNegativeButtonText("Cancel")
                .build();

        // Log in prompt

        Cipher cipher = null;
        SecretKey secretKey;
        try {
            cipher = Crypto.getCipher();
            secretKey = Crypto.getMasterKey();
            String ivString = sharedPreferences.getString("ivMasterKey", null);
            byte[] iv = Base64.getDecoder().decode(ivString);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        } catch (InvalidKeyException | NoSuchPaddingException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | IOException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        assert cipher != null;
        biometricPrompt.authenticate(promptInfo,
                new BiometricPrompt.CryptoObject(cipher));

    }

    public void makeToast(Integer toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
    public void makeToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

}