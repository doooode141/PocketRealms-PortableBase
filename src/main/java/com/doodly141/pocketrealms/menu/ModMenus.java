package com.doodly141.pocketrealms.menu;

import com.doodly141.pocketrealms.PocketRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PocketRealms.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PocketNavigatorMenu>> NAVIGATOR =
            MENUS.register("navigator", () ->
                    new MenuType<>(PocketNavigatorMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
