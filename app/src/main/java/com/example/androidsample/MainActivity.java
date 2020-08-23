package com.example.androidsample;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/* This program feeds user input to the
Dialogflow chatbot and displays the response

all methods before onCreate() are in large
part from the tutorial by Abhinav Tyagi
entitled "Android chatbot with Dialogflow"
url: https://medium.com/@abhi007tyagi/android-chatbot-with-dialogflow-8c0dcc8d8018

 */
public class MainActivity extends AppCompatActivity {
    private SessionName session;
    private SessionsClient sessionsClient;
    private String uuid = UUID.randomUUID().toString();

    // this class handles sending the user response to the chatbot and detecting the response
    private static class RequestJavaV2Task extends AsyncTask<Void, Void, DetectIntentResponse> {
        Activity activity;
        SessionName session;
        SessionsClient sessionsClient;
        QueryInput queryInput;

        // sends user response to chatbot
        RequestJavaV2Task(Activity activity, SessionName session, SessionsClient sessionsClient, QueryInput queryInput) {
            this.activity = activity;
            this.session = session;
            this.sessionsClient = sessionsClient;
            this.queryInput = queryInput;
        }


        @Override
        // captures response from chatbot
        protected DetectIntentResponse doInBackground(Void... voids) {
            try {
                DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder().setSession(session.toString()).setQueryInput(queryInput).build();
                return sessionsClient.detectIntent(detectIntentRequest);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        // sends response back to user
        protected void onPostExecute(DetectIntentResponse response) {
            ((MainActivity) activity).callbackV2(response);
        }
    }


    // creates instance (session) of Dialogflow chatbot
    private void initV2Chatbot() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.test_agent_credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials)credentials).getProjectId();
            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
            System.out.println("DONE");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

   // captures user input in order to send it to chatbot
   public void sendMessage(View view) {
        EditText inputText = findViewById(R.id.dollarText);
        TextView textView = findViewById(R.id.textView);
        String msg = inputText.getText().toString();
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your query", Toast.LENGTH_LONG).show();
        }

        else {
            textView.setText(msg);

            // Java V2
            QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en-US")).build();
            new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();
        }

    }


    // displays output from chatbot
    public void callbackV2(DetectIntentResponse response) {
        EditText inputText = findViewById(R.id.dollarText);
        TextView textView = findViewById(R.id.textView);
        if (response != null) {
            // process aiResponse here
            String botReply = response.getQueryResult().getFulfillmentText();
            textView.setText(botReply);
        } else {
            textView.setText("There was a communication issue. Please Try again!");
        }
    }


    // shows result of recorded speech if voice recognition is successful
    // or shows "failed to recognize speech" if voice recognition is not successful
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        EditText inputText = findViewById(R.id.dollarText);
        TextView textView = findViewById(R.id.textView);
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> dataText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            StringBuilder dataString = new StringBuilder();
            for (int i = 0; i < dataText.size(); i++) {
                dataString.append(dataText.get(i));
            }
            inputText.setText(dataString.toString());
        } else {
            Toast.makeText(getApplicationContext(), "Failed to recognize speech!", Toast.LENGTH_LONG).show();
        }
    }

    // sets voice recognition on click of record button
    // sets message to be clicked when send button clicked
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText inputText = findViewById(R.id.dollarText);
        TextView textView = findViewById(R.id.textView);
        Button sendBtn = findViewById(R.id.button1);
        Button recordBtn = findViewById(R.id.button2);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                startActivityForResult(intent, 10);
            }
        });
        initV2Chatbot();

    }



   
}