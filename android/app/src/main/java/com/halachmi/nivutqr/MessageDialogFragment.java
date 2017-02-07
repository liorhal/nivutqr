package com.halachmi.nivutqr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class MessageDialogFragment extends DialogFragment {
    public String message = "default Message";

    public MessageDialogFragment() {
    }

    public void SetArguments(String p_message){
        message = p_message;
    }

    public static MessageDialogFragment newInstance(String p_message) {
        MessageDialogFragment f = new MessageDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("message", p_message);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        message = getArguments().getString("message");
        String[] options = {"OK"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(message).setItems(options, null);
        return builder.create();
    }
}


