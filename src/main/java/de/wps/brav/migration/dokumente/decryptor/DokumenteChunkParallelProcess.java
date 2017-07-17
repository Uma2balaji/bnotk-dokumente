package de.wps.brav.migration.dokumente.decryptor;

import java.beans.PropertyVetoException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
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
import java.util.HashMap;
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

	private ExecutorService processAndWriteExecutor = Executors.newFixedThreadPool(40);
	private ExecutorService writeExecutor = Executors.newFixedThreadPool(100);
	private static int sizeOfTheReadingChunk = 50;
	private static int sizeOfTheWritingChunk = 25;
	private static int numberOfRecordsReadFromDB = 100;
	private static int executorThreadPoolSize = 40;
	private static String selectTotalDokCountQuery = null;
	private static String updateSDSIDPreparedStatementSQL = null;
	private static String insertSDSIDStatusPreparedStatementSQL = null;
	private static String selectCryptDataAndIdPagingQuery = null;
	private static String selectDoktypeVmData = null;

	private static String sdsRestServerURL = null;
	private static String sdsRestServerApp = null;
	private static String sdsRestServerUser = null;
	private static String sdsRestServerPassword = null;

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
		ps.println(dfOut.format(new Date()) + " > INFO  Properties Datei " + propertiesfilePathName + " wird geladen");
		props = PropertiesLoader.loadProperties(propertiesfilePathName);
		ps.println(dfOut.format(new Date()) + " > INFO  Properties Datei " + propertiesfilePathName
				+ " wurde erfolgreich geladen");
	}

	private static void doInitialSettings() {
		/** Initial settings - start */
		executorThreadPoolSize = Integer.parseInt(props.getProperty("ProcessAndWriteExecutorsThreadPoolSize"));

		sizeOfTheReadingChunk = Integer.parseInt(props.getProperty("sizeOfTheReadingChunkFromDokumente"));

		sizeOfTheWritingChunk = Integer.parseInt(props.getProperty("sizeOfTheWritingChunkIntoDokumente"));

		numberOfRecordsReadFromDB = Integer.parseInt(props.getProperty("NumberOfRecordsToBeReadFromDokumente"));

		selectTotalDokCountQuery = props.getProperty("selectTotalDokCountQuery");

		updateSDSIDPreparedStatementSQL = props.getProperty("updateSDSIDQuery");

		insertSDSIDStatusPreparedStatementSQL = props.getProperty("insertSDSIDStatusPreparedStatementSQL");

		selectCryptDataAndIdPagingQuery = props.getProperty("SelectCryptDataAndIdPagingQuery");

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
		ps.println("listDoktypeValueMapping : " + listDoktypeValueMapping);
		int totalIteration = (totalMeldCount / numberOfRecordsReadFromDB)
				+ ((totalMeldCount % numberOfRecordsReadFromDB) > 0 ? 1 : 0);

		ps.println("\n*******\n" + dfOut.format(new Date()) + " > " + " TRP = Total records processed "
				+ " | ETTM = Elapsed total time in minutes " + " | ARP = Actual Record Processed "
				+ " | ETAS = Elapsed time actual recordset in seconds " + " | RPM = Records Per Minute "
				+ "\n***********\n");

		// if (detailedLogsEnabled)
		ps.println(dfOut.format(new Date()) + " > " + " total dok count == " + totalMeldCount + " totalIteration == "
				+ totalIteration);

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
		 * 5. recieve response
		 * 
		 * 6. write the access key in transformation table against which id?
		 */

		for (int iteration = 0; iteration < totalIteration; iteration++) {
			DokumenteChunkParallelProcess bnotkObj = new DokumenteChunkParallelProcess();
			bnotkObj.intermediate(iteration, numberOfRecordsReadFromDB, executorThreadPoolSize, startTime);
		}

		endTime = new Date();

		long milli = (endTime.getTime() - startTime.getTime());

		double totalTimeInMin = (((double) milli / 1000) / 60);
		double averageSpeed = ((double) totalMeldCount / totalTimeInMin);

		ps.println("Total time elapsed" + ((double) totalTimeInMin) + " min.");

		// if(detailedLogsEnabled)
		ps.printf(dfOut.format(new Date()) + " > "
				+ "\n Total Records Processed : %d | Total Time Elapsed : %.3f Minutes | Average Speed : %.3f Records/Min."
				+ "\n", totalMeldCount, totalTimeInMin, averageSpeed);

		ps.println("failedresponses ");
		for (String str : failedresponses) {
			ps.println(str);
		}

		ps.println("Passed responses");
		for (String str : passedresponses) {
			ps.println(str);
		}

		ps.println("passedResponsesReq");
		for (String str : passedResponsesReq) {
			ps.println(str);
		}

		ps.println("nullResponseReq");
		for (String str : nullResponseReq) {
			ps.println(str);
		}
	}

	private static List<DoktypeValueMapping> fetchAllDokTypeValueMapping() {
		List<DoktypeValueMapping> list = new ArrayList<>();
		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
				PreparedStatement statement = sourceConnection.prepareStatement(selectDoktypeVmData);) {
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				DoktypeValueMapping dtvm = new DoktypeValueMapping(rs.getBigDecimal("DOKTYP"),
						rs.getString("DOKUMENTART"));
				list.add(dtvm);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		return list;
	}

	private static String getDokumentenartForDoktype(BigDecimal doktype) {
		String dokumentenart = null;

		for (DoktypeValueMapping dtvm : listDoktypeValueMapping) {
			if (dtvm.getDoktyp().compareTo(doktype) == 0) {
				dokumentenart = dtvm.getDOKUMENTART();
				break;
			}
		}
		ps.println("doktype : " + doktype.toString() + " dokumentenart " + dokumentenart);
		return dokumentenart;
	}

	private int iteration;

	private void intermediate(int iterationn, int range, int executorThreadPoolSize, Date totalProcessStartTime) {

		this.iteration = iterationn;

		Date iterationStartTime = new Date();

		processAndWriteExecutor = Executors.newFixedThreadPool(executorThreadPoolSize);

		writeExecutor = Executors
				.newFixedThreadPool(Integer.parseInt(props.getProperty("WriteExecutorsThreadPoolSize")));

		if (detailedLogsEnabled)
			ps.println("iteration == " + iteration + " iteration*range == new offset == " + (iteration * range)
					+ " range == " + range);

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
			final Date iterationStartTimee = iterationStartTime;
			final Date totalProcessStartTimee = iterationStartTime;

			Runnable worker = new Runnable() {
				@Override
				public void run() {
					int calculatedOffset = (iteration * range) + a * sizeOfTheReadingChunk;
					/** Changing logic for rerunnability feature */
					// int calculatedOffset = a * sizeOfTheReadingChunk;
					int calculatedRange = partss[a];
					if (detailedLogsEnabled)
						ps.println(dfOut.format(new Date()) + " > calculatedOffset == " + calculatedOffset
								+ " calculatedRange == " + calculatedRange);
					processAndWrite(calculatedOffset, calculatedRange, iterationStartTimee, totalProcessStartTimee);
				}
			};
			processAndWriteExecutor.execute(worker);
		}

		processAndWriteExecutor.shutdown();

		boolean b = true;
		while (!writeExecutor.isTerminated()) {

			if (b && (processAndWriteExecutor.isTerminated())) {
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

	}

	private boolean processAndWrite(int offset, int range, Date iterationStartTime, Date totalProcessStartTime) {

		boolean completed = false;

		Date readingStartTime = new Date();

		List<DokumenteVO> mvlist = read(offset, range);

		Date readingEndTime = new Date();

		long totalTimeInMillSecInReading = (readingEndTime.getTime() - readingStartTime.getTime());

		// if (detailedLogsEnabled)
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

		List<DokumenteVO> listMV = new ArrayList<DokumenteVO>();

		for (DokumenteVO mv : mvlist) {
			DokumenteVO mvWithStream = process(mv);
			listMV.add(mvWithStream);
		}

		Date processingEndTime = new Date();

		long totalTimeInMillSecInProcessing = (processingEndTime.getTime() - processingStartTime.getTime());

		// if (detailedLogsEnabled)
		ps.println(dfOut.format(new Date()) + " > Time elapsed to process "
				+ ((mvlist != null && mvlist.size() > 0) ? mvlist.size() : 0) + " records == "
				+ ((double) totalTimeInMillSecInProcessing / 1000) + " seconds. ");

		///////

		List<TMPDokumenteVO> keyList = new ArrayList<TMPDokumenteVO>();

		for (DokumenteVO mv : listMV) {
			String accessKey = uploadDocument(mv);
			passedresponses.add(
					"{" + "\"dokid\":" + "\"" + mv.getDokid() + "\",\"accessKey\":" + "\"" + accessKey + "\"" + "}");
			keyList.add(new TMPDokumenteVO(mv.getDokid(), accessKey));
		}

		///////

		int partsOfList = (keyList.size() / sizeOfTheWritingChunk)
				+ ((keyList.size() % sizeOfTheWritingChunk) > 0 ? 1 : 0);
		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > keyList.size() == " + keyList.size()
					+ " sizeOfTheWritingChunk == " + sizeOfTheWritingChunk + " partsOfList == " + partsOfList);
		for (int i = 0; i < partsOfList; i++) {
			final int a = i;
			final List<TMPDokumenteVO> listMVv = keyList;
			final Date totalProcessStartTimee = totalProcessStartTime;

			Runnable worker = new Runnable() {
				@Override
				public void run() {
					int startIndex = a * sizeOfTheWritingChunk;
					int lastIndex = startIndex + sizeOfTheWritingChunk;

					if (lastIndex >= listMVv.size()) {
						lastIndex = listMVv.size();
					}

					if (detailedLogsEnabled)
						ps.println(dfOut.format(new Date()) + " > " + a + ". keyList.size() == " + listMVv.size()
								+ " startIndex == " + startIndex + " lastIndex == " + lastIndex);
					write(listMVv.subList(startIndex, lastIndex), totalProcessStartTimee);
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		return count;
	}

	private List<DokumenteVO> read(int offset, int range) {

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > Reading data from offset == " + offset + " range == " + range);

		List<DokumenteVO> list = new ArrayList<DokumenteVO>();
		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
				PreparedStatement statement = sourceConnection.prepareStatement(selectCryptDataAndIdPagingQuery);) {

			statement.setInt(1, offset + range);
			statement.setInt(2, offset);
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				// String id = rs.getString("DOKID");
				// Blob cryptData = rs.getBlob("DOKDATA");
				// byte[] blobByteArray = cryptData.getBytes((long) 1, (int)
				// (cryptData.length()));
				// String meldnr = rs.getString("LANDKZ");
				// String meldid =rs.getString("MELDID");
				// DokumenteVO mv = new DokumenteVO(id, cryptData, meldnr,
				// meldid);
				// mv.setCryptDataByteArray(blobByteArray);
				// list.add(mv);

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

				DokumenteVO mv = new DokumenteVO(dokid, doktyp, cretms, drucktms, alfrescokz, gelesentms, meldid, vmid,
						anzseiten, bevollmnr, dokbez, dokdata, landkz);

				byte[] blobByteArray = null;
				if (dokdata != null) {
					blobByteArray = dokdata.getBytes((long) 1, (int) (dokdata.length()));
				}

				mv.setCryptDataByteArray(blobByteArray);

				list.add(mv);

			}
			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > Reading data from offset == " + offset + " range == " + range
						+ "Total records found :- " + list.size());
			// statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}

		return list;
	}

	private DokumenteVO process(DokumenteVO mv) {

		ByteArrayInputStream byteArrayInputStream = null;
		try {
			InputStream inputStream = new ByteArrayInputStream(mv.getCryptDataByteArray());
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			decryptStreamData(inputStream, byteArrayOutputStream);

			byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

			mv.setByteArrayInputStream(byteArrayInputStream);
			inputStream.close();
			byteArrayOutputStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return mv;
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

			// ps.println(dfOut.format(new Date()) + " > INFO
			// Deschifrierung erfolgreich durchgef�hrt");

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

			// ps.println(dfOut.format(new Date()) + " > INFO
			// Dearchivierung erfolgreich durchgef�hrt");
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String[][] replacements = { { "ä", "&auml" }, { "Ä", "&Auml" }, { "ö", "&ouml" }, { "Ö", "&Ouml" },
			{ "ü", "&uuml" }, { "Ü", "&Uuml" }, { "ß", "&szlig" }, { "§", "c2 a7" }, { "¬", "c2 ac" } };

	public DocumentDetails uploadDocument(String url, String app, StoreDocumentRequest documentDetails,
			String authUsername, String authPassword) {

		DocumentDetails documentResponse = null;
		String serializedObj = null;
		if (detailedLogsEnabled)
			ps.println("Entering into addUser method of AddUserClient");
		try {

			ObjectMapper mapper = new ObjectMapper();
			serializedObj = mapper.writeValueAsString(documentDetails);
			if (detailedLogsEnabled)
				ps.println("serializedObj " + serializedObj);
			for (String[] replacement : replacements) {
				serializedObj = serializedObj.replace(replacement[0], replacement[1]);
			}
			if (detailedLogsEnabled)
				ps.println(serializedObj);
			HttpEntity<String> requestEntity = new HttpEntity<>(serializedObj,
					AuthenticationHeader.getAuthenticationHeader(authUsername, authPassword));
			RestTemplate templte = new RestTemplate(AuthenticationHeader.httpsRequestFactory());
			templte.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
			templte.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

			System.out.println("URL :- " + url + "/" + app + "/" + "documents/");
			ResponseEntity<DocumentDetails> response = templte.exchange(url + "/" + app + "/" + "documents/",
					HttpMethod.POST, requestEntity, DocumentDetails.class);

			System.out.println("Got response:- " + response);

			documentResponse = response.getBody();
			if (documentResponse.getDocument_access_key() == null) {
				nullResponseReq.add(serializedObj);
			}
			passedResponsesReq.add(serializedObj);
		} catch (Exception ex) {

			ps.println("UpdateUserClient Exception :" + ex.getMessage());
			failedresponses.add(serializedObj);

		}
		return documentResponse;
	}

	private static List<String> failedresponses = new ArrayList<String>();
	private static List<String> passedresponses = new ArrayList<String>();
	private static List<String> passedResponsesReq = new ArrayList<String>();
	private static List<String> nullResponseReq = new ArrayList<String>();

	private String uploadDocument(DokumenteVO dokumentVO) {

		String accessKey = null;
		String url = sdsRestServerURL;
		String app = sdsRestServerApp;
		String authUsername = sdsRestServerUser;
		String authPassword = sdsRestServerPassword;

		StoreDocumentRequest storeDocumentRequest = new StoreDocumentRequest();
		try {

			Date date3 = Calendar.getInstance().getTime();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

			java.sql.Date date1 = null;

			try {
				date1 = new java.sql.Date(df.parse(df.format(date3)).getTime());
			} catch (Exception e) {
				e.printStackTrace();
			}
			// ps.println(date1);

			// file name replace with byte array of docgen
			storeDocumentRequest.setContent(new Content());
			storeDocumentRequest.getContent().setSource(SdsEncoding.generateBase64(dokumentVO.getCryptDataByteArray()));

			if (detailedLogsEnabled)
				ps.println(storeDocumentRequest.getContent().getSource());
			storeDocumentRequest.getContent().setChecksum(SdsEncoding.generateMd5(dokumentVO.getCryptDataByteArray()));

			if (detailedLogsEnabled)
				ps.println(storeDocumentRequest.getContent().getChecksum());

			storeDocumentRequest.setAttributes(new Attributes());
			storeDocumentRequest.getAttributes().setOwner("ZVR");
			storeDocumentRequest.getAttributes().setGroup("ZVR");
			// storeDocumentRequest.getAttributes().setTitle(dokumentVO.getDokbez());
			storeDocumentRequest.getAttributes().setTitle("DOKUMENTEDOKBEZ");
			storeDocumentRequest.getAttributes().setEncryption("none");

			if (dokumentVO.getCretms() != null) {
				storeDocumentRequest.getAttributes().setCreatedAt(dokumentVO.getCretms());

				Calendar c = Calendar.getInstance();
				c.setTime(dokumentVO.getCretms());
				c.add(Calendar.YEAR, 5);
				Date validTo = c.getTime();
				try {
					validTo = new java.sql.Date(df.parse(df.format(validTo)).getTime());
				} catch (Exception e) {
					e.printStackTrace();
				}
				storeDocumentRequest.getAttributes().setValidTo(validTo);
			}

			storeDocumentRequest.getAttributes().setFileFormat("pdf");
			storeDocumentRequest.getAttributes().setFileName(getDokumentenartForDoktype(dokumentVO.getDoktyp()));
			storeDocumentRequest.getAttributes().setBusinessKey("//templates//docgen//DemoGraphicStateAgeWise.pdf");
			// storeDocumentRequest.getAttributes().setGroup("templates");

			HashMap<String, Object> applicationSpecificData = new HashMap<String, Object>();
			applicationSpecificData = fillValues(dokumentVO);

			storeDocumentRequest.getAttributes().setApplicationSpecificData(applicationSpecificData);

			DocumentDetails res = /* StoreDocument. */uploadDocument(url, app, storeDocumentRequest, authUsername,
					authPassword);
			if (res != null) {
				if (detailedLogsEnabled)
					ps.println(res.getDocument_access_key());
				accessKey = res.getDocument_access_key();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (detailedLogsEnabled)
			ps.println("DONE");

		return accessKey;

	}

	private HashMap<String, Object> fillValues(DokumenteVO dokumentVO) {

		String[] keys = { "DOKID", "DOKTYPALT", "DOKUMENTENART", "DRUCKTMS", "ALFRESCOKZ", "MELDID", "VMID",
				"ANZSEITEN", "BEVOLLMNR", "DOKBEZ", "LANDKZ" };
		HashMap<String, Object> applicationSpecificData = new HashMap<String, Object>();

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
				applicationSpecificData.put("value2", getDokumentenartForDoktype(dokumentVO.getDoktyp()));
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

	private boolean write(List<TMPDokumenteVO> listMV, Date totalProcessStartTime) {

		Date startTime = new Date();
		Date endTime = null;

		int actualRecordsProcessed = 0;

		try {
			if (listMV == null || listMV.size() <= 0) {

				actualRecordsProcessed = 0;
				return false;
			}

			actualRecordsProcessed = listMV.size();

			try (Connection targetConnection = DataSource.getInstance().getTargetConnection();
					PreparedStatement updateSDSIDPreparedStatement = targetConnection
							.prepareStatement(updateSDSIDPreparedStatementSQL);
					Connection sourceConnection = DataSource.getInstance().getSourceConnection();
					PreparedStatement insertDecryptdataStatusPreparedStatement = sourceConnection
							.prepareStatement(insertSDSIDStatusPreparedStatementSQL);) {

				startTime = new Date();

				// PreparedStatement insertDecryptdataPreparedStatement =
				// targetConnection
				// .prepareStatement(insertDecryptdataPreparedStatementSQL);

				for (TMPDokumenteVO mv : listMV) {
					updateSDSIDPreparedStatement.setString(1, mv.getSdsid());
					updateSDSIDPreparedStatement.setString(2, mv.getDokid());
					updateSDSIDPreparedStatement.addBatch();
				}

				int[] response = updateSDSIDPreparedStatement.executeBatch();
				StringBuffer sb = new StringBuffer();
				for (int i : response) {
					sb.append(i + ",");
				}

				if (detailedLogsEnabled)
					ps.println(dfOut.format(new Date()) + " > Response of save the blob data : " + sb.toString()
							+ " listMV.size() == " + listMV.size());

				// insertRecordsIntoStatusTable(listMV, response);
				////////////////////

				int index = 0;

				for (TMPDokumenteVO mv : listMV) {

					if (response[index] > 0) {
						insertDecryptdataStatusPreparedStatement.setString(1, mv.getDokid());
						insertDecryptdataStatusPreparedStatement.setBigDecimal(2, new BigDecimal(1));
						insertDecryptdataStatusPreparedStatement.addBatch();
					}
				}

				int[] response1 = { 1 };// insertDecryptdataStatusPreparedStatement.executeBatch();
				StringBuffer sb1 = new StringBuffer();
				for (int i : response1) {
					sb1.append(i + ",");
				}
				if (detailedLogsEnabled)
					ps.println(dfOut.format(new Date()) + " > Response of save status of the blob data : "
							+ sb1.toString() + " listMV.size() == " + listMV.size());

				///////////////////

				targetConnection.commit();
				sourceConnection.commit();
				// insertDecryptdataPreparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();

			} catch (PropertyVetoException e) {
				e.printStackTrace();
			}

			// for (TMPDokumenteVO mv : listMV) {
			// if (mv.getByteArrayInputStream() != null) {
			// mv.getByteArrayInputStream().close();
			// }
			// if (mv.getInputStream() != null) {
			// mv.getInputStream().close();
			// }
			// if (mv.getByteArrayOutputStream() != null) {
			// mv.getByteArrayOutputStream().close();
			// }
			// }

			if (detailedLogsEnabled)
				ps.println(dfOut.format(new Date()) + " > INFO  XML-Dokument mit der ID " + listMV.size() + " meld ids "
						+ " wurde erfolgreich in Zieldatenbank generiert (BATCH-INSERT)");

		} catch (IOException e) {
			e.printStackTrace();
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

			// ps.printf(
			// dfOut.format(new Date())
			// + " > TRP : %d | ETTM : %.3f minutes | ARP : %d | ETAS : %.3f
			// seconds. \n",
			// totalRecordsProcessed, ettm, actualRecordsProcessed,
			// (((double) (endTime.getTime() - startTime.getTime()) / 1000)));
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

		try {
			ps = new PrintStream(new FileOutputStream("C:/Users/premendra.kumar/Desktop/MeldIDBlobUpdate.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (detailedLogsEnabled)
			ps.println(dfOut.format(new Date()) + " > INFO  START " + DokumenteChunkParallelProcess.class.getName());
		try {
			if (args != null && args.length == 1) {
				initialisiere(args);

				if ("1".equals(props.getProperty("FromDatabaseParallelDecrypt"))) {
					doInitialSettings();
					entryForProcessing();
				}

			} else {
				ps.println(dfOut.format(new Date())
						+ " > Please enter only one parameter with the full path and name of the properties file");
			}
		} finally {
			close();
			ps.println(dfOut.format(new Date()) + " > INFO  END DokumenteBLOBDecryptor");
		}

	}

}
