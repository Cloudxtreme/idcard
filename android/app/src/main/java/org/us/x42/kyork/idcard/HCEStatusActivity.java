package org.us.x42.kyork.idcard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class HCEStatusActivity extends AppCompatActivity {

    private static final String ACTION_STATUS_UPDATE = "org.us.x42.kyork.idcard.hce.Status";

    /**
     * @value int - a StatusCode value
     */
    private static final String EXTRA_HCE_STATE = "hce_state";
    private static final String EXTRA_HCE_MODE = "hce_mode";
    private static final String EXTRA_ERROR = "error";

    public enum StatusCode {
        /**
         * Talking with the server to get a ticket / update data
         */
        FETCHING_FROM_SERVER,
        /**
         * Talking with the card reader
         */
        COMMUNICATING_WITH_READER,
        /**
         * Operation complete
         */
        SUCCESS,
        /**
         * Data has been obtained and we're ready to talk to the reader (again?)
         */
        READY_FOR_READER,
        /**
         * The server rejected the request
         */
        SERVER_ERROR,
        /**
         * Contact with card reader was lost
         */
        READER_LOST,
        /**
         * Contact with card reader was lost, but we're still fetching something from the server
         */
        FETCHING_SERVER_READER_LOST,
        /**
         * App ID was deselected, activity should finish()
         */
        DESELECTED,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hce_status);

        IntentFilter filter = new IntentFilter(ACTION_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(new HCEUpdateReceiver(), filter);

        initializeUI(getIntent());
    }

    private class HCEUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateForIntent(intent);
        }
    }

    public static Intent newStatusIntent(Context context, StatusCode status) {
        Intent intent = new Intent(ACTION_STATUS_UPDATE);
        intent.putExtra(EXTRA_HCE_STATE, status.ordinal());
        return intent;
    }

    public static void addCardMode(Intent launchIntent, HCEService.HCEServiceUtils.OperationMode mode) {
        if (mode == HCEService.HCEServiceUtils.OperationMode.SERVER_UPDATE) {
            launchIntent.putExtra(EXTRA_HCE_MODE, true);
        } else {
            launchIntent.putExtra(EXTRA_HCE_MODE, false);
        }
    }

    public static void addServerErrorCode(Intent launchIntent, String errMsg) {
        launchIntent.putExtra(EXTRA_ERROR, errMsg);
    }

    public static void addStatusCode(Intent launchIntent, StatusCode status) {
        launchIntent.putExtra(EXTRA_HCE_STATE, status.ordinal());
    }

    @Override
    public void onNewIntent(Intent intent) {
        initializeUI(intent);
    }

    private void initializeUI(Intent intent) {

    }

    private void updateForIntent(Intent data) {
    }
}
