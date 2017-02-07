package halachmi.lior.nivutqr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

public class QuestionDialogFragment extends DialogFragment {
    public String[] options = {"1","2","3"};
    public String question = "default Question";

    public static QuestionDialogFragment newInstance(String p_question, String[] p_options) {
        QuestionDialogFragment f = new QuestionDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("question", p_question);
        args.putStringArray("options", p_options);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        question = getArguments().getString("question");
        options = getArguments().getStringArray("options");


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(question)
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        mListener.onQuestionOptionClick(QuestionDialogFragment.this, which+1);
                    }
                });
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface QuestionDialogListener {
        void onQuestionOptionClick(DialogFragment dialog, int selectedOption);
    }

    // Use this instance of the interface to deliver action events
    QuestionDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach (Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (QuestionDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}
