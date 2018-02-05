package com.megaele.prices;

import com.jaunt.Element;
import com.jaunt.JauntException;
import com.jaunt.UserAgent;

public class PriceComparator {

	public String getPricefromCompetitors(String reference) {

		String competitorsPrice = "price from competitors not found"; 
		try{
			UserAgent userAgent = new UserAgent();                       
			userAgent.settings.autoSaveAsHTML = true;                    

			userAgent.visit("http://idealo.es/resultados.html?q=" + reference);
			Element priceSpan = userAgent.doc.findFirst("<span class='priceRange-from'>");

			competitorsPrice = priceSpan.getText().trim();

		} catch(JauntException e) { 
			System.err.println(e);         
		} finally {
			return competitorsPrice;
		}
	}
}
