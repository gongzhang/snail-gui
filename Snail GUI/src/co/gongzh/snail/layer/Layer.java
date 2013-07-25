package co.gongzh.snail.layer;

import co.gongzh.snail.MouseEvent;
import co.gongzh.snail.ViewGraphics;
import co.gongzh.snail.util.Vector2D;

public abstract class Layer {
	
	private LayeredView superview;
	private boolean hidden;
	
	public Layer() {
		superview = null;
		hidden = false;
	}
	
	public final LayeredView getSuperView() {
		return superview;
	}
	
	final void setSuperView(LayeredView view) {
		this.superview = view;
	}
	
	public final boolean isHidden() {
		return hidden;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
		setNeedsRepaint();
	}
	
	public final void setNeedsRepaint() {
		if (superview != null) {
			superview.setNeedsRepaint();
		}
	}
	
	protected abstract void repaintLayer(ViewGraphics g);
	
	public abstract boolean isInside(Vector2D point);
	
	protected void mousePressed(MouseEvent e, Vector2D location) {
	}
	
	protected void mouseReleased(MouseEvent e, Vector2D location) {
	}
	
	protected void mouseMoved(MouseEvent e, Vector2D location) {
	}
	
	protected void mouseDragged(MouseEvent e, Vector2D location) {
	}
	
	protected void mouseEntered() {
	}
	
	protected void mouseExited() {
	}
	
}
