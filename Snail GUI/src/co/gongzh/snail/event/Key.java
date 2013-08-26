package co.gongzh.snail.event;

import java.util.HashMap;
import java.util.Map;

import co.gongzh.snail.View;

public final class Key {
	
	private static final Map<String, Key> KEYMAP = new HashMap<String, Key>();
	
	public static Key forName(String name) {
		Key key = KEYMAP.get(name);
		if (key == null) {
			throw new IllegalArgumentException();
		}
		return key;
	}
	
	private final String key;
	private final Class<? extends View> clazz;
	private final Class<?> argumentClass;
	
	public Key(String key, Class<? extends View> targetClass, Class<?> argumentClass) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (KEYMAP.containsKey(key)) {
			throw new IllegalArgumentException("duplicate key: " + key);
		}
		this.key = key;
		this.clazz = targetClass;
		this.argumentClass = argumentClass;
		KEYMAP.put(key, this);
	}
	
	public Class<? extends View> getTargetClass() {
		return clazz;
	}
	
	public Class<?> getArgumentClass() {
		return argumentClass;
	}
	
	@Override
	public String toString() {
		return key;
	}

}
