package org.us.x42.kyork.idcard.hexedit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.IDCard;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class HexeditEditorViewActivity extends AppCompatActivity implements HexeditLineFragment.OnListFragmentInteractionListener {

    private IDCard card;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    public static final String EDITOR_PARAMS_CARD = "org.us.x42.kyork.idcard.hexedit.Card";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hexedit_editor_view);

        Intent launchIntent = getIntent();
        card = launchIntent.getParcelableExtra(EDITOR_PARAMS_CARD);
        if (card == null || card.fileMetadata == null) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hexedit_editor_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // no longer needed - the Activity now always has the card
    private Map<HexeditLineFragment, MyHexeditLineRecyclerViewAdapter> adapterMap = new HashMap<>();

    @Override
    public void registerAdapterCallbacks(HexeditLineFragment fragment, MyHexeditLineRecyclerViewAdapter adapter) {
        adapterMap.put(fragment, adapter);
        if (card != null) {
            adapter.switchEditTarget(card);
        }
    }

    @Override
    public void unregisterAdapterCallbacks(HexeditLineFragment fragment) {
        adapterMap.remove(fragment);
    }

    @Override
    public void notifyContentChanged(int fileID) {
        // TODO
    }

    @Override
    public Context getContext() {
        return this;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_FILE_ID = "file_id";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int fileID) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_FILE_ID, fileID);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_hexedit_editor_view, container, false);

            Log.i("PlaceholderFragment", "Was constructeD");

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            for (AbstractCardFile f : card.files()) {
                if (position == 0) {
                    return HexeditLineFragment.newInstance(f.getFileID());
                }
                position--;
            }
            throw new ArrayIndexOutOfBoundsException();
        }

        @Override
        public int getCount() {
            return card.files().size();
        }
    }
}
