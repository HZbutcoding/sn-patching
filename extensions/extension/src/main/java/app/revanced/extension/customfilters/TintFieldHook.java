package app.revanced.extension.customfilters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TintFieldHook {
    public static void installTintField(Object table) {
        try {
            // Try to show a toast first — to confirm the hook actually runs
            showToast("Custom filter hook triggered ✅");

            ClassLoader cl = table.getClass().getClassLoader();

            Class<?> appClass = Class.forName("org.fortheloss.sticknodes.App", false, cl);
            Class<?> colorClass = Class.forName("com.badlogic.gdx.graphics.Color", false, cl);
            Class<?> labelFieldClass = Class.forName("org.fortheloss.framework.LabelColorInputIncrementField", false, cl);
            Class<?> figureFiltersClass = Class.forName("org.fortheloss.sticknodes.animationscreen.modules.tooltables.FigureFiltersToolTable", false, cl);

            Constructor<?> ctor = labelFieldClass.getConstructor(
                    android.content.Context.class,
                    String.class, String.class, int.class,
                    float.class, float.class, boolean.class
            );

            Method getModule = figureFiltersClass.getMethod("getModule");
            Object module = getModule.invoke(table);
            Method getContext = module.getClass().getMethod("getContext");
            Object context = getContext.invoke(module);

            Method localize = appClass.getMethod("localize", String.class);
            String tintLabel = (String) localize.invoke(null, "tint");

            Object field = ctor.newInstance(context, tintLabel, "0.00", 4, 0.0f, 1.0f, true);

            Method registerWidget = figureFiltersClass.getMethod("registerWidget", Object.class, int.class);
            registerWidget.invoke(table, field, 107);

            Method setHighFidelity = labelFieldClass.getMethod("setHighFidelity", boolean.class);
            setHighFidelity.invoke(field, true);

            Object whiteColor = colorClass.getField("WHITE").get(null);
            Method setValue = labelFieldClass.getMethod("setValue", colorClass);
            setValue.invoke(field, whiteColor);

            System.out.println("[CustomFilters] Tint field added successfully!");

        } catch (Throwable t) {
            t.printStackTrace();
            showToast("❌ Custom filter hook failed: " + t.getClass().getSimpleName());
        }
    }

    // Show Toast using main thread
    private static void showToast(String message) {
        try {
            Context context = getAppContext();
            if (context == null) {
                System.out.println("[CustomFilters] No context available for Toast");
                return;
            }

            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        } catch (Throwable ignored) {
        }
    }

    // Universal app context getter
    private static Context getAppContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null);
            return (Context) activityThread.getMethod("getApplication").invoke(currentActivityThread);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
