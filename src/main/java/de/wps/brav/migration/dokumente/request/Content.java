package de.wps.brav.migration.dokumente.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Content {

        // ref to  Base64 coded value of the MD5 checksum 
        private String checksum;
        
     // ref to content of file as Base64 code
        private String source;
    	/**
    	 * @return the source
    	 */
    	public String getSource() {
    		return source;
    	}
    	/**
    	 * @param source the source to set
    	 */
    	public void setSource(String source) {
    		this.source = source;
    	}
    	/**
    	 * @return the checksum
    	 */
    	public String getChecksum() {
    		return checksum;
    	}
    	/**
    	 * @param checksum the checksum to set
    	 */
    	public void setChecksum(String checksum) {
    		this.checksum = checksum;
    	}
    	/* (non-Javadoc)
    	 * @see java.lang.Object#toString()
    	 */
    	@Override
    	public String toString() {
    		return "Content [source=" + source + ", checksum=" + checksum + "]";
    	} 
    }

