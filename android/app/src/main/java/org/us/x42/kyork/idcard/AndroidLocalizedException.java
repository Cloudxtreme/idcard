package org.us.x42.kyork.idcard;

import android.content.Context;

/**
 * Interface for a {@link Throwable} that can localize its message on Android.
 */
public interface AndroidLocalizedException {
    String getLocalizedMessage(Context context);
}
