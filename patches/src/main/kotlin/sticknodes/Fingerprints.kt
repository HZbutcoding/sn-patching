package com.HZ.CustomFilters

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

val figureFiltersInitFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters("Lcom/badlogic/gdx/graphics/g2d/TextureAtlas;", "Lcom/badlogic/gdx/graphics/g2d/TextureAtlas;", "Lcom/badlogic/gdx/scenes/scene2d/utils/Drawable;")
    opcodes(
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IF_LT
    )
    custom { method, classDef ->
        classDef.type == "Lorg/fortheloss/sticknodes/animationscreen/modules/tooltables/FigureFiltersToolTable;" &&
                method.name == "<init>"
    }
}


