package com.xiying.shop.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiying.shop.XiYingShopPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ItemUtil {
    private static final Map<String, String> ZH = new HashMap<>();
    private static final Map<String, String> ENCHANT_ZH = new HashMap<>();
    private static final Map<String, String> POTION_ZH = new HashMap<>();
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    static {
        ENCHANT_ZH.put("protection", "保护");
        ENCHANT_ZH.put("fire_protection", "火焰保护");
        ENCHANT_ZH.put("feather_falling", "摔落保护");
        ENCHANT_ZH.put("blast_protection", "爆炸保护");
        ENCHANT_ZH.put("projectile_protection", "弹射物保护");
        ENCHANT_ZH.put("respiration", "水下呼吸");
        ENCHANT_ZH.put("aqua_affinity", "水下速掘");
        ENCHANT_ZH.put("thorns", "荆棘");
        ENCHANT_ZH.put("depth_strider", "深海探索者");
        ENCHANT_ZH.put("frost_walker", "冰霜行者");
        ENCHANT_ZH.put("binding_curse", "绑定诅咒");
        ENCHANT_ZH.put("soul_speed", "灵魂疾行");
        ENCHANT_ZH.put("swift_sneak", "迅捷潜行");
        ENCHANT_ZH.put("sharpness", "锋利");
        ENCHANT_ZH.put("smite", "亡灵杀手");
        ENCHANT_ZH.put("bane_of_arthropods", "节肢杀手");
        ENCHANT_ZH.put("knockback", "击退");
        ENCHANT_ZH.put("fire_aspect", "火焰附加");
        ENCHANT_ZH.put("looting", "抢夺");
        ENCHANT_ZH.put("sweeping_edge", "横扫之刃");
        ENCHANT_ZH.put("efficiency", "效率");
        ENCHANT_ZH.put("silk_touch", "精准采集");
        ENCHANT_ZH.put("unbreaking", "耐久");
        ENCHANT_ZH.put("fortune", "时运");
        ENCHANT_ZH.put("power", "力量");
        ENCHANT_ZH.put("punch", "冲击");
        ENCHANT_ZH.put("flame", "火矢");
        ENCHANT_ZH.put("infinity", "无限");
        ENCHANT_ZH.put("luck_of_the_sea", "海之眷顾");
        ENCHANT_ZH.put("lure", "饵钓");
        ENCHANT_ZH.put("loyalty", "忠诚");
        ENCHANT_ZH.put("impaling", "穿刺");
        ENCHANT_ZH.put("riptide", "激流");
        ENCHANT_ZH.put("channeling", "引雷");
        ENCHANT_ZH.put("multishot", "多重射击");
        ENCHANT_ZH.put("quick_charge", "快速装填");
        ENCHANT_ZH.put("piercing", "穿透");
        ENCHANT_ZH.put("mending", "经验修补");
        ENCHANT_ZH.put("vanishing_curse", "消失诅咒");
        ENCHANT_ZH.put("density", "致密");
        ENCHANT_ZH.put("breach", "破甲");
        ENCHANT_ZH.put("wind_burst", "风爆");

        POTION_ZH.put("WATER", "水");
        POTION_ZH.put("MUNDANE", "平凡");
        POTION_ZH.put("THICK", "浓稠");
        POTION_ZH.put("AWKWARD", "尴尬");
        POTION_ZH.put("NIGHT_VISION", "夜视");
        POTION_ZH.put("INVISIBILITY", "隐身");
        POTION_ZH.put("LEAPING", "跳跃");
        POTION_ZH.put("JUMP", "跳跃");
        POTION_ZH.put("FIRE_RESISTANCE", "抗火");
        POTION_ZH.put("SWIFTNESS", "迅捷");
        POTION_ZH.put("SPEED", "迅捷");
        POTION_ZH.put("SLOWNESS", "迟缓");
        POTION_ZH.put("TURTLE_MASTER", "神龟");
        POTION_ZH.put("WATER_BREATHING", "水肺");
        POTION_ZH.put("HEALING", "治疗");
        POTION_ZH.put("HARMING", "伤害");
        POTION_ZH.put("POISON", "剧毒");
        POTION_ZH.put("REGEN", "再生");
        POTION_ZH.put("REGENERATION", "再生");
        POTION_ZH.put("STRENGTH", "力量");
        POTION_ZH.put("WEAKNESS", "虚弱");
        POTION_ZH.put("LUCK", "幸运");
        POTION_ZH.put("SLOW_FALLING", "缓降");
        POTION_ZH.put("WIND_CHARGED", "风爆");
        POTION_ZH.put("WEAVING", "盘丝");
        POTION_ZH.put("OOZING", "渗浆");
        POTION_ZH.put("INFESTED", "蛀蚀");
    }

    private ItemUtil() {
    }

    public static void load(XiYingShopPlugin plugin) {
        ZH.clear();
        try (InputStream in = plugin.getResource("item-names-zh.json")) {
            if (in == null) return;
            Map<String, String> map = new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            if (map != null) ZH.putAll(map);
            plugin.getLogger().info("已加载物品中文名 " + ZH.size() + " 条");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "加载物品中文名失败", e);
        }
    }

    public static String encode(ItemStack stack) {
        ItemStack copy = stack.clone();
        return Base64.getEncoder().encodeToString(copy.serializeAsBytes());
    }

    public static ItemStack decode(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static String name(ItemStack stack) {
        if (stack == null) return "未知物品";
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            String custom = Msg.plain(meta.displayName());
            if (!custom.isBlank()) return custom;
        }
        String key = stack.getType().getKey().getKey();
        String zh = ZH.get(key);
        if (zh != null && !zh.isBlank()) return zh;
        return pretty(stack.getType());
    }

    /**
     * 带细节的物品名：附魔书带上附魔、药水带上效果、装备带上附魔，避免多个同类物品同名分不清。
     */
    public static String describe(ItemStack stack) {
        if (stack == null) return "未知物品";
        String base = name(stack);
        String potion = potionName(stack);
        if (potion != null) return potion;
        List<String> enchants = enchantNames(stack);
        if (!enchants.isEmpty()) return base + " · " + String.join(" / ", enchants);
        return base;
    }

    private static String potionName(ItemStack stack) {
        Material type = stack.getType();
        String prefix;
        boolean arrow = false;
        switch (type) {
            case POTION -> prefix = "";
            case SPLASH_POTION -> prefix = "喷溅";
            case LINGERING_POTION -> prefix = "滞留";
            case TIPPED_ARROW -> {
                prefix = "";
                arrow = true;
            }
            default -> {
                return null;
            }
        }
        if (!(stack.getItemMeta() instanceof PotionMeta meta)) return null;
        PotionType potion = meta.getBasePotionType();
        if (potion == null) return null;
        String raw = potion.name();
        boolean strong = raw.startsWith("STRONG_");
        boolean longer = raw.startsWith("LONG_");
        String core = raw.startsWith("STRONG_") ? raw.substring(7) : raw.startsWith("LONG_") ? raw.substring(5) : raw;
        String zh = POTION_ZH.getOrDefault(core, core.toLowerCase().replace('_', ' '));
        if (core.equals("WATER") && !arrow) return prefix + "水瓶";
        String effect = zh + (strong ? " II" : "") + (longer ? "（延长）" : "");
        return arrow ? "药箭（" + effect + "）" : prefix + effect + "药水";
    }

    private static List<String> enchantNames(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        Map<Enchantment, Integer> map = new HashMap<>();
        if (meta instanceof EnchantmentStorageMeta storage) map.putAll(storage.getStoredEnchants());
        if (meta != null) map.putAll(meta.getEnchants());
        if (map.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> e : map.entrySet()) {
            String key = e.getKey().getKey().getKey();
            String zh = ENCHANT_ZH.getOrDefault(key, key.replace('_', ' '));
            int level = e.getValue();
            out.add(level > 1 ? zh + " " + (level < ROMAN.length ? ROMAN[level] : String.valueOf(level)) : zh);
            if (out.size() >= 3) {
                if (map.size() > 3) out.add("等" + map.size() + "项");
                break;
            }
        }
        return out;
    }

    public static String pretty(Material material) {
        String raw = material.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public static int maxStack(ItemStack stack) {
        return Math.max(1, stack.getMaxStackSize());
    }
}
