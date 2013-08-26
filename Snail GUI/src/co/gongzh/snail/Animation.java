package co.gongzh.snail;

import javax.swing.SwingUtilities;

/**
 * Represents an abstract animation with a fixed duration.
 * Subclass should override {@link #animate(float)} method to implement the animation.
 * <p>
 * Call {@link #commit()} to play an animation, and call {@link #cancel()} to stop.
 * @author Gong Zhang
 */
abstract public class Animation {
	
	public static interface Interpolator {
		public float apply(float value);
	}
	
	public static final Interpolator Linear = new Interpolator() {
		@Override
		public float apply(float value) {
			return value;
		}
	};
	
	public static final Interpolator EaseIn = new Interpolator() {
		@Override
		public float apply(float value) {
			return value * value;
		}
	};
	
	public static final Interpolator EaseOut = new Interpolator() {
		@Override
		public float apply(float value) {
			return value * (2.0f - value);
		}
	};
	
	public static final Interpolator EaseInEaseOut = new Interpolator() {
		@Override
		public float apply(float value) {
			return value <= 0.5f ? (2.0f * value * value) : (1.0f - 2.0f * (value - 1) * (value - 1));
		}
	};

	private final float duration;
	private Interpolator interpolator;
	private float timer;
	private boolean playing;
	private final Object mutex;
	
	/**
	 * Creates an animation with specified <code>duration</code>.
	 * @param duration
	 */
	public Animation(float duration) {
		this(duration, null, null);
	}
	
	/**
	 * Creates an animation with specified <code>duration</code> and <code>mutex</code>.
	 * <p>
	 * The <code>mutex</code> is a tag that prevents animations with same tag playing at
	 * same time. When committing an animation with mutex tag, the GUI driver will cancel existing
	 * animation with same tag first, then play the new animation. The mutex tag is distinguished
	 * by {@link Object#equals(Object)} method. The mutex tag is optional. Pass <code>null</code>
	 * to <code>mutex</code> parameter if this animation doesn't need to exclude any other animations.
	 * @param duration
	 * @param mutex
	 */
	public Animation(float duration, Object mutex) {
		this(duration, null, mutex);
	}
	
	/**
	 * Creates an animation with specified <code>duration</code> and <code>interpolator</code>.
	 * @param duration
	 * @param interpolator
	 */
	public Animation(float duration, Interpolator interpolator) {
		this(duration, interpolator, null);
	}
	
	/**
	 * Creates an animation with specified <code>duration</code>, <code>interpolator</code>
	 * and <code>mutex</code>.
	 * <p>
	 * The <code>mutex</code> is a tag that prevents animations with same tag playing at
	 * same time. When committing an animation with mutex tag, the GUI driver will cancel existing
	 * animation with same tag first, then play the new animation. The mutex tag is distinguished
	 * by {@link Object#equals(Object)} method. The mutex tag is optional. Pass <code>null</code>
	 * to <code>mutex</code> parameter if this animation doesn't need to exclude any other animations.
	 * @param duration
	 * @param interpolator
	 * @param mutex
	 */
	public Animation(float duration, Interpolator interpolator, Object mutex) {
		this.duration = duration;
		this.interpolator = interpolator == null ? Linear : interpolator;
		this.mutex = mutex;
		timer = 0.0f;
		playing = false;
	}
	
	public final boolean isPlaying() {
		return playing;
	}
	
	public final float getDuration() {
		return duration;
	}
	
	public final Interpolator getInterpolator() {
		return interpolator;
	}
	
	public void setInterpolator(Interpolator interpolator) {
		this.interpolator = interpolator;
	}
	
	/**
	 * Starts to play the animation. 
	 * <p>
	 * <strong>Important:</strong>
	 * This method should be only called in GUI thread.
	 */
	public void commit() {
		if (!playing) {
			timer = 0.0f;
			ViewContext.SharedDriver.registerAnimation(this);
			playing = true;
			animate(interpolator.apply(0.0f));
		} else {
			throw new IllegalStateException("animation is already committed.");
		}
	}
	
	/**
	 * Cancel this animation. If not playing yet, this method does nothing.
	 * <p>
	 * <strong>Important:</strong>
	 * This method should be only called in GUI thread.
	 */
	public void cancel() {
		if (playing) {
			playing = false;
			ViewContext.SharedDriver.unregisterAnimation(this);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					completed(true);
				}
			});
		}
	}
	
	// called by Driver in GUI thread.
	final void update(float dt) {
		if (!playing) return;
		
		timer += dt;
		if (timer < duration) {
			animate(interpolator.apply(timer / duration));
		} else {
			animate(interpolator.apply(1.0f));
			playing = false;
			ViewContext.SharedDriver.unregisterAnimation(this);
			completed(false);
		}
	}
	
	/**
	 * Implements the animation at specified <code>progress</code>.
	 * Called on GUI thread.
	 * @param progress a percentage value (from 0.0 to 1.0)
	 */
	abstract protected void animate(float progress);
	
	/**
	 * Completion callback of the animation.
	 * Called on GUI thread.
	 * @param canceled indicates whether the animation is canceled by {@link #cancel()} or not.
	 */
	protected void completed(boolean canceled) {
	}
	
	protected final float getLinearProgress() {
		return timer / duration;
	}
	
	public final Object getMutex() {
		return mutex;
	}
	
	public final Animation makeDelay(float delay) {
		final Animation target = this;
		return new Animation(delay, this.mutex) {
			@Override
			protected void animate(float progress) {}
			@Override
			protected void completed(boolean canceled) {
				if (!canceled) target.commit();
			}
		};
	}
	
}
