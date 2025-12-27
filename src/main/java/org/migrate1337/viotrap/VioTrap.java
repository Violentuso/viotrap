package org.migrate1337.viotrap;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.migrate1337.viotrap.actions.CustomActionFactory;
import org.migrate1337.viotrap.actions.DenyItemUseCustomAction;
import org.migrate1337.viotrap.commands.*;
import org.migrate1337.viotrap.conditions.ConditionManager;
import org.migrate1337.viotrap.gui.ConditionEditorMenu;
import org.migrate1337.viotrap.gui.PlateSkinCreationMenu;
import org.migrate1337.viotrap.gui.SkinCreationMenu;
import org.migrate1337.viotrap.listeners.ChatListener;
import org.migrate1337.viotrap.listeners.DisorientItemListener;
import org.migrate1337.viotrap.listeners.DivineAuraItemListener;
import org.migrate1337.viotrap.listeners.FirestormItemListener;
import org.migrate1337.viotrap.listeners.PlateItemListener;
import org.migrate1337.viotrap.listeners.RevealItemListener;
import org.migrate1337.viotrap.listeners.TrapItemListener;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.ChatInputHandler;
import org.migrate1337.viotrap.utils.SkinPointsManager;
import org.migrate1337.viotrap.utils.SkinPointsPlaceholder;

public final class VioTrap extends JavaPlugin implements Listener {
    private static VioTrap plugin;
    private static FileConfiguration config;
    private String trapDisplayName;
    private String skinDisplayName;
    private List<String> trapDescription;
    private String trapType;
    private int trapCooldown;
    private int trapDuration;
    private String trapSchematic;
    private String trapMessageNearby;
    private String trapMessageCooldown;
    private String trapMessageFailed;
    private String trapSoundType;
    private float trapSoundVolume;
    private float trapSoundPitch;
    private String trapSoundTypeEnded;
    private float trapSoundVolumeEnded;
    private float trapSoundPitchEnded;
    private String plateDisplayName;
    private List<String> plateDescription;
    private String plateType;
    private int plateCooldown;
    private int plateDuration;
    private String plateMessageNearby;
    private String plateMessageCooldown;
    private String plateMessageFailed;
    private String plateSoundType;
    private float plateSoundVolume;
    private float plateSoundPitch;
    private String plateSoundTypeEnded;
    private float plateSoundVolumeEnded;
    private float plateSoundPitchEnded;
    private String plateForwardSchematic;
    private String plateForwardLeftSchematic;
    private String plateForwardRightSchematic;
    private String plateBackwardSchematic;
    private String plateBackwardLeftSchematic;
    private String plateBackwardRightSchematic;
    private String plateLeftSchematic;
    private String plateRightSchematic;
    private String plateUpSchematic;
    private String plateDownSchematic;
    private String revealItemType;
    private String revealItemDisplayName;
    private List<String> revealItemDescription;
    private int revealItemCooldown;
    private int revealItemRadius;
    private String revealItemSoundType;
    private float revealItemSoundVolume;
    private float revealItemSoundPitch;
    private String revealItemParticleType;
    private int revealItemGlowDuration;
    private String divineAuraItemName;
    private Material divineAuraItemMaterial;
    private List<String> divineAuraItemDescription;
    private int divineAuraItemCooldown;
    private String divineAuraParticleType;
    private String divineAuraSoundType;
    private float divineAuraSoundVolume;
    private float divineAuraSoundPitch;
    private TrapItemListener trapItemListener;
    private PlateItemListener plateItemListener;
    private ChatInputHandler chatInputHandler;
    private Map<String, String> tempSkinData;
    private File trapsFile;
    private File platesFile;
    private FileConfiguration trapsConfig;
    private FileConfiguration platesConfig;
    private File cfile;
    private SkinCreationMenu skinCreationMenu;
    private SkinPointsManager pointsManager;
    private static final String FIREWORK_FLAG_NAME = "viotrap";
    private StateFlag fireworkUseFlag;
    private ActiveSkinsManager activeSkinsManager;
    private final Map<String, String> tempPlateSkinData = new HashMap();
    private ConditionManager conditionManager;
    private ConditionEditorMenu conditionEditorMenu;

    public void onLoad() {
        try {
            this.registerFlags();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            StateFlag flag = new StateFlag("viotrap", true);
            registry.register(flag);
            this.fireworkUseFlag = flag;
        } catch (FlagConflictException var4) {
            Flag<?> existing = registry.get("viotrap");
            if (existing instanceof StateFlag) {
                this.fireworkUseFlag = (StateFlag)existing;
            }
        } catch (IllegalStateException e) {
        }

    }

    public void onEnable() {
        plugin = this;
        config = this.getConfig();
        this.getConfig().options().copyDefaults(true);
        this.cfile = new File(this.getDataFolder(), "config.yml");
        this.saveDefaultConfig();
        this.activeSkinsManager = new ActiveSkinsManager(this);
        this.loadTrapsConfig();
        this.loadPlatesConfig();
        this.loadPlateConfig();
        this.loadRevealItemConfig();
        this.loadDivineAuraItemConfig();
        this.plateItemListener = new PlateItemListener(this);
        this.trapItemListener = new TrapItemListener(this);
        this.getServer().getPluginManager().registerEvents(this.trapItemListener, this);
        this.getServer().getPluginManager().registerEvents(this.plateItemListener, this);
        this.getServer().getPluginManager().registerEvents(new RevealItemListener(this), this);
        this.getServer().getPluginManager().registerEvents(new DisorientItemListener(this), this);
        this.getServer().getPluginManager().registerEvents(new DivineAuraItemListener(this), this);
        this.skinCreationMenu = new SkinCreationMenu(this);
        this.conditionManager = new ConditionManager(this);
        this.conditionEditorMenu = new ConditionEditorMenu(this);
        this.getServer().getPluginManager().registerEvents(this.conditionEditorMenu, this);
        this.getServer().getPluginManager().registerEvents(this.skinCreationMenu, this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "minecraft:client_settings");

        this.pointsManager = new SkinPointsManager(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new SkinPointsPlaceholder(this, this.pointsManager, this.activeSkinsManager)).register();
        }
        VioTrapCommand mainCommand = new VioTrapCommand(this, this.pointsManager, this.activeSkinsManager);
        addDefaultConditionMessages();

        if (this.getCommand("viotrap") != null) {
            this.getCommand("viotrap").setExecutor(mainCommand);
            this.getCommand("viotrap").setTabCompleter(mainCommand);
        }
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        this.getServer().getPluginManager().registerEvents(new FirestormItemListener(this), this);
        this.getServer().getPluginManager().registerEvents(this, this);

        this.getServer().getPluginManager().registerEvents(new PlateSkinCreationMenu(this), this);
        DenyItemUseCustomAction denyListener = new DenyItemUseCustomAction("dummy", new HashSet<>());
        getServer().getPluginManager().registerEvents(denyListener, this);

        this.loadTrapConfig();
        this.chatInputHandler = new ChatInputHandler();
        this.tempSkinData = new HashMap();
    }

    public void onDisable() {
        if (this.trapItemListener != null) {
            this.trapItemListener.removeAllTraps();
        } else {
        }

        if (this.plateItemListener != null) {
            this.plateItemListener.removeAllPlates();
        } else {
        }

    }

    public SkinCreationMenu getSkinCreationMenu() {
        return this.skinCreationMenu;
    }

    public void loadTrapsConfig() {
        this.trapsFile = new File(this.getDataFolder(), "traps.yml");
        if (!this.trapsFile.exists()) {
            try {
                this.trapsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.trapsConfig = YamlConfiguration.loadConfiguration(this.trapsFile);
        this.trapsConfig.set("traps", (Object)null);
        this.saveTrapsConfig();
    }

    public void saveTrapsConfig() {
        try {
            this.trapsConfig.save(this.trapsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadPlatesConfig() {
        this.platesFile = new File(plugin.getDataFolder(), "plats.yml");
        if (!this.platesFile.exists()) {
            try {
                this.platesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.platesConfig = YamlConfiguration.loadConfiguration(this.platesFile);
    }

    public void savePlatesConfig() {
        try {
            this.platesConfig.save(this.platesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public FileConfiguration getTrapsConfig() {
        return this.trapsConfig;
    }

    public FileConfiguration getPlatesConfig() {
        return this.platesConfig;
    }

    public void loadTrapConfig() {
        this.trapDisplayName = config.getString("trap.display_name");
        this.trapDescription = config.getStringList("trap.description");
        this.trapType = config.getString("trap.type");
        this.trapCooldown = config.getInt("trap.cooldown");
        this.trapDuration = config.getInt("trap.duration");
        this.trapMessageNearby = config.getString("trap.messages.already_nearby");
        this.trapMessageCooldown = config.getString("trap.messages.cooldown_message");
        this.trapMessageFailed = config.getString("trap.messages.trap_failed");
        this.trapSchematic = config.getString("trap.schematic");
        this.trapSoundType = config.getString("trap.sound.type", "BLOCK_PISTON_CONTRACT");
        this.trapSoundVolume = (float)config.getDouble("trap.sound.volume", (double)10.0F);
        this.trapSoundPitch = (float)config.getDouble("trap.sound.pitch", (double)1.0F);
        this.trapSoundTypeEnded = config.getString("trap.sound.type-ended", "BLOCK_PISTON_EXTEND");
        this.trapSoundVolumeEnded = (float)config.getDouble("trap.sound.volume-ended", (double)10.0F);
        this.trapSoundPitchEnded = (float)config.getDouble("trap.sound.pitch-ended", (double)1.0F);
        if (this.trapItemListener != null) {
            this.trapItemListener.getSkinActions().clear();
            this.trapItemListener.getSkinActions().put("default", CustomActionFactory.loadActions("default", this));

            for(String skin : this.getSkinNames()) {
                this.trapItemListener.getSkinActions().put(skin, CustomActionFactory.loadActions(skin, this));
            }
        } else {
        }

    }

    public Map<String, String> getSkinFlags(String skinName) {
        Map<String, String> flags = new HashMap();
        String section = skinName.equals("default") ? "trap.flags" : "skins." + skinName + ".flags";
        ConfigurationSection flagsSection = config.getConfigurationSection(section);
        if (flagsSection != null) {
            for(String flagName : flagsSection.getKeys(false)) {
                if (flagsSection.isString(flagName)) {
                    flags.put(flagName, flagsSection.getString(flagName));
                } else if (flagsSection.isConfigurationSection(flagName)) {
                    ConfigurationSection flagSection = flagsSection.getConfigurationSection(flagName);
                    if (flagSection != null) {
                        for(String innerKey : flagSection.getKeys(false)) {
                            flags.put(flagName, innerKey + ":" + flagSection.getString(innerKey));
                        }
                    }
                }
            }
        }

        return flags;
    }

    public void loadPlateConfig() {
        this.plateDisplayName = config.getString("plate.display_name", "§6Пласт");
        this.plateDescription = config.getStringList("plate.description");
        this.plateType = config.getString("plate.type");
        this.plateCooldown = config.getInt("plate.cooldown", 1);
        this.plateDuration = config.getInt("plate.duration", 5);
        this.plateMessageNearby = config.getString("plate.messages.already_nearby");
        this.plateMessageCooldown = config.getString("plate.messages.cooldown_message");
        this.plateMessageFailed = config.getString("plate.messages.placement_failed");
        this.plateSoundType = config.getString("plate.sound.type", "BLOCK_ANVIL_PLACE");
        this.plateSoundVolume = (float)config.getDouble("plate.sound.volume", (double)10.0F);
        this.plateSoundPitch = (float)config.getDouble("plate.sound.pitch", (double)1.0F);
        this.plateSoundTypeEnded = config.getString("plate.sound.type-ended", "BLOCK_ANVIL_PLACE");
        this.plateSoundVolumeEnded = (float)config.getDouble("plate.sound.volume-ended", (double)10.0F);
        this.plateSoundPitchEnded = (float)config.getDouble("plate.sound.pitch-ended", (double)1.0F);
        this.plateForwardSchematic = config.getString("plate.forward_schematic", "plate_forward.schem");
        this.plateForwardLeftSchematic = config.getString("plate.forward_left_schematic", "plate_forward_left.schem");
        this.plateForwardRightSchematic = config.getString("plate.forward_right_schematic", "plate_forward_right.schem");
        this.plateBackwardSchematic = config.getString("plate.backward_schematic", "plate_backward.schem");
        this.plateBackwardLeftSchematic = config.getString("plate.backward_left_schematic", "plate_backward_left.schem");
        this.plateBackwardRightSchematic = config.getString("plate.backward_right_schematic", "plate_backward_right.schem");
        this.plateLeftSchematic = config.getString("plate.left_schematic", "plate_left.schem");
        this.plateRightSchematic = config.getString("plate.right_schematic", "plate_right.schem");
        this.plateUpSchematic = config.getString("plate.up_schematic", "plate_up.schem");
        this.plateDownSchematic = config.getString("plate.down_schematic", "plate.schem");
    }

    public void loadRevealItemConfig() {
        this.revealItemType = config.getString("reveal_item.type");
        this.revealItemDisplayName = config.getString("reveal_item.display_name");
        this.revealItemDescription = config.getStringList("reveal_item.description");
        this.revealItemCooldown = config.getInt("reveal_item.cooldown");
        this.revealItemRadius = config.getInt("reveal_item.radius");
        this.revealItemSoundType = config.getString("reveal_item.sound.type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.revealItemSoundVolume = (float)config.getDouble("reveal_item.sound.volume", (double)1.0F);
        this.revealItemSoundPitch = (float)config.getDouble("reveal_item.sound.pitch", (double)1.0F);
        this.revealItemParticleType = config.getString("reveal_item.particle_type", "END_ROD");
        this.revealItemGlowDuration = config.getInt("reveal_item.glow_duration");
    }

    public void loadDivineAuraItemConfig() {
        this.divineAuraItemName = config.getString("divine_aura.name", "Божья Аура");
        this.divineAuraItemMaterial = Material.getMaterial(config.getString("divine_aura.material", "GHAST_TEAR"));
        this.divineAuraItemDescription = config.getStringList("divine_aura.description");
        this.divineAuraItemCooldown = config.getInt("divine_aura.cooldown", 10);
        this.divineAuraParticleType = config.getString("divine_aura.particle_type", "VILLAGER_HAPPY");
        this.divineAuraSoundType = config.getString("divine_aura.sound.type", "ENTITY player_LEVELUP");
        this.divineAuraSoundVolume = (float)config.getDouble("divine_aura.sound.volume", (double)1.0F);
        this.divineAuraSoundPitch = (float)config.getDouble("divine_aura.sound.pitch", (double)1.0F);
    }

    public void loadDisorientItemConfig() {
        this.divineAuraItemName = config.getString("divine_aura.name", "Божья Аура");
        this.divineAuraItemMaterial = Material.getMaterial(config.getString("divine_aura.material", "GHAST_TEAR"));
        this.divineAuraItemDescription = config.getStringList("divine_aura.description");
        this.divineAuraItemCooldown = config.getInt("divine_aura.cooldown", 10);
        this.divineAuraParticleType = config.getString("divine_aura.particle_type", "VILLAGER_HAPPY");
        this.divineAuraSoundType = config.getString("divine_aura.sound.type", "ENTITY_PLAYER_LEVELUP");
        this.divineAuraSoundVolume = (float)config.getDouble("divine_aura.sound.volume", (double)1.0F);
        this.divineAuraSoundPitch = (float)config.getDouble("divine_aura.sound.pitch", (double)1.0F);
    }

    public static VioTrap getPlugin() {
        return plugin;
    }

    public String getTrapDisplayName() {
        return this.trapDisplayName;
    }

    public String getTrapSchematic() {
        return this.trapSchematic;
    }

    public List<String> getTrapDescription() {
        return this.trapDescription;
    }

    public String getTrapType() {
        return this.trapType;
    }

    public int getTrapCooldown() {
        return this.trapCooldown;
    }

    public int getTrapDuration() {
        return this.trapDuration;
    }

    public String getTrapMessageNearby() {
        return this.trapMessageNearby;
    }

    public String getTrapMessageCooldown() {
        return this.trapMessageCooldown;
    }

    public String getTrapMessageFailed() {
        return this.trapMessageFailed;
    }

    public String getTrapSoundType() {
        return this.trapSoundType;
    }

    public float getTrapSoundVolume() {
        return this.trapSoundVolume;
    }

    public float getTrapSoundPitch() {
        return this.trapSoundPitch;
    }

    public String getTrapSoundTypeEnded() {
        return this.trapSoundTypeEnded;
    }

    public float getTrapSoundVolumeEnded() {
        return this.trapSoundVolumeEnded;
    }

    public Map<String, String> getTempPlateSkinData() {
        return this.tempPlateSkinData;
    }

    public float getTrapSoundPitchEnded() {
        return this.trapSoundPitchEnded;
    }

    public String getPlateDisplayName() {
        return this.plateDisplayName;
    }

    public List<String> getPlateDescription() {
        return this.plateDescription;
    }

    public String getPlateType() {
        return this.plateType;
    }

    public int getPlateCooldown() {
        return this.plateCooldown;
    }

    public int getPlateDuration() {
        return this.plateDuration;
    }

    public String getPlateMessageNearby() {
        return this.plateMessageNearby;
    }

    public String getPlateMessageCooldown() {
        return this.plateMessageCooldown;
    }

    public String getPlateMessageFailed() {
        return this.plateMessageFailed;
    }

    public String getPlateSoundType() {
        return this.plateSoundType;
    }

    public float getPlateSoundVolume() {
        return this.plateSoundVolume;
    }

    public float getPlateSoundPitch() {
        return this.plateSoundPitch;
    }

    public String getPlateSoundTypeEnded() {
        return this.plateSoundTypeEnded;
    }

    public float getPlateSoundVolumeEnded() {
        return this.plateSoundVolumeEnded;
    }

    public float getPlateSoundPitchEnded() {
        return this.plateSoundPitchEnded;
    }

    public String getPlateForwardSchematic() {
        return this.plateForwardSchematic;
    }

    public String getPlateForwardLeftSchematic() {
        return this.plateForwardLeftSchematic;
    }

    public String getPlateForwardRightSchematic() {
        return this.plateForwardRightSchematic;
    }

    public String getPlateBackwardSchematic() {
        return this.plateBackwardSchematic;
    }

    public String getPlateBackwardLeftSchematic() {
        return this.plateBackwardLeftSchematic;
    }

    public String getPlateBackwardRightSchematic() {
        return this.plateBackwardRightSchematic;
    }

    public String getPlateLeftSchematic() {
        return this.plateLeftSchematic;
    }

    public String getPlateRightSchematic() {
        return this.plateRightSchematic;
    }

    public String getPlateUpSchematic() {
        return this.plateUpSchematic;
    }

    public String getPlateDownSchematic() {
        return this.plateDownSchematic;
    }

    public String getRevealItemType() {
        return this.revealItemType;
    }

    public String getRevealItemDisplayName() {
        return this.revealItemDisplayName;
    }

    public List<String> getRevealItemDescription() {
        return this.revealItemDescription;
    }

    public int getRevealItemCooldown() {
        return this.revealItemCooldown;
    }

    public int getRevealItemRadius() {
        return this.revealItemRadius;
    }

    public String getRevealItemSoundType() {
        return this.revealItemSoundType;
    }

    public float getRevealItemSoundVolume() {
        return this.revealItemSoundVolume;
    }

    public float getRevealItemSoundPitch() {
        return this.revealItemSoundPitch;
    }

    public String getRevealItemParticleType() {
        return this.revealItemParticleType;
    }

    public int getRevealItemGlowDuration() {
        return this.revealItemGlowDuration;
    }

    public int getDisorientItemCooldown() {
        return config.getInt("disorient_item.cooldown", 10);
    }

    public int getDisorientItemEffectDuration() {
        return config.getInt("disorient_item.effect_duration", 5);
    }

    public String getPlateSchematic(String skin, String direction) {
        return this.getConfig().getString("plate_skins." + skin + "." + direction);
    }

    public List<String> getPlateSkinDescription(String skin) {
        return this.getConfig().getStringList("plate_skins." + skin + ".desc_for_plate");
    }

    public String getPlateSkinDisplayName(String skin) {
        return this.getConfig().getString("plate_skins." + skin + ".name", "§6Пласт");
    }

    public List<String> getPlateSkinNames() {
        ConfigurationSection plateSkins = this.getConfig().getConfigurationSection("plate_skins");
        return plateSkins != null ? new ArrayList(plateSkins.getKeys(false)) : new ArrayList();
    }

    public int getDisorientItemRadius() {
        return config.getInt("disorient_item.radius", 10);
    }

    public String getDisorientItemSoundType() {
        return config.getString("disorient_item.sound.type", "ENTITY_WITHER_AMBIENT");
    }

    public float getDisorientItemSoundVolume() {
        return (float)config.getDouble("disorient_item.sound.volume", (double)1.0F);
    }

    public float getDisorientItemSoundPitch() {
        return (float)config.getDouble("disorient_item.sound.pitch", (double)1.0F);
    }

    public String getDisorientItemParticleType() {
        return config.getString("disorient_item.particle_type", "SMOKE_LARGE");
    }

    public String getDisorientItemName() {
        return config.getString("disorient_item.display_name", "Дезориентация");
    }

    public String getDisorientItemType() {
        return config.getString("disorient_item.type", "ENDER_EYE");
    }

    public List<String> getDisorientItemDescription() {
        return config.getStringList("disorient_item.description");
    }

    public String getDivineAuraItemName() {
        return this.divineAuraItemName;
    }

    public Material getDivineAuraItemMaterial() {
        return this.divineAuraItemMaterial;
    }

    public List<String> getDivineAuraItemDescription() {
        return this.divineAuraItemDescription;
    }

    public int getDivineAuraItemCooldown() {
        return this.divineAuraItemCooldown;
    }

    public String getDivineAuraItemParticleType() {
        return this.divineAuraParticleType;
    }

    public String getDivineAuraItemSoundType() {
        return this.divineAuraSoundType;
    }

    public float getDivineAuraItemSoundVolume() {
        return this.divineAuraSoundVolume;
    }

    public float getDivineAuraItemSoundPitch() {
        return this.divineAuraSoundPitch;
    }

    public ChatInputHandler getChatInputHandler() {
        return this.chatInputHandler;
    }

    public Map<String, String> getTempSkinData() {
        return this.tempSkinData;
    }

    public String getSkinSchematic(String skinName) {
        return skinName != null && !skinName.equals("default") ? config.getString("skins." + skinName + ".schem", this.trapSchematic) : this.trapSchematic;
    }

    public List<String> getSkinDescription(String skinName) {
        return skinName != null && !skinName.equals("default") ? config.getStringList("skins." + skinName + ".desc_for_trap") : this.trapDescription;
    }

    public String getSkinDisplayName(String skinName) {
        return skinName != null && !skinName.equals("default") ? config.getString("skins." + skinName + ".name") : this.trapDisplayName;
    }

    public List<String> getSkinNames() {
        ConfigurationSection skinsSection = config.getConfigurationSection("skins");
        List<String> skins = new ArrayList();
        skins.add("default");
        if (skinsSection != null) {
            skins.addAll(skinsSection.getKeys(false));
        }

        return skins;
    }
    private void addDefaultConditionMessages() {
        ConfigurationSection cmSection = getConfig().getConfigurationSection("condition_messages");
        if (cmSection == null) {
            cmSection = getConfig().createSection("condition_messages");
        }

        setDefaultMessageSection("permission", "У вас нет нужных прав!", "chat");
        setDefaultMessageSection("block_below", "Вы стоите на неверном блоке!", "chat");
        setDefaultMessageSection("is_sneaking", "Вы должны (не) красться!", "chat");
        setDefaultMessageSection("min_health", "У вас недостаточно здоровья!", "chat");
        setDefaultMessageSection("gamemode", "Вы в неверном режиме игры!", "chat");
        setDefaultMessageSection("offhand_item", "В левой руке неверный предмет!", "chat");
        setDefaultMessageSection("in_region", "Вы не в нужном регионе!", "chat");
        setDefaultMessageSection("not_in_region", "Вы в запрещенном регионе!", "chat");
        setDefaultMessageSection("is_flying", "Вы должны летать!", "chat");
        setDefaultMessageSection("not_flying", "Вы не должны летать!", "chat");
        setDefaultMessageSection("has_effect", "У вас нет нужного эффекта!", "chat");
        setDefaultMessageSection("no_effect", "У вас есть запрещенный эффект!", "chat");
        setDefaultMessageSection("in_biome", "Вы не в нужном биоме!", "chat");
        setDefaultMessageSection("not_in_biome", "Вы в запрещенном биоме!", "chat");
        setDefaultMessageSection("is_swimming", "Вы должны плыть!", "chat");
        setDefaultMessageSection("not_swimming", "Вы не должны плыть!", "chat");
        setDefaultMessageSection("default", "Условие не выполнено!", "chat");

        saveConfig();
    }

    private void setDefaultMessageSection(String key, String defaultMessage, String defaultType) {
        String path = "condition_messages." + key;
        if (!getConfig().contains(path + ".message")) {
            getConfig().set(path + ".message", defaultMessage);
            getConfig().set(path + ".type", defaultType);
        }
    }
    public TrapItemListener getTrapItemListener() {
        return this.trapItemListener;
    }

    public int getFirestormItemCooldown() {
        return this.getConfig().getInt("firestorm_item.cooldown", 10);
    }

    public int getFirestormItemRadius() {
        return this.getConfig().getInt("firestorm_item.radius", 5);
    }

    public int getFirestormItemFireDuration() {
        return this.getConfig().getInt("firestorm_item.fire_duration", 5);
    }

    public String getFirestormItemSoundType() {
        return this.getConfig().getString("firestorm_item.sound.type", "ENTITY_BLAZE_SHOOT");
    }
    public ConditionManager getConditionManager() {
        return conditionManager;
    }

    public ConditionEditorMenu getConditionEditorMenu() {
        return conditionEditorMenu;
    }
    public float getFirestormItemSoundVolume() {
        return (float)this.getConfig().getDouble("firestorm_item.sound.volume", (double)1.0F);
    }

    public float getFirestormItemSoundPitch() {
        return (float)this.getConfig().getDouble("firestorm_item.sound.pitch", (double)1.0F);
    }

    public String getFirestormItemName() {
        return this.getConfig().getString("firestorm_item.name", "§cОгненный смерч");
    }

    public String getFirestormItemType() {
        return this.getConfig().getString("firestorm_item.type", "BLAZE_ROD");
    }

    public List<String> getFirestormItemDescription() {
        return this.getConfig().getStringList("firestorm_item.description");
    }


    public SkinPointsManager getPointsManager() {
        return this.pointsManager;
    }


    public ActiveSkinsManager getActiveSkinsManager() {
        return this.activeSkinsManager;
    }

    public SkinPointsManager getSkinPointsManager() {
        return this.pointsManager;
    }

}
