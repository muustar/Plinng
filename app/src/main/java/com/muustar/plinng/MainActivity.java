package com.muustar.plinng;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FECO";
    private Toolbar mToolbar;

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout mTabLayout;

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mUserDatabase, mNotifyDatabase;
    private MenuItem mClearAllMenu, mAdminMenu;
    private boolean isVisible = false;
    private Boolean mCurrentUserIsAdmin = false;

    private Methods methods;
    private ChildEventListener requestTorloEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Constant.mAppTheme);
        setContentView(R.layout.activity_main);

        methods = new Methods();

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mCurrentUser != null) {
            if (mCurrentUser.isEmailVerified()) {

                // ellenőrizzük, hogy a Users-ben már létezik-e a profil
                //mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
                /*
                FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {
                            Toast.makeText(MainActivity.this, "Létezik", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "nem létezik", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });*/


                // ellenőrizzük, hogy az adott felhazsnáló ADMIN-e
                mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child
                        (mAuth.getCurrentUser().getUid());
                mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("admin")) {
                            if ((Boolean) dataSnapshot.child("admin").getValue()) {
                                mCurrentUserIsAdmin = true;
                                Constant.mCurrentUserIsAdmin = mCurrentUserIsAdmin;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

                // ha betöltjük az üzeneteket, akkor a hozzá tartozó értesítéseket töröljük az
                // adatbázisból
                requestTorloEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String key = dataSnapshot.getKey();

                        NotificationType n = dataSnapshot.getValue(NotificationType.class);

                        if (n.getType().equals("update")) {
                            mNotifyDatabase.child(key).removeValue();
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };


                // az aktuális verzió mentése az adatbázisba
                mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child
                        (mAuth.getCurrentUser().getUid());
                mUserDatabase.child("version").setValue(Constant.VERSION);

                // ellenőrizzük, hogy van-e frissebb verzió
                DatabaseReference mVerDBRef = FirebaseDatabase.getInstance().getReference().child
                        ("Ver");
                Query verQuery = mVerDBRef.limitToLast(1);
                verQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        VersionType ver = dataSnapshot.getValue(VersionType.class);
                        Constant.AVAIABLE_VERSION = ver.getVersion();
                        Constant.AVAIABLE_VERSION_DATE = ver.getRelease_date();
                        Constant.updateTXT = ver.getText();
                        if (Constant.VERSION == Constant.AVAIABLE_VERSION) {
                            mNotifyDatabase.addChildEventListener(requestTorloEventListener);
                        } else {
                            mNotifyDatabase.removeEventListener(requestTorloEventListener);
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                FirebaseAuth.getInstance().signOut();
            }
        }
        mToolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);

        // Tabs
        mViewPager = findViewById(R.id.main_tabPager);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mTabLayout = findViewById(R.id.main_tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) {
                    isVisible = true;
                } else {
                    isVisible = false;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // chehck the user logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child
                    (mAuth.getCurrentUser().getUid());
            mUserDatabase.child("online").setValue("true");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // chehck the user logged in
        final FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            sendToStart();
        } else {
            Query queryUsername = FirebaseDatabase.getInstance().getReference().child("Users")
                    .child(currentUser.getUid());
            queryUsername.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        getSupportActionBar().setTitle(dataSnapshot.child("name").getValue().toString
                                () + " \n(" + currentUser.getEmail() + ")");
                    } catch (Exception e) {
                        Log.d(TAG, "onDataChange: exception   --- " + e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            mNotifyDatabase = FirebaseDatabase.getInstance().getReference().child
                    ("Notifications").child(currentUser.getUid());
            mNotifyDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    mSectionsPagerAdapter.updateTitleData(dataSnapshot.getChildrenCount());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child
                    (mAuth.getCurrentUser().getUid());
            mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (Build.VERSION.SDK_INT > 11) {

            mAdminMenu.setVisible(mCurrentUserIsAdmin);
            mClearAllMenu.setVisible(isVisible);
            invalidateOptionsMenu();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.request_menu, menu);
        mClearAllMenu = menu.findItem(R.id.request_clear);
        mClearAllMenu.setVisible(false);

        mAdminMenu = menu.findItem(R.id.main_admin);
        mAdminMenu.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.main_logout_btn) {
            logOut();

            return true;
        }

        if (item.getItemId() == R.id.main_account_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        if (item.getItemId() == R.id.main_all_users) {
            Intent usersIntent = new Intent(MainActivity.this, UsersActivity.class);
            startActivity(usersIntent);
            return true;
        }

        if (item.getItemId() == R.id.request_clear) {
            mNotifyDatabase.removeValue();
            return true;
        }

        if (item.getItemId() == R.id.main_color_picker) {
            colorPicker();
            return true;
        }
        if (item.getItemId() == R.id.main_admin) {
            Intent adminIntent = new Intent(MainActivity.this, AdminActivity.class);
            startActivity(adminIntent);
            return true;
        }

        if (item.getItemId() == R.id.main_appinfo) {
            Intent appinfoIntent = new Intent(MainActivity.this, AppInfoActivity.class);
            startActivity(appinfoIntent);
            return true;
        }
        return false;
    }

    private void logOut() {
        // töröljük a sharedpreferences beállításokat
        SharedPreferences sharedPref = getSharedPreferences("Plinng", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        // kilogoláskor a devicetokent töröljük
        Map tokenMap = new HashMap();
        tokenMap.put("device_token", null);
        tokenMap.put("online", ServerValue.TIMESTAMP);
        mUserDatabase.updateChildren(tokenMap);
        FirebaseAuth.getInstance().signOut();

        //vissza dobjuk a splash screenre
        Intent startIntent = new Intent(MainActivity.this, SplashSreenActivity.class);
        startActivity(startIntent);
        finish();

        // if (mAuth.getCurrentUser() == null) {
        //   sendToStart();
        //}
    }

    private void colorPicker() {
        ColorPicker colorPicker = new ColorPicker(this);
        colorPicker.dismissDialog();
        colorPicker.setRoundColorButton(true);
        colorPicker.show();
        colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
            @Override
            public void onChooseColor(int position, int color) {
                saveColor(position, color);
                Intent restartIntent = new Intent(MainActivity.this, SplashSreenActivity.class);
                startActivity(restartIntent);
                finish();
            }

            @Override
            public void onCancel() {
                // put code
            }
        });
    }

    private void saveColor(int position, int color) {
        SharedPreferences sharedPref = getSharedPreferences("Plinng", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Constant.mColorValue = color;
        methods.setColorTheme();
        editor.putInt("position", position);
        editor.putInt("color", color);
        editor.putInt("theme", Constant.mAppTheme);
        editor.commit();
    }
}
