package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.us.x42.kyork.idcard.data.IDCard;

import java.io.IOException;


public class IntraProfileActivity extends AppCompatActivity {
    private static IntraAPI api = null;

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {

    }

    private void populateUI(String login, IDCard idcard) {
        if (idcard != null) {
            //Populate UI info with card data
        }
        if (api.isCached(login)) {
            ImageView bmImage = findViewById(R.id.user_picture);
            Picasso.get().load("https://cdn.intra.42.fr/users/medium_" + login + ".jpg").into(bmImage);

            TextView fullNameText = findViewById(R.id.full_name);
            fullNameText.setText(api.getFullName(login));

            TextView titleText = findViewById(R.id.title);
            String title = "(" + api.getTitle(login) + ")";
            titleText.setText(title);

            JSONArray cursus_users = api.getCursusArray(login);
            TextView cursusText = findViewById(R.id.cursus);
            TextView levelText = findViewById(R.id.level);
            TextView gradeText = findViewById(R.id.grade);

            String cursusNames = "";
            String cursusLevels = "";
            String cursusGrades = "";
            if (cursus_users != null) {
                try {
                    for (int i = 0; i < cursus_users.length(); i++) {
                        JSONObject cursus_user = cursus_users.getJSONObject(i);
                        JSONObject cursus = cursus_user.getJSONObject("cursus");
                        cursusNames += cursus.getString("name") + "\n";
                        cursusLevels += Double.toString(cursus_user.getDouble("level")) + "\n";
                        String grade = cursus_user.getString("grade");
                        if (!grade.equals("null"))
                            cursusGrades += grade + "\n";
                        else
                            cursusGrades += "Novice\n";
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace(System.err);
                }
            }
            cursusText.setText(cursusNames);
            levelText.setText(cursusLevels);
            gradeText.setText(cursusGrades);

            TextView accountTypeText = findViewById(R.id.account_type);
            accountTypeText.setText("Type: null");

            TextView coalitionText = findViewById(R.id.coalition);
            coalitionText.setText("Coalition: null");

            TextView phoneText = findViewById(R.id.phone);
            phoneText.setText("Phone: " + api.getPhone(login));
        }
    }

    private void fetchUser(final String login, final boolean updateUI, final IDCard idcard) {
        Thread request = new Thread() {
            @Override
            public void run() {
                try {
                    api.queryUser(login, false);
                } catch (IOException | JSONException e) {
                    e.printStackTrace(System.err);
                }

                if (updateUI) {
                    new Handler(Looper.getMainLooper()).post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    IntraProfileActivity.this.populateUI(login, idcard);
                                }
                            }
                    );
                }
            }
        };
        request.start();
    }

    private void fetchUser(final String login, final IDCard idcard) {
        this.fetchUser(login, true, idcard);
    }

    private void fetchUser(final String login) {
        this.fetchUser(login, false, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent launchIntent = getIntent();

        final IDCard idcard;
        final String login;
        if (launchIntent.hasExtra("idcard")) {
            idcard = launchIntent.getParcelableExtra("idcard");
            login = idcard.fileUserInfo.getLogin();
        }
        else if (launchIntent.hasExtra("login")) {
            login = launchIntent.getStringExtra("login");
            idcard = null;
        }
        else {
            login = "mlu";
            idcard = null;
        }

        Button refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntraProfileActivity.this.fetchUser(login, idcard);
            }
        });

        boolean shouldReload = launchIntent.getBooleanExtra("shouldReload", false);

        if (api == null)
            api = new IntraAPI();

        if (shouldReload || !api.isCached(login))
            this.fetchUser(login, idcard);
        else
            this.populateUI(login, idcard);
    }
}
