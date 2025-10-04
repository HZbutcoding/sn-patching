package com.HZ.CustomFilters

import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions



@Suppress("unused")
val AddCustomFilterSlot = bytecodePatch(
    name = "Custom Filter slot",
    description = "Adds a placeholder slot for now (debugging stage)"
) {
    compatibleWith(
        "org.fortheloss.sticknodes"("4.2.5"),
        "org.fortheloss.sticknodespro"("4.2.5"),
        "org.fortheloss.sticknodesbeta"("4.2.6")
    )

    // If you have an extension file, keep this; otherwise remove
    extendWith("extensions/extension.rve")

    // inside your bytecodePatch { execute { ... } } block

    execute {
        println("🚀 [CustomFilterPatch] execute() is running")

        val match = figureFiltersInitFingerprint.patternMatch
            ?: throw RuntimeException("Could not match FigureFiltersToolTable init")

        println("✅ [CustomFilterPatch] Found match at index ${match.startIndex}")

        val targetMethod = figureFiltersInitFingerprint.method
        println("✅ [CustomFilterPatch] Modifying method: ${targetMethod.name}")

        targetMethod.addInstruction(
            match.startIndex + 3, // or another index you’re testing
            """
        invoke-static {p0}, Lapp/revanced/extension/customfilters/TintFieldHook;->installTintField(Ljava/lang/Object;)V
        """.trimIndent()
        )

        println("✅ [CustomFilterPatch] Instruction added successfully")
    }







//        val figureFilterInit = figureFiltersInitFingerprint.patternMatch!!.endIndex;
//        figureFiltersInitFingerprint.method.addInstruction(0,
//            """
//                invoke-static {p0}, Lapp/revanced/extension/customfilters/TintFieldHook;->installTintField(Ljava/lang/Object;)V
//            """.trimIndent()
//
//        )


    }