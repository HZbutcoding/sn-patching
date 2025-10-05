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

            // Use the exact constructor signature from smali:
            // (AnimationScreen, String, String, int, float, float, boolean)
            Constructor<?> ctor = labelColorInputClass.getDeclaredConstructor(
                    animationScreenClass,
                    String.class,    // label text
                    String.class,    // default value string
                    int.class,       // inputMaxLength
                    float.class,     // minValue
                    float.class,     // maxValue
                    boolean.class    // isFloat
            );
            ctor.setAccessible(true);

            // Create with parameters from smali analysis
            Object fieldInstance = ctor.newInstance(
                    animationScreen,
                    labelText,       // "Tint" - localized label
                    "1.00",         // Default value
                    4,              // inputMaxLength - changed to 4 (from transparency field)
                    0.0f,           // minValue
                    1.0f,           // maxValue
                    true            // isFloat
            );

            Log.i(TAG, "✓ Field instance created");

            // ========================================
            // Step 5: Register the widget with ID 108 - FIXED!
            // ========================================
            Log.i(TAG, "Registering widget...");

            Class<?> actorClass = Class.forName("com.badlogic.gdx.scenes.scene2d.Actor", false, cl);

// FIX: Look for registerWidget in the superclass (ToolTable)
            Class<?> toolTableClass = Class.forName(
                    "org.fortheloss.sticknodes.animationscreen.modules.tooltables.ToolTable",
                    false, cl
            );

            Method registerWidget = toolTableClass.getDeclaredMethod("registerWidget", actorClass, int.class);
            registerWidget.setAccessible(true); // Make protected method accessible

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
            // Step 7: Add to mFiltersSubTable with proper row management
            // ========================================
            Log.i(TAG, "Adding to filters table...");

            Field filtersSubTableField = figureFiltersClass.getDeclaredField("mFiltersSubTable");
            filtersSubTableField.setAccessible(true);
            Object filtersSubTable = filtersSubTableField.get(table);

// Add to table and get Cell
            Method tableAdd = tableClass.getMethod("add", actorClass);
            Object cell = tableAdd.invoke(filtersSubTable, fieldInstance);

// Configure Cell layout EXACTLY like the original code
            Method fillX = cellClass.getMethod("fillX");
            Object cellAfterFillX = fillX.invoke(cell);

            Method colspan = cellClass.getMethod("colspan", int.class);
            colspan.invoke(cellAfterFillX, 2); // colspan(2)

// NOW call row() after cell configuration (matching smali pattern)
            Method tableRow = tableClass.getMethod("row");
            tableRow.invoke(filtersSubTable);

            Log.i(TAG, "✓ Added to filters table (matched original pattern)");




//            Log.i(TAG, "Adding to filters table...");
//
//            Field filtersSubTableField = figureFiltersClass.getDeclaredField("mFiltersSubTable");
//            filtersSubTableField.setAccessible(true);
//            Object filtersSubTable = filtersSubTableField.get(table);
//
//// FIX: Try different method signatures for Table.add
//            Method tableAdd = null;
//            try {
//                // First try: add(Actor)
//                tableAdd = tableClass.getMethod("add", actorClass);
//                Log.i(TAG, "✓ Found Table.add(Actor) method");
//            } catch (NoSuchMethodException e) {
//                Log.w(TAG, "✗ Table.add(Actor) not found, trying alternatives...");
//
//                // Try: add(Actor) with getDeclaredMethod
//                try {
//                    tableAdd = tableClass.getDeclaredMethod("add", actorClass);
//                    tableAdd.setAccessible(true);
//                    Log.i(TAG, "✓ Found Table.add(Actor) via getDeclaredMethod");
//                } catch (NoSuchMethodException e2) {
//                    Log.w(TAG, "✗ Table.add(Actor) not found with getDeclaredMethod");
//
//                    // Last resort: Try to find any add method that takes one parameter
//                    Method[] methods = tableClass.getMethods();
//                    for (Method method : methods) {
//                        if (method.getName().equals("add") && method.getParameterTypes().length == 1) {
//                            tableAdd = method;
//                            Log.i(TAG, "✓ Found compatible Table.add method: " + method);
//                            break;
//                        }
//                    }
//                }
//            }
//
//            if (tableAdd != null) {
//                Object cell = tableAdd.invoke(filtersSubTable, fieldInstance);
//                Log.i(TAG, "✓ Field added to table, got cell: " + (cell != null ? cell.getClass().getSimpleName() : "null"));
//
//                // Configure Cell layout: colspan(2).fillX()
//                try {
//                    Method colspan = cellClass.getMethod("colspan", int.class);
//                    Object cellAfterColspan = colspan.invoke(cell, 2);
//
//                    Method fillX = cellClass.getMethod("fillX");
//                    fillX.invoke(cellAfterColspan);
//
//                    Log.i(TAG, "✓ Cell configured (colspan=2, fillX)");
//                } catch (NoSuchMethodException e) {
//                    Log.w(TAG, "Could not configure Cell layout: " + e.getMessage());
//                }
//
//                // Add row separator
//                Method tableRow = tableClass.getMethod("row");
//                tableRow.invoke(filtersSubTable);
//
//                Log.i(TAG, "✓ Added to filters table");
//            } else {
//                throw new NoSuchMethodException("Could not find Table.add method with any signature");
//            }

            // ========================================
            // Step 8: SIMPLIFIED - Skip complex listener for now
            // ========================================
            Log.i(TAG, "Skipping complex listener setup for stability...");
            // The field will work with basic functionality without the listener

            // ========================================
            // Step 9: Success!
            // ========================================
            Log.i(TAG, "=== Custom tint filter installed successfully! ===");
            showToast(animationScreen, "Custom tint filter added!");

        } catch (Throwable t) {
            Log.e(TAG, "Failed to install custom tint slot", t);
            t.printStackTrace();
        }
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