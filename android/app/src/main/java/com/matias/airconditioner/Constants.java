package com.matias.airconditioner;

import java.util.UUID;

/**
 * Created by Matias on 11/07/2017.
 */
public class Constants {
    public static int minTemp = 16;
    public static int maxTemp = 25;
    public static int temp = 20;

    public static boolean IS_LOGGABLE = BuildConfig.DEBUG;
    // SPP UUID service - this should work for most devices
    public static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
}