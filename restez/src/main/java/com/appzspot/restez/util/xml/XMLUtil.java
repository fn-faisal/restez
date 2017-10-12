package com.appzspot.restez.util.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.appzspot.restez.util.annotation.EzModel;

public class XMLUtil {

	
	/***
	 * Get Document element from a File object
	 * @param xmlFile the xml file
	 * @return document object
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document getDocumentFromFile ( File xmlFile ) throws ParserConfigurationException, SAXException, IOException{
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		
		return doc;
	}
	
	/***
	 * Get String from xml document using XPath
	 *  Pattern : Root/Node/Node..
	 * @param doc the xml root element
	 * @param path the path to fetch the value for
	 * @return the value of xml node
	 * @throws XPathExpressionException
	 */
	public String getStringXPath(Document doc , String path) throws XPathExpressionException{
	
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile(path+"/text()");
		return (String)expr.evaluate(doc, XPathConstants.STRING);
		
	}
	
	public Node getNodeXPath(Document doc , String path) throws XPathExpressionException{
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile(path+"/*");
		return (Node)expr.evaluate(doc, XPathConstants.NODE);
		
	}
	
	public NodeList getNodeListXPath(Document doc , String path) throws XPathExpressionException{
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile(path+"/*");
		return (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		
	}
	
}
