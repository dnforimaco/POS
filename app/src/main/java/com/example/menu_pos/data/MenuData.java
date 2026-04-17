package com.example.menu_pos.data;

import com.example.menu_pos.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Static menu data from Bilao Specials, Chill Zone, Silog, Sizzlers, Combo Sizz, Naomie's, Chicken Wings, Ulam.
 */
public final class MenuData {

    private static final List<MenuCategory> CATEGORIES = new ArrayList<>();

    static {
        // --- BILAO SPECIALS ---
        List<MenuItem> bilaoItems = new ArrayList<>();
        bilaoItems.add(MenuItem.builder().id("bilao_miki_bihon").name("Miki Bihon")
                .imageResId(R.drawable.bilao_miki_bihon)
                .variant("5-6 PAX", 55000).variant("10 PAX", 85000).variant("15 PAX", 120000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_pancit_bihon").name("Pancit Bihon")
                .imageResId(R.drawable.bilao_pancit_bihon)
                .variant("5-6 PAX", 55000).variant("10 PAX", 85000).variant("15 PAX", 120000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_lumpiang_shanghai").name("Lumpiang Shanghai")
                .imageResId(R.drawable.bilao_lumpiang_shanghai)
                .variant("45 PCS", 50000).variant("90 PCS", 100000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_canton").name("Canton Guisado")
                .imageResId(R.drawable.bilao_canton_guisado)
                .variant("5-6 PAX", 55000).variant("10 PAX", 85000).variant("15 PAX", 120000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_chami").name("Chami")
                .imageResId(R.drawable.bilao_chami)
                .variant("5-6 PAX", 55000).variant("10 PAX", 85000).variant("15 PAX", 120000).build());
        CATEGORIES.add(MenuCategory.builder().id("bilao").name("Bilao Specials").subtitle("Noodles & lumpia")
                .items(bilaoItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- ULAM / MAIN DISHES (W/RICE) ---
        List<MenuItem> ulamItems = new ArrayList<>();
        ulamItems.add(MenuItem.builder().id("ulam_sinigang_salmon").name("Sinigang na Salmon")
                .variant("W/RICE", 15000).variant("Alacarte", 14000).imageResId(R.drawable.ulam_sinigang_na_salmon).build());
        ulamItems.add(MenuItem.builder().id("ulam_sisig").name("Sizzling Sisig")
                .variant("W/RICE", 15000).variant("Alacarte", 14000).imageResId(R.drawable.ulam_sizzling_sisig).build());
        ulamItems.add(MenuItem.builder().id("ulam_bicol").name("Bicol's Express")
                .variant("W/RICE", 15000).variant("Alacarte", 14000).imageResId(R.drawable.ulam_bicol_express).build());
        ulamItems.add(MenuItem.builder().id("ulam_dinuguan").name("Dinuguan")
                .variant("W/RICE", 10000).variant("Alacarte", 9000).imageResId(R.drawable.ulam_dinuguan).build());
        ulamItems.add(MenuItem.builder().id("ulam_dinakdakan").name("Dinakdakan")
                .variant("W/RICE", 15000).variant("Alacarte", 14000).imageResId(R.drawable.ulam_dinakdakan).build());
        ulamItems.add(MenuItem.builder().id("ulam_ensaladang_talong").name("Ensaladang Talong")
                .variant("W/RICE", 12000).variant("Alacarte", 11000).imageResId(R.drawable.ulam_ensaladangtalong).build());
        ulamItems.add(MenuItem.builder().id("ulam_sinigang_hipon").name("Sinigang na Hipon")
                .variant("W/RICE", 15000).variant("Alacarte", 14000).imageResId(R.drawable.ulam_sinigang_na_hipon).build());
        ulamItems.add(MenuItem.builder().id("ulam_pusit").name("Sizzling Pusit")
                .variant("W/RICE", 20000).variant("Alacarte", 19000).imageResId(R.drawable.ulam_sizzling_pusit).build());
        ulamItems.add(MenuItem.builder().id("ulam_bulalo").name("Bulalo")
                .variant("Solo", 25000).variant("Alacarte", 24000).variant("Good for Sharing", 45000)
                .imageResId(R.drawable.ulam_bulalo).build());
        ulamItems.add(MenuItem.builder().id("ulam_kare_kare").name("Bagnet Kare-Kare")
                .variant("Solo (with rice)", 15000)
                .variant("Alacarte", 14000)
                .variant("3–4 pax", 53500)
                .variant("6–8 pax", 101500)
                .variant("12–16 pax", 199000)
                .imageResId(R.drawable.ulam_kare_kare)
                .build());
        ulamItems.add(MenuItem.builder().id("ulam_relyenong_bangus").name("Relyenong Bangus (À la carte)")
                .variant("", 27000).imageResId(R.drawable.ulam_relyenong_bangus).build());
        ulamItems.add(MenuItem.builder().id("ulam_sinaing_isda").name("Sinaing na Isda")
                .variant("W/RICE", 20000).variant("Alacarte", 19000).imageResId(R.drawable.ulam_sinaing_na_isda).build());
        CATEGORIES.add(MenuCategory.builder().id("ulam").name("Ulam").subtitle("Main dishes with rice")
                .items(ulamItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- NAOMIE'S (Lomi, Bulalo, Gotong, etc.) ---
        List<MenuItem> naomieItems = new ArrayList<>();
        naomieItems.add(MenuItem.builder().id("naomie_lomi").name("Lomi")
                .variant("Regular", 8000).variant("Special", 10000).variant("Super Special", 12000).imageResId(R.drawable.naomies_lomi).build());
        naomieItems.add(MenuItem.builder().id("naomie_bulalo").name("Bulalo")
                .variant("Solo", 25000).variant("Alacarte", 24000).variant("Good for Sharing", 45000)
                .imageResId(R.drawable.ulam_bulalo).build());
        naomieItems.add(MenuItem.builder().id("naomie_gotong").name("Gotong Batangas")
                .variant("Alacarte", 10000).variant("Special", 12000).imageResId(R.drawable.naomies_gotong_batangas).build());
        naomieItems.add(MenuItem.builder().id("naomie_chami").name("Chami Sweet & Spicy").variant("", 10000).imageResId(R.drawable.naomies_chami).build());
        naomieItems.add(MenuItem.builder().id("naomie_miki").name("Miki Bihon").variant("", 12000).imageResId(R.drawable.naomies_miki_bihon).build());
        naomieItems.add(MenuItem.builder().id("naomie_pancit").name("Pancit Bihon").variant("", 11000).imageResId(R.drawable.naomies_pancit_bihon).build());
        naomieItems.add(MenuItem.builder().id("naomie_canton").name("Canton Guisado").variant("", 12000).imageResId(R.drawable.naomies_canton_guisado).build());
        CATEGORIES.add(MenuCategory.builder().id("naomie").name("Naomie's").subtitle("Lomi, Bulalo & Gotong Batangas")
                .items(naomieItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- FLAVORED CHICKEN WINGS (149, 6 pcs w/ rice) ---
        String[] wingNames = {"Lemon Glazed", "Teriyaki", "Garlic Parmesan", "Hickory Barbeque", "Buttered Garlic", "Salted Egg", "Sriracha", "Sweet Chili", "Buffalo", "Korean Spicy Barbeque"};
        int[] wingImages = {
                R.drawable.wings_lemon_glazed,
                R.drawable.wings_teriyaki,
                R.drawable.wings_garlic_parmesan,
                R.drawable.wings_hickory_bbq,
                R.drawable.wings_buttered_garlic,
                R.drawable.wings_salted_egg,
                R.drawable.wings_sriracha,
                R.drawable.wings_sweet_chili,
                R.drawable.wings_buffalo,
                R.drawable.wings_korean_spicy_bbq
        };
        boolean[] wingBestSeller = {false, false, true, false, false, false, false, true, false, true};
        boolean[] wingSpicy = {false, false, false, false, false, false, true, true, false, true};
        List<MenuItem> wingItems = new ArrayList<>();
        for (int i = 0; i < wingNames.length; i++) {
            wingItems.add(MenuItem.builder().id("wings_" + i).name(wingNames[i])
                    .variant("6 PCS W/ RICE", 14900).bestSeller(wingBestSeller[i]).spicy(wingSpicy[i]).imageResId(wingImages[i]).build());
        }
        // À la carte (6 pcs wings) – all existing wing flavors as variants
        wingItems.add(MenuItem.builder().id("wings_ala_carte").name("À la carte (6 pcs wings)")
                .variant("Lemon Glazed", 14000)
                .variant("Teriyaki", 14000)
                .variant("Garlic Parmesan", 14000)
                .variant("Hickory Barbeque", 14000)
                .variant("Buttered Garlic", 14000)
                .variant("Salted Egg", 14000)
                .variant("Sriracha", 14000)
                .variant("Sweet Chili", 14000)
                .variant("Buffalo", 14000)
                .variant("Korean Spicy Barbeque", 14000)
                .imageResId(R.drawable.wings_lemon_glazed)
                .build());
        CATEGORIES.add(MenuCategory.builder().id("wings").name("Flavored Chicken Wings").subtitle("6 pcs with 2 rice · ₱149")
                .items(wingItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- SIZZLERS (109 each) ---
        String[] sizzlerNames = {"Sizzling Backribs", "Sizzling Porkchop", "Sizzling Hungarian", "Sizzling Bangus", "Sizzling Shrimp", "Sizzling Tocino", "Sizzling Hotdog", "Sizzling Chicken", "Sizzling Bagnet", "Sizzling Tapa", "Sizzling Sisig", "Sizzling Liempo", "Sizzling Calamare"};
        int[] sizzlerImages = {
                R.drawable.sizzlers_backribs, R.drawable.sizzlers_porkchop, R.drawable.sizzlers_hungarian,
                R.drawable.sizzlers_bangus, R.drawable.sizzlers_shrimp, R.drawable.sizzlers_tocino,
                R.drawable.sizzlers_hotdog, R.drawable.sizzlers_chicken, R.drawable.sizzlers_bagnet,
                R.drawable.sizzlers_tapa, R.drawable.sizzlers_sisig, R.drawable.sizzlers_liempo,
                R.drawable.sizzlers_calamare
        };
        List<MenuItem> sizzlerItems = new ArrayList<>();
        for (int i = 0; i < sizzlerNames.length; i++) {
            sizzlerItems.add(MenuItem.builder().id("sizzler_" + i).name(sizzlerNames[i]).variant("W/ rice + veggies", 10900)
                    .imageResId(sizzlerImages[i]).build());
        }
        CATEGORIES.add(MenuCategory.builder().id("sizzlers").name("Sizzlers").subtitle("109 Pesos only · Unli gravy")
                .items(sizzlerItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- COMBO SIZZ (179 each) ---
        List<MenuItem> comboSizzItems = new ArrayList<>();
        comboSizzItems.add(MenuItem.builder().id("combo_hungarian_sisig").name("Hungarian-Sisig")
                .variant("", 17900).imageResId(R.drawable.sizzlers_hungarian).build());
        comboSizzItems.add(MenuItem.builder().id("combo_tapa_porkchop").name("Tapa-Porkchop")
                .variant("", 17900).imageResId(R.drawable.sizzlers_tapa).build());
        comboSizzItems.add(MenuItem.builder().id("combo_bagnet_bulaklak").name("Bagnet-Bulaklak")
                .variant("", 17900).imageResId(R.drawable.sizzlers_bagnet).build());
        comboSizzItems.add(MenuItem.builder().id("combo_bagnet_sisig").name("Bagnet-Sisig")
                .variant("", 17900).imageResId(R.drawable.sizzlers_bagnet).build());
        comboSizzItems.add(MenuItem.builder().id("combo_bangus_torta").name("Bangus-Torta")
                .variant("", 17900).imageResId(R.drawable.sizzlers_bangus).build());
        comboSizzItems.add(MenuItem.builder().id("combo_tocino_chicken").name("Tocino-Chicken")
                .variant("", 17900).imageResId(R.drawable.sizzlers_tocino).build());
        comboSizzItems.add(MenuItem.builder().id("combo_custom").name("Custom Combo")
                .variant("", 17900).imageResId(R.drawable.ic_food_placeholder).build());
        comboSizzItems.add(MenuItem.builder().id("combo_hungarian_porkchop").name("Hungarian-Porkchop")
                .variant("", 17900).imageResId(R.drawable.sizzlers_porkchop).build());
        comboSizzItems.add(MenuItem.builder().id("combo_backribs_sisig").name("Backribs-Sisig")
                .variant("", 17900).imageResId(R.drawable.sizzlers_backribs).build());
        CATEGORIES.add(MenuCategory.builder().id("combo_sizz").name("Combo Sizz").subtitle("179 Pesos · combo plates")
                .items(comboSizzItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- SILOG (vector drawables res/drawable/silog_*.xml) ---
        List<MenuItem> silogItems = new ArrayList<>();
        int[] silogPrices = {9500, 9500, 9500, 9500, 10500, 9500, 10500, 10500, 9500, 9000, 8000, 9000, 9500, 9500, 9500, 9500};
        String[] silogNames = {"Nuggets Silog", "Ham Silog", "Pork Sisig Silog", "Bagnet Silog", "Liempo Silog", "Calumpit Silog", "Porkchop Silog", "Bangus Silog", "Cebu Silog", "Tocino Silog", "TJ Hotdog Silog", "Shanghai Silog", "Vigan Silog", "Chicken Silog", "Hungarian Silog", "Beef Tapa Silog"};
        int[] silogImages = {
                R.drawable.silog_nuggets,
                R.drawable.silog_ham,
                R.drawable.silog_sisig,
                R.drawable.silog_bagnet,
                R.drawable.silog_liempo,
                R.drawable.silog_calumpit,
                R.drawable.silog_porkchop,
                R.drawable.silog_bangus,
                R.drawable.silog_cebu,
                R.drawable.siloog_tocino, // no silog_tocino asset yet
                R.drawable.silog_hotdog,
                R.drawable.silog_shanghai,
                R.drawable.silog_vigan,
                R.drawable.silog_chicken,
                R.drawable.silog_hungarian,
                R.drawable.silog_tapa
        };
        for (int i = 0; i < silogNames.length; i++) {
            silogItems.add(MenuItem.builder().id("silog_" + i).name(silogNames[i]).variant("", silogPrices[i])
                    .imageResId(silogImages[i]).build());
        }
        CATEGORIES.add(MenuCategory.builder().id("silog").name("Silog").subtitle("Sinangag + itlog")
                .items(silogItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- CRISPY ---
        List<MenuItem> crispyItems = new ArrayList<>();
        crispyItems.add(MenuItem.builder().id("crispy_bagnet").name("Crispy Bagnet")
                .variant("Solo", 12000)
                .variant("¼ kg", 15000)
                .variant("½ kg", 30000)
                .variant("1 kg", 60000)
                .imageResId(R.drawable.crispies_bagnet)
                .build());
        crispyItems.add(MenuItem.builder().id("crispy_bulaklak").name("Crispy Bulaklak").variant("", 12000).imageResId(R.drawable.crispies_bulaklak).build());
        crispyItems.add(MenuItem.builder().id("crispy_crablets").name("Crispy Crablets").variant("", 12000).imageResId(R.drawable.crispies_crablets).build());
        crispyItems.add(MenuItem.builder().id("crispy_chicken_skin").name("Crispy Chicken Skin").variant("", 12000).imageResId(R.drawable.crispies_chicken_skin).build());
        crispyItems.add(MenuItem.builder().id("crispy_butchi").name("Crispy Butchi").variant("", 12000).imageResId(R.drawable.crispies_butchi).build());
        crispyItems.add(MenuItem.builder().id("crispy_isaw_baboy").name("Crispy Isaw ng Baboy (Tumbong)").variant("", 20000).imageResId(R.drawable.crispies_isaw_ng_baboy).build());
        crispyItems.add(MenuItem.builder().id("crispy_calamare").name("Crispy Calamare").variant("", 12000).imageResId(R.drawable.crispies_calamares).build());
        crispyItems.add(MenuItem.builder().id("crispy_hipon").name("Crispy Hipon").variant("", 14000).imageResId(R.drawable.crispies_hipon).build());
        crispyItems.add(MenuItem.builder().id("crispy_proben").name("Crispy Proben").variant("", 10000).imageResId(R.drawable.crispies_proben).build());
        CATEGORIES.add(MenuCategory.builder().id("crispy").name("Crispy").subtitle("À la carte")
                .items(crispyItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- LONGGANISA ---
        List<MenuItem> longganisaItems = new ArrayList<>();
        longganisaItems.add(MenuItem.builder().id("long_cebu").name("Cebu Longganisa (500g)")
                .variant("Frozen", 21000)
                .variant("Cooked", 21000)
                .imageResId(R.drawable.longganisa_cebu)
                .build());
        longganisaItems.add(MenuItem.builder().id("long_vigan").name("Vigan Longganisa (500g)")
                .variant("Frozen", 17000)
                .variant("Cooked", 17000)
                .imageResId(R.drawable.longganisa_vigan)
                .build());
        longganisaItems.add(MenuItem.builder().id("long_calumpit").name("Calumpit Longganisa (500g)")
                .variant("Frozen", 21000)
                .variant("Cooked", 21000)
                .imageResId(R.drawable.longganisa_calumpit)
                .build());
        longganisaItems.add(MenuItem.builder().id("long_lucban").name("Lucban Longganisa")
                .variant("Frozen", 15000)
                .variant("Cooked", 15000)
                .imageResId(R.drawable.longganisa_lucban)
                .build());
        CATEGORIES.add(MenuCategory.builder().id("longganisa").name("Longganisa").subtitle("By the pack (500g)")
                .items(longganisaItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- APPETIZERS ---
        List<MenuItem> appItems = new ArrayList<>();
        appItems.add(MenuItem.builder().id("app_fries").name("Fries")
                .variant("Sour Cream", 5000)
                .variant("Cheese", 5000)
                .variant("Barbeque", 5000)
                .imageResId(R.drawable.appetizer_fries)
                .build());
        appItems.add(MenuItem.builder().id("app_cheesy_fries").name("Cheesy Fries")
                .variant("", 10000)
                .imageResId(R.drawable.appetizer_cheesyfries)
                .build());
        appItems.add(MenuItem.builder().id("app_dynamite").name("Dynamite").variant("", 7500).imageResId(R.drawable.others_dynamite).build());
        appItems.add(MenuItem.builder().id("app_cheesesticks").name("Cheesesticks").variant("", 7500).imageResId(R.drawable.others_cheesesticks).build());
        appItems.add(MenuItem.builder().id("app_lumpiang_shanghai").name("Lumpiang Shanghai (1 pc)").variant("", 1500).imageResId(R.drawable.bilao_lumpiang_shanghai).build());
        CATEGORIES.add(MenuCategory.builder().id("appetizers").name("Appetizers").subtitle("Starters & sides")
                .items(appItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- CHILL ZONE (Desserts & Drinks) ---
        List<MenuItem> chillItems = new ArrayList<>();
        chillItems.add(MenuItem.builder().id("chill_cone").name("Ice Cream in a Cone")
                .variant("Vanilla", 2000)
                .variant("Chocolate", 2000)
                .build());
        chillItems.add(MenuItem.builder().id("chill_coffee").name("Iced Coffee")
                .variant("Premium", 6000)
                .variant("Caramel", 6000)
                .variant("Cappuccino", 6000)
                .build());
        chillItems.add(MenuItem.builder().id("chill_sundae_twist").name("Sundae Twist")
                .variant("Strawberry", 4000)
                .variant("Caramel", 4000)
                .variant("Chocolate", 4000)
                .variant("Mango", 4000)
                .variant("Blueberry", 4000)
                .build());
        chillItems.add(MenuItem.builder().id("chill_sundae_other").name("Sundae's Best")
                .variant("Sundae Overload", 4500)
                .variant("Mango Graham", 4500)
                .variant("Rocky Road", 4500)
                .build());
        chillItems.add(MenuItem.builder().id("chill_chuckie").name("Float Yoghurt Special")
                .variant("Chuckie Float", 7500)
                .variant("Chuckie Berry", 7500)
                .variant("Berry Yoghurt", 7500)
                .variant("Blue Lemonade Yakult", 7500)
                .build());
        chillItems.add(MenuItem.builder().id("chill_soda_float").name("Soda Float")
                .variant("Coke", 5500)
                .variant("7UP", 5500)
                .variant("Royal", 5500)
                .build());
        chillItems.add(MenuItem.builder().id("chill_fruity").name("Fruity Soda")
                .variant("Strawberry", 5000)
                .variant("Blueberry", 5000)
                .variant("Lychee", 5000)
                .variant("Watermelon", 5000)
                .variant("Blue Lemonade", 5000)
                .variant("Green Apple", 5000)
                .variant("Four Season", 5000)
                .build());
        chillItems.add(MenuItem.builder().id("chill_yakult").name("Add-on: Yakult")
                .variant("Yakult", 2000)
                .build());
        CATEGORIES.add(MenuCategory.builder().id("chill").name("Chill Zone").subtitle("Desserts & drinks")
                .items(chillItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- DRINKS ---
        List<MenuItem> drinksItems = new ArrayList<>();
        drinksItems.add(MenuItem.builder().id("drinks_coke_mismo").name("Coke (Mismo)").variant("", 2500).imageResId(R.drawable.drinks_coke_mismo).build());
        drinksItems.add(MenuItem.builder().id("drinks_royal_mismo").name("Royal (Mismo)").variant("", 2500).imageResId(R.drawable.drinks_royal_mismo).build());
        drinksItems.add(MenuItem.builder().id("drinks_sprite_mismo").name("Sprite (Mismo)").variant("", 2500).imageResId(R.drawable.drinks_sprite_mismo).build());
        drinksItems.add(MenuItem.builder().id("drinks_bottled_water").name("Bottled Water")
                .variant("Small", 2000)
                .variant("Big", 3000)
                .imageResId(R.drawable.drinks_bottled_water)
                .build());
        drinksItems.add(MenuItem.builder().id("drinks_mt_dew_mismo").name("Mt. Dew (Mismo)").variant("", 2500).imageResId(R.drawable.drinks_mt__dew).build());
        drinksItems.add(MenuItem.builder().id("drinks_coke_15l").name("Coke (1.5L)").variant("", 10000).imageResId(R.drawable.drinks_coke_1_5).build());
        drinksItems.add(MenuItem.builder().id("drinks_royal_15l").name("Royal (1.5L)").variant("", 10000).imageResId(R.drawable.drinks_royal_1).build());
        drinksItems.add(MenuItem.builder().id("drinks_sprite_15l").name("Sprite (1.5L)").variant("", 10000).imageResId(R.drawable.drinks_sprite_1_5).build());
        drinksItems.add(MenuItem.builder().id("drinks_sting").name("Sting").variant("", 2500).imageResId(R.drawable.drinks_sting).build());
        CATEGORIES.add(MenuCategory.builder().id("drinks").name("Drinks").subtitle("Soft drinks & water")
                .items(drinksItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- OTHERS ---
        List<MenuItem> otherItems = new ArrayList<>();
        otherItems.add(MenuItem.builder().id("others_plain_rice").name("Plain Rice").variant("", 2000).imageResId(R.drawable.others_plain_rice).build());
        otherItems.add(MenuItem.builder().id("others_fried_rice").name("Fried Rice").variant("", 2500).imageResId(R.drawable.others_java_rice).build());
        otherItems.add(MenuItem.builder().id("others_boiled_egg").name("Boiled Egg").variant("", 2500).imageResId(R.drawable.others_boiled_egg).build());
        otherItems.add(MenuItem.builder().id("others_fried_egg").name("Fried Egg").variant("", 2000).imageResId(R.drawable.others_fried_egg).build());
        otherItems.add(MenuItem.builder().id("others_custom").name("Custom Item")
                .variant("", 0).imageResId(R.drawable.ic_food_placeholder).build());
        CATEGORIES.add(MenuCategory.builder().id("others").name("Others").subtitle("Rice & extras")
                .items(otherItems).iconResId(R.drawable.ic_food_placeholder).build());
    }

    public static List<MenuCategory> getCategories() {
        return new ArrayList<>(CATEGORIES);
    }

    @androidx.annotation.Nullable
    public static MenuCategory getCategoryById(String categoryId) {
        for (MenuCategory c : CATEGORIES) {
            if (c.getId().equals(categoryId)) return c;
        }
        return null;
    }

    /** Returns category name for a menu item id, or "Other" if not found. */
    public static String getCategoryNameForItemId(String itemId) {
        for (MenuCategory c : CATEGORIES) {
            for (MenuItem item : c.getItems()) {
                if (item.getId().equals(itemId)) return c.getName();
            }
        }
        return "Other";
    }
}
