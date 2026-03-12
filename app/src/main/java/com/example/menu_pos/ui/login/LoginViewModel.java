package com.example.menu_pos.ui.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.User;
import com.example.menu_pos.data.UserRepository;

public class LoginViewModel extends AndroidViewModel {

    private final UserRepository userRepo;
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        userRepo = new UserRepository(application);
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        User user = userRepo.login(username, password);
        if (user != null) {
            userRepo.setLoggedInUser(user.username);
            loginResult.setValue(new LoginResult.Success(user.username));
        } else {
            loginResult.setValue(new LoginResult.Error());
        }
    }

    public abstract static class LoginResult {
        public static final class Success extends LoginResult {
            public final String username;
            public Success(String username) { this.username = username; }
        }
        public static final class Error extends LoginResult {}
    }
}
