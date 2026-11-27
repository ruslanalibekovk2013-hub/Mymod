package com.mackros;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Самотест внутри игры (команда «самотест»): определение запросов, сборка и проверка всех механизмов, требования, проекты, правки, скорость. */
public final class NeroSelfTest {
    private static int ok, total;

    private static void check(List<String> out, String name, boolean pass, String detail) {
        total++;
        if (pass) ok++;
        out.add((pass ? "[OK] " : "[ОШИБКА] ") + name + (detail == null || detail.isEmpty() ? "" : ": " + detail));
    }

    private static NeroSpec spec(String key, String req) {
        NeroSpec s = new NeroSpec();
        s.kind = "custom";
        s.name = key;
        s.desc = req;
        s.bp = NeroArchetypes.bp(key, req);
        return s;
    }

    public static List<String> run() {
        ok = 0; total = 0;
        List<String> out = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        int bot = mc.world != null ? mc.world.getBottomY() : -64, top = mc.world != null ? mc.world.getTopY() : 320;
        BlockPos base = new BlockPos(0, 70, 0);
        out.add("== Определение запросов каталога ==");
        for (NeroMega.V v : NeroMega.ALL) { String got = NeroArchetypes.detect(LocalProvider.norm("Создай " + v.name())); check(out, v.name(), v.key().equals(got), got); }
        for (NeroWeapons.W w : NeroWeapons.ALL) { String got = NeroArchetypes.detect(LocalProvider.norm("Создай " + w.name())); check(out, w.name(), w.key().equals(got), got); }
        for (NeroArsenal.WR w : NeroArsenal.ALL) { String got = NeroArchetypes.detect(LocalProvider.norm("Создай " + w.name())); check(out, w.name(), w.key().equals(got), got); }
        for (NeroMech.K x : NeroMech.ALL) { String got = NeroArchetypes.detect(LocalProvider.norm("Создай " + x.name())); check(out, x.name(), x.key().equals(got), got); }
        out.add("== Сборка и проверка механизмов ==");
        List<String> keys = new ArrayList<>();
        for (NeroMega.V v : NeroMega.ALL) keys.add(v.key());
        for (NeroWeapons.W w : NeroWeapons.ALL) keys.add(w.key());
        for (NeroArsenal.WR w : NeroArsenal.ALL) keys.add(w.key());
        for (NeroMech.K x : NeroMech.ALL) keys.add(x.key());
        for (String k : new String[]{"m_tower", "m_bridge", "m_house", "m_fort", "m_pyramid", "printer", "portalgun", "lift"}) keys.add(k);
        int min = Integer.MAX_VALUE, max = 0;
        for (String k : keys) {
            try {
                long t0 = System.nanoTime();
                NeroSpec s = spec(k, "Создай " + k);
                NeroBuildPlan p = NeroPlanner.plan(s, base);
                NeroValidator.Report r = NeroValidator.validate(p, bot, top);
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                boolean big = k.startsWith("x_") || k.startsWith("m_");
                if (big) { min = Math.min(min, p.cbs.size()); max = Math.max(max, p.cbs.size()); }
                check(out, k, r.ok() && !p.cbs.isEmpty(), "КБ " + p.cbs.size() + ", блоков " + p.solid() + ", " + ms + " мс" + (r.ok() ? "" : ", ошибок " + r.errors.size() + ": " + r.errors.get(0)));
            } catch (Exception e) { check(out, k, false, e.getClass().getSimpleName() + " " + e.getMessage()); }
        }
        if (max > 0) out.add("Командных блоков в больших механизмах: от " + min + " до " + max);
        out.add("== Предметы и блоки ==");
        for (NeroWeaponItems.WI w : NeroWeaponItems.ALL) check(out, "Предмет " + w.id(), NeroWeaponItems.item(w.id()) != Items.AIR, "");
        check(out, "Блок small_button", NeroBlocks.SMALL_BUTTON != null, "");
        check(out, "Блок digital_display", NeroBlocks.DISPLAY != null && NeroBlocks.DISPLAY_BE != null, "");
        out.add("== Требования ==");
        String long1 = "Создай 3D-принтер, у которого слева будет большая платформа с исходной постройкой, а справа отдельная платформа с уменьшенной копией из маленьких блоков. Добавь подвижную печатающую головку, направляющие, экран управления, частицы во время печати и сделай саму копию в несколько раз меньше оригинала.";
        List<NeroRequirements.Req> rq = NeroRequirements.extract(long1);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (NeroRequirements.Req r : rq) ids.add(r.id);
        check(out, "Извлечение требований", rq.size() >= 6 && ids.size() == rq.size(), "найдено " + rq.size());
        out.add("== Проекты и правки ==");
        try {
            NeroSpec s = spec("lift", "лифт на 6 этажей");
            String id = NeroProjects.create("самотест", s);
            s.desc = s.desc + " (изменено)";
            NeroProjects.save(s, "тест");
            int vs = NeroProjects.versions(id).size();
            NeroSpec r1 = NeroProjects.restore(id, 1);
            check(out, "Проекты: версии и восстановление", vs >= 2 && r1 != null, "версий " + vs);
            NeroProjects.delete(id);
            NeroSpec p2 = spec("lift", "лифт на 6 этажей");
            String before = String.valueOf(p2.bp.getAsJsonArray("ops").size());
            NeroPatch.Result res = NeroPatch.apply(p2, "{\"ops\":[{\"type\":\"replace_part\",\"part\":\"Площадки этажей\",\"ops\":[{\"op\":\"box\",\"x1\":0,\"y1\":0,\"z1\":-2,\"x2\":4,\"y2\":0,\"z2\":-1,\"block\":\"minecraft:stone\"}]}]}");
            check(out, "Точечная правка части", res.error == null && !before.equals(String.valueOf(p2.bp.getAsJsonArray("ops").size())), res.error == null ? "изменено: " + res.changed : res.error);
        } catch (Exception e) { check(out, "Проекты и правки", false, e.getClass().getSimpleName()); }
        out.add("== Скорость (локальная сборка) ==");
        try {
            String[][] cases = {{"простой: лифт на 3 этажа", "lift"}, {"средний: оружие", "w_laser"}, {"сложный: автобашня", "m_tower"}, {"сложный: шагающий робот", "x_walker"}};
            for (String[] c : cases) {
                long t0 = System.nanoTime();
                NeroBuildPlan p = NeroPlanner.plan(spec(c[1], "лифт на 3 этажа"), base);
                long a = (System.nanoTime() - t0) / 1_000_000L;
                t0 = System.nanoTime();
                NeroPlanner.plan(spec(c[1], "лифт на 3 этажа"), base);
                long b = (System.nanoTime() - t0) / 1_000_000L;
                out.add("  " + c[0] + ": первый раз " + a + " мс, повтор " + b + " мс, КБ " + p.cbs.size());
            }
        } catch (Exception e) { out.add("  замер не удался: " + e.getClass().getSimpleName()); }
        out.add("== Сеть ==");
        if (NeroConfig.remoteReady()) { NeroChatClient.test(m -> NeroHistory.log("Сетевой тест: " + m)); out.add("Сетевой тест запущен, результат появится в журнале NERO"); }
        else out.add("[ПРОПУЩЕНО] ключ модели не задан: тесты A и B (API и чат) надо запускать с ключом");
        out.add("ИТОГО: пройдено " + ok + " из " + total);
        try { Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("mackros").resolve("selftest.txt"), String.join("\n", out), StandardCharsets.UTF_8); } catch (Exception ignored) { }
        return out;
    }

    private NeroSelfTest() { }
}
