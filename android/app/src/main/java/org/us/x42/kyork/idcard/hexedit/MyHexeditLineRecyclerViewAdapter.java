package org.us.x42.kyork.idcard.hexedit;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.us.x42.kyork.idcard.HexUtil;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.HexSpanInfo;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.hexedit.HexeditLineFragment.OnListFragmentInteractionListener;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        mFile = card.getFileByID((byte) mFileID);
        mFile.describeHexSpanContents(mValues);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public enum EditorType {
        NONE,
        NUMBER,
        ENUMERATED,
        STRING
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        public final TextView mIdView;
        private final TextView mHexView;
        public HexSpanInfo.Interface mItem;
        private boolean isExpanded;
        private @Nullable Button mApplyButton;
        private Button mHexButton;
        private EditorType mType = EditorType.NONE;

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
            mHexButton = (Button) mView.findViewById(R.id.hexEditButton);
            mHexButton.setOnClickListener(this);

            ConstraintLayout frame;
            if (mItem instanceof HexSpanInfo.Enumerated) {
                mType = EditorType.ENUMERATED;
                HexSpanInfo.Enumerated enumeratedDescriptor = (HexSpanInfo.Enumerated) this.mItem;
                frame = (ConstraintLayout) mView.findViewById(R.id.enumFrame);

                mApplyButton = (Button) frame.findViewById(R.id.enumApply);
                mApplyButton.setOnClickListener(this);

                Spinner spinner = (Spinner) frame.findViewById(R.id.spinner);
                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(mContext.getContext(), android.R.layout.simple_spinner_dropdown_item);

                int currentByRsc = enumeratedDescriptor.getContentStringResource(mFile.getRawContent());
                int currentPosition = 0;
                adapter.add(mContext.getContext().getString(R.string.editor_other));

                // Add items to the dropdown, and identify index of current value
                List<Integer> possibleValues = enumeratedDescriptor.getPossibleValues();
                for (int i = 0; i < possibleValues.size(); i++) {
                    Integer stringRsc = possibleValues.get(i);
                    adapter.add(mContext.getContext().getString(stringRsc));
                    if (stringRsc == currentByRsc) {
                        currentPosition = i + 1;
                    }
                }

                spinner.setAdapter(adapter);
                spinner.setSelection(currentPosition);

            } else if (mItem instanceof HexSpanInfo.Numeric) {
                mType = EditorType.NUMBER;
                HexSpanInfo.Numeric numericDescriptor = (HexSpanInfo.Numeric) mItem;
                frame = (ConstraintLayout) mView.findViewById(R.id.numberFrame);

                mApplyButton = (Button) frame.findViewById(R.id.numberApply);
                mApplyButton.setOnClickListener(this);

                EditText numericInput = frame.findViewById(R.id.numberInput);
                long curValue = numericDescriptor.getValue(mFile.getRawContent());
                String curString = NumberFormat.getIntegerInstance(Locale.getDefault()).format(curValue);
                numericInput.setText(curString);
            } else if (mItem instanceof HexSpanInfo.Stringish) {
                mType = EditorType.STRING;
            } else {
                mType = EditorType.NONE;
            }

            setFieldVisibility();
        }

        @Override
        public void onClick(View clickedView) {
            Log.i("HexeditLineViewHolder", "onClick " + clickedView.getClass().getName());
            if (clickedView == mApplyButton) {
                onClickApply();
            } else if (clickedView == mHexButton) {
                onClickHex();
            } else {
                onClickBody();
            }
        }

        private void onClickBody() {
            isExpanded = !isExpanded;
            ConstraintLayout container = mView.findViewById(R.id.container);
            TransitionManager.beginDelayedTransition(container); // this is a really good line
            setFieldVisibility();
        }

        private void onClickApply() {
            ConstraintLayout frame;
            CharSequence errString;
            // true if the mFile contents were edited
            boolean didEdit = false;

            switch (mType) {
                case NONE:
                    break;
                case NUMBER:
                    frame = (ConstraintLayout) mView.findViewById(R.id.numberFrame);
                    HexSpanInfo.Numeric numericDescriptor = (HexSpanInfo.Numeric) mItem;
                    EditText numberInput = frame.findViewById(R.id.numberInput);
                    try {
                        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());
                        numberFormat.setParseIntegerOnly(true);
                        Number newValue = numberFormat.parse(numberInput.getText().toString());

                        errString = numericDescriptor.checkValue(mContext.getContext(), newValue.longValue());
                        if (errString != null) {
                            numberInput.setError(errString);
                            didEdit = false;
                            break;
                        }

                        numericDescriptor.setValue(mFile.getRawContent(), newValue.longValue());
                        didEdit = true;
                        mFile.setDirty();

                    } catch (ParseException nfe) {
                        numberInput.setError(nfe.getLocalizedMessage());
                        didEdit = false;
                        break;
                    }
                    break;
                case ENUMERATED:
                    frame = (ConstraintLayout) mView.findViewById(R.id.enumFrame);
                    HexSpanInfo.Enumerated enumeratedDescriptor = (HexSpanInfo.Enumerated) this.mItem;
                    Spinner enumInput = frame.findViewById(R.id.spinner);

                    int pos = enumInput.getSelectedItemPosition();
                    if (pos == AdapterView.INVALID_POSITION || pos == 0 /* (Other) */) {
                        didEdit = false;
                        break;
                    }
                    pos = pos - 1; // un-offset for the 0=Other value
                    try {
                        int selectedStringRsc = enumeratedDescriptor.getPossibleValues().get(pos);
                        enumeratedDescriptor.setContentByStringResource(mFile.getRawContent(), selectedStringRsc);
                        didEdit = true;
                    } catch (IndexOutOfBoundsException e) {
                        didEdit = false;
                        break;
                    }

                    break;
                case STRING:
                    frame = null;
                    HexSpanInfo.Stringish stringishDescriptor = (HexSpanInfo.Stringish) this.mItem;

                    errString = stringishDescriptor.checkValue(mContext.getContext(), "");
                    if (errString != null) {
                        // setError();
                        didEdit = false;
                        break;
                    }
                    break;
            }

            if (didEdit) {
                mFile.setDirty();
                this.changeItem(mItem);
                // mContext.notifyContentChanged(mFileID);
            }
            Log.i(LOG_TAG, "clicked Apply button");
        }

        private void onClickHex() {
            AlertDialog.Builder b = new AlertDialog.Builder(mContext.getContext());
            final EditText input = new EditText(mContext.getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            input.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            input.setText(HexUtil.encodeHexLineWrapped(mFile.getRawContent(), mItem.getOffset(), mItem.getOffset() + mItem.getLength()),
                    EditText.BufferType.EDITABLE);

            b.setTitle(R.string.editor_hexdialog_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.editor_applyvalue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                byte[] newContent = HexUtil.decodeUserInput(input.getText().toString());
                                if (newContent.length != mItem.getLength()) {
                                    input.setError(mContext.getContext().getString(R.string.editor_err_input_wrong_size,
                                            mItem.getLength(), newContent.length));
                                    return;
                                }
                                mItem.setRawContents(mFile.getRawContent(), newContent);
                                mFile.setDirty();
                                ViewHolder.this.changeItem(mItem);

                                dialogInterface.dismiss();
                            } catch (HexUtil.DecodeException e) {
                                input.setError(e.getLocalizedMessage(mContext.getContext()));
                            }
                        }
                    });

            b.setView(input);
            b.create().show();
            Log.i(LOG_TAG, "clicked Hex view");
        }

        private void setFieldVisibility() {
            TextView subHead = mView.findViewById(R.id.subheading);
            if (isExpanded) {
                // only visible while collapsed
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
    }
}
