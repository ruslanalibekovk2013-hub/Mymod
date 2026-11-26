import json, hashlib, zlib, struct, os, re
ROOT = os.path.dirname(os.path.abspath(__file__)) + '/src/main/'
RES = ROOT + 'resources/'
JAVA = ROOT + 'java/com/mackros/'
T = dict(EFFECT=0, PUSH=1, SPAWN=2, DROP=3, GEN=4, BREAK=5, GROW=6, ENV=7, STRIKE=8, BOOM=9, DMG=10,
         SERVICE=11, SENS=12, LOGIC=13, TP=14, XP=15, FX=16, ITEM=17, LIGHT=18)
GROUPS = ["Зоны эффектов", "Ветер и пады", "Спавнеры", "Дропперы", "Генераторы", "Добыча, рост", "Погода, взрыв",
          "Урон, сервис", "Датчики/логика", "Телепорт/предм", "Свет и FX"]
GRP_EN = ["Effect zones", "Wind and pads", "Spawners", "Droppers", "Generators", "Mining and growth", "Weather and blasts",
          "Damage and service", "Sensors and logic", "Teleport and items", "Light and FX"]
# (C, E, I) recipe materials per group
MAT = [("lapis_lazuli", "redstone", "iron_ingot"), ("feather", "redstone", "iron_ingot"), ("bone", "redstone", "gold_ingot"),
       ("hopper", "redstone", "copper_ingot"), ("stone", "redstone", "gold_ingot"), ("iron_ingot", "redstone", "diamond"),
       ("gunpowder", "redstone", "diamond"), ("gold_ingot", "redstone", "iron_ingot"), ("quartz", "redstone", "iron_ingot"),
       ("ender_pearl", "redstone", "iron_ingot"), ("glowstone_dust", "redstone", "iron_ingot")]
D = []  # dict(id, grp, type, a, b, c, e, f, rs, ru, en, desc, x)


def add(id, grp, typ, a, b, c, e, f, rs, ru, en, desc, x):
    D.append(dict(id=id, grp=grp, type=T[typ], a=a, b=b, c=c, e=e, f=f, rs=rs, ru=ru, en=en, desc=desc, x=x))


# ---- group 0: effects (mask 1 players, 0 all living)
Z = [("speed", "Скорость", "Speed", "sugar", 0), ("jump_boost", "Прыгучесть", "Jump", "rabbit_foot", 0),
     ("regeneration", "Регенерация", "Regeneration", "ghast_tear", 0), ("night_vision", "Ночное зрение", "Night vision", "golden_carrot", 0),
     ("haste", "Спешка", "Haste", "golden_pickaxe", 0), ("resistance", "Защита", "Resistance", "shield", 0),
     ("fire_resistance", "Огнестойкость", "Fire resist", "magma_cream", 0), ("water_breathing", "Подводное дыхание", "Water breathing", "pufferfish", 0),
     ("slow_falling", "Плавное падение", "Slow falling", "phantom_membrane", 0), ("strength", "Сила", "Strength", "blaze_powder", 0),
     ("invisibility", "Невидимость", "Invisibility", "fermented_spider_eye", 0), ("absorption", "Поглощение", "Absorption", "golden_apple", 0),
     ("saturation", "Сытость", "Saturation", "cooked_beef", 0), ("luck", "Удача", "Luck", "rabbit_hide", 0),
     ("dolphins_grace", "Грация дельфина", "Dolphin", "nautilus_shell", 0), ("conduit_power", "Сила источника", "Conduit", "heart_of_the_sea", 0),
     ("health_boost", "Больше здоровья", "Health boost", "apple", 0), ("hero_of_the_village", "Герой деревни", "Hero", "emerald", 0),
     ("glowing", "Подсветка", "Glowing", "spectral_arrow", 0), ("levitation", "Левитация", "Levitation", "shulker_shell", 0)]
for ef, ru, en, x, amp in Z:
    add("zone_" + ef, 0, "EFFECT", "minecraft:" + ef, amp, 0, 1, 0, False, "Зона: " + ru, "Zone: " + en,
        "Выдаёт игрокам рядом эффект «%s». Радиус и сила растут с уровнем." % ru, x)
TR = [("slowness", "Замедление", "Slowness", "cobweb"), ("weakness", "Слабость", "Weakness", "poisonous_potato"),
      ("blindness", "Слепота", "Blindness", "ink_sac"), ("hunger", "Голод", "Hunger", "rotten_flesh"),
      ("poison", "Яд", "Poison", "spider_eye"), ("wither", "Иссушение", "Wither", "wither_rose"),
      ("mining_fatigue", "Усталость", "Fatigue", "prismarine_shard"), ("nausea", "Тошнота", "Nausea", "red_mushroom"),
      ("darkness", "Тьма", "Darkness", "sculk"), ("glowing", "Подсветка всех", "Glow all", "glow_ink_sac")]
for ef, ru, en, x in TR:
    add("trap_" + ef, 0, "EFFECT", "minecraft:" + ef, 0, 0, 0, 0, False, "Ловушка: " + ru, "Trap: " + en,
        "Накладывает «%s» на всех живых существ рядом." % ru, x)
add("trap_levitation", 0, "EFFECT", "minecraft:levitation", 0, 0, 4, 0, False, "Ловушка: Подъём", "Trap: Lift",
    "Поднимает в воздух всех мобов рядом (не игроков).", "chorus_fruit")

# ---- group 1: push (a mode, b strength*10, c range, e mask, f pad)
P = [("wind_tunnel", "Аэротруба", "Wind tunnel", "dir", 6, 6, 5, 0, "slime_block", "Дует вперёд (по грани), толкает мобов и предметы"),
     ("wind_gale", "Шквал", "Gale", "dir", 12, 8, 5, 0, "phantom_membrane", "Сильный ветер вперёд"),
     ("wind_storm", "Ураган", "Hurricane", "dir", 25, 10, 5, 0, "wind_charge_stub", "Очень сильный ветер вперёд"),
     ("lift_gentle", "Мягкий лифт", "Gentle lift", "dir", 3, 10, 0, 0, "string", "Плавный поток вдоль грани (ставь вверх)"),
     ("lift_fast", "Быстрый лифт", "Fast lift", "dir", 9, 12, 0, 0, "elytra", "Быстрый поток вдоль грани"),
     ("pad_jump_low", "Пад: Прыжок", "Pad: Hop", "dir", 10, 1, 0, 1, "leather", "Подбрасывает вдоль грани"),
     ("pad_jump_mid", "Пад: Взлёт", "Pad: Lift", "dir", 18, 1, 0, 1, "rabbit_foot", "Подбрасывает сильнее"),
     ("pad_jump_high", "Пад: Катапульта", "Pad: Catapult", "dir", 28, 1, 0, 1, "piston", "Мощный выброс"),
     ("pad_dash", "Пад: Рывок", "Pad: Dash", "dir", 14, 1, 0, 1, "sugar", "Рывок вдоль грани (ставь сбоку)"),
     ("pad_cannon", "Пад: Пушка", "Pad: Cannon", "dir", 40, 1, 0, 1, "tnt", "Выстрел игрока вдоль грани"),
     ("pad_sling", "Пад: Праща", "Pad: Sling", "dir", 60, 1, 0, 1, "fire_charge", "Предельный выстрел"),
     ("vortex", "Вихрь", "Vortex", "pull", 5, 8, 5, 0, "ender_eye", "Притягивает всё к блоку"),
     ("black_hole", "Чёрная дыра", "Black hole", "pull", 12, 12, 5, 0, "end_crystal", "Сильно притягивает"),
     ("repulsor", "Отталкиватель", "Repulsor", "repel", 6, 8, 0, 0, "iron_block", "Отталкивает всех живых"),
     ("shockwave", "Ударная волна", "Shockwave", "repel", 20, 6, 0, 0, "gunpowder", "Мощный отброс"),
     ("whirl", "Карусель", "Whirl", "spin", 6, 6, 5, 0, "lead", "Закручивает вокруг блока"),
     ("centrifuge", "Центрифуга", "Centrifuge", "spin", 14, 8, 0, 0, "cauldron", "Быстро раскручивает"),
     ("mob_wall", "Стена от мобов", "Mob wall", "repel", 8, 6, 2, 0, "iron_bars", "Отталкивает враждебных мобов"),
     ("mob_blower", "Мободув", "Mob blower", "dir", 8, 8, 4, 0, "wheat", "Толкает мобов (не игроков) вперёд"),
     ("item_blower", "Предметодув", "Item blower", "dir", 8, 8, 3, 0, "hopper", "Толкает предметы вперёд"),
     ("item_pull", "Дальний сбор", "Far pull", "pull", 4, 12, 3, 0, "ender_pearl", "Притягивает предметы издалека")]
for id, ru, en, a, b, c, e, f, x, d in P:
    if x == "wind_charge_stub": x = "heavy_core" if False else "dried_kelp_block"
    add(id, 1, "PUSH", a, b, c, e, f, False, ru, en, d + ". Сила и дальность растут с уровнем.", x)

# ---- group 2: spawners
S = [("cow", "Корова", "Cow", "leather", 400, 10), ("pig", "Свинья", "Pig", "porkchop", 400, 10), ("sheep", "Овца", "Sheep", "white_wool", 400, 10),
     ("chicken", "Курица", "Chicken", "feather", 300, 12), ("rabbit", "Кролик", "Rabbit", "rabbit_hide", 300, 12),
     ("horse", "Лошадь", "Horse", "saddle", 600, 6), ("fox", "Лиса", "Fox", "sweet_berries", 500, 6), ("cat", "Кошка", "Cat", "cod", 500, 6),
     ("parrot", "Попугай", "Parrot", "pumpkin_seeds", 500, 6), ("bee", "Пчела", "Bee", "honeycomb", 400, 8),
     ("villager", "Житель", "Villager", "emerald", 800, 5), ("iron_golem", "Железный голем", "Iron golem", "iron_block", 900, 3),
     ("wolf", "Волк", "Wolf", "lead", 500, 6), ("zombie", "Зомби", "Zombie", "rotten_flesh", 300, 8),
     ("skeleton", "Скелет", "Skeleton", "arrow", 300, 8), ("spider", "Паук", "Spider", "string", 300, 8),
     ("creeper", "Крипер", "Creeper", "gunpowder", 400, 5), ("slime", "Слизень", "Slime", "slime_ball", 400, 6),
     ("witch", "Ведьма", "Witch", "glass_bottle", 600, 4), ("husk", "Кадавр", "Husk", "sand", 300, 8),
     ("stray", "Зимогор", "Stray", "snowball", 300, 8), ("drowned", "Утопленник", "Drowned", "kelp", 300, 8),
     ("pillager", "Разбойник", "Pillager", "crossbow", 500, 5), ("enderman", "Эндермен", "Enderman", "ender_eye", 600, 4),
     ("blaze", "Ифрит", "Blaze", "blaze_rod", 500, 4)]
for ent, ru, en, x, b, c in S:
    add("spawn_" + ent, 2, "SPAWN", "minecraft:" + ent, b, c, 0, 0, False, "Спавн: " + ru, "Spawn: " + en,
        "Спавнит существо «%s» перед гранью (лимит рядом %d). Редстоун выключает. Уровень ускоряет." % (ru, c), x)

# ---- group 3: droppers (b count, c base period)
DR = [("cobblestone", "Булыжник", "Cobblestone", 4, 60), ("dirt", "Земля", "Dirt", 4, 60), ("sand", "Песок", "Sand", 4, 60),
      ("gravel", "Гравий", "Gravel", 4, 60), ("oak_log", "Дерево", "Oak log", 2, 80), ("stick", "Палки", "Sticks", 4, 60),
      ("torch", "Факелы", "Torches", 2, 80), ("bread", "Хлеб", "Bread", 1, 100), ("arrow", "Стрелы", "Arrows", 2, 80),
      ("string", "Нити", "String", 2, 80), ("snowball", "Снежки", "Snowballs", 4, 40), ("egg", "Яйца", "Eggs", 2, 80),
      ("bone_meal", "Костная мука", "Bone meal", 2, 80), ("wheat_seeds", "Семена", "Seeds", 4, 60), ("coal", "Уголь", "Coal", 1, 120),
      ("iron_nugget", "Жел. самородки", "Iron nuggets", 2, 120), ("gold_nugget", "Зол. самородки", "Gold nuggets", 2, 120),
      ("redstone", "Редстоун", "Redstone", 2, 100), ("glowstone_dust", "Светопыль", "Glowstone", 2, 100),
      ("slime_ball", "Слизь", "Slime", 1, 120), ("tnt", "ТНТ", "TNT", 1, 200)]
for it, ru, en, b, c in DR:
    add("drop_" + it, 3, "DROP", "minecraft:" + it, b, c, 0, 0, False, "Дроп: " + ru, "Drop: " + en,
        "Выбрасывает %d× «%s» перед гранью. Редстоун выключает. Уровень ускоряет." % (b, ru), it)
add("drop_xp", 3, "XP", "", 3, 40, 0, 0, False, "Дроп: Опыт", "Drop: XP", "Создаёт шарики опыта перед гранью. Уровень = больше опыта.", "experience_bottle")

# ---- group 4: generators (b base period)
G = [("cobble", "cobblestone", "Булыжник", "Cobble", 30), ("stone", "stone", "Камень", "Stone", 40), ("obsidian", "obsidian", "Обсидиан", "Obsidian", 120),
     ("ice", "ice", "Лёд", "Ice", 40), ("snow", "snow_block", "Снег", "Snow", 40), ("sand", "sand", "Песок", "Sand", 30),
     ("gravel", "gravel", "Гравий", "Gravel", 30), ("water", "water", "Вода", "Water", 60), ("lava", "lava", "Лава", "Lava", 100),
     ("netherrack", "netherrack", "Незерак", "Netherrack", 30), ("dirt", "dirt", "Земля", "Dirt", 30), ("glass", "glass", "Стекло", "Glass", 40)]
GX = {"water": "prismarine", "lava": "magma_block"}
for id, blk, ru, en, b in G:
    add("gen_" + id, 4, "GEN", "minecraft:" + blk, b, 0, 0, 0, False, "Ген: " + ru, "Gen: " + en,
        "Создаёт «%s» в блоке перед гранью, если там пусто. Уровень ускоряет." % ru, GX.get(id, blk))

# ---- group 5: breakers & growers (b reach/radius, c mode 0 line 1 area)
B = [("drill", "any", 0, 8, "Бур", "Drill", "iron_pickaxe", "Ломает первый блок по линии грани (до 8)"),
     ("tree_cutter", "log", 1, 4, "Лесоруб", "Lumberjack", "iron_axe", "Валит брёвна вокруг точки перед гранью"),
     ("ore_miner", "ore", 1, 5, "Рудокоп", "Ore miner", "diamond_pickaxe", "Добывает руды вокруг точки перед гранью"),
     ("harvester", "plant", 1, 4, "Жнец", "Harvester", "iron_hoe", "Собирает созревшие культуры и цветы"),
     ("leaf_cutter", "leaves", 1, 4, "Листорез", "Leaf cutter", "shears", "Срезает листву вокруг"),
     ("digger", "soft", 0, 6, "Землекоп", "Digger", "iron_shovel", "Копает мягкий грунт по линии"),
     ("stone_crusher", "stone", 1, 3, "Дробилка", "Crusher", "stone_pickaxe", "Дробит камень вокруг")]
for id, a, c, b, ru, en, x, d in B:
    add("break_" + id, 5, "BREAK", a, b, c, 0, 0, False, ru, en, d + ". Уровень ускоряет. Редстоун выключает.", x)
add("grow_crops", 5, "GROW", "rand", 4, 0, 0, 0, False, "Ускоритель роста", "Growth booster", "Ускоряет рост культур, саженцев, тростника в радиусе 4.", "wheat")
add("grow_bonemeal", 5, "GROW", "bone", 3, 0, 0, 0, False, "Автоудобрение", "Auto bonemeal", "Автоматически удобряет растения в радиусе 3.", "bone_meal")

# ---- group 6: env, strikes, booms
for id, a, ru, en, x in [("clear", "clear", "Ясно", "Clear sky", "sunflower"), ("rain", "rain", "Дождь", "Rain", "prismarine_crystals"),
                         ("thunder", "thunder", "Гроза", "Thunder", "lightning_rod"), ("day", "day", "День", "Day", "clock"),
                         ("night", "night", "Ночь", "Night", "coal"), ("noon", "noon", "Полдень", "Noon", "glowstone"),
                         ("midnight", "midnight", "Полночь", "Midnight", "obsidian")]:
    add("env_" + id, 6, "ENV", a, 0, 0, 0, 0, False, "Погода: " + ru, "Env: " + en, "Поддерживает «%s» в мире. Редстоун выключает." % ru, x)
add("strike_bolt", 6, "STRIKE", "real", 100, 0, 0, 0, True, "Молния", "Lightning", "По редстоун-сигналу бьёт молнией на расстоянии вперёд.", "copper_block")
add("strike_fx", 6, "STRIKE", "fx", 60, 0, 0, 0, True, "Молния-шоу", "Lightning FX", "Безопасная молния без урона и огня (по сигналу).", "amethyst_shard")
add("boom_tnt", 6, "BOOM", "tnt", 100, 0, 0, 0, True, "Взрыв", "Blast", "По сигналу взрывает точку перед гранью (ломает блоки).", "tnt")
add("boom_safe", 6, "BOOM", "safe", 100, 0, 0, 0, True, "Хлопушка", "Safe blast", "Взрыв без разрушения блоков (по сигналу).", "fire_charge")
add("boom_mega", 6, "BOOM", "mega", 160, 0, 0, 0, True, "Мегавзрыв", "Mega blast", "Очень мощный взрыв по сигналу.", "end_crystal")
add("boom_fire", 6, "BOOM", "fire", 100, 0, 0, 0, True, "Огненный взрыв", "Fire blast", "Взрыв с огнём, без разрушения блоков.", "flint_and_steel")

# ---- group 7: damage & service (b damage / amount)
for id, a, b, e, ru, en, x, d in [("hurt_hostile", "hurt", 2, 2, "Шип: враги", "Spike: hostile", "iron_sword", "Наносит урон враждебным мобам рядом"),
                                  ("hurt_all", "hurt", 1, 0, "Шип: все", "Spike: all", "bone_block", "Наносит урон всем живым рядом"),
                                  ("hurt_mobs", "hurt", 2, 4, "Шип: мобы", "Spike: mobs", "stone_sword", "Наносит урон мобам, но не игрокам"),
                                  ("burn_zone", "fire", 4, 0, "Зона огня", "Fire zone", "blaze_powder", "Поджигает всех живых рядом"),
                                  ("burn_hostile", "fire", 4, 2, "Огонь: враги", "Fire: hostile", "fire_charge", "Поджигает врагов рядом"),
                                  ("freeze_zone", "freeze", 0, 0, "Зона холода", "Freeze zone", "packed_ice", "Замораживает всех живых рядом"),
                                  ("reaper", "hurt", 1000, 2, "Жнец", "Reaper", "diamond_sword", "Убивает врагов рядом с выпадением добычи"),
                                  ("void_zone", "void", 0, 4, "Зона пустоты", "Void zone", "end_stone", "Бесследно удаляет мобов (не игроков)")]:
    add(id, 7, "DMG", a, b, 0, e, 0, False, ru, en, d + ". Радиус растёт с уровнем.", x)
for id, a, ru, en, x, d in [("feed", "feed", "Кормушка", "Feeder", "bread", "Кормит игрока на площадке"),
                            ("heal", "heal", "Лекарь", "Healer", "golden_apple", "Лечит игрока на площадке"),
                            ("xp", "xp", "Опыт-пад", "XP pad", "experience_bottle", "Даёт опыт игроку на площадке"),
                            ("air", "air", "Кислород", "Oxygen", "glass_bottle", "Наполняет запас воздуха"),
                            ("extinguish", "extinguish", "Огнетушитель", "Extinguisher", "sponge", "Тушит горящего игрока"),
                            ("repair", "repair", "Ремонтник", "Repair pad", "anvil", "Чинит предметы в инвентаре"),
                            ("cleanse", "cleanse", "Очиститель", "Cleanser", "honey_bottle", "Снимает все эффекты"),
                            ("restore", "restore", "Полное восст.", "Full restore", "totem_of_undying", "Лечит, кормит, даёт воздух"),
                            ("spawn", "spawn", "Точка возрожд.", "Respawn point", "red_bed", "Ставит точку возрождения на блок")]:
    add("svc_" + id, 7, "SERVICE", a, 1, 0, 1, 0, False, ru, en, d + ". Встань рядом сверху. Уровень = сила.", x)

# ---- group 8: sensors & logic
for id, a, ru, en, x in [("player", "player", "Игрок", "Player", "player_head"), ("hostile", "hostile", "Враг", "Hostile", "zombie_head"),
                         ("passive", "passive", "Животное", "Animal", "wheat"), ("mob", "mob", "Моб", "Mob", "skeleton_skull"),
                         ("item", "item", "Предмет", "Item", "hopper"), ("villager", "villager", "Житель", "Villager", "emerald"),
                         ("rain", "rain", "Дождь", "Rain", "lapis_lazuli"), ("thunder", "thunder", "Гроза", "Thunder", "lightning_rod"),
                         ("day", "day", "День", "Day", "sunflower"), ("night", "night", "Ночь", "Night", "coal"),
                         ("burn", "burn", "Горящий", "Burning", "blaze_rod"), ("sneak", "sneak", "Крадущийся", "Sneaking", "leather_boots")]:
    add("sens_" + id, 8, "SENS", a, 0, 0, 0, 0, False, "Датчик: " + ru, "Sensor: " + en,
        "Даёт редстоун-сигнал 15, когда условие «%s» верно. Радиус растёт с уровнем." % ru, x)
for id, a, b, c, ru, en, x, d in [("clock", "clock", 80, 4, "Часы", "Clock", "clock", "Импульсы; уровень = частота. Редстоун выключает"),
                                  ("fclock", "clock", 16, 2, "Быстрые часы", "Fast clock", "repeater", "Частые импульсы. Редстоун выключает"),
                                  ("sclock", "clock", 400, 10, "Медл. часы", "Slow clock", "comparator", "Редкие длинные импульсы"),
                                  ("rand", "rand", 10, 0, "Случайность", "Random", "dropper", "Случайно вкл/выкл; уровень = шанс"),
                                  ("delay", "delay", 0, 0, "Задержка", "Delay", "observer", "Повторяет вход с задержкой 4×уровень тиков"),
                                  ("toggle", "toggle", 0, 0, "Триггер", "Toggle", "lever", "Переключается при каждом новом сигнале"),
                                  ("not", "not", 0, 0, "Инвертор", "Inverter", "redstone_torch", "Выход включён, когда нет входа"),
                                  ("pulse", "pulse", 0, 0, "Импульсатор", "Pulser", "stone_button", "Сигнал превращает в импульс 10×уровень тиков"),
                                  ("daycycle", "daycycle", 0, 0, "Дневной цикл", "Day cycle", "daylight_detector", "Сигнал, пока в мире день")]:
    add("logic_" + id, 8, "LOGIC", a, b, c, 0, 0, False, ru, en, d + ".", x)

# ---- group 9: teleport & item
for id, a, ru, en, x, d in [("up", "up", "Вверх на поверхность", "To surface", "feather", "Телепортирует на поверхность над собой"),
                            ("spawn", "spawn", "К точке спавна", "To spawn", "red_bed", "Отправляет к точке возрождения"),
                            ("random", "random", "Случайный ТП", "Random TP", "chorus_fruit", "В случайную точку (радиус 16×уровень)"),
                            ("far", "far", "Дальний ТП", "Far TP", "ender_eye", "Вперёд по грани на 6×уровень блоков"),
                            ("sky", "sky", "Небесный ТП", "Sky TP", "firework_rocket", "Высоко вверх с плавным падением")]:
    add("tp_" + id, 9, "TP", a, 0, 0, 0, 0, False, "ТП: " + ru if len(ru) < 12 else ru, "TP: " + en,
        d + ". Встань на блок сверху.", x)
for id, a, ru, en, x, d in [("void", "void", "Утилизатор", "Item void", "magma_block", "Удаляет предметы вокруг"),
                            ("collect", "collect", "Сборщик", "Collector", "hopper", "Стягивает предметы в точку перед гранью"),
                            ("smelt", "smelt", "Печь-пад", "Smelter", "furnace", "Переплавляет предметы, лежащие сверху")]:
    add("item_" + id, 9, "ITEM", a, 0, 0, 0, 0, False, ru, en, d + ". Радиус растёт с уровнем.", x)

# ---- group 10: light & fx
for id, ru, en, x in [("white", "Белый", "White", "white_dye"), ("red", "Красный", "Red", "red_dye"), ("green", "Зелёный", "Green", "green_dye"),
                      ("blue", "Синий", "Blue", "blue_dye"), ("yellow", "Жёлтый", "Yellow", "yellow_dye"), ("purple", "Фиолетовый", "Purple", "purple_dye"),
                      ("orange", "Оранжевый", "Orange", "orange_dye"), ("cyan", "Бирюзовый", "Cyan", "cyan_dye")]:
    add("light_" + id, 10, "LIGHT", "", 0, 0, 0, 0, False, "Свет: " + ru, "Light: " + en, "Лампа с регулировкой яркости (ПКМ = уровень).", x)
for id, p, ru, en, x in [("flame", "flame", "Пламя", "Flame", "torch"), ("smoke", "smoke", "Дым", "Smoke", "coal"), ("heart", "heart", "Сердца", "Hearts", "poppy"),
                         ("portal", "portal", "Портал", "Portal", "obsidian"), ("enchant", "enchant", "Чары", "Enchant", "lapis_lazuli"),
                         ("endrod", "end_rod", "Стержни", "End rods", "end_rod"), ("snow", "snowflake", "Снегопад", "Snow", "snowball"),
                         ("bubble", "bubble", "Пузыри", "Bubbles", "kelp"), ("note", "note", "Ноты", "Notes", "note_block"),
                         ("firework", "firework", "Фейерверк", "Firework", "firework_star"), ("happy", "happy_villager", "Радость", "Happy", "bone_meal"),
                         ("soul", "soul", "Души", "Souls", "soul_sand")]:
    add("fx_" + id, 10, "FX", "minecraft:" + p, 0, 0, 0, 0, False, "FX: " + ru, "FX: " + en, "Выпускает частицы «%s» перед гранью. Уровень = плотность." % ru, x)

# ---- NEW: доп. спавнеры (20 новых существ, 1.20.4)
NS = [("snow_golem", "Снеговик", "Snow golem", "carved_pumpkin", 600, 4),
      ("polar_bear", "Белый медведь", "Polar bear", "powder_snow_bucket", 600, 4),
      ("panda", "Панда", "Panda", "bamboo", 600, 4),
      ("turtle", "Черепаха", "Turtle", "scute", 500, 6),
      ("dolphin", "Дельфин", "Dolphin", "tropical_fish", 500, 5),
      ("axolotl", "Аксолотль", "Axolotl", "axolotl_bucket", 500, 6),
      ("frog", "Лягушка", "Frog", "frogspawn", 400, 8),
      ("goat", "Коза", "Goat", "goat_horn", 500, 6),
      ("camel", "Верблюд", "Camel", "hay_block", 700, 4),
      ("allay", "Аллэй", "Allay", "amethyst_shard", 700, 3),
      ("vex", "Векс", "Vex", "iron_sword", 600, 4),
      ("phantom", "Фантом", "Phantom", "phantom_membrane", 600, 4),
      ("ghast", "Гаст", "Ghast", "ghast_tear", 800, 3),
      ("magma_cube", "Магмовый куб", "Magma cube", "magma_cream", 500, 6),
      ("piglin", "Пиглин", "Piglin", "gold_ingot", 500, 5),
      ("wither_skeleton", "Иссохший скелет", "Wither skeleton", "wither_skeleton_skull", 700, 4),
      ("guardian", "Страж", "Guardian", "prismarine_shard", 600, 4),
      ("shulker", "Шалкер", "Shulker", "shulker_shell", 900, 2),
      ("ravager", "Разоритель", "Ravager", "bone", 1000, 2),
      ("evoker", "Чаровед", "Evoker", "totem_of_undying", 900, 2)]
for ent, ru, en, x, b, c in NS:
    add("spawn_" + ent, 2, "SPAWN", "minecraft:" + ent, b, c, 0, 0, False, "Спавн: " + ru, "Spawn: " + en,
        "Спавнит существо «%s» перед гранью (лимит рядом %d). Редстоун выключает. Уровень ускоряет." % (ru, c), x)

# ---- NEW: доп. дропперы (6 новых ресурсов)
NDR = [("diamond", "Алмаз", "Diamond", 1, 260), ("emerald", "Изумруд", "Emerald", 1, 220),
       ("ender_pearl", "Жемчуг Края", "Ender pearl", 1, 200), ("honeycomb", "Соты", "Honeycomb", 1, 140),
       ("quartz", "Кварц", "Quartz", 2, 110), ("lapis_lazuli", "Лазурит", "Lapis lazuli", 2, 110)]
for it, ru, en, b, c in NDR:
    add("drop_" + it, 3, "DROP", "minecraft:" + it, b, c, 0, 0, False, "Дроп: " + ru, "Drop: " + en,
        "Выбрасывает %d× «%s» перед гранью. Редстоун выключает. Уровень ускоряет." % (b, ru), it)

# ---- NEW: доп. генераторы блоков (4 новых)
NG = [("deepslate", "deepslate", "Глубинный сланец", "Deepslate", 40),
      ("blackstone", "blackstone", "Чёрный камень", "Blackstone", 40),
      ("soul_sand", "soul_sand", "Песок душ", "Soul sand", 35),
      ("honey", "honey_block", "Мёд", "Honey", 90)]
for id, blk, ru, en, b in NG:
    add("gen_" + id, 4, "GEN", "minecraft:" + blk, b, 0, 0, 0, False, "Ген: " + ru, "Gen: " + en,
        "Создаёт «%s» в блоке перед гранью, если там пусто. Уровень ускоряет." % ru, blk)

# ---- NEW: доп. FX-частицы (3 новых, атмосферные/босс-тематика)
NFX = [("totem", "totem_of_undying", "Тотем", "Totem", "totem_of_undying"),
       ("dragon", "dragon_breath", "Дыхание дракона", "Dragon breath", "dragon_breath"),
       ("sonic", "sonic_boom", "Соник-удар", "Sonic boom", "echo_shard")]
for id, p, ru, en, x in NFX:
    add("fx_" + id, 10, "FX", "minecraft:" + p, 0, 0, 0, 0, False, "FX: " + ru, "FX: " + en,
        "Выпускает частицы «%s» перед гранью. Уровень = плотность." % ru, x)

# ---- NEW: доп. цвета ламп (4 новых, дополняет палитру)
NL = [("pink", "Розовый", "Pink", "pink_dye"), ("lime", "Лаймовый", "Lime", "lime_dye"),
      ("magenta", "Пурпурный", "Magenta", "magenta_dye"), ("black", "Чёрный", "Black", "black_dye")]
for id, ru, en, x in NL:
    add("light_" + id, 10, "LIGHT", "", 0, 0, 0, 0, False, "Свет: " + ru, "Light: " + en,
        "Лампа с регулировкой яркости (ПКМ = уровень).", x)

# ---- NEW: доп. физика/пады (3 новых)
add("push_tornado", 1, "PUSH", "spin", 18, 9, 5, 0, False, "Торнадо", "Tornado",
    "Мощно закручивает всё вокруг блока и немного поднимает. Сила и дальность растут с уровнем.", "feather")
add("push_item_vortex", 1, "PUSH", "pull", 8, 12, 3, 0, False, "Магнит предметов", "Item magnet",
    "Притягивает только предметы издалека, мощнее обычного сборщика. Сила и дальность растут с уровнем.", "chest")
add("push_float_zone", 1, "PUSH", "dir", 2, 8, 1, 0, False, "Невесомость", "Float zone",
    "Создаёт зону мягкой левитации для игроков (ставь вверх). Сила и дальность растут с уровнем.", "firework_rocket")

# ---- sanity
ids = [d['id'] for d in D]
assert len(ids) == len(set(ids)), [i for i in ids if ids.count(i) > 1]
seen = {}
for d in D:
    k = (d['grp'], d['x'])
    assert k not in seen, (k, d['id'], seen[k])
    seen[k] = d['id']
print("machines:", len(D))

# ---- MechDefs.java
def js(s): return json.dumps(s, ensure_ascii=False)
out = ["package com.mackros;", "", "import java.util.ArrayList;", "import java.util.List;", "",
       "/** Сгенерировано gen.py: каталог механизмов Mackros. */", "public final class MechDefs {",
       "    public static final class Def {",
       "        public final String id, a, ru, desc; public final int grp, type, b, c, e, f; public final boolean rs;",
       "        Def(String id, int grp, int type, String a, int b, int c, int e, int f, boolean rs, String ru, String desc) {",
       "            this.id = id; this.grp = grp; this.type = type; this.a = a; this.b = b; this.c = c; this.e = e; this.f = f; this.rs = rs; this.ru = ru; this.desc = desc;",
       "        }", "    }"]
for k, v in T.items(): out.append("    public static final int T_%s = %d;" % (k, v))
out.append("    public static final String[] GROUPS = {%s};" % ", ".join(js(g) for g in GROUPS))
out.append("    public static final Def[] ALL = {")
for d in D:
    out.append("        new Def(%s, %d, %d, %s, %d, %d, %d, %d, %s, %s, %s)," % (js(d['id']), d['grp'], d['type'], js(d['a']), d['b'], d['c'], d['e'], d['f'],
                                                                         'true' if d['rs'] else 'false', js(d['ru']), js(d['desc'])))
out += ["    };", "    public static List<Def> inGroup(int g) { List<Def> l = new ArrayList<>(); for (Def d : ALL) if (d.grp == g) l.add(d); return l; }",
        "    private MechDefs() { }", "}"]
open(JAVA + 'MechDefs.java', 'w', encoding='utf-8').write("\n".join(out) + "\n")

# ---- PNG
def png(path, px):  # px: 16x16 list of (r,g,b,a)
    raw = b''.join(b'\x00' + b''.join(bytes(p) for p in row) for row in px)
    def ch(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + ch(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + ch(b'IDAT', zlib.compress(raw)) + ch(b'IEND', b''))

PAL = [(70, 130, 220), (120, 200, 230), (90, 170, 80), (210, 160, 70), (140, 100, 200), (180, 120, 70),
       (210, 70, 60), (200, 90, 150), (60, 190, 170), (150, 90, 220), (230, 200, 70)]
def sh(c, k): return tuple(max(0, min(255, int(v * k))) for v in c)

TEX = RES + 'assets/mackros/textures/block/'
for d in D:
    h = hashlib.sha1(d['id'].encode()).digest()
    base = PAL[d['grp']]
    hue = [(1.0, 1.0, 1.0), (0.85, 1.05, 0.9), (1.05, 0.9, 0.9), (0.9, 0.95, 1.1)][h[0] % 4]
    base = tuple(max(0, min(255, int(base[i] * hue[i]))) for i in range(3))
    dark, mid, lite = sh(base, 0.35), sh(base, 0.65), sh(base, 1.15)
    glyph = [[(h[1 + r] >> c) & 1 for c in range(4)] for r in range(8)]
    # side: panel with border, rivets, stripe and small glyph
    for kind in ('side', 'top', 'top_on'):
        px = [[(*mid, 255)] * 16 for _ in range(16)]
        for y in range(16):
            for x in range(16):
                n = (h[(x * 7 + y * 13) % 20] % 9) - 4
                c = tuple(max(0, min(255, v + n)) for v in (mid if kind != 'top_on' else sh(base, 0.9)))
                if x in (0, 15) or y in (0, 15): c = dark
                elif x in (1, 14) or y in (1, 14): c = lite if kind != 'side' else sh(base, 0.8)
                px[y][x] = (*c, 255)
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): px[y][x] = (*dark, 255)
        gc = (255, 255, 255) if kind == 'top_on' else (lite if kind == 'side' else (25, 25, 30))
        if kind == 'side':
            for x in range(3, 13): px[7][x] = px[8][x] = (*dark, 255)
            for r in range(3):
                for c in range(3):
                    if glyph[r + 2][c]: px[4 + r * 3][5 + c * 3] = (*gc, 255)
        else:
            for r in range(8):
                for c in range(8):
                    cc = c if c < 4 else 7 - c
                    if glyph[r][cc]: px[4 + r][4 + c] = (*gc, 255)
            # output arrow on the top face
            for (x, y) in ((7, 2), (8, 2), (6, 3), (9, 3)): px[y][x] = (255, 255, 255, 255) if kind == 'top_on' else (*lite, 255)
        png(TEX + "%s_%s.png" % (d['id'], kind), px)

# ---- models / blockstates / loot / recipes
def wr(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8').write(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")

ROT = {'down': dict(x=180), 'up': {}, 'north': dict(x=90), 'south': dict(x=90, y=180), 'west': dict(x=90, y=270), 'east': dict(x=90, y=90)}
for d in D:
    i = d['id']
    for suf, top in (('', 'top'), ('_on', 'top_on')):
        wr(RES + 'assets/mackros/models/block/%s%s.json' % (i, suf), {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": "mackros:block/%s_%s" % (i, top), "bottom": "mackros:block/%s_side" % i, "side": "mackros:block/%s_side" % i}})
    wr(RES + 'assets/mackros/models/item/%s.json' % i, {"parent": "mackros:block/" + i})
    var = {}
    for f, r in ROT.items():
        for lit in ('false', 'true'):
            var["facing=%s,lit=%s" % (f, lit)] = dict(model="mackros:block/%s%s" % (i, '_on' if lit == 'true' else ''), **r)
    wr(RES + 'assets/mackros/blockstates/%s.json' % i, {"variants": var})
    wr(RES + 'data/mackros/loot_tables/blocks/%s.json' % i, {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "mackros:" + i}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    c, e, ii = MAT[d['grp']]
    wr(RES + 'data/mackros/recipes/%s.json' % i, {"type": "minecraft:crafting_shaped", "pattern": ["CXC", "EME", "III"], "key": {
        "C": {"item": "minecraft:" + c}, "X": {"item": "minecraft:" + d['x']}, "E": {"item": "minecraft:" + e},
        "M": {"item": "mackros:module"}, "I": {"item": "minecraft:" + ii}}, "result": {"item": "mackros:" + i, "count": 1}})

# ---- tag + lang
tp = RES + 'data/minecraft/tags/blocks/mineable/pickaxe.json'
tag = json.load(open(tp, encoding='utf-8'))
for d in D:
    v = "mackros:" + d['id']
    if v not in tag['values']: tag['values'].append(v)
wr(tp, tag)
for lang, key in (('ru_ru', 'ru'), ('en_us', 'en')):
    p = RES + 'assets/mackros/lang/%s.json' % lang
    L = json.load(open(p, encoding='utf-8'))
    for d in D: L['block.mackros.' + d['id']] = d[key]
    L['itemGroup.mackros.machines'] = 'Mackros: Механизмы' if lang == 'ru_ru' else 'Mackros: Machines'
    wr(p, L)
print("ok")
