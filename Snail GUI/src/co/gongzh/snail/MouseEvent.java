package co.gongzh.snail;

import co.gongzh.snail.util.Vector2D;

public class MouseEvent extends Event<java.awt.event.MouseEvent> {
	
	private final Vector2D velocity;
	private int button;
	
	MouseEvent(java.awt.event.MouseEvent AWTEvent, Vector2D velocity) {
		super(AWTEvent);
		this.velocity = velocity.clone();
		this.button = AWTEvent.getButton();
	}
	
	public int getClickCount() {
		return getAWTEvent().getClickCount();
	}
	
	public int getButton() {
		return button;
	}
	
	void setButton(int button) {
		this.button = button;
	}
	
	public Vector2D getPosition(final View view) {

		Vector2D rst = Vector2D.make(getAWTEvent().getPoint());
		if (view == null) return rst;
		
		View[] hierarchy = view.getViewHierarchy();
		for (View v : hierarchy) {
			rst = v.transformPointFromSuperView(rst);
		}
		return rst;
	}

	public Vector2D getVelocity(View view) {
		Vector2D p2 = this.velocity.clone();
		if (view == null) return p2;
		Vector2D p1 = Vector2D.make();
		View[] hierarchy = view.getViewHierarchy();
		for (View v : hierarchy) {
			p2 = v.transformPointFromSuperView(p2);
			p1 = v.transformPointFromSuperView(p1);
		}
		p2.decrease(p1);
		return p2;
	}
	
}
