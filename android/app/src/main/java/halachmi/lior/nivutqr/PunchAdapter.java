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

class PunchAdapter extends ArrayAdapter<Punch> {
    private final Context context;
    private final ArrayList<Punch> punches;

    PunchAdapter(Context context, ArrayList<Punch> punches) {
        super(context, -1, punches);
        this.context = context;
        this.punches = punches;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Collections.sort(punches, new PunchComparator());

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.simple_list_item, parent, false);
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView number = (TextView) rowView.findViewById(R.id.number);

        String answer = punches.get(position).getAnswer();
        DateFormat formatter = SimpleDateFormat.getTimeInstance();
        String dateFormatted = formatter.format(punches.get(position).getPunch_time());
        number.setText(String.valueOf(punches.get(position).checkpoint.getNumber()));
        firstLine.setText(dateFormatted);
        if (!answer.equals("")){
            secondLine.setText("Answered " + answer);
        }

        // change the icon
        if (answer.equals("")){
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            if (punches.get(position).getAnswer().equals(punches.get(position).getCheckpoint().getAnswer())) {
                imageView.setImageResource(R.mipmap.ic_correct);
            } else {
                imageView.setImageResource(R.mipmap.ic_wrong);
            }
        }

        Animation animation = AnimationUtils
                .loadAnimation(context, R.anim.slide_top_to_bottom);
        rowView.startAnimation(animation);

        return rowView;
    }
}

class PunchComparator implements Comparator<Punch>
{
    public int compare(Punch left, Punch right) {
        return right.getPunch_time().compareTo(left.getPunch_time());
    }
}