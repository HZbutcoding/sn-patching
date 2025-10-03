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



        val match = figureFiltersInitFingerprint.patternMatch!!.startIndex;
        figureFiltersInitFingerprint.method.addInstruction(0,
            """
            invoke-static {p0}, Lapp/revanced/extension/customfilters/TintFieldHook;->installTintField(Ljava/lang/Object;)V
            """.trimIndent()
        )








//        val figureFilterInit = figureFiltersInitFingerprint.patternMatch!!.endIndex;
//        figureFiltersInitFingerprint.method.addInstruction(0,
//            """
//                invoke-static {p0}, Lapp/revanced/extension/customfilters/TintFieldHook;->installTintField(Ljava/lang/Object;)V
//            """.trimIndent()
//
//        )
        }

    }