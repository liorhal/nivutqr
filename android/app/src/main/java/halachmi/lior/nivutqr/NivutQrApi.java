package halachmi.lior.nivutqr;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NivutQrApi {
    @GET("/game/{game}/checkpoint/{checkpoint}/participant/{participant}/punch")
    Call<PunchResponse> punch(
            @Path("participant") String participant,
            @Path("checkpoint") int checkpoint,
            @Path("game") int game
    );

    @GET("/game/{game}/checkpoint/{checkpoint}/participant/{participant}/answer/{answer}")
    Call<PunchResponse> answer(
            @Path("participant") String participant,
            @Path("checkpoint") int checkpoint,
            @Path("game") int game,
            @Path("answer") int answer
    );

    @GET("/game/{game}/message")
    Call<MessageResponse> message(
            @Path("game") int game
    );

    @GET("/game/{game}/participant/{participant}/register")
    Call<EventRegistrationResponse> register(
            @Path("participant") String participant,
            @Path("game") int game
    );
}
