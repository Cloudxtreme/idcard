package org.us.x42.kyork.idcard;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import org.us.x42.kyork.idcard.ProgressStepListFragment.ProgressStepListFragmentInterface;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ProgressStep} and makes a call to the
 * specified {@link ProgressStepListFragmentInterface}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ProgressStepRecyclerViewAdapter extends RecyclerView.Adapter<ProgressStepRecyclerViewAdapter.ViewHolder> {
    private final List<ProgressStep> mValues;
    private final ProgressStepListFragment.ProgressStepListFragmentInterface mListener;

    public ProgressStepRecyclerViewAdapter(ProgressStepListFragment.ProgressStepListFragmentInterface listener) {
        mListener = listener;
        mValues = listener.getProgressStepList();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_progressstep, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mItem = null;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.updateState();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ProgressBar progressSpinnerView;
        public final ImageView successImageView;
        public final TextView mContentView;
        public ProgressStep mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            progressSpinnerView = (ProgressBar) view.findViewById(R.id.progressBar);
            successImageView = (ImageView) view.findViewById(R.id.imageView);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        public void updateState() {
            mContentView.setText(mItem.getText());
            switch (mItem.state) {
                case ProgressStep.STATE_INIT:
                    successImageView.setVisibility(View.INVISIBLE);
                    progressSpinnerView.setVisibility(View.INVISIBLE);
                    break;
                case ProgressStep.STATE_WORKING:
                    successImageView.setVisibility(View.INVISIBLE);
                    progressSpinnerView.setVisibility(View.VISIBLE);
                    break;
                case ProgressStep.STATE_DONE:
                    successImageView.setVisibility(View.VISIBLE);
                    progressSpinnerView.setVisibility(View.GONE);
                    break;
                case ProgressStep.STATE_FAIL:
                    successImageView.setVisibility(View.INVISIBLE);
                    progressSpinnerView.setVisibility(View.INVISIBLE);
                    break;
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
