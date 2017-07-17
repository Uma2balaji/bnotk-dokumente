package de.wps.brav.migration.dokumente.db;

import java.math.BigDecimal;

public class DoktypeValueMapping {

	private BigDecimal doktyp;
	private String DOKUMENTART;
	public DoktypeValueMapping(BigDecimal doktyp, String dOKUMENTART) {
		super();
		this.doktyp = doktyp;
		DOKUMENTART = dOKUMENTART;
	}
	public BigDecimal getDoktyp() {
		return doktyp;
	}
	public void setDoktyp(BigDecimal doktyp) {
		this.doktyp = doktyp;
	}
	public String getDOKUMENTART() {
		return DOKUMENTART;
	}
	public void setDOKUMENTART(String dOKUMENTART) {
		DOKUMENTART = dOKUMENTART;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DoktypeValueMapping [doktyp=");
		builder.append(doktyp.toString());
		builder.append(", DOKUMENTART=");
		builder.append(DOKUMENTART);
		builder.append("]");
		return builder.toString();
	}
	
}
