package com.example.addon;

import com.example.addon.modules.KrispyStorageEsp;
import com.example.addon.modules.PistonEsp;
import com.example.addon.modules.StorageEsp;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Storage ESP");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        Modules.get().add(new StorageEsp());
        Modules.get().add(new PistonEsp());
        Modules.get().add(new KrispyStorageEsp());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
