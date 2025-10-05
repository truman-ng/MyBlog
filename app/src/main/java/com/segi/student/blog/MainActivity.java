package com.segi.student.blog;

import android.content.Intent;import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.segi.student.blog.databinding.ActivityMainBinding;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
// --- CHANGE 1: Import NavHostFragment ---
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private NavController navController; // --- CHANGE 2: Make NavController a class variable ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // --- CHANGE 3: Find NavController using NavHostFragment (the modern, correct way) ---
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        navController = navHostFragment.getNavController();

        // Find the BottomNavigationView from the included app_bar_main.xml layout
        BottomNavigationView navView = binding.appBarMain.bottomNavigation;

        // Set up the BottomNavigationView with the NavController
        NavigationUI.setupWithNavController(navView, navController);


        // --- START: THE DEFINITIVE FIX ---
        // We will manually handle bottom navigation clicks to gain full control.
        navView.setOnItemSelectedListener(item -> {
            // Build navigation options to achieve the desired behavior.
            NavOptions.Builder optionsBuilder = new NavOptions.Builder()
                    // Restore the state of the destination tab's back stack.
                    .setRestoreState(true)
                    // Pop up to the start destination of the graph to avoid building up a large stack.
                    .setPopUpTo(navController.getGraph().getStartDestinationId(), false);

            // This is the key: If the user is re-selecting the same tab,
            // we ALSO pop the stack for that specific tab.
            if (item.getItemId() == navController.getCurrentDestination().getId()) {
                optionsBuilder.setPopUpTo(item.getItemId(), true);
            }

            // Perform the navigation with our custom options.
            navController.navigate(item.getItemId(), null, optionsBuilder.build());

            // Return true to show the item as selected.
            return true;
        });

        // Also, listen for changes in the NavController to update the selected item in the BottomNav.
        // This ensures that if we navigate programmatically or with the back button, the tab icon updates.
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Menu menu = navView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == destination.getId()) {
                    menu.getItem(i).setChecked(true);
                    break;
                }
            }
        });
        // --- END: THE DEFINITIVE FIX ---

        // This block for the FAB is correct and does not need to change.
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WriteBlogActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            sendUserToAuthActivity();
        }
    }

    private void sendUserToAuthActivity() {
        Intent authIntent = new Intent(MainActivity.this, AuthActivity.class);
        authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(authIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // --- CHANGE 5: Add onSupportNavigateUp to handle the Toolbar's back arrow ---
    /**
     * This ensures that when the user presses the back arrow in a Toolbar
     * (like in BlogDetailFragment), the NavController handles the back action
     * correctly, instead of the app just closing.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
