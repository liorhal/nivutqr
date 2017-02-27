package halachmi.lior.nivutqr;

import java.sql.Timestamp;

public class Log {
    private String punch_time;
    private Checkpoint checkpoint;
    private String answer;
    private String participant;
    private int checkpoint_id;
    private boolean synced;

    public Log(String participant, Checkpoint checkpoint, String punch_time, String answer) {
        this.checkpoint = checkpoint;
        this.checkpoint_id = checkpoint.getCheckpoint_id();
        this.answer = answer;
        this.punch_time = punch_time;
        this.participant = participant;
        this.synced = false;
    }

    public Timestamp getPunch_time() {
        return Timestamp.valueOf(punch_time);
    }

    public Checkpoint getCheckpoint() {
        if (checkpoint == null) {
            checkpoint = MainActivity.checkpoints.get(checkpoint_id);
        }
        return checkpoint;

    }

    public String getAnswer() {
        return answer;
    }

    public boolean getSynced() { return synced; }

    public String getParticipant() {
        return participant;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
