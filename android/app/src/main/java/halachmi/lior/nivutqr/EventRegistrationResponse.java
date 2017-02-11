package halachmi.lior.nivutqr;

import java.sql.Timestamp;
import java.util.Date;
import java.util.IdentityHashMap;

public class EventRegistrationResponse {

    private int game;
    private String game_name;
    private boolean freeorder;
    private String organizer;
    private String phone;
    private Date time_limit;
    private String error;
    private PunchResponse[] checkpoints;

    public EventRegistrationResponse() {
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

    public String getOrganizer() {
        return organizer;
    }

    public String getPhone() {
        return phone;
    }

    public Date getTime_limit() {
        return time_limit;
    }

    public String getError() {
        return error;
    }
}
