package halachmi.lior.nivutqr;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Event {

    private int game;
    private String game_name;
    private boolean freeorder;
    private boolean questions;
    private String organizer;
    private String phone;
    private String time_limit;
    private String error;
    private Checkpoint[] checkpoints;

    public Event() {
    }

    public int getGame() {
        return game;
    }

    public String getGame_name() {
        return game_name;
    }

    public boolean isFreeorder() {
        return freeorder;
    }

    public boolean isQuestions() {
        return questions;
    }

    public String getOrganizer() {
        return organizer;
    }

    public String getPhone() {
        return phone;
    }

    public Timestamp getTime_limit() {
        //return time_limit;
        return Timestamp.valueOf(time_limit);
    }

    public String getError() {
        return error;
    }

    public Checkpoint[] getCheckpoints() {
        return checkpoints;
    }
}
