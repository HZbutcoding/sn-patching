package com.HZ.CustomFilters

import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.HZ.CustomFilters.figureFiltersInitFingerprint

// Fingerprint: tweak class/method checks to match the real target in your APK
val customFilterFingerprint = fingerprint {
    // match a void method (likely constructor or init method that creates the UI)
    returns("V")
    parameters() // no params — change if real target has params

    // Narrow by class + method name. Replace with the real class and method from JADX.
    custom { method, classDef ->
        classDef.type == "Lorg/fortheloss/sticknodes/animationscreen/modules/tooltables/FigureFiltersToolTable;"
                && (method.name == "<init>" || method.name == "initFilters" || method.name == "createUI")
    }
}

@Suppress("unused")
val AddCustomFilterSlot = bytecodePatch(
    name = "Custom Filter slot",
    description = "Adds a placeholder injection so patch project compiles; replace nop with real smali later."
) {
    compatibleWith(
        "org.fortheloss.sticknodes"("4.2.5"),
        "org.fortheloss.sticknodespro"("4.2.5"),
        "org.fortheloss.sticknodesbeta"("4.2.6")
    )

    // If you have an extension file, keep this; otherwise remove
    // extendWith("extensions/extension.rve")

    // inside your bytecodePatch { execute { ... } } block

    execute {
        val fpObj = figureFiltersInitFingerprint as Any

        fun tryInvokeNoArg(obj: Any, names: List<String>): Any? {
            for (n in names) {
                try {
                    val m = obj::class.java.getMethod(n)
                    if (m.parameterCount == 0) return m.invoke(obj)
                } catch (_: Throwable) {
                }
            }
            return null
        }

        fun tryGetField(obj: Any, names: List<String>): Any? {
            for (n in names) {
                try {
                    val f = obj::class.java.getDeclaredField(n)
                    f.isAccessible = true
                    return f.get(obj)
                } catch (_: Throwable) {
                }
            }
            return null
        }

        val match =
            tryInvokeNoArg(fpObj, listOf("invoke", "match", "getMatch", "get"))
                ?: tryGetField(fpObj, listOf("result", "match", "_result"))
                ?: tryInvokeNoArg(fpObj, listOf("get"))

        if (match == null) return@execute

        val methodObj = tryInvokeNoArg(
            match,
            listOf("getMutableMethod", "toMutableMethod", "toMutable", "mutableMethod", "getMethod", "method")
        ) ?: tryGetField(match, listOf("mutableMethod", "method"))

        if (methodObj == null) return@execute

        // --- SMALI instructions to add ---
        val smaliInstructions = listOf(
            // --- mGlowColorField ---
            "new-instance v0, Lorg/fortheloss/framework/LabelColorInputIncrementField;",
            "invoke-virtual {p0}, Lcom/HZ/CustomFilters/FigureFiltersToolTable;->getModule()Lorg/fortheloss/framework/Module;",
            "move-result-object v1",
            "invoke-virtual {v1}, Lorg/fortheloss/framework/Module;->getContext()Landroid/content/Context;",
            "move-result-object v2",
            "const-string v3, \"glow\"",
            "const-string v4, \"0.00\"",
            "const/4 v5, 0x4",
            "const/high16 v6, 0x0",
            "const/high16 v7, 0x40000000", // float 2.0f
            "const/4 v8, 0x1", // boolean true
            "invoke-direct {v0, v2, v3, v4, v5, v6, v7, v8}, Lorg/fortheloss/framework/LabelColorInputIncrementField;-><init>(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;IFFZ)V",
            "iput-object v0, p0, Lcom/HZ/CustomFilters/FigureFiltersToolTable;->mGlowColorField:Lorg/fortheloss/framework/LabelColorInputIncrementField;",
            "const/16 v9, 113",
            "invoke-virtual {p0, v0, v9}, Lcom/HZ/CustomFilters/FigureFiltersToolTable;->registerWidget(Lorg/fortheloss/framework/LabelColorInputIncrementField;I)V",
            "invoke-virtual {v0, 0x1}, Lorg/fortheloss/framework/LabelColorInputIncrementField;->setHighFidelity(Z)V",

            // --- mGlowIntensityField ---
            "new-instance v10, Lorg/fortheloss/framework/LabelColorInputIncrementField;",
            "invoke-virtual {p0}, Lcom/HZ/CustomFilters/FigureFiltersToolTable;->getModule()Lorg/fortheloss/framework/Module;",
            "move-result-object v11",
            "invoke-virtual {v11}, Lorg/fortheloss/framework/Module;->getContext()Landroid/content/Context;",
            "move-result-object v12",
            "const-string v13, \"intensity\"",
            "const-string v14, \"0\"",
            "const/4 v15, 0x4",
            "const/high16 v16, 0x0",
            "const/high16 v17, 0x40000000", //float 2.0f
            "const/4 v18, 0x1", //boolean true
            "invoke-direct {v10, v12, v13, v14, v15, v16, v17, v18}, Lorg/fortheloss/framework/LabelColorInputIncrementField;-><init>(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;IFFZ)V",
            "iput-object v10, p0, Lcom/HZ/CustomFilters/FigureFiltersToolTable;->mGlowIntensityField:Lorg/fortheloss/framework/LabelColorInputIncrementField;",
            "const/16 v19, 114",
            "invoke-virtual {p0, v10, v19}, Lcom/HZ/CustomFilters/FigureFiltersToolTable;->registerWidget(Lorg/fortheloss/framework/LabelColorInputIncrementField;I)V",
            "invoke-virtual {v10, 0x1}, Lorg/fortheloss/framework/LabelColorInputIncrementField;->setHighFidelity(Z)V"
        )

        val mutableMethod = methodObj as? MutableMethod ?: return@execute

        for (instr in smaliInstructions) {
            mutableMethod.addInstruction(instr)  // now uses the correct overload
        }

    }
}


//another comment


//package com.HZ.CustomFilters
//
//import app.revanced.patcher.patch.bytecodePatch
//import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
//import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
//import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
//import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
//
//
//@Suppress("unused")
//val AddCustomFilterSlot = bytecodePatch(
//    name = "Custom Filter slot",
//    description = "adds an extra custom filter slot for prototyping new shaders",
//) {
//    compatibleWith(
//        "org.fortheloss.sticknodes"("4.2.3"),
//        "org.fortheloss.sticknodespro"("4.2.3")
//        )
//
////    extendWith("extensions/extension.rve")
//
//    execute {
//        val method = figureFiltersInitFingerprint.resultOrThrow().method;
//
//
//        // Insert call to your helper at the end of the constructor
//        method.addInstruction(
//            method.implementation!!.instructions.size - 1, // right before return-void
//            "invoke-static {}, Lcom/HZ/CustomFilters/CustomMenu;->addCustomFilter()V"
//        )
//    }
//}
