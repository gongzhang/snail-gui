package co.gongzh.snail;

import java.util.HashMap;
import java.util.Map;

abstract class Event<T extends java.awt.event.InputEvent> {
	
	private final T AWTEvent;
	boolean handled;
	private Map<String, Object> userInfo;
	
	Event(T AWTEvent) {
		this.AWTEvent = AWTEvent;
		handled = false;
		userInfo = null;
	}
	
	public void handle() {
		handled = true;
	}
	
	public boolean isHandled() {
		return handled;
	}
	
	public T getAWTEvent() {
		return AWTEvent;
	}
	
	public Map<String, Object> userInfo() {
		if (userInfo == null) {
			userInfo = new HashMap<String, Object>(0);
		}
		return userInfo;
	}
	
}
