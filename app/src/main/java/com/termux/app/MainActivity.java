package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxInstaller;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.WindowManager;
import android.widget.Toolbar;

import static com.termux.app.TermuxActivity.TERMUX_FAILSAFE_SESSION_ACTION;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        TermuxInstaller.setupIfNeeded(MainActivity.this, null);

        // Add onClickListeners
        MainButtonView mRConsoleButton = findViewById(R.id.r_console_btn);

        // TODO: not working
        mRConsoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, TermuxActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });




    }
}
