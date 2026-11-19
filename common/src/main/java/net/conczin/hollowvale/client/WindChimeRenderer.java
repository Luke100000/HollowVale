package net.conczin.hollowvale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.conczin.hollowvale.block.WindChimeBlock;
import net.conczin.hollowvale.block.WindChimeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.List;

public class WindChimeRenderer implements BlockEntityRenderer<WindChimeBlockEntity> {
    private static final float CALM_SWING = 3.0f;
    private static final float RAIN_SWING = 6.0f;
    private static final float THUNDER_SWING = 8.0f;
    private static final int VIEW_DISTANCE = 32;

    private final BlockRenderDispatcher dispatcher;
    private final RandomSource random = RandomSource.create();

    public WindChimeRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(WindChimeBlockEntity chime, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Level level = chime.getLevel();
        if (level == null) {
            return;
        }
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        float amplitude = CALM_SWING + RAIN_SWING * rain + THUNDER_SWING * thunder;
        float time = (level.getGameTime() + partialTick) * (1.0f + 0.5f * rain + 0.5f * thunder);
        long seed = chime.getBlockPos().asLong();

        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        for (int rod = 1; rod <= WindChimeBlock.RODS; rod++) {
            BlockState state = chime.getBlockState().setValue(WindChimeBlock.ROD, rod);
            BakedModel model = dispatcher.getBlockModel(state);
            Vector3f pivot = topCenter(model, state);
            long hash = HashCommon.mix(seed + rod * 0x9E3779B97F4A7C15L);

            // A shared gust plus each rod's own sway, with its own speed and phase per axis.
            float gustX = Mth.sin(time * 0.05f + seed % 100);
            float gustZ = Mth.sin(time * 0.037f + seed % 77);
            float ownX = Mth.sin(time * (0.07f + 0.05f * unit(hash, 0)) + Mth.TWO_PI * unit(hash, 1));
            float ownZ = Mth.sin(time * (0.06f + 0.05f * unit(hash, 2)) + Mth.TWO_PI * unit(hash, 3));

            poseStack.pushPose();
            poseStack.translate(pivot.x, pivot.y, pivot.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(amplitude * (0.4f * gustX + 0.6f * ownX)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(amplitude * (0.4f * gustZ + 0.6f * ownZ)));
            poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
            dispatcher.getModelRenderer().renderModel(poseStack.last(), consumer, state, model, 1.0f, 1.0f, 1.0f, light, overlay);
            poseStack.popPose();
        }
    }

    // One of four independent values in [0, 1) from the hash.
    private static float unit(long hash, int index) {
        return ((hash >>> (index * 16)) & 0xFFFF) / 65536.0f;
    }

    // Rods swing around the middle of their top face, read from the model so it follows Blockbench edits.
    private Vector3f topCenter(BakedModel model, BlockState state) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Direction direction : DIRECTIONS) {
            List<BakedQuad> quads = model.getQuads(state, direction, random);
            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int i = 0; i < 4; i++) {
                    float x = Float.intBitsToFloat(vertices[i * stride]);
                    float y = Float.intBitsToFloat(vertices[i * stride + 1]);
                    float z = Float.intBitsToFloat(vertices[i * stride + 2]);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxY == -Float.MAX_VALUE ? new Vector3f(0.5f, 1.0f, 0.5f) : new Vector3f((minX + maxX) / 2, maxY, (minZ + maxZ) / 2);
    }

    private static final Direction[] DIRECTIONS = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
