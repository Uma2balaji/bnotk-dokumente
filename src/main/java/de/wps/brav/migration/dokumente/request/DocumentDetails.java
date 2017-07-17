package de.wps.brav.migration.dokumente.request;

public class DocumentDetails {

    // ref to attributes class
    private Attributes attributes;

    // ref to content class
    private Content content;

    public Attributes getAttributes() {
		return attributes;
	}

	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	public String getDocument_access_key() {
		return document_access_key;
	}

	public void setDocument_access_key(String document_access_key) {
		this.document_access_key = document_access_key;
	}



	// ref to document_access_key class
    private String document_access_key;
    
    // ref to applicationSpecificData
   
}
