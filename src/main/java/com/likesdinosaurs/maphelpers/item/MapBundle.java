package com.likesdinosaurs.maphelpers.item;

import java.util.*;

import com.likesdinosaurs.maphelpers.MapHelpers;
import com.likesdinosaurs.maphelpers.MapHelpersMathHelper;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class MapBundle extends Item {
	public MapBundle(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
		int mapCount = 0;
		if (itemStack.hasNbt() && itemStack.getNbt().contains("MapCount")) {
			mapCount = itemStack.getNbt().getInt("MapCount");
		}
		if (mapCount > 0)
			tooltip.add(Text.translatable("item.map-helpers.map_bundle.tooltip", mapCount));
		else
			tooltip.add(Text.translatable("item.map-helpers.map_bundle.tooltip2"));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ItemStack stack = context.getStack();
		if (context.getWorld().isClient) {
			return ActionResult.PASS;
		}
		if (!stack.hasNbt()) {
			return ActionResult.FAIL;
		}
		// Figure out where we're placing this
		BlockPos blockPos = context.getBlockPos();
		Direction direction = context.getSide();
		BlockPos blockPos2 = blockPos.offset(direction);
		Vec3d blockVec = new Vec3d(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
		NbtCompound nbt = stack.getNbt();
		int rotation = nbt.getInt("Rotation");
		// Get all the map offsets
		Map<Integer, int[]> offsets = new HashMap<Integer, int[]>();
		nbt.getKeys().stream().filter(key -> key.startsWith("map-")).forEach(key -> {
			int mapId = Integer.parseInt(key.substring(4));
			int[] offset = nbt.getIntArray(key);
			offsets.put(mapId, offset);
		});
		// Try to place all the item frames
		boolean failed = false;
		Map<Integer, ItemFrameEntity> frames = new HashMap<Integer, ItemFrameEntity>();
		for (Map.Entry<Integer, int[]> entry : offsets.entrySet()) {
			int mapId = entry.getKey();
			int[] offset = entry.getValue();
			// Calculate the final coordinates
			Vec3d original = new Vec3d(offset[0], 0, offset[1]);
			Vec3d rotated = MapHelpersMathHelper.rotateVector(original, direction, rotation);
			BlockPos finalPos = new BlockPos(blockVec.add(rotated));
			ItemFrameEntity frame = new ItemFrameEntity(context.getWorld(), new BlockPos(finalPos), direction);
			if (frame.canStayAttached()) {
				frames.put(mapId, frame);
			} else {
				failed = true;
			}
		}
		if (failed) {
			// Let the player know
			context.getPlayer().sendMessage(Text.translatable("item.map-helpers.map_bundle.failed"), true);
			return ActionResult.FAIL;
		}
		// Place all the item frames and add their maps
		for (Map.Entry<Integer, ItemFrameEntity> entry : frames.entrySet()) {
			int mapId = entry.getKey();
			ItemFrameEntity frame = entry.getValue();
			frame.setRotation(rotation);
			frame.onPlace();
			context.getWorld().spawnEntity(frame);
			context.getWorld().emitGameEvent((Entity) context.getPlayer(), GameEvent.ENTITY_PLACE, frame.getPos());
			frame.setHeldItemStack(createMapFromId(mapId, context.getWorld()));
		}
		// Clear the NBT
		List<String> keys = new ArrayList<String>();
		nbt.getKeys().stream().filter(key -> key.startsWith("map-")).forEach(keys::add);
		keys.forEach(nbt::remove);
		nbt.putBoolean("Bundled", false);
		nbt.putInt("MapCount", 0);
		nbt.remove("Rotation");
		return ActionResult.PASS;
	}

	private static ItemStack createMapFromId(int mapId, World world) {
		ItemStack map = new ItemStack(Items.FILLED_MAP);
		String string = FilledMapItem.getMapName(mapId);
		MapState mapState = FilledMapItem.getMapState(mapId, world);
		world.putMapState(string, mapState);
		map.getOrCreateNbt().putInt("map", mapId);
		return map;
	}

	public static boolean useOnItemFrame(ItemFrameEntity itemFrame, ItemStack itemStack) {
		if (itemStack.hasNbt() && itemStack.getNbt().contains("Bundled") && itemStack.getNbt().getBoolean("Bundled")) {
			return false;
		}
		ItemStack heldStack = itemFrame.getHeldItemStack();
		if (heldStack.isEmpty()) {
			// Let the item frame handle it
			return false;
		}
		if (!heldStack.isOf(Items.FILLED_MAP)) {
			// Let the item frame handle it
			return false;
		}
		MapState heldMapState = FilledMapItem.getMapState(FilledMapItem.getMapId(heldStack), itemFrame.world);
		// Get all the item frames in a 5x5 radius around the item frame that have the
		// same rotation, and size of maps
		Vec3d middlePos = itemFrame.getPos();
		Vec3d offset1 = MapHelpersMathHelper.rotateVector(new Vec3d(-2, 0, -2), itemFrame.getHorizontalFacing(), 0);
		Vec3d offset2 = MapHelpersMathHelper.rotateVector(new Vec3d(2, 0, 2), itemFrame.getHorizontalFacing(), 0);
		Box box = new Box(MapHelpersMathHelper.floorVec3d(middlePos.add(offset1)),
				MapHelpersMathHelper.floorVec3d(middlePos.add(offset2)));
		List<ItemFrameEntity> itemFrames = itemFrame.world.getEntitiesByClass(ItemFrameEntity.class,
				MapHelpersMathHelper.makeBoxFull(box), frame -> {
					if (frame.getHorizontalFacing() != itemFrame.getHorizontalFacing()) {
						MapHelpers.LOGGER.info("Wrong facing");
						return false;
					}
					if (frame.getRotation() != itemFrame.getRotation()) {
						MapHelpers.LOGGER.info("Wrong rotation");
						return false;
					}
					if (frame.getHeldItemStack().isEmpty()) {
						MapHelpers.LOGGER.info("empty");
						return false;
					}
					if (!frame.getHeldItemStack().isOf(Items.FILLED_MAP)) {
						MapHelpers.LOGGER.info("Not maps");
						return false;
					}
					return FilledMapItem.getMapState(FilledMapItem.getMapId(frame.getHeldItemStack()),
							itemFrame.world).scale == heldMapState.scale;
				});
		// For each item frame, filter out all the ones that are not the right offset
		List<ItemFrameEntity> filteredItemFrames = itemFrames.stream().filter(frame -> {
			return mapsAreRightOffset(itemFrame, frame);
		}).toList();
		if (filteredItemFrames.size() > 0) {
			// Let's add these all to the bundle!
			MapBundle.addFramesToEmptyBundle(itemStack, filteredItemFrames, itemFrame);
			for (ItemFrameEntity frame : filteredItemFrames) {
				frame.kill();
				frame.playSound(frame.getBreakSound(), 1.0F, 1.0F);
			}
			return true;
		}
		return false;
	}

	public static void addFramesToEmptyBundle(ItemStack bundle, List<ItemFrameEntity> itemFrames,
			ItemFrameEntity middleFrame) {
		// Mark this bundle as bundled
		bundle.getOrCreateNbt().putBoolean("Bundled", true);
		bundle.getOrCreateNbt().putInt("MapCount", itemFrames.size());
		bundle.getOrCreateNbt().putInt("Rotation", middleFrame.getRotation());
		// Add the item frames to the bundle
		for (ItemFrameEntity frame : itemFrames) {
			int[] offsets = getMapOffset(middleFrame, frame);
			int mapId = FilledMapItem.getMapId(frame.getHeldItemStack());
			bundle.getOrCreateNbt().putIntArray("map-" + String.valueOf(mapId), offsets);
		}
	}

	public static int[] getMapOffset(ItemFrameEntity middleFrame, ItemFrameEntity testFrame) {
		MapState middleMapState = FilledMapItem.getMapState(FilledMapItem.getMapId(middleFrame.getHeldItemStack()),
				middleFrame.world);
		MapState testMapState = FilledMapItem.getMapState(FilledMapItem.getMapId(testFrame.getHeldItemStack()),
				testFrame.world);

		int xDiff = middleMapState.centerX - testMapState.centerX;
		int zDiff = middleMapState.centerZ - testMapState.centerZ;

		int xMapDiff = xDiff / MapHelpersMathHelper.getSizeFromScale(middleMapState.scale);
		int zMapDiff = zDiff / MapHelpersMathHelper.getSizeFromScale(middleMapState.scale);

		return new int[] { xMapDiff, zMapDiff };
	}

	public static boolean mapsAreRightOffset(ItemFrameEntity middleFrame, ItemFrameEntity testFrame) {
		Vec3d middlePos = middleFrame.getPos();
		Vec3d testPos = testFrame.getPos();
		Vec3d offset = middlePos.subtract(testPos);
		offset = MapHelpersMathHelper.roundVec3d(
				MapHelpersMathHelper.unrotateVector(offset, middleFrame.getHorizontalFacing(), middleFrame.getRotation()));

		MapState middleMapState = FilledMapItem.getMapState(FilledMapItem.getMapId(middleFrame.getHeldItemStack()),
				middleFrame.world);
		MapState testMapState = FilledMapItem.getMapState(FilledMapItem.getMapId(testFrame.getHeldItemStack()),
				testFrame.world);

		int xDiff = middleMapState.centerX - testMapState.centerX;
		int zDiff = middleMapState.centerZ - testMapState.centerZ;

		int xMapDiff = xDiff / MapHelpersMathHelper.getSizeFromScale(middleMapState.scale);
		int zMapDiff = zDiff / MapHelpersMathHelper.getSizeFromScale(middleMapState.scale);

		if (offset.x == xMapDiff && offset.z == zMapDiff) {
			return true;
		}

		return false;
	}
}
