package com.nabto;

import com.nabto.api.NabtoClient;
import com.nabto.api.NabtoStatus;
import android.app.ProgressDialog;
import android.os.AsyncTask;

public class AsyncLoginTask extends
        AsyncTask<Void, Void, AsyncLoginTask.ResultStatus> {
    private String email;
    private String password;
    private NabtoClient client;
    private ProgressDialog dialog;
    private LoginActivity loginActivity;

    public class ResultStatus {
    }

    public class ResultStatusSuccess extends ResultStatus {
    }

    public class ResultStatusError extends ResultStatus {
        private String headLine;
        private String text;

        ResultStatusError(String headLine, String text) {
            this.headLine = headLine;
            this.text = text;
        }

        public String headLine() {
            return headLine;
        }

        public String text() {
            return text;
        }
    }

    public class ResultStatusLoginFailure extends ResultStatusError {
        ResultStatusLoginFailure() {
            super("Login failed", "Your credentials are not correct.");
        }
    }
    
    public class ResultStatusNoInternet extends ResultStatusError {
        ResultStatusNoInternet() {
            super("No internet", "Please check your internet connection.");
        }
    }
    
    public class ResultStatusCertFailure extends ResultStatusError {
    	ResultStatusCertFailure() {
            super("Certificate signing failed", "No connection to certificate server.");
        }
    }

    public class ResultStatusCustomError extends ResultStatusError {
        ResultStatusCustomError(String headLine, String text) {
            super(headLine, text);
        }
    }

    public AsyncLoginTask(LoginActivity loginActivity, String email,
            String password, String loginText, NabtoClient client) {
        this.email = email;
        this.password = password;
        this.client = client;
        this.loginActivity = loginActivity;

        dialog = new ProgressDialog(loginActivity);
        dialog.setCancelable(false);
        dialog.setMessage(loginText);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected ResultStatus doInBackground(Void... params) {
    	NabtoStatus status = client.init(email, password);
        if (status == NabtoStatus.OK)
            return new ResultStatusSuccess();
        else if (status == NabtoStatus.NO_NETWORK)
            return new ResultStatusNoInternet();
        else if (status == NabtoStatus.PORTAL_LOGIN_FAILURE || status == NabtoStatus.UNLOCK_PK_FAILED)
            return new ResultStatusLoginFailure();
        else if (status == NabtoStatus.CERT_SIGNING_ERROR)
            return new ResultStatusCertFailure();
        return new ResultStatusCustomError("" + status, "");
    }

    @Override
    protected void onPostExecute(ResultStatus resultStatus) {
        dialog.dismiss();
        if (resultStatus instanceof ResultStatusSuccess)
            loginActivity.startWebView(email, password);
        else if (resultStatus instanceof ResultStatusError) {
            ResultStatusError err = (ResultStatusError) resultStatus;
            loginActivity.showAlertDialog(err.headLine(), err.text());
        }
    }
}
