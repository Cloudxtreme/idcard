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
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileDoorPermissions;
import org.us.x42.kyork.idcard.data.FileSignatures;
import org.us.x42.kyork.idcard.data.FileUserInfo;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.tasks.WriteCardTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class IntraProfileActivity extends AppCompatActivity {
    private static IntraAPI api = null;

    private static final int NFC_REQUEST_CODE = 3;

    private List<ProgressBar> progressBars = new ArrayList<ProgressBar>();
    private TextView levelHeader;
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

    private void beginWriteTask(IDCard idcard) {
        Intent intent = new Intent(this, CardWriteActivity.class);
        WriteCardTask task = new WriteCardTask(idcard);
        intent.putExtra(CardWriteActivity.CARD_JOB_PARAMS, task);
        startActivityForResult(intent, NFC_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == NFC_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                WriteCardTask task = data.getParcelableExtra(CardWriteActivity.CARD_JOB_PARAMS);
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
            }
            Log.i("WriteCardTask", "RESULT => " + resultCode);
        }
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

        if (login.isEmpty())

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
                JSONObject user = IntraProfileActivity.api.cachedUser(login);
                if (user != null) {
                    try {
                        IDCard id = idcard;

                        if (id == null)
                            id = new IDCard();

                        if (id.fileUserInfo == null)
                            id.fileUserInfo = new FileUserInfo(new byte[CardDataFormat.FORMAT_USERINFO.expectedSize]);

                        if (id.fileDoorPermissions == null)
                            id.fileDoorPermissions = new FileDoorPermissions(new byte[CardDataFormat.FORMAT_DOORPERMS.expectedSize]);

                        if (id.fileSignatures == null)
                            id.fileSignatures = new FileSignatures(new byte[CardDataFormat.FORMAT_SIGNATURES.expectedSize]);

                        id.fileUserInfo.setLogin(login);
                        id.fileUserInfo.setIntraUserID(user.getInt("id"));

                        JSONArray campus_users = user.getJSONArray("campus_users");
                        for (int i = 0; i < campus_users.length(); i++) {
                            JSONObject campus_user = campus_users.getJSONObject(i);
                            if (campus_user.getBoolean("is_primary")) {
                                id.fileUserInfo.setCampusID((byte)campus_user.getInt("campus_id"));
                                break;
                            }
                        }

                        boolean staff = user.getBoolean("staff?");
                        JSONArray cursus_users = api.getCursusArray(login);

                        if (staff)
                            id.fileUserInfo.setAccountType((byte)0x03);
                        else {
                            if (cursus_users == null || cursus_users.length() == 1) {
                                if (cursus_users.length() == 1) {
                                    JSONObject cursus_user = cursus_users.getJSONObject(0);
                                    JSONObject cursus = cursus_user.getJSONObject("cursus");
                                    if (cursus.getString("slug").equals("piscine-c"))
                                        id.fileUserInfo.setAccountType((byte)0x02);
                                    else
                                        id.fileUserInfo.setAccountType((byte)0x01);
                                }
                                else
                                    id.fileUserInfo.setAccountType((byte)0x02);
                            }
                            else
                                id.fileUserInfo.setAccountType((byte)0x01);
                        }
                        /*
                        if (cursus_users != null) {
                            for (int i = 0; i < cursus_users.length(); i++) {
                                JSONObject cursus_user = cursus_users.getJSONObject(i);
                                JSONObject cursus = cursus_user.getJSONObject("cursus");
                                if (cursus.getString("slug").equals("piscine-c")) {
                                    String end_at = cursus.getString("end_at");
                                    Date date = new SimpleDateFormat("YYYY-MM-DD").parse(end_at.substring(0, 10));
                                    id.fileUserInfo.setPiscineEndDate(date);
                                }
                            }
                        }
                        */

                        id.fileUserInfo.getDirtyRanges().add(new int[] { 0, id.fileUserInfo.getExpectedFileSize() });
//                        id.fileDoorPermissions.getDirtyRanges().add(new int[] { 0, id.fileDoorPermissions.getExpectedFileSize() });

                        IntraProfileActivity.this.beginWriteTask(id);
                    }
                    catch (JSONException e) {
                        e.printStackTrace(System.err);
                    }
                }
                return true;
            }
        };

        boolean shouldReload = launchIntent.getBooleanExtra("shouldReload", false);

        if (api == null)
            api = new IntraAPI();

        if (shouldReload || !api.isCached(login))
            this.fetchUser(login, idcard);
        else
            this.populateUI(login, idcard);
    }
}
