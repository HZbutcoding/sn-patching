package com.HZ.CustomFilters



import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction



@Suppress("unused")
val AddCustomFilterSlot = bytecodePatch(
    name = "Custom Filter slot",
    description = "Adds a placeholder slot for now (debugging stage 2)"
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
        println("[CustomFilterPatch] execute() is running")

        val targetMethod = figureFiltersInitFingerprint.method
            ?: throw RuntimeException("Could not find FigureFiltersToolTable.initialize()")

        val impl = targetMethod.implementation
            ?: throw RuntimeException("Method implementation is null")

        println("Matched method: ${targetMethod.name}")
        println("Registers in ${targetMethod.name}: ${impl.registerCount}")

        // try to find the first "move-object ... p0" instruction in the method
        val instrs = impl.instructions
        val moveP0Index = instrs.indexOfFirst { it.toString().contains("p0") && it.toString().contains("move-object") }

        val insertIndex = if (moveP0Index >= 0) moveP0Index + 1 else 1   // fallback to 1 (not 0)
        println("Inserting at index $insertIndex (moveP0Index=$moveP0Index)")

        // Use range invoke so high regs are accepted
        targetMethod.addInstruction(
            insertIndex,
            """
        invoke-static/range {p0 .. p0}, Lapp/revanced/extension/customfilters/TintFieldHook;->scheduleInstall(Ljava/lang/Object;)V
        """.trimIndent()
        )

        println("Injection requested at index $insertIndex")
    }
}
