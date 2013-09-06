package co.gongzh.snail.event;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.gongzh.snail.View;

public class HandlerBundle {
	
	private final Map<Key, List<EventHandler>> handlerMap;
	
	public HandlerBundle() {
		handlerMap = new HashMap<Key, List<EventHandler>>(1);
	}
	
	public boolean addHandler(Key key, EventHandler handler) {
		if (handler == null) throw new IllegalArgumentException();
		List<EventHandler> list = handlerMap.get(key);
		if (list == null) {
			list = new LinkedList<EventHandler>();
			handlerMap.put(key, list);
		}
		if (!list.contains(handler)) {
			list.add(handler);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean removeHandler(Key key, EventHandler handler) {
		if (handler == null) throw new IllegalArgumentException();
		List<EventHandler> list = handlerMap.get(key);
		if (list != null && list.contains(handler)) {
			return list.remove(handler);
		} else {
			return false;
		}
	}
	
	public void fireEvent(View sender, Key key, Object arg) {
		List<EventHandler> list = handlerMap.get(key);
		if (list != null && !list.isEmpty()) {
			for (Object eventHandler : list.toArray()) {
				((EventHandler) eventHandler).handle(sender, key, arg);
			}
		}
	}
	
}
