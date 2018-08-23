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
import android.util.Log;

import org.us.x42.kyork.idcard.CardReadActivity;
import org.us.x42.kyork.idcard.CardWriteActivity;
import org.us.x42.kyork.idcard.ProgressStep;
import org.us.x42.kyork.idcard.ProgressStepListFragment;
import org.us.x42.kyork.idcard.ProgressStepRecyclerViewAdapter;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.tasks.CardNFCTask;
import org.us.x42.kyork.idcard.tasks.ReadCardTask;
import org.us.x42.kyork.idcard.tasks.WriteCardTask;

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
        startActivityForResult(intent1, REQ_READ_CARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IDCard card;
        switch (requestCode) {
            case REQ_READ_CARD:
                if (resultCode != RESULT_OK) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
                }
                ReadCardTask readTask = data.getParcelableExtra(CardReadActivity.RESULT_CARD);
                if (readTask.getCard() == null) {
                    Log.e(HexeditStartActivity.class.getSimpleName(), "ReadCardTask failed");
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
                }
                Intent intent2 = new Intent(this, HexeditEditorViewActivity.class);
                intent2.putExtra(HexeditEditorViewActivity.EDITOR_PARAMS_CARD, readTask.getCard());
                startActivityForResult(intent2, REQ_EDIT_CARD);
                break;
            case REQ_EDIT_CARD:
                if (resultCode != RESULT_OK) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
                }
                card = data.getParcelableExtra(CardReadActivity.RESULT_CARD);
                Intent intent3 = new Intent(this, CardWriteActivity.class);
                CardNFCTask task = new WriteCardTask(card);
                intent3.putExtra(CardWriteActivity.CARD_JOB_PARAMS, task);
                startActivityForResult(intent3, REQ_WRITE_CARD);
                break;
            case REQ_WRITE_CARD:
                setResult(resultCode, data);
                finish();
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
