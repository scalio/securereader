package info.guardianproject.securereaderinterface.uiutil;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {
    private static final String TAG = "SharedPrefHelper";

    public static void insertPref(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Global.PREF_SHARED_FILE_KEY, Context.MODE_PRIVATE);
        sharedPref.edit().putString(key, value).apply();
    }

    public static String getPref(Context context, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Global.PREF_SHARED_FILE_KEY, Context.MODE_PRIVATE);
        String data = sharedPref.getString(value, null);

        return data;
    }
    
    public static void deletePref(Context context, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Global.PREF_SHARED_FILE_KEY, Context.MODE_PRIVATE);
        sharedPref.edit().remove(value).commit();
    }
}