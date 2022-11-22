package com.robert.mildlysecurenotepadfp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
        Boolean firstRun = sharedPreferences.getBoolean("firstRun", true);

        Intent intent;
        if (firstRun) {
            // Go to create password activity
            intent = new Intent(getApplicationContext(), CreatePasswordActivity.class);
        } else {

            // Go to login activity
            intent = new Intent(getApplicationContext(), LogInActivity.class);
        }
        intent.putExtra("change", 0);
        startActivity(intent);
        StartActivity.this.finish();

    }

}