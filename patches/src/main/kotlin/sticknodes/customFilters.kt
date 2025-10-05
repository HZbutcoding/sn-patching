package com.HZ.CustomFilters

import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions

@Suppress("unused")
val AddCustomFilterSlot = bytecodePatch(
    name = "Custom Filter slot",
    description = "Adds a custom tint filter slot to Stick Nodes (debug 5)"
) {
    compatibleWith(
        "org.fortheloss.sticknodes"("4.2.5"),
        "org.fortheloss.sticknodespro"("4.2.5"),
        "org.fortheloss.sticknodesbeta"("4.2.6")
    )

    extendWith("extensions/extension.rve")

    execute {
        println("=== [CustomFilterPatch] Starting execution (GUARANTEED METHOD) ===")

        val targetMethod = figureFiltersInitFingerprint.method
            ?: throw RuntimeException("[CustomFilterPatch] Could not find FigureFiltersToolTable.initialize()")

        val implementation = targetMethod.implementation as? MutableMethodImplementation
            ?: throw RuntimeException("[CustomFilterPatch] Method implementation is not mutable")

        println("[CustomFilterPatch] ✓ Matched method: ${targetMethod.name}")
        println("[CustomFilterPatch] ✓ Registers: ${implementation.registerCount}")

        val registerCount = implementation.registerCount
        val paramCount = targetMethod.parameters.size + 1
        val p0Register = registerCount - paramCount

        println("[CustomFilterPatch] ✓ p0 maps to v$p0Register (register $p0Register)")

        // Find a SAFE injection point - after prologue
        val instructions = implementation.instructions
        var safeIndex = -1

        println("[CustomFilterPatch] Scanning ${instructions.size} instructions for safe point...")

        // Look for the FIRST instruction that's NOT a move or nop
        // This ensures we're past the prologue
        for (i in instructions.indices) {
            val instruction = instructions[i]
            val opcode = instruction.opcode

            // Skip move and nop instructions at the start
            if (opcode == Opcode.NOP ||
                opcode?.name?.startsWith("MOVE") == true) {
                continue
            }

            // Found first real instruction - inject AFTER it
            safeIndex = i + 1
            println("[CustomFilterPatch] ✓ Found safe point after ${opcode?.name} at index $i")
            break
        }

        if (safeIndex == -1 || safeIndex < 2) {
            safeIndex = 2 // Absolute minimum safe index
            println("[CustomFilterPatch] ⚠ Using minimum safe index: 2")
        }

        println("[CustomFilterPatch] Injection point: index $safeIndex")

        // Create the method reference for our hook
        val hookMethodRef = ImmutableMethodReference(
            "Lapp/revanced/extension/customfilters/TintFieldHook;",
            "scheduleInstall",
            listOf("Ljava/lang/Object;"),
            "V"
        )

        println("[CustomFilterPatch] Created method reference: $hookMethodRef")

        // Use BuilderInstruction3rc instead of BuilderInstruction35c for wider register support
        // This format supports register ranges (vX .. vY) instead of individual nibble registers
        val invokeInstruction = BuilderInstruction3rc(
            Opcode.INVOKE_STATIC_RANGE,
            p0Register, // start register
            1, // register count (just one parameter: p0)
            hookMethodRef
        )

        println("[CustomFilterPatch] Created invoke-static/range instruction")

        try {
            // Add the instruction at the safe index
            implementation.addInstruction(safeIndex, invokeInstruction)
            println("[CustomFilterPatch] ✓ Instruction added at index $safeIndex")
        } catch (e: Exception) {
            println("[CustomFilterPatch] ✗ Failed to add instruction: ${e.message}")
            e.printStackTrace()
            throw e
        }

        // Verify the injection
        println("[CustomFilterPatch] Verifying injection...")

        val updatedInstructions = implementation.instructions
        var foundAtIndex = -1

        for (i in updatedInstructions.indices) {
            val instr = updatedInstructions[i]

            // Check if this is an invoke-static to our hook (could be regular or range)
            if (instr.opcode == Opcode.INVOKE_STATIC || instr.opcode == Opcode.INVOKE_STATIC_RANGE) {
                when (instr) {
                    is Instruction3rc -> {
                        val ref = instr.reference as? MethodReference
                        if (ref?.definingClass?.contains("TintFieldHook") == true) {
                            foundAtIndex = i
                            println("[CustomFilterPatch] ✓✓✓ HOOK FOUND (range) at index $i ✓✓✓")
                            println("[CustomFilterPatch]     Method: ${ref.name}")
                            println("[CustomFilterPatch]     Class: ${ref.definingClass}")
                            println("[CustomFilterPatch]     Start register: v${instr.startRegister}")
                            println("[CustomFilterPatch]     Register count: ${instr.registerCount}")
                            break
                        }
                    }
                    // Also check for regular invoke-static in case it got optimized
                    is com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c -> {
                        val ref = instr.reference as? MethodReference
                        if (ref?.definingClass?.contains("TintFieldHook") == true) {
                            foundAtIndex = i
                            println("[CustomFilterPatch] ✓✓✓ HOOK FOUND (regular) at index $i ✓✓✓")
                            println("[CustomFilterPatch]     Method: ${ref.name}")
                            println("[CustomFilterPatch]     Class: ${ref.definingClass}")
                            break
                        }
                    }
                }
            }
        }

        if (foundAtIndex == -1) {
            println("[CustomFilterPatch] ✗✗✗ CRITICAL: Hook NOT found after injection! ✗✗✗")

            // Dump the area where we tried to inject
            println("[CustomFilterPatch] Dumping instructions around injection point:")
            val start = maxOf(0, safeIndex - 3)
            val end = minOf(updatedInstructions.size - 1, safeIndex + 5)

            for (i in start..end) {
                val marker = if (i == safeIndex) " <-- EXPECTED HERE" else ""
                val instr = updatedInstructions[i]
                println("  [$i] ${instr.opcode?.name} ${instr}$marker")
            }

            throw RuntimeException("Injection verification failed!")
        } else {
            println("[CustomFilterPatch] ===================================")
            println("[CustomFilterPatch] ✓ INJECTION SUCCESSFUL AND VERIFIED")
            println("[CustomFilterPatch] ===================================")

            // Show context
            println("[CustomFilterPatch] Context:")
            val start = maxOf(0, foundAtIndex - 2)
            val end = minOf(updatedInstructions.size - 1, foundAtIndex + 2)

            for (i in start..end) {
                val marker = if (i == foundAtIndex) " <-- HOOK" else ""
                val instr = updatedInstructions[i]
                println("  [$i] ${instr.opcode?.name}$marker")
            }
        }

        println("=== [CustomFilterPatch] Execution completed successfully ===")
    }
}








//package com.HZ.CustomFilters
//
//import app.revanced.patcher.patch.bytecodePatch
//import com.android.tools.smali.dexlib2.Opcode
//import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
//import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
//import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
//import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
//import com.android.tools.smali.dexlib2.iface.reference.MethodReference
//import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
//
//@Suppress("unused")
//val AddCustomFilterSlot = bytecodePatch(
//    name = "Custom Filter slot",
//    description = "Adds a custom tint filter slot to Stick Nodes (debug 5)"
//) {
//    compatibleWith(
//        "org.fortheloss.sticknodes"("4.2.5"),
//        "org.fortheloss.sticknodespro"("4.2.5"),
//        "org.fortheloss.sticknodesbeta"("4.2.6")
//    )
//
//    extendWith("extensions/extension.rve")
//
//    execute {
//        println("=== [CustomFilterPatch] Starting execution (GUARANTEED METHOD) ===")
//
//        val targetMethod = figureFiltersInitFingerprint.method
//            ?: throw RuntimeException("[CustomFilterPatch] Could not find FigureFiltersToolTable.initialize()")
//
//        val implementation = targetMethod.implementation as? MutableMethodImplementation
//            ?: throw RuntimeException("[CustomFilterPatch] Method implementation is not mutable")
//
//        println("[CustomFilterPatch] ✓ Matched method: ${targetMethod.name}")
//        println("[CustomFilterPatch] ✓ Registers: ${implementation.registerCount}")
//
//        val registerCount = implementation.registerCount
//        val paramCount = targetMethod.parameters.size + 1
//        val p0Register = registerCount - paramCount
//
//        println("[CustomFilterPatch] ✓ p0 maps to v$p0Register (register $p0Register)")
//
//        // Find a SAFE injection point - after prologue
//        val instructions = implementation.instructions
//        var safeIndex = -1
//
//        println("[CustomFilterPatch] Scanning ${instructions.size} instructions for safe point...")
//
//        // Look for the FIRST instruction that's NOT a move or nop
//        // This ensures we're past the prologue
//        for (i in instructions.indices) {
//            val instruction = instructions[i]
//            val opcode = instruction.opcode
//
//            // Skip move and nop instructions at the start
//            if (opcode == Opcode.NOP ||
//                opcode?.name?.startsWith("MOVE") == true) {
//                continue
//            }
//
//            // Found first real instruction - inject AFTER it
//            safeIndex = i + 1
//            println("[CustomFilterPatch] ✓ Found safe point after ${opcode?.name} at index $i")
//            break
//        }
//
//        if (safeIndex == -1 || safeIndex < 2) {
//            safeIndex = 2 // Absolute minimum safe index
//            println("[CustomFilterPatch] ⚠ Using minimum safe index: 2")
//        }
//
//        println("[CustomFilterPatch] Injection point: index $safeIndex")
//
//        // Create the method reference for our hook
//        val hookMethodRef = ImmutableMethodReference(
//            "Lapp/revanced/extension/customfilters/TintFieldHook;",
//            "scheduleInstall",
//            listOf("Ljava/lang/Object;"),
//            "V"
//        )
//
//        println("[CustomFilterPatch] Created method reference: $hookMethodRef")
//
//        // Create the invoke instruction manually using BuilderInstruction35c
//        // This is the LOW-LEVEL way that ALWAYS works
//        val invokeInstruction = BuilderInstruction35c(
//            Opcode.INVOKE_STATIC,
//            1, // register count
//            p0Register, // registerC (first/only register)
//            0, 0, 0, 0, // registerD, E, F, G (unused)
//            hookMethodRef
//        )
//
//        println("[CustomFilterPatch] Created invoke instruction")
//
//        try {
//            // Add the instruction at the safe index
//            implementation.addInstruction(safeIndex, invokeInstruction)
//            println("[CustomFilterPatch] ✓ Instruction added at index $safeIndex")
//        } catch (e: Exception) {
//            println("[CustomFilterPatch] ✗ Failed to add instruction: ${e.message}")
//            e.printStackTrace()
//            throw e
//        }
//
//        // Verify the injection
//        println("[CustomFilterPatch] Verifying injection...")
//
//        val updatedInstructions = implementation.instructions
//        var foundAtIndex = -1
//
//        for (i in updatedInstructions.indices) {
//            val instr = updatedInstructions[i]
//
//            // Check if this is an invoke-static to our hook
//            if (instr.opcode == Opcode.INVOKE_STATIC) {
//                val invoke = instr as? Instruction35c
//                if (invoke != null) {
//                    val ref = invoke.reference as? MethodReference
//                    if (ref?.definingClass?.contains("TintFieldHook") == true) {
//                        foundAtIndex = i
//                        println("[CustomFilterPatch] ✓✓✓ HOOK FOUND at index $i ✓✓✓")
//                        println("[CustomFilterPatch]     Method: ${ref.name}")
//                        println("[CustomFilterPatch]     Class: ${ref.definingClass}")
//                        break
//                    }
//                }
//            }
//        }
//
//        if (foundAtIndex == -1) {
//            println("[CustomFilterPatch] ✗✗✗ CRITICAL: Hook NOT found after injection! ✗✗✗")
//
//            // Dump the area where we tried to inject
//            println("[CustomFilterPatch] Dumping instructions around injection point:")
//            val start = maxOf(0, safeIndex - 3)
//            val end = minOf(updatedInstructions.size - 1, safeIndex + 5)
//
//            for (i in start..end) {
//                val marker = if (i == safeIndex) " <-- EXPECTED HERE" else ""
//                val instr = updatedInstructions[i]
//                println("  [$i] ${instr.opcode?.name} ${instr}$marker")
//            }
//
//            throw RuntimeException("Injection verification failed!")
//        } else {
//            println("[CustomFilterPatch] ===================================")
//            println("[CustomFilterPatch] ✓ INJECTION SUCCESSFUL AND VERIFIED")
//            println("[CustomFilterPatch] ===================================")
//
//            // Show context
//            println("[CustomFilterPatch] Context:")
//            val start = maxOf(0, foundAtIndex - 2)
//            val end = minOf(updatedInstructions.size - 1, foundAtIndex + 2)
//
//            for (i in start..end) {
//                val marker = if (i == foundAtIndex) " <-- HOOK" else ""
//                val instr = updatedInstructions[i]
//                println("  [$i] ${instr.opcode?.name}$marker")
//            }
//        }
//
//        println("=== [CustomFilterPatch] Execution completed successfully ===")
//    }