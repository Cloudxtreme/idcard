package org.us.x42.kyork.idcard.hexedit;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.HexSpanInfo;
import org.us.x42.kyork.idcard.data.IDCard;

import java.util.List;
import java.util.Optional;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class HexeditLineFragment extends Fragment {

    private static final String ARG_FILE_ID = "fileid";
    private OnListFragmentInteractionListener mListener;
    private int mFileID;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HexeditLineFragment() {
    }

    @SuppressWarnings("unused")
    public static HexeditLineFragment newInstance(int fileID) {
        HexeditLineFragment fragment = new HexeditLineFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FILE_ID, fileID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mFileID = getArguments().getInt(ARG_FILE_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hexeditline_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            if (mFileID == 0) {
                Log.e("HexeditLineFragment", "ERROR - created without a fileID");
                return view;
            }
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            MyHexeditLineRecyclerViewAdapter adapter = new MyHexeditLineRecyclerViewAdapter(mListener, mFileID);
            recyclerView.setAdapter(adapter);
            mListener.registerAdapterCallbacks(this, adapter);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException("HexeditLineFragment: activity is not a proper fragment listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.unregisterAdapterCallbacks(this);
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void registerAdapterCallbacks(HexeditLineFragment fragment, MyHexeditLineRecyclerViewAdapter adapter);
        void unregisterAdapterCallbacks(HexeditLineFragment fragment);
        void notifyContentChanged(int fileID);
        Context getContext();
    }
}
