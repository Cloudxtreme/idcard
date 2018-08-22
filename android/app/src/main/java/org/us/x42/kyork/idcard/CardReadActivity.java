package org.us.x42.kyork.idcard;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.us.x42.kyork.idcard.tasks.CardNFCTask;
import org.us.x42.kyork.idcard.tasks.ReadCardTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CardReadActivity extends AppCompatActivity implements ProgressStepListFragment.ProgressStepListFragmentInterface {
    private static final String LOG_TAG = CardWriteActivity.class.getSimpleName();
    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private CardNFCTask mTask;

    private Handler mHandler;

    private List<ProgressStep> progressSteps = new ArrayList<>();
    private ProgressStepRecyclerViewAdapter mProgressFragment;

    public static final String RESULT_CARD = "org.us.x42.kyork.idcard.CardReadActivity.cardResult";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        scanIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        scanFilter = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
        scanTechs = new String[][] { new String[] { IsoDep.class.getName(), NfcA.class.getName() } };

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CardNFCTask.MSG_ID_NFC_STATUS) {
                    progressSteps.get(msg.arg1).state = msg.arg2;
                    mProgressFragment.notifyItemChanged(msg.arg1);
                } else if (msg.what == CardNFCTask.MSG_ID_NFC_DONE) {
                    // Operation complete
                    Intent returnData = new Intent(Intent.ACTION_VIEW);
                    returnData.putExtra(RESULT_CARD, mTask);
                    setResult(RESULT_OK, returnData);
                    finish();
                }
            }
        };

        mTask = new ReadCardTask();
        mTask.writeListOfSteps(progressSteps);
        if (mProgressFragment != null) {
            mProgressFragment.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, scanIntent, scanFilter, scanTechs);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Objects.equals(intent.getAction(), NfcAdapter.ACTION_TECH_DISCOVERED)) {
            // is an NDEF tag
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            Log.i(LOG_TAG, "got tag");

            mTask.setTagAndHandler(tagFromIntent, mHandler);
            mTask.execute();
        }
    }

    @Override
    public @NonNull List<ProgressStep> getProgressStepList() {
        return this.progressSteps;
    }

    @Override
    public void attachFragmentListeners(ProgressStepRecyclerViewAdapter adapter) {
        this.mProgressFragment = adapter;
    }
}
