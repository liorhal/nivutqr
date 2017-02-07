package halachmi.lior.nivutqr;

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
