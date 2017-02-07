package com.halachmi.nivutqr;

public class PunchResponse {
    private int game;
    private int checkpoint;
    private String question;
    private String options;

    public PunchResponse() {
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
