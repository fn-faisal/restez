package com.appzspot.restez.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.appzspot.restez.RestEz;
import com.appzspot.restez.RestEz.Config;
import com.appzspot.restez.RestEz.Config.EzModelConfigs;
import com.appzspot.restez.util.annotation.EzItem;
import com.appzspot.restez.util.reflection.ClassAccessor;

/**
 * 
 * @author Faisal
 * The EzJson class is used to handle Json object response.
 */
public class EzJson {

	/**
	 * Convert the JSON response to model object.
	 * @param modelClass the model class.
	 * @param json the JSON response.
	 * @param methods the getter and setter methods that reflect the JSON response.
	 * @return converted object.
	 * @throws Exception
	 */
	public <T> T getObjectFromJsonString(Class modelClass, String json, HashMap methods) throws Exception {

		Object obj = new JSONTokener(json).nextValue();

		// if object is json object
		if (obj instanceof JSONObject) {
			return getObjectFromJSONObject(modelClass, (JSONObject) obj, methods);

		}
		// if object is json array
		else if (obj instanceof JSONArray) {
			return getObjectListFromJSONArray(modelClass, (JSONArray) obj, methods);
		}

		return null;
	}

	/**
	 * Get an array list of objects from a JSON array
	 * @param modelClass the model class
	 * @param jsonArr the json array
	 * @param methods the getter and setter methods.
	 * @return
	 * @throws Exception
	 */
	private <T> T getObjectListFromJSONArray(Class modelClass, JSONArray jsonArr, HashMap methods) throws Exception {

		ArrayList objectList = new ArrayList();

		for (int i = 0; i < jsonArr.length(); i++) {

			JSONObject json = jsonArr.getJSONObject(i);
			Object object = modelClass.newInstance();
			ClassAccessor classAccessor = new ClassAccessor();
			Config config = RestEz.getConfiguration();

			for (Map.Entry<String, String> entry : ((HashMap<String, String>) methods).entrySet()) {
				String methodName = entry.getKey();
				String jsonRef = entry.getValue();

				if (methodName.contains("set")) {
					boolean jsonHas = false;
					if (jsonRef.contains("->")) {
						jsonHas = jsonHas(json, jsonRef.split("->")[0]);
					} else {
						jsonHas = jsonHas(json, jsonRef);
					}
					if (jsonHas) {
						Object obj = getJsonVal(json, jsonRef);
						if (obj != null) {

							Method setterMethod = classAccessor.getMethodByName(modelClass, methodName);
							Class[] methodParams = setterMethod.getParameterTypes();

							Class objectClass = obj.getClass();

							// parse method list
							if (config.ezModelContains(setterMethod.getParameterTypes()[0].getName())) {
								Class modelObjectClass = Class.forName(setterMethod.getParameterTypes()[0].getName());
								EzModelConfigs modelConfigs = config.getEzModelConfigsForClass(modelObjectClass);

								obj = getObjectFromJSONObject(modelObjectClass, (JSONObject) obj,
										modelConfigs.getMethods());
								objectClass = obj.getClass();
							}

							obj = methodParams[0].cast(obj);
							
//							if (!(methodParams[0].isAssignableFrom(objectClass))) {
//								throw new IllegalArgumentException("Can't cast from " + objectClass.getName() + "  to "
//										+ methodParams[0].getName() + " for method : " + setterMethod.getName() + ".");
//							}

							setterMethod.invoke(object, obj);
						}
					}

				}
			}

			objectList.add(object);
			// return (T) object;
		}

		return (T) objectList;
	}

	/**
	 * Get the Java value from the json object. 
	 * @param json the JSON object
	 * @param key the key to look for in JSON.
	 * @return the value of the key in JSON.
	 */
	public <T> T getJsonVal(Object json, String key) {

		// path found
		if (key.contains("->")) {
			String keyPaths[] = key.split("->");
			String keyNext = "";

			for (int i = 0; i < keyPaths.length; i++) {
				if (i != 0) {
					keyNext += keyPaths[i];
					if (i != (keyPaths.length - 1)) {
						keyNext += "->";
					}
				}

			}
			if (((JSONObject) json).isNull(keyPaths[0]))
				return null;

			Object val = getJsonVal(((JSONObject) json).get(keyPaths[0]), keyNext);
			return (T) val;
		} else {
				if (((JSONObject) json).get(key) instanceof JSONArray) {

				if (((JSONObject) json).isNull(key))
					return null;

				return (T) getStringArrayFromJsonArray(((JSONObject) json).getJSONArray(key));

			} else {
				if (((JSONObject) json).isNull(key))
					return null;
				return (T) ((JSONObject) json).get(key);
			}
		}
	}

	/**
	 * Get the string array from a JSON array.
	 * @param json the JSON array.
	 * @return the String array.
	 */
	private String[] getStringArrayFromJsonArray(JSONArray json) {

		String[] stringArr = new String[json.length()];

		for (int i = 0; i < json.length(); i++) {

			if (json.get(i) instanceof String) {
				stringArr[i] = json.getString(i);
			}
			
		}

		return stringArr;
	}

	/**
	 * Get Java object from the json object using the model class.
	 * @param modelClass the model class.
	 * @param json the json object
	 * @param methods the getter and setter methods.
	 * @return the object.
	 * @throws Exception
	 */
	private <T> T getObjectFromJSONObject(Class modelClass, JSONObject json, HashMap methods) throws Exception {

		Object object = modelClass.newInstance();
		ClassAccessor classAccessor = new ClassAccessor();
		Config config = RestEz.getConfiguration();

		for (Map.Entry<String, String> entry : ((HashMap<String, String>) methods).entrySet()) {
			String methodName = entry.getKey();
			String jsonRef = entry.getValue();

			if (methodName.contains("set")) {
			
				boolean jsonHas = false;
				if (jsonRef.contains("->")) {
					jsonHas = jsonHas(json, jsonRef.split("->")[0]);
				} else {
					jsonHas = jsonHas(json, jsonRef);
				}
				if (jsonHas) {
					Object obj = getJsonVal(json, jsonRef);
					if (obj != null) {
						
						Method setterMethod = classAccessor.getMethodByName(modelClass, methodName);
						Class[] methodParams = setterMethod.getParameterTypes();

						Class objectClass = obj.getClass();

						// parse method list
						if (config.ezModelContains(setterMethod.getParameterTypes()[0].getName())) {

							Class modelObjectClass = Class.forName(setterMethod.getParameterTypes()[0].getName());
							EzModelConfigs modelConfigs = config.getEzModelConfigsForClass(modelObjectClass);

							obj = getObjectFromJSONObject(modelObjectClass, (JSONObject) obj,
									modelConfigs.getMethods());
							objectClass = obj.getClass();
						}

						obj = methodParams[0].cast(obj);
						
//						if (!(methodParams[0].isAssignableFrom(objectClass))) {
//							throw new IllegalArgumentException("Can't cast from " + objectClass.getName() + "  to "
//									+ methodParams[0].getName() + " for method : " + setterMethod.getName() + ".");
//						}
						setterMethod.invoke(object, obj);

					}

				}

			}
		}

		return (T) object;
	}

	/**
	 * Check if JSON object contains a key.
	 * @param json the JSON object
	 * @param key the key to look for.
	 * @return true if key found, false otherwise.
	 */
	private boolean jsonHas(JSONObject json, String key) {
		
		if (json.has(key))
			if (!json.isNull(key))
				return true;

		return false;
	}

}
