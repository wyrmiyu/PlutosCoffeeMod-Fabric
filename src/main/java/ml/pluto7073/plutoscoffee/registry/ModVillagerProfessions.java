package ml.pluto7073.plutoscoffee.registry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import ml.pluto7073.pdapi.item.PDItems;
import ml.pluto7073.pdapi.component.PDComponents;
import ml.pluto7073.pdapi.component.DrinkAdditions;
import ml.pluto7073.plutoscoffee.CoffeeUtil;
import ml.pluto7073.plutoscoffee.PlutosCoffee;
import ml.pluto7073.plutoscoffee.coffee.CoffeeTypes;
import ml.pluto7073.plutoscoffee.items.LatteItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class ModVillagerProfessions {

    public static final VillagerProfession BARISTA;

    private static final Predicate<Holder<PoiType>> WORKSTATION_PREDICATE = (entry) -> entry.is(ModPointOfInterests.DRINK_WORKSTATION);

    /**
     * Custom trade listing that lazily creates items with additions when the trade is generated
     */
    private static class LazyDrinkTrade implements VillagerTrades.ItemListing {
        private final Supplier<ItemStack> baseStackSupplier;
        private final String[] additions;
        private final int emeraldCost;
        private final int maxUses;
        private final int villagerXp;
        private final float priceMultiplier;

        public LazyDrinkTrade(Supplier<ItemStack> baseStackSupplier, String[] additions, int emeraldCost, int maxUses, int villagerXp, float priceMultiplier) {
            this.baseStackSupplier = baseStackSupplier;
            this.additions = additions;
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.villagerXp = villagerXp;
            this.priceMultiplier = priceMultiplier;
        }        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Creating lazy drink trade with specialty additions: {}", java.util.Arrays.toString(additions));
            }
            ItemStack baseStack = baseStackSupplier.get();
            // Use the new specialty additions method for proper cafe-style naming
            ItemStack resultStack = CoffeeUtil.getWithSpecialtyAdditions(baseStack, additions);
            
            PlutosCoffee.LOGGER.info("[PlutosCoffee] Created specialty drink trade: {}", resultStack.getDisplayName().getString());
            
            if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Trade item has custom name: {}", resultStack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Trade item additions component: {}", resultStack.getOrDefault(PDComponents.ADDITIONS, DrinkAdditions.EMPTY));
            }

            // Try to use our custom implementation that preserves ItemStack components
            try {
                return new CustomItemsForEmeralds(resultStack, emeraldCost, maxUses, villagerXp, priceMultiplier).getOffer(trader, random);
            } catch (Exception e) {
                PlutosCoffee.LOGGER.error("[PlutosCoffee] Failed to create custom merchant offer: {}", e.getMessage());

                // Fallback to the standard approach for now
                VillagerTrades.ItemsForEmeralds baseTrade = new VillagerTrades.ItemsForEmeralds(resultStack.getItem(), emeraldCost, resultStack.getCount(), maxUses, villagerXp, priceMultiplier);
                MerchantOffer offer = baseTrade.getOffer(trader, random);

                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Base offer created, original result: {}", offer.getResult());
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Base offer result display name: {}", offer.getResult().getDisplayName().getString());
                }

                return offer;
            }
        }
    }

    /**
     * Custom implementation that uses reflection to create a proper MerchantOffer
     * preserving the full ItemStack with components
     */
    private static class CustomItemsForEmeralds implements VillagerTrades.ItemListing {
        private final ItemStack resultStack;
        private final int emeraldCost;
        private final int maxUses;
        private final int villagerXp;
        private final float priceMultiplier;

        public CustomItemsForEmeralds(ItemStack resultStack, int emeraldCost, int maxUses, int villagerXp, float priceMultiplier) {
            this.resultStack = resultStack;
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.villagerXp = villagerXp;
            this.priceMultiplier = priceMultiplier;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] CustomItemsForEmeralds creating offer with custom result: {}", resultStack);
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Custom result display name: {}", resultStack.getDisplayName().getString());
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Custom result additions: {}", resultStack.getOrDefault(PDComponents.ADDITIONS, DrinkAdditions.EMPTY));
            }

            // First create a base offer, then use reflection to modify it
            VillagerTrades.ItemsForEmeralds baseTrade = new VillagerTrades.ItemsForEmeralds(resultStack.getItem(), emeraldCost, resultStack.getCount(), maxUses, villagerXp, priceMultiplier);
            MerchantOffer baseOffer = baseTrade.getOffer(trader, random);

            // Use reflection to set the result field with our custom ItemStack
            try {
                java.lang.reflect.Field resultField = MerchantOffer.class.getDeclaredField("result");
                resultField.setAccessible(true);
                resultField.set(baseOffer, resultStack);

                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Successfully set custom result via reflection: {}", baseOffer.getResult());
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Reflected offer result display name: {}", baseOffer.getResult().getDisplayName().getString());
                }

                return baseOffer;
            } catch (Exception e) {
                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Standard field name not found, trying obfuscated field names: {}", e.getMessage());
                }

                // Try alternative field names that might exist
                try {
                    java.lang.reflect.Field[] fields = MerchantOffer.class.getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (field.getType() == ItemStack.class) {
                            if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Found ItemStack field: {}", field.getName());
                            }
                            field.setAccessible(true);
                            ItemStack fieldValue = (ItemStack) field.get(baseOffer);
                            if (fieldValue != null && fieldValue.getItem() == resultStack.getItem()) {
                                field.set(baseOffer, resultStack);
                                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Successfully set field {} with custom result", field.getName());
                                }
                                return baseOffer;
                            }
                        }
                    }
                } catch (Exception e2) {
                    PlutosCoffee.LOGGER.error("[PlutosCoffee] All reflection attempts failed: {}", e2.getMessage());
                }

                // If reflection fails, return the base offer
                return baseOffer;
            }
        }
    }

    static {
        BARISTA = register("barista",
                new VillagerProfession(
                        PlutosCoffee.asId("barista").toString(),
                        WORKSTATION_PREDICATE,
                        WORKSTATION_PREDICATE,
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        SoundEvents.VILLAGER_WORK_CLERIC
                )
        );
    }

    public static void init() {}

    public static void postInit() {
        PlutosCoffee.LOGGER.info("Setting up barista trades...");
        VillagerTrades.TRADES.put(BARISTA, new Int2ObjectOpenHashMap<>(ImmutableMap.of(
                1, new VillagerTrades.ItemListing[]{
                        new VillagerTrades.EmeraldForItems(ModItems.COFFEE_BERRY, 24, 16, 2),
                        new VillagerTrades.ItemsForEmeralds(ModItems.MOCHA_SAUCE, 1, 3, 16, 2),
                        new VillagerTrades.EmeraldForItems(ModItems.COFFEE_BEAN, 22, 16, 2),
                        new VillagerTrades.EmeraldForItems(ModItems.USED_COFFEE_GROUNDS, 26, 16, 2)
                },
                2, new VillagerTrades.ItemListing[]{
                        new VillagerTrades.ItemsAndEmeraldsToItems(ModItems.LIGHT_ROAST_BEAN, 3, 2, ModItems.GROUND_LIGHT_ROAST, 12, 12, 5, 0.05F),
                        new VillagerTrades.ItemsAndEmeraldsToItems(ModItems.MEDIUM_ROAST_BEAN, 3, 2, ModItems.GROUND_MEDIUM_ROAST, 12, 12, 5, 0.05F),
                        new VillagerTrades.ItemsAndEmeraldsToItems(ModItems.DARK_ROAST_BEAN, 3, 2, ModItems.GROUND_DARK_ROAST, 12, 12, 5, 0.05F),
                        new VillagerTrades.ItemsAndEmeraldsToItems(ModItems.ESPRESSO_ROAST_BEAN, 3, 2, ModItems.GROUND_ESPRESSO_ROAST, 12, 12, 5, 0.05F)
                },
                3, new VillagerTrades.ItemListing[]{
                        new VillagerTrades.ItemsForEmeralds(CoffeeUtil.getBaseCoffee(CoffeeTypes.LIGHT_ROAST), 3, 1, 12, 10),
                        new VillagerTrades.ItemsForEmeralds(CoffeeUtil.getBaseCoffee(CoffeeTypes.MEDIUM_ROAST), 3, 1, 12, 10),
                        new VillagerTrades.ItemsForEmeralds(CoffeeUtil.getBaseCoffee(CoffeeTypes.DARK_ROAST), 3, 1, 12, 10),
                        new VillagerTrades.ItemsForEmeralds(LatteItem.getStandardLatte(), 5, 1, 12, 15),
                        new VillagerTrades.EmeraldForItems(PDItems.MILK_BOTTLE, 16, 12, 5)
                },
                4, new VillagerTrades.ItemListing[] {
                        new LazyDrinkTrade(LatteItem::getStandardLatte, new String[]{"pdapi:chorus_fruit"}, 10, 12, 20, 0.05F),
                        new LazyDrinkTrade(LatteItem::getStandardLatte, new String[]{"pdapi:glow_berries"}, 7, 12, 20, 0.05F),
                        new LazyDrinkTrade(LatteItem::getStandardLatte, new String[]{"pdapi:honey", "pdapi:sugar"}, 5, 12, 20, 0.05F),
                        new LazyDrinkTrade(LatteItem::getStandardLatte, new String[]{"plutoscoffee:mocha_syrup", "plutoscoffee:caramel"}, 8, 12, 20, 0.05F),
                        new VillagerTrades.ItemsAndEmeraldsToItems(ModItems.DARK_ROAST_BEAN, 16, 2, ModItems.DECAF_ROAST_BEAN, 16, 12, 20, 0.05F)
                },
                5, new VillagerTrades.ItemListing[]{
                        new VillagerTrades.ItemsForEmeralds(ModItems.COFFEE_BREWER, 24, 1, 12, 25),
                        new VillagerTrades.ItemsForEmeralds(ModItems.COFFEE_GRINDR, 20, 1, 12, 25),
                        new VillagerTrades.ItemsForEmeralds(ModItems.ESPRESSO_MACHINE, 30, 1, 12, 25)
                }
        )));
    }

    private static VillagerProfession register(String id, VillagerProfession profession ) {
        return Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, PlutosCoffee.asId(id), profession);
    }

}
