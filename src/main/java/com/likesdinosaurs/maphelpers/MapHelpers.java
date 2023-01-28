package com.likesdinosaurs.maphelpers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.likesdinosaurs.maphelpers.item.MapBundle;

public class MapHelpers implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("map-helpers");

	public static final MapBundle MAP_BUNDLE = new MapBundle(new Item.Settings().maxCount(1));

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, new Identifier("map-helpers", "map_bundle"), MAP_BUNDLE);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
			content.add(MAP_BUNDLE);
		});
		ModelPredicateProviderRegistry.register(MAP_BUNDLE, new Identifier("bundled"),
				(itemStack, clientWorld, livingEntity, i) -> {
					return itemStack.getOrCreateNbt().getBoolean("Bundled") ? 1 : 0;
				});
	}
}
