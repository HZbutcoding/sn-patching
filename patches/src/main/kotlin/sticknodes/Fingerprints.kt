package com.HZ.CustomFilters

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode




val figureFiltersInitFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters("Lcom/badlogic/gdx/graphics/g2d/TextureAtlas;", "Lcom/badlogic/gdx/graphics/g2d/TextureAtlas;", "Lcom/badlogic/gdx/scenes/scene2d/utils/Drawable;")

    custom { method, classDef ->
        val ok = classDef.type == "Lorg/fortheloss/sticknodes/animationscreen/modules/tooltables/FigureFiltersToolTable;" &&
                method.name == "initialize"
        if (ok) {
            println("== candidate fingerprint for ${classDef.type}#${method.name} (params=${method.parameterTypes})")
        }
        ok
    }

}

