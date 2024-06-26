package com.example.LibraryBee;

import static android.app.Activity.RESULT_OK;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class home extends Fragment {

    private Toolbar toolbar;

    private DrawerLayout drawerLayout;


    private ActivityResultLauncher<String> galleryLauncher;

    private DatabaseReference subscriptionRef;
    private FirebaseAuth auth;
    private Button btn1; // Change the type to Button
    private Button btn3;
    private Button btn4;
    private TextView usernameTextView;

    private Uri mImageUri;

    private CircleImageView profileImageView;

    private String imageUrl;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();
        fetchProfileImage();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        drawerLayout = view.findViewById(R.id.drawer_layout);


        NavigationView navigationView = view.findViewById(R.id.navigation_view);
        navigationView.setBackgroundColor(getResources().getColor(android.R.color.black));

        View headerView = navigationView.getHeaderView(0);
        profileImageView = headerView.findViewById(R.id.imageViewProfile);


        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                mImageUri = uri;
                uploadImageToFirebaseStorage();
            }
        });



        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Handle navigation drawer item clicks
                int id = item.getItemId();
                if (id == R.id.nav_profile_change) {
                    // Handle profile settings action
                    //Toast.makeText(getActivity(), "Profile Settings clicked", Toast.LENGTH_SHORT).show();
                    choosePhotoFromGallery();
                } else if (id == R.id.nav_contact_Library) {
                    // Handle contact library action
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Contact Library")
                            .setMessage("Phone: 7007084705")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing, just close the dialog
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else if (id == R.id.nav_contact_developer) {
                    // Handle contact library action
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Contact Developer")
                            .setMessage("Phone: 9936474273\nEmail: amulpandey007@gmail.com")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing, just close the dialog
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else if (id == R.id.nav_logout) {
                    // Handle logout action
                    showLogoutDialog(); // Call the method to show the logout dialog
                }

                // Close the drawer after handling the click
                drawerLayout.closeDrawers();
                return true;
            }
        });


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity(),
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
               // navigationView.setBackgroundColor(getResources().getColor(android.R.color.black));
                getActivity().invalidateOptionsMenu(); // If you have action items to hide/show
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActivity().invalidateOptionsMenu(); // If you have action items to hide/show
            }
        };
        drawerLayout.addDrawerListener(toggle);

// Setting custom toggle icon
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu); // Set your custom drawable here
        toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });


        // Correctly find the View
        btn1 = view.findViewById(R.id.btn1); // Initialize btn1 once
        usernameTextView = view.findViewById(R.id.usernameTextView);
        btn3 = view.findViewById(R.id.btn3);
        btn4 = view.findViewById(R.id.btn4);
        // Set click listener and start activity


        // Fetch and set the subscription status
        fetchUserData();

        Button btn2 = view.findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SeatSelectionActivity.class);
                startActivity(intent);
            }
        });

        registerForContextMenu(toolbar);

        return view;
    }


    private void fetchUserData() {
        if (isAdded()) {
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                String userId = currentUser.getUid();

                // Reference to the user's data in Firebase
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

                // Read the user's data from Firebase
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (isAdded() && dataSnapshot.exists()) {
                            // Retrieve the user's data
                            boolean isSubscribed = dataSnapshot.child("isSubscribed").getValue(Boolean.class);
                            String username = dataSnapshot.child("username").getValue(String.class);
                            String seatNumber = dataSnapshot.child("seatNumber").getValue(String.class);
                            String reserveSlot = dataSnapshot.child("timingSlot").getValue(String.class);

                            // Set the username in the TextView
                            if (usernameTextView != null) {
                                usernameTextView.setText(username);
                            } else {
                                // Handle the case where usernameTextView is null (e.g., log an error)
                                usernameTextView.setText("user");
                            }

                            // Update the UI based on the subscription status
                            updateUI(isSubscribed,seatNumber,reserveSlot);
                        } else {
                            // Handle the case where user data doesn't exist
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle errors here
                    }
                });
            } else {
                // User is not authenticated, handle accordingly (e.g., redirect to login screen)
            }
        }
    }



    private void updateUI(boolean isSubscribed, String seatNumber,String reserveSlot) {
        if (isSubscribed) {
            // Set ACTIVE with green color
            btn1.setText("ACTIVE");
            btn1.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btn3.setText(seatNumber);
            btn3.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btn4.setText(reserveSlot);
            btn4.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            // Set INACTIVE with red color
            btn1.setText("INACTIVE");
            btn1.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btn3.setText("Seat");
            btn3.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btn4.setText("Slot");
            btn4.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }


    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Sign out
                    auth.signOut();

                    // Redirect to the login page
                    Intent loginIntent = new Intent(getActivity(), Login.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(loginIntent);
                    getActivity().finish(); // Close the current activity
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User clicked Cancel, do nothing
                });

        builder.create().show();
    }

    public void choosePhotoFromGallery() {
        galleryLauncher.launch("image/*");
    }




    private void uploadImageToFirebaseStorage() {
        if (mImageUri != null) {
            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Uploading");
            progressDialog.setMessage("Please wait...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            final StorageReference imageReference = FirebaseStorage.getInstance().getReference("userprofiles").child(auth.getCurrentUser().getUid());
            imageReference.putFile(mImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        progressDialog.dismiss();
                        // Image uploaded successfully
                        // Now you can get the download URL and do something with it if needed
                        imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Handle the download URL (e.g., save it to a database)
                            imageUrl = uri.toString();
                            // Update profileImageView with the uploaded image
                            fetchProfileImage();
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        // Handle the error
                        Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void fetchProfileImage() {
        if (!isAdded()) {
            // Fragment is not attached, return
            return;
        }

        // Get the context only if the Fragment is attached
        Context context = getContext();
        if (context == null) {
            // Context is null, return
            return;
        }

        // Get the reference to the Firebase Storage path for the user's profile image
        StorageReference imageReference = FirebaseStorage.getInstance().getReference()
                .child("userprofiles")
                .child(auth.getCurrentUser().getUid());

        // Fetch the download URL of the image
        imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
            // Load the image into the profileImageView using Glide or Picasso
            Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.profile) // Placeholder image while loading
                    .error(R.drawable.profile) // Image to display in case of error
                    .into(profileImageView);
        }).addOnFailureListener(exception -> {
            // Handle any errors
            Log.e(TAG, "Failed to fetch profile image: " + exception.getMessage());
        });
    }

}


