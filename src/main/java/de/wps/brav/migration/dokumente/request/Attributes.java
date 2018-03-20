package de.wps.brav.migration.dokumente.request;

import java.util.Date;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attributes {
	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the encryption
	 */
	public String getEncryption() {
		return encryption;
	}

	/**
	 * @param encryption
	 *            the encryption to set
	 */
	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}

	/**
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @param createdAt
	 *            the createdAt to set
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * @return the validTo
	 */
	public Date getValidTo() {
		return validTo;
	}

	/**
	 * @param validTo
	 *            the validTo to set
	 */
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	/**
	 * @return the fileFormat
	 */
	public String getFileFormat() {
		return fileFormat;
	}

	/**
	 * @param fileFormat
	 *            the fileFormat to set
	 */
	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the businessKey
	 */
	public String getBusinessKey() {
		return businessKey;
	}

	/**
	 * @param businessKey
	 *            the businessKey to set
	 */
	public void setBusinessKey(String businessKey) {
		this.businessKey = businessKey;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @param group
	 *            the group to set
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment
	 *            the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	// ref to document owner
	private String owner;
	// ref to document title
	private String title;
	// ref to document encryption
	private String encryption;
	// ref to document encryption
	private Date createdAt;
	// ref to document encryption
	private Date validTo;
	// ref to document encryption
	private String fileFormat;
	// ref to document encryption
	private String fileName;
	// ref to document encryption
	private String businessKey;
	// ref to document encryption
	private String group;
	// ref to document encryption
	private String comment;
	/*
	 * private ApplicationSpecificData applicationSpecificData; public
	 * ApplicationSpecificData getApplicationSpecificData() { return
	 * applicationSpecificData; } public void
	 * setApplicationSpecificData(ApplicationSpecificData
	 * applicationSpecificData) { this.applicationSpecificData =
	 * applicationSpecificData; }
	 */

	private LinkedHashMap<String, Object> applicationSpecificData = new LinkedHashMap<String, Object>();
	@JsonAnyGetter
	public LinkedHashMap<String, Object> getApplicationSpecificData() {
		return applicationSpecificData;
	}
	@JsonAnySetter
	public void setApplicationSpecificData(LinkedHashMap<String, Object> applicationSpecificData) {
		this.applicationSpecificData = applicationSpecificData;
	}

}
