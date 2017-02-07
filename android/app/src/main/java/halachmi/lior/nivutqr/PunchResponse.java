package halachmi.lior.nivutqr;

public class PunchResponse {
    private int game;
    private int checkpoint;
    private int number;
    private boolean is_start;
    private boolean is_finish;

    private String question;
    private String options;

    public PunchResponse() {
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
        return game;
    }

    public int getCheckpoint() {
        return checkpoint;
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
}
