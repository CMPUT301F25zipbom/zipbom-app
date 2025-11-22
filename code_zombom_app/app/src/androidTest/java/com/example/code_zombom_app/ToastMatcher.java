package com.example.code_zombom_app;

import android.os.IBinder;
import android.view.WindowManager;
import androidx.test.espresso.Root;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * A custom Hamcrest Matcher for testing Toast messages with Espresso.
 * Toasts are not part of the standard view hierarchy and require a custom root matcher to be found.
 */
public class ToastMatcher extends TypeSafeMatcher<Root> {

    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams().get().type;
        // Check if the window type is a Toast
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();
            // A toast window's token should be the same as its application token
            return windowToken == appToken;
        }
        return false;
    }
}
