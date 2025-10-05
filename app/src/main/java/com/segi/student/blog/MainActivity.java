package com.segi.student.blog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
// Make sure to import View if it's not already there
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.segi.student.blog.databinding.ActivityMainBinding;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find the NavController
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Find the BottomNavigationView from the included app_bar_main.xml layout
        BottomNavigationView navView = binding.appBarMain.bottomNavigation;

        // Set up the BottomNavigationView with the NavController
        NavigationUI.setupWithNavController(navView, navController);

        // --- START: ADD THIS BLOCK FOR THE FAB ---
        // Find the FAB from the included app_bar_main.xml layout
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an Intent to launch WriteBlogActivity
                Intent intent = new Intent(MainActivity.this, WriteBlogActivity.class);
                startActivity(intent);
            }
        });
        // --- END: ADD THIS BLOCK FOR THE FAB ---
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No user is signed in, redirect to AuthActivity
            sendUserToAuthActivity();
        }
    }

    private void sendUserToAuthActivity() {
        Intent authIntent = new Intent(MainActivity.this, AuthActivity.class);
        // Add flags to clear the back stack and prevent the user from navigating back to MainActivity
        authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(authIntent);
        finish(); // Finish MainActivity so the user can't return to it
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // You can remove onSupportNavigateUp() if you are no longer using a top Toolbar with an Up button.
    // The AppBarConfiguration is also not needed anymore for the bottom navigation setup.
}
