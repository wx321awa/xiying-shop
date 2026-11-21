package com.xiying.shop.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiying.shop.XiYingShopPlugin;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public final class ItemUtil {
   private static final Map<String, String> ZH = new HashMap<>();
   private static final Map<String, String> ENCHANT_ZH = new HashMap<>();
   private static final Map<String, String> POTION_ZH = new HashMap<>();
   private static final String[] ROMAN = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
   public static final int MAX_ENCODED_BYTES = 32768;

   private ItemUtil() {
   }

   public static void load(XiYingShopPlugin var0) {
      ZH.clear();

      try {
         try (InputStream var1 = var0.getResource("item-names-zh.json")) {
            if (var1 != null) {
               Map var2 = (Map)new Gson().fromJson(new InputStreamReader(var1, StandardCharsets.UTF_8), (new TypeToken<Map<String, String>>() {
               }).getType());
               if (var2 != null) {
                  ZH.putAll(var2);
               }

               var0.getLogger().info("已加载物品中文名 " + ZH.size() + " 条");
               return;
            }
         }
      } catch (Exception var6) {
         var0.getLogger().log(Level.WARNING, "加载物品中文名失败", (Throwable)var6);
      }
   }

   public static String encode(ItemStack var0) {
      ItemStack var1 = var0.clone();
      byte[] var2 = var1.serializeAsBytes();
      if (var2.length > 32768) {
         throw new IllegalArgumentException("物品数据过大（" + var2.length + " 字节，上限 32768）");
      } else {
         return Base64.getEncoder().encodeToString(var2);
      }
   }

   public static ItemStack decode(String var0) {
      try {
         byte[] var1 = Base64.getDecoder().decode(var0);
         return ItemStack.deserializeBytes(var1);
      } catch (Exception var2) {
         return null;
      }
   }

   public static String name(ItemStack var0) {
      if (var0 == null) {
         return "未知物品";
      } else {
         ItemMeta var2 = var0.getItemMeta();
         String var1;
         if (var2 != null && var2.hasDisplayName() && var2.displayName() != null && !(var1 = Msg.plain(var2.displayName())).isBlank()) {
            return var1;
         } else {
            String var3 = var0.getType().getKey().getKey();
            String var4 = ZH.get(var3);
            return var4 != null && !var4.isBlank() ? var4 : pretty(var0.getType());
         }
      }
   }

   public static String describe(ItemStack var0) {
      if (var0 == null) {
         return "未知物品";
      } else {
         String var1 = name(var0);
         String var2 = potionName(var0);
         if (var2 != null) {
            return var2;
         } else {
            List var3 = enchantNames(var0);
            return !var3.isEmpty() ? var1 + " · " + String.join(" / ", var3) : var1;
         }
      }
   }

   private static String potionName(ItemStack var0) {
      Material var2 = var0.getType();
      boolean var3 = false;
      String var1;
      switch (var2) {
         case POTION:
            var1 = "";
            break;
         case SPLASH_POTION:
            var1 = "喷溅";
            break;
         case LINGERING_POTION:
            var1 = "滞留";
            break;
         case TIPPED_ARROW:
            var1 = "";
            var3 = true;
            break;
         default:
            return null;
      }

      if (!(var0.getItemMeta() instanceof PotionMeta var5)) {
         return null;
      } else {
         PotionType var6 = var5.getBasePotionType();
         if (var6 == null) {
            return null;
         } else {
            String var7 = var6.name();
            boolean var8 = var7.startsWith("STRONG_");
            boolean var9 = var7.startsWith("LONG_");
            String var10 = var7.startsWith("STRONG_") ? var7.substring(7) : (var7.startsWith("LONG_") ? var7.substring(5) : var7);
            String var11 = POTION_ZH.getOrDefault(var10, var10.toLowerCase().replace('_', ' '));
            if (var10.equals("WATER") && !var3) {
               return var1 + "水瓶";
            } else {
               String var12 = var11 + (var8 ? " II" : "") + (var9 ? "（延长）" : "");
               return var3 ? "药箭（" + var12 + "）" : var1 + var12 + "药水";
            }
         }
      }
   }

   private static List<String> enchantNames(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      HashMap<Enchantment, Integer> var2 = new HashMap<>();
      if (var1 instanceof EnchantmentStorageMeta var3) {
         var2.putAll(var3.getStoredEnchants());
      }

      if (var1 != null) {
         var2.putAll(var1.getEnchants());
      }

      if (var2.isEmpty()) {
         return List.of();
      } else {
         ArrayList var9 = new ArrayList();

         for (Entry var5 : var2.entrySet()) {
            String var6 = ((Enchantment)var5.getKey()).getKey().getKey();
            String var7 = ENCHANT_ZH.getOrDefault(var6, var6.replace('_', ' '));
            int var8 = (Integer)var5.getValue();
            var9.add(var8 > 1 ? var7 + " " + (var8 < ROMAN.length ? ROMAN[var8] : String.valueOf(var8)) : var7);
            if (var9.size() >= 3) {
               if (var2.size() > 3) {
                  var9.add("等" + (var2.size() - 3) + "项");
               }
               break;
            }
         }

         return var9;
      }
   }

   public static String pretty(Material var0) {
      String var1 = var0.name().toLowerCase().replace('_', ' ');
      StringBuilder var2 = new StringBuilder();

      for (String var6 : var1.split(" ")) {
         if (!var6.isEmpty()) {
            if (!var2.isEmpty()) {
               var2.append(' ');
            }

            var2.append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
         }
      }

      return var2.toString();
   }

   public static int maxStack(ItemStack var0) {
      return Math.max(1, var0.getMaxStackSize());
   }

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
}
