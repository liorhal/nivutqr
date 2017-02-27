package halachmi.lior.nivutqr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class PunchAdapter extends ArrayAdapter<Log> {
    private final Context context;
    private final ArrayList<Log> logs;

    PunchAdapter(Context context, ArrayList<Log> logs) {
        super(context, -1, logs);
        this.context = context;
        this.logs = logs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Collections.sort(logs, new LogComparator());

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.simple_list_item, parent, false);
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView number = (TextView) rowView.findViewById(R.id.number);

        String answer = logs.get(position).getAnswer();
        boolean synced = logs.get(position).getSynced();
        DateFormat formatter = SimpleDateFormat.getTimeInstance();
        String dateFormatted = formatter.format(logs.get(position).getPunch_time());
        number.setText(String.valueOf(logs.get(position).getCheckpoint().getNumber()));
        firstLine.setText(dateFormatted);
        if (answer != null && !answer.equals("")){
            secondLine.setText("Answered " + answer + ".");
        }

        // change the icon
        if (answer != null && answer.equals("")){
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            if (answer != null && logs.get(position).getAnswer().equals(logs.get(position).getCheckpoint().getAnswer())) {
                imageView.setImageResource(R.mipmap.ic_correct);
            } else {
                imageView.setImageResource(R.mipmap.ic_wrong);
            }
        }

        if (MainActivity.unsynced && !synced){
            secondLine.setText(secondLine.getText() + " NOT SYNCED");
        }

        Animation animation = AnimationUtils
                .loadAnimation(context, R.anim.slide_top_to_bottom);
        rowView.startAnimation(animation);

        return rowView;
    }
}

class LogComparator implements Comparator<Log>
{
    public int compare(Log left, Log right) {
        return right.getPunch_time().compareTo(left.getPunch_time());
    }
}