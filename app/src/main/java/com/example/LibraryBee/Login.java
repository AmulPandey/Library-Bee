package com.example.LibraryBee;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private Button loginAdminButton;
    private FirebaseAuth auth;

    private ProgressDialog loginProgressDialog;
    private ProgressDialog loginAdminProgressDialog;

    private TextView forgotPasswordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initializeViews();
        auth = FirebaseAuth.getInstance();
        checkCurrentUser();
        setListeners();

        loginProgressDialog = new ProgressDialog(this);
        loginProgressDialog.setMessage("Logging in...");
        loginProgressDialog.setCancelable(false);

        loginAdminProgressDialog = new ProgressDialog(this);
        loginAdminProgressDialog.setMessage("Logging in as admin...");
        loginAdminProgressDialog.setCancelable(false);

    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        signUpTextView = findViewById(R.id.signUpTextView);
        loginAdminButton = findViewById(R.id.loginAdminButton);

    }

    private void checkCurrentUser() {
        if (auth.getCurrentUser() != null) {
            String userType = getUserType();
            navigateToDashboard(userType);
            finish();
        }
    }

    private void setListeners() {
        loginButton.setOnClickListener(view -> {
            loginUser();
        });

        forgotPasswordTextView.setOnClickListener(view -> {
            // Handle the click on "Forgot Password?" TextView
            forgotPassword();
        });

        signUpTextView.setOnClickListener(view -> navigateToSignup());

        loginAdminButton.setOnClickListener(view -> {
            loginAsAdmin();
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please enter email and password");
            return;
        }
        // Show progress bar before signing in

        loginButton.setVisibility(View.GONE);
        loginProgressDialog.show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        checkAdminStatus();
                    } else {
                        showToast("Authentication failed. " + task.getException().getMessage());
                        loginButton.setVisibility(View.VISIBLE);
                    }

                    loginProgressDialog.dismiss();

                });

    }

    private void checkAdminStatus() {
        String adminEmail = "admin@gmail.com";
        String adminPassword = "654321";
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.equals(adminEmail) && password.equals(adminPassword)) {
            saveUserType("Admin");
            navigateToDashboard("Admin");
            loginButton.setVisibility(View.VISIBLE);
            finish();
        } else {
            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("admins")
                    .child(auth.getCurrentUser().getUid());
            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String userType = snapshot.exists() ? "Admin" : "User";
                    saveUserType(userType);
                    navigateToDashboard(userType);
                    loginButton.setVisibility(View.VISIBLE);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showToast("Database error: " + error.getMessage());
                }
            });
        }
    }

    private void loginAsAdmin() {

        loginAdminButton.setVisibility(View.GONE);
        loginAdminProgressDialog.show();

        String adminEmail = "admin@gmail.com";
        String adminPassword = "654321";
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please enter email and password");
            loginAdminProgressDialog.dismiss();
            loginAdminButton.setVisibility(View.VISIBLE);
            return;
        }

        if (!email.equals(adminEmail) || !password.equals(adminPassword)) {
            showToast("Invalid admin credentials");
            loginAdminProgressDialog.dismiss();
            loginAdminButton.setVisibility(View.VISIBLE);
            return;
        }

        loginAdminProgressDialog.dismiss();
        saveUserType("Admin");
        navigateToDashboard("Admin");
        loginAdminButton.setVisibility(View.VISIBLE);
        finish();

    }

    private void navigateToSignup() {
        startActivity(new Intent(Login.this, Signup.class));
    }

    private void navigateToDashboard(String userType) {
        Intent intent = userType.equals("Admin") ?
                new Intent(Login.this, AdminDashboardActivity.class) :
                new Intent(Login.this, MainActivity.class);
        startActivity(intent);
    }

    private void saveUserType(String userType) {
        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
        editor.putString("UserType", userType).apply();
    }

    private String getUserType() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getString("UserType", "");
    }

    private void showToast(String message) {
        Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
    }

    public void forgotPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.forgetpass, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        Button submitBtn = dialogView.findViewById(R.id.submitBtn);

        submitBtn.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                showToast("Password reset email sent");
                            } else {
                                showToast("Failed to send reset email");
                            }
                        });
                dialog.dismiss();
            } else {
                showToast("Please enter your email");
            }
        });

        dialog.show();
    }
}
