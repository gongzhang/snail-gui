package co.gongzh.snail.text;

import java.awt.event.InputMethodListener;
import java.awt.im.InputMethodRequests;

public interface InputMethodCompatible {
	
	public boolean isInputMethodEnabled();
	
	public InputMethodListener getInputMethodEventHandler();
	
	public InputMethodRequests getInputMethodInfoProvider();
	
}
