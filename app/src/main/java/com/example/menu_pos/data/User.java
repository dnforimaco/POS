package com.example.menu_pos.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * User account for login. Password may be empty (no password required).
 */
@Entity(tableName = "users", indices = {@Index(value = "username", unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public final String username;
    public final String password;

    /** For Room when reading from DB. */
    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password != null ? password : "";
    }

    @Ignore
    /** For inserting new users (id is auto-generated). */
    public User(String username, String password) {
        this.id = 0;
        this.username = username;
        this.password = password != null ? password : "";
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}
