package com.termux.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.system.Os;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import com.termux.R;
import com.termux.terminal.ByteQueue;
import com.termux.terminal.EmulatorDebug;
import com.termux.terminal.JNI;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import static com.termux.app.TermuxService.HOME_PATH;
import static com.termux.app.TermuxService.PREFIX_PATH;

/**
 * Install the Termux bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX folder below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging folder, $STAGING_PREFIX, is {@link #deleteFolder(File)} if left over from broken installation below.
 * <p/>
 * (4) The zip file is loaded from a shared library.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 *
 * https://its-pointless.github.io/setup-pointless-repo.sh
 *
 * apt-get update
 * apt-get --assume-yes upgrade
 * apt-get --assume-yes install coreutils gnupg
 * # Make the sources.list.d directory
 * mkdir -p $PREFIX/etc/apt/sources.list.d
 * # Write the needed source file
 * if apt-cache policy | grep -q "termux.*24\|termux.org\|bintray.*24\|k51qzi5uqu5dg9vawh923wejqffxiu9bhqlze5f508msk0h7ylpac27fdgaskx" ; then
 * echo "deb https://its-pointless.github.io/files/24 termux extras" > $PREFIX/etc/apt/sources.list.d/pointless.list
 * else
 * echo "deb https://its-pointless.github.io/files/21 termux extras" > $PREFIX/etc/apt/sources.list.d/pointless.list
 * fi
 * # Add signing key from https://its-pointless.github.io/pointless.gpg
 * if [ -n $(command -v curl) ]; then
 * curl -sLo $PREFIX/etc/apt/trusted.gpg.d/pointless.gpg --create-dirs https://its-pointless.github.io/pointless.gpg
 * elif [ -n $(command -v wget) ]; then
 * wget -qP $PREFIX/etc/apt/trusted.gpg.d https://its-pointless.github.io/pointless.gpg
 * fi
 * # Update apt
 * apt update
 *
 */
final class TermuxInstaller {

    private static final String TERMUX_INSTALLER_TAG = "TERMUX-INSTALLER";
    private static ProgressDialog progress;

    public static final String[] DEFAULT_PACKAGES_1 = new String[]{
        "curl",
        "wget",
        "gnupg"
    };

    public static final String POINTLESS_REPO_CONFIG_SCRIPT =
        "https://raw.githubusercontent.com/RonMobile/r-on-android/master/setup-pointless-repo.sh";

    // Packages needed to run R
    public static final String[] DEFAULT_PACKAGES_2 = new String[]{
        "r-base", "make", "clang", "gcc-9",  "openssl",
        "libcurl", "libicu", "libxml2", "ndk-sysroot",
        "pkg-config", "cmake", "git", "libcairo",
        "libtiff", "pango", "zlib"
    };

    public static final String PKG_PATH = TermuxService.PREFIX_PATH + "/bin/pkg";
    public static final String APT_GET_PATH = TermuxService.PREFIX_PATH + "/bin/apt-get";
    public static final String CURL_PATH = TermuxService.PREFIX_PATH + "/bin/curl";
    public static final String BASH_PATH = TermuxService.PREFIX_PATH + "/bin/bash";
    public static final String CP_PATH = TermuxService.PREFIX_PATH + "/bin/cp";
    public static final String YES_PATH = TermuxService.PREFIX_PATH + "/bin/yes";
    public static final String SETUP_CLANG_PATH = TermuxService.PREFIX_PATH + "/bin/setupclang-gfort-9";

    final static ByteQueue mProcessToTerminalIOQueue = new ByteQueue(4096);
    final static ByteTranslator mTermEmulator = new ByteTranslator(10, 1, 1);

    @SuppressLint("HandlerLeak")
    final static Handler mMainThreadHandler = new Handler() {
        final byte[] mReceiveBuffer = new byte[4 * 1024];

        @Override
        public void handleMessage(Message msg) {

            int bytesRead = mProcessToTerminalIOQueue.read(mReceiveBuffer, false);

            if (bytesRead > 0) {
                Log.e(TERMUX_INSTALLER_TAG, new String(mReceiveBuffer));

                if (progress != null) {
                    progress.setMessage(new String(mReceiveBuffer));
                }

            }

        }
    };

    /** Performs setup if necessary. */
    static void setupIfNeeded(final Activity activity, final Runnable whenDone) {
        // Termux can only be run as the primary user (device owner) since only that
        // account has the expected file system paths. Verify that:
        UserManager um = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        boolean isPrimaryUser = um.getSerialNumberForUser(android.os.Process.myUserHandle()) == 0;
        if (!isPrimaryUser) {
            new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_not_primary_user_message)
                .setOnDismissListener(dialog -> System.exit(0)).setPositiveButton(android.R.string.ok, null).show();
            return;
        }

        final File PREFIX_FILE = new File(PREFIX_PATH);
        if (PREFIX_FILE.isDirectory()) {
            // whenDone.run();
            return;
        }

        // Here is the pop-up window with "Installing.."
        // final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);

        new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                try {
                    final String STAGING_PREFIX_PATH = TermuxService.FILES_PATH + "/usr-staging";
                    final File STAGING_PREFIX_FILE = new File(STAGING_PREFIX_PATH);

                    if (STAGING_PREFIX_FILE.exists()) {
                        deleteFolder(STAGING_PREFIX_FILE);
                    }

                    final byte[] buffer = new byte[8096];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(50);

                    final byte[] zipBytes = loadZipBytes();
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = symlinksReader.readLine()) != null) {
                                    String[] parts = line.split("←");
                                    if (parts.length != 2)
                                        throw new RuntimeException("Malformed symlink line: " + line);
                                    String oldPath = parts[0];
                                    String newPath = STAGING_PREFIX_PATH + "/" + parts[1];
                                    symlinks.add(Pair.create(oldPath, newPath));

                                    ensureDirectoryExists(new File(newPath).getParentFile());
                                }
                            } else {
                                String zipEntryName = zipEntry.getName();
                                File targetFile = new File(STAGING_PREFIX_PATH, zipEntryName);
                                boolean isDirectory = zipEntry.isDirectory();

                                ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());

                                if (!isDirectory) {
                                    try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                        int readBytes;
                                        while ((readBytes = zipInput.read(buffer)) != -1)
                                            outStream.write(buffer, 0, readBytes);
                                    }
                                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") || zipEntryName.startsWith("lib/apt/methods")) {
                                        //noinspection OctalInteger
                                        Os.chmod(targetFile.getAbsolutePath(), 0700);
                                    }
                                }
                            }
                        }
                    }

                    if (symlinks.isEmpty())
                        throw new RuntimeException("No SYMLINKS.txt encountered");
                    for (Pair<String, String> symlink : symlinks) {
                        Os.symlink(symlink.first, symlink.second);
                    }

                    if (!STAGING_PREFIX_FILE.renameTo(PREFIX_FILE)) {
                        throw new RuntimeException("Unable to rename staging folder");
                    }

                    // Trying to install wget etc.
                    // It seems it works
                    // TerminalInstaller mTerminalInstaller = new TerminalInstaller();
                    // mTerminalInstaller.installDefault(service);
                    installDefault();

                    activity.runOnUiThread(whenDone);
                } catch (final Exception e) {
                    Log.e(EmulatorDebug.LOG_TAG, "Bootstrap error", e);
                    activity.runOnUiThread(() -> {
                        try {
                            new AlertDialog.Builder(activity)
                                .setTitle(R.string.bootstrap_error_title)
                                .setMessage(R.string.bootstrap_error_body)
                                .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                                    dialog.dismiss();
                                    activity.finish();
                                }).setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                                    dialog.dismiss();
                                    TermuxInstaller.setupIfNeeded(activity, whenDone);
                                }).show();
                        } catch (WindowManager.BadTokenException e1) {
                            // Activity already dismissed - ignore.
                        }
                    });
                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    private static void ensureDirectoryExists(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new RuntimeException("Unable to create directory: " + directory.getAbsolutePath());
        }
    }

    public static byte[] loadZipBytes() {
        // Only load the shared library when necessary to save memory usage.
        System.loadLibrary("termux-bootstrap");
        return getZip();
    }

    public static native byte[] getZip();

    /** Delete a folder and all its content or throw. Don't follow symlinks. */
    static void deleteFolder(File fileOrDirectory) throws IOException {
        if (fileOrDirectory.getCanonicalPath().equals(fileOrDirectory.getAbsolutePath()) && fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteFolder(child);
                }
            }
        }

        if (!fileOrDirectory.delete()) {
            throw new RuntimeException("Unable to delete " + (fileOrDirectory.isDirectory() ? "directory " : "file ") + fileOrDirectory.getAbsolutePath());
        }
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "termux-storage";
        new Thread() {
            public void run() {
                try {
                    File storageDir = new File(HOME_PATH, "storage");

                    if (storageDir.exists()) {
                        try {
                            deleteFolder(storageDir);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Could not delete old $HOME/storage, " + e.getMessage());
                            return;
                        }
                    }

                    if (!storageDir.mkdirs()) {
                        Log.e(LOG_TAG, "Unable to mkdirs() for $HOME/storage");
                        return;
                    }

                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    Os.symlink(downloadsDir.getAbsolutePath(), new File(storageDir, "downloads").getAbsolutePath());

                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    Os.symlink(dcimDir.getAbsolutePath(), new File(storageDir, "dcim").getAbsolutePath());

                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Os.symlink(picturesDir.getAbsolutePath(), new File(storageDir, "pictures").getAbsolutePath());

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    Os.symlink(musicDir.getAbsolutePath(), new File(storageDir, "music").getAbsolutePath());

                    File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    Os.symlink(moviesDir.getAbsolutePath(), new File(storageDir, "movies").getAbsolutePath());

                    final File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 1) {
                        for (int i = 1; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error setting up link", e);
                }
            }
        }.start();
    }

    /* INSTALLERS */

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void installDefault(){
        pkgInstall(DEFAULT_PACKAGES_1);
        // Curl and run
/*
        runProgram(CURL_PATH, new String[]{
            "-LO", "https://its-pointless.github.io/setup-pointless-repo.sh"
        });
*/
        runProgram(CURL_PATH, new String[]{
            "-LO", POINTLESS_REPO_CONFIG_SCRIPT
        });

        runProgram(BASH_PATH, new String[]{
            "setup-pointless-repo.sh"
        });

        pkgInstall(DEFAULT_PACKAGES_2);
        cp(new String[]{
            TermuxService.PREFIX_PATH + "/bin/gfortran-9",
            TermuxService.PREFIX_PATH + "/bin/gfortran",
        });
        runProgram(SETUP_CLANG_PATH, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void pkgInstall(String[] packages){
        String[] cmd = Stream.concat(Arrays.stream(new String[]{"install", "-y"}),
            Arrays.stream(packages)).toArray(String[]::new);
        runProgram(PKG_PATH, cmd);
    }

    private static void aptGet(String[] args){
        runProgram(APT_GET_PATH, args);
    }

    private static void cp(String[] fromAndTarget){
        runProgram(CP_PATH, fromAndTarget);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void yesBash(String[] bashArgs) {
        String[] cmd = Stream.concat(Arrays.stream(new String[]{"|", "bash"}),
            Arrays.stream(bashArgs)).toArray(String[]::new);
        runProgram(YES_PATH, cmd);
    }

    private static void runProgram(String programPath, String[] arguments){
        // String[] cmd = Stream.concat(Arrays.stream(command),
        //    Arrays.stream(packages)).toArray(String[]::new);

        Log.e("Run:", programPath);

        Uri executableUri = Uri.parse(programPath);
        String executablePath = (executableUri == null ? null : executableUri.getPath());

        // String[] arguments = (executableUri == null ? null : cmd);
        String cwd = HOME_PATH;
        boolean failsafe = false;
        new File(HOME_PATH).mkdirs();

        String[] env = BackgroundJob.buildEnvironment(failsafe, cwd);
        boolean isLoginShell = false;

        if (executablePath == null) {
            if (!failsafe) {
                for (String shellBinary : new String[]{"login", "bash", "zsh"}) {
                    File shellFile = new File(PREFIX_PATH + "/bin/" + shellBinary);
                    if (shellFile.canExecute()) {
                        executablePath = shellFile.getAbsolutePath();
                        break;
                    }
                }
            }
            if (executablePath == null) {
                // Fall back to system shell as last resort:
                executablePath = "/system/bin/sh";
            }
            isLoginShell = true;
        }

        String[] processArgs = BackgroundJob.setupProcessArgs(executablePath, arguments);
        executablePath = processArgs[0];
        int lastSlashIndex = executablePath.lastIndexOf('/');
        String processName = (isLoginShell ? "-" : "") +
            (lastSlashIndex == -1 ? executablePath : executablePath.substring(lastSlashIndex + 1));

        String[] args = new String[processArgs.length];
        args[0] = processName;
        if (processArgs.length > 1) System.arraycopy(processArgs, 1, args, 1, processArgs.length - 1);

        int[] processId = new int[1];

        int mTerminalFileDescriptor = JNI.createSubprocess(executablePath, cwd, args, env, processId, 0, 0);
        FileDescriptor mTerminalFileDescriptorWrapped = wrapFileDescriptor(mTerminalFileDescriptor);

        new Thread("TermSessionInputReader[pid=" + processId[0] + "]") {
            @Override
            public void run() {
                try (InputStream termIn = new FileInputStream(mTerminalFileDescriptorWrapped)) {
                    final byte[] buffer = new byte[4096];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1) return;
                        if (!mProcessToTerminalIOQueue.write(buffer, 0, read)) return;
                        mMainThreadHandler.sendEmptyMessage(1);
                    }
                } catch (Exception e) {
                    // Ignore, just shutting down.
                }
            }
        }.start();

        int processExitCode = JNI.waitFor(processId[0]);
        JNI.close(mTerminalFileDescriptor);
    }

    private static FileDescriptor wrapFileDescriptor(int fileDescriptor) {
        FileDescriptor result = new FileDescriptor();
        try {
            Field descriptorField;
            try {
                descriptorField = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                // For desktop java:
                descriptorField = FileDescriptor.class.getDeclaredField("fd");
            }
            descriptorField.setAccessible(true);
            descriptorField.set(result, fileDescriptor);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            Log.wtf(EmulatorDebug.LOG_TAG, "Error accessing FileDescriptor#descriptor private field", e);
            System.exit(1);
        }
        return result;
    }

}
