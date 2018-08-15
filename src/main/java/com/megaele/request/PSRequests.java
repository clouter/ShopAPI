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
	public List<String> getProductAttribute(PSWebServiceClient ws, String attribute, String url) throws Exception {
		List<String> productAtts = new ArrayList<String>();
		HashMap<String,Object> getSchemaOpt = new HashMap<String, Object>();
		try {
			NodeList nodeList = get(ws, getSchemaOpt, url, "product");
			for (int i = 0; i < nodeList.getLength(); i++) {
				productAtts.add(nodeList.item(i).getAttributes().getNamedItem(attribute).getTextContent());
			 }
		}catch (Exception e) {
			System.out.println("Exception getting attribute: " + attribute + "of the product: " + url);
			e.printStackTrace();
		}
		return productAtts;
	}

	
	/**
	 * Returns the product reference and their price.
	 * 
	 * @param ws
	 * @throws Exception
	 */
	public Map<String, String> getProductRefAndPrice(PSWebServiceClient ws, String url) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		try {
			Document doc = prepareXML(ws, url);
			if (doc.getElementsByTagName("reference").item(0).getChildNodes().item(0) != null) {
				map.put(doc.getElementsByTagName("reference").item(0).getChildNodes().item(0).getNodeValue(),
						doc.getElementsByTagName("price").item(0).getChildNodes().item(0).getNodeValue());
			}
		}catch (Exception e) {
			System.out.println("Exception getting reference and price of the product: " + url);
			e.printStackTrace();
		}
		return map;
	}
	
	
	/**
	 * Returns the product reference and their price from the meta_title
	 * 
	 * @param ws
	 * @throws Exception
	 */
	public Map<String, String> getProductRefAndPriceFromMetaTitle(PSWebServiceClient ws, String url) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		try {
		Document doc = prepareXML(ws, url);
		if (doc.getElementsByTagName("meta_title").item(0).getChildNodes().item(0) != null) {
			try {
				String result = null;
				String metaTitle = doc.getElementsByTagName("meta_title").item(0).getFirstChild().getFirstChild().getTextContent();
				
				String[] tokens = metaTitle.split("\\|");
				result = tokens[0].split(" ")[tokens[0].split(" ").length-1];
				
				map.put(result,	doc.getElementsByTagName("price").item(0).getChildNodes().item(0).getNodeValue());
			}catch(Exception e) {
				System.out.println("Exception parsing metatitle with " + doc.getElementsByTagName("meta_title").item(0).getChildNodes().item(0));
				e.printStackTrace();
			}
		}
		}catch (Exception e) {
			System.out.println("Exception getting reference and price from meta tile of the product: " + url);
			e.printStackTrace();
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
	public void refillStock(PSWebServiceClient ws, String url) throws Exception, TransformerException {
		HashMap<String,Object> getSchemaOpt = new HashMap<String, Object>();
		try {
			Document doc = prepareXML(ws, url);
			String stockUrl = doc.getElementsByTagName("stock_available").item(0).getAttributes().item(0).getNodeValue();
			doc = prepareXML(ws, stockUrl);
			doc.getElementsByTagName("quantity").item(0).getChildNodes().item(0).setNodeValue("100");
			getSchemaOpt.put("putXml", ws.DocumentToString(doc));
			updateXML(ws, doc, stockUrl);
		}catch (Exception e) {
			System.out.println("Exception refilling stock for product: " + url);
			e.printStackTrace();
		}
	}
	
	/**
	 * Update price for certain product given.
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	public void updatePrice(PSWebServiceClient ws, String url) throws Exception, TransformerException {
		try {
			Document doc = prepareXML(ws, url);
			Float price = Float.valueOf(doc.getElementsByTagName("price").item(0).getChildNodes().item(0).getNodeValue());
			price = (float) (price - 30.0);
			doc.getElementsByTagName("price").item(0).getChildNodes().item(0).setNodeValue(price.toString());
			removeMandatoryNodes(doc);
			updateXML(ws, doc, url);
		}catch (Exception e) {
			System.out.println("Exception updating product: " + url);
			e.printStackTrace();
		}
	}
	

	/**
	 * Marks as unactive product given
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	public void disableProduct(PSWebServiceClient ws, String url) throws Exception, TransformerException {
		try {
		Document doc = prepareXML(ws, url);
		doc.getElementsByTagName("active").item(0).getChildNodes().item(0).setNodeValue("0");
		removeMandatoryNodes(doc);
		updateXML(ws, doc, url);
		}catch (Exception e) {
			System.out.println("Exception disabling product: " + url);
			e.printStackTrace();
		}
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
		HashMap<String,Object> getSchemaOpt = new HashMap<String, Object>();
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
	private void updateXML(PSWebServiceClient ws, Document doc, String url) throws TransformerException, Exception {
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
	private void removeMandatoryNodes(Document doc) {
		Element element = (Element)doc.getElementsByTagName("quantity").item(0);
		element.getParentNode().removeChild(element);
		element = (Element)doc.getElementsByTagName("manufacturer_name").item(0);
		element.getParentNode().removeChild(element);
	}
}