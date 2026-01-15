package com.phovl.cinemaphovlmobile.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "cinema_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ID = "user_id";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Guarda sesión con token, nombre y id de usuario.
     */
    public void saveSession(String token, String userName, int userId) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_NAME, userName);
        editor.putInt(KEY_USER_ID, userId);
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Versión antigua compatible.
     */
    public void saveSession(String token, String userName) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_NAME, userName);
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
