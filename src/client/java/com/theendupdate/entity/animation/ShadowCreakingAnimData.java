package com.theendupdate.entity.animation;

/**
 * Blockbench roar/stumble rotation keyframes from shadow_creaking.bbmodel.
 * Each row is {timeSeconds, xDeg, yDeg, zDeg}.
 */
public final class ShadowCreakingAnimData {
	private ShadowCreakingAnimData() {
	}

	public static final float ROAR_LENGTH = 3.20833F;
	public static final float STUMBLE_LENGTH = 2.62500F;
	/** Extra seconds easing from the roar's hunched end pose back to standing */
	public static final float ROAR_STAND_SECONDS = 0.85F;
	public static final float ROAR_TOTAL = ROAR_LENGTH + ROAR_STAND_SECONDS;

	public static final float[][] STUMBLE_BODY2 = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.54167F, -15.00000F, 0.00000F, 0.00000F },
		{ 1.00000F, -37.50000F, 0.00000F, 0.00000F },
		{ 1.41667F, 22.50000F, 0.00000F, 0.00000F },
		{ 1.75000F, 57.50000F, 0.00000F, 0.00000F },
		{ 2.04167F, 32.50000F, 0.00000F, 0.00000F },
		{ 2.29167F, -30.00000F, 0.00000F, 0.00000F },
		{ 2.62500F, -2.50000F, 0.00000F, 0.00000F },
	};
	public static final float[][] STUMBLE_BODY = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
	};
	public static final float[][] STUMBLE_HEAD = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 1.29167F, 40.00000F, 0.00000F, 0.00000F },
		{ 1.41667F, 70.00000F, 0.00000F, 0.00000F },
		{ 1.75000F, 97.23640F, -6.18570F, 10.87060F },
		{ 2.62500F, 27.23640F, -6.18570F, 10.87060F },
	};
	public static final float[][] STUMBLE_LEFT_ARM = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.83333F, 29.03230F, 0.00000F, 0.00000F },
		{ 1.29167F, 65.07240F, -1.75850F, 1.77850F },
		{ 1.41667F, 95.80140F, 0.79290F, -43.48160F },
		{ 1.75000F, 92.47360F, 32.07900F, -52.99310F },
		{ 2.00000F, 23.10700F, 8.06140F, 7.66770F },
		{ 2.62500F, 8.05770F, -25.06860F, 10.65910F },
	};
	public static final float[][] STUMBLE_RIGHT_ARM = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.54167F, 25.16050F, -2.28970F, 4.44800F },
		{ 1.29167F, 119.05070F, -19.56570F, -5.76190F },
		{ 1.41667F, 189.05070F, -19.56570F, -5.76190F },
		{ 1.75000F, 229.05070F, -19.56570F, -5.76190F },
		{ 2.12500F, 34.05070F, -19.56570F, -5.76190F },
		{ 2.62500F, 44.05070F, -19.56570F, -5.76190F },
	};
	public static final float[][] STUMBLE_LEFT_LEG = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.54167F, -45.00000F, 0.00000F, 0.00000F },
		{ 1.00000F, 27.50000F, 0.00000F, 0.00000F },
		{ 1.41667F, -22.50000F, 0.00000F, 0.00000F },
		{ 1.75000F, 27.50000F, 0.00000F, 0.00000F },
		{ 2.12500F, -19.29000F, 0.00000F, 0.00000F },
		{ 2.37500F, 15.36000F, 0.00000F, 0.00000F },
		{ 2.62500F, 0.00000F, 0.00000F, 0.00000F },
	};
	public static final float[][] STUMBLE_RIGHT_LEG = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.54167F, 15.00000F, 0.00000F, 0.00000F },
		{ 1.00000F, -25.00000F, 0.00000F, 0.00000F },
		{ 1.41667F, 17.50000F, 0.00000F, 0.00000F },
		{ 1.75000F, -15.00000F, 0.00000F, 0.00000F },
		{ 2.12500F, 11.43000F, 0.00000F, 0.00000F },
		{ 2.37500F, -6.78000F, 0.00000F, 0.00000F },
		{ 2.62500F, 0.00000F, 0.00000F, 0.00000F },
	};
	public static final float[][] ROAR_BODY2 = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.83333F, 42.50000F, 0.00000F, 0.00000F },
		{ 1.66667F, -52.50000F, 0.00000F, 0.00000F },
		{ 1.79167F, -52.73740F, -4.55750F, 5.96260F },
		{ 2.29167F, -53.50510F, 8.33110F, -11.06590F },
		{ 3.20833F, -53.01240F, -5.24040F, 6.92920F },
	};
	public static final float[][] ROAR_BODY = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
	};
	public static final float[][] ROAR_HEAD = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.83333F, 25.00000F, 0.00000F, 0.00000F },
		{ 1.00000F, 39.22040F, -0.96180F, 12.43510F },
		{ 1.66667F, 69.22040F, -0.96180F, 12.43510F },
		{ 1.75000F, 71.49060F, -22.69170F, -0.20660F },
		{ 1.79167F, 71.84470F, 20.73130F, 25.20780F },
		{ 1.83333F, 71.49060F, -22.69170F, -0.20660F },
		{ 1.87500F, 71.84470F, 20.73130F, 25.20780F },
		{ 1.91667F, 71.49060F, -22.69170F, -0.20660F },
		{ 1.95833F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.00000F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.04167F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.08333F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.12500F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.16667F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.20833F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.25000F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.29167F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.33333F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.37500F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.41667F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.45833F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.50000F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.54167F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.58333F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.62500F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.66667F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.70833F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.75000F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.79167F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.83333F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.87500F, 71.84470F, 20.73130F, 25.20780F },
		{ 2.91667F, 71.49060F, -22.69170F, -0.20660F },
		{ 2.95833F, 71.84470F, 20.73130F, 25.20780F },
		{ 3.00000F, 71.49060F, -22.69170F, -0.20660F },
		{ 3.04167F, 71.84470F, 20.73130F, 25.20780F },
		{ 3.08333F, 71.49060F, -22.69170F, -0.20660F },
		{ 3.12500F, 71.84470F, 20.73130F, 25.20780F },
		{ 3.16667F, 71.49060F, -22.69170F, -0.20660F },
		{ 3.20833F, 71.84470F, 20.73130F, 25.20780F },
	};
	public static final float[][] ROAR_LEFT_ARM = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.83333F, 37.50000F, 0.00000F, -97.50000F },
		{ 1.66667F, 274.62840F, -14.04720F, -285.72940F },
	};
	public static final float[][] ROAR_RIGHT_ARM = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
		{ 0.83333F, 172.50000F, 0.00000F, 0.00000F },
		{ 1.66667F, -107.50000F, 0.00000F, 0.00000F },
	};
	public static final float[][] ROAR_LEFT_LEG = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
	};
	public static final float[][] ROAR_RIGHT_LEG = {
		{ 0.00000F, 0.00000F, 0.00000F, 0.00000F },
	};

	/** Linear sample of a keyframe track, returns degrees xyz */
	public static float[] sample(float[][] track, float timeSec) {
		if (track.length == 1) {
			return new float[] { track[0][1], track[0][2], track[0][3] };
		}
		if (timeSec <= track[0][0]) {
			return new float[] { track[0][1], track[0][2], track[0][3] };
		}
		float[] last = track[track.length - 1];
		if (timeSec >= last[0]) {
			return new float[] { last[1], last[2], last[3] };
		}
		for (int i = 0; i < track.length - 1; i++) {
			float[] a = track[i];
			float[] b = track[i + 1];
			if (timeSec >= a[0] && timeSec <= b[0]) {
				float span = b[0] - a[0];
				float u = span <= 1.0E-6F ? 1.0F : (timeSec - a[0]) / span;
				return new float[] {
					a[1] + (b[1] - a[1]) * u,
					a[2] + (b[2] - a[2]) * u,
					a[3] + (b[3] - a[3]) * u
				};
			}
		}
		return new float[] { last[1], last[2], last[3] };
	}

	/** Sample roar, then ease all channels to zero during the stand-up tail */
	public static float[] sampleRoar(float[][] track, float timeSec) {
		if (timeSec <= ROAR_LENGTH) {
			return sample(track, timeSec);
		}
		float[] end = sample(track, ROAR_LENGTH);
		float u = Math.min(1.0F, (timeSec - ROAR_LENGTH) / ROAR_STAND_SECONDS);
		// smoothstep back to standing
		u = u * u * (3.0F - 2.0F * u);
		float inv = 1.0F - u;
		return new float[] { wrap(end[0]) * inv, wrap(end[1]) * inv, wrap(end[2]) * inv };
	}

	/**
	 * Keyframes wind past a full turn (roar's left arm ends near -286 deg). Wrap
	 * to [-180, 180] so the arm settles instead of sweeping all the way around
	 */
	private static float wrap(float degrees) {
		float d = degrees % 360.0F;
		if (d >= 180.0F) {
			d -= 360.0F;
		} else if (d < -180.0F) {
			d += 360.0F;
		}
		return d;
	}
}
