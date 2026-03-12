package com.example.menu_pos.data;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Access to user accounts. Seeds Manager and Employee on first use.
 */
public class UserRepository {

    private static final String PREF_SESSION = "login_session";
    private static final String KEY_LOGGED_IN_USER = "logged_in_username";

    private final UserDao userDao;
    private final Context appContext;

    public UserRepository(Context context) {
        appContext = context.getApplicationContext();
        userDao = AppDatabase.getInstance(appContext).userDao();
        seedIfEmpty();
    }

    private void seedIfEmpty() {
        if (userDao.getCount() == 0) {
            userDao.insert(new User("Manager", "Naomie123"));
            userDao.insert(new User("Employee", ""));
        }
    }

    /** Returns the User if username exists and password matches (or no password required). */
    @Nullable
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) return null;
        User user = userDao.getByUsername(username.trim());
        if (user == null) return null;
        if (user.hasPassword()) {
            if (password == null || !password.equals(user.password)) return null;
        }
        return user;
    }

    public void setLoggedInUser(String username) {
        appContext.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOGGED_IN_USER, username)
                .apply();
    }

    @Nullable
    public String getLoggedInUser() {
        return appContext.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
                .getString(KEY_LOGGED_IN_USER, null);
    }

    public void logout() {
        appContext.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LOGGED_IN_USER)
                .apply();
    }

    public boolean isLoggedIn() {
        return getLoggedInUser() != null;
    }

    /** Change password for a user (e.g. Manager). Verifies current password first. Returns true if successful. */
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        User user = userDao.getByUsername(username);
        if (user == null) return false;
        if (user.hasPassword() && (currentPassword == null || !currentPassword.equals(user.password)))
            return false;
        String toStore = newPassword != null ? newPassword : "";
        userDao.updatePassword(username, toStore);
        return true;
    }
}
