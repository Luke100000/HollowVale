package net.conczin.hollowvale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.conczin.hollowvale.block.WindChimeBlock;
import net.conczin.hollowvale.block.WindChimeBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import net.minecraft.util.Mth;


public class WindChimeRenderer implements BlockEntityRenderer<WindChimeBlockEntity, WindChimeRenderer.State> {
    private static final float CALM_SWING = 3.0f;
    private static final float RAIN_SWING = 6.0f;
    private static final float THUNDER_SWING = 8.0f;
    private static final int VIEW_DISTANCE = 32;
    private static final float MAX_ELAPSED = 40.0f;

    public WindChimeRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        float amplitude;
        float time;
        long seed;
        final MovingBlockRenderState[] rods = new MovingBlockRenderState[WindChimeBlock.RODS];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WindChimeBlockEntity chime, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chime, state, partialTick, camera, breakProgress);
        if (!(chime.getLevel() instanceof ClientLevel level)) {
            return;
        }
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        state.amplitude = CALM_SWING + RAIN_SWING * rain + THUNDER_SWING * thunder;
        float now = level.getGameTime() + partialTick;
        float elapsed = now - chime.lastRenderTime;
        if (chime.lastRenderTime < 0.0f || elapsed < 0.0f || elapsed > MAX_ELAPSED) {
            chime.swingTime = now;
        } else {
            chime.swingTime += elapsed * (1.0f + 0.5f * rain + 0.5f * thunder);
        }
        chime.lastRenderTime = now;
        state.time = chime.swingTime;
        state.seed = chime.getBlockPos().asLong();
        for (int rod = 1; rod <= WindChimeBlock.RODS; rod++) {
            MovingBlockRenderState moving = new MovingBlockRenderState();
            moving.randomSeedPos = chime.getBlockPos();
            moving.blockPos = chime.getBlockPos();
            moving.blockState = chime.getBlockState().setValue(WindChimeBlock.ROD, rod);
            moving.biome = level.getBiome(chime.getBlockPos());
            moving.cardinalLighting = level.cardinalLighting();
            moving.lightEngine = level.getLightEngine();
            state.rods[rod - 1] = moving;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int rod = 1; rod <= WindChimeBlock.RODS; rod++) {
            MovingBlockRenderState moving = state.rods[rod - 1];
            if (moving == null) {
                continue;
            }
            long hash = HashCommon.mix(state.seed + rod * 0x9E3779B97F4A7C15L);

            // A shared gust plus each rod's own sway, with its own speed and phase per axis.
            float gustX = Mth.sin(state.time * 0.05f + state.seed % 100);
            float gustZ = Mth.sin(state.time * 0.037f + state.seed % 77);
            float ownX = Mth.sin(state.time * (0.07f + 0.05f * unit(hash, 0)) + Mth.TWO_PI * unit(hash, 1));
            float ownZ = Mth.sin(state.time * (0.06f + 0.05f * unit(hash, 2)) + Mth.TWO_PI * unit(hash, 3));

            poseStack.pushPose();
            poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
            poseStack.rotate(Axis.XP.rotationDegrees(state.amplitude * (0.4f * gustX + 0.6f * ownX)));
            poseStack.rotate(Axis.ZP.rotationDegrees(state.amplitude * (0.4f * gustZ + 0.6f * ownZ)));
            poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
            collector.submitMovingBlock(poseStack, moving, 0);
            poseStack.popPose();
        }
    }

    // Rods swing around the middle of their top face.
    private static final float PIVOT_X = 0.5f;
    private static final float PIVOT_Y = 1.0f;
    private static final float PIVOT_Z = 0.5f;

    // One of four independent values in [0, 1) from the hash.
    private static float unit(long hash, int index) {
        return ((hash >>> (index * 16)) & 0xFFFF) / 65536.0f;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
