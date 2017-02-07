package com.halachmi.nivutqr;

import java.util.Date;

/**
 * Created by owner on 28/01/2017.
 */
public class MessageResponse {
    private int game;
    private String message;

    public MessageResponse() {
    }

    public int getGame() {
        return game;
    }

    public String getMessage() {
        try {
            return message;
        } catch (Exception exp) {
            return null;
        }
    }
}
