package com.termux;

import android.provider.BaseColumns;

public class PackagesContract {

    private PackagesContract() {}


    public static class PackageEntry implements BaseColumns {
        public static final String TABLE_NAME = "package";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_VERSION = "version";
        public static final String COLUMN_NAME_ACTION = "action";

    }

}

