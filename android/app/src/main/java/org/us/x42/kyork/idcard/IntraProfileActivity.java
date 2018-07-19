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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;



public class IntraProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent launchIntent = getIntent();
        String login = launchIntent.getStringExtra("login");
        login = "apuel";

        ImageView bmImage = findViewById(R.id.user_picture);
        Picasso.get().load("https://cdn.intra.42.fr/users/medium_" + login + ".jpg").into(bmImage);

        // oauth2 wat do????








        TextView fullNameText = (TextView)findViewById(R.id.full_name);
        fullNameText.setText(getFullName(login));

        TextView loginText = (TextView)findViewById(R.id.login);
        loginText.setText(getLogin(login));

        TextView titleText = (TextView)findViewById(R.id.title);
        titleText.setText(getTitle(login));

        TextView levelText = (TextView)findViewById(R.id.level);
        levelText.setText(getLevel(login));

        TextView gradeText = (TextView)findViewById(R.id.grade);
        gradeText.setText(getGrade(login));

        TextView accountTypeText = (TextView)findViewById(R.id.account_type);
        accountTypeText.setText(getAccountType(login));

        TextView coalitionText = (TextView)findViewById(R.id.coalition);
        coalitionText.setText(getCoalition(login));

        TextView phoneText = (TextView)findViewById(R.id.phone);
        phoneText.setText(getPhone(login));
    }

    private String getFullName(String login)
    {
        return "Andres Puel";
    }

    private String getLogin(String login)
    {
        return "Login: " + login;
    }

    private String getTitle(String login)
    {
        return "Title: " + "emulat0r";
    }

    private String getLevel(String login)
    {
        return "Level: " + 9001;
    }

    private String getGrade(String login)
    {
        return "Grade: " + "High Lieutenant, Jr.";
    }

    private String getAccountType(String login)
    {
        return "Type: " + "Non-b00calian";
    }

    private String getCoalition(String login)
    {
        return "Coalition: " + "Memel0rds";
    }

    private String getPhone(String login)
    {
        return "Phone: " + "1-800-NID-FOOD";
    }












}
