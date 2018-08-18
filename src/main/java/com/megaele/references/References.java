package com.megaele.references;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class References {

	private Stream<String> stream;

	
	public List<String> getSpecificReferences() {
		List<String> referencesList = new ArrayList<String>();
		try {
			referencesList = readFile();
		}catch (Exception e){
			e.printStackTrace();
		}
		return referencesList;
	}
	
	public List<String> readFile() throws IOException {
		String fileName = "src/main/resources/references.properties";
		stream = Files.lines(Paths.get(fileName));
		return stream.collect(Collectors.toList());
	}
}
