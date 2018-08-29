package org.us.x42.kyork.idcard;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

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
        public final ViewSwitcher animator;
        public final TextView mContentView;
        public ProgressStep mItem;
        private FrameLayout groupA;
        private FrameLayout groupB;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            animator = view.findViewById(R.id.iconSwitcher);
            mContentView = (TextView) view.findViewById(R.id.content);
            groupA = new FrameLayout(getContext());
            groupB = new FrameLayout(getContext());
            LayoutInflater.from(getContext()).inflate(R.layout.progressstep_icon_blank, groupA);
            animator.addView(groupA, 0);
            animator.addView(groupB, 1);
        }

        public void updateState() {
            mContentView.setText(mItem.getText());
            groupB.removeAllViews();
            switch (mItem.state) {
                case ProgressStep.STATE_INIT:
                    LayoutInflater.from(getContext()).inflate(R.layout.progressstep_icon_blank, groupB);
                    break;
                case ProgressStep.STATE_WORKING:
                    LayoutInflater.from(getContext()).inflate(R.layout.progressstep_icon_working, groupB);
                    break;
                case ProgressStep.STATE_DONE:
                    LayoutInflater.from(getContext()).inflate(R.layout.progressstep_icon_success, groupB);
                    break;
                case ProgressStep.STATE_FAIL:
                    LayoutInflater.from(getContext()).inflate(R.layout.progressstep_icon_error, groupB);
                    break;
            }
            animator.showNext();
            FrameLayout tmp = groupA;
            groupA = groupB;
            groupB = tmp;
        }

        private Context getContext() { return (Context)mListener; }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
