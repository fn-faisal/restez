package com.appzspot.restez.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Faisal
 * The EzItem annotation is used to represent keys in the JSON response.
 */
@Target( ElementType.FIELD )
@Retention(RetentionPolicy.RUNTIME)
public @interface EzItem {

	/**
	 * The name of the JSON Key.
	 * @return the name of the key.
	 */
	String name() default "" ;
	
}
