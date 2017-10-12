package com.appzspot.restez.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Faisal
 * The EzModel annotation is used to represent model classes
 * that reflect the API end points.	
 *
 */
@Target( ElementType.TYPE )
@Retention(RetentionPolicy.RUNTIME)  
public @interface EzModel {
	
	/**
	 * The url extension that represent the API end point.
	 * eg. posts/1
	 * @return the url extension.
	 */
	String extension() default "";
	
	/**
	 * The name of the model.
	 * @return the model name.
	 */
	String name() default "" ;
	
}
