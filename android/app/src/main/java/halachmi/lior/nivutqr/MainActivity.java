package halachmi.lior.nivutqr;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;


public class MainActivity extends AppCompatActivity implements QuestionDialogFragment.QuestionDialogListener {

    static final String SHOWCASEVIEW_ID = "welcome_id";

    static final String API_BASE_URL =  "https://nivutqr.herokuapp.com/";//"http://10.0.2.2:5000/";
    static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 1;

    private Handler mHandler;

    int checkpoint = 0;
    int game = 0;
    String message = "";

    Event event_details = null;
    static SparseArray<Checkpoint> checkpoints = new SparseArray<>();
    static ArrayList<halachmi.lior.nivutqr.Log> logs = new ArrayList<>();
    static boolean unsynced = false;
    PunchAdapter punchAdapter;

    boolean debugMode = false;
    String debug_content = "{\"game\": \"1\",\"checkpoint\": \"1\"}";

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
    public void onBackPressed() {
        AlertDialog.Builder messageDialog = new AlertDialog.Builder(MainActivity.this);
        messageDialog.setMessage(R.string.exit_confirm);
        messageDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        messageDialog.setNegativeButton(R.string.no, null);
        messageDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.app_name));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                handleButtonClick((FloatingActionButton) view);
            }
        });

        mHandler = new Handler();
        setupUIEvent();

        showMaterialShowcaseview();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
        }
        GetAccountName();

        ListView listview = (ListView) findViewById(R.id.list_view);

        punchAdapter = new PunchAdapter(this, logs);
        // Assign adapter to ListView
        listview.setAdapter(punchAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long arg3) {
                // TODO Auto-generated method stub
                int itemPosition = position;

                ListView listview = (ListView) findViewById(R.id.list_view);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + " Value:" + listview.getItemAtPosition(itemPosition), Toast.LENGTH_LONG)
                        .show();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                 return;
            }
        }
    }

    private void GetAccountName() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] list = manager.getAccounts();
            for (Account account : list) {
                if (account.type.equalsIgnoreCase("com.google")) {
                    String[] email_parts = account.name.split("@");
                    if (email_parts.length > 0 && email_parts[0] != null) {
                        TextView nameTextView = (TextView) findViewById(R.id.text_name);
                        nameTextView.setText(email_parts[0]);

                    }
                    break;
                }
            }
        }
    }

    void showMaterialShowcaseview(){

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(300); // half second between each showcase view
        config.setShapePadding(100);

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this);

        sequence.setConfig(config);

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this)
                        .setTarget(findViewById(R.id.fab))
                        .setDismissText(getString(R.string.sc_got_it))
                        .setContentText(getString(R.string.sc_welcome))
                        .setDismissOnTouch(true)
                        .singleUse(SHOWCASEVIEW_ID)
                        .build()
        );

        sequence.start();
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

                    Call<Message> call = client.message(game, getEditTextValue(R.id.text_name));

                    call.enqueue(new Callback<Message>() {
                        @Override
                        public void onResponse(Call<Message> call, Response<Message> response) {
                            if (response != null && response.code() != 404) {
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
                                                                .setContentTitle(getString(R.string.alert_important_message))
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
                        public void onFailure(Call<Message> call, Throwable t) {
                            if (t != null) {
                                Log.d("response",t.toString());
                            }
                        }

                    });
                }
                catch (Exception exp) {
                    Log.d("status",exp.getMessage());
                }

            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                if (event_details!=null){
                    //stop checking messages 2 hours after the event time limit
                    Timestamp check_message_time_limit =  new Timestamp(event_details.getTime_limit().getTime() + 2 * 60 * 60 * 1000);
                    long now = System.currentTimeMillis();
                    Timestamp now_t = new Timestamp(now);
                    if (now_t.before(check_message_time_limit)){
                        //check for message every 2 minutes
                        mHandler.postDelayed(mMessageChecker, 120*1000);
                    }
                }

            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (event_details != null) {
            MenuItem mnuProgress = menu.findItem(R.id.action_event_progress);
            MenuItem mnuEventDetails = menu.findItem(R.id.action_event_details);

            mnuProgress.setVisible(true);
            mnuEventDetails.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handle item selection
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_event_details:
                showEventDetails();
                return true;
            case R.id.action_event_progress:
                LogCheckpointOnScreen();
                return true;
            case R.id.action_sync:
                Sync();
                return true;
            case R.id.action_exit:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu)
    {
        if (menu != null) {
            MenuItem mnuProgress = menu.findItem(R.id.action_event_progress);
            MenuItem mnuEventDetails = menu.findItem(R.id.action_event_details);
            MenuItem mnuSync = menu.findItem(R.id.action_sync);

            if (event_details != null) {
                mnuProgress.setVisible(true);
                mnuEventDetails.setVisible(true);
            }

            if (unsynced){
                mnuSync.setVisible(true);
            }
        }

        return true;
    }

    void showEventDetails(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setSubtitle(R.string.action_event_details);
        WebView webView = (WebView) findViewById(R.id.webView);
        findViewById(R.id.list_view).setVisibility(View.INVISIBLE);
        webView.setVisibility(View.VISIBLE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        String eventType = getString(R.string.by_number);
        if (event_details.isFreeorder()){
            eventType = getString(R.string.free_order);
        }
        String summary = "<html><body><table>" +
                "<tr><td>"+getString(R.string.event_name)+":</td><td>"+event_details.getGame_name()+"</td></tr>" +
                "<tr><td>"+getString(R.string.organizer)+":</td><td>"+event_details.getOrganizer()+"</td></tr>" +
                "<tr><td>"+getString(R.string.phone)+":</td><td>"+event_details.getPhone()+"</td></tr>" +
                "<tr><td>"+getString(R.string.event_type)+":</td><td>"+eventType+"</td></tr>" +
                "<tr><td>"+getString(R.string.time_limit)+":</td><td>"+event_details.getTime_limit()+"</td></tr>" +
                "</table><h2><b>"+getString(R.string.goodluck)+"</b></h2></body></html>";
        webView.loadData(summary, "text/html; charset=UTF-8", null);
    }


    void setupUIEvent(){

    }

    void handleButtonClick(FloatingActionButton button){
        TextView nameTextView = (TextView) findViewById(R.id.text_name);
        nameTextView.setText(nameTextView.getText().toString().trim());
        if (debugMode) {
            nameTextView.setText("Lior");
            //nameTextView.setError(null);
            //nameTextView.clearFocus();

            handleQrActivityResult(RESULT_OK, null);
        }
        else {
            if (nameTextView.getText().length() == 0) {
                nameTextView.setError(getString(R.string.err_fill_name));
            } else {
                nameTextView.setEnabled(false);

                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setOrientationLocked(true);
                integrator.setBeepEnabled(false);
                integrator.initiateScan();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                handleQrActivityResult(resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleQrActivityResult(int resultCode, Intent resultIntent) {
        String contents;
        if (resultCode == RESULT_OK) {
            if (debugMode){
                contents = debug_content;
            }
            else {
                contents = resultIntent.getStringExtra("SCAN_RESULT");
            }
            try {
                JSONObject qr = new JSONObject(contents);
                checkpoint = qr.getInt("checkpoint");
                game = qr.getInt("game");
            }
            catch(JSONException exp){
                Toast.makeText(MainActivity.this, exp.getMessage(), Toast.LENGTH_LONG).show();
            }
                if (event_details == null) {
                    Toast.makeText(MainActivity.this, R.string.event_details_first, Toast.LENGTH_LONG).show();
                    RegisterToEvent();
                }
                else {
                    Punch();
                }
        }
        else{
            Toast.makeText(this, R.string.user_canceled, Toast.LENGTH_LONG).show();
        }
        //Punch();
    }

    private void RegisterToEvent() {
        String name = getEditTextValue(R.id.text_name);
        try {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            Gson gson = new GsonBuilder()
                    //.setDateFormat("yyyy-MM-dd'THH:mm:ss")
                    //.registerTypeAdapter(Date.class, new DateDeserializer())
                    .create();

            Retrofit.Builder builder =
                    new Retrofit.Builder()
                            .baseUrl(API_BASE_URL)
                            .addConverterFactory(
                                    GsonConverterFactory.create(gson)
                            );

            Retrofit retrofit =
                    builder
                            .client(
                                    httpClient.build()
                            )
                            .build();

            NivutQrApi client = retrofit.create(NivutQrApi.class);

            Call<Event> call = client.register(name, game);

            call.enqueue(new Callback<Event>() {
                @Override
                public void onResponse(Call<Event> call, Response<Event> response) {
                    if (response != null) {
                        if (!response.body().getError().equalsIgnoreCase("unknown game")){
                            event_details = response.body();
                            startRepeatingTask();
                            Checkpoint[] checkpoints_array = event_details.getCheckpoints();
                            for(int i=0; i< checkpoints_array.length; i++){
                                checkpoints.append(checkpoints_array[i].getCheckpoint_id(), checkpoints_array[i]);
                            }
                            invalidateOptionsMenu();
                            TextView nameTextView = (TextView) findViewById(R.id.text_name);
                            nameTextView.setVisibility(View.INVISIBLE);
                            if (response.body().getError().equalsIgnoreCase("participant exists")){
                                Toast.makeText(MainActivity.this, R.string.participant_exists, Toast.LENGTH_LONG).show();
                                halachmi.lior.nivutqr.Log[] log_array = event_details.getLogs();
                                if (log_array != null) {
                                    for (int i = 0; i < log_array.length; i++) {
                                        log_array[i].setSynced(true);
                                        logs.add(log_array[i]);
                                        LogCheckpointOnScreen();
                                    }
                                }
                            }
                            else {
                                showEventDetails();
                            }
                        }
                        else{
                            Toast.makeText(MainActivity.this, R.string.unknown_game, Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Event> call, Throwable t) {
                    if (t != null) {
                        Log.d("response",t.toString());
                    }
                    Toast.makeText(MainActivity.this, "Unable to get event details", Toast.LENGTH_LONG).show();
                }

            });
        }
        catch (Exception exp) {
            Log.d("status",exp.getMessage());
        }
    }

    private void Punch() {
        if (event_details == null || checkpoints.size() == 0 || !event_details.isQuestions()) {
            String name = getEditTextValue(R.id.text_name);
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

                final halachmi.lior.nivutqr.Log log = new halachmi.lior.nivutqr.Log(name, checkpoints.get(checkpoint),
                        new Timestamp(System.currentTimeMillis()).toString(),
                        "");
                logs.add(log);
                LogCheckpointOnScreen();

                Call<Checkpoint> call = client.punch(name, checkpoint, game, new Timestamp(System.currentTimeMillis()).toString());

                call.enqueue(new Callback<Checkpoint>() {
                    @Override
                    public void onResponse(Call<Checkpoint> call, Response<Checkpoint> response) {
                        if (response != null) {
                            /*final String question = response.body().getQuestion();
                            if (question != null && !question.equals("")) {
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
                            else{*/
                                Toast.makeText(MainActivity.this, R.string.checkpoint_logged,Toast.LENGTH_LONG).show();
                                log.setSynced(true);
                            //}
                        }
                    }

                    @Override
                    public void onFailure(Call<Checkpoint> call, Throwable t) {
                        if (t != null) {
                            Log.d("response", t.toString());
                        }
                        //failed to log the punch
                        Toast.makeText(MainActivity.this, "Failed to log the punch.\nSettings->Sync when network available", Toast.LENGTH_LONG).show();
                        unsynced = true;
                        LogCheckpointOnScreen();
                    }

                });
            } catch (Exception exp) {
                Log.d("status", exp.getMessage());
            }
        }
        //if checkpoints internal cache already downloaded
        else{
            Checkpoint cp = checkpoints.get(checkpoint);
            if (cp.getQuestion() != null) {
                DialogFragment newDialogFragment = QuestionDialogFragment.newInstance(cp.getQuestion(), cp.getOptions().split(";"));
                newDialogFragment.show(getFragmentManager(), "question");
            }
        }
    }

    private void Sync() {
        unsynced = false;
        for (final halachmi.lior.nivutqr.Log log : logs) {
            if (!log.getSynced()) {
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

                    Call<Checkpoint> call = null;

                    call = client.punch(log.getParticipant(), log.getCheckpoint().getCheckpoint_id(), game, log.getPunch_time().toString());

                    if (!log.getAnswer().equals("")) {
                        call = client.answer(log.getParticipant(), log.getCheckpoint().getCheckpoint_id(), game, Integer.valueOf(log.getAnswer()), new Timestamp(System.currentTimeMillis()).toString());
                    }

                    call.enqueue(new Callback<Checkpoint>() {
                        @Override
                        public void onResponse(Call<Checkpoint> call, Response<Checkpoint> response) {
                            if (response != null) {
                                Toast.makeText(MainActivity.this, R.string.checkpoint_logged, Toast.LENGTH_LONG).show();
                                log.setSynced(true);
                                LogCheckpointOnScreen();
                            }
                        }

                        @Override
                        public void onFailure(Call<Checkpoint> call, Throwable t) {
                            if (t != null) {
                                Log.d("response", t.toString());
                            }
                            //failed to log the punch
                            Toast.makeText(MainActivity.this, "Failed to log the punch.\nSettings->Sync when network available", Toast.LENGTH_LONG).show();
                            unsynced = true;
                        }

                    });
                } catch (Exception exp) {
                    Log.d("status", exp.getMessage());
                }
            }
        }
        LogCheckpointOnScreen();
    }

    private String getEditTextValue(int TextViewId){
        EditText editText = (EditText) findViewById(TextViewId);
        Editable editable = editText.getText();
        return editable != null? editable.toString() : "";
    }

    public void LogCheckpointOnScreen(){
        //TextView textView = (TextView) findViewById(R.id.checkpointList);
        //textView.setText(textView.getText() + DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + "\t\t\t" + checkpoint + System.getProperty("line.separator"));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setSubtitle(R.string.action_event_progress);

        ListView view = (ListView)findViewById(R.id.list_view);
        if (view.getVisibility() == View.INVISIBLE) {
            findViewById(R.id.webView).setVisibility(View.INVISIBLE);
            view.setVisibility(View.VISIBLE);
        }

        /*
        String name = getEditTextValue(R.id.text_name);
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(API_BASE_URL + "game/"+game+"/participant/"+name);
        */
        punchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onQuestionOptionClick(DialogFragment dialog, final int selectedOption) {
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

            Call<Checkpoint> call = client.answer(name, checkpoint, game, selectedOption, new Timestamp(System.currentTimeMillis()).toString());

            final halachmi.lior.nivutqr.Log log = new halachmi.lior.nivutqr.Log(name, checkpoints.get(checkpoint), new Timestamp(System.currentTimeMillis()).toString(), String.valueOf(selectedOption));
            logs.add(log);
            LogCheckpointOnScreen();

            call.enqueue(new Callback<Checkpoint>() {
                @Override
                public void onResponse(Call<Checkpoint> call, final Response<Checkpoint> response) {
                    if (response != null) {
                        Handler h = new Handler(MainActivity.this.getMainLooper());
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, R.string.checkpoint_logged,Toast.LENGTH_LONG).show();
                                log.setSynced(true);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<Checkpoint> call, Throwable t) {
                    if (t != null) {
                        Log.d("response",t.toString());
                    }

                    //failed to log the punch
                    Toast.makeText(MainActivity.this, "Failed to log the punch.\nSettings->Sync when network available", Toast.LENGTH_LONG).show();
                    unsynced = true;
                }

            });
        }
        catch (Exception exp) {
            Log.d("status",exp.getMessage());
        }

    }
}
