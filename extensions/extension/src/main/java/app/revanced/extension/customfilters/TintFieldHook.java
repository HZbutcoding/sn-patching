package app.revanced.extension.customfilters;

import android.content.Context;
import android.widget.Toast;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import android.util.Log;

public class TintFieldHook {
    private static final String TAG = "CustomFilterHook";

    /**
     * Called from the patch script using smali injection.
     * This will confirm the hook is running in the actual app.
     */
    public static void installTintField(Object toolTable) {
        try {
            // Attempt to access context from the toolTable (if available)
            Context context = null;
            try {
                context = (Context) toolTable.getClass()
                        .getMethod("getContext")
                        .invoke(toolTable);
            } catch (Exception inner) {
                Log.w(TAG, "Could not retrieve context from toolTable", inner);
            }

            // Display a toast confirmation if we got the context
            if (context != null) {
                Toast.makeText(context, "✅ TintFieldHook triggered!", Toast.LENGTH_SHORT).show();
            }

            // Log both success and diagnostic info to adb logcat
            Log.i(TAG, "installTintField() invoked successfully!");
            if (toolTable != null) {
                Log.i(TAG, "ToolTable class: " + toolTable.getClass().getName());
            } else {
                Log.w(TAG, "ToolTable reference is null!");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to execute TintFieldHook", e);
        }
    }
}


/**
 * Reflection-only helper that creates a "Custom Tint" slot in FigureFiltersToolTable.
 *
 * Call: CustomTintSlotHook.installCustomTintSlot(tableObject);
 */

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

