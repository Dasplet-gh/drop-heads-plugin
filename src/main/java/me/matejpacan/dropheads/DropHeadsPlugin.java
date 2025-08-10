package me.matejpacan.dropheads;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Goat;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;

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
            if (config.getString(args[0] + MobDeathListener.TAG_SUFFIX_HEAD_TEXTURE) == null) {
                sender.sendMessage(Objects.requireNonNull(config.getString("message_gethead_missing_head")));
                return true;
            }
            // Выдача головы
            player.getInventory().addItem(MobDeathListener.createHeadItem(Objects.requireNonNull(
                            DropHeadsPlugin.config.getString(args[0] + MobDeathListener.TAG_SUFFIX_HEAD_TEXTURE)),
                    DropHeadsPlugin.config.getString(args[0] + MobDeathListener.TAG_SUFFIX_HEAD_NAME)));

            return true;
        }

        return false;
    }
}

class MobDeathListener implements Listener {

    static final String TAG_SUFFIX_HEAD_DROP_CHANCE = "_head_drop_chance";
    static final String TAG_SUFFIX_HEAD_NAME = "_head_name";
    static final String TAG_SUFFIX_HEAD_TEXTURE = "_head_texture";
    static final String TAG_PREFIX_HEAD_RARE = "rare_";

    static final String HEAD_UUID_HASH_PREFIX = "drop_heads_";

    static final String ITEM_CODE_PREFIX = "minecraft:";

    public static ItemStack createHeadItem(String textureCode, String headName) {
        // Начало ванильных голов
        String vanillaHeadStarts = ITEM_CODE_PREFIX;
        // Если голова ванильная, то получаем её и всё
        if (textureCode.startsWith(vanillaHeadStarts)) {
            Material material = Material.getMaterial(textureCode.substring(vanillaHeadStarts.length()).toUpperCase());
            assert material != null;
            return new ItemStack(material);
        }
        // Объект голова и мета данные головы
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        // Создание уникального uuid для типа головы и профиля мета данных для головы
        UUID uuid = UUID.nameUUIDFromBytes((HEAD_UUID_HASH_PREFIX + textureCode).getBytes());
        PlayerProfile profile = Bukkit.createProfile(uuid, null);
        profile.setProperty(new ProfileProperty("textures", textureCode));
        // Запись текстуры и названия головы в мета данные
        meta.setPlayerProfile(profile);
        meta.itemName(Component.text(DropHeadsPlugin.config.getString("head_name_starts") + " " + headName));
        // Установка мета данных голове
        head.setItemMeta(meta);
        // Возврат головы
        return head;
    }

    public static double calculateDropChance(Player killer, String dropChanceTag) {
        // Получение уровня добычи у оружия убийцы
        int lootLevel = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
        // Шанс выпадения головы
        double originalDropChance = DropHeadsPlugin.config.getDouble(dropChanceTag + TAG_SUFFIX_HEAD_DROP_CHANCE);
        double dropChance = originalDropChance +
                (DropHeadsPlugin.config.getDouble("looting_multiplier") * lootLevel * originalDropChance);
        // Возвращаем посчитанный шанс
        return dropChance;
    }

    public static boolean isMobHeadDrop(EntityDeathEvent event, Player killer, String dropChanceTag) {
        // Если убитый моб это ребёнок, то голова не выпадает
        if (event.getEntity() instanceof Ageable ageable && 
            event.getEntity().getType() != EntityType.HAPPY_GHAST) { // Исключение: Гастёнок
            if (!ageable.isAdult()) { return false; }
        }
        // Вычисление шанса выпадения головы
        double dropChance = calculateDropChance(killer, dropChanceTag);
        // Если выпал шанс, то голова дропается
        return (new Random().nextDouble() <= dropChance);
    }

    public static void triumphOfDropHead(Player player, String headName) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
        player.sendTitle(
            DropHeadsPlugin.config.getString("message_drop_head_title_header"),
            DropHeadsPlugin.config.getString("message_drop_head_text_prefix") + " " + headName,
            10, 50, 20
        );
    }

    public static void rareTriumphOfDropHead(Player player, String headName) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        player.sendTitle(
            DropHeadsPlugin.config.getString("message_rare_drop_head_title_header"),
            DropHeadsPlugin.config.getString("message_rare_drop_head_text_prefix") + " " + headName,
            10, 50, 20
        );
    }

    public static void dropMobHead(EntityDeathEvent event, String dropChanceTag, String textureTag) {
        // Получение убийцы
        Player killer = event.getEntity().getKiller();
        // Если убийцы нет, или шанс на выпадение не выпал, то голова не выпадает
        if (killer == null || !isMobHeadDrop(event, killer, dropChanceTag)) { return; }
        // Добавление головы в дроп
        event.getDrops().add(createHeadItem(Objects.requireNonNull(
            DropHeadsPlugin.config.getString(textureTag + TAG_SUFFIX_HEAD_TEXTURE)), 
            DropHeadsPlugin.config.getString(textureTag + TAG_SUFFIX_HEAD_NAME)));
        // Воспроизведение торжества в связи с выпадением головы
        triumphOfDropHead(killer, DropHeadsPlugin.config.getString(textureTag + TAG_SUFFIX_HEAD_NAME));
    }

    public static void dropMobHeadWithRareVariant(EntityDeathEvent event, String dropChanceTag, String textureTag) {
        // Получение убийцы
        Player killer = event.getEntity().getKiller();
        // Если убийцы нет, то голова не выпадает
        if (killer == null) { return; }
        // Генерация одного случайного числа для определения типа дропа
        double random = new Random().nextDouble();
        // Вычисление шансов дропа
        double rareDropChance = calculateDropChance(killer, TAG_PREFIX_HEAD_RARE + dropChanceTag);
        double normalDropChance = calculateDropChance(killer, dropChanceTag);
        // Определение типа головы которая выпадет
        String finalTextureTag;
        if (random <= rareDropChance) { // Проверка на выпадение редкой головы
            finalTextureTag = TAG_PREFIX_HEAD_RARE + textureTag;
            // Воспроизведение редкого торжества в связи с выпадением головы
            rareTriumphOfDropHead(killer, DropHeadsPlugin.config.getString(finalTextureTag + TAG_SUFFIX_HEAD_NAME));
        } else if (random <= normalDropChance) { // Проверка на выпадение обычной головы
            finalTextureTag = textureTag;
            // Воспроизведение редкого торжества в связи с выпадением головы
            triumphOfDropHead(killer, DropHeadsPlugin.config.getString(finalTextureTag + TAG_SUFFIX_HEAD_NAME));
        } else { return; } // Ничего не выпало
        // Добавление головы в дроп
        event.getDrops().add(createHeadItem(Objects.requireNonNull(
            DropHeadsPlugin.config.getString(finalTextureTag + TAG_SUFFIX_HEAD_TEXTURE)), 
            DropHeadsPlugin.config.getString(finalTextureTag + TAG_SUFFIX_HEAD_NAME)));
    }

    // =================================================================================================================

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Игрок -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

        if (event.getEntity() instanceof Player killedPlayer) {
            // Получение убитого и убийцы
            Player killer = killedPlayer.getKiller();
            // Если убийцы нет или не выпал шанс, то голова не выпадает
            if (killer == null || new Random().nextDouble() > DropHeadsPlugin.config.getDouble(
                        "player" + TAG_SUFFIX_HEAD_DROP_CHANCE)) { return; }
            // Объект голова и мета данные головы
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            // Запись текстуры головы в мета данные
            skullMeta.setOwningPlayer(killedPlayer);
            // Установка мета данных голове
            head.setItemMeta(skullMeta);
            // Добавление головы в дроп
            event.getDrops().add(head);
            // Воспроизведение торжества в связи с выпадением головы
            triumphOfDropHead(killer, killedPlayer.getName());
        }

        // Существа  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
        
        switch (event.getEntityType()) {

            // Боссы -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case ENDER_DRAGON -> // Дракон
                dropMobHead(event, "ender_dragon", "ender_dragon");
            
            case WITHER -> // Визер
                dropMobHeadWithRareVariant(event, "wither", "wither");

            // Обычные Мобы  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case PIG -> { // Свинья - 3
                Pig pig = (Pig) event.getEntity();
                Pig.Variant pigVariant = pig.getVariant();

                if (pigVariant == Pig.Variant.TEMPERATE) {
                    dropMobHead(event, "pig", "temperate_pig");
                } else if (pigVariant == Pig.Variant.COLD) {
                    dropMobHead(event, "pig", "cold_pig");
                } else if (pigVariant == Pig.Variant.WARM) {
                    dropMobHead(event, "pig", "warm_pig");
                } else {
                    dropMobHead(event, "pig", "temperate_pig");
                }
            }

            case SHEEP -> { // Овца - 17
                Sheep sheep = (Sheep) event.getEntity();
                DyeColor sheepDyeColor = sheep.getColor();

                if (sheep.getName().equals("jeb_")) {
                    dropMobHead(event, "sheep", "jeb_sheep");
                    return;
                }

                if (sheepDyeColor == null) { return; }
                switch (sheepDyeColor) {
                    case WHITE -> dropMobHead(event, "sheep", "white_sheep");
                    case ORANGE -> dropMobHead(event, "sheep", "orange_sheep");
                    case MAGENTA -> dropMobHead(event, "sheep", "magenta_sheep");
                    case LIGHT_BLUE -> dropMobHead(event, "sheep", "light_blue_sheep");
                    case YELLOW -> dropMobHead(event, "sheep", "yellow_sheep");
                    case LIME -> dropMobHead(event, "sheep", "lime_sheep");
                    case PINK -> dropMobHead(event, "sheep", "pink_sheep");
                    case GRAY -> dropMobHead(event, "sheep", "gray_sheep");
                    case LIGHT_GRAY -> dropMobHead(event, "sheep", "light_gray_sheep");
                    case CYAN -> dropMobHead(event, "sheep", "cyan_sheep");
                    case PURPLE -> dropMobHead(event, "sheep", "purple_sheep");
                    case BLUE -> dropMobHead(event, "sheep", "blue_sheep");
                    case BROWN -> dropMobHead(event, "sheep", "brown_sheep");
                    case GREEN -> dropMobHead(event, "sheep", "green_sheep");
                    case RED -> dropMobHead(event, "sheep", "red_sheep");
                    case BLACK -> dropMobHead(event, "sheep", "black_sheep");
                    default -> dropMobHead(event, "sheep", "white_sheep");
                }
            }

            case SPIDER -> // Паук
                dropMobHead(event, "spider", "spider");

            case SKELETON -> // Скелет
                dropMobHead(event, "skeleton", "skeleton");

            case ZOMBIE -> // Зомби
                dropMobHead(event, "zombie", "zombie");

            case CREEPER -> { // Крипер - 2
                Creeper creeper = (Creeper) event.getEntity();

                if (creeper.isPowered()) {
                    dropMobHead(event, "powered_creeper", "powered_creeper");
                } else {
                    dropMobHead(event, "creeper", "creeper");
                }
            }

            case COW -> { // Корова - 3 // Пока что новые варианты не работают
                // Cow cow = (Cow) event.getEntity();
                // Cow.Variant cowVariant = cow.getVariant();
                
                // if (cowVariant == Cow.Variant.TEMPERATE) {
                //     dropMobHead(event, "cow", "temperate_cow");
                // } else if (cowVariant == Cow.Variant.COLD) {
                //     dropMobHead(event, "cow", "cold_cow");
                // } else if (cowVariant == Cow.Variant.WARM) {
                //     dropMobHead(event, "cow", "warm_cow");
                // } else {
                //     dropMobHead(event, "cow", "temperate_cow");
                // }

                dropMobHead(event, "cow", "temperate_cow");
            }

            case CHICKEN -> { // Курица - 3
                Chicken chicken = (Chicken) event.getEntity();
                Chicken.Variant chickenVariant = chicken.getVariant();

                if (chickenVariant == Chicken.Variant.TEMPERATE) {
                    dropMobHead(event, "chicken", "temperate_chicken");
                } else if (chickenVariant == Chicken.Variant.COLD) {
                    dropMobHead(event, "chicken", "cold_chicken");
                } else if (chickenVariant == Chicken.Variant.WARM) {
                    dropMobHead(event, "chicken", "warm_chicken");
                } else {
                    dropMobHead(event, "chicken", "temperate_chicken");
                }
            }

            case GHAST -> // Гаст
                dropMobHead(event, "ghast", "ghast");

            case SLIME -> { // Слизень
                Slime slime = (Slime) event.getEntity();

                if (slime.getSize() == 1) {
                    dropMobHead(event, "slime", "slime");
                }
            }

            case SQUID -> // Спрут
                dropMobHead(event, "squid", "squid");

            case WOLF -> { // Волк - 9
                Wolf wolf = (Wolf) event.getEntity();
                Wolf.Variant wolfVariant = wolf.getVariant();
                
                if (wolfVariant == Wolf.Variant.PALE) {
                    dropMobHead(event, "pale_wolf", "pale_wolf");
                } else if (wolfVariant == Wolf.Variant.CHESTNUT) {
                    dropMobHead(event, "chestnut_wolf", "chestnut_wolf");
                } else if (wolfVariant == Wolf.Variant.WOODS) {
                    dropMobHead(event, "woods_wolf", "woods_wolf");
                } else if (wolfVariant == Wolf.Variant.ASHEN) {
                    dropMobHead(event, "ashen_wolf", "ashen_wolf");
                } else if (wolfVariant == Wolf.Variant.STRIPED) {
                    dropMobHead(event, "striped_wolf", "striped_wolf");
                } else if (wolfVariant == Wolf.Variant.SPOTTED) {
                    dropMobHead(event, "spotted_wolf", "spotted_wolf");
                } else if (wolfVariant == Wolf.Variant.RUSTY) {
                    dropMobHead(event, "rusty_wolf", "rusty_wolf");
                } else if (wolfVariant == Wolf.Variant.SNOWY) {
                    dropMobHead(event, "snowy_wolf", "snowy_wolf");
                } else if (wolfVariant == Wolf.Variant.BLACK) {
                    dropMobHead(event, "black_wolf", "black_wolf");
                } else {
                    dropMobHead(event, "pale_wolf", "pale_wolf");
                }
            }

            case MOOSHROOM -> { // Грибная корова - 2
                MushroomCow mushroom_cow = (MushroomCow) event.getEntity();
                MushroomCow.Variant mushroomCowVariant = mushroom_cow.getVariant();

                switch (mushroomCowVariant) {
                    case RED -> dropMobHead(event, "red_mooshroom", "red_mooshroom");
                    case BROWN -> dropMobHead(event, "brown_mooshroom", "brown_mooshroom");
                }
            }

            case SNOW_GOLEM -> { // Снежный голем - 2
                Snowman snowman = (Snowman) event.getEntity();

                if (snowman.isDerp()) {
                    dropMobHead(event, "snowman", "snowman");
                } else {
                    dropMobHead(event, "snow_golem", "snow_golem");
                }
            }

            case BLAZE -> // Всполох
                dropMobHead(event, "blaze", "blaze");

            case MAGMA_CUBE -> { // Магма Куб
                MagmaCube magmaCube = (MagmaCube) event.getEntity();

                if (magmaCube.getSize() == 1) {
                    dropMobHead(event, "magma_cube", "magma_cube");
                }
            }

            case OCELOT -> // Оцелот
                dropMobHead(event, "ocelot", "ocelot");

            case CAT -> { // Кошка - 11
                Player killer = event.getEntity().getKiller();
                if (killer == null) { return; }

                killer.sendMessage(
                        "§5* " + killer.getName() + " §5зверски убивает кошку, и понижает свою репутацию");

                Cat cat = (Cat) event.getEntity();
                Cat.Type catType = cat.getCatType();

                if (catType == Cat.Type.TABBY) {
                    dropMobHead(event, "cat", "tabby_cat");
                } else if (catType == Cat.Type.BLACK) {
                    dropMobHead(event, "cat", "black_cat");
                } else if (catType == Cat.Type.RED) {
                    dropMobHead(event, "cat", "red_cat");
                } else if (catType == Cat.Type.SIAMESE) {
                    dropMobHead(event, "cat", "siamese_cat");
                } else if (catType == Cat.Type.BRITISH_SHORTHAIR) {
                    dropMobHead(event, "cat", "british_shorthair_cat");
                } else if (catType == Cat.Type.CALICO) {
                    dropMobHead(event, "cat", "calico_cat");
                } else if (catType == Cat.Type.PERSIAN) {
                    dropMobHead(event, "cat", "persian_cat");
                } else if (catType == Cat.Type.RAGDOLL) {
                    dropMobHead(event, "cat", "ragdoll_cat");
                } else if (catType == Cat.Type.WHITE) {
                    dropMobHead(event, "cat", "white_cat");
                } else if (catType == Cat.Type.JELLIE) {
                    dropMobHead(event, "cat", "jellie_cat");
                } else if (catType == Cat.Type.ALL_BLACK) {
                    dropMobHead(event, "cat", "all_black_cat");
                }
            }

            case IRON_GOLEM -> // Железный голем
                dropMobHead(event, "iron_golem", "iron_golem");

            case BAT -> // Летучая Мышь
                dropMobHead(event, "bat", "bat");

            case WITCH -> // Ведьма
                dropMobHead(event, "witch", "witch");

            case WITHER_SKELETON -> { // Визер-скелет (ванильный дроп головы)
                Player killer = event.getEntity().getKiller();
                if (killer == null) { return; }
                
                if (event.getDrops().stream().anyMatch(item -> item.getType() == Material.WITHER_SKELETON_SKULL)) {
                    triumphOfDropHead(
                        killer, DropHeadsPlugin.config.getString("wither_skeleton" + TAG_SUFFIX_HEAD_NAME));
                }
            }

            case HORSE -> { // Лошадь - 35
                Horse horse = (Horse) event.getEntity();
                Horse.Color horseColor = horse.getColor();
                Horse.Style horseStyle = horse.getStyle();

                dropMobHead(event, "horse", generateHorseTextureTag(horseColor, horseStyle));
            }

            case SKELETON_HORSE -> // Лошадь-скелет
                dropMobHead(event, "skeleton_horse", "skeleton_horse");

            case ZOMBIE_HORSE -> // Лошадь-зомби
                dropMobHead(event, "zombie_horse", "zombie_horse");

            case DONKEY -> // Осёл
                dropMobHead(event, "donkey", "donkey");

            case MULE -> // Мул
                dropMobHead(event, "mule", "mule");

            case ENDERMAN -> // Эндермен
                dropMobHead(event, "enderman", "enderman");

            case ENDERMITE -> // Эндермит
                dropMobHead(event, "endermite", "endermite");

            case RABBIT -> { // Кролик - 8
                Rabbit rabbit = (Rabbit) event.getEntity();
                Rabbit.Type rabbitType = rabbit.getRabbitType();

                if (rabbit.getName().equals("Toast")) {
                    dropMobHead(event, "toast_rabbit", "toast_rabbit");
                }

                switch (rabbitType) {
                    case BROWN -> dropMobHead(event, "rabbit", "brown_rabbit");
                    case WHITE -> dropMobHead(event, "rabbit", "white_rabbit");
                    case BLACK -> dropMobHead(event, "rabbit", "black_rabbit");
                    case BLACK_AND_WHITE -> dropMobHead(event, "rabbit", "black_and_white_rabbit");
                    case GOLD -> dropMobHead(event, "rabbit", "gold_rabbit");
                    case SALT_AND_PEPPER -> dropMobHead(event, "rabbit", "salt_and_pepper_rabbit");
                    case THE_KILLER_BUNNY -> dropMobHead(event, "killer_bunny", "killer_bunny");
                }
            }

            case CAVE_SPIDER -> // Пещерный паук
                dropMobHead(event, "cave_spider", "cave_spider");

            case GUARDIAN -> // Страж
                dropMobHead(event, "guardian", "guardian");

            case ELDER_GUARDIAN -> // Древний Страж
                dropMobHead(event, "elder_guardian", "elder_guardian");

            case SILVERFISH -> // Чешуйница
                dropMobHead(event, "silverfish", "silverfish");

            case SHULKER -> // Шалкер
                dropMobHead(event, "shulker", "shulker");

            case POLAR_BEAR -> // Белый медведь
                dropMobHead(event, "polar_bear", "polar_bear");

            case HUSK -> // Кадавр
                dropMobHead(event, "husk", "husk");

            case STRAY -> // Зимогор
                dropMobHead(event, "stray", "stray");

            case LLAMA -> { // Лама - 4
                Llama llama = (Llama) event.getEntity();
                Llama.Color llamaColor = llama.getColor();

                switch (llamaColor) {
                    case WHITE -> dropMobHead(event, "llama", "white_llama");
                    case GRAY -> dropMobHead(event, "llama", "gray_llama");
                    case CREAMY -> dropMobHead(event, "llama", "creamy_llama");
                    case BROWN -> dropMobHead(event, "llama", "brown_llama");
                }
            }

            case EVOKER -> // Заклинатель
                dropMobHead(event, "evoker", "evoker");

            case VEX -> // Вредина
                dropMobHead(event, "vex", "vex");

            case VINDICATOR -> // Поборник
                dropMobHead(event, "vindicator", "vindicator");

            case PARROT -> {
                Parrot parrot = (Parrot) event.getEntity();
                Parrot.Variant parrotVariant = parrot.getVariant();

                switch (parrotVariant) {
                    case RED -> dropMobHead(event, "parrot", "red_parrot");
                    case BLUE -> dropMobHead(event, "parrot", "blue_parrot");
                    case GRAY -> dropMobHead(event, "parrot", "gray_parrot");
                    case CYAN -> dropMobHead(event, "parrot", "cyan_parrot");
                    case GREEN -> dropMobHead(event, "parrot", "green_parrot");
                }
            }

            case COD -> // Треска
                dropMobHead(event, "cod", "cod");

            case SALMON -> // Лосось
                dropMobHead(event, "salmon", "salmon");

            case TROPICAL_FISH -> // Тропическая рыба
                dropMobHead(event, "tropical_fish", "tropical_fish");

            case PUFFERFISH -> // Иглобрюх
                dropMobHead(event, "pufferfish", "pufferfish");

            case TURTLE -> // Черепаха
                dropMobHead(event, "turtle", "turtle");

            case DOLPHIN -> // Дельфин
                dropMobHead(event, "dolphin", "dolphin");

            case DROWNED -> // Утопленник
                dropMobHead(event, "drowned", "drowned");

            case PHANTOM -> // Фантом
                dropMobHead(event, "phantom", "phantom");

            case FOX -> { // Лиса - 2
                Fox fox = (Fox) event.getEntity();
                Fox.Type foxType = fox.getFoxType();

                switch (foxType) {
                    case RED -> dropMobHead(event, "fox", "red_fox");
                    case SNOW -> dropMobHead(event, "fox", "snow_fox");
                }
            }

            case WANDERING_TRADER -> // Странствующий торговец
                dropMobHead(event, "wandering_trader", "wandering_trader");

            case TRADER_LLAMA -> { // Лама странствующего торговца - 4
                TraderLlama traderllama = (TraderLlama) event.getEntity();
                TraderLlama.Color traderllamaColor = traderllama.getColor();

                switch (traderllamaColor) {
                    case WHITE -> dropMobHead(event, "trader_llama", "white_trader_llama");
                    case GRAY -> dropMobHead(event, "trader_llama", "gray_trader_llama");
                    case CREAMY -> dropMobHead(event, "trader_llama", "creamy_trader_llama");
                    case BROWN -> dropMobHead(event, "trader_llama", "brown_trader_llama");
                }
            }

            case PANDA -> { // Панда - 7
                Panda panda = (Panda) event.getEntity();
                Panda.Gene pandaMainGene = panda.getMainGene();
                Panda.Gene pandaHiddenGene = panda.getHiddenGene();

                switch (pandaMainGene) {
                    case AGGRESSIVE -> dropMobHead(event, "panda", "aggressive_panda");
                    case LAZY -> dropMobHead(event, "panda", "lazy_panda");
                    case WORRIED -> dropMobHead(event, "panda", "worried_panda");
                    case PLAYFUL -> dropMobHead(event, "panda", "playful_panda");
                    case WEAK -> {
                        if (pandaHiddenGene == Panda.Gene.WEAK) {
                            dropMobHead(event, "panda", "weak_panda");
                        } else {
                            dropMobHead(event, "panda", "normal_panda");
                        }
                    }
                    case BROWN -> {
                        if (pandaHiddenGene == Panda.Gene.BROWN) {
                            dropMobHead(event, "brown_panda", "brown_panda");
                        } else {
                            dropMobHead(event, "panda", "normal_panda");
                        }
                    }
                    default -> dropMobHead(event, "panda", "normal_panda");
                }
            }

            case PILLAGER -> // Разбойник
                dropMobHead(event, "pillager", "pillager");

            case RAVAGER -> // Разоритель
                dropMobHead(event, "ravager", "ravager");

            case BEE -> { // Пчела - 2
                Bee bee = (Bee) event.getEntity();

                if (bee.hasNectar()) {
                    dropMobHead(event, "pollinated_bee", "pollinated_bee");
                } else {
                    dropMobHead(event, "bee", "bee");
                }
            }

            case STRIDER -> // Лавомерка
                dropMobHead(event, "strider", "strider");

            case ZOMBIFIED_PIGLIN -> // Зомбифицированный пиглин
                dropMobHead(event, "zombified_piglin", "zombified_piglin");

            case HOGLIN -> // Хоглин
                dropMobHead(event, "hoglin", "hoglin");

            case ZOGLIN -> // Зоглин
                dropMobHead(event, "zoglin", "zoglin");

            case PIGLIN -> // Пиглин
                dropMobHead(event, "piglin", "piglin");

            case PIGLIN_BRUTE -> // Брутальный пиглин
                dropMobHead(event, "piglin_brute", "piglin");

            case GLOW_SQUID -> // Светящийся спрут
                dropMobHead(event, "glow_squid", "glow_squid");

            case AXOLOTL -> { // Аксолотль - 5
                Axolotl axolotl = (Axolotl) event.getEntity();
                Axolotl.Variant axolotlVariant = axolotl.getVariant();

                switch (axolotlVariant) {
                    case LUCY -> dropMobHead(event, "axolotl", "lucy_axolotl");
                    case WILD -> dropMobHead(event, "axolotl", "wild_axolotl");
                    case GOLD -> dropMobHead(event, "axolotl", "gold_axolotl");
                    case CYAN -> dropMobHead(event, "axolotl", "cyan_axolotl");
                    case BLUE -> dropMobHead(event, "blue_axolotl", "blue_axolotl");
                }
            }

            case GOAT -> { // Коза - 8
                Goat goat = (Goat) event.getEntity();
                boolean goatHasLeftHorn = goat.hasLeftHorn();
                boolean goatHasRightHorn = goat.hasRightHorn();

                if (goat.isScreaming()) {
                    if (goatHasLeftHorn && goatHasRightHorn) {
                        dropMobHead(event, "screaming_goat", "screaming_goat");
                    } else if (goatHasLeftHorn) {
                        dropMobHead(event, "screaming_goat_left", "screaming_goat_left");
                    } else if (goatHasRightHorn) {
                        dropMobHead(event, "screaming_goat_right", "screaming_goat_right");
                    } else {
                        dropMobHead(event, "screaming_goat_none", "screaming_goat_none");
                    }
                } else {
                    if (goatHasLeftHorn && goatHasRightHorn) {
                        dropMobHead(event, "goat", "goat");
                    } else if (goatHasLeftHorn) {
                        dropMobHead(event, "goat_left", "goat_left");
                    } else if (goatHasRightHorn) {
                        dropMobHead(event, "goat_right", "goat_right");
                    } else {
                        dropMobHead(event, "goat_none", "goat_none");
                    }
                }
            }

            case TADPOLE -> // Головастик
                dropMobHead(event, "tadpole", "tadpole");

            case FROG -> { // Лягушка - 3
                Frog frog = (Frog) event.getEntity();
                Frog.Variant frogVariant = frog.getVariant();

                if (frogVariant == Frog.Variant.COLD) {
                    dropMobHead(event, "frog", "cold_frog");
                } else if (frogVariant == Frog.Variant.TEMPERATE) {
                    dropMobHead(event, "frog", "temperate_frog");
                } else if (frogVariant == Frog.Variant.WARM) {
                    dropMobHead(event, "frog", "warm_frog");
                }
            }

            case ALLAY -> // Тихоня
                dropMobHead(event, "allay", "allay");

            case WARDEN -> // Хранитель
                dropMobHead(event, "warden", "warden");

            case CAMEL -> // Верблюд
                dropMobHead(event, "camel", "camel");

            case SNIFFER -> // Нюхач
                dropMobHead(event, "sniffer", "sniffer");

            case ARMADILLO -> // Броненосец
                dropMobHead(event, "armadillo", "armadillo");

            case BREEZE -> // Вихрь
                dropMobHead(event, "breeze", "breeze");

            case BOGGED -> // Болотник
                dropMobHead(event, "bogged", "bogged");
            
            case HAPPY_GHAST -> { // Счастливый гаст - 2
                HappyGhast happyGhast = (HappyGhast) event.getEntity();
                
                if (happyGhast instanceof Ageable ageable && !ageable.isAdult()) {
                    dropMobHead(event, "ghastling", "ghastling");
                } else {
                    dropMobHead(event, "happy_ghast", "happy_ghast");
                }
            }

            // Крестьянин  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case VILLAGER -> { // Крестьянин
                Villager villager = (Villager) event.getEntity();
                Villager.Type villagerType = villager.getVillagerType();
                Villager.Profession villagerProfession = villager.getProfession();

                dropMobHead(event, "villager", 
                    generateVillagerTextureTag(villagerType, villagerProfession, "villager"));
            }

            // Крестьянин-зомби  -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            case ZOMBIE_VILLAGER -> { // Крестьянин-зомби
                ZombieVillager zombieVillager = (ZombieVillager) event.getEntity();
                Villager.Type zombieVillagerType = zombieVillager.getVillagerType();
                Villager.Profession zombieVillagerProfession = zombieVillager.getVillagerProfession();

                dropMobHead(event, "zombie_villager", 
                    generateVillagerTextureTag(zombieVillagerType, zombieVillagerProfession, "zombie_villager"));
            }

            // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            default -> {
                // Ничего не делаем для неизвестных типов сущностей
            }
        }
    }

    public static String generateHorseTextureTag(Horse.Color horseColor, Horse.Style horseStyle) {
        String textureTag = "";

        textureTag += switch (horseColor) {
            case WHITE -> "white";
            case CREAMY -> "creamy";
            case CHESTNUT -> "chestnut";
            case BROWN -> "brown";
            case BLACK -> "black";
            case GRAY -> "gray";
            case DARK_BROWN -> "dark_brown";
        };

        textureTag += "_";

        textureTag += switch (horseStyle) {
            case NONE -> "none";
            case WHITE -> "white";
            case WHITEFIELD -> "whitefield";
            case WHITE_DOTS -> "white_dots";
            case BLACK_DOTS -> "black_dots";
        };

        textureTag += "_";

        textureTag += "horse";

        return textureTag;
    }

    public static String generateVillagerTextureTag(
            Villager.Type villagerType, Villager.Profession villagerProfession, String villagerTag) {
        String textureTag = "";

        if (villagerType == Villager.Type.PLAINS) {
            textureTag += "plains";
        } else if (villagerType == Villager.Type.DESERT) {
            textureTag += "desert";
        } else if (villagerType == Villager.Type.JUNGLE) {
            textureTag += "jungle";
        } else if (villagerType == Villager.Type.SAVANNA) {
            textureTag += "savanna";
        } else if (villagerType == Villager.Type.SNOW) {
            textureTag += "snow";
        } else if (villagerType == Villager.Type.SWAMP) {
            textureTag += "swamp";
        } else if (villagerType == Villager.Type.TAIGA) {
            textureTag += "taiga";
        }

        textureTag += "_";

        if (villagerProfession == Villager.Profession.NONE) {
            textureTag += "none";
        } else if (villagerProfession == Villager.Profession.NITWIT) {
            textureTag += "nitwit";
        } else if (villagerProfession == Villager.Profession.ARMORER) {
            textureTag += "armorer";
        } else if (villagerProfession == Villager.Profession.BUTCHER) {
            textureTag += "butcher";
        } else if (villagerProfession == Villager.Profession.CARTOGRAPHER) {
            textureTag += "cartographer";
        } else if (villagerProfession == Villager.Profession.CLERIC) {
            textureTag += "cleric";
        } else if (villagerProfession == Villager.Profession.FARMER) {
            textureTag += "farmer";
        } else if (villagerProfession == Villager.Profession.FISHERMAN) {
            textureTag += "fisherman";
        } else if (villagerProfession == Villager.Profession.FLETCHER) {
            textureTag += "fletcher";
        } else if (villagerProfession == Villager.Profession.LEATHERWORKER) {
            textureTag += "leatherworker";
        } else if (villagerProfession == Villager.Profession.LIBRARIAN) {
            textureTag += "librarian";
        } else if (villagerProfession == Villager.Profession.MASON) {
            textureTag += "mason";
        } else if (villagerProfession == Villager.Profession.SHEPHERD) {
            textureTag += "shepherd";
        } else if (villagerProfession == Villager.Profession.TOOLSMITH) {
            textureTag += "toolsmith";
        } else if (villagerProfession == Villager.Profession.WEAPONSMITH) {
            textureTag += "weaponsmith";
        }

        textureTag += "_";

        textureTag += villagerTag;

        return textureTag;
    }
}
