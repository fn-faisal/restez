package com.appzspot.restez;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.appzspot.restez.RestEz.Config.EzModelConfigs;
import com.appzspot.restez.RestEz.Config.EzUrl;
import com.appzspot.restez.net.HttpClient;
import com.appzspot.restez.util.EzJson;
import com.appzspot.restez.util.annotation.EzItem;
import com.appzspot.restez.util.annotation.EzModel;
import com.appzspot.restez.util.exceptions.EZCacheException;
import com.appzspot.restez.util.exceptions.EZExceptionResponseNot200Exception;
import com.appzspot.restez.util.reflection.ClassAccessor;
import com.appzspot.restez.util.xml.XMLContract;
import com.appzspot.restez.util.xml.XMLUtil;

/**
 * 
 * @author Faisal
 *
 * The main RestEz class.
 *
 */
public class RestEz {

	private static EzCache cache;
	private static Config configuration;
	private static Query query;

	/**
	 * Configure RestEz from given data.
	 * No argument configure method( Used when the ezconfig.xml file is available ).
	 * @throws Exception
	 */
	public synchronized static void configure() throws Exception {
		if (configuration == null) {
			XMLUtil xmlUtil = new XMLUtil();

			// init config file
			File xmlConfigFile = new File(XMLContract.ConfigContract.CONFIG_FILE_NAME + ".xml");

			if (!xmlConfigFile.exists()) {
				// load configs from android manifest
				xmlConfigFile = new File("AndroidManifest.xml");

				if (!xmlConfigFile.exists())
					throw new Exception(
							"Config file not found." + " Please make a ezconfig.xml file in your project folder. ");
			}
			Document document = xmlUtil.getDocumentFromFile(xmlConfigFile);

			configuration = new Config(document);
		}
	}

	/***
	 * Configure RestEz from given data
	 * @param ezName the module name
	 * @param ezUrl the url
	 * @param classes the model classes
	 * @throws Exception
	 */
	public synchronized static void configure(String ezName, String ezUrl, Class...classes) throws Exception {
		if (configuration == null) {
			configuration = new Config(ezName, ezUrl, classes);
		}
	}

	/**
	 * Return the cache object.
	 * @return EzCache.
	 * @throws Exception
	 */
	public synchronized static EzCache getCache() throws Exception {
		if (cache == null) {
			cache = new EzCache();
		}
		return cache;
	}

	/**
	 * Get the Configuration object.
	 * @return Config object.
	 * @throws Exception
	 */
	public synchronized static Config getConfiguration() throws Exception {
		if (configuration == null) {
			XMLUtil xmlUtil = new XMLUtil();

			// init config file
			File xmlConfigFile = new File(XMLContract.ConfigContract.CONFIG_FILE_NAME + ".xml");

			if (!xmlConfigFile.exists())
				throw new Exception("Config file not found. Please make a ezconfig.xml file in your project folder");

			Document document = xmlUtil.getDocumentFromFile(xmlConfigFile);

			configuration = new Config(document);
		}
		return configuration;
	}

	/***
	 * Method used to get the Config object and initialize it if it already has not been initialized.
	 * @param ezName the module name.
	 * @param ezUrl the api URL.
	 * @param classes model classes
	 * @return Config object.
	 * @throws Exception
	 */
	public synchronized static Config getConfigure(String ezName, String ezUrl, Class...classes )
			throws Exception {
		if (configuration == null) {
			configuration = new Config(ezName, ezUrl, classes);
		}
		return configuration;
	}

	/**
	 * Get the query object that is used to call Rest API end points.
	 * @param config the configuration object.
	 * @return the Query object.
	 */
	public synchronized static Query getQuery(Config config) {

		if (query == null)
			query = new Query(config);

		return query;

	}

	/**
	 * @author Faisal
	 *	Query class is used to call Rest API end points with or without different parameters.
	 *
	 */
	public static class Query {

		private Config config;
		private boolean storeInCache;

		/**
		 * Single arg constructor.
		 * @param config the Config object
		 */
		private Query(Config config) {
			this.config = config;
			this.storeInCache = true;
		}

		/**
		 * Check if store in cache option is toggled.
		 * @return true if cache storage is allowed, false otherwise.
		 */
		public boolean isStoreInCache() {
			return storeInCache;
		}

		/**
		 * Set caching of the API calls.
		 * @param storeInCache allow caching or not
		 */
		public void setStoreInCache(boolean storeInCache) {
			this.storeInCache = storeInCache;
		}

		/**
		 * Execute the query and call the API end point.
		 * @param modelClass the model that reflects the API end point.
		 * @param query the query ( eg. q=?,c=? )
		 * @param parameters the parameters for the query.
		 * @return The resulting response ( Object for single object respone and ArrayList for multiple object response )
		 * @throws Exception
		 */
		public <R, T> R execQuery(Class modelClass, String query, T... parameters) throws Exception {

			ClassAccessor classAccessor = new ClassAccessor();
			EzModelConfigs modelConfigs = config.getEzModelConfigsForClass(modelClass);
			EzCache ezCache = getCache();

			if (modelConfigs == null)
				return null;

			EzUrl urlConfigs = config.getEzUrl();

			String url = urlConfigs.getProtocol() + "://" + urlConfigs.getAuth() + "/"
					+ modelConfigs.getModelExtension();

			String[] queryParams = query.split(",");

			if (queryParams.length != parameters.length)
				throw new Exception("invalid query parameter size!");

			String urlQuery = "";

			if ( queryParams.length > 0 ){
				
				urlQuery += "?";
				
			}
			
			for (int i = 0; i < queryParams.length; i++) {

				String queryWithVal = queryParams[i].replace("?", parameters[i].toString());

				urlQuery += queryWithVal;

				if (i < queryParams.length - 1)
					urlQuery += "&";
			}

			url = url.replace("#", urlQuery);

			R responseObject = null;
			if (ezCache.isCached(url)) {
				responseObject = (R) ezCache.getCachedObject(url);
			} else {
				String response = getResponse(url);
				responseObject = getObjectFromResponse(modelClass, response, modelConfigs.getMethods());

				if (storeInCache) {
					if (!ezCache.cacheObject(url, responseObject)) {
						throw new EZCacheException("Could not cache object : " + responseObject + ". Request url : "
								+ url + ". Class : " + modelClass.getName());
					}
				}

			}
			return responseObject;
		}

		/**
		 * Execute the query and call the API end point.
		 * @param modelClass the model that reflects the end point.
		 * @param parameter optional parameter ( if no parameter is required, use an empty string )
		 * @return Object for single object response, ArrayList for multiple objects response.
		 * @throws Exception
		 */
		public <R, T> R execQuery(Class modelClass, T parameter) throws Exception {

			ClassAccessor classAccessor = new ClassAccessor();
			EzModelConfigs modelConfigs = config.getEzModelConfigsForClass(modelClass);
			EzCache ezCache = getCache();
			if (modelConfigs == null)
				return null;

			EzUrl urlConfigs = config.getEzUrl();

			String url = urlConfigs.getProtocol() + "://" + urlConfigs.getAuth() + "/"
					+ modelConfigs.getModelExtension();

			url = url.replace("#", parameter.toString());

			R responseObject = null;
			if (ezCache.isCached(url)) {
				responseObject = (R) ezCache.getCachedObject(url);
			} else {
				String response = getResponse(url);
				responseObject = getObjectFromResponse(modelClass, response, modelConfigs.getMethods());

				if (storeInCache) {
					if (!ezCache.cacheObject(url, responseObject)) {
						throw new EZCacheException("Could not cache object : " + responseObject + ". Request url : "
								+ url + ". Class : " + modelClass.getName());
					}
				}

			}
			return responseObject;
		}

		/**
		 * Convert the response to Java Object.
		 * @param modelClass the model that Reflects the API end point. 
		 * @param json the json response.
		 * @param methods a HashMap of the getter and setter methods.
		 * @return The response as Java Object.
		 * @throws Exception
		 */
		private <T> T getObjectFromResponse(Class modelClass, String json, HashMap methods) throws Exception {
			EzJson ezJson = new EzJson();
			return ezJson.getObjectFromJsonString(modelClass, json, methods);
		}

		/**
		 * Call the API ending for the given URL
		 * @param url the url of the end point.
		 * @return the response.
		 * @throws IOException
		 * @throws EZExceptionResponseNot200Exception
		 */
		private String getResponse(String url) throws IOException, EZExceptionResponseNot200Exception {

			HttpClient client = new HttpClient();

			int statusCode = client.sendGetForStatusCode(url);

			if (statusCode != 200)
				throw new EZExceptionResponseNot200Exception(url, statusCode);

			return client.sendGet(url);
		}

	}

	/**
	 * 
	 * @author Faisal
	 * The EzCache class is used to cache responses to avoid redundant duplicate API calls. 
	 */
	public static class EzCache {

		// key : uri , val : obj
		private HashMap<String, Object> restEzCache;

		public <R, T> R getValueFromCache(String query, T... ts) {

			return null;
		}

		public EzCache() {
			restEzCache = new HashMap<String, Object>();
		}

		public boolean isCached(String url) {

			for (Map.Entry<String, Object> entry : restEzCache.entrySet()) {
				if (entry.getKey().equals(url))
					return true;
			}

			return false;
		}

		public Object getCachedObject(String url) {
			for (Map.Entry<String, Object> entry : restEzCache.entrySet()) {
				if (entry.getKey().equals(url))
					return entry.getValue();
			}

			return null;
		}

		public boolean cacheObject(String url, Object object) {
			restEzCache.put(url, object);
			return isCached(url);
		}

	}

	/**
	 * 
	 * @author Faisal
	 * The Config class is used to configure RestEz.
	 */
	public static class Config {

		// component name
		private String ezName = "";

		// url info
		private EzUrl ezUrl;

		private ArrayList<EzModelConfigs> restEzModels;

		/***
		 * Generate configurations from params
		 * 
		 * @param ezName the name of the module
		 * @param ezUrl the api url
		 * @param classes the model classes 
		 * @param ezModelPkgPath model package path
		 * @throws Exception
		 */
		private Config(String ezName, String ezUrl, Class... classes) throws Exception {

			this.ezName = ezName;
			this.ezUrl = new EzUrl();

			String urlProtocol = ezUrl.split("://")[0];
			String urlAuth = ezUrl.split("://")[1];
			this.ezUrl.setAuth(urlAuth);
			this.ezUrl.setProtocol(urlProtocol);

			// init models
			
			if ( this.restEzModels == null )
				this.restEzModels = new ArrayList<RestEz.Config.EzModelConfigs>();
			
			if (classes.length > 0) {

				for (Class c : classes) {

					if (c.isAnnotationPresent(EzModel.class)) {
						EzModelConfigs modelConfigs = new EzModelConfigs();
						EzModel ezModel = (EzModel) c.getAnnotation(EzModel.class);

						// setup the model
						modelConfigs.setModelExtension(ezModel.extension());
						modelConfigs.setName(ezModel.name());
						modelConfigs.setClassPath(c.getName());

						// validating model
						Field[] fields = c.getDeclaredFields();

						Method[] methods = c.getMethods();

						for (Field field : fields) {
							if (field.isAnnotationPresent(EzItem.class)) {
								EzItem ezItem = field.getAnnotation(EzItem.class);

								String fieldName = field.getName();
								String fieldCapatalized = fieldName.substring(0, 1).toUpperCase()
										+ fieldName.substring(1);

								String getterMethodString = "get" + fieldCapatalized;
								String setterMethodString = "set" + fieldCapatalized;

								boolean getterOk = false;
								boolean setterOk = false;

								for (Method method : methods) {

									if (method.getName().equals(getterMethodString)) {
										getterOk = true;
										modelConfigs.getMethods().put(method.getName(), ezItem.name());

									} else if (method.getName().equals(setterMethodString)) {
										setterOk = true;
										modelConfigs.getMethods().put(method.getName(), ezItem.name());
									}

								}

								if (!getterOk)
									throw new Exception("Getter not found int class type : " + c.getName()
											+ " , for Field : " + fieldName);

								if (!setterOk)
									throw new Exception("Setter not found int class type : " + c.getName()
											+ " , for Field : " + fieldName);

							}
						}

						restEzModels.add(modelConfigs);

					}
				}
			}

		}

		/***
		 * Generate configuration from document object
		 * 
		 * @param document
		 *            the document object
		 * @throws Exception
		 */
		private Config(Document document) throws Exception {

			XMLUtil xmlUtil = new XMLUtil();
			restEzModels = new ArrayList<RestEz.Config.EzModelConfigs>();

			// init ez model
			ezName = (xmlUtil.getStringXPath(document,
					XMLContract.ConfigContract.XML_ROOT_NAME + "/" + XMLContract.ConfigContract.XML_EZ_NAME)).trim();

			// init ez url
			initEzUrl(document);

			// ini ezmodel
			initEzModel(document);

		}

		/**
		 * Initialize the EzUrl
		 * @param document the config file
		 * @throws XPathExpressionException
		 */
		private void initEzUrl(Document document) throws XPathExpressionException {
			XMLUtil xmlUtil = new XMLUtil();

			// set url auth
			String urlAuth = (xmlUtil.getStringXPath(document, XMLContract.ConfigContract.XML_ROOT_NAME + "/"
					+ XMLContract.ConfigContract.XML_EZ_URL_SRC + "/" + XMLContract.ConfigContract.XML_EZ_URL_AUTH))
							.trim();

			// set url protocol
			String urlProtocol = (xmlUtil.getStringXPath(document, XMLContract.ConfigContract.XML_ROOT_NAME + "/"
					+ XMLContract.ConfigContract.XML_EZ_URL_SRC + "/" + XMLContract.ConfigContract.XML_EZ_URL_PROTOCOL))
							.trim();

			EzUrl ezUrl = new EzUrl();
			ezUrl.setAuth(urlAuth);
			ezUrl.setProtocol(urlProtocol);

			this.ezUrl = ezUrl;

		}

		/**
		 * Initialize model objects that reflect the API end points.
		 * @param document the config file.
		 * @throws Exception
		 */
		private void initEzModel(Document document) throws Exception {
			ClassAccessor classAccessor = new ClassAccessor();
			XMLUtil xmlUtil = new XMLUtil();

			// get the root model node list
			NodeList modelRootNodeList = xmlUtil.getNodeListXPath(document,
					XMLContract.ConfigContract.XML_ROOT_NAME + "/" + XMLContract.ConfigContract.XML_EZ_MODEL_ROOT);

			// if model node, in xml, does not exists
			if (modelRootNodeList.getLength() <= 0) {

				Set<Class<?>> annotated = classAccessor.getEzModelClasses("");
				ArrayList<EzModelConfigs> list = new ArrayList<EzModelConfigs>();

				for (Class c : annotated) {

					if (c.isAnnotationPresent(EzModel.class)) {
						EzModelConfigs modelConfigs = new EzModelConfigs();
						EzModel ezModel = (EzModel) c.getAnnotation(EzModel.class);

						// setup the model
						modelConfigs.setModelExtension(ezModel.extension());
						modelConfigs.setName(ezModel.name());
						modelConfigs.setClassPath(c.getName());

						// validating model
						Field[] fields = c.getDeclaredFields();

						Method[] methods = c.getMethods();

						for (Field field : fields) {
							if (field.isAnnotationPresent(EzItem.class)) {
								EzItem ezItem = field.getAnnotation(EzItem.class);

								String fieldName = field.getName();
								String fieldCapatalized = fieldName.substring(0, 1).toUpperCase()
										+ fieldName.substring(1);
								;

								String getterMethodString = "get" + fieldCapatalized;
								String setterMethodString = "set" + fieldCapatalized;

								boolean getterOk = false;
								boolean setterOk = false;

								for (Method method : methods) {

									if (method.getName().equals(getterMethodString)) {
										getterOk = true;
										modelConfigs.getMethods().put(method.getName(), ezItem.name());

									} else if (method.getName().equals(setterMethodString)) {
										setterOk = true;
										modelConfigs.getMethods().put(method.getName(), ezItem.name());
									}

								}

								if (!getterOk)
									throw new Exception("Getter not found int class type : " + c.getName()
											+ " , for Field : " + fieldName);

								if (!setterOk)
									throw new Exception("Setter not found int class type : " + c.getName()
											+ " , for Field : " + fieldName);

							}
						}

						restEzModels.add(modelConfigs);

					}
				}

			}
			// if model node, in xml, exists
			else {

				for (int i = 0; i < modelRootNodeList.getLength(); i++) {

					Node node = modelRootNodeList.item(i);
					NodeList childNodes = node.getChildNodes();

					String modelName = "";
					String modelClass = "";
					String modelExtension = "";

					for (int j = 0; j < childNodes.getLength(); j++) {

						Node modelInfo = childNodes.item(j);

						// model name
						if (modelInfo.getNodeName().equals(XMLContract.ConfigContract.XML_EZ_MODEL_SRC_NAME)) {
							modelName = modelInfo.getTextContent();
						}
						// model class
						else if (modelInfo.getNodeName().equals(XMLContract.ConfigContract.XML_EZ_MODEL_SRC_CLASS)) {
							modelClass = modelInfo.getTextContent();
						}
						// model extension
						else if (modelInfo.getNodeName()
								.equals(XMLContract.ConfigContract.XML_EZ_MODEL_SRC_EXTENSION)) {
							modelExtension = modelInfo.getTextContent();
						}
					}

					EzModelConfigs ezModel = new EzModelConfigs();
					ezModel.setName(modelName);
					ezModel.setClassPath(modelClass);
					ezModel.setModelExtension(modelExtension);

					classAccessor.getClassEzModelAnnotation(modelClass);
					classAccessor.saveMethodsToHash(Class.forName(modelClass), ezModel.getMethods());

					restEzModels.add(ezModel);

				}
			}
		}
		
		/**
		 * Check if the model exists.
		 * @param modelName the EzModel name.
		 * @return true if model exists, false otherwise.
		 */
		public boolean ezModelContains(String modelName) {

			for (EzModelConfigs model : restEzModels) {
				if (model.getClassPath().equals(modelName))
					return true;
			}

			return false;
		}

		/**
		 * Get the configuration for the class.
		 * @param modelClass the model class
		 * @return Model config object.
		 * @throws ClassNotFoundException
		 */
		public EzModelConfigs getEzModelConfigsForClass(Class modelClass) throws ClassNotFoundException {
			ClassAccessor classAccessor = new ClassAccessor();

			for (EzModelConfigs model : restEzModels) {

				Class modelListClass = classAccessor.getClassFromName(model.getClassPath());

				if (modelListClass.isAssignableFrom(modelClass)) {
					return model;
				}

			}
			return null;
		}

		/**
		 * Get the configurations for the model classes
		 * @return an array list of model config objects.
		 */
		public ArrayList<EzModelConfigs> getRestEzModels() {
			return restEzModels;
		}

		/**
		 * Get the configurations for the model classes.
		 * @param restEzModels the list of model config objects.
		 */
		public void setRestEzModels(ArrayList<EzModelConfigs> restEzModels) {
			this.restEzModels = restEzModels;
		}

		/**
		 * Get the Ez Module name.
		 * @return the module name.
		 */
		public String getEzName() {
			return ezName;
		}

		/**
		 * Set the Ez Module name.
		 * @param ezName the name for the module.
		 */
		public void setEzName(String ezName) {
			this.ezName = ezName;
		}

		/**
		 * Get the API url.
		 * @return the API url.
		 */
		public EzUrl getEzUrl() {
			return ezUrl;
		}

		/**
		 * Set the API url.
		 * @param ezUrl the API url.
		 */
		public void setEzUrl(EzUrl ezUrl) {
			this.ezUrl = ezUrl;
		}

		/***
		 * 
		 * @author Faisal
		 * The EzUrl class holds the main/host url of the REST API.
		 */
		public class EzUrl {

			private String auth;
			private String protocol;

			public String getAuth() {
				return auth;
			}

			public void setAuth(String auth) {
				this.auth = auth;
			}

			public String getProtocol() {
				return protocol;
			}

			public void setProtocol(String protocol) {
				this.protocol = protocol;
			}

		}

		/***
		 *
		 * @author Faisal
		 * The EzModelConfigs class holds configuration data related to each individual model class.
		 */
		public class EzModelConfigs {

			private String name;
			private String classPath;
			private String modelExtension;
			private HashMap<String, String> methods;

			public EzModelConfigs() {
				methods = new HashMap<String, String>();
			}

			/**
			 * Return the getter and setter methods for the fields that reflect the JSON response.
			 * @return the getter and setter methods.
			 */
			public HashMap<String, String> getMethods() {
				return methods;
			}
			
			/**
			 * Set the getter and setter methods for the fields that reflect the JSON response.
			 * @param methods the getter and setter methods.
			 */
			public void setMethods(HashMap<String, String> methods) {
				this.methods = methods;
			}

			/**
			 * Get the name of the model.
			 * @return the name of the model.
			 */
			public String getName() {
				return name;
			}

			/**
			 * Set the name of the model.
			 * @param name the name of the model.
			 */
			public void setName(String name) {
				this.name = name;
			}

			/**
			 * Get the path of the model class.
			 * @return the path of the model class.
			 */
			public String getClassPath() {
				return classPath;
			}

			/**
			 * Set the path of the model class.
			 * @param classPath the path of the model class.
			 */
			public void setClassPath(String classPath) {
				this.classPath = classPath;
			}

			/**
			 * Get the url extension of the model class.
			 * @return the Url extension of the model class.
			 */
			public String getModelExtension() {
				return modelExtension;
			}

			/**
			 * Set the url extension for the model class.
			 * @param modelExtension
			 */
			public void setModelExtension(String modelExtension) {
				this.modelExtension = modelExtension;
			}

		}

	}

}
