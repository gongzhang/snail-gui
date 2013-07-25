package co.gongzh.snail;

public class KeyEvent extends Event<java.awt.event.KeyEvent> {

	KeyEvent(java.awt.event.KeyEvent AWTEvent) {
		super(AWTEvent);
	}
	
	public char getKeyChar() {
		return getAWTEvent().getKeyChar();
	}
	
	public int getKeyCode() {
		return getAWTEvent().getKeyCode();
	}
	
	public boolean isShiftDown() {
		return getAWTEvent().isShiftDown();
	}
	
	public boolean isCtrlDown() {
		return getAWTEvent().isControlDown();
	}
	
	public boolean isAltDown() {
		return getAWTEvent().isAltDown();
	}
	
	public boolean isMetaDown() {
		return getAWTEvent().isMetaDown();
	}
	
	public boolean isMetaOrCtrlDown() {
		return getAWTEvent().isMetaDown() || getAWTEvent().isControlDown();
	}

}
