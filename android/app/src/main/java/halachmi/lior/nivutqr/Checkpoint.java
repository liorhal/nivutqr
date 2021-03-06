package halachmi.lior.nivutqr;

public class Checkpoint {
    private int game_id;
    private int checkpoint_id;
    private int number;
    private boolean is_start;
    private boolean is_finish;

    private String question;
    private String options;
    private String answer;

    public Checkpoint() {
    }

    public int getNumber() {
        return number;
    }

    public boolean is_start() {
        return is_start;
    }

    public boolean is_finish() {
        return is_finish;
    }

    public int getGame() {
        return game_id;
    }

    public int getCheckpoint_id() {
        return checkpoint_id;
    }

    public String getQuestion() {
        try {
            return question;
        }
        catch(Exception exp){
            return null;
        }

    }

    public String getOptions() {
        return options;
    }

    public String getAnswer() {
        return answer;
    }
}
