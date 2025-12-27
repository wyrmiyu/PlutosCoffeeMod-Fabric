package ml.pluto7073.plutoscoffee;

import com.mojang.datafixers.util.Pair;
import ml.pluto7073.pdapi.component.DrinkAdditions;
import ml.pluto7073.pdapi.component.PDComponents;
import ml.pluto7073.pdapi.util.DrinkUtil;
import ml.pluto7073.pdapi.addition.DrinkAddition;
import ml.pluto7073.pdapi.addition.DrinkAdditionManager;
import ml.pluto7073.plutoscoffee.coffee.CoffeeType;
import ml.pluto7073.plutoscoffee.coffee.CoffeeTypes;
import ml.pluto7073.plutoscoffee.items.BrewedCoffee;
import ml.pluto7073.plutoscoffee.mixins.StructurePoolAccessor;
import ml.pluto7073.plutoscoffee.registry.ModComponents;
import ml.pluto7073.plutoscoffee.registry.ModItems;
import ml.pluto7073.plutoscoffee.tags.ModItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class CoffeeUtil {

    private CoffeeUtil(){}

    public static <T> boolean collectionContainsOnlyAll(Collection<T> c1, Collection<T> c2) {
        List<T> l1 = new ArrayList<>(c1);
        for (T t : c2) {
            l1.remove(t);
        }
        List<T> l2 = new ArrayList<>(c2);
        for (T t : c1) {
            l2.remove(t);
        }
        return (l1.isEmpty()) && (l2.isEmpty());
    }

    public static CoffeeType getCoffeeType(ItemStack stack) {
        return stack.getOrDefault(ModComponents.COFFEE_TYPE, CoffeeTypes.EMPTY);
    }

    public static ItemStack setCoffeeType(ItemStack stack, CoffeeType type) {
        stack.set(ModComponents.COFFEE_TYPE, type);
        return stack;
    }

    public static ItemStack getBaseCoffee(CoffeeType type) {
        return setCoffeeType(new ItemStack(ModItems.BREWED_COFFEE), type);
    }

    public static CoffeeType getCoffeeType(@Nullable CompoundTag nbt) {
        return nbt == null ? CoffeeTypes.EMPTY : CoffeeType.byId(nbt.getString("CoffeeType"));
    }

    public static boolean isItemACoffeeGround(Item item) {
        return TagUtil.isIn(ModItemTags.COFFEE_GROUNDS, item);
    }

    public static boolean isItemACoffeeBean(Item item) {
        return TagUtil.isIn(ModItemTags.COFFEE_BEANS, item);
    }

    public static int getCoffeeColour(ItemStack stack) {
        DrinkAddition[] addIns = DrinkUtil.getAdditionsFromStack(stack);
        if (addIns == null) {
            return BrewedCoffee.DEFAULT_COLOUR;
        }
        return getCoffeeColour(addIns);
    }

    public static int getCoffeeColour(DrinkAddition[] addIns) {
        int colour = BrewedCoffee.DEFAULT_COLOUR;
        if (Arrays.stream(addIns).map(DrinkAdditionManager::getId).anyMatch(identifier -> identifier.toString().equals("pdapi:milk"))) {
            colour = BrewedCoffee.COLOUR_WITH_MILK;
        }
        float r = (colour >> 16 & 255) / 255.0F;
        float g = (colour >> 8 & 255) / 255.0F;
        float b = (colour & 255) / 255.0F;
        int colourCount = 1;
        int allowedMilk = 3;
        for (DrinkAddition addition : addIns) {
            if (!addition.changesColor()) continue;
            if (DrinkAdditionManager.getId(addition).toString().equals("pdapi:milk") && allowedMilk > 0) {
                allowedMilk--;
                continue;
            }
            int additionColour = addition.color();
            r += (additionColour >> 16 & 255) / 255.0F;
            g += (additionColour >> 8 & 255) / 255.0F;
            b += (additionColour & 255) / 255.0F;
            colourCount += 1;
        }
        r = r / (float) colourCount * 255.0F;
        g = g / (float) colourCount * 255.0F;
        b = b / (float) colourCount * 255.0F;
        return 255 << 24 | (int) r << 16 | (int) g << 8 | (int) b;
    }



    public static int getLatteColour(ItemStack stack) {
        DrinkAddition[] addIns = DrinkUtil.getAdditionsFromStack(stack);
        if (addIns == null) {
            return 0xFFFFFF;
        }
        return getLatteColour(addIns);
    }

    public static int getLatteColour(DrinkAddition[] addIns) {
        int color = 0xFFFFFF;
        if (Arrays.stream(addIns).map(DrinkAdditionManager::getId).anyMatch(id -> id.toString().equals("plutoscoffee:espresso_shot")
                || id.toString().equals("plutoscoffee:blonde_espresso_shot")
                || id.toString().equals("plutoscoffee:decaf_espresso_shot"))) {
            color = BrewedCoffee.COLOUR_WITH_MILK;
        }
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        int colourCount = 1;
        int allowedShots = 2;
        for (DrinkAddition addition : addIns) {
            if (!addition.changesColor()) continue;
            if ((DrinkAdditionManager.getId(addition).toString().equals("plutoscoffee:espresso_shot") ||
                    DrinkAdditionManager.getId(addition).toString().equals("plutoscoffee:blonde_espresso_shot") ||
                    DrinkAdditionManager.getId(addition).toString().equals("plutoscoffee:decaf_espresso_shot")) && allowedShots > 0) {
                allowedShots--;
                continue;
            }
            int additionColour = addition.color();
            r += (additionColour >> 16 & 255) / 255.0F;
            g += (additionColour >> 8 & 255) / 255.0F;
            b += (additionColour & 255) / 255.0F;
            colourCount += 1;
        }
        r = r / (float) colourCount * 255.0F;
        g = g / (float) colourCount * 255.0F;
        b = b / (float) colourCount * 255.0F;
        return 255 << 24 | (int) r << 16 | (int) g << 8 | (int) b;
    }

    public static int calculateHealthBarHeightPixels(int health, int maxHeartsPerRow, int rowHeight) {
        double hearts = health / 2.0;
        int rows = (int) Math.floor(hearts / maxHeartsPerRow);
        return rows * rowHeight;
    }

    /**
     * Borrowed from the FriendsAndFoes mod by Faboslav
     */
    public static void addElementToStructurePool(Registry<StructureTemplatePool> templateRegistry, ResourceLocation poolLocation, String name, int weight) {
        StructureTemplatePool pool = templateRegistry.get(poolLocation);
        if (pool == null) return;

        SinglePoolElement piece = SinglePoolElement.single(PlutosCoffee.asId(name).toString()).apply(StructureTemplatePool.Projection.RIGID);

        for (int i = 0; i < weight; i++) {
            ((StructurePoolAccessor) pool).getTemplates().add(piece);
        }

        List<Pair<StructurePoolElement, Integer>> pieceCounts = new ArrayList<>(((StructurePoolAccessor) pool).getRawTemplates());
        pieceCounts.add(new Pair<>(piece, weight));
        ((StructurePoolAccessor) pool).setRawTemplates(pieceCounts);
    }    /**
     * Creates a copy of the given ItemStack with additional DrinkAdditions.
     * This method is used by villager trades to create specialty drinks.
     *
     * @param stack The base ItemStack to add additions to
     * @param additions String IDs of the additions to add (e.g., "pdapi:honey", "plutoscoffee:mocha_syrup")
     * @return A new ItemStack with the specified additions
     */
    public static ItemStack getWithAdditions(ItemStack stack, String... additions) {
        if (PlutosCoffee.LOGGER.isDebugEnabled()) {
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] getWithAdditions called with {} and additions: {}", 
                stack.getItem(), java.util.Arrays.toString(additions));
        }
        
        ItemStack result = stack.copy();

        // Get existing additions or create empty if none
        DrinkAdditions existingAdditions = result.getOrDefault(PDComponents.ADDITIONS, DrinkAdditions.EMPTY);
        if (PlutosCoffee.LOGGER.isDebugEnabled()) {
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] Existing additions: {}", existingAdditions);
        }
        
        // Log existing addition IDs for debugging
        if (!existingAdditions.additions().isEmpty() && PlutosCoffee.LOGGER.isDebugEnabled()) {
            for (DrinkAddition existing : existingAdditions.additions()) {
                PlutosCoffee.LOGGER.debug("[PlutosCoffee] Existing addition ID: {}", DrinkAdditionManager.getId(existing));
            }
        }

        // Add each new addition
        for (String additionId : additions) {
            try {
                ResourceLocation id = new ResourceLocation(additionId);
                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Looking for DrinkAddition with ID: {}", id);
                }
                DrinkAddition addition = DrinkAdditionManager.get(id);
                if (addition != null) {
                    if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                        PlutosCoffee.LOGGER.debug("[PlutosCoffee] Found addition: {}, adding to existing", addition);
                    }
                    existingAdditions = existingAdditions.withAddition(addition);
                } else {
                    PlutosCoffee.LOGGER.warn("[PlutosCoffee] Failed to find DrinkAddition with ID: {}", additionId);
                }
            } catch (Exception e) {
                PlutosCoffee.LOGGER.error("[PlutosCoffee] Failed to parse addition ID: {}", additionId, e);
            }
        }

        if (PlutosCoffee.LOGGER.isDebugEnabled()) {
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] Final additions to set: {}", existingAdditions);
        }
        // Set the updated additions on the result stack
        result.set(PDComponents.ADDITIONS, existingAdditions);
        
        // Note: Display name is handled by AbstractCustomizableDrinkItem.getName() override
        // No need to set custom name component here
        
        if (PlutosCoffee.LOGGER.isDebugEnabled()) {
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] Returning result with additions: {}", result.getOrDefault(PDComponents.ADDITIONS, DrinkAdditions.EMPTY));
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] Result item display name: {}", result.getDisplayName().getString());
        }
        return result;
    }

    /**
     * Creates a drink with specialty additions where only the specialty additions are shown in the display name
     * (like a real cafe menu), while all additions are preserved for functionality and tooltips.
     * 
     * @param stack The base ItemStack to add additions to
     * @param specialtyAdditions The specialty additions that should appear in the display name
     * @return A new ItemStack with all additions but display name showing only specialty ones
     */
    public static ItemStack getWithSpecialtyAdditions(ItemStack stack, String... specialtyAdditions) {
        if (PlutosCoffee.LOGGER.isDebugEnabled()) {
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] getWithSpecialtyAdditions called with {} and specialty additions: {}", 
                stack.getItem(), java.util.Arrays.toString(specialtyAdditions));
        }
        
        ItemStack result = stack.copy();

        // Get existing additions or create empty if none
        DrinkAdditions existingAdditions = result.getOrDefault(PDComponents.ADDITIONS, DrinkAdditions.EMPTY);
        if (PlutosCoffee.LOGGER.isDebugEnabled()) {
            PlutosCoffee.LOGGER.debug("[PlutosCoffee] Existing additions: {}", existingAdditions);
        }

        // Add each specialty addition to the full additions (for functionality)
        for (String additionId : specialtyAdditions) {
            try {
                ResourceLocation id = new ResourceLocation(additionId);
                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Looking for DrinkAddition with ID: {}", id);
                }
                DrinkAddition addition = DrinkAdditionManager.get(id);
                if (addition != null) {
                    if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                        PlutosCoffee.LOGGER.debug("[PlutosCoffee] Found addition: {}, adding to existing", addition);
                    }
                    existingAdditions = existingAdditions.withAddition(addition);
                } else {
                    PlutosCoffee.LOGGER.warn("[PlutosCoffee] Failed to find DrinkAddition with ID: {}", additionId);
                }
            } catch (Exception e) {
                PlutosCoffee.LOGGER.error("[PlutosCoffee] Failed to parse addition ID: {}", additionId, e);
            }
        }

        // Set the updated additions on the result stack (all additions for functionality)
        result.set(PDComponents.ADDITIONS, existingAdditions);
        
        // Create display name with only specialty additions (cafe menu style)
        if (specialtyAdditions.length > 0) {
            // Get the base name from the original stack (without additions) to avoid recursion
            // This gives us just "Latte" instead of "Espresso Shot Espresso Shot Latte"
            Component baseName = stack.getItem().getDescription();
            StringBuilder nameBuilder = new StringBuilder();
            
            // Build name like "Glow Berries Latte" (only specialty additions)
            for (String additionId : specialtyAdditions) {
                try {
                    ResourceLocation id = new ResourceLocation(additionId);
                    // Convert "pdapi:glow_berries" to "Glow Berries"
                    String additionName = id.getPath().replace("_", " ");
                    String[] words = additionName.split("\\s+");
                    for (int i = 0; i < words.length; i++) {
                        if (!words[i].isEmpty()) {
                            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
                        }
                    }
                    if (nameBuilder.length() > 0) {
                        nameBuilder.append(" ");
                    }
                    nameBuilder.append(String.join(" ", words));
                } catch (Exception e) {
                    PlutosCoffee.LOGGER.error("[PlutosCoffee] Failed to parse specialty addition name: {}", additionId, e);
                }
            }
            
            if (nameBuilder.length() > 0) {
                // Real cafe style: just "Specialty Latte", not "Specialty Espresso Shot Espresso Shot Latte"
                Component customName = Component.literal(nameBuilder.toString() + " " + baseName.getString());
                result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);
                if (PlutosCoffee.LOGGER.isDebugEnabled()) {
                    PlutosCoffee.LOGGER.debug("[PlutosCoffee] Set specialty display name: {}", customName.getString());
                }
            }
        }
        
        PlutosCoffee.LOGGER.info("[PlutosCoffee] Created specialty drink: {} with additions: {}", 
            result.getDisplayName().getString(), existingAdditions);
        return result;
    }

}
