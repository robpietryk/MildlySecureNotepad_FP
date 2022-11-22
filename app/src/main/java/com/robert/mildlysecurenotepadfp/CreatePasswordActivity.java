package com.robert.mildlysecurenotepadfp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

public class CreatePasswordActivity extends AppCompatActivity {

    Integer mode = 0;
    Boolean authenticationErr = false;
    Boolean firstRun;
    byte[] subKey;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    String KEY_NAME = "MildlySecureNpdFP";

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_password);

        TextView textView = (TextView)findViewById(R.id.createPasswordSmallTextView);
        textView.setText(R.string.create_pass_message);
        Intent intent = getIntent();
        try {
            mode = intent.getIntExtra("change", 0);
            if (mode == -1) {
                textView.setText(R.string.change_pass_message);
            } else {
                textView.setText(R.string.create_pass_message);
            }
        } catch (Exception e) {
            Log.i("ERROR", "CreatePasswordActivity");
            e.printStackTrace();
        }

    }

    private void makeBiometricPrompt(byte[] _subKey) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
        firstRun = sharedPreferences.getBoolean("firstRun", true);

        if (firstRun) {
            try {
                Crypto.generateMasterKey(new KeyGenParameterSpec.Builder(
                        KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .setInvalidatedByBiometricEnrollment(true)
                        .build());
                Log.i("Secret key","generated");
            } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        }

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(CreatePasswordActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                makeToast(errString.toString());
                authenticationErr = true;
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                makeToast("Authentication successful!");

                String data = null;
                String iv = null;

                Log.i("Authentication successful", "first run");

                try {
                    Cipher cipher = Objects.requireNonNull(result.getCryptoObject()).getCipher();
                    assert cipher != null;
                    iv = Base64.getEncoder().encodeToString(cipher.getIV());
                    data = Base64.getEncoder().encodeToString(cipher.doFinal(_subKey));
                    Log.i("Master Key", "encrypted");
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                sharedPreferences.edit().putString("encryptedMasterKey", data).apply();
                sharedPreferences.edit().putString("ivMasterKey", iv).apply();
                sharedPreferences.edit().putBoolean("firstRun", false).apply(); // put flag

                Log.i("Authentication", "Successful");
                makeToast(R.string.pass_saved);
                Intent intentMain = new Intent(getApplicationContext(), MainActivity.class);
                intentMain.putExtra("secretKey", subKey);
                intentMain.putExtra("change", mode);
                startActivity(intentMain);
                CreatePasswordActivity.this.finish();

            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                makeToast("Authentication failed try again");
                authenticationErr = true;
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
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (InvalidKeyException | NoSuchPaddingException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }
        assert cipher != null;
        biometricPrompt.authenticate(promptInfo,
                new BiometricPrompt.CryptoObject(cipher));
    }

    private byte[] getNumberOfBytes(byte[] byteArray, int offset, int length) throws ArrayIndexOutOfBoundsException {

        byte[] targetArray = new byte[length];
        if (length >= 0) System.arraycopy(byteArray, offset, targetArray, 0, length);
        return targetArray;

    }

    public void createPassButtonFunc(View view) throws NoSuchAlgorithmException, InvalidKeySpecException {

        EditText createPasswordEditText = (EditText)findViewById(R.id.createPasswordEditText);
        EditText confirmPasswordEditText = (EditText)findViewById(R.id.confirmPasswordEditText);
        TextView createPasswordTextView = (TextView)findViewById(R.id.createPasswordTextView);
        // Create password
        createPasswordTextView.setText(R.string.create_pass);
        String password = createPasswordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        if (password.equals("")) {
            makeToast(R.string.cannot_empty);
        } else if (password.length() < 8) {
            makeToast(R.string.pass_too_short);
        } else if (!password.equals(confirmPassword)) {
            makeToast(R.string.do_not_match);
        } else {
            if (!authenticationErr) {
                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
                sharedPreferences.edit().clear().apply();

                byte[] salt = Crypto.generateSalt(); // generate salt
                sharedPreferences.edit().putString("salt", Base64.getEncoder().encodeToString(salt)).apply(); // put salt into SharedPreferences

                byte[] generatedSecretKey = Crypto.generateKey(password, salt); // generate secret key with 128 bits used to validate user

                byte[] passwordToHash = getNumberOfBytes(generatedSecretKey, 0, 16); // take 128 bits of generated key as password to hash and place into SharedPreferences
                subKey = getNumberOfBytes(generatedSecretKey, 16, 32); // take 256 bits of generated key as secret key used in AES256CBC

                String passwordToHashString = new String(passwordToHash);
                String hashPass = Crypto.hashFunc(passwordToHashString, new String(salt)); // hash 128 bits of the generated secret key with salt

                sharedPreferences.edit().putString("password", hashPass).apply(); // put hashed password as password check in SharedPreferences
            }

            makeBiometricPrompt(subKey);

        }

    }

    public void makeToast(Integer toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
    public void makeToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

}