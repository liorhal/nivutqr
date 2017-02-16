package halachmi.lior.nivutqr;

public class Message {
    private int game;
    private String message;
    private String participants;

    public Message() {
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

    public String getParticipants() {
        return participants;
    }
}
