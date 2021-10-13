package com.termux.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.termux.BuildConfig;
import com.termux.CodeFragment;
import com.termux.PackagesContract;
import com.termux.PackagesDbHelper;
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

import java.lang.reflect.Method;

import io.github.rosemoe.editor.widget.CodeEditor;

import static com.termux.app.TermuxActivity.TERMUX_FAILSAFE_SESSION_ACTION;

public class MainActivity extends Activity {

    private ConstraintLayout mMainMenu;
    private FrameLayout mFrameLayout;
    public PackagesDbHelper packagesDbHelper;

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
        packagesDbHelper = new PackagesDbHelper(this);

        if (BuildConfig.DEBUG) {
            try {
                Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
                Method getAddressLog = debugDB.getMethod("getAddressLog");
                Object value = getAddressLog.invoke(null);
                Log.e("DEBUG-ADDR", (String) value);
                // Toast.makeText(this, (String) value, Toast.LENGTH_LONG).show();
            } catch (Exception ignore) {

            }
        }

        // Start db
        // Uwaga na "ciapki"
        SQLiteDatabase db = packagesDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PackagesContract.PackageEntry.COLUMN_NAME_NAME, "shiny");
        values.put(PackagesContract.PackageEntry.COLUMN_NAME_VERSION, "1.6.0");
        values.put(PackagesContract.PackageEntry.COLUMN_NAME_ACTION, "runExample('01_hello', launch.browser = TRUE)");
        db.insert(PackagesContract.PackageEntry.TABLE_NAME, null, values);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(PackagesContract.PackageEntry.TABLE_NAME, null, values);

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
