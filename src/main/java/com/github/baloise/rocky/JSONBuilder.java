package com.github.baloise.rocky;

import static java.util.UUID.randomUUID;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class JSONBuilder {
	Map<String, Object> map = new HashMap<String, Object>();

	private JSONBuilder(String key, Object value) {
		put(key, value);
	}

	public static JSONBuilder json(String key, Object... value) {
		return new JSONBuilder(key, value);
	}

	public static JSONBuilder json(String key, Object value) {
		return new JSONBuilder(key, value);
	}

	public JSONBuilder field(String key, Object... value) {
		put(key, value);
		return this;
	}

	public JSONBuilder field(String key, Object value) {
		put(key, value);
		return this;
	}

	private Object put(String key, Object value) {
		if (value instanceof JSONBuilder) {
			value = ((JSONBuilder) value).map;
		}
		return map.put(key, value);
	}

	public Map<String, Object> build() {
		return map;
	}

	public String buildJSON() {
		return new Gson().toJson(map);
	}

	public String buildDDP() {
		return new Gson().toJson(new String[] {buildJSON() });
	}

	public JSONBuilder ddpMethodCall(String method) {
		return ddpMethodCall(method, randomUUID().toString());
	}
	
	public JSONBuilder ddpMethodCall(String method, String id) {
		return msg("method")
		.field("method", method)
		.id(id)
		.field("params", array(map));
	}
	
	public static Object[] array(Object ... objects ) {
		return objects;
	}

	public JSONBuilder id(String id) {
		return field("id", id);
	}
	public static JSONBuilder msg(String msg) {
		return json("msg", msg);
	}
	public JSONBuilder id() {
		return id(randomUUID().toString());
	}
}
