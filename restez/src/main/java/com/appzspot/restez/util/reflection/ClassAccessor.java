package com.appzspot.restez.util.reflection;

import java.lang.reflect.Field;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.modelmbean.ModelMBeanConstructorInfo;

import org.json.JSONObject;
import org.reflections.Reflections;

import com.appzspot.restez.RestEz;
import com.appzspot.restez.RestEz.Config.EzModelConfigs;
import com.appzspot.restez.util.annotation.EzItem;
import com.appzspot.restez.util.annotation.EzModel;

public class ClassAccessor {

	/***
	 * Full call name (package name included)
	 * 
	 * @param name
	 *            the name to search the class for
	 * @return the class
	 * @throws ClassNotFoundException
	 */
	public Class<?> getClassFromName(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public EzModel getClassEzModelAnnotation(String className) throws ClassNotFoundException {

		Class modelClass = Class.forName(className);

		if (modelClass.isAnnotationPresent(EzModel.class)) {
			return (EzModel) modelClass.getAnnotation(EzModel.class);
		}

		return null;
	}

	public Set<Class<?>> getEzModelClasses(String pkg) {

		// search debug for all annotated classes
		Reflections reflections = new Reflections(pkg);
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(EzModel.class);

		return annotated;
	}

	public void saveMethodsToHash(Class modelClass, HashMap methodsMap) throws Exception {

		if (modelClass.isAnnotationPresent(EzModel.class)) {

			System.out.println("Class type : " + modelClass.getName() + "  ");

			EzModel ezModel = (EzModel) modelClass.getAnnotation(EzModel.class);

			System.out.println(" Extension : " + ezModel.extension());

			Field[] fields = modelClass.getDeclaredFields();

			Method[] methods = modelClass.getMethods();

			for (Field field : fields) {
				if (field.isAnnotationPresent(EzItem.class)) {
					EzItem ezItem = field.getAnnotation(EzItem.class);

					String fieldName = field.getName();
					String fieldCapatalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
					;

					String getterMethodString = "get" + fieldCapatalized;
					String setterMethodString = "set" + fieldCapatalized;

					boolean getterOk = false;
					boolean setterOk = false;

					for (Method method : methods) {

						if (method.getName().equals(getterMethodString)){
							getterOk = true;
							methodsMap.put(method.getName(), ezItem.name() );
						}
						else if (method.getName().equals(setterMethodString)){
							setterOk = true;
							methodsMap.put(method.getName(), ezItem.name() );
						}
					}

					if (!getterOk)
						throw new Exception(
								"Getter not found int class type : " + modelClass.getName() + " , for Field : " + fieldName);

					if (!setterOk)
						throw new Exception(
								"Setter not found int class type : " + modelClass.getName() + " , for Field : " + fieldName);

				}
			}
		}

	}

	public Method getMethodByName ( Class model, String name ){
		
		Method[] methodsInClass = model.getMethods();
		
		for ( Method method : methodsInClass ){
			if ( method.getName().equals(name) ){
				return method;
			}
		}
		
		return null;
	}
	
	public void validateAnnotations(Set<Class<?>> classes) throws Exception {

		for (Class c : classes) {

			if (c.isAnnotationPresent(EzModel.class)) {

				System.out.println("Class type : " + c.getName() + "  ");

				EzModel ezModel = (EzModel) c.getAnnotation(EzModel.class);

				System.out.println(" Extension : " + ezModel.extension());

				Field[] fields = c.getDeclaredFields();

				Method[] methods = c.getMethods();

				for (Field field : fields) {
					if (field.isAnnotationPresent(EzItem.class)) {
						EzItem ezItem = field.getAnnotation(EzItem.class);

						String fieldName = field.getName();
						String fieldCapatalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
						;

						String getterMethodString = "get" + fieldCapatalized;
						String setterMethodString = "set" + fieldCapatalized;

						boolean getterOk = false;
						boolean setterOk = false;

						for (Method method : methods) {

							if (method.getName().equals(getterMethodString))
								getterOk = true;

							else if (method.getName().equals(setterMethodString))
								setterOk = true;

						}

						if (!getterOk)
							throw new Exception(
									"Getter not found int class type : " + c.getName() + " , for Field : " + fieldName);

						if (!setterOk)
							throw new Exception(
									"Setter not found int class type : " + c.getName() + " , for Field : " + fieldName);

					}
				}
			}
		}

	}

	public void testcode() throws Exception {
		// search debug for all annotated classes
		Reflections reflections = new Reflections("debugtest.bean");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(EzModel.class);

		for (Class c : annotated) {

			if (c.isAnnotationPresent(EzModel.class)) {

				System.out.println("Class type : " + c.getName() + "  ");

				EzModel ezModel = (EzModel) c.getAnnotation(EzModel.class);

				System.out.println(" Extension : " + ezModel.extension());

				Field[] fields = c.getDeclaredFields();

				Method[] methods = c.getMethods();

				for (Field field : fields) {
					if (field.isAnnotationPresent(EzItem.class)) {
						EzItem ezItem = field.getAnnotation(EzItem.class);

						String fieldName = field.getName();
						String fieldCapatalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
						;

						String getterMethodString = "get" + fieldCapatalized;
						String setterMethodString = "set" + fieldCapatalized;

						boolean getterOk = false;
						boolean setterOk = false;

						for (Method method : methods) {

							if (method.getName().equals(getterMethodString))
								getterOk = true;

							else if (method.getName().equals(setterMethodString))
								setterOk = true;

						}

						if (!getterOk)
							throw new Exception(
									"Getter not found int class type : " + c.getName() + " , for Field : " + fieldName);

						if (!setterOk)
							throw new Exception(
									"Setter not found int class type : " + c.getName() + " , for Field : " + fieldName);

					}
				}
			}
		}

	}

}
