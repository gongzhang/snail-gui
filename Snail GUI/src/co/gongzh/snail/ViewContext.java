package co.gongzh.snail;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.TextHitInfo;
import java.awt.geom.Rectangle2D;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.lang.reflect.InvocationTargetException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import co.gongzh.snail.text.InputMethodCompatible;
import co.gongzh.snail.util.Vector2D;


public class ViewContext {
	
	//// Static ////
	
	static class Driver implements Runnable {
		
		private final List<ViewContext> viewContexts;
		private final List<Animation> animations;
		private boolean running;
		int fps;
		int max_fps;
		
		Driver() {
			viewContexts = new ArrayList<ViewContext>();
			animations = new LinkedList<Animation>();
			running = false;
			max_fps = 60;
			fps = 0;
		}
		
		synchronized void registerContext(ViewContext w) {
			if (!viewContexts.contains(w)) {
				viewContexts.add(w);
				if (running == false) {
					running = true;
					new Thread(this).start();
				}
			}
		}
		
		synchronized void unregisterContext(ViewContext w) {
			if (viewContexts.contains(w)) {
				if (w.graphics != null) {
					w.graphics.dispose();
					w.graphics = null;
				}
				if (w.temporaryImage != null) {
					w.temporaryImage.flush();
					w.temporaryImage = null;
				}
				w.bufferPool.clearBuffer();
				viewContexts.remove(w);
				if (viewContexts.size() == 0) {
					running = false;
					if (sharedGraphics != null) {
						sharedGraphics.dispose();
						sharedGraphics = null;
					}
					if (sharedTemporaryImage != null) {
						sharedTemporaryImage.flush();
						sharedTemporaryImage = null;
					}
				}
			}
		}
		
		// only called by Animation.commit in GUI thread
		void registerAnimation(Animation a) {
			if (a.getMutex() != null) {
				for (Animation animation : animations) {
					if (a.getMutex().equals(animation.getMutex())) {
						animation.cancel();
						break;
					}
				}
			}
			animations.add(a);
		}
		
		// only called by Animation.cancel in GUI thread
		void unregisterAnimation(Animation a) {
			animations.remove(a);
		}
		
		@Override
		public void run() {
			class Timer {
				long t0 = System.currentTimeMillis();
				long t, dt;
				float fdt; // "dt" in seconds
			}
			final Timer timer = new Timer();
			final Runnable updateAll = new Runnable() {
				ViewContext[] contexts = new ViewContext[1];
				Animation[] animations_array = new Animation[1];
				@Override
				public void run() {
					int count;
					
					// animation
					count = animations.size();
					animations_array = animations.toArray(animations_array);
					for (int i = 0; i < count; i++) {
						Animation ani = animations_array[i];
						ani.update(timer.fdt);
					}
					
					// rendering
					synchronized (Driver.this) {
						count = viewContexts.size();
						contexts = viewContexts.toArray(contexts);
					}
					for (int i = 0; i < count; i++) {
						contexts[i].update(timer.dt);
						if (contexts[i].contentInvalid) {
							contexts[i].content.repaint();
							contexts[i].contentInvalid = false;
						}
					}
					
				}
			};
			while (running) {
				timer.t = System.currentTimeMillis();
				if ((timer.dt = timer.t - timer.t0) * max_fps > 1000L) {
					fps = (int) (1000L / timer.dt);
					timer.fdt = timer.dt / 1000.0f;
					try {
						SwingUtilities.invokeAndWait(updateAll);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					} catch (InvocationTargetException ex) {
						ex.printStackTrace();
					}
					Thread.yield();
					timer.t0 = timer.t;
				} else {
					try {
						Thread.sleep(1000L / max_fps - timer.dt);
					} catch (InterruptedException e) {
						Thread.yield();
					}
				}
			}
		}
	}
	
	final static Driver SharedDriver = new Driver();
	private static BufferedImage sharedTemporaryImage = null;
	private static ViewGraphics sharedGraphics = null;
	
	public static GraphicsConfiguration getDefaultGraphicsConfiguration() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
	}
	
	public static ViewGraphics getSharedTemporaryGraphicsContext() {
		if (sharedTemporaryImage == null) {
			sharedTemporaryImage = getDefaultGraphicsConfiguration().createCompatibleImage(1, 1, Transparency.TRANSLUCENT);
		}
		if (sharedGraphics == null) {
			sharedGraphics = new ViewGraphics(sharedTemporaryImage.createGraphics());
		}
		return sharedGraphics;
	}

	public static int getFPS() {
		return SharedDriver.fps;
	}

	public static int getMaximumFPS() {
		return SharedDriver.max_fps;
	}

	public static void setMaximumFPS(int max_fps) {
		SharedDriver.max_fps = max_fps;
	}

	//// Instance ////
	
	
	class BufferPool {
		long buffer_active = 0;
		long buffer_cached = 0;
		long buffer_limitation = 32 * 1024 * 1024; // 32 MB
		static private final int SQUARE = 0;		// 4:3 - 3:4
		static private final int WIDE = 1;			// 4:3 - 4:2
		static private final int NARROW = 2;		// 3:4 - 2:4
		static private final int VERY_WIDE = 3;		// 4:2 - 4:0
		static private final int VERY_NARROW = 4;	// 2:4 - 0:4
		private int getRectangleType(int width, int height) {
			width *= 12;
			if (height * 9 <= width && width <= height * 16) return SQUARE;
			else {
				height *= 12;
				if (width > height) {
					return (width >> 1) <= height ? WIDE : VERY_WIDE;
				} else {
					return (height >> 1) <= width ? NARROW : VERY_NARROW;
				}
			}
		}
		private final ArrayList<LinkedList<VolatileImage>> pools;
		BufferPool() {
			pools = new ArrayList<LinkedList<VolatileImage>>(5);
			for (int i = 0; i < 5; i++) {
				pools.add(new LinkedList<VolatileImage>());
			}
		}
		private VolatileImage requestBufferFromPool(int width, int height) {
			LinkedList<VolatileImage> pool = pools.get(getRectangleType(width, height));
			for (VolatileImage buf : pool) {
				// skip small buffer
				if (buf.getWidth() >= width && buf.getHeight() >= height) {
					// compute waste
					if (buf.getWidth() <= width * 2 && buf.getHeight() <= height * 2) {
						// hit
						pool.remove(buf);
						return buf;
					} else {
						return null;
					}
				}
			}
			return null;
		}
		VolatileImage getBuffer(int width, int height) {
			if (width <= 0) width = 1;
			if (height <= 0) height = 1;
			VolatileImage buf = requestBufferFromPool(width, height);
			if (buf == null) {
				// create new buffer
				buffer_active += width * height * 4;
				buf = configuration.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
			} else {
				int delta = buf.getWidth() * buf.getHeight() * 4;
				buffer_active += delta;
				buffer_cached -= delta;
			}
			return buf;
		}
		void turnBackBuffer(VolatileImage buf) {
			int delta = buf.getWidth() * buf.getHeight() * 4;
			LinkedList<VolatileImage> pool = pools.get(getRectangleType(buf.getWidth(), buf.getHeight()));
			ListIterator<VolatileImage> it = pool.listIterator();
			while (it.hasNext()) {
				VolatileImage img = (VolatileImage) it.next();
				if (buf.getWidth() * buf.getHeight() <= img.getWidth() * img.getHeight()) {
					// insert
					it.set(buf);
					it.add(img);
					buffer_active -= delta;
					buffer_cached += delta;
					checkBufferLimitation();
					return;
				}
			}
			it.add(buf);
			buffer_active -= delta;
			buffer_cached += delta;
			checkBufferLimitation();
		}
		void checkBufferLimitation() {
			while (buffer_cached > 0 && buffer_cached + buffer_active > buffer_limitation) {
				for (LinkedList<VolatileImage> pool : pools) {
					if (pool.size() > 0) {
						VolatileImage buf = pool.removeLast();
						buffer_cached -= buf.getWidth() * buf.getHeight() * 4;
						buf.flush();
						if (buffer_cached + buffer_active <= buffer_limitation) {
							return;
						}
					}
				}
			}
		}
		void clearBuffer() {
			for (LinkedList<VolatileImage> pool : pools) {
				for (VolatileImage buf : pool) {
					buf.flush();
				}
				pool.clear();
			}
			buffer_cached = 0;
		}
	}
	
	// content
	
	private final JPanel content;
	private View rootView;
	private final GraphicsConfiguration configuration;
	private boolean contentInvalid = true;
	
	// utility
	private BufferedImage temporaryImage;
	private ViewGraphics graphics;
	final BufferPool bufferPool;
	
	// debug mode
	private boolean debugMode = false;
	private View debugTarget = null;
	private Font debugFont = null;
	
	public ViewContext(Container container) {
		this(container, new View());
	}
	
	public ViewContext(final Container parent, final View rootView) {
		configuration = parent.getGraphicsConfiguration();
		contentInvalid = true;
		bufferPool = new BufferPool();
		
		final InputMethodRequests inputMethodRequests = new InputMethodRequests() {
			
			@Override
			public Rectangle getTextLocation(TextHitInfo offset) {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().getTextLocation(offset);
				} else {
					return new Rectangle();
				}
			}
			
			@Override
			public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().getSelectedText(attributes);
				} else {
					return new co.gongzh.snail.text.AttributedString().iterator();
				}
			}
			
			@Override
			public TextHitInfo getLocationOffset(int x, int y) {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().getLocationOffset(x, y);
				} else {
					return TextHitInfo.leading(0);
				}
			}
			
			@Override
			public int getInsertPositionOffset() {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().getInsertPositionOffset();
				} else {
					return 0;
				}
			}
			
			@Override
			public int getCommittedTextLength() {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().getCommittedTextLength();
				} else {
					return 0;
				}
			}
			
			@Override
			public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, Attribute[] attributes) {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().getCommittedText(beginIndex, endIndex, attributes);
				} else {
					return new co.gongzh.snail.text.AttributedString().iterator();
				}
			}
			
			@Override
			public AttributedCharacterIterator cancelLatestCommittedText(Attribute[] attributes) {
				if (inputMethodView != null) {
					return inputMethodView.getInputMethodInfoProvider().cancelLatestCommittedText(attributes);
				} else {
					return null;
				}
			}
			
		};
		content = new JPanel() {
			private static final long serialVersionUID = 8751687976961703833L;
			@Override
			protected void paintComponent(Graphics g) {
				ViewContext.this.paint((Graphics2D) g);
			}
			@Override
			public InputMethodRequests getInputMethodRequests() {
				return inputMethodRequests;
			}
		};
//		content.enableInputMethods(false);
		content.addInputMethodListener(new InputMethodListener() {
			
			@Override
			public void inputMethodTextChanged(InputMethodEvent e) {
				if (inputMethodView != null)
					inputMethodView.getInputMethodEventHandler().inputMethodTextChanged(e);
			}
			
			@Override
			public void caretPositionChanged(InputMethodEvent e) {
				if (inputMethodView != null)
					inputMethodView.getInputMethodEventHandler().caretPositionChanged(e);
			}
			
		});
		
		content.setBackground(Color.BLACK);
		content.setLocation(0, 0);
		content.setSize(parent.getSize());
		content.setFocusable(true);
		content.setFocusTraversalKeysEnabled(false);
		
		parent.setLayout(new BorderLayout());
		parent.add(content, BorderLayout.CENTER);
		parent.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				content.repaint();
			}
		});
		parent.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				contentResized(content.getWidth(), content.getHeight());
			}
		});
		
		content.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				fireKeyTyped(e);
				e.consume();
			}
			@Override
			public void keyReleased(KeyEvent e) {
				fireKeyReleased(e);
				e.consume();
			}
			@Override
			public void keyPressed(KeyEvent e) {
				fireKeyPressed(e);
				e.consume();
			}
		});
		content.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				currentMousePosition = Vector2D.make(e.getPoint());
				co.gongzh.snail.MouseEvent event = new co.gongzh.snail.MouseEvent(e, getVelocity());
				fireMouseReleased(event);
				fireMouseClicked(event = new co.gongzh.snail.MouseEvent(e, getVelocity()));
				updateMouseOnViewChain(event.getPosition(rootView));
				e.consume();
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (!content.isFocusOwner()) {
					content.requestFocusInWindow();
				}
				currentMousePosition = Vector2D.make(e.getPoint());
				pressedMouseButton = e.getButton();
				co.gongzh.snail.MouseEvent event = new co.gongzh.snail.MouseEvent(e, getVelocity());
				updateMouseOnViewChain(event.getPosition(rootView));
				fireMousePressed(event);
				e.consume();
			}
			@Override
			public void mouseExited(MouseEvent e) {
				if (e.getButton() == MouseEvent.NOBUTTON) {
					updateMouseOnViewChain(rootView.transformPointFromSuperView(Vector2D.make(e.getPoint())));
					setMouseOnView(new LinkedList<View>());
				}
				e.consume();
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				e.consume();
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
			}
		});
		content.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				currentMousePosition = Vector2D.make(e.getPoint());
				co.gongzh.snail.MouseEvent event = new co.gongzh.snail.MouseEvent(e, getVelocity());
				updateMouseOnViewChain(event.getPosition(rootView));
				fireMouseMoved(event);
				e.consume();
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				currentMousePosition = Vector2D.make(e.getPoint());
				co.gongzh.snail.MouseEvent event = new co.gongzh.snail.MouseEvent(e, getVelocity());
				event.setButton(pressedMouseButton);
				fireMouseDragged(event);
				e.consume();
			}
		});
		content.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				currentMousePosition = Vector2D.make(e.getPoint());
				co.gongzh.snail.MouseWheelEvent event = new co.gongzh.snail.MouseWheelEvent(e, getVelocity());
				updateMouseOnViewChain(event.getPosition(rootView));
				fireMouseWheelMoved(event);
				e.consume();
			}
		});
		
		setRootView(rootView);
		SharedDriver.registerContext(ViewContext.this);
	}
	
	public final View getRootView() {
		return rootView;
	}

	public void setRootView(View rootView) {
		if (this.rootView != null) {
			View.setViewContext(rootView, null);
		}
		if (rootView == null) rootView = new View();
		this.rootView = rootView;
		if (rootView.getViewContext() != null) {
			throw new IllegalArgumentException("the view already has an owner context.");
		}
		View.setViewContext(rootView, this);
		contentResized(content.getWidth(), content.getHeight());
	}

	public final GraphicsConfiguration getGraphicsConfiguration() {
		return configuration;
	}

	/**
	 * @deprecated
	 * @see #getSwingContainer()
	 */
	@Deprecated
	public final Container getAWTContainer() {
		return content;
	}
	
	public final JPanel getSwingContainer() {
		return content;
	}

	public final ViewGraphics getTemporaryGraphicsContext() {
		if (temporaryImage == null) {
			temporaryImage = configuration.createCompatibleImage(1, 1, Transparency.TRANSLUCENT);
		}
		if (graphics == null) {
			graphics = new ViewGraphics(temporaryImage.createGraphics());
		}
		return graphics;
	}

	// TODO: prepare content
//	public void prepareContent(int width, int height) {
//		rootView.setSize(width, height);
//		ViewGraphics g = new ViewGraphics(getTemporaryGraphicsContext());
//		rootView.repaint(g, false);
//		g.dispose();
//	}
//
//	public void prepareContent() {
//		prepareContent(content.getWidth(), content.getHeight());
//	}

	public final int getMaximumBufferSize() {
		return (int) (bufferPool.buffer_limitation / 1024 / 1024);
	}

	public void setMaximumBufferSize(int megaByte) {
		bufferPool.buffer_limitation = megaByte * 1024L * 1024L;
	}

	public void dispose() {
		SharedDriver.unregisterContext(this);
		content.setVisible(false);
		Container container = content.getParent();
		container.remove(content);
	}

	public boolean isDebugModeEnable() {
		return debugMode;
	}

	public void setDebugModeEnable(boolean debugMode) {
		this.debugMode = debugMode;
		if (debugFont == null)
			debugFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
		invalid();
	}

	public View getDebugTarget() {
		return debugMode ? debugTarget : null;
	}

	public void setDebugTarget(View view) {
		if (debugMode && view != null && view.getViewContext() == this) {
			debugTarget = view;
			invalid();
		}
	}

	private void contentResized(int w, int h) {
		if (rootView != null) {
			rootView.setPosition(0, 0);
			rootView.setSize(w, h);
		}
	}
	
	private void paint(Graphics2D g) {
		if (rootView != null) {
			ViewGraphics gx = new ViewGraphics(g);
			rootView.repaint(gx, false);

			// debug mode
			if (debugMode) {
				g.setStroke(new BasicStroke());
				g.setColor(Color.MAGENTA);
				g.setFont(debugFont);
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				
				String label = null;
				Rectangle2D rect = null;
				final FontMetrics fm = g.getFontMetrics();
				final int ascent = fm.getMaxAscent();
				
				if (debugTarget != null) {
					
					// border
					Vector2D pos = debugTarget.getPositionInRootView();
					g.drawRect(pos.x, pos.y, debugTarget.getWidth() - 1, debugTarget.getHeight() - 1);
					
					// class name
					String className = null;
					if (debugTarget.getClass().isAnonymousClass()) {
						className = debugTarget.getClass().getSuperclass().getSimpleName();
					} else {
						className = debugTarget.getClass().getSimpleName();
					}
					
					// show label
					label = String.format("%s(%d,%d,%d,%d)",
							className,
							debugTarget.getLeft(),
							debugTarget.getTop(),
							debugTarget.getWidth(),
							debugTarget.getHeight()
							);
					
					rect = fm.getStringBounds(label, g);
					g.fillRect(pos.x, pos.y, (int) rect.getWidth() + 1, (int) rect.getHeight() + 1);
					g.setColor(Color.white);
					g.drawString(label, pos.x + 1, pos.y + ascent + 1);
				}
				
				// show memory info
				label = String.format("fps = %d, view_alloc = %d, active_buf = %.1f MB, cached_buf = %.1f MB",
						SharedDriver.fps,
						View.debug_view_alloc_count,
						bufferPool.buffer_active / 1024.0 / 1024.0,
						bufferPool.buffer_cached / 1024.0 / 1024.0
						);
				rect = fm.getStringBounds(label, g);
				g.setColor(Color.orange);
				g.fillRect(rootView.getWidth() - (int) rect.getWidth() - 1, rootView.getHeight() - (int) rect.getHeight() - 1, (int) rect.getWidth() + 1, (int) rect.getHeight() + 1);
				g.setColor(Color.black);
				g.drawString(label, rootView.getWidth() - (int) rect.getWidth(), rootView.getHeight() - (int) rect.getHeight() + ascent);
				
				// repaint on every frames
				invalid();
			}
		}
	}
	
	void invalid() {
		contentInvalid = true;
	}
	
	//// Keyboard Event ////
	
	private LinkedList<View> focusViewChain = new LinkedList<View>();
	private InputMethodCompatible inputMethodView = null;
	
	void requestFocus(View v) {
		if (isFocus(v)) return;
		
		final LinkedList<View> newChain = new LinkedList<View>();
		final LinkedList<View> oldChain = new LinkedList<View>(focusViewChain);
		View view = v;
		while (view != null) {
			newChain.addFirst(view);
			view = view.getSuperView();
		}
		// notify
		if (!focusViewChain.isEmpty()) {
			if (inputMethodView != null) {
//				content.enableInputMethods(false);
				inputMethodView = null;
			}
		}
		focusViewChain = newChain;
		if (!oldChain.isEmpty()) {
			oldChain.getLast().fireLostKeyboardFocus();
		}
		if (!newChain.isEmpty()) {
			v.fireGotKeyboardFocus();
			if (v instanceof InputMethodCompatible) {
				InputMethodCompatible imv = (InputMethodCompatible) v;
				if (imv.isInputMethodEnabled()) {
					inputMethodView = imv;
//					content.enableInputMethods(true);
				}
			}
		}
	}
	
	boolean isInFocusChain(View v) {
		return focusViewChain.contains(v);
	}
	
	boolean isFocus(View v) {
		return !focusViewChain.isEmpty() && focusViewChain.getLast() == v;
	}
	
	private void fireKeyPressed(KeyEvent event) {
		final co.gongzh.snail.KeyEvent e = new co.gongzh.snail.KeyEvent(event);
		final View[] chain = focusViewChain.toArray(new View[focusViewChain.size()]);
		for (int i = 0; i < chain.length; i++) {
			chain[i].firePreKeyPressed(e);
			if (e.handled) return;
		}
		for (int i = chain.length - 1; i >= 0; i--) {
			chain[i].fireKeyPressed(e);
			if (e.handled) return;
		}
	}
	
	private void fireKeyReleased(KeyEvent event) {
		final co.gongzh.snail.KeyEvent e = new co.gongzh.snail.KeyEvent(event);
		final View[] chain = focusViewChain.toArray(new View[focusViewChain.size()]);
		for (int i = 0; i < chain.length; i++) {
			chain[i].firePreKeyReleased(e);
			if (e.handled) return;
		}
		for (int i = chain.length - 1; i >= 0; i--) {
			chain[i].fireKeyReleased(e);
			if (e.handled) return;
		}
	}
	
	private void fireKeyTyped(KeyEvent event) {
		final co.gongzh.snail.KeyEvent e = new co.gongzh.snail.KeyEvent(event);
		final View[] chain = focusViewChain.toArray(new View[focusViewChain.size()]);
		for (int i = 0; i < chain.length; i++) {
			chain[i].firePreKeyTyped(e);
			if (e.handled) return;
		}
		for (int i = chain.length - 1; i >= 0; i--) {
			chain[i].fireKeyTyped(e);
			if (e.handled) return;
		}
	}
	
	//// Mouse Event ////
	
	private List<View> mouseOnViewChain = new ArrayList<View>();
	private final Vector2D lastMousePositionOnRootView = Vector2D.make();
	
	boolean isInMouseOnViewChain(View view) {
		return mouseOnViewChain.contains(view);
	}
	
	void updateMouseOnViewChain(Vector2D positionOnRootView) {
		Vector2D p = null;
		if (positionOnRootView != null) {
			p = positionOnRootView;
			lastMousePositionOnRootView.set(positionOnRootView);
		} else {
			p = lastMousePositionOnRootView.clone();
		}
		final List<View> newMouseOnViewChain = new ArrayList<View>();
		newMouseOnViewChain.add(rootView);
		View.getViewHierachyAtPoint(rootView, p, newMouseOnViewChain);
		setMouseOnView(newMouseOnViewChain);
		if (debugMode) {
			View newDebugTarget = newMouseOnViewChain.get(newMouseOnViewChain.size() - 1);
			if (debugTarget != newDebugTarget) {
				debugTarget = newDebugTarget;
				invalid();
			}
		}
	}
	
	private void setMouseOnView(List<View> new_chain) {
		for (View old_view : mouseOnViewChain) {
			if (!new_chain.contains(old_view)) old_view.fireMouseExited();
		}
		for (View new_view : new_chain) {
			if (!mouseOnViewChain.contains(new_view)) new_view.fireMouseEntered();
		}
		mouseOnViewChain = new_chain;
	}
	
	private void fireMousePressed(co.gongzh.snail.MouseEvent e) {
		for (View view : mouseOnViewChain) {
			view.firePreMousePressed(e);
			if (e.handled) return;
		}
		for (int i = mouseOnViewChain.size() - 1; i >= 0; i--) {
			mouseOnViewChain.get(i).fireMousePressed(e);
			if (e.handled) return;
		}
	}
	
	private void fireMouseReleased(co.gongzh.snail.MouseEvent e) {
		for (View view : mouseOnViewChain) {
			view.firePreMouseReleased(e);
			if (e.handled) return;
		}
		for (int i = mouseOnViewChain.size() - 1; i >= 0; i--) {
			mouseOnViewChain.get(i).fireMouseReleased(e);
			if (e.handled) return;
		}
	}
	
	private void fireMouseClicked(co.gongzh.snail.MouseEvent e) {
		Vector2D p = currentMousePosition;
		int i = 0;
		for (; i < mouseOnViewChain.size(); i++) {
			View view = mouseOnViewChain.get(i);
			p = view.transformPointFromSuperView(p);
			if (!view.isInside(p)) break;
			view.firePreMouseClicked(e);
			if (e.handled) return;
		}
		for (i--; i >= 0; i--) {
			mouseOnViewChain.get(i).fireMouseClicked(e);
			if (e.handled) return;
		}
	}
	
	private void fireMouseMoved(co.gongzh.snail.MouseEvent e) {
		for (View view : mouseOnViewChain) {
			view.firePreMouseMoved(e);
			if (e.handled) return;
		}
		for (int i = mouseOnViewChain.size() - 1; i >= 0; i--) {
			mouseOnViewChain.get(i).fireMouseMoved(e);
			if (e.handled) return;
		}
	}

	private void fireMouseDragged(co.gongzh.snail.MouseEvent e) {
		for (View view : mouseOnViewChain) {
			view.firePreMouseDragged(e);
			if (e.handled) return;
		}
		for (int i = mouseOnViewChain.size() - 1; i >= 0; i--) {
			mouseOnViewChain.get(i).fireMouseDragged(e);
			if (e.handled) return;
		}
	}

	private void fireMouseWheelMoved(co.gongzh.snail.MouseWheelEvent e) {
		for (View view : mouseOnViewChain) {
			view.firePreMouseWheelMoved(e);
			if (e.handled) return;
		}
		for (int i = mouseOnViewChain.size() - 1; i >= 0; i--) {
			mouseOnViewChain.get(i).fireMouseWheelMoved(e);
			if (e.handled) return;
		}
	}
	
	//// Mouse Velocity ////
	
	private Vector2D currentMousePosition = null, lastMousePosition = null;
	private Vector2D cachedMouseVelocity = null;
	private long mouseTimer = 0;
	private int pressedMouseButton = java.awt.event.MouseEvent.NOBUTTON;
	
	private Vector2D getVelocity() {
		return cachedMouseVelocity == null ? Vector2D.make() : cachedMouseVelocity.clone();
	}
	
	private void update(long dt) {
		mouseTimer += dt;
		if (mouseTimer >= 40) {
			if (lastMousePosition != null) {
				cachedMouseVelocity = Vector2D.subtract(currentMousePosition, lastMousePosition);
				cachedMouseVelocity.multiplied(1000.0f / mouseTimer);
			}
			lastMousePosition = currentMousePosition;
			mouseTimer = 0;
		}
	}
	
}
