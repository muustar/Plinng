package com.muustar.plinng;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "FECO";
    private String DEFAULT_IMAGE_ON_STORAGE = "https://firebasestorage.googleapis.com/v0/b/plinng-b623f"
            + ".appspot.com/o/default.png?alt=media&token=5f27d934-c781-46ee-9fc7-4b2a09a54a1a";
    private TextInputLayout mEmail, mPassword;
    private Button mLogin;
    private ProgressBar mProgressBar;
    private Toolbar mToolbar;
    private FirebaseAuth mAuth;
    private TextView mNewPasswd;
    private Random r = new Random();

    private DatabaseReference mUsersDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Constant.mAppTheme);
        setContentView(R.layout.activity_login);


        //firebase section
        mAuth = FirebaseAuth.getInstance();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        // displeay elements imitializing
        mProgressBar = (ProgressBar) findViewById(R.id.login_progressbar);
        mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.login);
        // setDisplayHomeAsUpEnabled(true) engedélyezi, hogy az appbaron ott legyen bal oldalt a kicsi nyíl
        // a manifestben meg kell határozni, ki a parent, oda mutat a visszanyíl
        //  <activity android:name=".RegisterActivity"
        //      android:parentActivityName=".StartActivity"></activity>
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEmail = (TextInputLayout) findViewById(R.id.login_email);
        mPassword = (TextInputLayout) findViewById(R.id.login_password);
        mLogin = (Button) findViewById(R.id.login_loginBtn);
        mNewPasswd = findViewById(R.id.login_newpassword);

        mNewPasswd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newPasswordIntent = new Intent(LoginActivity.this, ReqNewPasswordActivity.class);
                String emailAddress = mEmail.getEditText().getText().toString().trim();
                if (!TextUtils.isEmpty(emailAddress)) {
                    newPasswordIntent.putExtra("email", emailAddress);
                }
                startActivity(newPasswordIntent);

            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getEditText().getText().toString().trim();
                String password = mPassword.getEditText().getText().toString().trim();
                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {

                    loginUser(email, password);
                }
            }
        });

    }

    private void loginUser(String email, String password) {
        mProgressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mProgressBar.setVisibility(View.INVISIBLE);
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            final FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // ellenőrizzük, hogy az email megerősítés megtörtént e
                                if (user.isEmailVerified()) {


                                    // ellenőrizzük, hogy a Users-ben már létezik-e a profil
                                    //mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

                                    ValueEventListener profileellenorzesListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            if (dataSnapshot.exists()) {
                                                //Toast.makeText(LoginActivity.this, "Létezik", Toast.LENGTH_SHORT).show();
                                                // ------------- device token ---------------------
                                                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                                mUsersDatabase.child(user.getUid()).child("device_token").setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(mainIntent);
                                                        finish();
                                                    }
                                                });
                                            } else {
                                                //Toast.makeText(LoginActivity.this, "nem létezik", Toast.LENGTH_SHORT).show();

                                                //user adatainak mentése az adatbázisba
                                                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                                // a statusnak beállítunk egy véletlen funnystringet
                                                int funnyPosition = r.nextInt(Constant.mFunnyStrings.length) + 1;
                                                String funnyStatus = Constant.mFunnyStrings[funnyPosition];


                                                Map<String, Object> userMap = new HashMap<>();
                                                userMap.put("name", user.getDisplayName());
                                                userMap.put("status", funnyStatus);
                                                userMap.put("image", DEFAULT_IMAGE_ON_STORAGE);
                                                userMap.put("image_thumb", DEFAULT_IMAGE_ON_STORAGE);
                                                userMap.put("email", user.getEmail());
                                                userMap.put("uid", user.getUid());
                                                userMap.put("device_token", deviceToken);
                                                userMap.put("email_visible", false);

                                                mUsersDatabase.child(user.getUid()).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(mainIntent);
                                                            finish();
                                                        }
                                                    }
                                                });


                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    };

                                    mUsersDatabase.child(user.getUid()).addListenerForSingleValueEvent(profileellenorzesListener);

                                } else {
                                    // amennyiben az email megerősítés még nem történt meg tájékoztatjuk és kilogolunk

                                    // hide keyboard
                                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    mgr.hideSoftInputFromWindow(mPassword.getWindowToken(), 0);

                                    // tájékoztatás megjelenítése
                                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
                                    alertDialog.setMessage("Az email címed még nem lett megerősítve, kérlek ellenőrizd az emal fiókod!");
                                    alertDialog.setPositiveButton("Megértettem",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                    alertDialog.setNegativeButton("Email újraküldése", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    finish();
                                                }
                                            });
                                        }
                                    });
                                    alertDialog.show();
                                }

                            }
                        } else {
                            mProgressBar.setVisibility(View.INVISIBLE);
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }

                        // ...
                    }
                });
    }

    private void logOut() {
        // töröljük a sharedpreferences beállításokat
        SharedPreferences sharedPref = getSharedPreferences("Plinng", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();


        FirebaseAuth.getInstance().signOut();

        //vissza dobjuk a splash screenre
        Intent startIntent = new Intent(LoginActivity.this, SplashSreenActivity.class);
        startActivity(startIntent);
        finish();

        // if (mAuth.getCurrentUser() == null) {
        //   sendToStart();
        //}
    }
}
