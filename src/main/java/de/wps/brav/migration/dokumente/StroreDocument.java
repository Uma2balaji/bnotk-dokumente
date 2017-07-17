package de.wps.brav.migration.dokumente;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.bnotk.zvr.interfaces.sds.storedocument.Attributes;
import de.bnotk.zvr.interfaces.sds.storedocument.Content;
import de.bnotk.zvr.interfaces.sds.storedocument.DocumentDetails;
import de.bnotk.zvr.interfaces.sds.storedocument.SdsEncoding;
import de.bnotk.zvr.interfaces.sds.storedocument.StoreDocument;
import de.bnotk.zvr.interfaces.sds.storedocument.StoreDocumentRequest;

public class StroreDocument {

	// public static DocumentDetails uploadDocument(String url, String app,
	// StoreDocumentRequest documentDetails,
	// String authUsername, String authPassword) {
	//
	//// LOGGER.info("Entering into addUser method of AddUserClient");
	// try {
	//
	// ObjectMapper mapper = new ObjectMapper();
	// String serializedObj = mapper.writeValueAsString(documentDetails);
	// System.out.println(serializedObj);
	// HttpEntity<String> requestEntity = new HttpEntity<>(serializedObj,
	// AuthenticationHeader.getAuthenticationHeader(authUsername,
	// authPassword));
	// RestTemplate templte = new
	// RestTemplate(AuthenticationHeader.httpsRequestFactory());
	// templte.getMessageConverters().add(new
	// MappingJackson2HttpMessageConverter());
	// templte.getMessageConverters().add(0, new
	// StringHttpMessageConverter(Charset.forName("UTF-8")));
	// ResponseEntity<DocumentDetails> response = templte.exchange(url + "/" +
	// app + "/" + "documents/",
	// HttpMethod.POST, requestEntity, DocumentDetails.class);
	//
	// documentResponse = response.getBody();
	// } catch (HttpClientErrorException ex) {
	// LOG.log(new BaseRuntimeException(REGMessageCodeConstants.INTERNAL_SERVER,
	// ex));
	// LOGGER.info("UpdateUserClient Exception :" + ex.getMessage());
	// userhandleException = new DocumentErrorHandler();
	// userhandleException.handleDocumentException(userException,
	// ex.getStatusCode());
	// } catch (Exception e) {
	// LOG.log(new BaseRuntimeException(REGMessageCodeConstants.INTERNAL_SERVER,
	// e));
	// LOGGER.info("UpdateUserClient method Main exception :" + e.getMessage());
	// }
	// return documentResponse;
	// }

	public static void main(String[] arg) {
		uploadDocument(
				"C:/Users/premendra.kumar/Desktop/Old-Desktop/Dokument/DocumentsforTest/Docs/output/2c7f3529-10d1-484f-b021-3bafb701a00f.pdf");
	}

	private static void uploadDocument(String fileName) {

		String url = "https://cs-sds-1-test-01.test.bnotk.net:8443/sds/documentstore/v1/";
		String app = "testuserapp";
		String authUsername = "testuser";
		String authPassword = "password";
		// String fileName =
		// "C:/Users/premendra.kumar/Desktop/Old-Desktop/Dokument/DocumentsforTest/Docs/output/2c7f3529-10d1-484f-b021-3bafb701a00f.pdf";
		// // args[0];
		StoreDocumentRequest storeDocumentRequest = new StoreDocumentRequest();
		try {

			Date date3 = Calendar.getInstance().getTime();
			SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");

			java.sql.Date date1 = null;

			try {
				date1 = new java.sql.Date(df.parse(df.format(date3)).getTime());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(date1);
			// file name replace with byte array of docgen
			storeDocumentRequest.setContent(new Content());
			storeDocumentRequest.getContent().setSource(SdsEncoding.generateBase64(fileName));
			System.out.println(storeDocumentRequest.getContent().getSource());
			storeDocumentRequest.getContent().setChecksum(SdsEncoding.generateMd5(fileName));
			System.out.println(storeDocumentRequest.getContent().getChecksum());
			storeDocumentRequest.setAttributes(new Attributes());
			storeDocumentRequest.getAttributes().setOwner("docgen");
			storeDocumentRequest.getAttributes().setTitle("ZVR Test");
			storeDocumentRequest.getAttributes().setEncryption("none");
			Date d = new Date();

			try {
				// d=sdf.parse("2016-12-07");
				storeDocumentRequest.getAttributes().setCreatedAt(date1);
				// d=sdf.parse("9999-12-31");
				storeDocumentRequest.getAttributes().setValidTo(date1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			storeDocumentRequest.getAttributes().setFileFormat("pdf");
			storeDocumentRequest.getAttributes().setFileName("DemoGraphicStateAgeWisePM.pdf");
			storeDocumentRequest.getAttributes().setBusinessKey("//templates//docgen//DemoGraphicStateAgeWise.pdf");
			storeDocumentRequest.getAttributes().setGroup("templates");
			DocumentDetails res = StoreDocument.uploadDocument(url, app, storeDocumentRequest, authUsername,
					authPassword);
			if (res != null)
				System.out.println(res.getDocument_access_key());
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("DONE");

	}

}
