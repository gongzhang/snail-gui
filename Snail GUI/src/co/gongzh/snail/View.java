package co.gongzh.snail;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import co.gongzh.snail.event.EventHandler;
import co.gongzh.snail.event.HandlerBundle;
import co.gongzh.snail.event.Key;
import co.gongzh.snail.util.Insets;
import co.gongzh.snail.util.Rectangle;
import co.gongzh.snail.util.Vector2D;

public class View implements Iterable<View> {
	
	static long debug_view_alloc_count = 0;
	
	private static final Color CLEAR_COLOR = new Color(0x00ffffff, true);
	private static final AlphaComposite SRC_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC);
	private static final AlphaComposite SRC_OVER_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	
	// structure
	private View superView;
	private final List<View> subviews;
	private ViewContext viewContext;
	
	// position and size
	private int left, top, width, height;
	
	// paint
	private boolean needsRepaint;
	private Color backgroundColor;
	VolatileImage buffer;
	private boolean clipped;
	private boolean hidden;
	private float alpha;
	private PaintMode paintMode;
	private AffineTransform transform;
	private AffineTransform inversedTransform;
	
	// template features
	private HandlerBundle handlerBundle;
	private int tag;
	
	public View() {
		debug_view_alloc_count++;
		
		// structure
		superView = null;
		subviews = new ArrayList<View>(0);
		viewContext = null;
		
		// position and size
		left = 0;
		top = 0;
		width = 0;
		height = 0;
		
		// paint
		needsRepaint = true;
		backgroundColor = Color.WHITE;
		clipped = false;
		hidden = false;
		alpha = 1.0f;
		paintMode = PaintMode.BUFFERED;
		transform = inversedTransform = null;
		
		// template
		handlerBundle = null;
		tag = 0;
	}
	
	public View(int left, int top, int width, int height) {
		this();
		setPosition(left, top);
		setSize(width, height);
	}
	
	public final ViewContext getViewContext() {
		return viewContext;
	}

	static final void setViewContext(View view, ViewContext viewContext) {
		view.viewContext = viewContext;
		if (viewContext != null) viewContext.invalid();
		for (View child : view) {
			setViewContext(child, viewContext);
		}
	}

	//// Structure-Related Methods ////
	
	public final View getSuperView() {
		return superView;
	}
	
	@SuppressWarnings("unchecked")
	public final <E extends View> E getSuperViewInHierarchy(Class<E> clazz) {
		View[] views = getViewHierarchy();
		for (int i = views.length - 2; i >= 0; i--) {
			if (clazz.isInstance(views[i])) {
				return (E) views[i];
			}
		}
		return null;
	}

	public final View[] getSubviews() {
		return subviews.toArray(new View[0]);
	}
	
	public final <T> T[] getSubviews(T[] array) {
		return subviews.toArray(array);
	}
	
	private class ViewIterator implements ListIterator<View> {
		
		private final ListIterator<View> it;
		
		public ViewIterator(ListIterator<View> it) {
			this.it = it;
		}

		@Override
		public void add(View e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public boolean hasPrevious() {
			return it.hasPrevious();
		}

		@Override
		public View next() {
			return it.next();
		}

		@Override
		public int nextIndex() {
			return it.nextIndex();
		}

		@Override
		public View previous() {
			return it.previous();
		}

		@Override
		public int previousIndex() {
			return it.previousIndex();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(View e) {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public final Iterator<View> iterator() {
		return listIterator();
	}
	
	public final ListIterator<View> listIterator() {
		return new ViewIterator(subviews.listIterator());
	}
	
	public final ListIterator<View> listIterator(int index) {
		return new ViewIterator(subviews.listIterator(index));
	}
	
	public final View getSubview(int index) {
		return subviews.get(index);
	}
	
	public final View addSubview(View v) {
		return addSubview(v, subviews.size());
	}

	public View addSubview(View v, int index) {
		if (v.superView != null) {
			throw new IllegalArgumentException("the view already has a super view.");
		} else if (index < 0 || index > subviews.size()) {
			throw new IllegalArgumentException("invalid view index");
		}
		v.superView = this;
		View.setViewContext(v, this.viewContext);
		subviews.add(index, v);
		if (viewContext != null) viewContext.invalid();
		v.setNeedsRepaint();
		fireSubviewAdded(v);
		return this;
	}

	public void removeFromSuperView() {
		if (superView != null) {
			turnBackBuffer(this);
			if (viewContext != null && viewContext.isInFocusChain(this)) {
				viewContext.requestFocus(null);
			}
			superView.subviews.remove(this);
			if (viewContext != null && viewContext.isInMouseOnViewChain(this)) {
				viewContext.updateMouseOnViewChain(null);
			}
			superView.fireSubviewRemoved(this);
			if (superView.viewContext != null) superView.viewContext.invalid();
			superView = null;
			setViewContext(this, null);
		} else {
			throw new IllegalStateException("the view does not have a super view.");
		}
	}
	
	private void turnBackBuffer(View view) {
		if (view.buffer != null) {
			viewContext.bufferPool.turnBackBuffer(view.buffer);
			view.buffer = null;
		}
		for (View v : view) {
			turnBackBuffer(v);
		}
	}
	
	public final void removeAllSubviews() {
		while (count() > 0) {
			getSubview(0).removeFromSuperView();
		}
	}

	public final View[] getViewHierarchy() {
		LinkedList<View> stack = new LinkedList<View>();
		View view = this;
		while (view != null) {
			stack.addFirst(view);
			view = view.superView;
		}
		return stack.toArray(new View[stack.size()]);
	}

	public final int getSubviewIndex(View view) {
		return subviews.indexOf(view);
	}
	
	public void setSubviewIndex(View view, int targetIndex) {
		if (view.getIndex() != targetIndex) {
			subviews.remove(view);
			subviews.add(targetIndex, view);
			if (viewContext != null) viewContext.invalid();
			fireEvent(SUBVIEW_INDEX_CHANGED, view);
		}
	}
	
	public final void setSubviewIndex(int index, int targetIndex) {
		setSubviewIndex(subviews.get(index), targetIndex);
	}
	
	public final int getIndex() {
		return superView.getSubviewIndex(this);
	}
	
	public final void setIndex(int index) {
		superView.setSubviewIndex(this, index);
	}
	
	public final int count() {
		return subviews.size();
	}
	
	public final int getTag() {
		return tag;
	}
	
	public final void setTag(int tag) {
		this.tag = tag;
	}
	
	public final View getTaggedSubview(int tag) {
		return getTaggedSubview(this, tag);
	}
	
	private View getTaggedDirectSubview(int tag) {
		for (View view : this) {
			if (view.tag == tag)
			return view;
		}
		return null;
	}
	
	private static View getTaggedSubview(View target, int tag) {
		View rst = target.getTaggedDirectSubview(tag);
		if (rst != null) return rst;
		else {
			for (View view : target) {
				View result = getTaggedSubview(view, tag);
				if (result != null) return result;
			}
			return null;
		}
	}
	
	//// Position & Size ////
	
	public final int getLeft() {
		return left;
	}
	
	public final int getTop() {
		return top;
	}
	
	public final int getWidth() {
		return width;
	}
	
	public final int getHeight() {
		return height;
	}
	
	public final int getRight() {
		return superView.width - left - width;
	}
	
	public final int getBottom() {
		return superView.height - top - height;
	}
	
	
	public final void setLeft(int left) {
		setPosition(left, top);
	}
	
	public final void setTop(int top) {
		setPosition(left, top);
	}
	
	public final void setWidth(int width) {
		setSize(width, height);
	}
	
	public final void setHeight(int height) {
		setSize(width, height);
	}
	
	public void setPosition(int left, int top) {
		if (this.left != left || this.top != top) {
			this.left = left;
			this.top = top;
			if (viewContext != null) viewContext.invalid();
			fireEvent(POSITION_CHANGED, getPosition());
		}
	}
	
	public final void setPosition(Vector2D pos) {
		setPosition(pos.x, pos.y);
	}
	
	public final Vector2D getPosition() {
		return Vector2D.make(left, top);
	}
	
	public final Vector2D getPositionInRootView() {
		if (superView == null) return getPosition();
		else return superView.transformPointToRootView(getPosition());
	}
	
	public void setSize(int width, int height) {
		if (width != this.width || height != this.height) {
			this.width = width;
			this.height = height;
			setNeedsRepaint();
			fireEvent(SIZE_CHANGED, getSize());
			if (!isHidden()) layout();
		}
	}
	
	public final void setSize(Vector2D size) {
		setSize(size.x, size.y);
	}
	
	public final Vector2D getSize() {
		return Vector2D.make(width, height);
	}
	
	public final Rectangle getFrame() {
		return Rectangle.make(left, top, width, height);
	}
	
	public final void setFrame(int left, int top, int width, int height) {
		setPosition(left, top);
		setSize(width, height);
	}
	
	public final void setFrame(Rectangle rect) {
		setPosition(rect.origin);
		setSize(rect.size);
	}
	
	public int getPreferredWidth() {
		throw new UnsupportedOperationException();
	}
	
	public int getPreferredHeight() {
		throw new UnsupportedOperationException();
	}
	
	public final Vector2D getPreferredSize() {
		return Vector2D.make(getPreferredWidth(), getPreferredHeight());
	}
	
	//// Static Layout Methods ////
	
	public static void putViewWithLeft(View view, int left) {
		view.setLeft(left);
	}
	
	public static void putViewWithLeft(View view, View ref) {
		view.setLeft(ref.left);
	}

	public static void putViewWithTop(View view, int top) {
		view.setTop(top);
	}
	
	public static void putViewWithTop(View view, View ref) {
		view.setTop(ref.top);
	}

	public static void putViewWithRight(View view, int right) {
		view.setLeft(view.superView.width - right - view.width);
	}
	
	public static void putViewWithRight(View view, View ref) {
		view.setLeft(view.superView.width - (ref.left + ref.width) - view.width);
	}

	public static void putViewWithBottom(View view, int bottom) {
		view.setTop(view.superView.height - bottom - view.height);
	}
	
	public static void putViewWithBottom(View view, View ref) {
		view.setTop(view.superView.height - (ref.top + ref.height) - view.height);
	}

	public static void putViewWithHorizontalCenter(View view, int hc) {
		view.setLeft(hc - view.width / 2);
	}
	
	public static void putViewWithHorizontalCenter(View view, View ref) {
		view.setLeft(ref.left + ref.width / 2 - view.width / 2);
	}

	public static void putViewWithVerticalCenter(View view, int vc) {
		view.setTop(vc - view.height / 2);
	}
	
	public static void putViewWithVerticalCenter(View view, View ref) {
		view.setTop(ref.top + ref.height / 2 - view.height / 2);
	}
	
	public static void putViewWithLeftAndTop(View view, int left, int top) {
		view.setPosition(left, top);
	}
	
	public static void putViewWithLeftAndBottom(View view, int left, int bottom) {
		view.setPosition(left, view.superView.height - bottom - view.height);
	}
	
	public static void putViewWithLeftAndVerticalCenter(View view, int left, int vc) {
		view.setPosition(left, vc - view.height / 2);
	}

	public static void putViewWithRightAndTop(View view, int right, int top) {
		view.setPosition(view.superView.width - right - view.width, top);
	}

	public static void putViewWithRightAndBottom(View view, int right, int bottom) {
		view.setPosition(view.superView.width - right - view.width, view.superView.height - bottom - view.height);
	}
	
	public static void putViewWithRightAndVerticalCenter(View view, int right, int vc) {
		view.setPosition(view.superView.width - right - view.width, vc - view.height / 2);
	}
	
	public static void putViewWithHorizontalCenterAndTop(View view, int hc, int top) {
		view.setPosition(hc - view.width / 2, top);
	}

	public static void putViewWithHorizontalCenterAndBottom(View view, int hc, int bottom) {
		view.setPosition(hc - view.width / 2, view.superView.height - bottom - view.height);
	}
	
	public static void putViewWithHorizontalCenterAndVerticalCenter(View view, int hc, int vc) {
		view.setPosition(hc - view.width / 2, vc - view.height / 2);
	}
	
	public static void putViewAtLeftSideOfView(View view, View ref, int margin) {
		view.setLeft(ref.left - margin - view.width);
	}
	
	public static void putViewAtRightSideOfView(View view, View ref, int margin) {
		view.setLeft(ref.left + ref.width + margin);
	}
	
	public static void putViewAtTopSideOfView(View view, View ref, int margin) {
		view.setTop(ref.top - margin - view.height);
	}
	
	public static void putViewAtBottomSideOfView(View view, View ref, int margin) {
		view.setTop(ref.top + ref.height + margin);
	}
	
	public static void putViewAtCenterOfView(View view, View ref) {
		putViewWithHorizontalCenterAndVerticalCenter(view, ref.left + ref.width / 2, ref.top + ref.height / 2);
	}
	
	public static void putViewAtCenterOfSuperView(View view) {
		putViewWithHorizontalCenterAndVerticalCenter(view, view.superView.width / 2, view.superView.height / 2);
	}
	
	public static void scaleViewWithLeft(View view, int left) {
		int delta = view.left - left;
		view.setLeft(left);
		view.setWidth(view.width + delta);
	}
	
	public static void scaleViewWithLeft(View view, View ref) {
		int delta = view.left - ref.left;
		view.setLeft(ref.left);
		view.setWidth(view.width + delta);
	}
	
	public static void scaleViewWithRight(View view, int right) {
		view.setWidth(view.superView.width - right - view.left);
	}
	
	public static void scaleViewWithRight(View view, View ref) {
		view.setWidth(view.superView.width - (ref.superView.width - ref.left - ref.width) - view.left);
	}
	
	public static void scaleViewWithTop(View view, int top) {
		int delta = view.top - top;
		view.setTop(top);
		view.setHeight(view.height + delta);
	}
	
	public static void scaleViewWithTop(View view, View ref) {
		int delta = view.top - ref.top;
		view.setTop(ref.top);
		view.setHeight(view.height + delta);
	}
	
	public static void scaleViewWithBottom(View view, int bottom) {
		view.setHeight(view.superView.height - bottom - view.top);
	}
	
	public static void scaleViewWithBottom(View view, View ref) {
		view.setHeight(view.superView.height - (ref.superView.height - ref.top - ref.height) - view.top);
	}
	
	public static void scaleViewWithLeftAndTop(View view, int left, int top) {
		scaleViewWithLeft(view, left);
		scaleViewWithTop(view, top);
	}
	
	public static void scaleViewWithLeftAndBottom(View view, int left, int bottom) {
		scaleViewWithLeft(view, left);
		scaleViewWithBottom(view, bottom);
	}
	
	public static void scaleViewWithRightAndTop(View view, int right, int top) {
		scaleViewWithRight(view, right);
		scaleViewWithTop(view, top);
	}
	
	public static void scaleViewWithRightAndBottom(View view, int right, int bottom) {
		scaleViewWithRight(view, right);
		scaleViewWithBottom(view, bottom);
	}
	
	public static void scaleViewWithLeftAndRight(View view, int left, int right) {
		view.setLeft(left);
		scaleViewWithRight(view, right);
	}
	
	public static void scaleViewWithTopAndBottom(View view, int top, int bottom) {
		view.setTop(top);
		scaleViewWithBottom(view, bottom);
	}
	
	public static void scaleViewWithMarginToSuperView(View view, int margin) {
		view.setPosition(margin, margin);
		scaleViewWithRightAndBottom(view, margin, margin);
	}
	
	public static void scaleViewWithInsetsToSuperView(View view, Insets insets) {
		view.setPosition(insets.left, insets.top);
		scaleViewWithRightAndBottom(view, insets.right, insets.bottom);
	}
	
	//// Core Painting Methods ////
	
	private void createBuffer() {
		if (buffer != null) {
			viewContext.bufferPool.turnBackBuffer(buffer);
		}
		buffer = viewContext.bufferPool.getBuffer(width, height);
	}
	
	private void createBiggerBuffer() {
		if (buffer != null) {
			viewContext.bufferPool.turnBackBuffer(buffer);
		}
		buffer = viewContext.bufferPool.getBuffer(width + (width >> 3), height + (height >> 3));
	}
	
	protected final ViewGraphics getTemporaryGraphicsContext() {
		if (viewContext != null) return viewContext.getTemporaryGraphicsContext();
		else return ViewContext.getSharedTemporaryGraphicsContext();
	}
	
	private void repaintBuffer(GraphicsConfiguration gc, boolean customTarget) {
		do {
			if (buffer.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
				createBuffer();
			}
			ViewGraphics g = new ViewGraphics(buffer.createGraphics());
			
			// clear current content
			g.setComposite(SRC_COMPOSITE);
			g.setColor(backgroundColor == null ? CLEAR_COLOR : backgroundColor);
			g.fillRect(0, 0, width, height);
			g.setComposite(SRC_OVER_COMPOSITE);
			
			// repaint
			fireEvent(PRE_REPAINT, g);
			repaintView(g);
			fireEvent(REPAINT, g);

			g.dispose();
		} while (buffer.contentsLost());
		if (!customTarget) needsRepaint = false;
	}
	
	final void repaint(ViewGraphics g, boolean customTarget) {
		if (hidden) return;
		
		final boolean no_size = width <= 0 || height <= 0;		
		// translate
		g.translate(left, top);
		
		// transform
		if (transform != null) g.transform(transform);
		
		// clip
		Shape clipShape = null;
		if (clipped && !no_size) {
			clipShape = g.getClip();
			g.clipRect(0, 0, width, height);
		}
		
		// alpha
		final float old_alpha = g.getAlpha();
		g.setAlpha(old_alpha * alpha);

		// repaint self
		if (!no_size) {
			if (customTarget || paintMode == PaintMode.DIRECTLY) {
				
				// DIRECTLY MODE
				if (!clipped) {
					clipShape = g.getClip();
					g.clipRect(0, 0, width, height);
				}
				if (backgroundColor != null) {
					g.setColor(backgroundColor);
					g.fillRect(0, 0, width, height);
				}
				fireEvent(PRE_REPAINT, g);
				repaintView(g);
				fireEvent(REPAINT, g);
				if (!clipped) {
					g.setClip(clipShape);
					clipShape = null;
				}
			} else if (paintMode == PaintMode.BUFFERED) {
				
				// BUFFERED MODE
				if (buffer == null) createBuffer();
				do {
					int returnCode = buffer.validate(g.getDeviceConfiguration());
					if (returnCode == VolatileImage.IMAGE_RESTORED ||
						returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
						// contents need to be restored/recreated
						repaintBuffer(g.getDeviceConfiguration(), customTarget); // restored/recreate contents
					} else if (needsRepaint) {
						// check buffer size
						if (buffer.getWidth() < width || buffer.getHeight() < height) createBiggerBuffer();
						repaintBuffer(g.getDeviceConfiguration(), customTarget);
					}
					
					// on screen
					g.drawImage(buffer, 0, 0, width, height, 0, 0, width, height, null);
				} while (buffer.contentsLost());
			}
		}
		
		if (subviews.size() > 0 && (!clipped || !no_size)) {
			// repaint children
			for (int i = 0; i < subviews.size(); i++) {
				subviews.get(i).repaint(g, customTarget);
			}
		}
		
		// restore
		g.setAlpha(old_alpha);
		if (clipShape != null) {
			g.setClip(clipShape);
		}
		if (inversedTransform != null) g.transform(inversedTransform);
		g.translate(-left, -top);
	}
	
	public final void paintOnTarget(ViewGraphics g) {
		repaint(g, true);
	}
	
	//// Painting-Related Methods ////
	
	public final void ensureBufferSize(int width, int height) {
		width = Math.max(getWidth(), width);
		height = Math.max(getHeight(), height);
		if (buffer != null) {
			if (buffer.getWidth() < width || buffer.getHeight() < height) {
				viewContext.bufferPool.turnBackBuffer(buffer);
				buffer = viewContext.bufferPool.getBuffer(width, height);
				setNeedsRepaint();
			} else {
				return;
			}
		}
	}

	public final void trimBufferSize() {
		createBuffer();
		setNeedsRepaint();
	}

	public final void setNeedsRepaint() {
		needsRepaint = true;
		if (viewContext != null) viewContext.invalid();
	}

	public final Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		if (this.backgroundColor != backgroundColor) {
			this.backgroundColor = backgroundColor;
			setNeedsRepaint();
			fireEvent(BACKGROUND_COLOR_CHANGED, backgroundColor);
		}
	}

	public final boolean isClipped() {
		return clipped;
	}

	public void setClipped(boolean clipped) {
		this.clipped = clipped;
		if (viewContext != null) viewContext.invalid();
	}

	public final boolean isHidden() {
		return hidden;
	}
	
	public void setHidden(boolean hidden) {
		if (this.hidden != hidden) {
			this.hidden = hidden;
			if (hidden && viewContext != null && viewContext.isInFocusChain(this)) {
				viewContext.requestFocus(null);
			}
			if (viewContext != null) viewContext.invalid();
			fireEvent(HIDDEN_CHANGED, null);
			if (!hidden) layout();
		}
	}
	
	public final float getAlpha() {
		return alpha;
	}
	
	public void setAlpha(float alpha) {
		if (this.alpha != alpha) {
			this.alpha = alpha;
			if (viewContext != null) viewContext.invalid();
			fireEvent(ALPHA_CHANGED, alpha);
		}
	}
	
	public PaintMode getPaintMode() {
		return paintMode;
	}

	public void setPaintMode(PaintMode paintMode) {
		this.paintMode = paintMode;
		if (paintMode != PaintMode.BUFFERED && buffer != null) {
			viewContext.bufferPool.turnBackBuffer(buffer);
			buffer = null;
		}
		if (viewContext != null) viewContext.invalid();
	}
	
	public final AffineTransform getTransform() {
		return transform == null ? null : (AffineTransform) transform.clone();
	}
	
	public void setTransform(AffineTransform transform) {
		if (transform == null) {
			this.transform = null;
			this.inversedTransform = null;
		} else {
			this.transform = (AffineTransform) transform.clone();
			try {
				this.inversedTransform = this.transform.createInverse();
			} catch (NoninvertibleTransformException e) {
				throw new IllegalArgumentException(e);
			}
		}
		if (viewContext != null) viewContext.invalid();
		fireEvent(TRANSFORM_CHANGED, null);
	}
	
	//// Mouse Behavior ////
	
	public final Vector2D transformPointFromSuperView(Vector2D point) {
		point = Vector2D.make(point.x - left, point.y - top);
		if (inversedTransform != null) {
			point.set(inversedTransform.transform(point.toPoint2D(), null));
		}
		return point;
	}
	
	public final Vector2D transformPointToSuperView(Vector2D point) {
		point = point.clone();
		if (transform != null) {
			point.set(transform.transform(point.toPoint2D(), null));
		}
		point.x += left;
		point.y += top;
		return point;
	}
	
	public final Vector2D transformPointFromRootView(Vector2D point) {
		Vector2D rst = Vector2D.make(point);
		View[] hierarchy = getViewHierarchy();
		for (View v : hierarchy) {
			rst = v.transformPointFromSuperView(rst);
		}
		return rst;
	}
	
	public final Vector2D transformPointToRootView(Vector2D point) {
		Vector2D rst = Vector2D.make(point);
		View[] hierarchy = getViewHierarchy();
		for (int i = hierarchy.length - 1; i >= 0; i--) {
			View v = hierarchy[i];
			rst = v.transformPointToSuperView(rst);
		}
		return rst;
	}
	
	public static final Vector2D transformPoint(Vector2D point, View fromView, View toView) {
		return toView.transformPointFromRootView(fromView.transformPointToRootView(point));
	}
	
	public boolean isInside(Vector2D point) {
		return point.x >= 0 && point.x < width && point.y >= 0 && point.y < height;
	}
	
	public final View[] getSubviewHierachyAtPoint(Vector2D point) {
		List<View> list = new ArrayList<View>();
		getViewHierachyAtPoint(this, point, list);
		return list.toArray(new View[list.size()]);
	}
	
	static final void getViewHierachyAtPoint(View view, Vector2D p, List<View> dstList) {
		ListIterator<View> it = view.listIterator(view.count());
		while (it.hasPrevious()) {
			View child = it.previous();
			if (!child.isHidden()) { 
				Vector2D p1 = child.transformPointFromSuperView(p);
				if (child.isInside(p1)) {
					dstList.add(child);
					getViewHierachyAtPoint(child, p1, dstList);
					return;
				}
			}
		}
	}
	
	//// Keyboard Focus ////
	
	public boolean isKeyboardFocus() {
		return viewContext != null && viewContext.isFocus(this);
	}
	
	public boolean isInKeyboardFocusHierarchy() {
		return viewContext != null && viewContext.isInFocusChain(this);
	}

	public void requestKeyboardFocus() {
		if (viewContext != null && !hidden && !isKeyboardFocus()) {
			viewContext.requestFocus(this);
		}
	}

	public void resignKeyboardFocus() {
		if (isKeyboardFocus()) viewContext.requestFocus(null);
	}
	
	//// Key Definition & Event Feature ////
	
	public static final Key ALPHA_CHANGED = new Key("alphaChanged", View.class, Float.class);
	public static final Key BACKGROUND_COLOR_CHANGED = new Key("backgroundColorChanged", View.class, Color.class);
	public static final Key HIDDEN_CHANGED = new Key("hiddenChanged", View.class, null);
	public static final Key POSITION_CHANGED = new Key("positionChanged", View.class, Vector2D.class);
	public static final Key SIZE_CHANGED = new Key("sizeChanged", View.class, Vector2D.class);
	public static final Key TRANSFORM_CHANGED = new Key("transformChanged", View.class, null);
	public static final Key PRE_REPAINT = new Key("preRepaint", View.class, ViewGraphics.class);
	public static final Key REPAINT = new Key("repaint", View.class, ViewGraphics.class);
	public static final Key SUBVIEW_ADDED = new Key("subviewAdded", View.class, View.class);
	public static final Key SUBVIEW_REMOVED = new Key("subviewRemoved", View.class, View.class);
	public static final Key SUBVIEW_INDEX_CHANGED = new Key("subviewIndexChanged", View.class, View.class);
	public static final Key PRE_LAYOUT = new Key("preLayout", View.class, null);
	public static final Key LAYOUT = new Key("layout", View.class, null);
	
	public static final Key MOUSE_ENTERED = new Key("mouseEntered", View.class, null);
	public static final Key MOUSE_EXITED = new Key("mouseExited", View.class, null);
	public static final Key PRE_KEY_PRESSED = new Key("preKeyPressed", View.class, KeyEvent.class);
	public static final Key PRE_KEY_RELEASED = new Key("preKeyReleased", View.class, KeyEvent.class);
	public static final Key PRE_KEY_TYPED = new Key("preKeyTyped", View.class, KeyEvent.class);
	public static final Key PRE_MOUSE_PRESSED = new Key("preMousePressed", View.class, MouseEvent.class);
	public static final Key PRE_MOUSE_RELEASED = new Key("preMouseReleased", View.class, MouseEvent.class);
	public static final Key PRE_MOUSE_CLICKED = new Key("preMouseClicked", View.class, MouseEvent.class);
	public static final Key PRE_MOUSE_MOVED = new Key("preMouseMoved", View.class, MouseEvent.class);
	public static final Key PRE_MOUSE_DRAGGED = new Key("preMouseDragged", View.class, MouseEvent.class);
	public static final Key PRE_MOUSE_WHEEL_MOVED = new Key("preMouseWheelMoved", View.class, MouseWheelEvent.class);
	
	public static final Key GOT_KEYBOARD_FOCUS = new Key("gotKeyboardFocus", View.class, null);
	public static final Key LOST_KEYBOARD_FOCUS = new Key("lostKeyboardFocus", View.class, null);
	public static final Key KEY_PRESSED = new Key("keyPressed", View.class, KeyEvent.class);
	public static final Key KEY_RELEASED = new Key("keyReleased", View.class, KeyEvent.class);
	public static final Key KEY_TYPED = new Key("keyTyped", View.class, KeyEvent.class);
	public static final Key MOUSE_PRESSED = new Key("mousePressed", View.class, MouseEvent.class);
	public static final Key MOUSE_RELEASED = new Key("mouseReleased", View.class, MouseEvent.class);
	public static final Key MOUSE_CLICKED = new Key("mouseClicked", View.class, MouseEvent.class);
	public static final Key MOUSE_MOVED = new Key("mouseMoved", View.class, MouseEvent.class);
	public static final Key MOUSE_DRAGGED = new Key("mouseDragged", View.class, MouseEvent.class);
	public static final Key MOUSE_WHEEL_MOVED = new Key("mouseWheelMoved", View.class, MouseWheelEvent.class);

	
	public final boolean addEventHandler(Key key, EventHandler handler) {
		if (handlerBundle == null) {
			handlerBundle = new HandlerBundle();
		}
		return handlerBundle.addHandler(key, handler);
	}

	public final boolean removeEventHandler(Key key, EventHandler handler) {
		if (handlerBundle != null) {
			return handlerBundle.removeHandler(key, handler);
		} else {
			return false;
		}
	}

	public final void fireEvent(Key key, Object arg) {
		if (handlerBundle != null) handlerBundle.fireEvent(this, key, arg);
	}
	
	//// Rendering & Painting Handlers ////

	protected void repaintView(ViewGraphics g) {}
	
	//// Layout ////
	
	public final void layout() { fireEvent(PRE_LAYOUT, null); layoutView(); fireEvent(LAYOUT, null); }
	protected void layoutView() {}
	
	//// Structure Changing Handlers ////

	final void fireSubviewAdded(View subview) { subviewAdded(subview); fireEvent(SUBVIEW_ADDED, subview); }
	final void fireSubviewRemoved(View subview) { subviewRemoved(subview); fireEvent(SUBVIEW_REMOVED, subview); }
	
	protected void subviewAdded(View subview) {}
	protected void subviewRemoved(View subview) {}
	
	//// Keyboard Event Handlers ////

	final void fireGotKeyboardFocus() { gotKeyboardFocus(); fireEvent(GOT_KEYBOARD_FOCUS, null); }
	final void fireLostKeyboardFocus() { lostKeyboardFocus(); fireEvent(LOST_KEYBOARD_FOCUS, null); }
	
	protected void gotKeyboardFocus() {}
	protected void lostKeyboardFocus() {}
	
	final void firePreKeyTyped(KeyEvent e) { preKeyTyped(e); fireEvent(PRE_KEY_TYPED, e); }
	final void firePreKeyPressed(KeyEvent e) { preKeyPressed(e); fireEvent(PRE_KEY_PRESSED, e); }
	final void firePreKeyReleased(KeyEvent e) { preKeyReleased(e); fireEvent(PRE_KEY_RELEASED, e); }
	
	final void fireKeyTyped(KeyEvent e) { keyTyped(e); fireEvent(KEY_TYPED, e); }
	final void fireKeyPressed(KeyEvent e) { keyPressed(e); fireEvent(KEY_PRESSED, e); }
	final void fireKeyReleased(KeyEvent e) { keyReleased(e); fireEvent(KEY_RELEASED, e); }
	
	protected void preKeyTyped(KeyEvent e) {}
	protected void preKeyPressed(KeyEvent e) {}
	protected void preKeyReleased(KeyEvent e) {}
	
	protected void keyTyped(KeyEvent e) {}
	protected void keyPressed(KeyEvent e) {}
	protected void keyReleased(KeyEvent e) {}
	
	//// Mouse Event Handlers ////
	
	final void fireMouseEntered() { mouseEntered(); fireEvent(MOUSE_ENTERED, null); }
	final void fireMouseExited() { mouseExited(); fireEvent(MOUSE_EXITED, null); }
	
	protected void mouseEntered() {}
	protected void mouseExited() {}
	
	final void firePreMousePressed(MouseEvent e) { preMousePressed(e); fireEvent(PRE_MOUSE_PRESSED, e); }
	final void firePreMouseReleased(MouseEvent e) { preMouseReleased(e); fireEvent(PRE_MOUSE_RELEASED, e); }
	final void firePreMouseClicked(MouseEvent e) { preMouseClicked(e); fireEvent(PRE_MOUSE_CLICKED, e);	}
	final void firePreMouseMoved(MouseEvent e) { preMouseMoved(e); fireEvent(PRE_MOUSE_MOVED, e);	}
	final void firePreMouseDragged(MouseEvent e) { preMouseDragged(e); fireEvent(PRE_MOUSE_DRAGGED, e);	}
	final void firePreMouseWheelMoved(MouseWheelEvent e) { preMouseWheelMoved(e); fireEvent(PRE_MOUSE_WHEEL_MOVED, e);	}

	final void fireMousePressed(MouseEvent e) { mousePressed(e); fireEvent(MOUSE_PRESSED, e); }
	final void fireMouseReleased(MouseEvent e) { mouseReleased(e); fireEvent(MOUSE_RELEASED, e); }
	final void fireMouseClicked(MouseEvent e) { mouseClicked(e); fireEvent(MOUSE_CLICKED, e); }
	final void fireMouseMoved(MouseEvent e) { mouseMoved(e); fireEvent(MOUSE_MOVED, e); }
	final void fireMouseDragged(MouseEvent e) { mouseDragged(e); fireEvent(MOUSE_DRAGGED, e); }
	final void fireMouseWheelMoved(MouseWheelEvent e) { mouseWheelMoved(e); fireEvent(MOUSE_WHEEL_MOVED, e); }
	
	protected void preMousePressed(MouseEvent e) {}
	protected void preMouseReleased(MouseEvent e) {}
	protected void preMouseClicked(MouseEvent e) {}
	protected void preMouseMoved(MouseEvent e) {}
	protected void preMouseDragged(MouseEvent e) {}
	protected void preMouseWheelMoved(MouseWheelEvent e) {}

	protected void mousePressed(MouseEvent e) {}
	protected void mouseReleased(MouseEvent e) {}
	protected void mouseClicked(MouseEvent e) {}
	protected void mouseMoved(MouseEvent e) {}
	protected void mouseDragged(MouseEvent e) {}
	protected void mouseWheelMoved(MouseWheelEvent e) {}
	
}

