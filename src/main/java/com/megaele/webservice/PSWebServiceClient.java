package com.megaele.webservice;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Creates and implements the connection and operations through prestashop webservice
 * 
 * @author rpila - 04/04/2018
 *
 */
public class PSWebServiceClient {

	protected String url;
	protected String key;
	protected boolean debug;

	private final CloseableHttpClient httpclient;
	private CloseableHttpResponse response;
	private HashMap<String, Object> responseReturns;

	/**
	 * PrestaShopWebservice constructor. <code>
	 * 
	 * @param url Root URL for the shop
	 * @param key Authentification key
	 * @param debug Debug mode Activated (true) or deactivated (false)
	 */
	public PSWebServiceClient(String url, String key, boolean debug) {
		this.url = url;
		this.key = key;
		this.debug = debug;

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(key, ""));

		this.httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
	}

	/**
	 * Take the status code and throw an exception if the server didn't return
	 * 200 or 201 code
	 * 
	 * @param status_code Status code of an HTTP return
	 * @throws pswebservice.Exception
	 */
	public void checkStatusCode(int status_code) throws Exception {
		switch (status_code) {
		case 200:
		case 201:
			break;
		case 204:
			throw new Exception("No content");
		case 400:
			throw new Exception("Bad Request");
		case 401:
			throw new Exception("Unauthorized");
		case 404:
			throw new Exception("Not Found");
		case 405:
			throw new Exception("Method Not Allowed");
		case 500:
			throw new Exception("Internal Server Error");
		default:
			throw new Exception("This call to PrestaShop Web Services returned an unexpected HTTP status of:" + status_code);
		}
	}

	protected String getResponseContent() {
		try {
			return readInputStreamAsString((InputStream) this.responseReturns.get("response"));
		} catch (IOException ex) {
			return "";
		}
	}

	/**
	 * Handles request to PrestaShop Webservice. Can throw exception.
	 * 
	 * @param url Resource name
	 * @param request
	 * @return array status_code, response
	 * @throws pswebservice.Exception
	 */
	public HashMap<String, Object> executeRequest(HttpUriRequest request) throws Exception {
		HashMap<String, Object> returns = new HashMap<>();
		try {
			response = httpclient.execute(request);
			Header[] headers = response.getAllHeaders();
			HttpEntity entity = response.getEntity();

			if (this.debug) {
				System.out.println("Status:  " + response.getStatusLine());
				System.out.println("====================Header======================");
				for (Header h : headers) {
					System.out.println(h.getName() + " : " + h.getValue());
				}

			}
			returns.put("status_code", response.getStatusLine().getStatusCode());
			returns.put("response", entity.getContent());
			returns.put("header", headers);
			this.responseReturns = returns;
		} catch (IOException ex) {
			throw new Exception("Bad HTTP response : " + ex.toString());
		}

		return returns;
	}

	/**
	 * Load XML from string. Can throw exception
	 * 
	 * @param responseBody
	 * @return parsedXml
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 * @throws java.io.IOException
	 */
	protected Document parseXML(InputStream responseBody)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.parse(responseBody);
	}

	/**
	 * Add (POST) a resource
	 * <p>
	 * Unique parameter must take : <br>
	 * <br>
	 * 'resource' => Resource name<br>
	 * 'postXml' => Full XML string to add resource<br>
	 * <br>
	 * 
	 * @param opt
	 * @return xml response
	 */
	public Document add(Map<String, Object> opt) throws Exception {
		if ((opt.containsKey("resource") && opt.containsKey("postXml"))
				|| (opt.containsKey("url") && opt.containsKey("postXml"))) {
			String completeUrl;
			completeUrl = (opt.containsKey("resource") ? this.url + "/api/" + (String) opt.get("resource")
			: (String) opt.get("url"));
			String xml = (String) opt.get("postXml");
			if (opt.containsKey("id_shop"))
				completeUrl += "&id_shop=" + (String) opt.get("id_shop");
			if (opt.containsKey("id_group_shop"))
				completeUrl += "&id_group_shop=" + (String) opt.get("id_group_shop");

			StringEntity entity = new StringEntity(xml, ContentType.create("text/xml", Consts.UTF_8));
			// entity.setChunked(true);

			HttpPost httppost = new HttpPost(completeUrl);
			httppost.setEntity(entity);

			HashMap<String, Object> result = this.executeRequest(httppost);
			this.checkStatusCode((Integer) result.get("status_code"));

			try {
				Document doc = this.parseXML((InputStream) result.get("response"));
				response.close();
				return doc;
			} catch (ParserConfigurationException | SAXException | IOException ex) {
				throw new Exception("Response XML Parse exception");
			}

		} else {
			throw new Exception("Bad parameters given");
		}

	}

	/**
	 * Retrieve (GET) a resource
	 * <p>
	 * Unique parameter must take : <br>
	 * <br>
	 * 'url' => Full URL for a GET request of Webservice (ex:
	 * http://mystore.com/api/customers/1/)<br>
	 * OR<br>
	 * 'resource' => Resource name,<br>
	 * 'id' => ID of a resource you want to get<br>
	 * <br>
	 * </p>
	 * <code>
	 * 
	 * try
	 * {
	 *  PSWebServiceClient ws = new PrestaShopWebservice('http://mystore.com/', 'ZQ88PRJX5VWQHCWE4EE7SQ7HPNX00RAJ', false);
	 *  HashMap<String,Object> opt = new HashMap();
	 *  opt.put("resouce","orders");
	 *  opt.put("id",1);
	 *  Document xml = ws->get(opt);
	 *	// Here in xml, a XMLElement object you can parse
	 * catch (Exception ex)
	 * {
	 *  Handle exception
	 * }
	 * 
	 * </code>
	 * 
	 * @param opt
	 *            Map representing resource to get.
	 * @return Document response
	 */
	public Document get(Map<String, Object> opt) throws Exception {
		String completeUrl;
		if (opt.containsKey("url")) {
			completeUrl = (String) opt.get("url");
		} else if (opt.containsKey("resource")) {
			completeUrl = this.url + "/api/" + opt.get("resource");
			if (opt.containsKey("id"))
				completeUrl += "/" + opt.get("id");

			String[] params = new String[] { "filter", "display", "sort", "limit", "id_shop", "id_group_shop" };
			for (String p : params)
				if (opt.containsKey("p"))
					try {
						completeUrl += "?" + p + "=" + URLEncoder.encode((String) opt.get(p), "UTF-8") + "&";
					} catch (UnsupportedEncodingException ex) {
						throw new Exception("URI encodin excepton: " + ex.toString());
					}

		} else {
			throw new Exception("Bad parameters given");
		}

		HttpGet httpget = new HttpGet(completeUrl);
		HashMap<String, Object> result = this.executeRequest(httpget);

		this.checkStatusCode((int) result.get("status_code"));// check the

		try {
			Document doc = this.parseXML((InputStream) result.get("response"));
			response.close();
			return doc;
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new Exception("Response XML Parse exception: " + ex.toString());
		}

	}

	/**
	 * Head method (HEAD) a resource
	 *
	 * @param opt Map representing resource for head request.
	 * @return XMLElement status_code, response
	 */
	public Map<String, String> head(Map<String, Object> opt) throws Exception {
		String completeUrl;
		if (opt.containsKey("url")) {
			completeUrl = (String) opt.get("url");
		} else if (opt.containsKey("resource")) {
			completeUrl = this.url + "/api/" + opt.get("resource");
			if (opt.containsKey("id"))
				completeUrl += "/" + opt.get("id");

			String[] params = new String[] { "filter", "display", "sort", "limit" };
			for (String p : params)
				if (opt.containsKey("p"))
					try {
						completeUrl += "?" + p + "=" + URLEncoder.encode((String) opt.get(p), "UTF-8") + "&";
					} catch (UnsupportedEncodingException ex) {
						throw new Exception("URI encodin excepton: " + ex.toString());
					}

		} else
			throw new Exception("Bad parameters given");

		HttpHead httphead = new HttpHead(completeUrl);
		HashMap<String, Object> result = this.executeRequest(httphead);
		this.checkStatusCode((int) result.get("status_code"));// check the

		HashMap<String, String> headers = new HashMap<String, String>();
		for (Header h : (Header[]) result.get("header")) {
			headers.put(h.getName(), h.getValue());
		}
		return headers;
	}

	/**
	 * Edit (PUT) a resource
	 * <p>
	 * Unique parameter must take : <br>
	 * <br>
	 * 'resource' => Resource name ,<br>
	 * 'id' => ID of a resource you want to edit,<br>
	 * 'putXml' => Modified XML string of a resource<br>
	 * <br>
	 * 
	 * @param opt representing resource to edit.
	 * @return
	 */
	public Document edit(Map<String, Object> opt) throws Exception {

		String xml = "";
		String completeUrl;
		if (opt.containsKey("url"))
			completeUrl = (String) opt.get("url");
		else if (((opt.containsKey("resource") && opt.containsKey("id")) || opt.containsKey("url"))
				&& opt.containsKey("putXml")) {
			completeUrl = (opt.containsKey("url")) ? (String) opt.get("url")
					: this.url + "/api/" + opt.get("resource") + "/" + opt.get("id");
			xml = (String) opt.get("putXml");
			if (opt.containsKey("id_shop"))
				completeUrl += "&id_shop=" + opt.get("id_shop");
			if (opt.containsKey("id_group_shop"))
				completeUrl += "&id_group_shop=" + opt.get("id_group_shop");
		} else
			throw new Exception("Bad parameters given");

		StringEntity entity = new StringEntity(xml, ContentType.create("text/xml", Consts.UTF_8));

		HttpPut httpput = new HttpPut(completeUrl);
		httpput.setEntity(entity);
		HashMap<String, Object> result = this.executeRequest(httpput);
		this.checkStatusCode((int) result.get("status_code"));// check the

		try {
			Document doc = this.parseXML((InputStream) result.get("response"));
			response.close();
			return doc;
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new Exception("Response XML Parse exception: " + ex.toString());
		}
	}

	/**
	 * Delete (DELETE) a resource. Unique parameter must take : <br>
	 * <br>
	 * 'resource' => Resource name<br>
	 * 'id' => ID or array which contains IDs of a resource(s) you want to
	 * delete<br>
	 * <br>
	 * 
	 * @param opt representing resource to delete.
	 * @return
	 */
	public boolean delete(Map<String, Object> opt) throws Exception {
		String completeUrl = "";
		if (opt.containsKey("url"))
			completeUrl = (String) opt.get("url");
		else if (opt.containsKey("resource") && opt.containsKey("id"))
			// if (opt.get("id"))
			// completeUrl =
			// this.url+"/api/"+opt.get("resource")+"/?id=[".implode(',',
			// $options['id'])+"]";
			// else
			completeUrl = this.url + "/api/" + opt.get("resource") + "/" + opt.get("id");

		if (opt.containsKey("id_shop"))
			completeUrl += "&id_shop=" + opt.get("id_shop");
		if (opt.containsKey("id_group_shop"))
			completeUrl += "&id_group_shop=" + opt.get("id_group_shop");

		HttpDelete httpdelete = new HttpDelete(completeUrl);
		HashMap<String, Object> result = this.executeRequest(httpdelete);

		this.checkStatusCode((int) result.get("status_code"));// check the
		// response
		// validity

		return true;
	}

	private String readInputStreamAsString(InputStream in) throws IOException {

		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			byte b = (byte) result;
			buf.write(b);
			result = bis.read();
		}

		String returns = buf.toString();
		return returns;
	}

	public String DocumentToString(Document doc) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
		trans.setOutputProperty(OutputKeys.METHOD, "xml");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));

		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(doc.getDocumentElement());

		trans.transform(source, result);
		String xmlString = sw.toString();

		return xmlString;
	}

}
