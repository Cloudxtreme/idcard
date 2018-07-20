package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.us.x42.kyork.idcard.data.IDCard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class IntraProfileActivity extends AppCompatActivity {
    private static IntraAPI api = null;

    private List<ProgressBar> progressBars = new ArrayList<ProgressBar>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);

    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {

    }

    private void populateUI(String login, IDCard idcard) {
        if (idcard != null) {
            //Populate UI info with card data
        }
        if (api.isCached(login)) {
            TextView fullNameText = findViewById(R.id.full_name);
            fullNameText.setText(api.getFullName(login));

            TextView titleText = findViewById(R.id.title);
            String title = api.getTitle(login);
            titleText.setText(title);

            ImageView bmImage = findViewById(R.id.user_picture);
            Picasso.get().load(api.getImageURL(login)).fit().centerInside().noFade().into(bmImage);
            bmImage.setVisibility(View.VISIBLE);

            JSONArray cursus_users = api.getCursusArray(login);

            TextView cursusHeader = findViewById(R.id.cursus_header);
            TextView levelHeader = findViewById(R.id.level_header);
            TextView gradeHeader = findViewById(R.id.grade_header);
            cursusHeader.setVisibility(View.VISIBLE);
            levelHeader.setVisibility(View.VISIBLE);
            gradeHeader.setVisibility(View.VISIBLE);

            TextView cursusText = findViewById(R.id.cursus);
            TextView levelText = findViewById(R.id.level);
            TextView gradeText = findViewById(R.id.grade);

            float dens = getResources().getDisplayMetrics().density;
            System.out.println(" *****************DEBUG dens = " + dens);

            StringBuilder cursusNames = new StringBuilder();
            StringBuilder cursusLevels = new StringBuilder();
            StringBuilder cursusGrades = new StringBuilder();
            if (cursus_users != null) {
                try {
                    for (int i = 0; i < cursus_users.length(); i++) {
                        JSONObject cursus_user = cursus_users.getJSONObject(i);
                        JSONObject cursus = cursus_user.getJSONObject("cursus");
                        cursusNames.append(cursus.getString("name")).append("\n");

                        double level = cursus_user.getDouble("level");
                        ProgressBar progressBar;
                        if (i < this.progressBars.size())
                            progressBar = this.progressBars.get(i);
                        else {
                            View view = this.getLayoutInflater().inflate(R.layout.level_bar, (ViewGroup)levelHeader.getParent());
                            progressBar = view.findViewWithTag("Unused");
                            progressBar.setTag("Used");
                            //progressBar.setY(progressBar.getY() + (i * 33)); //There's got to be a better way to get this height lol
                            progressBar.setY(progressBar.getY() + (i * 16.5f * dens));
                            this.progressBars.add(progressBar);
                        }
                        progressBar.setProgress((int)(progressBar.getMax() * (level - (int)level)));
                        progressBar.setVisibility(View.VISIBLE);

                        cursusLevels.append(Double.toString(level)).append("\n");
                        String grade = cursus_user.getString("grade");
                        if (!grade.equals("null"))
                            cursusGrades.append(grade).append("\n");
                        else
                            cursusGrades.append("Novice\n");
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace(System.err);
                }
            }
            cursusText.setText(cursusNames.toString());
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
                }
                catch (IOException | JSONException e) {
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

    private void resetUI() {
        TextView fullNameText = findViewById(R.id.full_name);
        fullNameText.setText("");
        TextView titleText = findViewById(R.id.title);
        titleText.setText("");
        ImageView bmImage = findViewById(R.id.user_picture);
        bmImage.setVisibility(View.INVISIBLE);
        TextView cursusHeader = findViewById(R.id.cursus_header);
        TextView levelHeader = findViewById(R.id.level_header);
        TextView gradeHeader = findViewById(R.id.grade_header);
        cursusHeader.setVisibility(View.INVISIBLE);
        levelHeader.setVisibility(View.INVISIBLE);
        gradeHeader.setVisibility(View.INVISIBLE);
        TextView cursusText = findViewById(R.id.cursus);
        TextView levelText = findViewById(R.id.level);
        TextView gradeText = findViewById(R.id.grade);
        cursusText.setText("");
        levelText.setText("");
        gradeText.setText("");
        for (ProgressBar progressBar : this.progressBars)
            progressBar.setVisibility(View.INVISIBLE);
        TextView accountTypeText = findViewById(R.id.account_type);
        accountTypeText.setText("");
        TextView coalitionText = findViewById(R.id.coalition);
        coalitionText.setText("");
        TextView phoneText = findViewById(R.id.phone);
        phoneText.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.resetUI();

        final IDCard idcard;
        final String login;
        Intent launchIntent = getIntent();
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
                IntraProfileActivity.this.resetUI();
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
