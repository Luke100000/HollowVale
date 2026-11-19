package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.BoneWhale;
import net.conczin.hollowvale.entity.Fry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CircleHostGoal extends Goal {
    private static final double DISTANCE = 4.0;
    private static final int REPATH_INTERVAL = 10;
    private static final float STEP = 0.6f;

    private final Fry fry;
    private final double speed;
    private float angle;

    public CircleHostGoal(Fry fry, double speed) {
        this.fry = fry;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return fry.getTarget() == null && fry.getHost() != null;
    }

    @Override
    public void start() {
        BoneWhale host = fry.getHost();
        angle = host == null ? 0.0f : (float) Mth.atan2(fry.getZ() - host.getZ(), fry.getX() - host.getX());
    }

    @Override
    public void stop() {
        fry.getNavigation().stop();
    }

    @Override
    public void tick() {
        BoneWhale host = fry.getHost();
        if (host == null || fry.tickCount % REPATH_INTERVAL != 0) {
            return;
        }
        angle += STEP;
        double radius = host.getBbWidth() * 0.5 + DISTANCE;
        double x = host.getX() + Mth.cos(angle) * radius;
        double y = host.getY(0.5) + Mth.sin(angle * 1.7f);
        double z = host.getZ() + Mth.sin(angle) * radius;
        fry.getNavigation().moveTo(x, y, z, speed);
    }
}
