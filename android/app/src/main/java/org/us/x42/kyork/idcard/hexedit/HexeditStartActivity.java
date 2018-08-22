package org.us.x42.kyork.idcard.hexedit;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;

import org.us.x42.kyork.idcard.CardReadActivity;
import org.us.x42.kyork.idcard.ProgressStep;
import org.us.x42.kyork.idcard.ProgressStepListFragment;
import org.us.x42.kyork.idcard.ProgressStepRecyclerViewAdapter;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.tasks.CardNFCTask;

import java.util.ArrayList;
import java.util.List;

public class HexeditStartActivity extends Activity {

    /**
     * CardReadActivity. Returns the IDCard via {@link CardReadActivity#RESULT_CARD}.
     */
    private static final int REQ_READ_CARD = 1;
    /**
     * HexeditEditorViewActivity. Takes and returns a IDCard via
     * {@link HexeditEditorViewActivity#EDITOR_PARAMS_CARD}.
     */
    private static final int REQ_EDIT_CARD = 2;
    /**
     * CardWriteActivity. Takes a {@link org.us.x42.kyork.idcard.tasks.WriteCardTask} via
     * {@link org.us.x42.kyork.idcard.CardWriteActivity#CARD_JOB_PARAMS}.
     */
    private static final int REQ_WRITE_CARD = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent1 = new Intent(this, CardReadActivity.class);
        intent1.putExtra("TODO", false);
        this.startActivityForResult(intent1, REQ_READ_CARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_READ_CARD:
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
