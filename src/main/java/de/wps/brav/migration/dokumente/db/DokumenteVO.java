package de.wps.brav.migration.dokumente.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
// import java.math.BigInteger;
import java.sql.Blob;
import java.util.Arrays;
import java.util.Date;

public class DokumenteVO {

	private String dokid; // DOKID
	private BigDecimal doktyp; // DOKTYP
	private Date cretms;// CRETMS
	private Date drucktms;// DRUCKTMS
	private boolean alfrescokz;// ALFRESCOKZ
	private Date gelesentms;// GELESENTMS
	private String meldid;// MELDID
	private String vmid;// VMID
	private BigDecimal anzseiten;// ANZSEITEN
	private BigDecimal bevollmnr;// BEVOLLMNR
	private String dokbez;// DOKBEZ
	private Blob dokdata; // DOKDATA
	private String landkz; // LANDKZ
	private boolean status; //STATUS
	private String dokumentart;//DOKUMENTART

	// private BigDecimal meldnr;
	private byte[] cryptDataByteArray;
	private byte[] decryptDataByteArray;

	private ByteArrayInputStream byteArrayInputStream;
	private InputStream inputStream;
	private ByteArrayOutputStream byteArrayOutputStream;

	// public DokumenteVO(String dokid, Blob dokdata,String landkz,String
	// meldid) {
	// super();
	// this.dokid = dokid;
	// this.dokdata = dokdata;
	// this.landkz = landkz;
	// this.meldid=meldid;
	// }

	public DokumenteVO() {
		super();
	}

	public DokumenteVO(String dokid, BigDecimal doktyp, Date cretms, Date drucktms, boolean alfrescokz, Date gelesentms,
			String meldid, String vmid, BigDecimal anzseiten, BigDecimal bevollmnr, String dokbez, Blob dokdata,
			String landkz) {
		super();
		this.dokid = dokid;
		this.doktyp = doktyp;
		this.cretms = cretms;
		this.drucktms = drucktms;
		this.alfrescokz = alfrescokz;
		this.gelesentms = gelesentms;
		this.meldid = meldid;
		this.vmid = vmid;
		this.anzseiten = anzseiten;
		this.bevollmnr = bevollmnr;
		this.dokbez = dokbez;
		this.dokdata = dokdata;
		this.landkz = landkz;
	}

	public ByteArrayInputStream getByteArrayInputStream() {
		return byteArrayInputStream;
	}

	public void setByteArrayInputStream(ByteArrayInputStream byteInputStream) {
		this.byteArrayInputStream = byteInputStream;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public ByteArrayOutputStream getByteArrayOutputStream() {
		return byteArrayOutputStream;
	}

	public void setByteArrayOutputStream(ByteArrayOutputStream byteArrayOutputStream) {
		this.byteArrayOutputStream = byteArrayOutputStream;
	}

	public byte[] getCryptDataByteArray() {
		return cryptDataByteArray;
	}

	public void setCryptDataByteArray(byte[] cryptDataByteArray) {
		this.cryptDataByteArray = cryptDataByteArray;
	}

	public String getDokid() {
		return dokid;
	}

	public void setDokid(String dokid) {
		this.dokid = dokid;
	}

	public Blob getDokdata() {
		return dokdata;
	}

	public void setDokdata(Blob dokdata) {
		this.dokdata = dokdata;
	}

	public String getLandkz() {
		return landkz;
	}

	public void setLandkz(String landkz) {
		this.landkz = landkz;
	}

	public String getMeldid() {
		return meldid;
	}

	public void setMeldid(String meldid) {
		this.meldid = meldid;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DokumenteVO [dokid=");
		builder.append(dokid);
		builder.append(", doktyp=");
		builder.append(doktyp);
		builder.append(", cretms=");
		builder.append(cretms);
		builder.append(", drucktms=");
		builder.append(drucktms);
		builder.append(", alfrescokz=");
		builder.append(alfrescokz);
		builder.append(", gelesentms=");
		builder.append(gelesentms);
		builder.append(", meldid=");
		builder.append(meldid);
		builder.append(", vmid=");
		builder.append(vmid);
		builder.append(", anzseiten=");
		builder.append(anzseiten);
		builder.append(", bevollmnr=");
		builder.append(bevollmnr);
		builder.append(", dokbez=");
		builder.append(dokbez);
		builder.append(", dokdata=");
		builder.append(dokdata);
		builder.append(", landkz=");
		builder.append(landkz);
		builder.append(", cryptDataByteArray=");
		builder.append(Arrays.toString(cryptDataByteArray));
		builder.append(", byteArrayInputStream=");
		builder.append(byteArrayInputStream);
		builder.append(", inputStream=");
		builder.append(inputStream);
		builder.append(", byteArrayOutputStream=");
		builder.append(byteArrayOutputStream);
		builder.append("]");
		return builder.toString();
	}

	public BigDecimal getDoktyp() {
		return doktyp;
	}

	public void setDoktyp(BigDecimal doktyp) {
		this.doktyp = doktyp;
	}

	public Date getCretms() {
		return cretms;
	}

	public void setCretms(Date cretms) {
		this.cretms = cretms;
	}

	public Date getDrucktms() {
		return drucktms;
	}

	public void setDrucktms(Date drucktms) {
		this.drucktms = drucktms;
	}

	public boolean getAlfrescokz() {
		return alfrescokz;
	}

	public void setAlfrescokz(boolean alfrescokz) {
		this.alfrescokz = alfrescokz;
	}

	public Date getGelesentms() {
		return gelesentms;
	}

	public void setGelesentms(Date gelesentms) {
		this.gelesentms = gelesentms;
	}

	public String getVmid() {
		return vmid;
	}

	public void setVmid(String vmid) {
		this.vmid = vmid;
	}

	public BigDecimal getAnzseiten() {
		return anzseiten;
	}

	public void setAnzseiten(BigDecimal anzseiten) {
		this.anzseiten = anzseiten;
	}

	public BigDecimal getBevollmnr() {
		return bevollmnr;
	}

	public void setBevollmnr(BigDecimal bevollmnr) {
		this.bevollmnr = bevollmnr;
	}

	public String getDokbez() {
		return dokbez;
	}

	public void setDokbez(String dokbez) {
		this.dokbez = dokbez;
	}

	public boolean getStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getDokumentart() {
		return dokumentart;
	}

	public void setDokumentart(String dokumentart) {
		this.dokumentart = dokumentart;
	}

	public byte[] getDecryptDataByteArray() {
		return decryptDataByteArray;
	}

	public void setDecryptDataByteArray(byte[] decryptDataByteArray) {
		this.decryptDataByteArray = decryptDataByteArray;
	}

}
