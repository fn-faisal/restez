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

/**
 * 
 * @author Faisal
 * The ClassAccessor class is used for class reflection.
 */
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

	/**
	 * Get the class's EzModel annotation.
	 * @param className the class name.
	 * @return EzModel annotation if class annotated, null otherwise.
	 * @throws ClassNotFoundException
	 */
	public EzModel getClassEzModelAnnotation(String className) throws ClassNotFoundException {

		Class modelClass = Class.forName(className);

		if (modelClass.isAnnotationPresent(EzModel.class)) {
			return (EzModel) modelClass.getAnnotation(EzModel.class);
		}

		return null;
	}

	/**
	 * Get the model classes for the given package.
	 * @param pkg the package name.
	 * @return A set of classes that are annotated with EzModel annotation.
	 */
	public Set<Class<?>> getEzModelClasses(String pkg) {

		// search debug for all annotated classes
		Reflections reflections = new Reflections(pkg);
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(EzModel.class);

		return annotated;
	}

	/**
	 * Save the getter and setter methods to the given HashMap
	 * @param modelClass the model class.
	 * @param methodsMap the HashMap to save the getter/setter methods for.
	 * @throws Exception
	 */
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

	/**
	 * Get a method by its name.
	 * @param model the model class.
	 * @param name the name of the method.
	 * @return the method, if found, null otherwise.
	 */
	public Method getMethodByName ( Class model, String name ){
		
		Method[] methodsInClass = model.getMethods();
		
		for ( Method method : methodsInClass ){
			if ( method.getName().equals(name) ){
				return method;
			}
		}
		
		return null;
	}
	
	/**
	 * Validate annotations for the given class set.
	 * Check whether all of the given classes in class set are 
	 * annotated with {@link EzModel} annotation and the fields 
	 * are annotated with {@link EzItem} annotation and the respective fields 
	 * have getters and setters defined.
	 * @param classes the class set
	 * @throws Exception
	 */
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
