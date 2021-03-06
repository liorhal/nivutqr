package halachmi.lior.nivutqr;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NivutQrApi {
    @GET("/game/{game}/checkpoint/{checkpoint}/participant/{participant}/punch/{punch_time}")
    Call<Checkpoint> punch(
            @Path("participant") String participant,
            @Path("checkpoint") int checkpoint,
            @Path("game") int game,
            @Path("punch_time") String punch_time
    );

    @GET("/game/{game}/checkpoint/{checkpoint}/participant/{participant}/answer/{answer}/punch/{punch_time}")
    Call<Checkpoint> answer(
            @Path("participant") String participant,
            @Path("checkpoint") int checkpoint,
            @Path("game") int game,
            @Path("answer") int answer,
            @Path("punch_time") String punch_time
    );

    @GET("/game/{game}/participant/{participant}/message")
    Call<Message> message(
            @Path("game") int game,
            @Path("participant") String participant
    );

    @GET("/game/{game}/participant/{participant}/register2")
    Call<Event> register(
            @Path("participant") String participant,
            @Path("game") int game
    );
}
