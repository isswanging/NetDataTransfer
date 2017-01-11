package net.ui.fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class CustAlertDialog extends DialogFragment {
    String title;
    String alertText;
    DialogInterface.OnClickListener listener;

    public void setTitle(String text) {
        title = text;
    }

    public void setAlertText(String text) {
        alertText = text;
    }

    public void setListener(DialogInterface.OnClickListener l) {
        listener = l;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title)
                .setMessage(alertText)
                .setPositiveButton("退出",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getActivity().setResult(getActivity().RESULT_OK);// 确定按钮事件
                                System.exit(0);
                            }
                        })
                .setNegativeButton("重试", listener);
        return builder.create();
    }
}
