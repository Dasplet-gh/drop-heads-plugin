package me.matejpacan.dropheads;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import org.bukkit.Bukkit;
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


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public final class DropHeadsPlugin extends JavaPlugin implements CommandExecutor {

    static FileConfiguration config;

    @Override
    public void onEnable() {
        config = getConfig();
        getServer().getPluginManager().registerEvents(new MobDeathListener(), this);
        Objects.requireNonNull(getCommand("gethead")).setExecutor(this);
        getLogger().info(config.getString("message_on_enable"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        // Проверка, является ли отправитель игроком
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Objects.requireNonNull(config.getString("message_not_player")));
            return true;
        }
        // Проверка, является ли отправитель оператором сервера
        if (!sender.isOp()) {
            sender.sendMessage(Objects.requireNonNull(config.getString("message_not_permission")));
            return true;
        }

        // Команда gethead
        if (command.getName().equalsIgnoreCase("gethead")) {
            // Проверяем, был ли передан хотя бы один аргумент
            if (args.length == 0) {
                sender.sendMessage(Objects.requireNonNull(config.getString("message_gethead_usage")));
                return true;
            }
            // Получение текстуры головы, что бы проверить её наличие
            if (config.getString(args[0] + MobDeathListener.TagSuffixHeadTexture) == null) {
                sender.sendMessage(Objects.requireNonNull(config.getString("message_gethead_missing_head")));
                return true;
            }
            // Выдача головы
            player.getInventory().addItem(MobDeathListener.CreateHeadItem(Objects.requireNonNull(
                            DropHeadsPlugin.config.getString(args[0] + MobDeathListener.TagSuffixHeadTexture)),
                    DropHeadsPlugin.config.getString(args[0] + MobDeathListener.TagSuffixHeadName)));

            return true;
        }

        return false;
    }
}

class MobDeathListener implements Listener {

    static final String TagSuffixHeadDropChance = "_head_drop_chance";
    static final String TagSuffixHeadName = "_head_name";
    static final String TagSuffixHeadTexture = "_head_texture";

    static final String TagPrefixHeadUUIDHash = "drop_heads_";

    static final String ItemCodePrefix = "minecraft:";

    static final String NameTextures = "textures";

    public static void TriumphOfDropHead(Player player, String head_name) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
        player.sendTitle(
                DropHeadsPlugin.config.getString("message_drop_head_title_header"),
                DropHeadsPlugin.config.getString("message_drop_head_text_prefix") + " " + head_name,
                10, 50, 20);
    }

    public static ItemStack CreateHeadItem(String texture_code, String head_name) {
        // Начало ванильных голов
        String vanilla_head_starts = ItemCodePrefix;
        // Если голова ванильная, то получаем её и всё
        if (texture_code.startsWith(vanilla_head_starts)) {
            Material material =
                    Material.getMaterial(texture_code.substring(vanilla_head_starts.length()).toUpperCase());
            assert material != null;
            return new ItemStack(material);
        }
        // Объект голова и мета данные головы
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        // Создание уникального uuid для типа головы и профиля мета данных для головы
        UUID uuid = UUID.nameUUIDFromBytes((TagPrefixHeadUUIDHash + texture_code).getBytes());
        PlayerProfile profile = Bukkit.createProfile(uuid, null);
        profile.setProperty(new ProfileProperty(NameTextures, texture_code));
        // Запись текстуры и названия головы в мета данные
        meta.setPlayerProfile(profile);
        meta.displayName(Component.text(
                DropHeadsPlugin.config.getString("head_name_starts") + " " + head_name,
                NamedTextColor.YELLOW
        ).decoration(TextDecoration.ITALIC, false));
        // Установка мета данных голове
        head.setItemMeta(meta);
        // Возврат головы
        return head;
    }

    public static boolean IsMobHeadDrop(EntityDeathEvent event, String drop_chance_tag) {
        // Получение убийцы
        Player killer = event.getEntity().getKiller();
        // Если нет убийцы, то голова не выпадает
        if (killer == null) { return false; }
        // Если убитый моб это ребёнок, то голова не выпадает
        if (event.getEntity() instanceof Ageable ageable) {
            if (!ageable.isAdult()) { return false; }
        }
        // Получение уровня добычи у оружия убийцы
        int loot_level = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
        // Шанс выпадения головы
        double original_drop_chance = DropHeadsPlugin.config.getDouble(drop_chance_tag + TagSuffixHeadDropChance);
        double drop_chance = original_drop_chance +
                (DropHeadsPlugin.config.getDouble("looting_multiplier") * loot_level * original_drop_chance);
        // Если выпал шанс, то голова дропается
        return (new Random().nextDouble() <= drop_chance);
    }

    public static void DropMobHead(EntityDeathEvent event, String drop_chance_tag, String texture_tag) {
        // Получение убийцы
        Player killer = event.getEntity().getKiller();
        // Если убийцы нет, или шанс на выпадение не выпал, то голова не выпадает
        if (killer == null || !IsMobHeadDrop(event, drop_chance_tag)) { return; }
        // Добавление головы в дроп
        event.getDrops().add(CreateHeadItem(Objects.requireNonNull(
                        DropHeadsPlugin.config.getString(texture_tag + TagSuffixHeadTexture)),
                DropHeadsPlugin.config.getString(texture_tag + TagSuffixHeadName)));
        // Воспроизведение торжества в связи с выпадением головы
        TriumphOfDropHead(killer, DropHeadsPlugin.config.getString(texture_tag + TagSuffixHeadName));
    }

    // =================================================================================================================

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Игрок -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        if (event.getEntity() instanceof Player killed_player) {
            // Получение убитого и убийцы
            Player killer = killed_player.getKiller();
            // Если есть убийца и выпал шанс, то голова дропается
            if (killer != null &&
                    new Random().nextDouble() <= DropHeadsPlugin.config.getDouble(
                            "player" + TagSuffixHeadDropChance)) {
                // Объект голова и мета данные головы
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skull_meta = (SkullMeta) head.getItemMeta();
                // Запись текстуры головы в мета данные
                skull_meta.setOwningPlayer(killed_player);
                // Установка мета данных голове
                head.setItemMeta(skull_meta);
                // Добавление головы в дроп
                event.getDrops().add(head);
                // Воспроизведение торжества в связи с выпадением головы
                TriumphOfDropHead(killer, killed_player.getName());
            }
        }

        // Существа  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        switch (event.getEntityType()) {

            // Боссы -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case ENDER_DRAGON: // Дракон
                DropMobHead(event, "ender_dragon", "ender_dragon");
                break;

            case WITHER: // Визер
                DropMobHead(event, "wither", "wither");
                break;

            // Обычные Мобы  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case PIG: // Свинья
                DropMobHead(event, "pig", "pig");
                break;

            case SHEEP: // Овца - 17
                Sheep sheep = (Sheep) event.getEntity();
                if (!sheep.isAdult()) { break; }
                DyeColor sheep_dye_color = sheep.getColor();

                if (sheep.getName().equals("jeb_")) {
                    DropMobHead(event, "sheep", "jeb_sheep");
                    break;
                }

                if (sheep_dye_color == null) { break; }
                switch (sheep_dye_color) {
                    case WHITE:
                        DropMobHead(event, "sheep", "white_sheep");
                        break;
                    case ORANGE:
                        DropMobHead(event, "sheep", "orange_sheep");
                        break;
                    case MAGENTA:
                        DropMobHead(event, "sheep", "magenta_sheep");
                        break;
                    case LIGHT_BLUE:
                        DropMobHead(event, "sheep", "light_blue_sheep");
                        break;
                    case YELLOW:
                        DropMobHead(event, "sheep", "yellow_sheep");
                        break;
                    case LIME:
                        DropMobHead(event, "sheep", "lime_sheep");
                        break;
                    case PINK:
                        DropMobHead(event, "sheep", "pink_sheep");
                        break;
                    case GRAY:
                        DropMobHead(event, "sheep", "gray_sheep");
                        break;
                    case LIGHT_GRAY:
                        DropMobHead(event, "sheep", "light_gray_sheep");
                        break;
                    case CYAN:
                        DropMobHead(event, "sheep", "cyan_sheep");
                        break;
                    case PURPLE:
                        DropMobHead(event, "sheep", "purple_sheep");
                        break;
                    case BLUE:
                        DropMobHead(event, "sheep", "blue_sheep");
                        break;
                    case BROWN:
                        DropMobHead(event, "sheep", "brown_sheep");
                        break;
                    case GREEN:
                        DropMobHead(event, "sheep", "green_sheep");
                        break;
                    case RED:
                        DropMobHead(event, "sheep", "red_sheep");
                        break;
                    case BLACK:
                        DropMobHead(event, "sheep", "black_sheep");
                        break;
                    default:
                        break;
                }
                break;

            case SPIDER: // Паук
                DropMobHead(event, "spider", "spider");
                break;

            case SKELETON: // Скелет
                DropMobHead(event, "skeleton", "skeleton");
                break;

            case ZOMBIE: // Зомби
                DropMobHead(event, "zombie", "zombie");
                break;

            case CREEPER: // Крипер - 2
                Creeper creeper = (Creeper) event.getEntity();

                if (creeper.isPowered()) {
                    DropMobHead(event, "powered_creeper", "powered_creeper");
                } else {
                    DropMobHead(event, "creeper", "creeper");
                }
                break;

            case COW: // Корова
                DropMobHead(event, "cow", "cow");
                break;

            case CHICKEN: // Курица
                DropMobHead(event, "chicken", "chicken");
                break;

            case GHAST: // Гаст
                DropMobHead(event, "ghast", "ghast");
                break;

            case SLIME: // Слизень
                Slime slime = (Slime) event.getEntity();

                if (slime.getSize() == 1) {
                    DropMobHead(event, "slime", "slime");
                }
                break;

            case SQUID: // Спрут
                DropMobHead(event, "squid", "squid");
                break;

            case WOLF: // Волк - 17
                Wolf wolf = (Wolf) event.getEntity();

                if (wolf.isTamed()) {
                    DyeColor wolf_collar_color = wolf.getCollarColor();

                    switch (wolf_collar_color) {
                        case WHITE:
                            DropMobHead(event, "tamed_wolf", "white_tamed_wolf");
                            break;
                        case ORANGE:
                            DropMobHead(event, "tamed_wolf", "orange_tamed_wolf");
                            break;
                        case MAGENTA:
                            DropMobHead(event, "tamed_wolf", "magenta_tamed_wolf");
                            break;
                        case LIGHT_BLUE:
                            DropMobHead(event, "tamed_wolf", "light_blue_tamed_wolf");
                            break;
                        case YELLOW:
                            DropMobHead(event, "tamed_wolf", "yellow_tamed_wolf");
                            break;
                        case LIME:
                            DropMobHead(event, "tamed_wolf", "lime_tamed_wolf");
                            break;
                        case PINK:
                            DropMobHead(event, "tamed_wolf", "pink_tamed_wolf");
                            break;
                        case GRAY:
                            DropMobHead(event, "tamed_wolf", "gray_tamed_wolf");
                            break;
                        case LIGHT_GRAY:
                            DropMobHead(event, "tamed_wolf", "light_gray_tamed_wolf");
                            break;
                        case CYAN:
                            DropMobHead(event, "tamed_wolf", "cyan_tamed_wolf");
                            break;
                        case PURPLE:
                            DropMobHead(event, "tamed_wolf", "purple_tamed_wolf");
                            break;
                        case BLUE:
                            DropMobHead(event, "tamed_wolf", "blue_tamed_wolf");
                            break;
                        case BROWN:
                            DropMobHead(event, "tamed_wolf", "brown_tamed_wolf");
                            break;
                        case GREEN:
                            DropMobHead(event, "tamed_wolf", "green_tamed_wolf");
                            break;
                        case RED:
                            DropMobHead(event, "tamed_wolf", "red_tamed_wolf");
                            break;
                        case BLACK:
                            DropMobHead(event, "tamed_wolf", "black_tamed_wolf");
                            break;
                        default:
                            break;
                    }
                } else {
                    DropMobHead(event, "wolf", "wolf");
                }
                break;

            case MOOSHROOM: // Грибная корова - 2
                MushroomCow mushroom_cow = (MushroomCow) event.getEntity();
                MushroomCow.Variant mushroom_cow_variant = mushroom_cow.getVariant();

                switch (mushroom_cow_variant) {
                    case RED:
                        DropMobHead(event, "red_mooshroom", "red_mooshroom");
                        break;
                    case BROWN:
                        DropMobHead(event, "brown_mooshroom", "brown_mooshroom");
                        break;
                    default:
                        break;
                }
                break;

            case SNOW_GOLEM: // Снежный голем - 2
                Snowman snowman = (Snowman) event.getEntity();

                if (snowman.isDerp()) {
                    DropMobHead(event, "snowman", "snowman");
                } else {
                    DropMobHead(event, "snow_golem", "snow_golem");
                }
                break;

            case BLAZE: // Всполох
                DropMobHead(event, "blaze", "blaze");
                break;

            case MAGMA_CUBE: // Магма Куб
                MagmaCube magma_cube = (MagmaCube) event.getEntity();

                if (magma_cube.getSize() == 1) {
                    DropMobHead(event, "magma_cube", "magma_cube");
                }
                break;

            case OCELOT: // Оцелот
                DropMobHead(event, "ocelot", "ocelot");
                break;

            case CAT: // Кошка - 11
                Player killer = event.getEntity().getKiller();
                if (killer == null) { break; }

                killer.sendMessage(
                        "§5* " + killer.getName() + " §5зверски убивает кошку, и понижает свою репутацию");

                Cat cat = (Cat) event.getEntity();
                Cat.Type cat_type = cat.getCatType();

                if (cat_type == Cat.Type.TABBY) {
                    DropMobHead(event, "cat", "tabby_cat");
                } else if (cat_type == Cat.Type.BLACK) {
                    DropMobHead(event, "cat", "black_cat");
                } else if (cat_type == Cat.Type.RED) {
                    DropMobHead(event, "cat", "red_cat");
                } else if (cat_type == Cat.Type.SIAMESE) {
                    DropMobHead(event, "cat", "siamese_cat");
                } else if (cat_type == Cat.Type.BRITISH_SHORTHAIR) {
                    DropMobHead(event, "cat", "british_shorthair_cat");
                } else if (cat_type == Cat.Type.CALICO) {
                    DropMobHead(event, "cat", "calico_cat");
                } else if (cat_type == Cat.Type.PERSIAN) {
                    DropMobHead(event, "cat", "persian_cat");
                } else if (cat_type == Cat.Type.RAGDOLL) {
                    DropMobHead(event, "cat", "ragdoll_cat");
                } else if (cat_type == Cat.Type.WHITE) {
                    DropMobHead(event, "cat", "white_cat");
                } else if (cat_type == Cat.Type.JELLIE) {
                    DropMobHead(event, "cat", "jellie_cat");
                } else if (cat_type == Cat.Type.ALL_BLACK) {
                    DropMobHead(event, "cat", "all_black_cat");
                }

                break;

            case IRON_GOLEM: // Железный голем
                DropMobHead(event, "iron_golem", "iron_golem");
                break;

            case BAT: // Летучая Мышь
                DropMobHead(event, "bat", "bat");
                break;

            case WITCH: // Ведьма
                DropMobHead(event, "witch", "witch");
                break;

            case HORSE: // Лошадь - 35
                Horse horse = (Horse) event.getEntity();
                Horse.Color horse_color = horse.getColor();
                Horse.Style horse_style = horse.getStyle();

                DropMobHead(event, "horse", GenerateHorseTextureTag(horse_color, horse_style));
                break;

            case SKELETON_HORSE: // Лошадь-скелет
                DropMobHead(event, "skeleton_horse", "skeleton_horse");
                break;

            case ZOMBIE_HORSE: // Лошадь-зомби
                DropMobHead(event, "zombie_horse", "zombie_horse");
                break;

            case DONKEY: // Осёл
                DropMobHead(event, "donkey", "donkey");
                break;

            case MULE: // Мул
                DropMobHead(event, "mule", "mule");
                break;

            case ENDERMAN: // Эндермен
                DropMobHead(event, "enderman", "enderman");
                break;

            case ENDERMITE: // Эндермит
                DropMobHead(event, "endermite", "endermite");
                break;

            case RABBIT: // Кролик - 8
                Rabbit rabbit = (Rabbit) event.getEntity();
                Rabbit.Type rabbit_type = rabbit.getRabbitType();

                if (rabbit.getName().equals("Toast")) {
                    DropMobHead(event, "toast_rabbit", "toast_rabbit");
                }

                switch (rabbit_type) {
                    case BROWN:
                        DropMobHead(event, "rabbit", "brown_rabbit");
                        break;
                    case WHITE:
                        DropMobHead(event, "rabbit", "white_rabbit");
                        break;
                    case BLACK:
                        DropMobHead(event, "rabbit", "black_rabbit");
                        break;
                    case BLACK_AND_WHITE:
                        DropMobHead(event, "rabbit", "black_and_white_rabbit");
                        break;
                    case GOLD:
                        DropMobHead(event, "rabbit", "gold_rabbit");
                        break;
                    case SALT_AND_PEPPER:
                        DropMobHead(event, "rabbit", "salt_and_pepper_rabbit");
                        break;
                    case THE_KILLER_BUNNY:
                        DropMobHead(event, "killer_bunny", "killer_bunny");
                        break;
                    default:
                        break;
                }
                break;

            case CAVE_SPIDER: // Пещерный паук
                DropMobHead(event, "cave_spider", "cave_spider");
                break;

            case GUARDIAN: // Страж
                DropMobHead(event, "guardian", "guardian");
                break;

            case ELDER_GUARDIAN: // Древний Страж
                DropMobHead(event, "elder_guardian", "elder_guardian");
                break;

            case SILVERFISH: // Чешуйница
                DropMobHead(event, "silverfish", "silverfish");
                break;

            case SHULKER: // Шалкер
                DropMobHead(event, "shulker", "shulker");
                break;

            case POLAR_BEAR: // Белый медведь
                DropMobHead(event, "polar_bear", "polar_bear");
                break;

            case HUSK: // Кадавр
                DropMobHead(event, "husk", "husk");
                break;

            case STRAY: // Зимогор
                DropMobHead(event, "stray", "stray");
                break;

            case LLAMA: // Лама - 4
                Llama llama = (Llama) event.getEntity();
                Llama.Color llama_color = llama.getColor();

                switch (llama_color) {
                    case WHITE:
                        DropMobHead(event, "llama", "white_llama");
                        break;
                    case GRAY:
                        DropMobHead(event, "llama", "gray_llama");
                        break;
                    case CREAMY:
                        DropMobHead(event, "llama", "creamy_llama");
                        break;
                    case BROWN:
                        DropMobHead(event, "llama", "brown_llama");
                        break;
                    default:
                        break;
                }
                break;

            case EVOKER: // Заклинатель
                DropMobHead(event, "evoker", "evoker");
                break;

            case VEX: // Вредина
                DropMobHead(event, "vex", "vex");
                break;

            case VINDICATOR: // Поборник
                DropMobHead(event, "vindicator", "vindicator");
                break;

            case PARROT:
                Parrot parrot = (Parrot) event.getEntity();
                Parrot.Variant parrot_variant = parrot.getVariant();

                switch (parrot_variant) {
                    case RED:
                        DropMobHead(event, "parrot", "red_parrot");
                        break;
                    case BLUE:
                        DropMobHead(event, "parrot", "blue_parrot");
                        break;
                    case GRAY:
                        DropMobHead(event, "parrot", "gray_parrot");
                        break;
                    case CYAN:
                        DropMobHead(event, "parrot", "cyan_parrot");
                        break;
                    case GREEN:
                        DropMobHead(event, "parrot", "green_parrot");
                        break;
                    default:
                        break;
                }
                break;

            case COD: // Треска
                DropMobHead(event, "cod", "cod");
                break;

            case SALMON: // Лосось
                DropMobHead(event, "salmon", "salmon");
                break;

            case TROPICAL_FISH: // Тропическая рыба
                DropMobHead(event, "tropical_fish", "tropical_fish");
                break;

            case PUFFERFISH: // Иглобрюх
                DropMobHead(event, "pufferfish", "pufferfish");
                break;

            case TURTLE: // Черепаха
                DropMobHead(event, "turtle", "turtle");
                break;

            case DOLPHIN: // Дельфин
                DropMobHead(event, "dolphin", "dolphin");
                break;

            case DROWNED: // Утопленник
                DropMobHead(event, "drowned", "drowned");
                break;

            case PHANTOM: // Фантом
                DropMobHead(event, "phantom", "phantom");
                break;

            case FOX: // Лиса - 2
                Fox fox = (Fox) event.getEntity();
                Fox.Type fox_type = fox.getFoxType();

                switch (fox_type) {
                    case RED:
                        DropMobHead(event, "fox", "red_fox");
                        break;
                    case SNOW:
                        DropMobHead(event, "fox", "snow_fox");
                        break;
                    default:
                        break;
                }
                break;

            case WANDERING_TRADER:  // Странствующий торговец
                DropMobHead(event, "wandering_trader", "wandering_trader");
                break;

            case TRADER_LLAMA: // Лама странствующего торговца - 4
                TraderLlama traderllama = (TraderLlama) event.getEntity();
                TraderLlama.Color traderllama_color = traderllama.getColor();

                switch (traderllama_color) {
                    case WHITE:
                        DropMobHead(event, "trader_llama", "white_trader_llama");
                        break;
                    case GRAY:
                        DropMobHead(event, "trader_llama", "gray_trader_llama");
                        break;
                    case CREAMY:
                        DropMobHead(event, "trader_llama", "creamy_trader_llama");
                        break;
                    case BROWN:
                        DropMobHead(event, "trader_llama", "brown_trader_llama");
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
                        DropMobHead(event, "panda", "aggressive_panda");
                        break;
                    case LAZY:
                        DropMobHead(event, "panda", "lazy_panda");
                        break;
                    case WORRIED:
                        DropMobHead(event, "panda", "worried_panda");
                        break;
                    case PLAYFUL:
                        DropMobHead(event, "panda", "playful_panda");
                        break;
                    case WEAK:
                        if (panda_hidden_gene == Panda.Gene.WEAK) {
                            DropMobHead(event, "panda", "weak_panda");
                        } else {
                            DropMobHead(event, "panda", "normal_panda");
                        }
                        break;
                    case BROWN:
                        if (panda_hidden_gene == Panda.Gene.BROWN) {
                            DropMobHead(event, "brown_panda", "brown_panda");
                        } else {
                            DropMobHead(event, "panda", "normal_panda");
                        }
                        break;
                    default:
                        DropMobHead(event, "panda", "normal_panda");
                        break;
                }
                break;

            case PILLAGER: // Разбойник
                DropMobHead(event, "pillager", "pillager");
                break;

            case RAVAGER: // Разоритель
                DropMobHead(event, "ravager", "ravager");
                break;

            case BEE: // Пчела - 2
                Bee bee = (Bee) event.getEntity();

                if (bee.hasNectar()) {
                    DropMobHead(event, "pollinated_bee", "pollinated_bee");
                } else {
                    DropMobHead(event, "bee", "bee");
                }
                break;

            case STRIDER: // Лавомерка
                DropMobHead(event, "strider", "strider");
                break;

            case ZOMBIFIED_PIGLIN: // Зомбифицированный пиглин
                DropMobHead(event, "zombified_piglin", "zombified_piglin");
                break;

            case HOGLIN: // Хоглин
                DropMobHead(event, "hoglin", "hoglin");
                break;

            case ZOGLIN: // Зоглин
                DropMobHead(event, "zoglin", "zoglin");
                break;

            case PIGLIN: // Пиглин
                DropMobHead(event, "piglin", "piglin");
                break;

            case PIGLIN_BRUTE: // Брутальный пиглин
                DropMobHead(event, "piglin_brute", "piglin");
                break;

            case GLOW_SQUID: // Светящийся спрут
                DropMobHead(event, "glow_squid", "glow_squid");
                break;

            case AXOLOTL: // Аксолотль - 5
                Axolotl axolotl = (Axolotl) event.getEntity();
                Axolotl.Variant axolotl_variant = axolotl.getVariant();

                switch (axolotl_variant) {
                    case LUCY:
                        DropMobHead(event, "axolotl", "lucy_axolotl");
                        break;
                    case WILD:
                        DropMobHead(event, "axolotl", "wild_axolotl");
                        break;
                    case GOLD:
                        DropMobHead(event, "axolotl", "gold_axolotl");
                        break;
                    case CYAN:
                        DropMobHead(event, "axolotl", "cyan_axolotl");
                        break;
                    case BLUE:
                        DropMobHead(event, "blue_axolotl", "blue_axolotl");
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
                        DropMobHead(event, "screaming_goat", "screaming_goat");
                    } else if (goat_has_left_horn) {
                        DropMobHead(event, "screaming_goat_left", "screaming_goat_left");
                    } else if (goat_has_right_horn) {
                        DropMobHead(event, "screaming_goat_right", "screaming_goat_right");
                    } else {
                        DropMobHead(event, "screaming_goat_none", "screaming_goat_none");
                    }
                } else {
                    if (goat_has_left_horn && goat_has_right_horn) {
                        DropMobHead(event, "goat", "goat");
                    } else if (goat_has_left_horn) {
                        DropMobHead(event, "goat_left", "goat_left");
                    } else if (goat_has_right_horn) {
                        DropMobHead(event, "goat_right", "goat_right");
                    } else {
                        DropMobHead(event, "goat_none", "goat_none");
                    }
                }
                break;

            case TADPOLE: // Головастик
                DropMobHead(event, "tadpole", "tadpole");
                break;

            case FROG: // Лягушка - 3
                Frog frog = (Frog) event.getEntity();
                Frog.Variant frog_variant = frog.getVariant();

                if (frog_variant == Frog.Variant.COLD) {
                    DropMobHead(event, "frog", "cold_frog");
                } else if (frog_variant == Frog.Variant.TEMPERATE) {
                    DropMobHead(event, "frog", "temperate_frog");
                } else if (frog_variant == Frog.Variant.WARM) {
                    DropMobHead(event, "frog", "warm_frog");
                }

                break;

            case ALLAY: // Тихоня
                DropMobHead(event, "allay", "allay");
                break;

            case WARDEN: // Хранитель
                DropMobHead(event, "warden", "warden");
                break;

            case CAMEL: // Верблюд
                DropMobHead(event, "camel", "camel");
                break;

            case SNIFFER: // Нюхач
                DropMobHead(event, "sniffer", "sniffer");
                break;

            // Крестьянин  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case VILLAGER: // Крестьянин
                Villager villager = (Villager) event.getEntity();
                Villager.Type villager_type = villager.getVillagerType();
                Villager.Profession villager_profession = villager.getProfession();

                DropMobHead(event, "villager", GenerateVillagerTextureTag(
                        villager_type, villager_profession, "villager"));
                break;

            // Крестьянин-зомби  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case ZOMBIE_VILLAGER: // Крестьянин-зомби
                ZombieVillager zombie_villager = (ZombieVillager) event.getEntity();
                Villager.Type zombie_villager_type = zombie_villager.getVillagerType();
                Villager.Profession zombie_villager_profession = zombie_villager.getVillagerProfession();

                DropMobHead(event, "zombie_villager", GenerateVillagerTextureTag(
                        zombie_villager_type, zombie_villager_profession, "zombie_villager"));
                break;

            // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            default:
                break;
        }
    }

    public static String GenerateHorseTextureTag(Horse.Color horse_color, Horse.Style horse_style) {
        String texture_tag = "";

        switch (horse_color) {
            case WHITE:
                texture_tag += "white";
                break;
            case CREAMY:
                texture_tag += "creamy";
                break;
            case CHESTNUT:
                texture_tag += "chestnut";
                break;
            case BROWN:
                texture_tag += "brown";
                break;
            case BLACK:
                texture_tag += "black";
                break;
            case GRAY:
                texture_tag += "gray";
                break;
            case DARK_BROWN:
                texture_tag += "dark_brown";
                break;
            default:
                break;
        }

        texture_tag += "_";

        switch (horse_style) {
            case NONE:
                texture_tag += "none";
                break;
            case WHITE:
                texture_tag += "white";
                break;
            case WHITEFIELD:
                texture_tag += "whitefield";
                break;
            case WHITE_DOTS:
                texture_tag += "white_dots";
                break;
            case BLACK_DOTS:
                texture_tag += "black_dots";
                break;
            default:
                break;
        }

        texture_tag += "_";

        texture_tag += "horse";

        return texture_tag;
    }

    public static String GenerateVillagerTextureTag(
            Villager.Type villager_type, Villager.Profession villager_profession, String villager_tag) {
        String texture_tag = "";

        if (villager_type == Villager.Type.PLAINS) {
            texture_tag += "plains";
        } else if (villager_type == Villager.Type.DESERT) {
            texture_tag += "desert";
        } else if (villager_type == Villager.Type.JUNGLE) {
            texture_tag += "jungle";
        } else if (villager_type == Villager.Type.SAVANNA) {
            texture_tag += "savanna";
        } else if (villager_type == Villager.Type.SNOW) {
            texture_tag += "snow";
        } else if (villager_type == Villager.Type.SWAMP) {
            texture_tag += "swamp";
        } else if (villager_type == Villager.Type.TAIGA) {
            texture_tag += "taiga";
        }

        texture_tag += "_";

        if (villager_profession == Villager.Profession.NONE) {
            texture_tag += "none";
        } else if (villager_profession == Villager.Profession.NITWIT) {
            texture_tag += "nitwit";
        } else if (villager_profession == Villager.Profession.ARMORER) {
            texture_tag += "armorer";
        } else if (villager_profession == Villager.Profession.BUTCHER) {
            texture_tag += "butcher";
        } else if (villager_profession == Villager.Profession.CARTOGRAPHER) {
            texture_tag += "cartographer";
        } else if (villager_profession == Villager.Profession.CLERIC) {
            texture_tag += "cleric";
        } else if (villager_profession == Villager.Profession.FARMER) {
            texture_tag += "farmer";
        } else if (villager_profession == Villager.Profession.FISHERMAN) {
            texture_tag += "fisherman";
        } else if (villager_profession == Villager.Profession.FLETCHER) {
            texture_tag += "fletcher";
        } else if (villager_profession == Villager.Profession.LEATHERWORKER) {
            texture_tag += "leatherworker";
        } else if (villager_profession == Villager.Profession.LIBRARIAN) {
            texture_tag += "librarian";
        } else if (villager_profession == Villager.Profession.MASON) {
            texture_tag += "mason";
        } else if (villager_profession == Villager.Profession.SHEPHERD) {
            texture_tag += "shepherd";
        } else if (villager_profession == Villager.Profession.TOOLSMITH) {
            texture_tag += "toolsmith";
        } else if (villager_profession == Villager.Profession.WEAPONSMITH) {
            texture_tag += "weaponsmith";
        }

        texture_tag += "_";

        texture_tag += villager_tag;

        return texture_tag;
    }
}
