package com.termux.app;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;

public class RConsole {

    public static final String R_PATH = TermuxService.PREFIX_PATH + "/bin/R";

    public static void start(TermuxService service){
        Intent commandIntent = new Intent();
        commandIntent.setAction(TermuxService.ACTION_EXECUTE);
        commandIntent.setData(Uri.parse(R_PATH));
        service.onStartCommand(commandIntent, Service.START_FLAG_REDELIVERY, 1);
    }


}
