package ml.pluto7073.plutoscoffee.items;

import ml.pluto7073.pdapi.addition.chemicals.ConsumableChemicalRegistry;
import ml.pluto7073.pdapi.item.AbstractCustomizableDrinkItem;
import ml.pluto7073.plutoscoffee.CoffeeUtil;
import ml.pluto7073.plutoscoffee.coffee.CoffeeType;
import ml.pluto7073.plutoscoffee.coffee.CoffeeTypes;
import ml.pluto7073.plutoscoffee.registry.ModStats;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

@MethodsReturnNonnullByDefault
public class BrewedCoffee extends AbstractCustomizableDrinkItem {

    public static final int DEFAULT_COLOUR = 0x160A02;
    public static final int COLOUR_WITH_MILK = 0x885737;

    public BrewedCoffee(Properties properties) {
        super(Items.GLASS_BOTTLE, Temperature.HOT, properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return CoffeeUtil.setCoffeeType(super.getDefaultInstance(), CoffeeTypes.MEDIUM_ROAST);
    }

    @Override
    public int getChemicalContent(String name, ItemStack stack) {
        int total = super.getChemicalContent(name, stack);
        if (!"caffeine".equals(name)) return total;
        CoffeeType type = CoffeeUtil.getCoffeeType(stack);
        return type.getCaffeineContent() + total;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        Player player = user instanceof Player ? (Player) user : null;

        if (player != null) {
            player.awardStat(ModStats.DRINK_COFFEE);
        }

        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag config) {
        if (CoffeeUtil.getCoffeeType(stack) != CoffeeTypes.EMPTY) {
            tooltip.add(Component.translatable(CoffeeUtil.getCoffeeType(stack).getTranslationKey()).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, config);
    }

}
