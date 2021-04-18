package com.termux.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.termux.terminal.EmulatorDebug.LOG_TAG;

public class SequentialJob {

    private static final String LOG_TAG = "termux-task";

    final Process mProcess;

    public SequentialJob(String cwd, String fileToExecute, final String[] args, final TermuxService service){
        this(cwd, fileToExecute, args, service, null);
    }

    public SequentialJob(String cwd, String fileToExecute, final String[] args,
                         final TermuxService service, PendingIntent pendingIntent){
        String[] env = BackgroundJob.buildEnvironment(false, cwd);
        if (cwd == null) cwd = TermuxService.HOME_PATH;

        final String[] progArray = BackgroundJob.setupProcessArgs(fileToExecute, args);
        final String processDescription = Arrays.toString(progArray);

        Process process;
        try {
            process = Runtime.getRuntime().exec(progArray, env, new File(cwd));
        } catch (IOException e) {
            mProcess = null;
            // TODO: Visible error message?
            Log.e(LOG_TAG, "Failed running background job: " + processDescription, e);
            return;
        }

        mProcess = process;
        final int pid = BackgroundJob.getPid(mProcess);
        final Bundle result = new Bundle();
        final StringBuilder outResult = new StringBuilder();
        final StringBuilder errResult = new StringBuilder();

        Log.i(LOG_TAG, "[" + pid + "] starting: " + processDescription);
        InputStream stdout = mProcess.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));

        String line;
        try {
            // FIXME: Long lines.
            while ((line = reader.readLine()) != null) {
                Log.i(LOG_TAG, "[" + pid + "] stdout: " + line);
                outResult.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error reading output", e);
        }

        try {
            int exitCode = mProcess.waitFor();
            if (exitCode == 0) {
                Log.i(LOG_TAG, "[" + pid + "] exited normally");
            } else {
                Log.w(LOG_TAG, "[" + pid + "] exited with code: " + exitCode);
            }

            result.putString("stdout", outResult.toString());
            result.putInt("exitCode", exitCode);


            result.putString("stderr", errResult.toString());

            Intent data = new Intent();
            data.putExtra("result", result);

            if(pendingIntent != null) {
                try {
                    pendingIntent.send(service.getApplicationContext(), Activity.RESULT_OK, data);
                } catch (PendingIntent.CanceledException e) {
                    // The caller doesn't want the result? That's fine, just ignore
                }
            }
        } catch (InterruptedException e) {
            // Ignore
        }
    }

}
