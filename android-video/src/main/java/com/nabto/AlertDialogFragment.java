package com.nabto;

/* This is to allow usage of the R class wherever it is generated */

import com.nabto.nabtovideo.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class AlertDialogFragment extends android.support.v4.app.DialogFragment {
    // public static FakeDanfoss unused_danfoss;
    // public static FakeNabtoBrowser unused_nabto_browser;

    public static AlertDialogFragment createInstance(String title,
            String message) {
        AlertDialogFragment fragment = new AlertDialogFragment();

        Bundle args = new Bundle();
        args.putString("TITLE", title);
        args.putString("MESSAGE", message);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("TITLE");
        String message = getArguments().getString("MESSAGE");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setIcon(R.drawable.icon);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismiss();
            }
        });

        return builder.create();
    }
}