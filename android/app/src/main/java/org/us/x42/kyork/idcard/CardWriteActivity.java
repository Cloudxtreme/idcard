package org.us.x42.kyork.idcard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
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
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.tasks.CardNFCTask;
import org.us.x42.kyork.idcard.tasks.WriteCardTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CardWriteActivity extends AppCompatActivity implements ProgressStepListFragment.ProgressStepListFragmentInterface {
    private static final String LOG_TAG = CardWriteActivity.class.getSimpleName();
    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private CardNFCTask mTask;

    private Handler mHandler;

    private List<ProgressStep> progressSteps = new ArrayList<>();
    private ProgressStepRecyclerViewAdapter mProgressFragment;

    /**
     * A CardNFCTask instance. Both required to start the CardWriteActivity and returned on completion.
     */
    public static final String CARD_JOB_PARAMS = "org.us.x42.kyork.idcard.CARD_JOB_PARAMS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        scanIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        scanFilter = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
        scanTechs = new String[][]{new String[]{IsoDep.class.getName(), NfcA.class.getName()}};

        mHandler = new Handler(Looper.getMainLooper()) {
            private boolean anyErrors;
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CardNFCTask.MSG_ID_NFC_STATUS) {
                    progressSteps.get(msg.arg1).state = msg.arg2;
                    mProgressFragment.notifyItemChanged(msg.arg1);
                    if (msg.arg2 == ProgressStep.STATE_FAIL) {
                        anyErrors = true;
                    }
                } else if (msg.what == CardNFCTask.MSG_ID_NFC_DONE) {
                    // Operation complete
                    Intent returnData = new Intent(Intent.ACTION_VIEW);
                    returnData.putExtra(CARD_JOB_PARAMS, mTask);
                    setResult(RESULT_OK, returnData);
                    if (anyErrors) {
                        mHandler.postDelayed(CardWriteActivity.this::finish, 1000);
                    } else {
                        finish();
                    }
                }
            }
        };

        Intent launchIntent = getIntent();
        mTask = launchIntent.getParcelableExtra(CARD_JOB_PARAMS);
        if (mTask == null) {
            Log.e("CardWriteActivity", "no CARD_DATA extra");
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        mTask.writeListOfSteps(progressSteps);
        if (mProgressFragment != null) {
            mProgressFragment.notifyDataSetChanged();
        }
    }

    /**
     * Create an intent to launch CardWriteActivity with a given {@link CardNFCTask}.
     * @param context The caller activity.
     * @param task The task to perform with the card.
     * @return An Intent that can be passed to {@link #startActivityForResult(Intent, int)}.
     */
    public static Intent getIntent(Context context, CardNFCTask task) {
        Intent intent = new Intent(context, CardWriteActivity.class);
        intent.putExtra(CARD_JOB_PARAMS, task);
        return intent;
    }

    /**
     * Get the CardNFCTask with results filled in from a returned intent.
     * @param resultIntent Intent obtained from {@link #onActivityResult(int, int, Intent)}
     * @param <T> The subclass of CardNFCTask used to launch the CardWriteActivity.
     * @return The CardNFCTask
     */
    public static <T extends CardNFCTask> T getResultData(Intent resultIntent) {
        return resultIntent.getParcelableExtra(CARD_JOB_PARAMS);
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
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

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
