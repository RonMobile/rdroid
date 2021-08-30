package com.termux.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.termux.CodeFragment;
import com.termux.R;
import com.termux.RPackageInstallerFragment;
import com.termux.RunScriptFragment;
import com.termux.RunShinyAppFragment;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxInstaller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;


import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import io.github.rosemoe.editor.widget.CodeEditor;

import static com.termux.app.TermuxActivity.TERMUX_FAILSAFE_SESSION_ACTION;

public class MainActivity extends Activity {

    private ConstraintLayout mMainMenu;
    private FrameLayout mFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        TermuxInstaller.setupIfNeeded(MainActivity.this, null);

        // Add onClickListeners
        mMainMenu =  findViewById(R.id.main_menu);
        mFrameLayout = findViewById(R.id.frame_layout);

        MainButtonView mRConsoleButton = findViewById(R.id.btn_r_console);
        MainButtonView mRPackagesInstaller = findViewById(R.id.btn_r_packages_installer);
        MainButtonView mRunScript = findViewById(R.id.btn_run_script);
        MainButtonView mRunShinyApp = findViewById(R.id.btn_run_shiny_app);
        MainButtonView mCodeEditor = findViewById(R.id.btn_code_editor);

        // TODO: not working
        mRConsoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, TermuxActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        mRPackagesInstaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFragment(new RPackageInstallerFragment());
            }
        });

        mRunScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,"Select file or dir"), 1);
                setResult(Activity.RESULT_OK);
                //loadFragment(new RunScriptFragment());
            }
        });

        mRunShinyApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFragment(new RunShinyAppFragment());
            }
        });

        mCodeEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFragment(new CodeFragment());
            }
        });

    }

    @Override
    public void onBackPressed()
    {
        mMainMenu.setVisibility(View.VISIBLE);
        mFrameLayout.setVisibility(View.INVISIBLE);

/*        FragmentManager fm = getFragmentManager();
        // create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        // replace the FrameLayout with new Fragment
        fragmentTransaction*/
        //super.onBackPressed();
    }


    private void loadFragment(Fragment fragment) {

        // create a FragmentManager
        FragmentManager fm = getFragmentManager();
        // create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        // replace the FrameLayout with new Fragment
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit(); // save the changes
        mFrameLayout.setVisibility(View.VISIBLE);
        mMainMenu.setVisibility(View.INVISIBLE);
    }

}
