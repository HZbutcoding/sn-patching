package app.revanced.extension.customfilters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Hook class that adds a custom tint filter field to FigureFiltersToolTable
 * Injected via Revanced Patcher into initialize() method
 */
public class TintFieldHook {
    private static final String TAG = "CustomFilterHook";
    private static boolean installed = false;

    /**
     * Schedule a safe install after initialization.
     * Call this from your smali injection.
     */
    public static void scheduleInstall(final Object toolTable) {
        if (installed) {
            Log.w(TAG, "Already installed, skipping");
            return;
        }
        installed = true;

        Log.i(TAG, "=== scheduleInstall() called ===");
        Log.i(TAG, "Thread: " + Thread.currentThread().getName());
        Log.i(TAG, "Table class: " + (toolTable != null ? toolTable.getClass().getName() : "NULL"));

        // Ensure this runs on the main thread after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                installCustomTintSlot(toolTable);
            }
        }, 500);
    }

    /**
     * Main installation method - creates and adds the custom tint filter slot
     */
    public static void installCustomTintSlot(Object table) {
        Log.i(TAG, "=== installCustomTintSlot() starting ===");

        try {
            ClassLoader cl = table.getClass().getClassLoader();
            if (cl == null) {
                Log.e(TAG, "ClassLoader is null!");
                return;
            }

            // ========================================
            // Step 1: Load all required classes
            // ========================================
            Log.i(TAG, "Loading classes...");

            Class<?> figureFiltersClass = Class.forName(
                    "org.fortheloss.sticknodes.animationscreen.modules.tooltables.FigureFiltersToolTable",
                    false, cl
            );
            Class<?> labelColorInputClass = Class.forName(
                    "org.fortheloss.framework.LabelColorInputIncrementField",
                    false, cl
            );
            Class<?> animationScreenClass = Class.forName(
                    "org.fortheloss.sticknodes.animationscreen.AnimationScreen",
                    false, cl
            );
            Class<?> colorClass = Class.forName(
                    "com.badlogic.gdx.graphics.Color",
                    false, cl
            );
            Class<?> tableClass = Class.forName(
                    "com.badlogic.gdx.scenes.scene2d.ui.Table",
                    false, cl
            );
            Class<?> cellClass = Class.forName(
                    "com.badlogic.gdx.scenes.scene2d.ui.Cell",
                    false, cl
            );

            Log.i(TAG, "✓ All classes loaded successfully");

            // ========================================
            // Step 2: Get AnimationScreen context
            // ========================================
            Log.i(TAG, "Getting AnimationScreen context...");

            Method getModule = figureFiltersClass.getMethod("getModule");
            Object module = getModule.invoke(table);

            Method getContext = module.getClass().getMethod("getContext");
            Object animationScreen = getContext.invoke(module);

            Log.i(TAG, "✓ AnimationScreen context retrieved: " + animationScreen.getClass().getName());

            // ========================================
            // Step 3: Get localized label text
            // ========================================
            Log.i(TAG, "Getting localized label...");

            Class<?> appClass = Class.forName("org.fortheloss.sticknodes.App", false, cl);
            Method localize = appClass.getMethod("localize", String.class);
            String labelText = (String) localize.invoke(null, "tint"); // Use "tint" key

            Log.i(TAG, "✓ Label text: " + labelText);

            // ========================================
            // Step 4: Create LabelColorInputIncrementField instance
            // ========================================
            Log.i(TAG, "Creating field instance...");

            // Constructor signature: <init>(AnimationScreen, String, String, I, F, F, Z)
            Constructor<?> ctor = labelColorInputClass.getDeclaredConstructor(
                    animationScreenClass,
                    String.class,    // label text
                    String.class,    // default string
                    int.class,       // precision
                    float.class,     // min
                    float.class,     // max
                    boolean.class    // boolean flag
            );
            ctor.setAccessible(true);

            // Create instance with parameters:
            // - animationScreen: context
            // - labelText: localized "tint"
            // - "0.00": default value string
            // - 4: precision (decimal places)
            // - 0.0f: minimum value
            // - 1.0f: maximum value
            // - true: boolean flag (exact purpose varies, typically "enabled")
            Object fieldInstance = ctor.newInstance(
                    animationScreen,
                    labelText,
                    "0.00",
                    4,
                    0.0f,
                    1.0f,
                    true
            );

            Log.i(TAG, "✓ Field instance created");

            // ========================================
            // Step 5: Register the widget with ID 108
            // ========================================
            Log.i(TAG, "Registering widget...");

            Method registerWidget = figureFiltersClass.getMethod("registerWidget", Object.class, int.class);
            registerWidget.invoke(table, fieldInstance, 108); // ID 108 (0x6C)

            Log.i(TAG, "✓ Widget registered with ID 108");

            // ========================================
            // Step 6: Configure field properties
            // ========================================
            Log.i(TAG, "Configuring field properties...");

            // Set high fidelity mode
            Method setHigh = labelColorInputClass.getMethod("setHighFidelity", boolean.class);
            setHigh.invoke(fieldInstance, true);

            // Set initial value to WHITE
            Field whiteField = colorClass.getField("WHITE");
            Object whiteColor = whiteField.get(null);
            Method setValue = labelColorInputClass.getMethod("setValue", colorClass);
            setValue.invoke(fieldInstance, whiteColor);

            Log.i(TAG, "✓ Field configured (high fidelity, white color)");

            // ========================================
            // Step 7: Add to mFiltersSubTable
            // ========================================
            Log.i(TAG, "Adding to filters table...");

            Field filtersSubTableField = figureFiltersClass.getDeclaredField("mFiltersSubTable");
            filtersSubTableField.setAccessible(true);
            Object filtersSubTable = filtersSubTableField.get(table);

            // Add to table and get Cell
            Method tableAdd = tableClass.getMethod("add", Object.class);
            Object cell = tableAdd.invoke(filtersSubTable, fieldInstance);

            // Configure Cell layout: colspan(2).fillX()
            try {
                Method colspan = cellClass.getMethod("colspan", int.class);
                Object cellAfterColspan = colspan.invoke(cell, 2);

                Method fillX = cellClass.getMethod("fillX");
                fillX.invoke(cellAfterColspan);

                Log.i(TAG, "✓ Cell configured (colspan=2, fillX)");
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "Could not configure Cell layout: " + e.getMessage());
            }

            // Add row separator
            Method tableRow = tableClass.getMethod("row");
            tableRow.invoke(filtersSubTable);

            Log.i(TAG, "✓ Added to filters table");

            // ========================================
            // Step 8: Create and set field listener
            // ========================================
            Log.i(TAG, "Creating field listener...");

            Class<?> listenerInterface = Class.forName(
                    "org.fortheloss.framework.LabelColorInputIncrementField$ColorFieldListener",
                    false, cl
            );

            Object listenerProxy = createFieldListener(
                    cl,
                    listenerInterface,
                    table,
                    figureFiltersClass,
                    colorClass
            );

            // Set the listener
            Class<?> baseFieldListenerClass = Class.forName(
                    "org.fortheloss.framework.LabelInputIncrementField$FieldListener",
                    false, cl
            );
            Method setFieldListener = labelColorInputClass.getMethod("setFieldListener", baseFieldListenerClass);
            setFieldListener.invoke(fieldInstance, listenerProxy);

            Log.i(TAG, "✓ Field listener attached");

            // ========================================
            // Step 9: Success!
            // ========================================
            Log.i(TAG, "=== Custom tint filter installed successfully! ===");

            // Show toast notification
            showToast(animationScreen, "Custom tint filter added!");

        } catch (Throwable t) {
            Log.e(TAG, "Failed to install custom tint slot", t);
            t.printStackTrace();
        }
    }

    /**
     * Creates a dynamic proxy for the ColorFieldListener interface
     */
    private static Object createFieldListener(
            ClassLoader cl,
            Class<?> listenerInterface,
            Object table,
            Class<?> figureFiltersClass,
            Class<?> colorClass
    ) throws Exception {

        return Proxy.newProxyInstance(
                cl,
                new Class<?>[]{listenerInterface},
                new InvocationHandler() {
                    // Cache reflective lookups for performance
                    Method redrawModuleMethod = null;
                    Field animationBasedModuleRefField = null;
                    Method setAmountMethod = null;
                    Method setColorMethod = null;

                    {
                        try {
                            // Get redrawModule() method
                            redrawModuleMethod = figureFiltersClass.getMethod("redrawModule");
                            Log.i(TAG, "  ✓ Found redrawModule()");
                        } catch (Exception e) {
                            Log.w(TAG, "  ✗ Could not find redrawModule(): " + e.getMessage());
                        }

                        try {
                            // Get animation module reference field
                            animationBasedModuleRefField = figureFiltersClass.getDeclaredField("mAnimationBasedModuleRef");
                            animationBasedModuleRefField.setAccessible(true);

                            Class<?> animBasedModuleClass = animationBasedModuleRefField.getType();

                            // Get methods on animation module
                            try {
                                setAmountMethod = animBasedModuleClass.getMethod("setFigureTintAmountTo", float.class);
                                Log.i(TAG, "  ✓ Found setFigureTintAmountTo()");
                            } catch (NoSuchMethodException e) {
                                Log.w(TAG, "  ✗ Could not find setFigureTintAmountTo()");
                            }

                            try {
                                setColorMethod = animBasedModuleClass.getMethod("setFigureTintColor", colorClass);
                                Log.i(TAG, "  ✓ Found setFigureTintColor()");
                            } catch (NoSuchMethodException e) {
                                Log.w(TAG, "  ✗ Could not find setFigureTintColor()");
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "  ✗ Could not access animation module: " + e.getMessage());
                        }
                    }

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();

                        try {
                            if ("onTextFieldTouchEvent".equals(methodName)) {
                                // User touched the field - redraw module
                                Log.d(TAG, "Listener: onTextFieldTouchEvent");
                                if (redrawModuleMethod != null) {
                                    redrawModuleMethod.invoke(table);
                                }
                                return null;

                            } else if ("onTextFieldChange".equals(methodName)) {
                                // Value changed: args[0] = float value, args[1] = boolean isFinal
                                float value = ((Number) args[0]).floatValue();
                                boolean isFinal = args.length > 1 ? (Boolean) args[1] : false;

                                Log.d(TAG, "Listener: onTextFieldChange value=" + value + " final=" + isFinal);

                                // Redraw module
                                if (redrawModuleMethod != null) {
                                    redrawModuleMethod.invoke(table);
                                }

                                // Set tint amount on animation module
                                if (animationBasedModuleRefField != null && setAmountMethod != null) {
                                    Object animRef = animationBasedModuleRefField.get(table);
                                    if (animRef != null) {
                                        setAmountMethod.invoke(animRef, value);
                                    }
                                }
                                return null;

                            } else if ("onColorChange".equals(methodName)) {
                                // Color changed: args[0] = Color
                                Object colorArg = args[0];

                                Log.d(TAG, "Listener: onColorChange color=" + colorArg);

                                // Set tint color on animation module
                                if (animationBasedModuleRefField != null && setColorMethod != null && colorArg != null) {
                                    Object animRef = animationBasedModuleRefField.get(table);
                                    if (animRef != null) {
                                        setColorMethod.invoke(animRef, colorArg);
                                    }
                                }
                                return null;
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Error in listener callback: " + methodName, t);
                            t.printStackTrace();
                        }

                        return null;
                    }
                }
        );
    }

    /**
     * Shows a toast notification
     */
    private static void showToast(Object animationScreen, String message) {
        try {
            Context ctx = null;

            if (animationScreen instanceof Context) {
                ctx = (Context) animationScreen;
            }

            if (ctx != null) {
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Could not show toast - no Android Context available");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error showing toast", t);
        }
    }
}
//public class TintFieldHook {
//    public static void installCustomTintSlot(Object table) {
//        try {
//            ClassLoader cl = table.getClass().getClassLoader();
//
//            // Core classes
//            Class<?> figureFiltersClass = Class.forName("org.fortheloss.sticknodes.animationscreen.modules.tooltables.FigureFiltersToolTable", false, cl);
//            Class<?> labelColorInputClass = Class.forName("org.fortheloss.framework.LabelColorInputIncrementField", false, cl);
//            Class<?> animationScreenClass = Class.forName("org.fortheloss.sticknodes.animationscreen.AnimationScreen", false, cl);
//            Class<?> colorClass = Class.forName("com.badlogic.gdx.graphics.Color", false, cl);
//            Class<?> tableClass = Class.forName("com.badlogic.gdx.scenes.scene2d.ui.Table", false, cl);
//            Class<?> cellClass = Class.forName("com.badlogic.gdx.scenes.scene2d.ui.Cell", false, cl);
//            Class<?> moduleClass = Class.forName("org.fortheloss.sticknodes.animationscreen.modules.Module", false, cl);
//
//            // Get module/context
//            Method getModule = figureFiltersClass.getMethod("getModule");
//            Object module = getModule.invoke(table);
//            Method getContext = module.getClass().getMethod("getContext");
//            Object animationScreen = getContext.invoke(module); // AnimationScreen instance, used by ctor
//
//            // Localization helper: App.localize("tint")
//            Class<?> appClass = Class.forName("org.fortheloss.sticknodes.App", false, cl);
//            Method localize = appClass.getMethod("localize", String.class);
//            String labelText = (String) localize.invoke(null, "custom"); // use "custom" key; replace if you want "tint"
//
//            // Constructor for LabelColorInputIncrementField
//            // Smali showed signature: <init>(AnimationScreen, String, String, I, F, F, Z)
//            Constructor<?> ctor = labelColorInputClass.getDeclaredConstructor(
//                    animationScreenClass,
//                    String.class,
//                    String.class,
//                    int.class,
//                    float.class,
//                    float.class,
//                    boolean.class
//            );
//            ctor.setAccessible(true);
//
//            // Create instance
//            // Arguments: (AnimationScreen, label text, defaultString, precision/int, min, max, boolean)
//            Object fieldInstance = ctor.newInstance(animationScreen, labelText, "0.00", 4, 0.0f, 1.0f, true);
//
//            // registerWidget(Actor, int)
//            Method registerWidget = figureFiltersClass.getMethod("registerWidget", Object.class, int.class);
//            // Choose ID 108 (0x6C) — original tint used 0x6B = 107
//            registerWidget.invoke(table, fieldInstance, 108);
//
//            // setHighFidelity(true)
//            Method setHigh = labelColorInputClass.getMethod("setHighFidelity", boolean.class);
//            setHigh.invoke(fieldInstance, true);
//
//            // setValue(Color.WHITE)
//            Field whiteField = colorClass.getField("WHITE");
//            Object whiteColor = whiteField.get(null);
//            Method setValue = labelColorInputClass.getMethod("setValue", colorClass);
//            setValue.invoke(fieldInstance, whiteColor);
//
//            // Add to mFiltersSubTable: get field mFiltersSubTable from the FigureFiltersToolTable
//            Field filtersSubTableField = figureFiltersClass.getDeclaredField("mFiltersSubTable");
//            filtersSubTableField.setAccessible(true);
//            Object filtersSubTable = filtersSubTableField.get(table); // Table instance
//
//            // Table.add(Actor) --> returns Cell
//            Method tableAdd = tableClass.getMethod("add", Object.class);
//            Object cell = tableAdd.invoke(filtersSubTable, fieldInstance);
//
//            // Call colspan(2) and fillX() on returned Cell
//            try {
//                Method colspan = cellClass.getMethod("colspan", int.class);
//                Object cellAfterColspan = colspan.invoke(cell, 2);
//
//                Method fillX = cellClass.getMethod("fillX");
//                Object cellAfterFill = fillX.invoke(cellAfterColspan);
//
//                // optional: call height(float) if you want to match separator height (skip if you do not know region)
//                // Method height = cellClass.getMethod("height", float.class);
//                // height.invoke(cellAfterFill, someFloat);
//            } catch (NoSuchMethodException nsme) {
//                // Some Cell signatures are generified — try alternate names or ignore if unavailable
//            }
//
//            // table.row()
//            Method tableRow = tableClass.getMethod("row");
//            tableRow.invoke(filtersSubTable);
//
//            // Create a dynamic proxy listener for LabelColorInputIncrementField$ColorFieldListener
//            Class<?> listenerInterface = Class.forName("org.fortheloss.framework.LabelColorInputIncrementField$ColorFieldListener", false, cl);
//
//            Object listenerProxy = Proxy.newProxyInstance(
//                    cl,
//                    new Class<?>[]{listenerInterface},
//                    new InvocationHandler() {
//                        // caching reflective lookups for speed
//                        Method redrawModuleMethod = null;
//                        Field animationBasedModuleRefField = null;
//                        Method setAmountMethod = null;
//                        Method setColorMethod = null;
//
//                        {
//                            try {
//                                // redrawModule() is a method on FigureFiltersToolTable
//                                redrawModuleMethod = figureFiltersClass.getMethod("redrawModule");
//                            } catch (Exception ignored) {}
//                            try {
//                                animationBasedModuleRefField = figureFiltersClass.getDeclaredField("mAnimationBasedModuleRef");
//                                animationBasedModuleRefField.setAccessible(true);
//                                // attempt to load methods on the animation-based module (if present)
//                                Class<?> animBasedModuleClass = animationBasedModuleRefField.getType();
//                                // Common method names (from app): setFigureTintAmountTo(float), setFigureTintColor(Color)
//                                try {
//                                    setAmountMethod = animBasedModuleClass.getMethod("setFigureTintAmountTo", float.class);
//                                } catch (NoSuchMethodException ignored) {}
//                                try {
//                                    setColorMethod = animBasedModuleClass.getMethod("setFigureTintColor", colorClass);
//                                } catch (NoSuchMethodException ignored) {}
//                            } catch (Exception ignored) {}
//                        }
//
//                        @Override
//                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                            String name = method.getName();
//                            try {
//                                if ("onTextFieldTouchEvent".equals(name)) {
//                                    // redrawModule()
//                                    if (redrawModuleMethod != null) {
//                                        redrawModuleMethod.invoke(table);
//                                    }
//                                    return null;
//                                } else if ("onTextFieldChange".equals(name)) {
//                                    // args: (float value, boolean isFinal)
//                                    float value = ((Number) args[0]).floatValue();
//                                    if (redrawModuleMethod != null) {
//                                        redrawModuleMethod.invoke(table);
//                                    }
//                                    if (animationBasedModuleRefField != null && setAmountMethod != null) {
//                                        Object animRef = animationBasedModuleRefField.get(table);
//                                        if (animRef != null) {
//                                            setAmountMethod.invoke(animRef, value);
//                                        }
//                                    }
//                                    return null;
//                                } else if ("onColorChange".equals(name)) {
//                                    // args: (Color color)
//                                    Object colorArg = args[0];
//                                    if (animationBasedModuleRefField != null && setColorMethod != null && colorArg != null) {
//                                        Object animRef = animationBasedModuleRefField.get(table);
//                                        if (animRef != null) {
//                                            setColorMethod.invoke(animRef, colorArg);
//                                        }
//                                    }
//                                    return null;
//                                }
//                            } catch (Throwable t) {
//                                // swallow individual listener errors — but print for debugging
//                                t.printStackTrace();
//                            }
//                            return null;
//                        }
//                    }
//            );
//
//            // setFieldListener(listener)
//            Method setFieldListener = labelColorInputClass.getMethod("setFieldListener", Class.forName("org.fortheloss.framework.LabelInputIncrementField$FieldListener", false, cl));
//            // Note: LabelColorInputIncrementField likely inherits/uses the nested FieldListener type; pass the proxy
//            setFieldListener.invoke(fieldInstance, listenerProxy);
//
//            // Show a small Toast for debugging/confirmation (if possible)
//            try {
//                // module.getContext() earlier returned AnimationScreen, but it should be a Context or have an Activity reference
//                // We'll attempt to use the animationScreen's getContext() result if it's an actual android Context
//                Context ctx = null;
//                if (animationScreen instanceof Context) {
//                    ctx = (Context) animationScreen;
//                } else {
//                    // If AnimationScreen isn't a Context, attempt to retrieve an android context via module.getContext() or similar
//                    // Fallback: try module.getContext() result (we already used animationScreen variable)
//                    // If not a Context, this will fail silently
//                }
//                if (ctx != null) {
//                    Toast.makeText(ctx, "Custom filter slot installed", Toast.LENGTH_SHORT).show();
//                } else {
//                    // If we didn't get an Android Context, attempt to call android.widget.Toast with reflection
//                    try {
//                        Class<?> toastClass = Class.forName("android.widget.Toast", false, cl);
//                        Method makeText = toastClass.getMethod("makeText", Context.class, CharSequence.class, int.class);
//                        // Attempt to reuse animationScreen as Context if it actually is; if not, this will throw
//                        if (animationScreen instanceof Context) {
//                            Object toast = makeText.invoke(null, animationScreen, "Custom filter slot installed", Toast.LENGTH_SHORT);
//                            Method show = toastClass.getMethod("show");
//                            show.invoke(toast);
//                        }
//                    } catch (Exception ignored) {
//                    }
//                }
//            } catch (Throwable t) {
//                // hide toast errors; not essential
//                t.printStackTrace();
//            }
//
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
//    }
//}
//

