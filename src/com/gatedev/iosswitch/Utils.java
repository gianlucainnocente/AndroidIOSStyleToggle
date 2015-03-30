package com.gatedev.iosswitch;

import android.graphics.PointF;

public final class Utils {
	
	public static float distance(PointF p1, PointF p2) {
		return (float) Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
	}

	public static float distance(float p1x, float p1y, float p2x, float p2y) {
		return (float) Math.sqrt(Math.pow((p2x - p1x), 2) + Math.pow((p2y - p1y), 2));
	}

}