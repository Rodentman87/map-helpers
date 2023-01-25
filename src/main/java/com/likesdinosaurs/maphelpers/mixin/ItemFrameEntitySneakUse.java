package com.likesdinosaurs.maphelpers.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntitySneakUse {
	private static final float ninetyDeg = (float) Math.PI / 2;

	@Inject(at = @At("HEAD"), method = "interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", cancellable = true)
	public void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> info) {
		if (player.world.isClient) {
			return;
		}
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isOf(Items.FILLED_MAP) && player.isSneaking()) {
			if (((ItemFrameEntity) (Object) this).getHeldItemStack().isOf(Items.FILLED_MAP)) {
				// Get the map ids
				int handMapId = FilledMapItem.getMapId(itemStack);
				int frameMapId = FilledMapItem.getMapId(((ItemFrameEntity) (Object) this).getHeldItemStack());

				// If they're the same, yell at the player
				if (handMapId == frameMapId) {
					player.sendMessage(Text.literal("These maps are the same"), true);
					info.setReturnValue(ActionResult.SUCCESS);
					return;
				}

				// If they're different, make sure they're the same scale, and same dimensions
				MapState handMapState = FilledMapItem.getMapState(handMapId, player.world);
				MapState frameMapState = FilledMapItem.getMapState(frameMapId, player.world);

				if (handMapState.scale != frameMapState.scale) {
					player.sendMessage(Text.literal("These maps are different scales"), true);
					info.setReturnValue(ActionResult.SUCCESS);
					return;
				}
				if (handMapState.dimension != frameMapState.dimension) {
					player.sendMessage(Text.literal("These maps are different dimensions"), true);
					info.setReturnValue(ActionResult.SUCCESS);
					return;
				}

				int xDiff = handMapState.centerX - frameMapState.centerX;
				int zDiff = handMapState.centerZ - frameMapState.centerZ;

				int xMapDiff = xDiff / getSizeFromScale(handMapState.scale);
				int zMapDiff = zDiff / getSizeFromScale(handMapState.scale);

				if (Math.abs(xMapDiff) > 5 || Math.abs(zMapDiff) > 5) {
					player.sendMessage(Text.literal("These maps are too far apart"), true);
					info.setReturnValue(ActionResult.FAIL);
					return;
				}

				Vec3d offset = new Vec3d(xMapDiff, 0, zMapDiff);

				Vec3d pos = ((ItemFrameEntity) (Object) this).getPos();

				Direction facing = ((ItemFrameEntity) (Object) this).getHorizontalFacing();
				int rotation = ((ItemFrameEntity) (Object) this).getRotation();

				Vec3d rotatedOffset = roundVector(
						rotateVector(offset, facing, rotation));

				Vec3d finalCoord = pos.add(rotatedOffset);

				// See if there's an item frame there
				List<ItemFrameEntity> frames = player.world.getEntitiesByClass(ItemFrameEntity.class,
						new Box(finalCoord, finalCoord), frame -> frame.getHorizontalFacing().equals(facing));

				if (frames.size() > 0) {
					// There's a frame here
					ItemFrameEntity frame = frames.get(0);
					ItemStack frameStack = frame.getHeldItemStack();
					if (frameStack.isEmpty()) {
						// The frame is empty, so put the map in it
						frame.setHeldItemStack(itemStack.copy());
						if (!player.getAbilities().creativeMode) {
							itemStack.decrement(1);
						}
						frame.setRotation(rotation);
						info.setReturnValue(ActionResult.SUCCESS);
						return;
					}
				} else {
					// Check if the player has item frames in their inventory
					boolean hasItemFrames = player.getInventory().containsAny(stack -> stack.isOf(Items.ITEM_FRAME));
					if (hasItemFrames || player.getAbilities().creativeMode) {
						// We can place an item frame for them
						// But first, let's check that the block is empty
						BlockState state = player.world.getBlockState(new BlockPos(finalCoord));
						if (!state.isAir()) {
							player.sendMessage(Text.literal("There's a block in the way"), true);
							info.setReturnValue(ActionResult.FAIL);
							return;
						}
						if (!player.canPlaceOn(new BlockPos(finalCoord), facing, itemStack)) {
							player.sendMessage(Text.literal("Can't place item frame on block"), true);
							info.setReturnValue(ActionResult.FAIL);
							return;
						}
						int slot = player.getInventory().getSlotWithStack(new ItemStack(Items.ITEM_FRAME));
						if (slot != -1 || player.getAbilities().creativeMode) {
							ItemStack frameStack = player.getInventory().getStack(slot);
							ItemFrameEntity frame = new ItemFrameEntity(player.world, new BlockPos(finalCoord), facing);
							if (frame.canStayAttached()) {
								frame.onPlace();
								player.world.emitGameEvent((Entity) player, GameEvent.ENTITY_PLACE, frame.getPos());
								player.world.spawnEntity(frame);
								if (!player.getAbilities().creativeMode) {
									frameStack.decrement(1);
									itemStack.decrement(1);
								}
								frame.setHeldItemStack(itemStack.copy());
								frame.setRotation(rotation);
								info.setReturnValue(ActionResult.SUCCESS);
								return;
							} else {
								player.sendMessage(Text.literal("Can't place item frame on block"), true);
								info.setReturnValue(ActionResult.FAIL);
								return;
							}
						}
					}
				}
			}
		}
	}

	private int getSizeFromScale(int scale) {
		switch (scale) {
			case 0:
				return 128;
			case 1:
				return 256;
			case 2:
				return 512;
			case 3:
				return 1024;
			case 4:
				return 2048;
			default:
				return 128;
		}
	}

	private Vec3d rotateVector(Vec3d vector, Direction dir, int rotation) {
		switch (dir) {
			case UP:
				vector = vector.rotateY(-ninetyDeg * rotation);
				return vector;
			case DOWN:
				vector = vector.rotateZ(ninetyDeg * 2);
				vector = vector.rotateY(ninetyDeg * 2);
				vector = vector.rotateY(ninetyDeg * rotation);
				return vector;
			case NORTH:
				vector = vector.rotateX(ninetyDeg);
				vector = vector.rotateZ(ninetyDeg * rotation);
				return vector;
			case SOUTH:
				vector = vector.rotateX(-ninetyDeg);
				vector = vector.rotateZ(ninetyDeg * rotation);
				return vector;
			case WEST:
				vector = vector.rotateX(-ninetyDeg);
				vector = vector.rotateY(-ninetyDeg);
				vector = vector.rotateX(ninetyDeg * rotation);
				return vector;
			case EAST:
				vector = vector.rotateX(-ninetyDeg);
				vector = vector.rotateY(ninetyDeg);
				vector = vector.rotateX(ninetyDeg * rotation);
				return vector;
			default:
				return vector;
		}
	}

	private Vec3d roundVector(Vec3d vector) {
		return new Vec3d(Math.round(vector.x), Math.round(vector.y), Math.round(vector.z));
	}
}
