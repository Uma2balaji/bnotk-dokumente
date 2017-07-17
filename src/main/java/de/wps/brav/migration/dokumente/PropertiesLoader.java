package de.wps.brav.migration.dokumente;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class PropertiesLoader {
	
	
	public static Properties loadProperties(String filename) {
		Properties props = new Properties();
		try{
			FileInputStream fi = new FileInputStream(filename);
			props.load(fi);
			fi.close();	
		}catch (IOException e) {
			e.printStackTrace();
		}
		return props;
	}
}
