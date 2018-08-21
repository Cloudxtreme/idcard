package org.us.x42.kyork.idcard;

import android.support.annotation.StringRes;

public class ProgressStep {
    public @StringRes int text;
    public int state;

    /**
     * Nothing is shown.
     */
    public static final int STATE_INIT = 0;
    /**
     * A progress bar spinner is shown.
     */
    public static final int STATE_WORKING = 1;
    /**
     * A green checkmark is shown.
     */
    public static final int STATE_DONE = 2;
    /**
     * A red cross is shown.
     */
    public static final int STATE_FAIL = 3;

    public ProgressStep(@StringRes int text) {
        this.text = text;
        this.state = STATE_INIT;
    }

    public @StringRes int getText() {
        return text;
    }

    public static class WithDoneText extends ProgressStep {
        public @StringRes int doneText;
        public @StringRes int errText;


        public WithDoneText(@StringRes int text, @StringRes int doneText, @StringRes int errText) {
            super(text);
            this.doneText = doneText;
            this.errText = errText;
        }

        @Override
        public @StringRes int getText() {
            switch (this.state) {
                case STATE_INIT:
                case STATE_WORKING:
                    return text;
                case STATE_DONE:
                    return doneText;
                case STATE_FAIL:
                    return errText;
            }
            return errText;
        }
    }
}
