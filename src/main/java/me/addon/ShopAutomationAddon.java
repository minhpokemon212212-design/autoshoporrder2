package me.addon;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import me.addon.modules.ShopAutoModule;

public class ShopAutomationAddon extends MeteorAddon {

    @Override
    public void onInitialize() {
        addModule(new ShopAutoModule());
    }

    @Override
    public String getWebsite() {
        return "https://github.com/yourname/shop-automation-addon";
    }

    @Override
    public String getCommit() {
        return "";
    }
}
