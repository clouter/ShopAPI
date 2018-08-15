package com.megaele.application;

public class ForTesting {

	public static void main(String[] args) {
		String result="";
		String test = "Venta Online de Placa Vitrocerámica Bosch PKM875DP1D|Megaelectrodomesticos.com";
		String[] tokens = test.split("\\|");
		result = tokens[0].split(" ")[tokens[0].split(" ").length-1];
		System.out.println(result);

	}

}
