package co.gongzh.snail.layer;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import co.gongzh.snail.MouseEvent;
import co.gongzh.snail.PaintMode;
import co.gongzh.snail.View;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.event.EventHandler;
import co.gongzh.snail.event.Key;
import co.gongzh.snail.util.Vector2D;

public class LayeredView extends View {
	
	private final List<Layer> layers;
	private AffineTransform layerTransform;
	AffineTransform inversedLayerTransform;
	private boolean antialiased;
	private final EventHandler mouseEventHandler = new EventHandler() {
		@Override
		public void handle(View sender, Key eventName, Object arg) {
			if (eventName.equals(View.MOUSE_PRESSED)) {
				layeredViewMousePressed((MouseEvent) arg);
			} else if (eventName.equals(View.MOUSE_RELEASED)) {
				layeredViewMouseReleased((MouseEvent) arg);
			} else if (eventName.equals(View.MOUSE_MOVED)) {
				layeredViewMouseMoved((MouseEvent) arg);
			} else if (eventName.equals(View.MOUSE_DRAGGED)) {
				layeredViewMouseDragged((MouseEvent) arg);
			} else if (eventName.equals(View.MOUSE_EXITED)) {
				layeredViewMouseExited();
			}
		}
	};
	
	public LayeredView() {
		antialiased = true;
		layers = new ArrayList<Layer>();
		layerTransform = null;
		inversedLayerTransform = null;
		setPaintMode(PaintMode.DIRECTLY);
		setBackgroundColor(null);
		addEventHandler(View.REPAINT, new EventHandler() {
			@Override
			public void handle(View sender, Key eventName, Object arg) {
				repaintLayeredView((ViewGraphics) arg);
			}
		});
		addEventHandler(MOUSE_PRESSED, mouseEventHandler);
		addEventHandler(MOUSE_RELEASED, mouseEventHandler);
		addEventHandler(MOUSE_MOVED, mouseEventHandler);
		addEventHandler(MOUSE_DRAGGED, mouseEventHandler);
		addEventHandler(MOUSE_EXITED, mouseEventHandler);
	}
	
	public void addLayer(Layer layer) {
		layer.setSuperView(this);
		layers.add(layer);
		setNeedsRepaint();
	}
	
	public void addLayer(Layer layer, int index) {
		layer.setSuperView(this);
		layers.add(index, layer);
		setNeedsRepaint();
	}
	
	public void removeLayer(Layer layer) {
		if (!layers.remove(layer)) throw new IllegalArgumentException();
		if (mouseFocus == layer) {
			setMouseFocus(null);
		}
		layer.setSuperView(null);
		setNeedsRepaint();
	}
	
	public final void removeLayer(int index) {
		Layer layer = layers.get(index);
		removeLayer(layer);
	}
	
	public final void removeAllLayers() {
		for (Layer layer : layers) {
			layer.setSuperView(null);
		}
		layers.clear();
		setMouseFocus(null);
		setNeedsRepaint();
	}
	
	public final Layer getLayer(int index) {
		return layers.get(index);
	}
	
	public final Layer[] getLayers() {
		return layers.toArray(new Layer[layers.size()]);
	}
	
	public final AffineTransform getLayerTransform() {
		return (AffineTransform) layerTransform.clone();
	}
	
	public void setLayerTransform(AffineTransform layerTransform) {
		this.layerTransform = (AffineTransform) layerTransform.clone();
		try {
			this.inversedLayerTransform = layerTransform.createInverse();
		} catch (NoninvertibleTransformException e) {
			throw new IllegalArgumentException();
		}
		setNeedsRepaint();
	}
	
	public boolean isAntialiased() {
		return antialiased;
	}

	public void setAntialiased(boolean antialiased) {
		this.antialiased = antialiased;
		setNeedsRepaint();
	}
	
	private void repaintLayeredView(ViewGraphics g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
		AffineTransform tr = null;
		if (layerTransform != null) {
			tr = g.getTransform();
			g.transform(layerTransform);
		}
		for (Layer layer : layers) {
			if (!layer.isHidden()) layer.repaintLayer(g);
		}
		if (layerTransform != null) {
			g.setTransform(tr);
		}
	}
	
	public final Vector2D transformPointToLayers(Vector2D v) {
		v = v.clone();
		if (inversedLayerTransform != null) {
			Point2D p = inversedLayerTransform.transform(v.toPoint2D(), null);
			v.x = (int) p.getX();
			v.y = (int) p.getY();
		}
		return v;
	}
	
	public final Vector2D transformPointFromLayers(Vector2D v) {
		v = v.clone();
		if (layerTransform != null) {
			Point2D p = layerTransform.transform(v.toPoint2D(), null);
			v.x = (int) p.getX();
			v.y = (int) p.getY();
		}
		return v;
	}
	
	@Override
	public boolean isInside(Vector2D point) {
		return getLayerAtPoint(point) != null;
	}
	
	private Layer mouseFocus = null;
	
	public void setMouseFocus(Layer mouseFocus) {
		if (this.mouseFocus == mouseFocus) return;
		if (this.mouseFocus != null) {
			this.mouseFocus.mouseExited();
		}
		this.mouseFocus = mouseFocus;
		if (this.mouseFocus != null) {
			this.mouseFocus.mouseEntered();
		}
	}
	
	public final Layer getLayerAtPoint(Vector2D point) {
		return getLayerAtTransformedPoint(transformPointToLayers(point));
	}
	
	private Layer getLayerAtTransformedPoint(Vector2D point) {
		ListIterator<Layer> it = layers.listIterator(layers.size());
		while (it.hasPrevious()) {
			Layer layer = it.previous();
			Vector2D p = point.clone();
			if (!layer.isHidden() && layer.isInside(p)) {
				return layer;
			}
		}
		return null;
	}
	
	private void layeredViewMousePressed(MouseEvent e) {
		Vector2D vec = transformPointToLayers(e.getPosition(this));
		setMouseFocus(getLayerAtTransformedPoint(vec));
		if (mouseFocus != null) mouseFocus.mousePressed(e, vec);
	}
	
	private void layeredViewMouseReleased(MouseEvent e) {
		Vector2D vec = transformPointToLayers(e.getPosition(this));
		Layer layer = mouseFocus;
		if (layer != null) layer.mouseReleased(e, vec);
		setMouseFocus(getLayerAtTransformedPoint(vec));
	}
	
	private void layeredViewMouseMoved(MouseEvent e) {
		Vector2D vec = transformPointToLayers(e.getPosition(this));
		Layer layer = getLayerAtTransformedPoint(vec);
		setMouseFocus(layer);
		if (layer != null) {
			layer.mouseMoved(e, vec);
		}
	}
	
	private void layeredViewMouseDragged(MouseEvent e) {
		Vector2D vec = transformPointToLayers(e.getPosition(this));
		if (mouseFocus != null) mouseFocus.mouseDragged(e, vec);
	}
	
	private void layeredViewMouseExited() {
		setMouseFocus(null);
	}

}
