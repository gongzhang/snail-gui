package co.gongzh.snail.event;

import co.gongzh.snail.View;

public interface EventHandler {
	
	public void handle(View sender, Key key, Object arg);
	
}
