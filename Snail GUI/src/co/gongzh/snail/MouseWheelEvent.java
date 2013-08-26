package co.gongzh.snail;

import co.gongzh.snail.util.Vector2D;

public class MouseWheelEvent extends MouseEvent {
	
	MouseWheelEvent(java.awt.event.MouseWheelEvent AWTEvent, Vector2D velocity) {
		super(AWTEvent, velocity);
	}
	
	public java.awt.event.MouseWheelEvent getAWTMouseWheelEvent() {
		return (java.awt.event.MouseWheelEvent) getAWTEvent();
	}

	public int getRotation() {
		java.awt.event.MouseWheelEvent e = getAWTMouseWheelEvent();
		if (e.getScrollType() == java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL) {
			return e.getUnitsToScroll();
		} else {
			return e.getWheelRotation();
		}
	}

}
