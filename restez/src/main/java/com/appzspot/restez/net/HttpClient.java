package com.appzspot.restez.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

/**
 * 
 * @author Faisal
 *
 *	The HttpClient class is used to send Get requests.
 */
public class HttpClient {

	/**
	 * Send Get request for only the status code.
	 * @param urlString the url
	 * @return response code.
	 * @throws IOException
	 */
	public int sendGetForStatusCode(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		return connection.getResponseCode();
	}

	/**
	 * Send Get request to the url.
	 * @param urlString the urlString.
	 * @return the response.
	 * @throws IOException
	 */
	public String sendGet(String urlString) throws IOException {
		URL url = new URL(urlString);
		String response = IOUtils.toString(url.openConnection().getInputStream());
		return response;
	}

}
