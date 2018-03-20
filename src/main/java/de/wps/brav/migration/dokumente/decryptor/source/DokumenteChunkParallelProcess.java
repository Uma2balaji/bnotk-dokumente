package de.wps.brav.migration.dokumente.decryptor.source;

import java.beans.PropertyVetoException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.bnotk.zvr.common.rest.client.AuthenticationHeader;
import de.bnotk.zvr.interfaces.sds.storedocument.SdsEncoding;
import de.wps.brav.migration.dokumente.PropertiesLoader;
import de.wps.brav.migration.dokumente.db.DataSource;
import de.wps.brav.migration.dokumente.db.DoktypeValueMapping;
import de.wps.brav.migration.dokumente.db.DokumenteVO;
import de.wps.brav.migration.dokumente.db.TMPDokumenteVO;
import de.wps.brav.migration.dokumente.request.Attributes;
import de.wps.brav.migration.dokumente.request.Content;
import de.wps.brav.migration.dokumente.request.DocumentDetails;
import de.wps.brav.migration.dokumente.request.StoreDocumentRequest;

public class DokumenteChunkParallelProcess {

	private static Connection sourceConnection = null;
	private static Connection targetConnection = null;
	private static boolean detailedLogsEnabled = true;
	private static DateFormat dfOut = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private static Properties props;

	private ExecutorService readAndProcessExecutor = Executors.newFixedThreadPool(40);
	private ExecutorService writeExecutor = Executors.newFixedThreadPool(100);
	private static int sizeOfTheReadingChunk = 50;
	private static int sizeOfTheWritingChunk = 25;
	private static int numberOfRecordsReadFromDB = 100;
	private static int executorThreadPoolSize = 40;
	private static String selectTotalDokCountQuery = null;
	private static String updateSDSIDPreparedStatementSQL = null;
	private static String updateSDSIDStatusPreparedStatementSQL = null;
	private static String selectCryptDataAndIdPagingQuery = null;
	private static String selectDoktypeVmData = null;

	private static String sdsRestServerURL = null;
	private static String sdsRestServerApp = null;
	private static String sdsRestServerUser = null;
	private static String sdsRestServerPassword = null;

	private static boolean rerunable = false;

	private static List<DoktypeValueMapping> listDoktypeValueMapping = new ArrayList<>();

	private static PrintStream ps = null;

	private static void close() {
		try {
			if (targetConnection != null) {
				targetConnection.close();
			}
			if (sourceConnection != null) {
				sourceConnection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Inizialisiert das Properties-Object aus der Properties-datei
	 */
	private static void initialisiere(String[] args) {
		String propertiesfilePathName = args[0];
		System.out.println(dfOut.format(new Date()) + " > " + " INFO  Properties Datei " + propertiesfilePathName
				+ " wird geladen");
		props = PropertiesLoader.loadProperties(propertiesfilePathName);
		System.out.println(dfOut.format(new Date()) + " > " + " INFO  Properties Datei " + propertiesfilePathName
				+ " wurde erfolgreich geladen");
	}

	private static void doInitialSettings() {
		/** Initial settings - start */

		rerunable = "true".equalsIgnoreCase(props.getProperty("rerunable"));

		executorThreadPoolSize = Integer.parseInt(props.getProperty("ReadAndProcessExecutorsThreadPoolSize"));

		sizeOfTheReadingChunk = Integer.parseInt(props.getProperty("sizeOfTheReadingChunkFromDokumente"));

		sizeOfTheWritingChunk = Integer.parseInt(props.getProperty("sizeOfTheWritingChunkIntoDokumente"));

		numberOfRecordsReadFromDB = Integer.parseInt(props.getProperty("NumberOfRecordsToBeReadFromDokumente"));

		if (rerunable) {
			selectCryptDataAndIdPagingQuery = props.getProperty("SelectCryptDataAndIdPagingQuery_status_column");
			selectTotalDokCountQuery = props.getProperty("selectTotalDokCountQuery_status_column");
			updateSDSIDPreparedStatementSQL = props.getProperty("updateSDSIDQuery_status_column");
		} else {
			selectCryptDataAndIdPagingQuery = props.getProperty("SelectCryptDataAndIdPagingQuery");
			selectTotalDokCountQuery = props.getProperty("selectTotalDokCountQuery");
			updateSDSIDPreparedStatementSQL = props.getProperty("updateSDSIDQuery");
		}

		updateSDSIDStatusPreparedStatementSQL = props
				.getProperty("updateSDSIDStatusPreparedStatementSQL_status_column");

		sdsRestServerURL = props.getProperty("SDSRestServerURL");

		sdsRestServerApp = props.getProperty("SDSRestServerApp");

		sdsRestServerUser = props.getProperty("SDSRestServerUser");

		sdsRestServerPassword = props.getProperty("SDSRestServerPassword");

		selectDoktypeVmData = props.getProperty("selectDoktypeVmData");

		detailedLogsEnabled = "1".equalsIgnoreCase(props.getProperty("EnableDetailedLogs"));

		DataSource.setProps(props);

		/** Initial settings - end */
	}

	private static void entryForProcessing() {

		Date startTime = new Date();
		Date endTime = null;
		int totalMeldCount = getDokCount();

		listDoktypeValueMapping = fetchAllDokTypeValueMapping();
		ps.println(dfOut.format(new Date()) + " > " + "listDoktypeValueMapping : " + listDoktypeValueMapping);

		int totalIteration = (totalMeldCount / numberOfRecordsReadFromDB)
				+ ((totalMeldCount % numberOfRecordsReadFromDB) > 0 ? 1 : 0);

		ps.println("\n*******\n" + dfOut.format(new Date()) + " > " + " TRP = Total records processed "
				+ " | ETTM = Elapsed total time in minutes " + " | ARP = Actual Record Processed "
				+ " | ETAS = Elapsed time actual recordset in seconds " + " | RPM = Records Per Minute "
				+ "\n***********\n");

		// if (detailedLogsEnabled)
		ps.println(dfOut.format(new Date()) + " > " + " Total Dokument Count == " + totalMeldCount
				+ " Total Iteration == " + totalIteration
				+ " Number Of Records To Be Read From Table In One Iteration == " + numberOfRecordsReadFromDB);

		/**
		 * 1. Read All dokumente
		 * 
		 * 2. process/decrypt data one by one
		 * 
		 * 3. create request object with all required data in json as given
		 * format
		 * 
		 * 4. send web service request one by one
		 * 
		 * 5. recieve response and accumulate them
		 * 
		 * 6. write/update the access key in transformation table against which
		 * id
		 */

		for (int iteration = 0; iteration < totalIteration; iteration++) {
			DokumenteChunkParallelProcess bnotkObj = new DokumenteChunkParallelProcess();
			bnotkObj.intermediate(iteration, numberOfRecordsReadFromDB, executorThreadPoolSize, startTime);
		}

		endTime = new Date();

		long milli = (endTime.getTime() - startTime.getTime());

		double totalTimeInMin = (((double) milli / 1000) / 60);
		double averageSpeed = ((double) totalMeldCount / totalTimeInMin);

		// if(detailedLogsEnabled)
		ps.printf(dfOut.format(new Date()) + " > "
				+ "\n Total Records Processed : %d | Total Time Elapsed : %.3f Minutes | Average Speed : %.3f Records/Min."
				+ "\n", totalMeldCount, totalTimeInMin, averageSpeed);

	}

	private static List<DoktypeValueMapping> fetchAllDokTypeValueMapping() {
		List<DoktypeValueMapping> list = new ArrayList<DoktypeValueMapping>();

		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
				PreparedStatement statement = sourceConnection.prepareStatement(selectDoktypeVmData);) {

			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				DoktypeValueMapping dokvm = new DoktypeValueMapping(rs.getBigDecimal("DOKTYP"),
						rs.getString("DOKUMENTART"));
				list.add(dokvm);
			}
		} catch (SQLException e) {
			e.printStackTrace(ps);
		} catch (IOException e) {
			e.printStackTrace(ps);
		} catch (PropertyVetoException e) {
			e.printStackTrace(ps);
		}

		return list;
	}

	private int iteration;

	private void intermediate(int iterationn, int range, int executorThreadPoolSize, Date totalProcessStartTime) {

		this.iteration = iterationn;

		Date iterationStartTime = new Date();

		readAndProcessExecutor = Executors.newFixedThreadPool(executorThreadPoolSize);

		writeExecutor = Executors
				.newFixedThreadPool(Integer.parseInt(props.getProperty("WriteExecutorsThreadPoolSize")));

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > " + "iteration == " + iteration
					+ " iteration*range == new offset == " + (iteration * range) + " range == " + range);

		int partsOfList = (range / sizeOfTheReadingChunk) + ((range % sizeOfTheReadingChunk) > 0 ? 1 : 0);

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > Number Of Records To Be Read From Meldungen == " + range
					+ " sizeOfTheReadingChunk == " + sizeOfTheReadingChunk + " partsOfList == " + partsOfList);

		int[] parts = new int[partsOfList];
		int total = 0;
		for (int i = 0; i < partsOfList; i++) {
			parts[i] = sizeOfTheReadingChunk;
			total += parts[i];
			if (total >= range) {
				parts[i] = sizeOfTheReadingChunk - (total - range);
			}
		}

		for (int i = 0; i < parts.length; i++) {

			final int a = i;
			final int[] partss = parts;

			Runnable worker = new Runnable() {
				@Override
				public void run() {
					int calculatedOffset = 0;
					if (!rerunable) {
						calculatedOffset = (iteration * range) + a * sizeOfTheReadingChunk;
					} else {
						/** Changing logic for rerunnability feature */
						calculatedOffset = a * sizeOfTheReadingChunk;
					}

					int calculatedRange = partss[a];
					if (detailedLogsEnabled)
						ps.println(dfOut.format(new Date()) + " > calculatedOffset == " + calculatedOffset
								+ " calculatedRange == " + calculatedRange);
					processAndWrite(calculatedOffset, calculatedRange, iterationStartTime, totalProcessStartTime);
				}
			};
			readAndProcessExecutor.execute(worker);
		}

		readAndProcessExecutor.shutdown();

		boolean b = true;
		while (!writeExecutor.isTerminated()) {

			if (b && (readAndProcessExecutor.isTerminated())) {
				writeExecutor.shutdown();
				b = false;
			}
		}

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > iteration == " + iteration + "Finished all threads");

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > Processing of data from offset " + iteration * range
					+ " and range " + range + " has been started. ");

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > New offset is :- " + (iteration + 1) * range);

		// boolean printResponses=detailedLogsEnabled;
		boolean printResponses = true;

		if (printResponses && (failedresponses != null) && (failedresponses.size() > 0)) {
			ps.println(dfOut.format(new Date()) + " > " + "failedresponses " + failedresponses.size());
			for (String str : failedresponses) {
				ps.println(dfOut.format(new Date()) + " > " + str);
			}
		}

		if (detailedLogsEnabled) {
			ps.println(dfOut.format(new Date()) + " > " + "Passed responses "
					+ ((passedresponses != null) ? passedresponses.size() : 0));
			for (String str : passedresponses) {
				ps.println(dfOut.format(new Date()) + " > " + str);
			}
		}

		if (detailedLogsEnabled) {
			ps.println(dfOut.format(new Date()) + " > " + "passedResponsesReq "
					+ ((passedResponsesReq != null) ? passedResponsesReq.size() : 0));
			for (String str : passedResponsesReq) {
				ps.println(dfOut.format(new Date()) + " > " + str);
			}
		}

		if (printResponses && (nullResponseReq != null) && (nullResponseReq.size() > 0)) {
			ps.println(dfOut.format(new Date()) + " > " + "nullResponseReq " + nullResponseReq.size());
			for (String str : nullResponseReq) {
				ps.println(dfOut.format(new Date()) + " > " + str);
			}
		}

	}

	private boolean processAndWrite(int offset, int range, Date iterationStartTime, Date totalProcessStartTime) {

		boolean completed = false;

		Date readingStartTime = new Date();

		List<DokumenteVO> mvlist = read(offset, range);

		Date readingEndTime = new Date();

		long totalTimeInMillSecInReading = (readingEndTime.getTime() - readingStartTime.getTime());

		ps.println(dfOut.format(new Date()) + " > Time elapsed to read "
				+ ((mvlist != null && mvlist.size() > 0) ? mvlist.size() : 0) + " records == "
				+ ((double) totalTimeInMillSecInReading / 1000) + " seconds. ");

		if (mvlist == null) {
			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + "mv is null");
			return completed;
		}

		if (mvlist.size() <= 0) {
			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + "mvlist size is zero");
			return completed;
		}

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > " + Thread.currentThread().getName()
					+ " Processing started for = " + mvlist.size() + " records. ");

		Date processingStartTime = new Date();

		List<DokumenteVO> listDokumenteVO = new ArrayList<DokumenteVO>();

		for (DokumenteVO mv : mvlist) {
			DokumenteVO mvWithStream = process(mv);
			listDokumenteVO.add(mvWithStream);
		}

		Date processingEndTime = new Date();

		long totalTimeInMillSecInProcessing = (processingEndTime.getTime() - processingStartTime.getTime());

		ps.println(dfOut.format(new Date()) + " > Time Elapsed To Process "
				+ ((mvlist != null && mvlist.size() > 0) ? mvlist.size() : 0) + " records == "
				+ ((double) totalTimeInMillSecInProcessing / 1000) + " seconds. ");

		/** After processing the data */

		List<TMPDokumenteVO> keyList = new ArrayList<TMPDokumenteVO>();

		for (DokumenteVO objDokumenteVO : listDokumenteVO) {
			String accessKey = uploadDocument(objDokumenteVO);
			passedresponses.add("{" + "\"dokid\":" + "\"" + objDokumenteVO.getDokid() + "\",\"accessKey\":" + "\""
					+ accessKey + "\"" + "}");
			keyList.add(new TMPDokumenteVO(objDokumenteVO.getDokid(), accessKey));
		}

		///////

		int partsOfList = (keyList.size() / sizeOfTheWritingChunk)
				+ ((keyList.size() % sizeOfTheWritingChunk) > 0 ? 1 : 0);
		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > keyList.size() == " + keyList.size()
					+ " sizeOfTheWritingChunk == " + sizeOfTheWritingChunk + " partsOfList == " + partsOfList);
		for (int i = 0; i < partsOfList; i++) {
			final int a = i;

			Runnable worker = new Runnable() {
				@Override
				public void run() {
					int startIndex = a * sizeOfTheWritingChunk;
					int lastIndex = startIndex + sizeOfTheWritingChunk;

					if (lastIndex >= keyList.size()) {
						lastIndex = keyList.size();
					}

					if (detailedLogsEnabled)
						ps.println(dfOut.format(new Date()) + " > " + a + ". keyList.size() == " + keyList.size()
								+ " startIndex == " + startIndex + " lastIndex == " + lastIndex);
					write(keyList.subList(startIndex, lastIndex), totalProcessStartTime);
				}
			};
			writeExecutor.execute(worker);
		}

		return completed;
	}

	private static int getDokCount() {
		int count = 0;
		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
				PreparedStatement statement = sourceConnection.prepareStatement(selectTotalDokCountQuery);) {

			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				count = rs.getInt("TOTAL_COUNT");
			}
		} catch (SQLException e) {
			e.printStackTrace(ps);
		} catch (IOException e) {
			e.printStackTrace(ps);
		} catch (PropertyVetoException e) {
			e.printStackTrace(ps);
		}
		return count;
	}

	private List<DokumenteVO> read(int offset, int range) {

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > Reading data from offset == " + offset + " range == " + range);

		List<DokumenteVO> listDokumenteVO = new ArrayList<DokumenteVO>();
		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
				PreparedStatement statement = sourceConnection.prepareStatement(selectCryptDataAndIdPagingQuery);) {

			statement.setInt(1, offset + range);
			statement.setInt(2, offset);
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				String dokid = rs.getString("DOKID");
				BigDecimal doktyp = rs.getBigDecimal("DOKTYP");
				Date cretms = rs.getDate("CRETMS");
				Date drucktms = rs.getDate("DRUCKTMS");
				boolean alfrescokz = rs.getBoolean("ALFRESCOKZ");
				Date gelesentms = rs.getDate("GELESENTMS");
				String meldid = rs.getString("MELDID");
				String vmid = rs.getString("VMID");
				BigDecimal anzseiten = rs.getBigDecimal("ANZSEITEN");
				BigDecimal bevollmnr = rs.getBigDecimal("BEVOLLMNR");
				String dokbez = rs.getString("DOKBEZ");
				Blob dokdata = rs.getBlob("DOKDATA");
				String landkz = rs.getString("LANDKZ");
				boolean status = false;
				if (rerunable) {
					status = rs.getBoolean("STATUS");
				}

				String dokumentart = rs.getString("DOKUMENTART");

				DokumenteVO objDokumenteVO = new DokumenteVO(dokid, doktyp, cretms, drucktms, alfrescokz, gelesentms,
						meldid, vmid, anzseiten, bevollmnr, dokbez, dokdata, landkz);
				objDokumenteVO.setStatus(status);
				objDokumenteVO.setDokumentart((dokumentart != null) ? dokumentart : "-");

				byte[] blobByteArray = null;
				if (dokdata != null) {
					blobByteArray = dokdata.getBytes((long) 1, (int) (dokdata.length()));
				}

				objDokumenteVO.setCryptDataByteArray(blobByteArray);

				listDokumenteVO.add(objDokumenteVO);

			}
			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > Reading data from offset == " + offset + " range == " + range
						+ "Total records found :- " + listDokumenteVO.size());

		} catch (SQLException e) {
			e.printStackTrace(ps);
		} catch (IOException e) {
			e.printStackTrace(ps);
		} catch (PropertyVetoException e) {
			e.printStackTrace(ps);
		}

		return listDokumenteVO;
	}

	private DokumenteVO process(DokumenteVO objDokumenteVO) {

		// ByteArrayInputStream byteArrayInputStream = null;
		try {
			InputStream inputStream = new ByteArrayInputStream(objDokumenteVO.getCryptDataByteArray());
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			decryptStreamData(inputStream, byteArrayOutputStream);

			// byteArrayInputStream = new
			// ByteArrayInputStream(byteArrayOutputStream.toByteArray());

			objDokumenteVO.setDecryptDataByteArray(byteArrayOutputStream.toByteArray());

			// objDokumenteVO.setByteArrayInputStream(byteArrayInputStream);

			inputStream.close();
			byteArrayOutputStream.close();

		} catch (IOException e) {
			e.printStackTrace(ps);
		}

		return objDokumenteVO;
	}

	private void decryptStreamData(InputStream is, OutputStream os) {

		try {
			final int UNZIP_BUFFER_SIZE = 1024;
			final int DECRYPT_BUFFER_SIZE = 100;
			byte[] keyByteArray = { (byte) 0x1E, (byte) 0x54, (byte) 0x35, (byte) 0x43, (byte) 0x43, (byte) 0xF4,
					(byte) 0x8C, (byte) 0x9A };
			byte[] ivBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0xAB,
					(byte) 0xCD, (byte) 0xEF };
			DESKeySpec desks = new DESKeySpec(keyByteArray);
			SecretKey skey = null;
			skey = SecretKeyFactory.getInstance("DES").generateSecret(desks);

			IvParameterSpec ivps = new IvParameterSpec(ivBytes);
			Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, skey, ivps);

			int count;

			byte[] input = new byte[DECRYPT_BUFFER_SIZE];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CipherOutputStream cos = new CipherOutputStream(baos, cipher);
			BufferedInputStream bis = new BufferedInputStream(is);
			while ((count = bis.read(input, 0, DECRYPT_BUFFER_SIZE)) != -1) {
				cos.write(input, 0, count);
			}
			cos.flush();
			cos.close();

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > INFO Deschifrierung erfolgreich durchgef�hrt");

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bais));
			ZipEntry entry = zis.getNextEntry();
			if (entry != null) {
				entry.getName();
			}
			BufferedOutputStream dest = new BufferedOutputStream(os);
			byte[] data = new byte[UNZIP_BUFFER_SIZE];
			while ((count = zis.read(data, 0, UNZIP_BUFFER_SIZE)) != -1) {
				dest.write(data, 0, count);
			}

			dest.flush();
			dest.close();
			zis.close();
			baos.close();
			bais.close();

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > INFO Dearchivierung erfolgreich durchgef�hrt");
		}

		catch (InvalidKeySpecException e) {
			e.printStackTrace(ps);
		} catch (InvalidKeyException e) {
			e.printStackTrace(ps);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(ps);
		} catch (NoSuchPaddingException e) {
			e.printStackTrace(ps);
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace(ps);
		} catch (IOException e) {
			e.printStackTrace(ps);
		}
	}

	private static String[][] replacements = { { "ä", "&auml" }, { "Ä", "&Auml" }, { "ö", "&ouml" }, { "Ö", "&Ouml" },
			{ "ü", "&uuml" }, { "Ü", "&Uuml" }, { "ß", "&szlig" }, { "§", "c2 a7" }, { "¬", "c2 ac" } };

	public DocumentDetails uploadDocument(String url, String app, StoreDocumentRequest documentDetails,
			String authUsername, String authPassword) {

		DocumentDetails documentResponse = null;
		String serializedObj = null;
		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > " + "Entering into addUser method of AddUserClient");
		try {

			ObjectMapper mapper = new ObjectMapper();
			serializedObj = mapper.writeValueAsString(documentDetails);

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + "serializedObj " + serializedObj);

			for (String[] replacement : replacements) {
				serializedObj = serializedObj.replace(replacement[0], replacement[1]);
			}

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + serializedObj);

			HttpEntity<String> requestEntity = new HttpEntity<>(serializedObj,
					AuthenticationHeader.getAuthenticationHeader(authUsername, authPassword));
			RestTemplate templte = new RestTemplate(AuthenticationHeader.httpsRequestFactory());
			templte.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
			templte.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + "URL :- " + url + "/" + app + "/" + "documents/");
			ResponseEntity<DocumentDetails> response = templte.exchange(url + "/" + app + "/" + "documents/",
					HttpMethod.POST, requestEntity, DocumentDetails.class);

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + "Got response:- " + response);

			documentResponse = response.getBody();

			if (documentResponse.getDocument_access_key() == null) {
				nullResponseReq.add(serializedObj);
			}

			passedResponsesReq.add(serializedObj);
		} catch (Exception ex) {

			ps.println(dfOut.format(new Date()) + " > " + "UpdateUserClient Exception : " + ex.getMessage());
			ps.println(dfOut.format(new Date()) + " > Error in getting response for request object " + serializedObj);
			ex.printStackTrace();
			failedresponses.add(serializedObj);

		}
		return documentResponse;
	}

	private List<String> failedresponses = new ArrayList<String>();
	private List<String> passedresponses = new ArrayList<String>();
	private List<String> passedResponsesReq = new ArrayList<String>();
	private List<String> nullResponseReq = new ArrayList<String>();

	private String uploadDocument(DokumenteVO objDokumenteVO) {

		String accessKey = null;
		String url = sdsRestServerURL;
		String app = sdsRestServerApp;
		String authUsername = sdsRestServerUser;
		String authPassword = sdsRestServerPassword;

		StoreDocumentRequest storeDocumentRequest = new StoreDocumentRequest();
		try {

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

			/** FILE NAME REPLACE WITH BYTE ARRAY OF DOCGEN */
			storeDocumentRequest.setContent(new Content());
			storeDocumentRequest.getContent()
					.setSource(SdsEncoding.generateBase64(objDokumenteVO.getDecryptDataByteArray()));

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + " storeDocumentRequest.getContent().getSource() ->> "
						+ storeDocumentRequest.getContent().getSource());
			storeDocumentRequest.getContent()
					.setChecksum(SdsEncoding.generateMd5(objDokumenteVO.getDecryptDataByteArray()));

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > " + " storeDocumentRequest.getContent().getChecksum() ->> "
						+ storeDocumentRequest.getContent().getChecksum());

			storeDocumentRequest.setAttributes(new Attributes());
			storeDocumentRequest.getAttributes().setOwner(props.getProperty("OwnerOfDocument"));
			storeDocumentRequest.getAttributes().setGroup("ZVR");
			storeDocumentRequest.getAttributes().setTitle("DOKUMENTEDOKBEZ");
			storeDocumentRequest.getAttributes().setEncryption("none");

			if (objDokumenteVO.getCretms() != null) {
				storeDocumentRequest.getAttributes().setCreatedAt(objDokumenteVO.getCretms());

				Calendar calender = Calendar.getInstance();
				calender.setTime(objDokumenteVO.getCretms());
				calender.add(Calendar.YEAR, 5);
				Date validTo = calender.getTime();
				try {
					validTo = new java.sql.Date(df.parse(df.format(validTo)).getTime());
				} catch (Exception e) {
					e.printStackTrace(ps);
				}
				storeDocumentRequest.getAttributes().setValidTo(validTo);
			}

			storeDocumentRequest.getAttributes().setFileFormat("pdf");

			/**
			 * As discussed, now this program has to read data from TMPDOKUMENTE
			 * table, where DOKUMENTART value will be auto populated by talend
			 * program. Hence removing this code.
			 */
			storeDocumentRequest.getAttributes().setFileName(getDokumentenartForDoktype(objDokumenteVO.getDoktyp()));
			// storeDocumentRequest.getAttributes().setFileName(objDokumenteVO.getDokumentart());

			storeDocumentRequest.getAttributes().setBusinessKey("//templates//docgen//DemoGraphicStateAgeWise.pdf");

			LinkedHashMap<String, Object> applicationSpecificData = new LinkedHashMap<String, Object>();

			applicationSpecificData = fillValues(objDokumenteVO);

			storeDocumentRequest.getAttributes().setApplicationSpecificData(applicationSpecificData);

			DocumentDetails response = uploadDocument(url, app, storeDocumentRequest, authUsername, authPassword);
			if (response != null) {
				if (detailedLogsEnabled)
					ps.println(dfOut.format(new Date()) + " > " + response.getDocument_access_key());
				accessKey = response.getDocument_access_key();
			}
		} catch (Exception e) {
			e.printStackTrace(ps);
		}

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > " + " The upload of document having DOKID == "
					+ objDokumenteVO.getDokid() + " has been DONE. Access key recieved == " + accessKey);

		return accessKey;

	}

	private LinkedHashMap<String, Object> fillValues(DokumenteVO dokumentVO) {

		String[] keys = { "DOKID", "DOKTYPALT", "DOKUMENTENART", "DRUCKTMS", "ALFRESCOKZ", "MELDID", "VMID",
				"ANZSEITEN", "BEVOLLMNR", "DOKBEZ", "LANDKZ" };
		LinkedHashMap<String, Object> applicationSpecificData = new LinkedHashMap<String, Object>();

		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			switch (key) {
			case "DOKID":
				applicationSpecificData.put("key", "DOKID");
				applicationSpecificData.put("value", dokumentVO.getDokid());
				break;
			case "DOKTYPALT":
				applicationSpecificData.put("key1", "DOKTYPALT");
				applicationSpecificData.put("value1", "testvalue2");
				break;
			case "DOKUMENTENART":
				applicationSpecificData.put("key2", "DOKTYPALT");
				/**
				 * As discussed, now this program has to read data from
				 * TMPDOKUMENTE table, where DOKUMENTART value will be auto
				 * populated by talend program. Hence removing this code.
				 */
				applicationSpecificData.put("value2", getDokumentenartForDoktype(dokumentVO.getDoktyp()));
				// applicationSpecificData.put("value2",
				// dokumentVO.getDokumentart());
				break;
			case "DRUCKTMS":
				applicationSpecificData.put("key3", "DRUCKTMS");
				applicationSpecificData.put("value3", dokumentVO.getDrucktms());
				break;
			case "ALFRESCOKZ":
				applicationSpecificData.put("key4", "ALFRESCOKZ");
				applicationSpecificData.put("value4", dokumentVO.getAlfrescokz());
				break;
			case "MELDID":
				applicationSpecificData.put("key5", "MELDID");
				applicationSpecificData.put("value5", dokumentVO.getMeldid());
				break;
			case "VMID":
				applicationSpecificData.put("key6", "VMID");
				applicationSpecificData.put("value6", dokumentVO.getVmid());
				break;
			case "ANZSEITEN":
				applicationSpecificData.put("key7", "ANZSEITEN");
				applicationSpecificData.put("value7", dokumentVO.getAnzseiten());
				break;
			case "BEVOLLMNR":
				applicationSpecificData.put("key8", "BEVOLLMNR");
				applicationSpecificData.put("value8", dokumentVO.getBevollmnr());
				break;
			case "DOKBEZ":
				applicationSpecificData.put("key9", "DOKBEZ");
				applicationSpecificData.put("value9", dokumentVO.getDokbez());
				break;
			case "LANDKZ":
				applicationSpecificData.put("key10", "LANDKZ");
				applicationSpecificData.put("value10", dokumentVO.getLandkz());
				break;
			}

		}

		return applicationSpecificData;
	}

	private String getDokumentenartForDoktype(BigDecimal doktyp) {
		String dokumenteart = "-";
		for (DoktypeValueMapping dokvm : listDoktypeValueMapping) {
			if (dokvm.getDoktyp().compareTo(doktyp) == 0) {
				dokumenteart = dokvm.getDOKUMENTART();
				break;
			}
		}
		return dokumenteart;
	}

	private boolean write(List<TMPDokumenteVO> listMV, Date totalProcessStartTime) {

		Date startTime = new Date();
		Date endTime = null;

		int actualRecordsProcessed = 0;

		try {
			if (listMV == null || listMV.size() <= 0) {

				actualRecordsProcessed = 0;
				return false;
			}

			try (Connection targetConnection = DataSource.getInstance().getTargetConnection();
					PreparedStatement updateSDSIDPreparedStatement = targetConnection
							.prepareStatement(updateSDSIDPreparedStatementSQL);

					Connection sourceConnection = DataSource.getInstance().getSourceConnection();
					PreparedStatement updateSDSIDStatusPreparedStatement = sourceConnection
							.prepareStatement(updateSDSIDStatusPreparedStatementSQL);) {

				startTime = new Date();

				for (TMPDokumenteVO mv : listMV) {
					int counter = 1;
					if (mv.getSdsid() != null) {
						updateSDSIDPreparedStatement.setString(counter++, mv.getSdsid());

						// if (rerunable)
						// updateSDSIDPreparedStatement.setBigDecimal(counter++,
						// new BigDecimal(1));

						updateSDSIDPreparedStatement.setString(counter++, mv.getDokid());

						updateSDSIDPreparedStatement.addBatch();
						actualRecordsProcessed++;
					}

				}

				int[] response = updateSDSIDPreparedStatement.executeBatch();
				StringBuffer sb = new StringBuffer();
				for (int i : response) {
					sb.append(i + ",");
				}

				if (detailedLogsEnabled)
					ps.println(dfOut.format(new Date()) + " > Response of save the blob data : " + sb.toString()
							+ " actualRecordsProcessed == " + actualRecordsProcessed);

				//
				////////////////////

				if (rerunable) {
					int index = 0;

					for (TMPDokumenteVO mv : listMV) {

						int counter = 1;
						if (response[index] > 0) {

							updateSDSIDStatusPreparedStatement.setBigDecimal(counter++, new BigDecimal(1));
							updateSDSIDStatusPreparedStatement.setString(counter++, mv.getDokid());
							updateSDSIDStatusPreparedStatement.addBatch();
						}
					}

					int[] response1 = updateSDSIDStatusPreparedStatement.executeBatch();
					StringBuffer sb1 = new StringBuffer();
					for (int i : response1) {
						sb1.append(i + ",");
					}
					if (detailedLogsEnabled)
						ps.println(dfOut.format(new Date()) + " > Response of save status of the blob data : "
								+ sb1.toString() + " listMV.size() == " + listMV.size());
				}

				///////////////////

				targetConnection.commit();
				if (rerunable)
					sourceConnection.commit();

			} catch (SQLException e) {
				e.printStackTrace(ps);

			} catch (PropertyVetoException e) {
				e.printStackTrace(ps);
			}

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > INFO  XML-Dokument mit der ID " + listMV.size() + " meld ids "
						+ " wurde erfolgreich in Zieldatenbank generiert (BATCH-INSERT)");

		} catch (IOException e) {
			e.printStackTrace(ps);
		} finally {
			endTime = new Date();

			long totalTimeInMillSecFromProcessStart = (endTime.getTime() - totalProcessStartTime.getTime());

			setTotalRecordsProcessed(actualRecordsProcessed);
			int totalRecProc = getTotalRecordsProcessed();
			double ettm = ((double) totalTimeInMillSecFromProcessStart / 1000) / 60;

			ps.printf(
					dfOut.format(new Date())
							+ " > TRP : %d | ETTM : %.3f minutes | ARP : %d | ETAS : %.3f seconds. | RPM : %.3f records/min \n",
					totalRecProc, ettm, actualRecordsProcessed,
					(((double) (endTime.getTime() - startTime.getTime()) / 1000)), ((double) totalRecProc / ettm));
		}

		return true;
	}

	private static int totalRecordsProcessed = 0;

	private synchronized static int getTotalRecordsProcessed() {
		return totalRecordsProcessed;
	}

	private synchronized static void setTotalRecordsProcessed(int arp) {
		totalRecordsProcessed += arp;
	}

	public static void main(String[] args) {

		ps = System.out;

		try {
			if (args != null && args.length == 1) {
				initialisiere(args);
				String logFileLocation = props.getProperty("LogFileLocation");
				try {
					ps = new PrintStream(new FileOutputStream(logFileLocation));
				} catch (Exception e) {
					// e.printStackTrace();
					System.out.println("Error in getting logfile location : " + logFileLocation
							+ " All logs will be written to console only");
				}
				ps.println(
						dfOut.format(new Date()) + " > INFO  START " + DokumenteChunkParallelProcess.class.getName());
				if ("1".equals(props.getProperty("FromDatabaseParallelDecrypt"))) {
					doInitialSettings();
					entryForProcessing();
				}
			} else {
				System.out.println(dfOut.format(new Date())
						+ " > Please enter only one parameter with the full path and name of the properties file");
			}
		} finally {
			close();
			ps.println(dfOut.format(new Date()) + " > INFO  END DokumenteBLOBDecryptor");
		}

	}

}
