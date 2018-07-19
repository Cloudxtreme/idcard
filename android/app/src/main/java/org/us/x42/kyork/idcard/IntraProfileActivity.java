package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class IntraProfileActivity extends AppCompatActivity {
    private static IntraAPI api = null;

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent launchIntent = getIntent();
        //String login = launchIntent.getStringExtra("login");
        final String login = "mlu"; //This should be loaded from the nfc card?
        boolean shouldReload = false;

        if (api == null)
            api = new IntraAPI();

        if (shouldReload || !api.isCached(login)) {
            Thread request = new Thread() {
                @Override
                public void run() {
                    try {
                        api.queryUser(login, false);
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    } catch (JSONException e) {
                        e.printStackTrace(System.err);
                    }
                }
            };
            request.start();

            try {
                request.join(); //Temporary, ideally while this thread runs, the ui should be doing something else
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }

        ImageView bmImage = findViewById(R.id.user_picture);
        Picasso.get().load("https://cdn.intra.42.fr/users/medium_" + login + ".jpg").into(bmImage);

        TextView fullNameText = (TextView)findViewById(R.id.full_name);
        fullNameText.setText("Name: " + api.getFullName(login));

        TextView loginText = (TextView)findViewById(R.id.login);
        loginText.setText("Login: " + login);

        TextView titleText = (TextView)findViewById(R.id.title);
        titleText.setText("Title: " + api.getTitle(login));

        JSONObject cursus = api.getCursus(login, "42");

        TextView levelText = (TextView)findViewById(R.id.level);
        if (cursus != null) {
            try {
                levelText.setText("Level: " + cursus.getString("level"));
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                levelText.setText("Level: null");
            }
        }
        else
            levelText.setText("Level: null");

        TextView gradeText = (TextView)findViewById(R.id.grade);
        if (cursus != null) {
            try {
                gradeText.setText("Grade: " + cursus.getString("grade"));
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                gradeText.setText("Grade: null");
            }
        }
        else
            gradeText.setText("Grade: null");

        TextView accountTypeText = (TextView)findViewById(R.id.account_type);
        accountTypeText.setText("Type: null");

        TextView coalitionText = (TextView)findViewById(R.id.coalition);
        coalitionText.setText("Coalition: null");

        TextView phoneText = (TextView)findViewById(R.id.phone);
        phoneText.setText("Phone: " + api.getPhone(login));
    }
}
