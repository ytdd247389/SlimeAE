package me.ddggdd135.slimeae.core.slimefun;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import javax.annotation.Nonnull;
import me.ddggdd135.slimeae.SlimeAEPlugin;
import me.ddggdd135.slimeae.api.ItemRequest;
import me.ddggdd135.slimeae.api.interfaces.IMEObject;
import me.ddggdd135.slimeae.api.interfaces.IStorage;
import me.ddggdd135.slimeae.core.NetworkInfo;
import me.ddggdd135.slimeae.core.items.MenuItems;
import me.ddggdd135.slimeae.utils.ItemUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class MEInterface extends SlimefunItem implements IMEObject, InventoryBlock {
    public static final int[] Boarder_Slots = new int[] {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53
    };
    public static final int[] Setting_Slots = new int[] {9, 10, 11, 12, 13, 14, 15, 16, 17};
    public static final int[] Item_Slots = new int[] {18, 19, 20, 21, 22, 23, 24, 25, 26};

    public MEInterface(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        createPreset(this, this.getItem().getItemMeta().getDisplayName(), this::constructMenu);
        addItemHandler(onBlockBreak());
        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {
                BlockMenu inv = StorageCacheUtils.getMenu(block.getLocation());
                for (int slot : Setting_Slots) {
                    ItemStack setting = inv.getItemInSlot(slot);
                    if (setting == null || setting.getType().isAir()) {
                        ItemUtils.setSettingItem(inv.getInventory(), slot, MenuItems.Setting);
                    }
                }
                NetworkInfo info = SlimeAEPlugin.getNetworkData().getNetworkInfo(block.getLocation());
                if (info == null) return;
                IStorage networkStorage = info.getStorage();
                for (int slot : Item_Slots) {
                    int settingSlot = slot - 9;
                    ItemStack setting = ItemUtils.getSettingItem(inv.getInventory(), settingSlot);
                    ItemStack itemStack = inv.getItemInSlot(slot);
                    if (SlimefunUtils.isItemSimilar(setting, MenuItems.Setting, false, false)
                            || (itemStack != null
                                    && !itemStack.getType().isAir()
                                    && !SlimefunUtils.isItemSimilar(setting, itemStack, false, false))) {
                        if (itemStack != null && !itemStack.getType().isAir()) networkStorage.pushItem(itemStack);
                        continue;
                    }
                    if (itemStack == null || itemStack.getType().isAir()) continue;

                    int amount = itemStack.getAmount();
                    if (amount > setting.getAmount()) {
                        ItemStack toPush = itemStack.clone();
                        toPush.setAmount(amount - setting.getAmount());
                        networkStorage.pushItem(toPush);
                        itemStack.setAmount(setting.getAmount() + toPush.getAmount());
                        continue;
                    }

                    ItemStack[] received =
                            networkStorage.tryTakeItem(new ItemRequest(setting, setting.getAmount() - amount));
                    if (received.length != 0) {
                        if (!itemStack.getType().isAir()) itemStack.setAmount(amount + received[0].getAmount());
                        else inv.replaceExistingItem(slot, received[0]);
                    }
                }
            }
        });
    }

    @Override
    public void onNetworkUpdate(Block block, NetworkInfo networkInfo) {}

    @Nonnull
    private BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), Item_Slots);
                }
            }
        };
    }

    protected void constructMenu(BlockMenuPreset preset) {
        preset.drawBackground(Boarder_Slots);
        for (int slot : Setting_Slots) {
            preset.addMenuClickHandler(slot, ItemUtils.getSettingSlotClickHandler());
        }
    }

    @Override
    public int[] getInputSlots() {
        return Item_Slots;
    }

    @Override
    public int[] getOutputSlots() {
        return Item_Slots;
    }
}
