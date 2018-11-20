package com.nabto;

import com.nabto.api.NabtoClient;
import com.nabto.api.NabtoStatus;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncResetAccountPasswordTask extends
        AsyncTask<Void, Void, NabtoStatus> {
    private String email;
    private NabtoClient api;
    private ProgressDialog dialog;
    private LoginActivity loginActivity;

    public AsyncResetAccountPasswordTask(LoginActivity loginActivity,
            String email, NabtoClient api) {
        this.email = email;
        this.api = api;
        this.loginActivity = loginActivity;

        dialog = new ProgressDialog(loginActivity);
        dialog.setCancelable(false);
        dialog.setMessage("Resetting password...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected NabtoStatus doInBackground(Void... params) {
        return api.resetAccountPassword(email);
    }

    @Override
    protected void onPostExecute(NabtoStatus resultStatus) {
        dialog.dismiss();
        Log.d(this.getClass().getSimpleName(), "Reset status: " + resultStatus);

        switch (resultStatus) {
        default:
            loginActivity.showAlertDialog("Failed",
                    "Could not reset password. Please try again later.");
            break;
        case OK:
            loginActivity.showAlertDialog("Password reset",
                    "A password reset mail has been sent to your email account. Check your inbox.");
            break;

        case INVALID_ADDRESS:
            loginActivity.showAlertDialog("Email address invalid",
                    "Could not reset account password since the email supplied is invalid.");
            break;

        case NO_NETWORK:
            loginActivity.showAlertDialog("No internet",
                    "Please check your internet connection.");
            break;
        }
    }
}
