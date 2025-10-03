package com.HZ.CustomFilters

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode




val figureFiltersInitFingerprint = fingerprint {
    custom { _, classDef ->
        if (classDef.type.endsWith("FigureFiltersToolTable;")) {
            println("== Methods in ${classDef.type} ==")
            classDef.methods.forEach { method ->
                println("  ${method.name}(${method.parameterTypes.joinToString()}) -> ${method.returnType}")
            }
            true
        } else false
    }
}


//val figureFiltersInitFingerprint = fingerprint {
//    accessFlags(AccessFlags.PUBLIC)
//    returns("V")
//    parameters("Lcom/badlogic/gdx/graphics/g2d/TextureAtlas;", "Lcom/badlogic/gdx/graphics/g2d/TextureAtlas;", "Lcom/badlogic/gdx/scenes/scene2d/utils/Drawable;")
//
//    custom { method, classDef ->
//        classDef.type == "Lorg/fortheloss/sticknodes/animationscreen/modules/tooltables/FigureFiltersToolTable;" &&
//                method.name == "initialize"
//    }
//}

