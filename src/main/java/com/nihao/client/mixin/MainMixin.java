package com.nihao.client.mixin;

import com.nihao.client.SignIn;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Main.class)
public abstract class MainMixin {
    // Runs before Minecraft builds its user from the launch arguments: the session window and Ely.by sign-in rewrite them.
    @ModifyVariable(method = "main([Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private static String[] nihao$signIn(String[] args) {
        return SignIn.run(args);
    }
}
