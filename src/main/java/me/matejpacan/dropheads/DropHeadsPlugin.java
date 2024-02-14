package me.matejpacan.dropheads;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import org.bukkit.DyeColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public final class DropHeadsPlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        MobDeathListener.config = getConfig();
        getServer().getPluginManager().registerEvents(new MobDeathListener(), this);
        Objects.requireNonNull(getCommand("gethead")).setExecutor(this);
        getLogger().info("Плагин успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин успешно отключен!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        // Проверка, является ли отправитель игроком
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько игрок может использовать команду!");
            return true;
        }
        // Проверка, является ли отправитель оператором сервера
        if (!sender.isOp()) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        // Проверка имени команды
        if (command.getName().equalsIgnoreCase("gethead")) {
            // Проверяем, был ли передан хотя бы один аргумент
            if (args.length == 0) {
                sender.sendMessage("§6Используйте: /gethead <head_tag>");
                return true;
            }
            // Выдача головы
            Player player = (Player) sender;
            player.sendMessage("Hello!" + args[0]);

            return true;
        }

        return false;
    }
}

class MobDeathListener implements Listener {

    static FileConfiguration config;

    public static ItemStack CreateCustomHeadOrGetVanillaHead(String texture_name) {
        // Текстура головы
        String texture = config.getString(texture_name + "_head_texture");
        assert texture != null;
        // Если голова ванильная, то получаем её и всё
        if (texture.startsWith("minecraft:")) {
            Material material = Material.getMaterial(texture);
            assert material != null;
            return new ItemStack(material);
        }
        // Имя головы
        String name = config.getString(texture_name + "_head_name");
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
        // Возврат головы
        return head;
    }

    public static void DropHead(EntityDeathEvent event, String drop_chance_name, String texture_name) {
        // Получение убийцы
        Player killer = event.getEntity().getKiller();
        Ageable ageable = (Ageable) event.getEntity();
        // Если нет убийцы или моб ребёнок, то голова не выпадает
        if (killer == null || !ageable.isAdult()) {
           return;
        }
        // Получение уровня добычи у оружия убийцы
        int loot_level = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
        // Шанс выпадения головы
        double original_drop_chance = config.getDouble(drop_chance_name + "_head_drop_chance");
        double drop_chance = original_drop_chance +
                (config.getDouble("looting_multiplier") * loot_level * original_drop_chance);
        // Если выпал шанс, то голова дропается
        if (new Random().nextDouble() <= drop_chance) {
            // Добавление головы в дроп
            event.getDrops().add(CreateCustomHeadOrGetVanillaHead(texture_name));
            // Имя головы
            String name = config.getString(texture_name + "_head_name");
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

        // Существа  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        switch (event.getEntityType()) {

            // Боссы -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case ENDER_DRAGON: // Дракон
                DropHead(event, "ender_dragon", "ender_dragon");
                break;

            case WITHER: // Визер
                DropHead(event, "wither", "wither");
                break;

            // Обычные Мобы  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case PIG: // Свинья
                DropHead(event, "pig", "pig");
                break;

            case SHEEP: // Овца - 17
                Sheep sheep = (Sheep) event.getEntity();
                DyeColor sheep_dye_color = sheep.getColor();

                if (sheep.getName().equals("jeb_")) {
                    DropHead(event, "sheep", "jeb_sheep");
                }

                if (sheep_dye_color == null) { break; }
                switch (sheep_dye_color) {
                    case WHITE:
                        DropHead(event, "sheep", "white_sheep");
                        break;
                    case ORANGE:
                        DropHead(event, "sheep", "orange_sheep");
                        break;
                    case MAGENTA:
                        DropHead(event, "sheep", "magenta_sheep");
                        break;
                    case LIGHT_BLUE:
                        DropHead(event, "sheep", "light_blue_sheep");
                        break;
                    case YELLOW:
                        DropHead(event, "sheep", "yellow_sheep");
                        break;
                    case LIME:
                        DropHead(event, "sheep", "lime_sheep");
                        break;
                    case PINK:
                        DropHead(event, "sheep", "pink_sheep");
                        break;
                    case GRAY:
                        DropHead(event, "sheep", "gray_sheep");
                        break;
                    case LIGHT_GRAY:
                        DropHead(event, "sheep", "light_gray_sheep");
                        break;
                    case CYAN:
                        DropHead(event, "sheep", "cyan_sheep");
                        break;
                    case PURPLE:
                        DropHead(event, "sheep", "purple_sheep");
                        break;
                    case BLUE:
                        DropHead(event, "sheep", "blue_sheep");
                        break;
                    case BROWN:
                        DropHead(event, "sheep", "brown_sheep");
                        break;
                    case GREEN:
                        DropHead(event, "sheep", "green_sheep");
                        break;
                    case RED:
                        DropHead(event, "sheep", "red_sheep");
                        break;
                    case BLACK:
                        DropHead(event, "sheep", "black_sheep");
                        break;
                    default:
                        break;
                }
                break;

            case SPIDER: // Паук
                DropHead(event, "spider", "spider");
                break;

            case SKELETON: // Скелет
                DropHead(event, "skeleton", "skeleton");
                break;

            case ZOMBIE: // Зомби
                DropHead(event, "zombie", "zombie");
                break;

            case CREEPER: // Крипер - 2
                Creeper creeper = (Creeper) event.getEntity();

                if (creeper.isPowered()) {
                    DropHead(event, "powered_creeper", "powered_creeper");
                } else {
                    DropHead(event, "creeper", "creeper");
                }
                break;

            case COW: // Корова
                DropHead(event, "cow", "cow");
                break;

            case CHICKEN: // Курица
                DropHead(event, "chicken", "chicken");
                break;

            case GHAST: // Гаст
                DropHead(event, "ghast", "ghast");
                break;

            case SLIME: // Слизень
                Slime slime = (Slime) event.getEntity();

                if (slime.getSize() == 1) {
                    DropHead(event, "slime", "slime");
                }
                break;

            case SQUID: // Спрут
                DropHead(event, "squid", "squid");
                break;

            case WOLF: // Волк - 17
                Wolf wolf = (Wolf) event.getEntity();

                if (wolf.isTamed()) {
                    DyeColor wolf_collar_color = wolf.getCollarColor();

                    switch (wolf_collar_color) {
                        case WHITE:
                            DropHead(event, "tamed_wolf", "white_tamed_wolf");
                            break;
                        case ORANGE:
                            DropHead(event, "tamed_wolf", "orange_tamed_wolf");
                            break;
                        case MAGENTA:
                            DropHead(event, "tamed_wolf", "magenta_tamed_wolf");
                            break;
                        case LIGHT_BLUE:
                            DropHead(event, "tamed_wolf", "light_blue_tamed_wolf");
                            break;
                        case YELLOW:
                            DropHead(event, "tamed_wolf", "yellow_tamed_wolf");
                            break;
                        case LIME:
                            DropHead(event, "tamed_wolf", "lime_tamed_wolf");
                            break;
                        case PINK:
                            DropHead(event, "tamed_wolf", "pink_tamed_wolf");
                            break;
                        case GRAY:
                            DropHead(event, "tamed_wolf", "gray_tamed_wolf");
                            break;
                        case LIGHT_GRAY:
                            DropHead(event, "tamed_wolf", "light_gray_tamed_wolf");
                            break;
                        case CYAN:
                            DropHead(event, "tamed_wolf", "cyan_tamed_wolf");
                            break;
                        case PURPLE:
                            DropHead(event, "tamed_wolf", "purple_tamed_wolf");
                            break;
                        case BLUE:
                            DropHead(event, "tamed_wolf", "blue_tamed_wolf");
                            break;
                        case BROWN:
                            DropHead(event, "tamed_wolf", "brown_tamed_wolf");
                            break;
                        case GREEN:
                            DropHead(event, "tamed_wolf", "green_tamed_wolf");
                            break;
                        case RED:
                            DropHead(event, "tamed_wolf", "red_tamed_wolf");
                            break;
                        case BLACK:
                            DropHead(event, "tamed_wolf", "black_tamed_wolf");
                            break;
                        default:
                            break;
                    }
                } else {
                    DropHead(event, "wolf", "wolf");
                }
                break;

            case MUSHROOM_COW: // Грибная корова - 2
                MushroomCow mushroom_cow = (MushroomCow) event.getEntity();
                MushroomCow.Variant mushroom_cow_variant = mushroom_cow.getVariant();

                switch (mushroom_cow_variant) {
                    case RED:
                        DropHead(event, "red_mooshroom", "red_mooshroom");
                        break;
                    case BROWN:
                        DropHead(event, "brown_mooshroom", "brown_mooshroom");
                        break;
                    default:
                        break;
                }
                break;

            case SNOWMAN: // Снежный голем - 2
                Snowman snowman = (Snowman) event.getEntity();

                if (snowman.isDerp()) {
                    DropHead(event, "snowman", "snowman");
                } else {
                    DropHead(event, "snow_golem", "snow_golem");
                }
                break;

            case BLAZE: // Всполох
                DropHead(event, "blaze", "blaze");
                break;

            case MAGMA_CUBE: // Магма Куб
                MagmaCube magma_cube = (MagmaCube) event.getEntity();

                if (magma_cube.getSize() == 1) {
                    DropHead(event, "magma_cube", "magma_cube");
                }
                break;

            case OCELOT: // Оцелот
                DropHead(event, "ocelot", "ocelot");
                break;

            case CAT: // Кошка - 11
                Player killer = event.getEntity().getKiller();
                if (killer == null) {
                    return;
                }

                killer.sendMessage(
                        "§5* " + killer.getName() + " §5зверски убивает кошку, и понижает свою репутацию");

                Cat cat = (Cat) event.getEntity();
                Cat.Type cat_type = cat.getCatType();

                switch (cat_type) {
                    case TABBY:
                        DropHead(event, "cat", "tabby_cat");
                        break;
                    case BLACK:
                        DropHead(event, "cat", "black_cat");
                        break;
                    case RED:
                        DropHead(event, "cat", "red_cat");
                        break;
                    case SIAMESE:
                        DropHead(event, "cat", "siamese_cat");
                        break;
                    case BRITISH_SHORTHAIR:
                        DropHead(event, "cat", "british_shorthair_cat");
                        break;
                    case CALICO:
                        DropHead(event, "cat", "calico_cat");
                        break;
                    case PERSIAN:
                        DropHead(event, "cat", "persian_cat");
                        break;
                    case RAGDOLL:
                        DropHead(event, "cat", "ragdoll_cat");
                        break;
                    case WHITE:
                        DropHead(event, "cat", "white_cat");
                        break;
                    case JELLIE:
                        DropHead(event, "cat", "jellie_cat");
                        break;
                    case ALL_BLACK:
                        DropHead(event, "cat", "all_black_cat");
                        break;
                    default:
                        break;
                }
                break;

            case IRON_GOLEM: // Железный голем
                DropHead(event, "iron_golem", "iron_golem");
                break;

            case BAT: // Летучая Мышь
                DropHead(event, "bat", "bat");
                break;

            case WITCH: // Ведьма
                DropHead(event, "witch", "witch");
                break;

            case HORSE: // Лошадь - 35
                Horse horse = (Horse) event.getEntity();
                Horse.Color horse_color = horse.getColor();
                Horse.Style horse_style = horse.getStyle();

                switch (horse_color) {
                    case WHITE:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "white_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "white_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "white_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "white_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "white_black_dots_horse");
                                break;
                        }
                        break;

                    case CREAMY:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "creamy_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "creamy_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "creamy_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "creamy_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "creamy_black_dots_horse");
                                break;
                        }
                        break;

                    case CHESTNUT:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "chestnut_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "chestnut_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "chestnut_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "chestnut_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "chestnut_black_dots_horse");
                                break;
                        }
                        break;

                    case BROWN:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "brown_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "brown_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "brown_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "brown_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "brown_black_dots_horse");
                                break;
                        }
                        break;

                    case BLACK:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "black_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "black_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "black_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "black_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "black_black_dots_horse");
                                break;
                        }
                        break;

                    case GRAY:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "gray_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "gray_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "gray_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "gray_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "gray_black_dots_horse");
                                break;
                        }
                        break;

                    case DARK_BROWN:
                        switch (horse_style) {
                            case NONE:
                                DropHead(event, "horse", "dark_brown_none_horse");
                                break;
                            case WHITE:
                                DropHead(event, "horse", "dark_brown_white_horse");
                                break;
                            case WHITEFIELD:
                                DropHead(event, "horse", "dark_brown_whitefield_horse");
                                break;
                            case WHITE_DOTS:
                                DropHead(event, "horse", "dark_brown_white_dots_horse");
                                break;
                            case BLACK_DOTS:
                                DropHead(event, "horse", "dark_brown_black_dots_horse");
                                break;
                        }
                        break;
                }
                break;

            case SKELETON_HORSE: // Лошадь-скелет
                DropHead(event, "skeleton_horse", "skeleton_horse");
                break;

            case ZOMBIE_HORSE: // Лошадь-зомби
                DropHead(event, "zombie_horse", "zombie_horse");
                break;

            case DONKEY: // Осёл
                DropHead(event, "donkey", "donkey");
                break;

            case MULE: // Мул
                DropHead(event, "mule", "mule");
                break;

            case ENDERMAN: // Эндермен
                DropHead(event, "enderman", "enderman");
                break;

            case ENDERMITE: // Эндермит
                DropHead(event, "endermite", "endermite");
                break;

            case RABBIT: // Кролик - 8
                Rabbit rabbit = (Rabbit) event.getEntity();
                Rabbit.Type rabbit_type = rabbit.getRabbitType();

                if (rabbit.getName().equals("Toast")) {
                    DropHead(event, "toast_rabbit", "toast_rabbit");
                }

                switch (rabbit_type) {
                    case BROWN:
                        DropHead(event, "rabbit", "brown_rabbit");
                        break;
                    case WHITE:
                        DropHead(event, "rabbit", "white_rabbit");
                        break;
                    case BLACK:
                        DropHead(event, "rabbit", "black_rabbit");
                        break;
                    case BLACK_AND_WHITE:
                        DropHead(event, "rabbit", "black_and_white_rabbit");
                        break;
                    case GOLD:
                        DropHead(event, "rabbit", "gold_rabbit");
                        break;
                    case SALT_AND_PEPPER:
                        DropHead(event, "rabbit", "salt_and_pepper_rabbit");
                        break;
                    case THE_KILLER_BUNNY:
                        DropHead(event, "killer_bunny", "killer_bunny");
                        break;
                    default:
                        break;
                }
                break;

            case CAVE_SPIDER: // Пещерный паук
                DropHead(event, "cave_spider", "cave_spider");
                break;

            case GUARDIAN: // Страж
                DropHead(event, "guardian", "guardian");
                break;

            case ELDER_GUARDIAN: // Древний Страж
                DropHead(event, "elder_guardian", "elder_guardian");
                break;

            case SILVERFISH: // Чешуйница
                DropHead(event, "silverfish", "silverfish");
                break;

            case SHULKER: // Шалкер
                DropHead(event, "shulker", "shulker");
                break;

            case POLAR_BEAR: // Белый медведь
                DropHead(event, "polar_bear", "polar_bear");
                break;

            case HUSK: // Кадавр
                DropHead(event, "husk", "husk");
                break;

            case STRAY: // Зимогор
                DropHead(event, "stray", "stray");
                break;

            case LLAMA: // Лама - 4
                Llama llama = (Llama) event.getEntity();
                Llama.Color llama_color = llama.getColor();

                switch (llama_color) {
                    case WHITE:
                        DropHead(event, "llama", "white_llama");
                        break;
                    case GRAY:
                        DropHead(event, "llama", "gray_llama");
                        break;
                    case CREAMY:
                        DropHead(event, "llama", "creamy_llama");
                        break;
                    case BROWN:
                        DropHead(event, "llama", "brown_llama");
                        break;
                    default:
                        break;
                }
                break;

            case EVOKER: // Заклинатель
                DropHead(event, "evoker", "evoker");
                break;

            case VEX: // Вредина
                DropHead(event, "vex", "vex");
                break;

            case VINDICATOR: // Поборник
                DropHead(event, "vindicator", "vindicator");
                break;

            case PARROT:
                Parrot parrot = (Parrot) event.getEntity();
                Parrot.Variant parrot_variant = parrot.getVariant();

                switch (parrot_variant) {
                    case RED:
                        DropHead(event, "parrot", "red_parrot");
                        break;
                    case BLUE:
                        DropHead(event, "parrot", "blue_parrot");
                        break;
                    case GRAY:
                        DropHead(event, "parrot", "gray_parrot");
                        break;
                    case CYAN:
                        DropHead(event, "parrot", "cyan_parrot");
                        break;
                    case GREEN:
                        DropHead(event, "parrot", "green_parrot");
                        break;
                    default:
                        break;
                }
                break;

            case COD: // Треска
                DropHead(event, "cod", "cod");
                break;

            case SALMON: // Лосось
                DropHead(event, "salmon", "salmon");
                break;

            case TROPICAL_FISH: // Тропическая рыба
                DropHead(event, "tropical_fish", "tropical_fish");
                break;

            case PUFFERFISH: // Иглобрюх
                DropHead(event, "pufferfish", "pufferfish");
                break;

            case TURTLE: // Черепаха
                DropHead(event, "turtle", "turtle");
                break;

            case DOLPHIN: // Дельфин
                DropHead(event, "dolphin", "dolphin");
                break;

            case DROWNED: // Утопленник
                DropHead(event, "drowned", "drowned");
                break;

            case PHANTOM: // Фантом
                DropHead(event, "phantom", "phantom");
                break;

            case FOX: // Лиса - 2
                Fox fox = (Fox) event.getEntity();
                Fox.Type fox_type = fox.getFoxType();

                switch (fox_type) {
                    case RED:
                        DropHead(event, "fox", "red_fox");
                        break;
                    case SNOW:
                        DropHead(event, "fox", "snow_fox");
                        break;
                    default:
                        break;
                }
                break;

            case WANDERING_TRADER:  // Странствующий торговец
                DropHead(event, "wandering_trader", "wandering_trader");
                break;

            case TRADER_LLAMA: // Лама странствующего торговца - 4
                TraderLlama traderllama = (TraderLlama) event.getEntity();
                TraderLlama.Color traderllama_color = traderllama.getColor();

                switch (traderllama_color) {
                    case WHITE:
                        DropHead(event, "trader_llama", "white_trader_llama");
                        break;
                    case GRAY:
                        DropHead(event, "trader_llama", "gray_trader_llama");
                        break;
                    case CREAMY:
                        DropHead(event, "trader_llama", "creamy_trader_llama");
                        break;
                    case BROWN:
                        DropHead(event, "trader_llama", "brown_trader_llama");
                        break;
                    default:
                        break;
                }
                break;

            case PANDA: // Панда - 7
                Panda panda = (Panda) event.getEntity();
                Panda.Gene panda_main_gene = panda.getMainGene();
                Panda.Gene panda_hidden_gene = panda.getHiddenGene();

                switch (panda_main_gene) {
                    case AGGRESSIVE:
                        DropHead(event, "panda", "aggressive_panda");
                        break;
                    case LAZY:
                        DropHead(event, "panda", "lazy_panda");
                        break;
                    case WORRIED:
                        DropHead(event, "panda", "worried_panda");
                        break;
                    case PLAYFUL:
                        DropHead(event, "panda", "playful_panda");
                        break;
                    case WEAK:
                        if (panda_hidden_gene == Panda.Gene.WEAK) {
                            DropHead(event, "panda", "weak_panda");
                        } else {
                            DropHead(event, "panda", "normal_panda");
                        }
                        break;
                    case BROWN:
                        if (panda_hidden_gene == Panda.Gene.BROWN) {
                            DropHead(event, "brown_panda", "brown_panda");
                        } else {
                            DropHead(event, "panda", "normal_panda");
                        }
                        break;
                    default:
                        DropHead(event, "panda", "normal_panda");
                        break;
                }
                break;

            case PILLAGER: // Разбойник
                DropHead(event, "pillager", "pillager");
                break;

            case RAVAGER: // Разоритель
                DropHead(event, "ravager", "ravager");
                break;

            case BEE: // Пчела - 2
                Bee bee = (Bee) event.getEntity();

                if (bee.hasNectar()) {
                    DropHead(event, "pollinated", "pollinated");
                } else {
                    DropHead(event, "bee", "bee");
                }
                break;

            case STRIDER: // Лавомерка
                DropHead(event, "strider", "strider");
                break;

            case ZOMBIFIED_PIGLIN: // Зомбифицированный пиглин
                DropHead(event, "zombified_piglin", "zombified_piglin");
                break;

            case HOGLIN: // Хоглин
                DropHead(event, "hoglin", "hoglin");
                break;

            case ZOGLIN: // Зоглин
                DropHead(event, "zoglin", "zoglin");
                break;

            case PIGLIN: // Пиглин
                DropHead(event, "piglin", "piglin");
                break;

            case PIGLIN_BRUTE: // Брутальный пиглин
                DropHead(event, "piglin_brute", "piglin");
                break;

            case GLOW_SQUID: // Светящийся спрут
                DropHead(event, "glow_squid", "glow_squid");
                break;

            case AXOLOTL: // Аксолотль - 5
                Axolotl axolotl = (Axolotl) event.getEntity();
                Axolotl.Variant axolotl_variant = axolotl.getVariant();

                switch (axolotl_variant) {
                    case LUCY:
                        DropHead(event, "axolotl", "lucy_axolotl");
                        break;
                    case WILD:
                        DropHead(event, "axolotl", "wild_axolotl");
                        break;
                    case GOLD:
                        DropHead(event, "axolotl", "gold_axolotl");
                        break;
                    case CYAN:
                        DropHead(event, "axolotl", "cyan_axolotl");
                        break;
                    case BLUE:
                        DropHead(event, "blue_axolotl", "blue_axolotl");
                        break;
                    default:
                        break;
                }
                break;

            case GOAT: // Коза - 8
                Goat goat = (Goat) event.getEntity();
                boolean goat_has_left_horn = goat.hasLeftHorn();
                boolean goat_has_right_horn = goat.hasRightHorn();

                if (goat.isScreaming()) {
                    if (goat_has_left_horn && goat_has_right_horn) {
                        DropHead(event, "screaming_goat", "screaming_goat");
                    } else if (goat_has_left_horn) {
                        DropHead(event, "screaming_goat_left", "screaming_goat_left");
                    } else if (goat_has_right_horn) {
                        DropHead(event, "screaming_goat_right", "screaming_goat_right");
                    } else {
                        DropHead(event, "screaming_goat_none", "screaming_goat_none");
                    }
                } else {
                    if (goat_has_left_horn && goat_has_right_horn) {
                        DropHead(event, "goat", "goat");
                    } else if (goat_has_left_horn) {
                        DropHead(event, "goat_left", "goat_left");
                    } else if (goat_has_right_horn) {
                        DropHead(event, "goat_right", "goat_right");
                    } else {
                        DropHead(event, "goat_none", "goat_none");
                    }
                }
                break;

            case TADPOLE: // Головастик
                DropHead(event, "tadpole", "tadpole");
                break;

            case FROG: // Лягушка - 3
                Frog frog = (Frog) event.getEntity();
                Frog.Variant frog_variant = frog.getVariant();

                switch (frog_variant) {
                    case COLD:
                        DropHead(event, "frog", "cold_frog");
                        break;
                    case TEMPERATE:
                        DropHead(event, "frog", "temperate_frog");
                        break;
                    case WARM:
                        DropHead(event, "frog", "warm_frog");
                        break;
                    default:
                        break;
                }
                break;

            case ALLAY: // Тихоня
                DropHead(event, "allay", "allay");
                break;

            case WARDEN: // Хранитель
                DropHead(event, "warden", "warden");
                break;

            case CAMEL: // Верблюд
                DropHead(event, "camel", "camel");
                break;

            case SNIFFER: // Нюхач
                DropHead(event, "sniffer", "sniffer");
                break;

            // Жители  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            // Зомби Жители  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            default:
                break;
        }
    }
}
