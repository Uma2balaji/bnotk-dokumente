package de.wps.brav.migration.dokumente.request;

public class StoreDocumentRequest {
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



}
