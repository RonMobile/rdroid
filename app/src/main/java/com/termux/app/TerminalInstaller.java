package com.termux.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.stream.Stream;

public class TerminalInstaller {

    // TODO: merge with TermuxInstaller

    private static final String[] DEFAULT_PACKAGES_1 = new String[]{
        "curl",
        "wget",
        "gnupg"
    };

    // Packages needed to run R
    private static final String[] DEFAULT_PACKAGES_2 = new String[]{
        "r-base", "make", "clang gcc-9",  "openssl",
        "libcurl", "libicu", "libxml2", "ndk-sysroot",
        "pkg-config", "cmake", "git", "libcairo",
        "libtiff", "pango", "zlib"
    };

    private static final String PKG_PATH = TermuxService.PREFIX_PATH + "/bin/pkg";

    //  mTermService = ((TermuxService.LocalBinder) service).service;
    // Utworzyć osobne klasy do instalacji pakietów
    // Spróbować uruchomić w tle
    // Intent commandIntent = new Intent();
    // commandIntent.setAction("com.termux.service_execute");
    // commandIntent.setData(Uri.parse("/data/data/com.termux/files/usr/bin/pkg"));
    //commandIntent.putExtra("com.termux.execute.arguments", new String[]{"install", "wget"});
    //mTermService.onStartCommand(commandIntent, Service.START_FLAG_REDELIVERY, 1);

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void install(TermuxService service, String[] packages){

        String[] cmd = Stream.concat(Arrays.stream(new String[]{"install", "-y"}),
            Arrays.stream(packages)).toArray(String[]::new);

        Intent commandIntent = new Intent();
        // Cannot create hanlder inside!!!
        commandIntent.setAction(TermuxService.ACTION_EXECUTE_HIDDEN);
        commandIntent.setData(Uri.parse(PKG_PATH));
        commandIntent.putExtra(TermuxService.EXTRA_ARGUMENTS, cmd);
        commandIntent.putExtra(TermuxService.EXTRA_EXECUTE_IN_BACKGROUND, false);
        service.onStartCommand(commandIntent, Service.START_FLAG_REDELIVERY, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void installDefault(TermuxService service){
        this.install(service, DEFAULT_PACKAGES_1);
    }

}
