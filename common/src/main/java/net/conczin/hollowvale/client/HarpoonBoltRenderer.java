package net.conczin.hollowvale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.HarpoonBolt;
import net.conczin.hollowvale.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Draws the harpoon tip and the line from the wielder's hand to it.
 */
public class HarpoonBoltRenderer extends EntityRenderer<HarpoonBolt, HarpoonBoltRenderer.State> {
    private static final Identifier LINE_TEXTURE = Common.locate("textures/entity/harpoon_line.png");
    private static final int SEGMENTS = 16;
    private static final float LINE_WIDTH = 0.03f;
    // How many blocks of line one repeat of the texture covers.
    private static final float TEXTURE_LENGTH = 0.5f;
    private static final float TIP_SCALE = 0.5f;
    // The model is 2.6 blocks long around its center, this puts the tip at the hook point.
    private static final float TIP_OFFSET = 1.3f;
    // Light is sampled this far back from the hook point, which is inside the block it hit.
    private static final double LIGHT_SAMPLE_OFFSET = 0.4;
    private static final float SLACK_SAG = 0.3f;
    private static final float TAUT_SAG = 0.05f;

    private final ItemModelResolver itemModelResolver;
    // Created on first use, item components aren't bound yet when renderers are built.
    private ItemStack tip;

    public HarpoonBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    public static class State extends EntityRenderState {
        final ItemStackRenderState tip = new ItemStackRenderState();
        boolean visible;
        boolean showTip;
        Quaternionf tipRotation = new Quaternionf();
        int tipLight;
        int lineLight;
        Vec3 lineStart = Vec3.ZERO;
        Vec3 lineEnd = Vec3.ZERO;
        float sag;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(HarpoonBolt bolt, Frustum frustum, double x, double y, double z) {
        return bolt.getOwner() != null;
    }

    @Override
    public void extractRenderState(HarpoonBolt bolt, State state, float partialTick) {
        super.extractRenderState(bolt, state, partialTick);
        Entity owner = bolt.getOwner();
        state.visible = owner != null;
        if (owner == null) {
            return;
        }
        state.showTip = bolt.getHookedEntity() == null;
        if (state.showTip) {
            // Projectile rotation, unlike an entity's view direction, is yaw = atan2(x, z) and pitch up.
            float yRot = Mth.lerp(partialTick, bolt.yRotO, bolt.getYRot()) * Mth.DEG_TO_RAD;
            float xRot = Mth.lerp(partialTick, bolt.xRotO, bolt.getXRot()) * Mth.DEG_TO_RAD;
            Vec3 direction = new Vec3(Mth.sin(yRot) * Mth.cos(xRot), Mth.sin(xRot), Mth.cos(yRot) * Mth.cos(xRot));
            BlockPos lightPos = BlockPos.containing(bolt.getPosition(partialTick).subtract(direction.scale(LIGHT_SAMPLE_OFFSET)));
            state.tipRotation.rotationTo(0.0f, 1.0f, 0.0f, (float) direction.x, (float) direction.y, (float) direction.z);
            state.tipLight = LevelRenderer.getLightCoords(bolt.level(), lightPos);
            if (tip == null) {
                tip = new ItemStack(Items.BONE_HARPOON);
            }
            itemModelResolver.updateForNonLiving(state.tip, tip, ItemDisplayContext.NONE, bolt);
        }
        Vec3 origin = bolt.getPosition(partialTick);
        state.lineStart = handPosition(owner, bolt.isOffHand(), partialTick).subtract(origin);
        state.lineEnd = bolt.getHookPoint(partialTick).subtract(origin);
        state.sag = bolt.isHooked() ? TAUT_SAG : SLACK_SAG;
        // The line is lit like its wielder, a hooked block would otherwise make it dark.
        state.lineLight = entityRenderDispatcher.getRenderer(owner).getPackedLightCoords(owner, partialTick);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) {
            return;
        }
        if (state.showTip) {
            pose.pushPose();
            pose.mulPose(state.tipRotation);
            pose.scale(TIP_SCALE, TIP_SCALE, TIP_SCALE);
            pose.translate(0.0f, -TIP_OFFSET, 0.0f);
            state.tip.submit(pose, collector, state.tipLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
        }
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        Vec3 cameraPos = camera.pos;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(LINE_TEXTURE),
                (last, ribbon) -> drawLine(last, ribbon, origin, cameraPos, state.lineStart, state.lineEnd, state.sag, state.lineLight));
        super.submit(state, pose, collector, camera);
    }

    private static void drawLine(PoseStack.Pose last, VertexConsumer ribbon, Vec3 origin, Vec3 camera, Vec3 start, Vec3 end, float sag, int light) {
        Vec3 direction = end.subtract(start).normalize();
        float repeat = (float) (end.subtract(start).length() / TEXTURE_LENGTH);

        Vec3 previous = start;
        Vec3 previousSide = Vec3.ZERO;
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            Vec3 point = start.lerp(end, t).add(0.0, -Mth.sin(t * Mth.PI) * sag, 0.0);
            // A flat ribbon turned toward the camera, so the line reads as a rope from any angle.
            Vec3 side = point.add(origin).subtract(camera).cross(direction).normalize().scale(LINE_WIDTH);
            if (i > 0) {
                float v0 = (i - 1) / (float) SEGMENTS * repeat;
                float v1 = t * repeat;
                vertex(ribbon, last, previous.subtract(previousSide), 0.0f, v0, light);
                vertex(ribbon, last, previous.add(previousSide), 1.0f, v0, light);
                vertex(ribbon, last, point.add(side), 1.0f, v1, light);
                vertex(ribbon, last, point.subtract(side), 0.0f, v1, light);
            }
            previous = point;
            previousSide = side;
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, float u, float v, int light) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    private static Vec3 handPosition(Entity owner, boolean offHand, float partialTick) {
        if (!(owner instanceof Player player)) {
            return owner.getEyePosition(partialTick);
        }
        boolean rightArm = (player.getMainArm() == HumanoidArm.RIGHT) != offHand;
        // The local player is seen from behind the camera, so the line follows where it looks instead of its body.
        boolean firstPerson = player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        float yaw = (firstPerson ? Mth.lerp(partialTick, player.yRotO, player.getYRot()) : Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot)) * Mth.DEG_TO_RAD;
        double side = rightArm ? 0.35 : -0.35;
        double forward = 0.4;
        return player.getPosition(partialTick).add(
                -Mth.cos(yaw) * side - Mth.sin(yaw) * forward,
                player.getBbHeight() * (firstPerson ? 0.75 : 0.62),
                -Mth.sin(yaw) * side + Mth.cos(yaw) * forward);
    }

}
