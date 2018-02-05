package com.megaele.application;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.megaele.prices.PriceComparator;
import com.megaele.request.PSRequests;
import com.megaele.webservice.PSWebServiceClient;

public class ClientApplication {

	static PSRequests psRequests = new PSRequests();
	static PriceComparator priceComparator = new PriceComparator();
	static String PRODUCT_URL = "http://megaelectrodomesticos.com/api/products/465";
	
	public static void main(String[] args) {
		try {
			
			Instant start = Instant.now();
			
			PSWebServiceClient ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);

//			psRequests.disableProduct(ws, PRODUCT_URL);
//			psRequests.refillStock(ws);

			List<String> productIds = psRequests.getProductAttribute(ws, "id");
			
			Map<String, String> refsAndPrices = new HashMap<String, String>();
			for (String id: productIds) {
				refsAndPrices = psRequests.getProductRefAndPrice(ws, id);
				for (Entry<String, String> entry: refsAndPrices.entrySet()) {
					System.out.println(entry.getKey() + "| Our Price " + entry.getValue() + "| Competitors Price " + priceComparator.getPricefromCompetitors(entry.getKey()));
				}
			}
			
			
			Instant end = Instant.now();
			
			System.out.println("Duration of the application: " + Duration.between(start, end));

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

}
