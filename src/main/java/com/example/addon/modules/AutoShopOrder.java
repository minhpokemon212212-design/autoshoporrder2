package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.item.ItemStack;
import java.util.Objects;

public class AutoShopOrder extends Module {
    public static final String NAME = "auto-shop-order";
    public static final String DESC = "Tự mua Que Quỷ Lửa, tạo đơn, tìm đơn và nạp vật phẩm lặp lại";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Cấu hình chính
    private final Setting<Integer> slotDiaNguc = sgGeneral.add(new IntSetting.Builder()
        .name("vị-trí-mục-địa-ngục")
        .description("Khe mở mục Địa Ngục (đếm từ 0)")
        .defaultValue(10)
        .min(0).max(53)
        .build()
    );

    private final Setting<Integer> slotQueQuyLua = sgGeneral.add(new IntSetting.Builder()
        .name("vị-trí-que-quỷ-lửa")
        .description("Khe chọn Que Quỷ Lửa")
        .defaultValue(14)
        .min(0).max(53)
        .build()
    );

    private final Setting<Integer> soLanAnMua = sgGeneral.add(new IntSetting.Builder()
        .name("số-lần-ấn-mua")
        .description("Số lần nhấn nút mua mỗi vòng")
        .defaultValue(1)
        .min(1).max(10)
        .build()
    );

    private final Setting<Integer> slotNutMua = sgGeneral.add(new IntSetting.Builder()
        .name("vị-trí-nút-mua")
        .description("Khe nút xác nhận mua")
        .defaultValue(22)
        .min(0).max(53)
        .build()
    );

    private final Setting<String> tenTimKiem = sgGeneral.add(new StringSetting.Builder()
        .name("tên-tìm-đơn")
        .description("Tên người/vật phẩm để tìm đơn hàng")
        .defaultValue("")
        .build()
    );

    private final Setting<Integer> doTre = sgGeneral.add(new IntSetting.Builder()
        .name("độ-trễ-mỗi-bước")
        .description("Thời gian chờ (tick, 20 tick = 1 giây)")
        .defaultValue(18)
        .min(5).max(40)
        .build()
    );

    // Trạng thái chạy
    private int buoc = 0;
    private int demLay = 0;
    private int demAnMua = 0;

    public AutoShopOrder() {
        super(Categories.Player, NAME, DESC);
    }

    @Override
    public void onActivate() {
        buoc = 1;
        demLay = 0;
        demAnMua = 0;
        info("Đã bắt đầu vòng lặp tự động!");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (demLay > 0) { demLay--; return; }

        switch (buoc) {
            // B1: Gửi lệnh /shop
            case 1 -> {
                ChatUtils.sendCommand("shop");
                demLay = doTre.get();
                buoc = 2;
            }

            // B2: Chọn mục Địa Ngục
            case 2 -> {
                if (mc.currentScreen instanceof GenericContainerScreen s) {
                    clickKhe(s, slotDiaNguc.get());
                    demLay = doTre.get();
                    buoc = 3;
                }
            }

            // B3: Chọn Que Quỷ Lửa
            case 3 -> {
                if (mc.currentScreen instanceof GenericContainerScreen s) {
                    clickKhe(s, slotQueQuyLua.get());
                    demLay = doTre.get();
                    buoc = 4;
                }
            }

            // B4+B5: Ấn mua đủ số lần
            case 4 -> {
                if (mc.currentScreen instanceof GenericContainerScreen s) {
                    if (demAnMua < soLanAnMua.get()) {
                        clickKhe(s, slotNutMua.get());
                        demAnMua++;
                        demLay = doTre.get() / 2;
                    } else {
                        demAnMua = 0;
                        demLay = doTre.get();
                        buoc = 5;
                    }
                }
            }

            // B6: Thoát menu
            case 5 -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                demLay = doTre.get();
                buoc = 6;
            }

            // B7: Gửi lệnh /order tìm đơn
            case 6 -> {
                String ten = tenTimKiem.get().trim();
                if (!ten.isEmpty()) ChatUtils.sendCommand("order " + ten);
                demLay = doTre.get();
                buoc = 7;
            }

            // B8: Chọn đơn hàng gần nhất (trái sang phải, trên xuống dưới)
            case 7 -> {
                if (mc.currentScreen instanceof GenericContainerScreen s) {
                    boolean daChon = false;
                    // Quét từ khe 0 đến hết, chọn cái đầu tiên khớp
                    for (int i = 0; i < s.getScreenHandler().slots.size(); i++) {
                        ItemStack vatPham = s.getScreenHandler().getSlot(i).getStack();
                        if (!vatPham.isEmpty()) {
                            clickKhe(s, i);
                            daChon = true;
                            break;
                        }
                    }
                    demLay = doTre.get();
                    buoc = 8;
                }
            }

            // B9: Nạp toàn bộ vật phẩm từ túi vào đơn
            case 8 -> {
                if (mc.currentScreen instanceof GenericContainerScreen s) {
                    // Đưa tất cả vật phẩm từ túi vào ô nhận đơn
                    for (int i = 0; i < 36; i++) {
                        InvUtils.quickMove(s.getScreenHandler().syncId, i + s.getScreenHandler().slots.size() - 36);
                    }
                    info("Đã nạp toàn bộ vật phẩm vào đơn!");
                    demLay = doTre.get();
                    buoc = 9;
                }
            }

            // B10: Thoát và lặp lại từ đầu
            case 9 -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                demLay = doTre.get();
                buoc = 1; // Quay lại B1 lặp vô hạn
                info("Bắt đầu vòng lặp mới...");
            }
        }
    }

    // Hàm nhấn khe giao diện
    private void clickKhe(GenericContainerScreen s, int maKhe) {
        Objects.requireNonNull(mc.interactionManager).clickSlot(
            s.getScreenHandler().syncId, maKhe, 0,
            ScreenHandlerType.MOUSE_LEFT, mc.player
        );
    }
}
