package me.addon.modules;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import me.addon.ShopAutomation;

public class ShopAutoModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> itemName = sgGeneral.add(new Setting.Builder<String>()
        .name("item-name")
        .description("Tên vật phẩm cần tìm trong /order")
        .defaultValue("Que quỷ lửa")
        .build()
    );

    private final Setting<Integer> buyTimes = sgGeneral.add(new Setting.Builder<Integer>()
        .name("buy-times")
        .description("Số lần ấn nút mua")
        .defaultValue(1)
        .min(1)
        .max(64)
        .build()
    );

    private final Setting<Integer> delayMs = sgGeneral.add(new Setting.Builder<Integer>()
        .name("delay")
        .description("Độ trễ giữa các hành động (ms)")
        .defaultValue(500)
        .min(100)
        .max(5000)
        .build()
    );

    private enum State {
        IDLE, OPENING_SHOP, SELECTING_HELL, SELECTING_ITEM, BUYING,
        CLOSING_SHOP, ORDERING, SELECTING_ORDER_ITEM, PUTTING_ITEMS, DONE
    }

    private State currentState = State.IDLE;
    private long lastActionTime = 0;
    private int buyCounter = 0;
    private int itemSlotIndex = -1;

    public ShopAutoModule() {
        super(ShopAutomation.CATEGORY, "shop-auto", "Tự động hóa quy trình mua hàng từ shop");
    }

    @Override
    public void onActivate() {
        currentState = State.OPENING_SHOP;
        lastActionTime = System.currentTimeMillis();
        buyCounter = 0;
        itemSlotIndex = -1;
        ChatUtils.info("Shop Automation bắt đầu...");
    }

    @Override
    public void onDeactivate() {
        currentState = State.IDLE;
        ChatUtils.info("Shop Automation dừng");
    }

    @Override
    public void onTick() {
        if (!canAction()) return;

        switch (currentState) {
            case OPENING_SHOP -> handleOpenShop();
            case SELECTING_HELL -> handleSelectHell();
            case SELECTING_ITEM -> handleSelectItem();
            case BUYING -> handleBuying();
            case CLOSING_SHOP -> handleCloseShop();
            case ORDERING -> handleOrder();
            case SELECTING_ORDER_ITEM -> handleSelectOrderItem();
            case PUTTING_ITEMS -> handlePuttingItems();
            case DONE -> currentState = State.OPENING_SHOP;
            default -> {}
        }
    }

    private boolean canAction() {
        return System.currentTimeMillis() - lastActionTime >= delayMs.get();
    }

    private void updateActionTime() {
        lastActionTime = System.currentTimeMillis();
    }

    private void handleOpenShop() {
        ChatUtils.sendPlayerMsg("/shop");
        currentState = State.SELECTING_HELL;
        updateActionTime();
    }

    private void handleSelectHell() {
        if (mc.currentScreen instanceof GenericContainerScreen container) {
            for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
                Slot slot = container.getScreenHandler().slots.get(i);
                ItemStack stack = slot.getStack();
                
                if (stack != null && stack.hasCustomName() && 
                    stack.getCustomName().getString().contains("địa ngục")) {
                    clickSlot(container, i);
                    currentState = State.SELECTING_ITEM;
                    updateActionTime();
                    return;
                }
            }
        }
    }

    private void handleSelectItem() {
        if (mc.currentScreen instanceof GenericContainerScreen container) {
            for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
                Slot slot = container.getScreenHandler().slots.get(i);
                ItemStack stack = slot.getStack();
                
                if (stack != null && stack.hasCustomName() && 
                    stack.getCustomName().getString().contains("Que quỷ lửa")) {
                    clickSlot(container, i);
                    itemSlotIndex = i;
                    currentState = State.BUYING;
                    updateActionTime();
                    return;
                }
            }
        }
    }

    private void handleBuying() {
        if (mc.currentScreen instanceof GenericContainerScreen container) {
            for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
                Slot slot = container.getScreenHandler().slots.get(i);
                ItemStack stack = slot.getStack();
                
                if (stack != null && stack.getCount() == 64) {
                    clickSlot(container, i);
                    break;
                }
            }
            
            if (buyCounter < buyTimes.get()) {
                for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
                    Slot slot = container.getScreenHandler().slots.get(i);
                    ItemStack stack = slot.getStack();
                    
                    if (stack != null && stack.hasCustomName() && 
                        stack.getCustomName().getString().contains("Mua")) {
                        clickSlot(container, i);
                        buyCounter++;
                        updateActionTime();
                        return;
                    }
                }
            } else {
                currentState = State.CLOSING_SHOP;
                updateActionTime();
            }
        }
    }

    private void handleCloseShop() {
        mc.player.closeHandledScreen();
        currentState = State.ORDERING;
        updateActionTime();
    }

    private void handleOrder() {
        ChatUtils.sendPlayerMsg("/order " + itemName.get());
        currentState = State.SELECTING_ORDER_ITEM;
        updateActionTime();
    }

    private void handleSelectOrderItem() {
        if (mc.currentScreen instanceof GenericContainerScreen container) {
            if (itemSlotIndex >= 0 && itemSlotIndex < container.getScreenHandler().slots.size()) {
                Slot slot = container.getScreenHandler().slots.get(itemSlotIndex);
                if (slot != null && !slot.getStack().isEmpty()) {
                    clickSlot(container, itemSlotIndex);
                    currentState = State.PUTTING_ITEMS;
                    updateActionTime();
                }
            }
        }
    }

    private void handlePuttingItems() {
        if (mc.currentScreen instanceof GenericContainerScreen container) {
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem().getName().getString().contains(itemName.get())) {
                    mc.interactionManager.clickSlot(container.getScreenHandler().syncId, i, 0, 
                        net.minecraft.screen.SlotActionType.THROW);
                }
            }
            
            mc.player.closeHandledScreen();
            currentState = State.DONE;
            updateActionTime();
        }
    }

    private void clickSlot(GenericContainerScreen container, int slot) {
        mc.interactionManager.clickSlot(container.getScreenHandler().syncId, slot, 0,
            net.minecraft.screen.SlotActionType.PICKUP);
    }
    }
