package com.muustar.plinng;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private RecyclerView mUsersList;
    private FirebaseRecyclerAdapter<User, UsersViewHolder> adapter;
    private DatabaseReference mUsersRef;
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Constant.mAppTheme);
        setContentView(R.layout.activity_users);


        mToolbar = findViewById(R.id.users_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getString(R.string.users));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mUsersList = findViewById(R.id.users_list);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));

        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");


        //a firebase UI recycrerview kezelőjét használjuk
        //User osztály:  private String name, status, image, image_thumb;

        // https://github.com/firebase/FirebaseUI-Android/blob/master/database/README.md

        Query query = mUsersRef
                .orderByChild("name");

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {
            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ctx = parent.getContext();
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);
                return new UsersViewHolder(view);

            }

            @Override
            protected void onBindViewHolder(@NonNull final UsersViewHolder holder, int position, @NonNull final User model) {
                holder.setmSingleImage(getApplicationContext(), model.getImage_thumb());
                if (Constant.mCurrentUserIsAdmin){
                    holder.setmSingleDisplayname(model.getName()+" "+model.getVersion());
                }else{
                    holder.setmSingleDisplayname(model.getName());
                }

                holder.setmSingleStatus(model.getStatus());
                holder.setmEmail(model.getEmail_visible(), model.getEmail());


                // online dot
                mUsersRef.child(model.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("online").getValue() != null) {
                            String online = dataSnapshot.child("online").getValue().toString();
                            holder.setOnlineDot(online);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                // ha nem tárolnám a User osztályban a UID-t, akkor ezzel tudjuk lekérni.
                // String userID = getRef(position).getKey();

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("uid", model.getUid());

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            startActivity(profileIntent);
                        } else {
                            Pair[] pairs = new Pair[3];
                            pairs[0] = new Pair<View, String>(holder.mSingleImage, "imageTrans");
                            pairs[1] = new Pair<View, String>(holder.mSingleDisplayname, "nameTrans");
                            pairs[2] = new Pair<View, String>(holder.mSingleStatus, "statusTrans");

                            ActivityOptions options = ActivityOptions
                                    .makeSceneTransitionAnimation(UsersActivity.this, pairs);

                            startActivity(profileIntent, options.toBundle());
                        }
                    }
                });
            }


        };
        mUsersList.setAdapter(adapter);


    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();

        // chehck the user logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid());
            mUserDatabase.child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView mSingleImage;
        private TextView mSingleDisplayname, mSingleStatus, mEmail;
        private ImageView mOnlineDot;

        public void setOnlineDot(String b) {
            mOnlineDot = (ImageView) mView.findViewById(R.id.users_single_dot);
            if (b.equals("true")) {
                mOnlineDot.setImageResource(R.mipmap.online_dot);
                //animáció
                Animation animation = AnimationUtils.loadAnimation(ctx, R.anim.anim_online);
                mOnlineDot.startAnimation(animation);
            } else {
                mOnlineDot.setImageResource(R.mipmap.offline_dot);
                //animáció
                //Animation animation = AnimationUtils.loadAnimation(ctx, R.anim.anim_offline);
                //mOnlineDot.startAnimation(animation);
            }
        }

        public void setmEmail(Boolean isVisible, String mail) {
            if (isVisible) {
                mEmail.setText(mail);
                mEmail.setVisibility(View.VISIBLE);
            } else {
                mEmail.setText("");
                mEmail.setVisibility(View.GONE);
            }
        }

        public void setmSingleImage(Context ctx, String imgurl) {
            mSingleImage = mView.findViewById(R.id.users_single_image);
            GlideApp
                    .with(ctx)
                    .load(imgurl)
                    .placeholder(R.mipmap.ic_placeholder_face)
                    .error(R.mipmap.ic_placeholder_face)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(mSingleImage);
        }

        public void setmSingleDisplayname(String name) {
            mSingleDisplayname = mView.findViewById(R.id.users_single_displayname);
            mSingleDisplayname.setText(name);
        }

        public void setmSingleStatus(String status) {
            mSingleStatus = mView.findViewById(R.id.users_single_status);
            mSingleStatus.setText(status);
        }

        public UsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mEmail = mView.findViewById(R.id.users_single_email);

        }


    }
}
