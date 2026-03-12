package com.example.menu_pos.data;

import com.example.menu_pos.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Static menu data from Bilao Specials, Chill Zone, Silog, Sizzlers, Naomie's, Chicken Wings, Ulam.
 */
public final class MenuData {

    private static final List<MenuCategory> CATEGORIES = new ArrayList<>();

    static {
        // --- BILAO SPECIALS ---
        List<MenuItem> bilaoItems = new ArrayList<>();
        bilaoItems.add(MenuItem.builder().id("bilao_miki").name("Miki Bihon")
                .imageResId(R.drawable.bilao_miki)
                .variant("3-5 PAX", 60000).variant("10 PAX", 90000).variant("15 PAX", 120000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_pancit").name("Pancit Bihon")
                .imageResId(R.drawable.bilao_pancit)
                .variant("3-5 PAX", 60000).variant("10 PAX", 90000).variant("15 PAX", 120000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_lumpia").name("Lumpiang Shanghai")
                .imageResId(R.drawable.bilao_shanghai)
                .variant("45 PCS", 50000).variant("90 PCS", 100000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_canton").name("Canton Guisado")
                .imageResId(R.drawable.bilao_canton)
                .variant("3-5 PAX", 60000).variant("10 PAX", 90000).variant("15 PAX", 120000).build());
        bilaoItems.add(MenuItem.builder().id("bilao_chami").name("Chami")
                .imageResId(R.drawable.bilao_chami)
                .variant("3-5 PAX", 60000).variant("10 PAX", 90000).variant("15 PAX", 120000).build());
        CATEGORIES.add(MenuCategory.builder().id("bilao").name("Bilao Specials").subtitle("Noodles & lumpia")
                .items(bilaoItems).iconResId(R.drawable.ic_food_placeholder).build());

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

        // --- SILOG ---
        List<MenuItem> silogItems = new ArrayList<>();
        int[] silogPrices = {9500, 9500, 9500, 9500, 10500, 9500, 10500, 10500, 9500, 9000, 8000, 9000, 9500, 9500, 9500, 9500};
        String[] silogNames = {"Nuggets Silog", "Ham Silog", "Pork Sisig Silog", "Bagnet Silog", "Liempo Silog", "Calumpit Silog", "Porkchop Silog", "Bangus Silog", "Cebu Silog", "Tocino Silog", "TJ Hotdog Silog", "Shanghai Silog", "Vigan Silog", "Chicken Silog", "Hungarian Silog", "Beef Tapa Silog"};
        for (int i = 0; i < silogNames.length; i++) {
            silogItems.add(MenuItem.builder().id("silog_" + i).name(silogNames[i]).variant("", silogPrices[i]).build());
        }
        CATEGORIES.add(MenuCategory.builder().id("silog").name("Silog").subtitle("Sinangag + itlog")
                .items(silogItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- SIZZLERS (109 each) ---
        String[] sizzlerNames = {"Sizzling Backribs", "Sizzling Porkchop", "Sizzling Hungarian", "Sizzling Bangus", "Sizzling Shrimp", "Sizzling Tocino", "Sizzling Hotdog", "Sizzling Chicken", "Sizzling Bagnet", "Sizzling Tapa", "Sizzling Sisig", "Sizzling Liempo", "Sizzling Calamare"};
        List<MenuItem> sizzlerItems = new ArrayList<>();
        for (int i = 0; i < sizzlerNames.length; i++) {
            sizzlerItems.add(MenuItem.builder().id("sizzler_" + i).name(sizzlerNames[i]).variant("W/ rice + veggies", 10900).build());
        }
        CATEGORIES.add(MenuCategory.builder().id("sizzlers").name("Sizzlers").subtitle("109 Pesos only · Unli gravy")
                .items(sizzlerItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- NAOMIE'S (Lomi, Bulalo, Gotong, etc.) ---
        List<MenuItem> naomieItems = new ArrayList<>();
        naomieItems.add(MenuItem.builder().id("naomie_lomi").name("Lomi")
                .variant("Regular", 8000).variant("Special", 10000).variant("Super Special", 12000).build());
        naomieItems.add(MenuItem.builder().id("naomie_bulalo").name("Bulalo")
                .variant("Solo", 25000).variant("Good for Sharing", 45000).build());
        naomieItems.add(MenuItem.builder().id("naomie_gotong").name("Gotong Batangas")
                .variant("Regular", 10000).variant("Special", 12000).build());
        naomieItems.add(MenuItem.builder().id("naomie_chami").name("Chami Sweet & Spicy").variant("", 10000).build());
        naomieItems.add(MenuItem.builder().id("naomie_miki").name("Miki Bihon").variant("", 12000).build());
        naomieItems.add(MenuItem.builder().id("naomie_pancit").name("Pancit Bihon").variant("", 11000).build());
        naomieItems.add(MenuItem.builder().id("naomie_canton").name("Canton Guisado").variant("", 12000).build());
        CATEGORIES.add(MenuCategory.builder().id("naomie").name("Naomie's").subtitle("Lomi, Bulalo & Gotong Batangas")
                .items(naomieItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- FLAVORED CHICKEN WINGS (149, 6 pcs w/ 2 rice) ---
        String[] wingNames = {"Lemon Glazed", "Teriyaki", "Garlic Parmesan", "Hickory Barbeque", "Buttered Garlic", "Salted Egg", "Sriracha", "Sweet Chili", "Buffalo", "Korean Spicy Barbeque"};
        boolean[] wingBestSeller = {false, false, true, false, false, false, false, true, false, true};
        boolean[] wingSpicy = {false, false, false, false, false, false, true, true, false, true};
        List<MenuItem> wingItems = new ArrayList<>();
        for (int i = 0; i < wingNames.length; i++) {
            wingItems.add(MenuItem.builder().id("wings_" + i).name(wingNames[i])
                    .variant("6 PCS W/ 2 RICE", 14900).bestSeller(wingBestSeller[i]).spicy(wingSpicy[i]).build());
        }
        CATEGORIES.add(MenuCategory.builder().id("wings").name("Flavored Chicken Wings").subtitle("6 pcs with 2 rice · ₱149")
                .items(wingItems).iconResId(R.drawable.ic_food_placeholder).build());

        // --- ULAM / MAIN DISHES (W/RICE) ---
        List<MenuItem> ulamItems = new ArrayList<>();
        ulamItems.add(MenuItem.builder().id("ulam_sinigang_salmon").name("Sinigang na Salmon").variant("W/RICE", 15000).build());
        ulamItems.add(MenuItem.builder().id("ulam_sisig").name("Sizzling Sisig").variant("W/RICE", 15000).build());
        ulamItems.add(MenuItem.builder().id("ulam_bicol").name("Bicol's Express").variant("W/RICE", 15000).build());
        ulamItems.add(MenuItem.builder().id("ulam_dinuguan").name("Dinuguan").variant("W/RICE", 10000).build());
        ulamItems.add(MenuItem.builder().id("ulam_dinakdakan").name("Dinakdakan").variant("W/RICE", 15000).build());
        ulamItems.add(MenuItem.builder().id("ulam_bagnet_kare").name("Bagnet Kare-Kare").variant("W/RICE", 15000).build());
        ulamItems.add(MenuItem.builder().id("ulam_sinigang_hipon").name("Sinigang na Hipon").variant("W/RICE", 15000).build());
        ulamItems.add(MenuItem.builder().id("ulam_pusit").name("Sizzling Pusit").variant("W/RICE", 20000).build());
        ulamItems.add(MenuItem.builder().id("ulam_bulalo").name("Bulalo").variant("W/RICE", 25000).build());
        CATEGORIES.add(MenuCategory.builder().id("ulam").name("Ulam").subtitle("Main dishes with rice")
                .items(ulamItems).iconResId(R.drawable.ic_food_placeholder).build());
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
