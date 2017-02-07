package com.halachmi.nivutqr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.sql.Date;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements QuestionDialogFragment.QuestionDialogListener{

    static final int QR_REQUEST_CODE = 1020;

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";

    static final String API_BASE_URL = "https://nivutqr.herokuapp.com/";

    private Handler mHandler;

    static final String textInputState = "textInputState";

    int checkpoint = 0;
    int game = 0;
    String message = "";

    boolean debugMode = true;

    private boolean mIsInForegroundMode = false;

    @Override
    protected void onResume() {
        mIsInForegroundMode = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mIsInForegroundMode = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopRepeatingTask();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                                               .setAction("Action", null).show();
                                   }
                               });

        mHandler = new Handler();
        startRepeatingTask();
        setupUIEvent();
    }

    void startRepeatingTask() {
        mMessageChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mMessageChecker);
    }

    Runnable mMessageChecker = new Runnable() {
        @Override
        public void run() {
            try {
                try {
                    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

                    Retrofit.Builder builder =
                            new Retrofit.Builder()
                                    .baseUrl(API_BASE_URL)
                                    .addConverterFactory(
                                            GsonConverterFactory.create()
                                    );

                    Retrofit retrofit =
                            builder
                                    .client(
                                            httpClient.build()
                                    )
                                    .build();

                    NivutQrApi client = retrofit.create(NivutQrApi.class);

                    Call<MessageResponse> call = client.message(game);

                    call.enqueue(new Callback<MessageResponse>() {
                        @Override
                        public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                            if (response != null) {
                                String new_message = response.body().getMessage();
                                if (!message.equals(new_message)) {
                                    message = new_message;
                                    Handler h = new Handler(MainActivity.this.getMainLooper());
                                    h.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                                            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                                            try {
                                                NotificationCompat.Builder mBuilder =
                                                        new NotificationCompat.Builder(MainActivity.this)
                                                                .setSmallIcon(R.drawable.message_icon)
                                                                .setContentTitle("Important Message")
                                                                .setContentText(message);
                                                // Sets an ID for the notification
                                                int mNotificationId = 001;
                                                NotificationManager mNotifyMgr =
                                                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                                mNotifyMgr.notify(mNotificationId, mBuilder.build());
                                            }
                                            catch (Exception exp)
                                            {
                                                message = exp.toString();
                                            }

                                            AlertDialog.Builder messageDialog = new AlertDialog.Builder(MainActivity.this);
                                            messageDialog.setTitle(R.string.alert_important_message);
                                            messageDialog.setMessage(message);
                                            messageDialog.setPositiveButton("OK", null);
                                            messageDialog.show();
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<MessageResponse> call, Throwable t) {
                            if (t != null) {
                                Log.d("response",t.toString());
                            }
                        }

                    });
                }
                catch (Exception exp) {
                    Log.d("status",exp.getMessage().toString());
                }

            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mMessageChecker, 60*1000);
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_event_details:
                showEventDetails();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void showEventDetails(){
        Toast.makeText(MainActivity.this, "action_event_details", Toast.LENGTH_LONG).show();
    }


    void setupUIEvent(){
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleButtonClick((Button) view);
            }
        });
    }

    void handleButtonClick(Button button){
        if (debugMode) {
            TextView nameTextView = (TextView) findViewById(R.id.text_name);
            nameTextView.setText("Lior");

            handleQrActivityResult(RESULT_OK, null);
        }
        else {

            TextView nameTextView = (TextView) findViewById(R.id.text_name);

            if (nameTextView.getText().length() == 0) {
                if (nameTextView.getText().length() == 0) {

                    nameTextView.setError("Please Fill Name");
                }
            } else {
                nameTextView.setEnabled(false);

                try {
                    //start the scanning activity from the com.google.zxing.client.android.SCAN intent
                    Intent intent = new Intent(ACTION_SCAN);
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    startActivityForResult(intent, QR_REQUEST_CODE);
                } catch (ActivityNotFoundException anfe) {
                    //on catch, show the download dialog
                    showDialog(this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
                }
            }
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {

                }
            }
        });
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        switch (requestCode){
            case QR_REQUEST_CODE:
                handleQrActivityResult(resultCode, resultIntent);
                break;
        }
    }

    private void handleQrActivityResult(int resultCode, Intent resultIntent) {
        String contents = "";
        if (resultCode == RESULT_OK) {
            if (debugMode){
                contents = "{\"game\": \"1\",\"checkpoint\": \"3\"}";
            }
            else {
                contents = resultIntent.getStringExtra("SCAN_RESULT");
                //String format = resultIntent.getStringExtra("SCAN_RESULT_FORMAT");
            }
            String name = getEditTextValue(R.id.text_name);

            try {
                JSONObject qr = new JSONObject(contents);
                checkpoint = qr.getInt("checkpoint");
                game = qr.getInt("game");
            }
            catch(JSONException exp){
                Toast.makeText(MainActivity.this, exp.getMessage(), Toast.LENGTH_LONG).show();
            }
            try {
                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

                Retrofit.Builder builder =
                        new Retrofit.Builder()
                                .baseUrl(API_BASE_URL)
                                .addConverterFactory(
                                        GsonConverterFactory.create()
                                );

                Retrofit retrofit =
                        builder
                                .client(
                                        httpClient.build()
                                )
                                .build();

                NivutQrApi client = retrofit.create(NivutQrApi.class);

                Call<PunchResponse> call = client.punch(name, checkpoint, game);

                call.enqueue(new Callback<PunchResponse>() {
                    @Override
                    public void onResponse(Call<PunchResponse> call, Response<PunchResponse> response) {
                        if (response != null) {
                            final String question = response.body().getQuestion();
                            if (question != null && question != "") {
                                Handler h = new Handler(MainActivity.this.getMainLooper());
                                final String[] options = response.body().getOptions().split(";");

                                h.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        DialogFragment newDialogFragment = QuestionDialogFragment.newInstance(question, options);
                                        newDialogFragment.show(getFragmentManager(), "question");
                                    }
                                });
                            }
                            else
                            {
                                LogCheckpointOnScreen();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PunchResponse> call, Throwable t) {
                        if (t != null) {
                            Log.d("response",t.toString());
                        }
                    }

                });
            }
            catch (Exception exp) {
                Log.d("status",exp.getMessage().toString());
            }
        }
        else{
            Toast.makeText(this, "User Canceled", Toast.LENGTH_LONG).show();
        }
    }
    private String getEditTextValue(int TextViewId){
        EditText editText = (EditText) findViewById(TextViewId);
        Editable editable = editText.getText();
        return editable != null? editable.toString() : "";
    }

    public void LogCheckpointOnScreen(){
        //TextView textView = (TextView) findViewById(R.id.checkpointList);
        //textView.setText(textView.getText() + DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + "\t\t\t" + checkpoint + System.getProperty("line.separator"));

        String name = getEditTextValue(R.id.text_name);

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setUseWideViewPort(true);
        webView.loadUrl("https://nivutqr.herokuapp.com/game/"+game+"/participant/"+name);
    }

    @Override
    public void onQuestionOptionClick(DialogFragment dialog, int selectedOption) {
        try {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            Retrofit.Builder builder =
                    new Retrofit.Builder()
                            .baseUrl(API_BASE_URL)
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            );

            Retrofit retrofit =
                    builder
                            .client(
                                    httpClient.build()
                            )
                            .build();

            NivutQrApi client = retrofit.create(NivutQrApi.class);

            String name = getEditTextValue(R.id.text_name);

            Call<PunchResponse> call = client.answer(name, checkpoint, game, selectedOption);

            call.enqueue(new Callback<PunchResponse>() {
                @Override
                public void onResponse(Call<PunchResponse> call, Response<PunchResponse> response) {
                    if (response != null) {
                        Handler h = new Handler(MainActivity.this.getMainLooper());

                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"your checkpoint logged",Toast.LENGTH_LONG).show();
                                LogCheckpointOnScreen();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<PunchResponse> call, Throwable t) {
                    if (t != null) {
                        Log.d("response",t.toString());
                    }
                }

            });
        }
        catch (Exception exp) {
            Log.d("status",exp.getMessage().toString());
        }

    }

    // ...



}
