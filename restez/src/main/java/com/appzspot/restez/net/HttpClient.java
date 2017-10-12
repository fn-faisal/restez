package com.appzspot.restez.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class HttpClient {

	public int sendGetForStatusCode(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		return connection.getResponseCode();
	}

	public String sendGet(String urlString) throws IOException {

		URL url = new URL(urlString);
		
		String response = IOUtils.toString(url.openConnection().getInputStream());
		
		return response;
	}

}
