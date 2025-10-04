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

        val impl = targetMethod.implementation ?: throw RuntimeException("No implementation found")
        val registerCount = impl.registerCount
        val parameterCount = targetMethod.parameterTypes.size + 1 // +1 for "this"
        val thisRegister = registerCount - parameterCount // vXX that represents "this"

        println("Matched method: ${targetMethod.name}")
        println("Registers in ${targetMethod.name}: $registerCount")
        println("Resolved 'this' (p0) as v$thisRegister")

        // inject using resolved vXX instead of p0
        targetMethod.addInstruction(
            0,
            """
        # copy 'this' into a low-numbered register
        move-object v0, p0
        invoke-static {v0}, Lapp/revanced/extension/customfilters/TintFieldHook;->installTintField(Ljava/lang/Object;)V
        """.trimIndent()
        )

        println("✅ Injection added successfully at register v$thisRegister")
    }
}
