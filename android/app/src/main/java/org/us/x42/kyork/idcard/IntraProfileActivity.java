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
import org.us.x42.kyork.idcard.data.IDCard;

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
        IDCard idcard = null;
        String login = "mlu";
        if (launchIntent.hasExtra("idcard")) {
            idcard = launchIntent.getParcelableExtra("idcard");
            login = idcard.fileUserInfo.getLogin();
        }
        else if (launchIntent.hasExtra("login")) {
            login = launchIntent.getStringExtra("login");
        }
        boolean shouldReload = launchIntent.getBooleanExtra("shouldReload", false);

        if (api == null)
            api = new IntraAPI();

        if (shouldReload || !api.isCached(login)) {
            final String _login = login;
            Thread request = new Thread() {
                @Override
                public void run() {
                    try {
                        api.queryUser(_login, false);
                    } catch (IOException | JSONException e) {
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

        if (api.isCached(login)) {
            ImageView bmImage = findViewById(R.id.user_picture);
            Picasso.get().load("https://cdn.intra.42.fr/users/medium_" + login + ".jpg").into(bmImage);

            TextView fullNameText = findViewById(R.id.full_name);
            fullNameText.setText("Name: " + api.getFullName(login));

            TextView loginText = findViewById(R.id.login);
            loginText.setText("Login: " + login);

            TextView titleText = findViewById(R.id.title);
            titleText.setText("Title: " + api.getTitle(login));

            JSONObject cursus = api.getCursus(login, "42");

            TextView levelText = findViewById(R.id.level);
            levelText.setText("Level: 0");
            if (cursus != null) {
                try {
                    levelText.setText("Level: " + cursus.getString("level"));
                } catch (JSONException e) {
                    e.printStackTrace(System.err);
                }
            }

            TextView gradeText = findViewById(R.id.grade);
            gradeText.setText("Grade: Novice");
            if (cursus != null) {
                try {
                    gradeText.setText("Grade: " + cursus.getString("grade"));
                } catch (JSONException e) {
                    e.printStackTrace(System.err);
                }
            }

            TextView accountTypeText = findViewById(R.id.account_type);
            accountTypeText.setText("Type: null");

            TextView coalitionText = findViewById(R.id.coalition);
            coalitionText.setText("Coalition: null");

            TextView phoneText = findViewById(R.id.phone);
            phoneText.setText("Phone: " + api.getPhone(login));
        }
    }
}
