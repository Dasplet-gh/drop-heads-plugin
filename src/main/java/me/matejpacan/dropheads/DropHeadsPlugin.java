package me.matejpacan.dropheads;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import org.bukkit.DyeColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import java.lang.reflect.Field;

import java.util.Random;
import java.util.UUID;

public final class DropHeadsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        createPluginFolder();
        loadConfig();
        getServer().getPluginManager().registerEvents(new MobDeathListener(), this);
    }

    private void createPluginFolder() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                getLogger().warning("Не удалось создать директорию для данных плагина!");
            }
        }
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveResource("config.yml", false);
            reloadConfig();
        }

        MobDeathListener.config = getConfig();
    }

    @Override
    public void onDisable() {}
}

class MobDeathListener implements Listener {

    static final double LOOTING_MULTIPLIER = 0.2;

    static FileConfiguration config;

    public static void CreateAndDropHead(EntityDeathEvent event,
                                         String name,
                                         double drop_chance,
                                         String texture) {
        // Получение убийцы
        Player killer = event.getEntity().getKiller();
        // Если нет убийцы, то голова не выпадает
        if (killer == null) {
           return;
        }
        // Получение уровня добычи у оружия убийцы
        int loot_level = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
        // Если выпал шанс, то голова дропается
        if (new Random().nextDouble() <= drop_chance + (LOOTING_MULTIPLIER * loot_level * drop_chance)) {
            // Объект голова и мета данные головы
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            // Создание профиля мета данных для головы
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(texture.getBytes()), name);
            profile.getProperties().put("textures", new Property("textures", texture));
            // Запись текстуры головы в мета данные
            try {
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
            // Установка мета данных голове
            head.setItemMeta(meta);
            // Добавление головы в дроп
            event.getDrops().add(head);
            // Воспроизведение торжества в связи с выпадением головы
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
            killer.sendTitle("§6Добыт Трофей", "§l§8Голова " + name, 10, 50, 20);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Игрок -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
        if (event.getEntity() instanceof Player) {
            // Получение убитого и убийцы
            Player killedPlayer = (Player) event.getEntity();
            Player killer = killedPlayer.getKiller();
            // Если есть убийца и выпал шанс, то голова дропается
            if (killer != null && new Random().nextDouble() <= config.getDouble("player_head_drop_chance")) {
                // Объект голова и мета данные головы
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skull_meta = (SkullMeta) head.getItemMeta();
                // Запись текстуры головы в мета данные
                skull_meta.setOwningPlayer(killedPlayer);
                // Установка мета данных голове
                head.setItemMeta(skull_meta);
                // Добавление головы в дроп
                event.getDrops().add(head);
                // Воспроизведение торжества в связи с выпадением головы
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
                killer.sendTitle("§6Добыт Трофей", "§l§8Голова " + killedPlayer.getName(), 10, 50, 20);
            }
        }

        // Боссы -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        // Мобы  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        // Свинья
        if (event.getEntityType() == EntityType.PIG) {
            CreateAndDropHead(event, "свиньи",
                    config.getDouble("pig_head_drop_chance"), config.getString("pig_head_texture"));
        }
        // Овца - 17
        if (event.getEntityType() == EntityType.SHEEP) {
            Sheep sheep = (Sheep) event.getEntity();
            String custom_name = sheep.getName();
            DyeColor sheep_dye_color = sheep.getColor();
            // -=-=-=-=-
            if (custom_name.equals("jeb_")) {
                CreateAndDropHead(event, "разноцветной овцы",
                        config.getDouble("jeb_sheep_head_drop_chance"),
                        config.getString("jeb_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.WHITE) {
                CreateAndDropHead(event, "белой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("white_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.ORANGE) {
                CreateAndDropHead(event, "оранжевой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("orange_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.MAGENTA) {
                CreateAndDropHead(event, "пурпурной овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("magenta_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.LIGHT_BLUE) {
                CreateAndDropHead(event, "голубой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("light_blue_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.YELLOW) {
                CreateAndDropHead(event, "жёлтой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("yellow_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.LIME) {
                CreateAndDropHead(event, "лаймовой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("lime_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.PINK) {
                CreateAndDropHead(event, "розовой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("pink_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.GRAY) {
                CreateAndDropHead(event, "серой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("gray_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.LIGHT_GRAY) {
                CreateAndDropHead(event, "светло-серой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("light_gray_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.CYAN) {
                CreateAndDropHead(event, "голубой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("cyan_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.PURPLE) {
                CreateAndDropHead(event, "фиолетовой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("purple_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.BLUE) {
                CreateAndDropHead(event, "синей овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("blue_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.BROWN) {
                CreateAndDropHead(event, "коричневой овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("brown_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.GREEN) {
                CreateAndDropHead(event, "зелёной овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("green_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.RED) {
                CreateAndDropHead(event, "красной овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("red_sheep_head_texture"));
            } else if (sheep_dye_color == DyeColor.BLACK) {
                CreateAndDropHead(event, "чёрной овцы",
                        config.getDouble("sheep_head_drop_chance"),
                        config.getString("black_sheep_head_texture"));
            }
        }
        // Грибная корова - 2
        if (event.getEntityType() == EntityType.MUSHROOM_COW) {
            MushroomCow mushroom_cow = (MushroomCow) event.getEntity();
            MushroomCow.Variant mushroom_cow_variant = mushroom_cow.getVariant();
            // -=-=-=-=-
            if (mushroom_cow_variant == MushroomCow.Variant.RED) {
                CreateAndDropHead(event, "красной грибной коровы",
                        config.getDouble("mooshroom_head_drop_chance"),
                        config.getString("red_mooshroom_head_drop_chance"));
            } else if (mushroom_cow_variant == MushroomCow.Variant.BROWN) {
                CreateAndDropHead(event, "коричневой грибной коровы",
                        config.getDouble("mooshroom_head_drop_chance"),
                        config.getString("brown_mooshroom_head_drop_chance"));
            }
        }
        // Корова
        if (event.getEntityType() == EntityType.COW) {
            CreateAndDropHead(event, "коровы",
                    config.getDouble("cow_head_drop_chance"), config.getString("cow_head_texture"));
        }
        // Курица
        if (event.getEntityType() == EntityType.CHICKEN) {
            CreateAndDropHead(event, "курицы",
                    config.getDouble("chicken_head_drop_chance"), config.getString("chicken_head_texture"));
        }
        // Оцелот
        if (event.getEntityType() == EntityType.OCELOT) {
            CreateAndDropHead(event, "оцелота",
                    config.getDouble("ocelot_head_drop_chance"), config.getString("ocelot_head_texture"));
        }
        // Кошка - 11
        if (event.getEntityType() == EntityType.CAT) {
            // Получение убийцы
            Player killer = event.getEntity().getKiller();
            // Если нет убийцы, то работа функции останавливается
            if (killer == null) {
                return;
            }
            // Сообщение
            killer.sendMessage("§5* " + killer.getName() + " §5зверски убивает кошку, и понижает свою репутацию");
            // Получение типа кошки
            Cat cat = (Cat) event.getEntity();
            Cat.Type cat_type = cat.getCatType();
            // -=-=-=-=-
            if (cat_type == Cat.Type.TABBY) {
                CreateAndDropHead(event, "полосатой кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("tabby_cat_head_texture"));
            } else if (cat_type == Cat.Type.BLACK) {
                CreateAndDropHead(event, "чёрно-белой кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("black_cat_head_texture"));
            } else if (cat_type == Cat.Type.RED) {
                CreateAndDropHead(event, "рыжей кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("red_cat_head_texture"));
            } else if (cat_type == Cat.Type.SIAMESE) {
                CreateAndDropHead(event, "сиамской кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("siamese_cat_head_texture"));
            } else if (cat_type == Cat.Type.BRITISH_SHORTHAIR) {
                CreateAndDropHead(event, "британской короткошерстной кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("british_shorthair_cat_head_texture"));
            } else if (cat_type == Cat.Type.CALICO) {
                CreateAndDropHead(event, "ситцевой кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("calico_cat_head_texture"));
            } else if (cat_type == Cat.Type.PERSIAN) {
                CreateAndDropHead(event, "персидской кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("persian_cat_head_texture"));
            } else if (cat_type == Cat.Type.RAGDOLL) {
                CreateAndDropHead(event, "кошки рэгдолл",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("ragdoll_cat_head_texture"));
            } else if (cat_type == Cat.Type.WHITE) {
                CreateAndDropHead(event, "белой кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("white_cat_head_texture"));
            } else if (cat_type == Cat.Type.JELLIE) {
                CreateAndDropHead(event, "кошки джелли",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("jellie_cat_head_texture"));
            } else if (cat_type == Cat.Type.ALL_BLACK) {
                CreateAndDropHead(event, "чёрной кошки",
                        config.getDouble("cat_head_drop_chance"),
                        config.getString("all_black_cat_head_texture"));
            }
        }
        // Летучая Мышь
        if (event.getEntityType() == EntityType.BAT) {
            CreateAndDropHead(event, "летучей мыши",
                    config.getDouble("bat_head_drop_chance"), config.getString("bat_head_texture"));
        }
        // Лошадь - 35
        if (event.getEntityType() == EntityType.HORSE) {
            Horse horse = (Horse) event.getEntity();
            Horse.Color horse_color = horse.getColor();
            Horse.Style horse_style = horse.getStyle();
            // -=-=-=-=-
            if (horse_color == Horse.Color.WHITE && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "белой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("white_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.CREAMY && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "буланой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("creamy_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.CHESTNUT && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "игреневой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("chestnut_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.BROWN && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "светло-гнедой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("brown_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.BLACK && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "вороной лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("black_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.GRAY && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "сивой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("gray_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.DARK_BROWN && horse_style == Horse.Style.NONE) {
                CreateAndDropHead(event, "темно-гнедой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("dark_brown_none_horse_head_texture"));
            } else if (horse_color == Horse.Color.WHITE && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "белой лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("white_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.CREAMY && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "буланой лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("creamy_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.CHESTNUT && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "игреневой лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("chestnut_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.BROWN && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "светло-гнедой лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("brown_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.BLACK && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "вороной лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("black_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.GRAY && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "сивой лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("gray_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.DARK_BROWN && horse_style == Horse.Style.WHITE) {
                CreateAndDropHead(event, "темно-гнедой лошади с чулками и звездой на морде",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("dark_brown_white_horse_head_texture"));
            } else if (horse_color == Horse.Color.WHITE && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "белой пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("white_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.CREAMY && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "буланой пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("creamy_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.CHESTNUT && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "игреневой пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("chestnut_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.BROWN && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "светло-гнедой пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("brown_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.BLACK && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "вороной пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("black_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.GRAY && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "сивой пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("gray_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.DARK_BROWN && horse_style == Horse.Style.WHITEFIELD) {
                CreateAndDropHead(event, "темно-гнедой пегой лошади",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("dark_brown_whitefield_horse_head_texture"));
            } else if (horse_color == Horse.Color.WHITE && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "белой лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("white_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.CREAMY && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "буланой лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("creamy_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.CHESTNUT && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "игреневой лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("chestnut_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.BROWN && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "светло-гнедой лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("brown_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.BLACK && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "вороной лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("black_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.GRAY && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "сивой лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("gray_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.DARK_BROWN && horse_style == Horse.Style.WHITE_DOTS) {
                CreateAndDropHead(event, "темно-гнедой лошади с белыми пятнами",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("dark_brown_white_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.WHITE && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "белой лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("white_black_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.CREAMY && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "буланой лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("creamy_black_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.CHESTNUT && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "игреневой лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("chestnut_black_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.BROWN && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "светло-гнедой лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("brown_black_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.BLACK && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "вороной лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("black_black_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.GRAY && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "сивой лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("gray_black_dots_horse_head_texture"));
            } else if (horse_color == Horse.Color.DARK_BROWN && horse_style == Horse.Style.BLACK_DOTS) {
                CreateAndDropHead(event, "темно-гнедой лошади с тёмной спиной",
                        config.getDouble("horse_head_drop_chance"),
                        config.getString("dark_brown_black_dots_horse_head_texture"));
            }
        }
        // Осёл
        if (event.getEntityType() == EntityType.DONKEY) {
            CreateAndDropHead(event, "осла",
                    config.getDouble("donkey_head_drop_chance"), config.getString("donkey_head_texture"));
        }
        // Мул
        if (event.getEntityType() == EntityType.MULE) {
            CreateAndDropHead(event, "мула",
                    config.getDouble("mule_head_drop_chance"), config.getString("mule_head_texture"));
        }
        // Кролик - 8
        if (event.getEntityType() == EntityType.RABBIT) {
            Rabbit rabbit = (Rabbit) event.getEntity();
            String custom_name = rabbit.getName();
            Rabbit.Type rabbit_type = rabbit.getRabbitType();
            // -=-=-=-=-
            if (custom_name.equals("Toast")) {
                CreateAndDropHead(event, "toast кролика",
                        config.getDouble("toast_rabbit_head_drop_chance"),
                        config.getString("toast_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.BROWN) {
                CreateAndDropHead(event, "коричневого кролика",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("brown_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.WHITE) {
                CreateAndDropHead(event, "белого кролика",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("white_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.BLACK) {
                CreateAndDropHead(event, "чёрного кролика",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("black_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.BLACK_AND_WHITE) {
                CreateAndDropHead(event, "чёрно-белого кролика",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("black_and_white_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.GOLD) {
                CreateAndDropHead(event, "золотистого кролика",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("gold_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.SALT_AND_PEPPER) {
                CreateAndDropHead(event, "бело-коричневого кролика",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("salt_and_pepper_rabbit_head_texture"));
            } else if (rabbit_type == Rabbit.Type.THE_KILLER_BUNNY) {
                CreateAndDropHead(event, "кролика-убийцы",
                        config.getDouble("rabbit_head_drop_chance"),
                        config.getString("killer_bunny_head_texture"));
            }
        }
        // Попугай - 5
        if (event.getEntityType() == EntityType.PARROT) {
            Parrot parrot = (Parrot) event.getEntity();
            Parrot.Variant parrot_variant = parrot.getVariant();
            // -=-=-=-=-
            if (parrot_variant == Parrot.Variant.RED) {
                CreateAndDropHead(event, "красного попугая",
                        config.getDouble("parrot_head_drop_chance"),
                        config.getString("red_parrot_head_texture"));
            } else if (parrot_variant == Parrot.Variant.BLUE) {
                CreateAndDropHead(event, "тёмно-синего попугая",
                        config.getDouble("parrot_head_drop_chance"),
                        config.getString("blue_parrot_head_texture"));
            } else if (parrot_variant == Parrot.Variant.GRAY) {
                CreateAndDropHead(event, "серого попугая",
                        config.getDouble("parrot_head_drop_chance"),
                        config.getString("gray_parrot_head_texture"));
            } else if (parrot_variant == Parrot.Variant.CYAN) {
                CreateAndDropHead(event, "голубого попугая",
                        config.getDouble("parrot_head_drop_chance"),
                        config.getString("cyan_parrot_head_texture"));
            } else if (parrot_variant == Parrot.Variant.GREEN) {
                CreateAndDropHead(event, "салатового попугая",
                        config.getDouble("parrot_head_drop_chance"),
                        config.getString("green_parrot_head_texture"));
            }
        }
        // Треска
        if (event.getEntityType() == EntityType.COD) {
            CreateAndDropHead(event, "трески",
                    config.getDouble("cod_head_drop_chance"), config.getString("cod_head_texture"));
        }
        // Лосось
        if (event.getEntityType() == EntityType.SALMON) {
            CreateAndDropHead(event, "лосося",
                    config.getDouble("salmon_head_drop_chance"), config.getString("salmon_head_texture"));
        }
        // Лиса - 2
        if (event.getEntityType() == EntityType.FOX) {
            Fox fox = (Fox) event.getEntity();
            Fox.Type fox_type = fox.getFoxType();
            // -=-=-=-=-
            if (fox_type == Fox.Type.RED) {
                CreateAndDropHead(event, "лисицы",
                        config.getDouble("fox_head_drop_chance"),
                        config.getString("red_fox_head_texture"));
            } else if (fox_type == Fox.Type.SNOW) {
                CreateAndDropHead(event, "песца",
                        config.getDouble("fox_head_drop_chance"),
                        config.getString("snow_fox_head_texture"));
            }
        }

        // Жители  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        // Зомби Жители  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    }
}
