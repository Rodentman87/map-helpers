package com.likesdinosaurs.maphelpers.tooltip;

import com.likesdinosaurs.maphelpers.item.MapBundle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;

import java.util.Map;
import java.util.Optional;

public class MapBundleTooltipComponent implements TooltipData, TooltipComponent {
	private final MinecraftClient client = MinecraftClient.getInstance();
	private int[] size;
	private Map<Integer, int[]> offsets;

	public MapBundleTooltipComponent(int[] size, Map<Integer, int[]> offsets) {
		this.size = size;
		this.offsets = offsets;
	}

	public TooltipComponent toComponent() {
		return this;
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (MapBundle.getMapCount(stack) == 0)
			return Optional.empty();
		return Optional.of(new MapBundleTooltipComponent(MapBundle.getSize(stack), MapBundle.getOffsets(stack)));
	}

	@Override
	public int getHeight() {
		int height = this.size[3] - this.size[2] + 1;
		return 32 * height + 4;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		int width = this.size[1] - this.size[0] + 1;
		return 32 * width;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer,
			int z) {
		VertexConsumerProvider.Immediate vertices = this.client.getBufferBuilders().getEntityVertexConsumers();
		MapRenderer mapRenderer = this.client.gameRenderer.getMapRenderer();
		int maxX = this.size[1];
		int maxZ = this.size[3];
		for (Map.Entry<Integer, int[]> entry : this.offsets.entrySet()) {
			int mapId = entry.getKey();
			int offsetX = entry.getValue()[0];
			int offsetZ = entry.getValue()[1];
			MapState state = FilledMapItem.getMapState(mapId, this.client.world);
			matrices.push();
			matrices.translate(x + (32 * (maxX - offsetX)), y + (32 * (maxZ - offsetZ)), z);
			matrices.scale(0.25F, 0.25F, 0);
			mapRenderer.draw(matrices, vertices, mapId, state, true,
					LightmapTextureManager.MAX_LIGHT_COORDINATE);
			vertices.draw();
			matrices.pop();
		}
	}
}
