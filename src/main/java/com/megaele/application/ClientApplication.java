package com.megaele.application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.megaele.prices.PriceComparator;
import com.megaele.references.References;
import com.megaele.request.PSRequests;
import com.megaele.webservice.PSWebServiceClient;

/**
 *
 * Main application
 * 
 * @author rpila
 * 
 * @version 1.0 - 04/04/2018
 *
 */
public class ClientApplication {

	final static Logger logger = Logger.getLogger(ClientApplication.class);

	static PSRequests psRequests = new PSRequests();
	static PriceComparator priceComparator = new PriceComparator();
	static References references = new References();
	static String PRODUCT_URL = "http://megaelectrodomesticos.com/api/products/";
	static String PRODUCTS_URL = "http://www.megaelectrodomesticos.com/api/products";

	public static void main(String[] args) {
		Instant start = Instant.now();
		try {

			//String[] proxyUrl = new ProxyFactory().getProxy();
			PSWebServiceClient ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
			List<String> productIds = psRequests.getProductAttribute(ws, "id", PRODUCTS_URL);

			updatePrices(ws, productIds);

			Instant end = Instant.now();
			logger.info("Duration of the application: " + Duration.between(start, end));
		} catch (Exception e) {
			logger.error("Error on the main application", e);
			e.printStackTrace();
		}

	}

	/**
	 * All related with prices
	 * 
	 * @param ws
	 * @param productIds
	 * @throws Exception
	 */
	private static void updatePrices(PSWebServiceClient ws, List<String> productIds) throws Exception {
		for (String id: productIds) {
			logger.info("ID: " + id);	
			List<String> referencesList = references.getSpecificReferences();
			Map<String, String> refsAndPrices = new HashMap<String, String>();
			refsAndPrices = psRequests.getProductRefAndPriceFromMetaTitle(ws,PRODUCT_URL+id);
			for (Entry<String, String> entry: refsAndPrices.entrySet()) {
				String reference = entry.getKey();
				String value = entry.getValue();
				logger.info("Evaluating " + reference);
				Optional<String> matched = referencesList.stream().filter(refFromFile->reference.equals(refFromFile)).findFirst();
				updatePricesFromOurCompetitorsPriceExcludingReferences(ws, productIds, id, reference, value, matched);
//				updatePricesForSpecificReferences(ws, productIds, id);
			}
		}
	}

	/**
	 * Update the whole product stack in site using the competitor's price var
	 */
	private static void updatePricesFromOurCompetitorsPriceExcludingReferences(PSWebServiceClient ws, List<String> productIds, String id, 
			String reference, String value, Optional<String> matched) throws Exception {
		if (!matched.isPresent()) {
			String competitorPrice =  priceComparator.getPricefromCompetitors(reference);
			if (competitorPrice != null && !competitorPrice.isEmpty()) {
				Float finalPrice = priceComparator.calculatePrice(value, competitorPrice);
				logger.info(reference + "| Our Price " + value + "| Competitors Price " + competitorPrice + "| Our Final Price " + finalPrice);
				if (finalPrice.compareTo(Float.valueOf(value)) != 0) {
					psRequests.updatePrice(ws, PRODUCT_URL+id, finalPrice);
					logger.info(PRODUCT_URL+id + " price has been updated!");
					ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
				}

			}
		} else {
			logger.info("Reference: " + reference + " skipped.");
		}
	}


	/**
	 * Just update the price of the products with references in src/main/resources/references.properties
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	private static void updatePricesForSpecificReferences(PSWebServiceClient ws, List<String> productIds, String id,
			String reference, String value, Optional<String> matched) throws Exception {
		if (matched.isPresent()) {
			logger.info("There is match with: " + matched);
			psRequests.updatePriceFromModifier(ws, PRODUCT_URL+id, (float) 30.0);
			logger.info(PRODUCT_URL+id + " price has been updated!");
			ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
		}
	}
}
