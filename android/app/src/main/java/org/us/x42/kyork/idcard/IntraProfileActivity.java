package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.tasks.WriteCardTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class IntraProfileActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_UPDATECARD = 1;
    private static final int REQUEST_CODE_CHECKUPDATES = 2;

    private List<ProgressBar> progressBars = new ArrayList<ProgressBar>();
    private TextView levelHeader;
    private IDCard updateContent = null;
    private MenuItem.OnMenuItemClickListener refreshCallback;
    private MenuItem.OnMenuItemClickListener writeCallback;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        MenuItem refresh = menu.findItem(R.id.refresh);
        refresh.setOnMenuItemClickListener(this.refreshCallback);
        MenuItem write = menu.findItem(R.id.write);
        write.setOnMenuItemClickListener(this.writeCallback);
        return true;
    }

    private void beginUpdateTask() {
        startActivityForResult(CardWriteActivity.getIntent(this, new WriteCardTask(updateContent)), REQUEST_CODE_UPDATECARD);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_UPDATECARD) {
            if (resultCode == RESULT_OK) {
                WriteCardTask task = CardWriteActivity.getResultData(data);
                String err = task.getErrorString(this);
                if (err.isEmpty()) {
                    String userLogin = task.getCard().fileUserInfo.getLogin();
                    Snackbar statusBar = Snackbar.make(levelHeader,
                            String.format(this.getResources().getString(R.string.writecard_success), userLogin),
                            Snackbar.LENGTH_LONG);
                    statusBar.show();
                    Log.i("WriteCardTask", "success");
                } else {
                    Snackbar statusBar = Snackbar.make(levelHeader, err, Snackbar.LENGTH_LONG);
                    statusBar.show();
                    Log.i("WriteCardTask", err);
                }
            } else {
                // TODO
            }
            Log.i("WriteCardTask", "RESULT => " + resultCode);
        }
    }

    private void populateUI(String login, IDCard idcard) {
        if (idcard != null) {
            //Populate UI info with card data
        }
        IntraAPI api = IntraAPI.get();

        if (api.isCached(login)) {
            TextView fullNameText = findViewById(R.id.full_name);
            fullNameText.setText(api.getFullName(login));

            TextView titleText = findViewById(R.id.title);
            String title = api.getTitle(login);
            titleText.setText(title);

            ImageView bmImage = findViewById(R.id.user_picture);
            Picasso.get().load(api.getImageURL(login)).fit().centerInside().noFade().into(bmImage);
            bmImage.setVisibility(View.VISIBLE);

            TextView cursusHeader = findViewById(R.id.cursus_header);
            TextView gradeHeader = findViewById(R.id.grade_header);
            cursusHeader.setVisibility(View.VISIBLE);
            levelHeader.setVisibility(View.VISIBLE);
            gradeHeader.setVisibility(View.VISIBLE);

            TextView cursusText = findViewById(R.id.cursus);
            TextView levelText = findViewById(R.id.level);
            TextView gradeText = findViewById(R.id.grade);

            StringBuilder cursusNames = new StringBuilder();
            StringBuilder cursusLevels = new StringBuilder();
            StringBuilder cursusGrades = new StringBuilder();

            float dens = getResources().getDisplayMetrics().density;
            JSONArray cursus_users = api.getCursusArray(login);

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
                            progressBar.setY(progressBar.getY() + (i * 16.5f * dens));
                            this.progressBars.add(progressBar);
                        }
                        progressBar.setProgress((int)(progressBar.getMax() * (level % 1.0)));
                        progressBar.setVisibility(View.VISIBLE);

                        cursusLevels.append(String.format(Locale.US, "%.2f", level)).append("\n");
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
                    IntraAPI.get().queryUser(login, false);
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

                // Check for updates to the card
                if (idcard != null) {
                    try {
                        IDCard updateContent = ServerAPIFactory.getAPI().getCardUpdates(idcard.serial, idcard.fileUserInfo.getLastUpdated());
                        if (updateContent != null) {
                            IntraProfileActivity.this.updateContent = updateContent;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    IntraProfileActivity.this.markUpdateRequired();
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
        ImageView updateAlert = findViewById(R.id.update_needed_alert);
        updateAlert.setVisibility(View.GONE);
        TextView cursusHeader = findViewById(R.id.cursus_header);
        // levelHeader = findViewById(R.id.level_header);
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

    private void markUpdateRequired() {
        ImageView updateAlert = findViewById(R.id.update_needed_alert);
        updateAlert.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        levelHeader = findViewById(R.id.level_header);

        this.resetUI();

        final IDCard idcard;
        final String login;
        Intent launchIntent = getIntent();
        if (launchIntent.hasExtra("idcard")) {
            idcard = launchIntent.getParcelableExtra("idcard");
            if (idcard.fileUserInfo != null)
                login = idcard.fileUserInfo.getLogin();
            else
                throw new IllegalArgumentException("Missing FileUserInfo on IDCard");
        }
        else if (launchIntent.hasExtra("login")) {
            login = launchIntent.getStringExtra("login");
            idcard = null;
        }
        else {
            login = "mlu";
            idcard = null;
        }

        this.refreshCallback = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                IntraProfileActivity.this.resetUI();
                IntraProfileActivity.this.fetchUser(login, idcard);
                return true;
            }
        };

        this.writeCallback = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                IntraProfileActivity.this.beginUpdateTask();
                return true;
            }
        };

        ImageView updateAlert = findViewById(R.id.update_needed_alert);
        updateAlert.setClickable(true);
        updateAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntraProfileActivity.this.beginUpdateTask();
            }
        });

        boolean shouldReload = launchIntent.getBooleanExtra("shouldReload", false);

        if (shouldReload || !IntraAPI.get().isCached(login))
            this.fetchUser(login, idcard);
        else
            this.populateUI(login, idcard);
    }
}
