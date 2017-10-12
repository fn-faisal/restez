package com.appzspot.restez.util.exceptions;

public class EZExceptionResponseNot200Exception extends Exception{
	
	private static final String exceptionMessage = "The uri '%uri%' return status code other than 200 , RESPONSE_CODE : %rcode%";
	
	public EZExceptionResponseNot200Exception( String uri ,int statusCode ){
		super((exceptionMessage.replace("%uri%", uri).replace("%rcode%", statusCode+"")));
		
	}

}
