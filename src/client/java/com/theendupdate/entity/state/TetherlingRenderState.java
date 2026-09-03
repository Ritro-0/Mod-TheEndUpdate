package com.theendupdate.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class TetherlingRenderState extends LivingEntityRenderState {
    /** 0 = relaxed, 1+ = stretched toward player, can exceed 1.0 for long reaches */
    public float tentacleExtend;
    /** Extra 0-1 snap on top of extend (forward "yeet") */
    public float tentacleYeet;
    /** distance to player, used for reach calc */
    public float distanceToPlayer;
    /** pitch toward player */
    public float pitchToPlayer;
    /** yaw toward player */
    public float yawToPlayer;
    public float hoverBob;
}