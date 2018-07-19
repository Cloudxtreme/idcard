package org.us.x42.kyork.idcard;

import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.accounts.*;
import android.app.Activity;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;


public class IntraProfileActivity extends AppCompatActivity {
    private static JSONObject token = null;
    private static HashMap<String,HashMap<String,String>> properties = new HashMap<String,HashMap<String,String>>();

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {

    }

    private static String getProperty(String login, String key) {
        HashMap<String,String> props = IntraProfileActivity.properties.get(login);
        if (props == null)
            return (null);
        return (props.get(key));
    }

    private static String apiCall(String apipath) throws IOException, JSONException {
        URL url = new URL("https://api.intra.42.fr" + apipath);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + IntraProfileActivity.token.get("access_token"));

        InputStream stream = conn.getInputStream();
        String response = "";
        byte[] buffer = new byte[4096];
        int b;
        while ((b = stream.read(buffer)) != -1)
        response += new String(Arrays.copyOfRange(buffer, 0, b));
        stream.close();
        return response;
    }

    private static String getResponse(URLConnection conn) throws IOException {
        InputStream stream = conn.getInputStream();
        String response = "";
        byte[] buffer = new byte[4096];
        int b;
        while ((b = stream.read(buffer)) != -1)
            response += new String(Arrays.copyOfRange(buffer, 0, b));
        stream.close();
        return response;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent launchIntent = getIntent();
        //String login = launchIntent.getStringExtra("login");
        final String login = "mlu";

        Thread tokenThread = new Thread() {
            @Override
            public void run() {
                String uid = "c7c56a88eef81dbcd50dea5d1384d1e9448e786699672d78db78e29c9f2584e7";
                String secret = "bea0ca93b5bc8606cddc318c083f89008e09c20bb7568b4bc8925c3fe9419e6b";

                try {
                    if (IntraProfileActivity.token == null) {
                        URL url = new URL("https://api.intra.42.fr/oauth/token");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        String data = "client_id=" + uid;
                        data += "&client_secret=" + secret;
                        data += "&grant_type=client_credentials";
                        conn.setRequestMethod("POST");

                        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                        writer.write(data);
                        writer.flush();
                        writer.close();

                        JSONObject token = new JSONObject(getResponse(conn));
                        if (!token.has("access_token"))
                            throw new JSONException("Failed to retrieve token!");

                        IntraProfileActivity.token = token;
                    }

                    HashMap<String,String> props = IntraProfileActivity.properties.get(login);
                    if (props == null) {
                        props = new HashMap<String,String>();
                        IntraProfileActivity.properties.put(login, props);
                    }

                    JSONObject user = new JSONObject(apiCall("/v2/users/" + login));
                    props.put("first_name", user.getString("first_name"));
                    props.put("last_name", user.getString("last_name"));
                    props.put("phone", user.getString("phone"));

                    JSONArray titles_users = user.getJSONArray("titles_users");
                    int title_id = -1;
                    for (int i = 0; i < titles_users.length(); i++) {
                        JSONObject titles_user = titles_users.getJSONObject(i);
                        if (titles_user.getBoolean("selected")) {
                            title_id = titles_user.getInt("title_id");
                            break;
                        }
                    }

                    JSONArray titles = user.getJSONArray("titles");
                    String title = login;
                    if (title_id != -1) {
                        for (int i = 0; i < titles.length(); i++) {
                            JSONObject title_obj = titles.getJSONObject(i);
                            if (title_id == title_obj.getInt("id")) {
                                title = title_obj.getString("name").replaceAll("%login", login);
                                break;
                            }
                        }
                    }
                    props.put("title", title);

                    JSONArray cursus_users = user.getJSONArray("cursus_users");
                    for (int i = 0; i < cursus_users.length(); i++) {
                        JSONObject cursus_user = cursus_users.getJSONObject(i);
                        JSONObject cursus = cursus_user.getJSONObject("cursus");
                        if ("42".equals(cursus.getString("name"))) {
                            props.put("level", Double.toString(cursus_user.getDouble("level")));
                            props.put("grade", cursus_user.getString("grade"));
                            break;
                        }
                    }


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        tokenThread.start();

        try {
            tokenThread.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        ImageView bmImage = findViewById(R.id.user_picture);
        Picasso.get().load("https://cdn.intra.42.fr/users/medium_" + login + ".jpg").into(bmImage);

        TextView fullNameText = (TextView)findViewById(R.id.full_name);
        fullNameText.setText("Name: " + getFullName(login));

        TextView loginText = (TextView)findViewById(R.id.login);
        loginText.setText("Login: " + login);

        TextView titleText = (TextView)findViewById(R.id.title);
        titleText.setText("Title: " + getTitle(login));

        TextView levelText = (TextView)findViewById(R.id.level);
        levelText.setText("Level: " + getLevel(login));

        TextView gradeText = (TextView)findViewById(R.id.grade);
        gradeText.setText("Grade: " + getGrade(login));

        TextView accountTypeText = (TextView)findViewById(R.id.account_type);
        accountTypeText.setText("Type: " + getAccountType(login));

        TextView coalitionText = (TextView)findViewById(R.id.coalition);
        coalitionText.setText("Coalition: " + getCoalition(login));

        TextView phoneText = (TextView)findViewById(R.id.phone);
        phoneText.setText("Phone: " + getPhone(login));
    }

    private String getFullName(String login) { return getProperty(login,"first_name") + " " + getProperty(login,"last_name"); }

    private String getTitle(String login)
    {
        return getProperty(login,"title");
    }

    private String getLevel(String login) { return getProperty(login,"level"); }

    private String getGrade(String login)
    {
        return getProperty(login,"grade");
    }

    private String getAccountType(String login) { return getProperty(login,"account_type"); }

    private String getCoalition(String login)
    {
        return getProperty(login,"coalition");
    }

    private String getPhone(String login)
    {
        return getProperty(login,"phone");
    }












}
