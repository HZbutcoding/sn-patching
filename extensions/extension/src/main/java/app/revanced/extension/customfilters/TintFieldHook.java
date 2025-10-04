package app.revanced.extension.customfilters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TintFieldHook {
    public static void installTintField(Object table) {
        try {
            Log.d("ReVancedCustomFilter", "installTintField called!");

            ClassLoader cl = table.getClass().getClassLoader();

            // Get FigureFiltersToolTable module + context
            Class<?> figureFiltersClass = Class.forName(
                    "org.fortheloss.sticknodes.animationscreen.modules.tooltables.FigureFiltersToolTable",
                    false,
                    cl
            );
            Method getModule = figureFiltersClass.getMethod("getModule");
            Object module = getModule.invoke(table);
            Method getContext = module.getClass().getMethod("getContext");
            Context context = (Context) getContext.invoke(module);

            // ✅ Show a Toast to confirm the hook actually ran
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "ReVanced filter patch active!", Toast.LENGTH_LONG).show()
            );

            // Continue with your original code
            Class<?> appClass = Class.forName("org.fortheloss.sticknodes.App", false, cl);
            Class<?> colorClass = Class.forName("com.badlogic.gdx.graphics.Color", false, cl);
            Class<?> labelFieldClass = Class.forName("org.fortheloss.framework.LabelColorInputIncrementField", false, cl);

            Constructor<?> ctor = labelFieldClass.getConstructor(
                    Context.class,
                    String.class, String.class, int.class,
                    float.class, float.class, boolean.class
            );

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

            Log.d("ReVancedCustomFilter", "installTintField completed successfully!");

        } catch (Throwable t) {
            Log.e("ReVancedCustomFilter", "Error in installTintField", t);
            t.printStackTrace();
        }
    }
}
