/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.araplox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class TemperaturePanel extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = "araplox";
	private SurfaceHolder holder;
	
	// Protected by "holder"
	private boolean created;
	private Paint paint;
	private Rect rect;

	public TemperaturePanel(Context ctx, AttributeSet attrSet) {
		super(ctx, attrSet);
		this.holder = getHolder();
		synchronized (this.holder) {
			this.created = false;
			this.paint = new Paint();
			this.rect = new Rect();
		}
		synchronized (this.holder) {
			this.holder.addCallback(this);
		}
	}

	public void updateTemperatures(float[][] normalizedTemps) {
		synchronized (this.holder) {
			if (!this.created) {
				// There's no surface (either it hasn't been created yet, or it's been
				// destroyed), so there's nothing for us to do.
				return;
			}

			Canvas canvas = this.holder.lockCanvas();
			if (canvas == null) {
				Log.w(TAG, "can't get canvas; ignoring temp update");
				return;
			}
			
			boolean ok = false;
			try {
				canvas.drawColor(0);
				this.doUpdateTemperatures(normalizedTemps, canvas);
				ok = true;
			} finally {
				// In a finally block in case doUpdateMlx90620() throws
				// an exception unexpectedly
				holder.unlockCanvasAndPost(canvas);
			}
		}
	}
	
	private void doUpdateTemperatures(float[][] normalizedTemps, Canvas canvas) {
		int cWidth = canvas.getWidth(), cHeight = canvas.getHeight();
		int nRows = normalizedTemps.length, nCols = normalizedTemps[0].length;
		int rectWidth = cWidth / nCols, rectHeight = cHeight / nRows;
		
		rectWidth = rectHeight = Math.min(rectWidth, rectHeight);
		
		for (int r = 0; r < nRows; r++) {
			for (int c = 0; c < nCols; c++) {
				int left = c * rectWidth, top = r * rectHeight;
				int right = (c + 1) * rectWidth - 1, bottom = (r + 1) * rectHeight - 1;
				assert 0 <= left && left < cWidth;
				assert 0 <= top && top < cHeight;
				assert 0 < right && right <= cWidth;
				assert 0 < bottom && bottom <= cHeight;
				this.rect.set(left, top, right, bottom);	
				this.paint.setColor(normTempToColor(normalizedTemps[r][c]));
				canvas.drawRect(rect, paint);
			}
		}
	}
	
	private static int normTempToColor(float normalizedTemp) {
		// This is a simple heatmap that maps 0.0f to blue, 1.0f
		// to red, and other values to the hues between.
		if (normalizedTemp > 1.0f) {
			Log.w(TAG, "clamping temperature " + normalizedTemp + " to 1.0f");
			normalizedTemp = 1.0f;
		}
		if (normalizedTemp < 0.0f) {
			Log.w(TAG, "clamping temperature " + normalizedTemp + " to 0.0f");
			normalizedTemp = 0.0f;
		}
		float h = 240.0f * (1.0f - normalizedTemp);
		float s = 1.0f;
		float v = 1.0f;
		return Color.HSVToColor(new float[]{h, s, v});
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		this.doSurfaceUpdate(holder);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		this.doSurfaceUpdate(holder);
	}
	
	private void doSurfaceUpdate(SurfaceHolder holder) {
		synchronized (this.holder) {
			if (this.holder != holder) {
				throw new IllegalStateException("unrecognized SurfaceHolder");
			}
			this.created = true;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		synchronized (this.holder) {
			if (this.holder != holder) {
				throw new IllegalStateException("unrecognized SurfaceHolder");
			}
			this.created = false;
		}
	}
}
