package org.us.x42.kyork.idcard.hexedit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.us.x42.kyork.idcard.HexUtil;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.HexSpanInfo;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.hexedit.HexeditLineFragment.OnListFragmentInteractionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link HexSpanInfo.Interface} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyHexeditLineRecyclerViewAdapter extends RecyclerView.Adapter<MyHexeditLineRecyclerViewAdapter.ViewHolder> {
    private static final String LOG_TAG = MyHexeditLineRecyclerViewAdapter.class.getSimpleName();

    private final OnListFragmentInteractionListener mContext;
    private final List<HexSpanInfo.Interface> mValues;
    private final int mFileID;
    private AbstractCardFile mFile;

    public MyHexeditLineRecyclerViewAdapter(OnListFragmentInteractionListener context, int fileID) {
        Log.i("HexeditLineRecyclerView", "file id " + fileID);
        mContext = context;
        mValues = new ArrayList<>();
        mFileID = fileID;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_hexeditline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.changeItem(mValues.get(position));

        holder.mView.setOnClickListener(holder);
        holder.mHexView.setOnClickListener(holder);
    }

    public void switchEditTarget(IDCard card) {
        mFile = card.getFileByID((byte)mFileID);
        mFile.describeHexSpanContents(mValues);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        public final TextView mIdView;
        private final TextView mHexView;
        public HexSpanInfo.Interface mItem;
        private boolean isExpanded;
        private Button mApplyButton;
        private Button mHexButton;

        // TODO saved instance state

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mView.setClickable(true);
            mIdView = (TextView) view.findViewById(R.id.title);
            mHexView = (TextView) mView.findViewById(R.id.hexView);
            mHexButton = (Button) mView.findViewById(R.id.hexEditButton);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mIdView.getText() + "'";
        }

        public void changeItem(HexSpanInfo.Interface item) {
            mItem = item;
            this.mIdView.setText(item.getFieldName());

            TextView subHead = mView.findViewById(R.id.subheading);
            subHead.setText(item.getShortContents(mContext.getContext(), mFile.getRawContent()));

            mHexView.setText(HexUtil.encodeHexLineWrapped(mFile.getRawContent(), mItem.getOffset(), mItem.getOffset() + mItem.getLength()));

            ConstraintLayout frame;
            if (mItem instanceof HexSpanInfo.Enumerated) {
                frame = (ConstraintLayout) mView.findViewById(R.id.enumFrame);
                mApplyButton = (Button) frame.findViewById(R.id.enumApply);
                Spinner spinner = (Spinner) frame.findViewById(R.id.spinner);

                mApplyButton.setOnClickListener(this);

                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(mContext.getContext(), android.R.layout.simple_spinner_item);

                for (Integer stringRsc : ((HexSpanInfo.Enumerated) mItem).getPossibleValues()) {
                    adapter.add(mContext.getContext().getString(stringRsc));
                }
                spinner.setAdapter(adapter);

            } else if (mItem instanceof HexSpanInfo.Numeric) {
                frame = (ConstraintLayout) mView.findViewById(R.id.numberFrame);
                mApplyButton = (Button) frame.findViewById(R.id.numberApply);
            } else {
                // no buttons
            }
        }

        @Override
        public void onClick(View clickedView) {
            if (clickedView == mApplyButton) {
                onClickApply();
            } else if (clickedView == mHexView) {
                onClickHex();
            } else {
                onClickBody();
            }
        }

        public void onClickBody() {
            isExpanded = !isExpanded;
            ConstraintLayout container = mView.findViewById(R.id.container);
            TransitionManager.beginDelayedTransition(container); // this is a really good line

            // only visible while collapsed
            TextView subHead = mView.findViewById(R.id.subheading);
            if (isExpanded) {
                subHead.setVisibility(View.GONE);
            } else {
                subHead.setVisibility(View.VISIBLE);
            }

            if (isExpanded) {
                mHexView.setVisibility(View.VISIBLE);
                mHexButton.setVisibility(View.VISIBLE);
            } else {
                mHexView.setVisibility(View.GONE);
                mHexButton.setVisibility(View.GONE);
            }

            ConstraintLayout frame;
            frame = (ConstraintLayout) mView.findViewById(R.id.enumFrame);
            if (isExpanded && mItem instanceof HexSpanInfo.Enumerated) {
                frame.setVisibility(View.VISIBLE);
            } else {
                frame.setVisibility(View.GONE);
            }
            frame = (ConstraintLayout) mView.findViewById(R.id.numberFrame);
            if (isExpanded && mItem instanceof HexSpanInfo.Numeric) {
                frame.setVisibility(View.VISIBLE);
            } else {
                frame.setVisibility(View.GONE);
            }
        }

        public void onClickApply() {
            Log.i(LOG_TAG, "clicked Apply button");
        }

        public void onClickHex() {
            Log.i(LOG_TAG, "clicked Hex view");
        }
    }
}
