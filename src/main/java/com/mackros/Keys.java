package com.mackros;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Keys {
    private static final Map<String, KeyBinding> K = new LinkedHashMap<>();

    private static void reg(String id, int key) {
        K.put(id, KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.mackros." + id, InputUtil.Type.KEYSYM, key, "category.mackros")));
    }

    public static void register() {
        reg("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        reg("pos1", GLFW.GLFW_KEY_Z);
        reg("pos2", GLFW.GLFW_KEY_X);
        reg("copy", GLFW.GLFW_KEY_B);
        reg("cut", GLFW.GLFW_KEY_N);
        reg("paste", GLFW.GLFW_KEY_V);
        reg("rotate", GLFW.GLFW_KEY_R);
        reg("flip", GLFW.GLFW_KEY_G);
        reg("undo", GLFW.GLFW_KEY_U);
        reg("redo", GLFW.GLFW_KEY_I);
        reg("preview", GLFW.GLFW_KEY_P);
        reg("fill", GLFW.GLFW_KEY_J);
        reg("clear", GLFW.GLFW_KEY_K);
        reg("sphere", GLFW.GLFW_KEY_H);
        reg("sym", GLFW.GLFW_KEY_Y);
        reg("nxp", GLFW.GLFW_KEY_RIGHT);
        reg("nxm", GLFW.GLFW_KEY_LEFT);
        reg("nzm", GLFW.GLFW_KEY_UP);
        reg("nzp", GLFW.GLFW_KEY_DOWN);
        reg("nyp", GLFW.GLFW_KEY_PAGE_UP);
        reg("nym", GLFW.GLFW_KEY_PAGE_DOWN);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        Editor.tickQueue(mc);
        for (Map.Entry<String, KeyBinding> e : K.entrySet()) {
            while (e.getValue().wasPressed()) run(mc, e.getKey());
        }
    }

    private static void run(MinecraftClient mc, String id) {
        int s = Editor.step;
        switch (id) {
            case "menu" -> mc.setScreen(new MackrosScreen());
            case "pos1" -> Editor.setPos(mc, 1);
            case "pos2" -> Editor.setPos(mc, 2);
            case "copy" -> Editor.copy(mc);
            case "cut" -> Editor.cut(mc);
            case "paste" -> Editor.paste(mc);
            case "rotate" -> Editor.rotate();
            case "flip" -> Editor.flip(true);
            case "undo" -> Editor.undo(mc);
            case "redo" -> Editor.redo(mc);
            case "preview" -> Editor.togglePreview();
            case "fill" -> Editor.fill(mc, Editor.held(mc));
            case "clear" -> Editor.fill(mc, Blocks.AIR.getDefaultState());
            case "sphere" -> Editor.sphere(mc, false);
            case "sym" -> Editor.cycleSym(mc);
            case "nxp" -> Editor.nudge(s, 0, 0);
            case "nxm" -> Editor.nudge(-s, 0, 0);
            case "nzp" -> Editor.nudge(0, 0, s);
            case "nzm" -> Editor.nudge(0, 0, -s);
            case "nyp" -> Editor.nudge(0, s, 0);
            case "nym" -> Editor.nudge(0, -s, 0);
            default -> {}
        }
    }
}
