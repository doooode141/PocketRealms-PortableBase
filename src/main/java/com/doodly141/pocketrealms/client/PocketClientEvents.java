package com.doodly141.pocketrealms.client;

import com.doodly141.pocketrealms.PocketRealms;
import com.doodly141.pocketrealms.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = PocketRealms.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PocketClientEvents {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.NAVIGATOR.get(), PocketNavigatorScreen::new);
    }

    private PocketClientEvents() {}
}
