package org.us.x42.kyork.idcard;

import android.content.ComponentName;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.tasks.ProvisionBlankCardTask;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ProgressStepListFragment.ProgressStepListFragmentInterface {

    private static final int REQUEST_ID_PROVISION = 4;

    private @NonNull List<ProgressStep> progressSteps;
    private ProgressStepRecyclerViewAdapter mProgressFragment;

    public MainActivity() {
        progressSteps = ImmutableList.of(
                new ProgressStep(R.string.editor_sig_fil1),
                new ProgressStep(R.string.editor_sig_fil2),
                new ProgressStep(R.string.editor_sig_fil3),
                new ProgressStep.WithDoneText(R.string.editor_user_act, R.string.editor_user_act_student, R.string.editor_user_act_piscine)
                );
    }

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
                Intent intent = new Intent(MainActivity.this, CardAdminActivity.class);
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

        Button provisionBlank = (Button) findViewById(R.id.launch_provisioning);
        provisionBlank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CardWriteActivity.class);
                EditText loginTextbox = (EditText) findViewById(R.id.login_textbox);
                String login = loginTextbox.getText().toString().toLowerCase();
                ProvisionBlankCardTask task = new ProvisionBlankCardTask(login, false);
                intent.putExtra(CardWriteActivity.CARD_JOB_PARAMS, task);
                startActivityForResult(intent, REQUEST_ID_PROVISION);
            }
        });

        findViewById(R.id.test_hce).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgress(0, ProgressStep.STATE_DONE);
                setProgress(1, ProgressStep.STATE_WORKING);
                setProgress(2, ProgressStep.STATE_FAIL);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CardEmulation emu = CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(MainActivity.this));
                    ComponentName hceService = new ComponentName(MainActivity.this, HCEService.class);
                    emu.registerAidsForService(hceService, CardEmulation.CATEGORY_OTHER, ImmutableList.of(CardJob.ISO_APPID_CARD42));

                    boolean result = emu.isDefaultServiceForAid(hceService, CardJob.ISO_APPID_CARD42);
                    if (result) {
                        Toast.makeText(MainActivity.this, "is default handler", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "is NOT default handler", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "too old for HCE support", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    protected void setProgress(int idx, int progressState) {
        progressSteps.get(idx).state = progressState;
        mProgressFragment.notifyItemChanged(idx);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ID_PROVISION) {
            if (resultCode == RESULT_OK) {
                ProvisionBlankCardTask result = data.getParcelableExtra(CardWriteActivity.CARD_JOB_PARAMS);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.provision_result_title);
                if (result.getErrorString() == null || result.getErrorString().isEmpty()) {
                    builder.setMessage(R.string.provision_result_success);
                } else {
                    builder.setMessage("Error: " + result.getErrorString());
                }
                builder.create().show();
            }
        }
    }

    @Override
    public @NonNull List<ProgressStep> getProgressStepList() {
        return progressSteps;
    }

    @Override
    public void attachFragmentListeners(ProgressStepRecyclerViewAdapter frag) {
        mProgressFragment = frag;
    }
}
