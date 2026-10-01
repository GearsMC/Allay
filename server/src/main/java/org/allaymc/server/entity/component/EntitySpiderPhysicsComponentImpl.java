package org.allaymc.server.entity.component;

import org.allaymc.server.entity.impl.EntityImpl;

/**
 * Orumcek fizigi: yurume fizigine duvar tirmanma eklenir — HeartCore {@code Spider::updateClimbingState}.
 *
 * <p>Motor yatay carpisma bayragi tutmuyor; bunun yerine hareketin uygulanmadan onceki ve
 * sonraki yatay hizi karsilastirilir: yurume denetleyicisi ileri itiyor ama blok engelliyorsa
 * hiz sifirlanmistir. O durumda orumcek yukari tirmanir (yerdeyken en az {@code 0.32}, havadayken
 * en az {@code 0.15} dikey hiz) ve istemciye duvar tirmanma pozu gonderilir.</p>
 */
public class EntitySpiderPhysicsComponentImpl extends EntityMobPhysicsComponentImpl {

    /** Bu yatay hizin (blok/tick) uzerindeki itme "ilerlemeye calisiyor" sayilir. */
    protected static final double PUSH_THRESHOLD = 0.02;
    /** Hareketten sonra bu hizin altinda kalan yatay hiz "engellendi" sayilir. */
    protected static final double BLOCKED_THRESHOLD = 0.005;
    protected static final double CLIMB_SPEED_GROUND = 0.32;
    protected static final double CLIMB_SPEED_AIR = 0.15;

    @Override
    public boolean applyMotion() {
        var before = getMotion();
        double pushSquared = before.x() * before.x() + before.z() * before.z();
        boolean grounded = isOnGround();
        boolean moved = super.applyMotion();

        var after = getMotion();
        double afterSquared = after.x() * after.x() + after.z() * after.z();
        boolean blocked = shouldClimb(pushSquared, afterSquared);
        setClimbing(blocked);
        if (blocked) {
            setMotion(after.x(), climbMotionY(grounded, after.y()), after.z());
        }
        return moved;
    }

    /** @return itme vardi ama hareket engellendi mi */
    static boolean shouldClimb(double pushSquared, double afterSquared) {
        return pushSquared > PUSH_THRESHOLD * PUSH_THRESHOLD && afterSquared < BLOCKED_THRESHOLD * BLOCKED_THRESHOLD;
    }

    /** @return tirmanirken uygulanacak dikey hiz */
    static double climbMotionY(boolean onGround, double currentY) {
        return onGround ? Math.max(currentY, CLIMB_SPEED_GROUND) : Math.max(currentY, CLIMB_SPEED_AIR);
    }

    private void setClimbing(boolean climbing) {
        if (thisEntity instanceof EntityImpl impl && impl.getBaseComponent() instanceof EntitySpiderBaseComponentImpl spider) {
            spider.setClimbing(climbing);
        }
    }
}
