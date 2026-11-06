package com.nihao.client.mixin;

//#if ZOOM && MC < V1_21_9
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Lets the mod add its own category to the Controls screen (versions with the `zoom` feature flag). */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> nihao$order() {
        throw new AssertionError();
    }
}
//#else
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Placeholder: the key-category hack is only for versions with the `zoom` flag below 26.x (they get the real file). */
@Mixin(LivingEntityRenderer.class)
public abstract class KeyMappingAccessor {}
//#endif
