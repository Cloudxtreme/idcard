package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button launchSetup = (Button) findViewById(R.id.launch_setup);
        launchSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SetupActivity.class);
                startActivity(intent);
            }
        });

        Button launchIntraProfile = (Button) findViewById(R.id.launch_intraprofile);
        launchIntraProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, IntraProfileActivity.class);
                EditText loginTextbox = (EditText) findViewById(R.id.login_textbox);
                String login = loginTextbox.getText().toString().toLowerCase();
                if (login.equals(""))
                    login = "ashih";
                intent.putExtra("login", login);
                startActivity(intent);
            }
        });
    }

}
