package com.megaele.request;

import java.util.HashMap;

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
 * Lists all the products in the shop.
 * 
 * @author rpila - 04/04/2018
 *
 */
public class ClientRequests {

	static Document doc;
	static HashMap<String,Object> getSchemaOpt = new HashMap<String, Object>();
	static String PRODUCT_URL = "http://megaelectrodomesticos.com/api/products/465";
	
	/**
	 * Connects to Presta API
	 * @param url
	 * @param key
	 * @param debug
	 * @return
	 */
	private static PSWebServiceClient connect(String url, String key, boolean debug) {
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
	private static NodeList get(PSWebServiceClient ws, HashMap<String, Object> getSchemaOpt, String url, String tag) throws Exception {
		getSchemaOpt.put("url",url);       
		Document doc = ws.get(getSchemaOpt);    
		return doc.getElementsByTagName(tag);
	}
	
	
	public static void main(String[] args) {
		try {
			PSWebServiceClient ws = connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
			
			
			disableProduct(ws);
			
			refillStock(ws);
			
			
//			NodeList nodeList = get(ws, getSchemaOpt, "http://www.megaelectrodomesticos.com/api/products", "product");
//			List<String> productIds = new ArrayList<String>();
//			for (int i = 0; i < nodeList.getLength(); i++) {
//				productIds.add(nodeList.item(i).getAttributes().getNamedItem("id").getTextContent());
//			 }
//			
//			for (String id: productIds) {
//				getSchemaOpt.put("url","http://www.megaelectrodomesticos.com/api/products/" + id);
//				if (id.equals("465")){
//					doc = ws.get(getSchemaOpt);
//					doc.getElementsByTagName("on_sale").item(0).getChildNodes().item(0).setNodeValue("1");
//					System.out.println(ws.DocumentToString(doc));
//					ws.edit(getSchemaOpt);
//				}
//			}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}

	/**
	 * Always replenish stock in prestashop, stock would be managed by ERP 
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	private static void refillStock(PSWebServiceClient ws) throws Exception, TransformerException {
		String stockUrl = doc.getElementsByTagName("stock_available").item(0).getAttributes().item(0).getNodeValue();
		prepareXML(ws, stockUrl);
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
	private static void disableProduct(PSWebServiceClient ws) throws Exception, TransformerException {
		prepareXML(ws, PRODUCT_URL);

		doc.getElementsByTagName("active").item(0).getChildNodes().item(0).setNodeValue("0");
		removeMandatoryNodes();
		getSchemaOpt.put("putXml", ws.DocumentToString(doc));
		
		updateXML(ws, PRODUCT_URL);
	}

	/**
	 * Prepares the URL to interact
	 * Prepares the XML to realize any CRUD operation
	 * 
	 * @param ws
	 * @throws Exception
	 * @throws TransformerException
	 */
	private static void prepareXML(PSWebServiceClient ws, String url) throws Exception, TransformerException {
		getSchemaOpt.put("url",url);
		doc = ws.get(getSchemaOpt);
	}
	
	/**
	 * Updates 
	 * 
	 * @param ws
	 * @throws TransformerException
	 * @throws Exception
	 */
	private static void updateXML(PSWebServiceClient ws, String url) throws TransformerException, Exception {
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
	private static void removeMandatoryNodes() {
		Element element = (Element)doc.getElementsByTagName("quantity").item(0);
		element.getParentNode().removeChild(element);
		element = (Element)doc.getElementsByTagName("manufacturer_name").item(0);
		element.getParentNode().removeChild(element);
	}
}