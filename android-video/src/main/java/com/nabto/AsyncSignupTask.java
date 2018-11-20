package com.nabto;

import com.nabto.api.NabtoClient;
import com.nabto.api.NabtoStatus;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncSignupTask extends AsyncTask<Void, Void, NabtoStatus> {
    private String email;
    private String password;
    private NabtoClient api;
    private ProgressDialog dialog;
    private LoginActivity loginActivity;

    public AsyncSignupTask(LoginActivity loginActivity, String email,
            String password, NabtoClient api) {
        this.email = email;
        this.password = password;
        this.api = api;
        this.loginActivity = loginActivity;

        dialog = new ProgressDialog(loginActivity);
        dialog.setCancelable(false);
        dialog.setMessage("Signing up...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected NabtoStatus doInBackground(Void... params) {
        return api.signup(email, password);
    }

    @Override
    protected void onPostExecute(NabtoStatus resultStatus) {
        dialog.dismiss();
        Log.d(this.getClass().getSimpleName(), "Signup status: " + resultStatus);
        
        switch (resultStatus) {
        default:
            loginActivity.showAlertDialog("Account signup failed",
                    "Could not create account. Try again later.");
            break;
        case OK:
            loginActivity.showAlertDialog("Account Created",
                    "Email verification is needed to continue. Check your email inbox.");
            break;

        case ADDRESS_IN_USE:
            loginActivity.showAlertDialog("Email in use",
                    "Could not create account, since the email address you are signing up for is already in use.");
            break;

        case INVALID_ADDRESS:
            loginActivity.showAlertDialog("Email address invalid",
                    "Could not create account since the email supplied is invalid.");
            break;

        case NO_NETWORK:
            loginActivity.showAlertDialog("No internet",
                    "Please check your internet connection.");
            break;
        }
    }
}
