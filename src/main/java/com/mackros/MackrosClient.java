package com.mackros;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class MackrosClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        NeroConfig.load();
        NeroHistory.load();
        Keys.register();
        ClientSetup.init();
        ClientTickEvents.END_CLIENT_TICK.register(Keys::tick);
        WorldRenderEvents.LAST.register(Renderer::render);
        HudRenderCallback.EVENT.register(Hud::render);
    }
}
