package halachmi.lior.nivutqr;

import java.sql.Timestamp;
import java.util.Date;

public class Punch {
    Timestamp punch_time;
    Checkpoint checkpoint;
    String answer;

    public void setPunch_time(Timestamp punch_time) {
        this.punch_time = punch_time;
    }

    public void setCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Date getPunch_time() {
        return punch_time;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public String getAnswer() {
        return answer;
    }
}
