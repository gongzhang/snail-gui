package co.gongzh.snail.text;

import java.awt.Color;

import co.gongzh.snail.Animation;
import co.gongzh.snail.PaintMode;
import co.gongzh.snail.View;
import co.gongzh.snail.util.Vector2D;

public abstract class CaretView extends View {

	public abstract void showCaret();
	public abstract void hideCaret();
	public abstract void enableFlicker();
	public abstract void disableFlicker();
	public abstract void locateCaretView(Vector2D basePoint, int ascent, int descent);
	
	public static class DefaultCaretView extends CaretView {
		
		private final Animation flickerAnimation;
		private boolean flicker = true;
		private static final Color CARET_COLOR = new Color(0x0077aa);
		
		public DefaultCaretView() {
			setBackgroundColor(CARET_COLOR);
			setPaintMode(PaintMode.DIRECTLY);
			
			// animation durations:
			final float d1 = 0.4f; // hold on
			final float d2 = 0.17f; // fading out
			final float d3 = 0.25f; // disappeared
			final float d4 = 0.14f; // fading in
			
			flickerAnimation = new Animation(d1 + d2 + d3 + d4) {
				@Override
				protected void animate(float t) {
					if (t < d1) {
						t = 1.0f;
					} else if (t < d1 + d2) {
						t = (d1 + d2 - t) / d2;
					} else if (t < d1 + d2 + d3) {
						t = 0.0f;
					} else {
						t = (t - d1 - d2 - d3) / d4;
					}
					setAlpha(t);
				}
				@Override
				protected void completed(boolean canceled) {
					if (!canceled) commit(); // auto-replay
				}
			};
		}

		@Override
		public void showCaret() {
			setHidden(false);
			setAlpha(1.0f);
			if (!flickerAnimation.isPlaying()) {
				flickerAnimation.commit();
			}
		}

		@Override
		public void hideCaret() {
			if (flickerAnimation.isPlaying()) {
				flickerAnimation.cancel();
			}
			setHidden(true);
		}

		@Override
		public void enableFlicker() {
			flicker = true;
			if (!flickerAnimation.isPlaying()) {
				flickerAnimation.commit();
			}
		}

		@Override
		public void disableFlicker() {
			flicker = false;
			if (flickerAnimation.isPlaying()) {
				flickerAnimation.cancel();
				setAlpha(1.0f);
			}
		}

		@Override
		public void locateCaretView(Vector2D basePoint, int ascent, int descent) {
			setPosition(basePoint.x, basePoint.y - ascent);
			setSize(2, ascent + descent);
			if (!isHidden() && flicker) {
				// force to replay flicker animation 
				disableFlicker();
				enableFlicker();
			}
		}
		
	}
	
}
