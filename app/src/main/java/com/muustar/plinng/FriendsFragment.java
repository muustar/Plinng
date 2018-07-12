package com.muustar.plinng;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {
    private RecyclerView mFriendsView;
    private FirebaseRecyclerAdapter<Friend, FriendsViewHolder> adapter;
    private Context ctx;
    private String mCurrentUser;
    private DatabaseReference usersRef;
    private DatabaseReference friendsRef;

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ctx = container.getContext();
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_friends, container, false);
        mFriendsView = fragmentView.findViewById(R.id.friends_recycler);
        mFriendsView.setHasFixedSize(true);
        LinearLayoutManager mLayout = new LinearLayoutManager(ctx);

        //mLayout.setReverseLayout(true);
        mFriendsView.setLayoutManager(mLayout);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        usersRef.keepSynced(true);

        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child
                (mCurrentUser);
        friendsRef.keepSynced(true);

        Query query = friendsRef.orderByChild("date");

        FirebaseRecyclerOptions<Friend> options =
                new FirebaseRecyclerOptions.Builder<Friend>()
                        .setQuery(query, Friend.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Friend, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int
                    position, @NonNull final Friend model) {

                holder.setConnectDate(model.getDate());

                //adatfeltöltés a Users táblából
                final String list_user_id = getRef(position).getKey();
                usersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User u;
                        if (!dataSnapshot.exists()) {
                            //private String name, status, image, image_thumb, email, uid;
                            u = new User("Törölt profile", "...", "default", "default", "törölt",
                                    "null", false);
                        } else {
                            u = dataSnapshot.getValue(User.class);
                        }

                        final String userName = u.getName();
                        final String chatUserImg = u.getImage_thumb();
                        holder.setmSingleDisplayname(userName);
                        try {
                            holder.setmSingleImage(ctx, chatUserImg);
                        } catch (Exception e) {
                            Log.d("ERROR", e.getMessage());
                        }

                        //online ststus ellenőrzés

                        if (dataSnapshot.child("online").getValue() != null) {
                            String online = dataSnapshot.child("online").getValue().toString();
                            holder.setOnlineDot(online);
                        }

                        //kattintás feature
                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                // felugró menüből választhatunk, hogy a csetbe megyünk vagy a
                                // profilra
                                CharSequence options[] = new CharSequence[]{getString(R.string
                                        .open_profile), getString(R.string.send_message)};

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                //builder.setTitle(R.string.select_options);
                                builder.setTitle(userName);
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        if (which == 0) {
                                            Intent profileIntent = new Intent(ctx,
                                                    ProfileActivity.class);
                                            profileIntent.putExtra("uid", list_user_id);
                                            startActivity(profileIntent);
                                        }

                                        if (which == 1) {
                                            Intent chatIntent = new Intent(ctx, ChatActivity.class);
                                            chatIntent.putExtra("uid", list_user_id);
                                            chatIntent.putExtra("name", userName);
                                            chatIntent.putExtra("img", chatUserImg);
                                            startActivity(chatIntent);
                                        }
                                    }
                                });
                                builder.show();

                              /*
                                // menü helyett egyből bele ugrik a chatbe
                                Intent chatIntent = new Intent(ctx, ChatActivity.class);
                                chatIntent.putExtra("uid", list_user_id);
                                chatIntent.putExtra("name", userName);
                                chatIntent.putExtra("img", chatUserImg);
                                startActivity(chatIntent);*/
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public FriendsFragment.FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup
                                                                                parent, int
                                                                                viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.friends_single_layout, parent, false);
                return new FriendsFragment.FriendsViewHolder(view);
            }
        };

        // a megfelelő pozícióra ugrik
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                //mFriendsView.smoothScrollToPosition(adapter.getItemCount());
                mFriendsView.smoothScrollToPosition(0);
            }
        });
        mFriendsView.setAdapter(adapter);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.startListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.stopListening();
    }

    public class FriendsViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView mSingleImage;
        private TextView mSingleDisplayname, mConnectDate, mEmail;
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
                Animation animation = AnimationUtils.loadAnimation(ctx, R.anim.anim_offline);
                mOnlineDot.startAnimation(animation);
            }
        }

        public void setmSingleImage(Context ctx, String imgurl) {
            mSingleImage = mView.findViewById(R.id.users_single_image);
            RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy
                    .ALL); // ezzel lehet a képeket a lemezen synkronban tartani
            GlideApp
                    .with(ctx)
                    .load(imgurl)
                    .placeholder(R.mipmap.ic_placeholder_face)
                    .error(R.mipmap.ic_placeholder_face)
                    .apply(options)
                    .into(mSingleImage);
        }

        public void setmSingleDisplayname(String name) {
            mSingleDisplayname = mView.findViewById(R.id.users_single_displayname);
            mSingleDisplayname.setText(name);
        }

        public void setConnectDate(String status) {
            mConnectDate = mView.findViewById(R.id.users_single_status);
            mConnectDate.setText(status);
        }

        public FriendsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }
    }
}
