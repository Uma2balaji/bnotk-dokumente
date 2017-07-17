package de.wps.brav.migration.dokumente.db;

public class TMPDokumenteVO {
	
	private String dokid; // DOKID
	private String sdsid; // SDSID
	
	
	
	public TMPDokumenteVO(String dokid, String sdsid) {
		super();
		this.dokid = dokid;
		this.sdsid = sdsid;
	}
	
	public String getDokid() {
		return dokid;
	}
	public void setDokid(String dokid) {
		this.dokid = dokid;
	}
	public String getSdsid() {
		return sdsid;
	}
	public void setSdsid(String sdsid) {
		this.sdsid = sdsid;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TMPDokumenteVO [dokid=");
		builder.append(dokid);
		builder.append(", sdsid=");
		builder.append(sdsid);
		builder.append("]");
		return builder.toString();
	}

}
