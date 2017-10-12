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

public class RestEz {

	private static EzCache cache;
	private static Config configuration;
	private static Query query;

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

	public synchronized static EzCache getCache() throws Exception {
		if (cache == null) {
			cache = new EzCache();
		}
		return cache;
	}

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
	 * 
	 * @param ezName
	 * @param ezUrl
	 * @param classes model classes
	 * @return
	 * @throws Exception
	 */
	public synchronized static Config getConfigure(String ezName, String ezUrl, Class...classes )
			throws Exception {
		if (configuration == null) {
			configuration = new Config(ezName, ezUrl, classes);
		}
		return configuration;
	}

	public synchronized static Query getQuery(Config config) {

		if (query == null)
			query = new Query(config);

		return query;

	}

	public static class Query {

		private Config config;
		private boolean storeInCache;

		private Query(Config config) {
			this.config = config;
			this.storeInCache = true;
		}

		public boolean isStoreInCache() {
			return storeInCache;
		}

		public void setStoreInCache(boolean storeInCache) {
			this.storeInCache = storeInCache;
		}

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

		private <T> T getObjectFromResponse(Class modelClass, String json, HashMap methods) throws Exception {
			EzJson ezJson = new EzJson();
			return ezJson.getObjectFromJsonString(modelClass, json, methods);
		}

		private String getResponse(String url) throws IOException, EZExceptionResponseNot200Exception {

			HttpClient client = new HttpClient();

			int statusCode = client.sendGetForStatusCode(url);

			if (statusCode != 200)
				throw new EZExceptionResponseNot200Exception(url, statusCode);

			return client.sendGet(url);
		}

	}

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

		public boolean ezModelContains(String modelName) {

			for (EzModelConfigs model : restEzModels) {
				if (model.getClassPath().equals(modelName))
					return true;
			}

			return false;
		}

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

		public ArrayList<EzModelConfigs> getRestEzModels() {
			return restEzModels;
		}

		public void setRestEzModels(ArrayList<EzModelConfigs> restEzModels) {
			this.restEzModels = restEzModels;
		}

		public String getEzName() {
			return ezName;
		}

		public void setEzName(String ezName) {
			this.ezName = ezName;
		}

		public EzUrl getEzUrl() {
			return ezUrl;
		}

		public void setEzUrl(EzUrl ezUrl) {
			this.ezUrl = ezUrl;
		}

		/***
		 * 
		 * @author Faisal
		 *         <h3>URL class to hold url config data</h3>
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
		 *         <h3>Model class to hold model config data</h3>
		 */
		public class EzModelConfigs {

			private String name;
			private String classPath;
			private String modelExtension;
			private HashMap<String, String> methods;

			public EzModelConfigs() {
				methods = new HashMap<String, String>();
			}

			public HashMap<String, String> getMethods() {
				return methods;
			}

			public void setMethods(HashMap<String, String> methods) {
				this.methods = methods;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getClassPath() {
				return classPath;
			}

			public void setClassPath(String classPath) {
				this.classPath = classPath;
			}

			public String getModelExtension() {
				return modelExtension;
			}

			public void setModelExtension(String modelExtension) {
				this.modelExtension = modelExtension;
			}

		}

	}

}
