package com.megaele.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.http.Consts;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.megaele.webservice.PSWebServiceClient;


/**
 * Handles every connection and operation through PrestaShop Api
 * 
 * @author rpila - 04/04/2018
 *
 */
public class PSRequests {

	Document doc;
	HashMap<String,Object> getSchemaOpt = new HashMap<String, Object>();

	
	/**
	 * Connects to Presta API
	 * @param url
	 * @param key
	 * @param debug
	 * @return
	 */
	public PSWebServiceClient connect(String url, String key, boolean debug) {
		PSWebServiceClient ws = new PSWebServiceClient(url, key, debug);
		return ws;
	}
	
	/**
	 * Returns the NodeList of that tag
	 * 
	 * @param ws
	 * @param getSchemaOpt
	 * @param url
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private NodeList get(PSWebServiceClient ws, HashMap<String, Object> getSchemaOpt, String url, String tag) throws Exception {
		getSchemaOpt.put("url",url);       
		Document doc = ws.get(getSchemaOpt);    
		return doc.getElementsByTagName(tag);
	}
	
	
	/**
	 * Returns the product ids that currently are in the shop.
	 * 
	 * @param ws
	 * @throws Exception
	 */
	public List<String> getProductAttribute(PSWebServiceClient ws, String attribute) throws Exception {
		NodeList nodeList = get(ws, getSchemaOpt, "http://www.megaelectrodomesticos.com/api/products", "product");
		List<String> productAtts = new ArrayList<String>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			productAtts.add(nodeList.item(i).getAttributes().getNamedItem(attribute).getTextContent());
		 }
		return productAtts;
	}

	
	/**
	 * Returns the product reference and their price.
	 * 
	 * @param ws
	 * @throws Exception
	 */
	public Map<String, String> getProductRefAndPrice(PSWebServiceClient ws, String id) throws Exception {
		Document doc = prepareXML(ws, "http://www.megaelectrodomesticos.com/api/products" + "/" + id);
		Map<String, String> map = new HashMap<String, String>();
		if (doc.getElementsByTagName("reference").item(0).getChildNodes().item(0) != null) {
			map.put(doc.getElementsByTagName("reference").item(0).getChildNodes().item(0).getNodeValue(),
					doc.getElementsByTagName("price").item(0).getChildNodes().item(0).getNodeValue());
		}
		return map;
	}
	
	/**
	 * Always replenish stock in prestashop, stock would be managed by ERP 
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	public void refillStock(PSWebServiceClient ws) throws Exception, TransformerException {
		String stockUrl = doc.getElementsByTagName("stock_available").item(0).getAttributes().item(0).getNodeValue();
		Document doc = prepareXML(ws, stockUrl);
		doc.getElementsByTagName("quantity").item(0).getChildNodes().item(0).setNodeValue("100");
		
		getSchemaOpt.put("putXml", ws.DocumentToString(doc));
		
		updateXML(ws, stockUrl);
	}

	/**
	 * Marks as unactive product given
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	public void disableProduct(PSWebServiceClient ws, String url) throws Exception, TransformerException {
		Document doc = prepareXML(ws, url);

		doc.getElementsByTagName("active").item(0).getChildNodes().item(0).setNodeValue("0");
		removeMandatoryNodes();
		getSchemaOpt.put("putXml", ws.DocumentToString(doc));
		
		updateXML(ws, url);
	}

	/**
	 * Prepares the URL to interact
	 * Prepares the XML to realize any CRUD operation
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	private Document prepareXML(PSWebServiceClient ws, String url) throws Exception, TransformerException {
		getSchemaOpt.put("url",url);
		return ws.get(getSchemaOpt);
	}
	
	/**
	 * Updates 
	 * 
	 * @param ws
	 * @throws TransformerException
	 * @throws Exception
	 */
	private void updateXML(PSWebServiceClient ws, String url) throws TransformerException, Exception {
		StringEntity entity = new StringEntity(ws.DocumentToString(doc), ContentType.create("text/xml", Consts.UTF_8));
		HttpPut httpput = new HttpPut(url);
		httpput.setEntity(entity);

		HashMap<String, Object> result = ws.executeRequest(httpput);
		ws.checkStatusCode((Integer) result.get("status_code"));
	}

	/**
	 * 
	 * Completely mandatory delete these nodes before any product update 
	 *
	 */
	private void removeMandatoryNodes() {
		Element element = (Element)doc.getElementsByTagName("quantity").item(0);
		element.getParentNode().removeChild(element);
		element = (Element)doc.getElementsByTagName("manufacturer_name").item(0);
		element.getParentNode().removeChild(element);
	}
}