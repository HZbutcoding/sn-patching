package app.revanced.extension.customfilters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Late-injection helper. Called from smali injection.
 *
 * This will:
 *  - schedule retries until a) we get a usable context/module or b) timeout
 *  - when ready show a toast and attempt to add the tint field via reflection
 */
public class TintFieldHook {
    private static final String TAG = "CustomFilterHook";
    private static final int INITIAL_DELAY_MS = 200;     // first check delay
    private static final int RETRY_INTERVAL_MS = 500;    // interval between retries
    private static final int MAX_RETRIES = 40;           // ~20 seconds max

    public static void installTintField(final Object toolTable) {
        try {
            Log.i(TAG, "installTintField() invoked, scheduling readiness checks...");
            scheduleAttempt(toolTable, 0);
        } catch (Throwable t) {
            // Must never let this bubble out into the app process
            Log.e(TAG, "installTintField top-level failure", t);
        }
    }

    private static void scheduleAttempt(final Object toolTable, final int attempt) {
        try {
            // use main looper so any UI calls are safe
            Handler mainHandler = new Handler(Looper.getMainLooper());
            int delay = (attempt == 0) ? INITIAL_DELAY_MS : RETRY_INTERVAL_MS;

            mainHandler.postDelayed(() -> {
                try {
                    if (toolTable == null) {
                        Log.w(TAG, "toolTable is null, aborting attempts.");
                        return;
                    }

                    // Try to obtain a context (via getContext() on the Module or toolTable)
                    Context context = tryGetContext(toolTable);
                    boolean hasContext = context != null;

                    Log.i(TAG, "attempt " + attempt + " - context available: " + hasContext);

                    // Try to get module or other indicators that initialization ran
                    Object module = tryGetModule(toolTable);
                    boolean hasModule = module != null;
                    Log.i(TAG, "attempt " + attempt + " - module available: " + hasModule +
                            (module != null ? (" (" + module.getClass().getName() + ")") : ""));

                    // If we have a context -> show a toast so you can see it's working
                    if (context != null) {
                        final Context ctx = context;
                        // show toast on main thread
                        Toast.makeText(ctx, "✅ TintFieldHook triggered (late)!", Toast.LENGTH_SHORT).show();
                    }

                    // If we have module and context, attempt to install the field (safe reflect)
                    if (hasModule && hasContext) {
                        boolean installed = tryInstallField(toolTable, module, context);
                        if (installed) {
                            Log.i(TAG, "Tint field installed successfully; stopping retries.");
                            return; // success -> stop retrying
                        } else {
                            Log.i(TAG, "tryInstallField returned false (requirements not met yet).");
                        }
                    }

                    // Not ready yet -> retry until MAX_RETRIES
                    if (attempt < MAX_RETRIES) {
                        scheduleAttempt(toolTable, attempt + 1);
                    } else {
                        Log.w(TAG, "TintFieldHook: reached max retries, giving up.");
                    }

                } catch (Throwable inner) {
                    // swallow and continue retrying to avoid crashing the host app
                    Log.e(TAG, "Error during scheduled attempt", inner);
                    if (attempt < MAX_RETRIES) scheduleAttempt(toolTable, attempt + 1);
                }
            }, delay);
        } catch (Throwable t) {
            Log.e(TAG, "Failed scheduling attempt", t);
        }
    }

    /**
     * Try to call toolTable.getModule() or fallback to other getters.
     */
    private static Object tryGetModule(Object toolTable) {
        try {
            // Many ToolTable classes have getModule() (in your earlier decompiled code it did)
            Method getModule = null;
            try {
                getModule = toolTable.getClass().getMethod("getModule");
            } catch (NoSuchMethodException ignored) {
            }
            if (getModule != null) {
                Object module = getModule.invoke(toolTable);
                if (module != null) return module;
            }

            // Some implementations may store a 'module' field; check reflectively
            try {
                Field moduleField = toolTable.getClass().getDeclaredField("mModule");
                moduleField.setAccessible(true);
                Object module = moduleField.get(toolTable);
                if (module != null) return module;
            } catch (NoSuchFieldException ignored) {
            }

        } catch (Throwable t) {
            Log.w(TAG, "tryGetModule failed", t);
        }
        return null;
    }

    /**
     * Try to obtain a Context. Try several strategies safely.
     */
    private static Context tryGetContext(Object toolTable) {
        try {
            // 1) direct getContext() on toolTable
            try {
                Method getContext = toolTable.getClass().getMethod("getContext");
                Object ctx = getContext.invoke(toolTable);
                if (ctx instanceof Context) return (Context) ctx;
            } catch (NoSuchMethodException ignored) {
            }

            // 2) via module.getContext()
            Object module = tryGetModule(toolTable);
            if (module != null) {
                try {
                    Method getContext = module.getClass().getMethod("getContext");
                    Object ctx = getContext.invoke(module);
                    if (ctx instanceof Context) return (Context) ctx;
                } catch (NoSuchMethodException ignored) {
                }
            }

            // 3) try a common field lookup (last resort)
            try {
                Field ctxField = toolTable.getClass().getDeclaredField("mContext");
                ctxField.setAccessible(true);
                Object ctx = ctxField.get(toolTable);
                if (ctx instanceof Context) return (Context) ctx;
            } catch (NoSuchFieldException ignored) {
            }

        } catch (Throwable t) {
            Log.w(TAG, "tryGetContext failed", t);
        }
        return null;
    }

    /**
     * Attempt to create & register the tint field via reflection.
     * Returns true on success (field created and registered), false if not enough requirements yet.
     *
     * This method purposely tolerates missing constructors/methods and will NOT throw errors up.
     */
    private static boolean tryInstallField(Object toolTable, Object module, Context context) {
        try {
            // We want to construct a LabelColorInputIncrementField similar to the app's code:
            // new LabelColorInputIncrementField(animationScreen, localizedLabel, "0.00", 4, 0.0f, 1.0f, true);
            // Then call registerWidget(table, 107) and setHighFidelity(true), setValue(Color.WHITE) etc.

            ClassLoader cl = toolTable.getClass().getClassLoader();

            // Load classes reflectively; if any missing, bail out gracefully
            Class<?> labelColorFieldClass;
            try {
                labelColorFieldClass = Class.forName("org.fortheloss.framework.LabelColorInputIncrementField", false, cl);
            } catch (Throwable e) {
                Log.i(TAG, "LabelColorInputIncrementField class not present yet.");
                return false; // not ready to create field
            }

            // Need animation screen (module.getContext())
            Object animationScreen = null;
            try {
                Method getContext = module.getClass().getMethod("getContext");
                animationScreen = getContext.invoke(module);
            } catch (NoSuchMethodException ignored) {
            }
            if (animationScreen == null) {
                Log.i(TAG, "animationScreen not available yet.");
                return false;
            }

            // Localization: App.localize("tint")
            String tintLabel = "tint";
            try {
                Class<?> appClass = Class.forName("org.fortheloss.sticknodes.App", false, cl);
                Method localize = appClass.getMethod("localize", String.class);
                Object res = localize.invoke(null, "tint");
                if (res instanceof String) tintLabel = (String) res;
            } catch (Throwable t) {
                Log.w(TAG, "Could not localize 'tint', using fallback", t);
            }

            // Find the constructor signature:
            // <init>(AnimationScreen, String, String, int, float, float, boolean)
            Constructor<?> ctor = null;
            for (Constructor<?> c : labelColorFieldClass.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length >= 7) { // loose matching (some builds may differ)
                    ctor = c;
                    break;
                }
            }
            if (ctor == null) {
                Log.i(TAG, "No suitable constructor found for LabelColorInputIncrementField yet.");
                return false;
            }

            ctor.setAccessible(true);

            // Create instance (safely)
            Object fieldInstance;
            try {
                // fallback param values (match the game's constructor as best as possible)
                Object[] args = new Object[] { animationScreen, tintLabel, "0.00", Integer.valueOf(4),
                        Float.valueOf(0.0f), Float.valueOf(1.0f), Boolean.TRUE };
                fieldInstance = ctor.newInstance(args);
            } catch (Throwable t) {
                Log.w(TAG, "Constructor invocation failed", t);
                return false;
            }

            // registerWidget(table, id)
            try {
                Method registerWidget = toolTable.getClass().getMethod("registerWidget", Object.class, int.class);
                registerWidget.invoke(toolTable, fieldInstance, Integer.valueOf(107));
            } catch (NoSuchMethodException nm) {
                // try find in superclass
                try {
                    Method registerWidget = toolTable.getClass().getMethod("registerWidget", Object.class, Integer.TYPE);
                    registerWidget.invoke(toolTable, fieldInstance, Integer.valueOf(107));
                } catch (Throwable t) {
                    Log.w(TAG, "registerWidget not available; cannot register field", t);
                    // still proceed to add to UI if possible (but bail as not fully integrated)
                    return false;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed to invoke registerWidget", t);
                return false;
            }

            // setHighFidelity(true)
            try {
                Method setHighFidelity = fieldInstance.getClass().getMethod("setHighFidelity", boolean.class);
                setHighFidelity.invoke(fieldInstance, Boolean.TRUE);
            } catch (Throwable ignored) {
                Log.i(TAG, "setHighFidelity not present or failed - ignoring");
            }

            // set default color value if possible: setValue(Color.WHITE)
            try {
                Class<?> colorClass = Class.forName("com.badlogic.gdx.graphics.Color", false, cl);
                Field white = colorClass.getField("WHITE");
                Object whiteColor = white.get(null);
                Method setValue = fieldInstance.getClass().getMethod("setValue", colorClass);
                setValue.invoke(fieldInstance, whiteColor);
            } catch (Throwable ignored) {
                Log.i(TAG, "Could not set default color - ignoring");
            }

            Log.i(TAG, "tryInstallField: created and registered field instance: " + fieldInstance.getClass().getName());
            // Success
            return true;

        } catch (Throwable t) {
            Log.e(TAG, "Unexpected error in tryInstallField", t);
            return false;
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

