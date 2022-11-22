package com.robert.mildlysecurenotepadfp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {

    static ArrayList<String> notes = new ArrayList<>();
    private ArrayList<String> encryptedNotes = new ArrayList<>();
    private byte[] secretKey;
    static CustomAdapter arrayAdapter;
    ListView listView;
    Integer mode = 0;
    Integer noteId = -1;

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onResume() {
        super.onResume();

        if (noteId != -1) {
            // encrypt note that has just been created using noteId variable
            // save all data into SharedPreferences using saveData()
            try {
                saveData(noteId);
            } catch (Exception e) {
                Log.i("ERROR", "onResume() Method");
                e.printStackTrace();
            }
        }
        noteId = -1;

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);

        Intent intent = getIntent();
        secretKey = intent.getByteArrayExtra("secretKey");
        mode = intent.getIntExtra("change", 0);
        if (mode == -1) {
            encryptedNotes = new ArrayList();
            for (String note : notes) {
                try {
                    encryptOneByOne(note);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            saveEverything();
            mode = 0;
        }

        listView = (ListView)findViewById(R.id.listView);

        try {
            if (encryptedNotes.size() < 1) {
                HashSet<String> set = (HashSet<String>) sharedPreferences.getStringSet("notes", null);
                decryptData(set);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("ERROR", "onCreate() Method, decryption");
        }

        arrayAdapter = new CustomAdapter(this, R.layout.adapter_view_layout, notes);
        listView.setAdapter(arrayAdapter);
        // Display note
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(), NotepadActivity.class);
                intent.putExtra("noteId", position);
                noteId = position;
                startActivity(intent);

            }
        });

        // Delete note
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete the note?")
                        .setMessage("You won't be able to revert this action")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                notes.remove(position);
                                encryptedNotes.remove(position);
                                arrayAdapter.notifyDataSetChanged();
                                try {
                                    saveData(-1);
                                } catch (Exception e) {
                                    Log.i("ERROR", "Menu Delete Note Method");
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void encryptOneByOne(String string) throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

        encryptedNotes.add(Crypto.encrypt(string, secretKey));

    }

    private void saveEverything() {

        HashSet<String> set = new HashSet(encryptedNotes);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
        sharedPreferences.edit().putStringSet("notes", set).apply();
        makeToast("Everything encrypted and saved with new key");

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveData(Integer notePosition) throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        // create HashSet with ArrayList<String> encryptedNotes
        // then save into SharedPreferences

        if (notePosition != -1) {
            if (notePosition >= encryptedNotes.size()) {
                encryptedNotes.add(Crypto.encrypt(notes.get(notePosition), secretKey));
            } else {
                encryptedNotes.set(notePosition, Crypto.encrypt(notes.get(notePosition), secretKey));
            }
        }
        HashSet<String> set = new HashSet(encryptedNotes);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
        sharedPreferences.edit().putStringSet("notes", set).apply();
        makeToast("Data encrypted and saved");

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void decryptData(HashSet<String> set) throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        // decrypt into ArrayList<String> notes

        if (set == null) {
            notes = new ArrayList();
        } else {
            notes = new ArrayList();
            encryptedNotes = new ArrayList(set);
            String decr;
            for (String encNote : set) {
                decr = Crypto.decrypt(encNote, secretKey);
                notes.add(decr);
                Log.i("ENCRYPTED", encNote);
                Log.i("DECRYPTED", decr);
            }
            makeToast("Data decrypted");
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        // Add Note
            if (item.getItemId() == R.id.add_note) {
                noteId = notes.size();
                Intent intent = new Intent(getApplicationContext(), NotepadActivity.class);
                startActivity(intent);
            }
        // Delete everything
            if (item.getItemId() == R.id.delete_everything) {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete absolutely everything?")
                        .setMessage("Everything will be deleted including notes and password\nYou won't be able to revert this action")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.robert.mildlysecurenotepadfp", Context.MODE_PRIVATE);
                                sharedPreferences.edit().clear().apply();
                                noteId = -1;
                                notes.clear();
                                arrayAdapter.notifyDataSetChanged();
                                Intent intent = new Intent(getApplicationContext(), StartActivity.class);
                                startActivity(intent);
                                MainActivity.this.finish();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        // Change Password
            if (item.getItemId() == R.id.change_password) {
                Intent changeIntent = new Intent(getApplicationContext(), LogInActivity.class);
                changeIntent.putExtra("change", -1);
                startActivity(changeIntent);
                MainActivity.this.finish();
            }
        return true;

    }

    public void makeToast(Integer toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
    public void makeToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

}