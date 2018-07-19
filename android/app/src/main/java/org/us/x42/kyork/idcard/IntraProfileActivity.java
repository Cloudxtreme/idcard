package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.URL;

public class IntraProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intra_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent launchIntent = getIntent();
        String profileName = launchIntent.getStringExtra("login");

        ImageView bmImage = findViewById(R.id.user_picture);

        Picasso.get().load("https://cdn.intra.42.fr/users/medium_apuel.jpg").into(bmImage);
        // can't gradle

//        Glide.with(this).load("http://i.imgur.com/DvpvklR.png").into(imageView);
        // can't gradle


//        try {
//            bmImage.setImageBitmap(BitmapFactory.decodeStream((new URL("http://i.imgur.com/DvpvklR.png")).openConnection() .getInputStream()));
//        } catch (IOException e) {
//
//        }









//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

    }

}
