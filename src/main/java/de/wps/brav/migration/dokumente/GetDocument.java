package de.wps.brav.migration.dokumente;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import de.bnotk.zvr.common.rest.client.AuthenticationHeader;
import de.wps.brav.migration.dokumente.request.DocumentDetails;

public class GetDocument {

	private static PrintStream ps = null;

	public static void main(String[] args) {

		try {
			ps = new PrintStream(new FileOutputStream("C:/Users/premendra.kumar/Desktop/MeldIDBlobGetDocument.txt"));
			//
		} catch (Exception e) {
			// e.printStackTrace();
			// System.out.println("Error in getting logfile location : "+);

		}

		ps = System.out;

		String[] accesskeys = { "5971d45c7d369f171877bd58", "5971d46a7d369f171877bd68", "5971d46e7d369f171877bd71",
				"5971d47d7d369f171877bd7d", "5971d4807d369f171877bd82", "5971d47a7d369f171877bd77",
				"5971d4897d369f171877bd88", "5971d48b7d369f171877bd8c", "5971d3be7d369f171877bd34",
				"5971d3be7d369f171877bd35" };
		String parentFolderName = "C:/Users/premendra.kumar/Desktop/SDSGetDocument/";

		for (String accessKey : accesskeys) {
			try {
				getDocumentDetails("https://cs-sds-1-test-01.test.bnotk.net:8443/sds/documentstore/v1/", "testuserapp",
						accessKey, "testuser", "password", parentFolderName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Function call to read document from sds
	 * 
	 * @param url
	 * @param userId
	 * @param authUsername
	 * @param authPassword
	 * @return DocumentDetails JSON response of document
	 * @throws IOException
	 */
	public static DocumentDetails getDocumentDetails(String url, String app, String documentAccessKey,
			String authUsername, String authPassword, String parentFolderName) throws IOException {

		DocumentDetails documentResponse = null;
		try {
			// LOGGER.info("Entering into getUserDetails of UserDetailsClient");
			RestTemplate template = new RestTemplate(AuthenticationHeader.httpsRequestFactory());
			HttpEntity<String> request = new HttpEntity<>(
					AuthenticationHeader.getAuthenticationHeader(authUsername, authPassword));
			ResponseEntity<DocumentDetails> response = template.exchange(
					url + "/" + app + "/documents/" + documentAccessKey, HttpMethod.GET, request,
					DocumentDetails.class);

			documentResponse = response.getBody();
		} catch (HttpClientErrorException ex) {
			// LOG.log(new
			// BaseRuntimeException(REGMessageCodeConstants.INTERNAL_SERVER,
			// ex));
			ps.println("DocumentDetails Exception :" + ex.getMessage());
			// documentHandlerException = new DocumentErrorHandler();
			// documentHandlerException.handleDocumentException(documentException,
			// ex.getStatusCode());
			ex.printStackTrace(ps);
		} catch (Exception e) {
			// ps.println(new
			// BaseRuntimeException(REGMessageCodeConstants.INTERNAL_SERVER,
			// e));
			ps.println("DocumentDetails method Main exception :" + e.getMessage());
			e.printStackTrace(ps);
		}
		// Base64.decodeBase64(documentResponse.getContent().getSource());
		FileOutputStream fos = null;
		fos = new FileOutputStream(parentFolderName + documentAccessKey + "_" + System.currentTimeMillis() + ".pdf");
		IOUtils.copy(new ByteArrayInputStream(Base64.decodeBase64(documentResponse.getContent().getSource())), fos);
		fos.flush();
		fos.close();
		return documentResponse;
	}

}
