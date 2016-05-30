package de.caluga.test.mongo.suite.data;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import de.caluga.morphium.TypeMapper;

public class CustomMappedObjectMapper implements TypeMapper<CustomMappedObject> {

	@Override
	public Object marshall(CustomMappedObject o) {
		Map<String, Object> map = new HashMap<>();
		
		map.put("name", o.getName());
		map.put("string_value", o.getValue());
		map.put("int_value", o.getIntValue());
		return new Document("value", map);
	}

	@Override
	public CustomMappedObject unmarshall(Object d) {
		if (d instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) d;
			
			CustomMappedObject cmo = new CustomMappedObject();
			cmo.setName((String) map.get("name"));
			cmo.setValue((String) map.get("string_value"));
			cmo.setIntValue((int) map.get("int_value"));
			return cmo;
		}
		return null;
	}

}