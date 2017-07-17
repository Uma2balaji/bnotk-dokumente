//package de.wps.brav.migration.dokumente.decryptor;
//
//import java.beans.PropertyVetoException;
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.math.BigDecimal;
//import java.security.InvalidAlgorithmParameterException;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.security.spec.InvalidKeySpecException;
//import java.sql.Blob;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Properties;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//import java.util.zip.ZipOutputStream;
//
//import javax.crypto.Cipher;
//import javax.crypto.CipherOutputStream;
//import javax.crypto.NoSuchPaddingException;
//import javax.crypto.SecretKey;
//import javax.crypto.SecretKeyFactory;
//import javax.crypto.spec.DESKeySpec;
//import javax.crypto.spec.IvParameterSpec;
//
//import de.wps.brav.migration.dokumente.PropertiesLoader;
//import de.wps.brav.migration.dokumente.db.DataSource;
//import de.wps.brav.migration.dokumente.db.DokumenteVO;
//
//public class DokumenteChunkRead {
//
//	private static Connection sourceConnection = null;
//	private static Connection targetConnection = null;
//	private static DateFormat dfOut = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//	private static Properties props;
//	private static PreparedStatement UPDATE_ENCRYPTDATA_PreparedStatement;
//	private static PreparedStatement UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement;
//	private static PreparedStatement UPDATE_DECRYPTDATA_PreparedStatement;
//	private static PreparedStatement INSERT_DECRYPTDATA_PreparedStatement;
//
//	/**
//	 * Inizialisiert das Properties-Object aus der Properties-datei
//	 */
//	private static void initialisiere(String[] args) {
//		String propertiesfilePathName = args[0];
//		System.out.println(
//				dfOut.format(new Date()) + " > INFO  Properties Datei " + propertiesfilePathName + " wird geladen");
//		props = PropertiesLoader.loadProperties(propertiesfilePathName);
//		System.out.println(dfOut.format(new Date()) + " > INFO  Properties Datei " + propertiesfilePathName
//				+ " wurde erfolgreich geladen");
//	}
//
//	private static Connection getSourceConnection() {
//		if (sourceConnection == null) {
//			try {
//				sourceConnection = DataSource.getInstance().getSourceConnection();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (PropertyVetoException e) {
//				e.printStackTrace();
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//			return sourceConnection;
//		} else {
//			return sourceConnection;
//		}
//	}
//
//	private static Connection getTargetConnection() {
//		try {
//			targetConnection = DataSource.getInstance().getTargetConnection();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}catch(IOException e){
//			e.printStackTrace();
//		}catch(PropertyVetoException e){
//			e.printStackTrace();
//		}
//		return targetConnection;
//	}
//
//	// private void schreibeEinVerschluesseltesTestBlobAusDateiInTabelle() {
//	//
//	// String inputFile = new
//	// String(props.getProperty("TestEncryptedBLOBFileName"));
//	// String dataSetID = new
//	// String(props.getProperty("TestToBeUpdatedDataSetID"));
//	// System.out.println(dfOut.format(new Date()) + " > INFO die Testdatei " +
//	// inputFile + " wird in den Datensatz "
//	// + dataSetID + " als BLOB unver�ndert geschrieben");
//	// try {
//	// BufferedInputStream bufferedInputstream = new BufferedInputStream(new
//	// FileInputStream(inputFile));
//	// int result = setEncryptedDataToBLOB(bufferedInputstream, dataSetID);
//	// bufferedInputstream.close();
//	// if (result > 0) {
//	// System.out.println(dfOut.format(new Date()) + " > INFO die Testdatei " +
//	// inputFile
//	// + " wurde in den Datensatz " + dataSetID + " als BLOB unver�ndert
//	// erfolgreich geschrieben");
//	// } else {
//	// System.out.println(dfOut.format(new Date()) + " > INFO die Testdatei " +
//	// inputFile
//	// + " wurde NICHT in den Datensatz " + dataSetID + " als BLOB unver�ndert
//	// geschrieben");
//	// }
//	//
//	// } catch (FileNotFoundException e) {
//	// e.printStackTrace();
//	// } catch (IOException e) {
//	// e.printStackTrace();
//	// }
//	// }
//
//	// private void verarbeiteBLOBDatenFromSourceDB() {
//	// try {
//	//
//	// Connection sourceConnection = getSourceConnection();
//	// System.out.println("connection got .. starting the processing!!");
//	// Statement statement = sourceConnection.createStatement();
//	// ResultSet rs =
//	// statement.executeQuery(props.getProperty("SelectCryptDataAndId"));
//	//
//	// while (rs.next()) {
//	//
//	// try {
//	// String id = rs.getString("ID");
//	// Blob cryptData = rs.getBlob("BLOBDATA");
//	// System.out.println(dfOut.format(new Date())
//	// + " > INFO Beginne die Verarbeitung des Datensatzes mit der ID " + id);
//	// if (cryptData != null) {
//	//
//	// InputStream inputStream = cryptData.getBinaryStream();
//	// ByteArrayOutputStream byteArrayOutputStream = new
//	// ByteArrayOutputStream();
//	// decryptStreamData(inputStream, byteArrayOutputStream);
//	//
//	// if ("1".equals(props.getProperty("DecryptToXMLFile"))) {
//	// String outputFile = new String(
//	// props.getProperty("OutputFolder") +
//	// props.getProperty("OutputFileNamePrefix") + id
//	// + props.getProperty("OutputFileNameSuffix"));
//	// BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(
//	// new FileOutputStream(outputFile));
//	// bufferedFileOutputStream.write(byteArrayOutputStream.toByteArray());
//	// bufferedFileOutputStream.flush();
//	// bufferedFileOutputStream.close();
//	// System.out.println(dfOut.format(new Date()) + " > INFO XML-Datei " +
//	// outputFile
//	// + " wurde erfolgreich generiert");
//	// }
//	//
//	// if ("1".equals(props.getProperty("DecryptToDBTableAsUpdatte"))) {
//	// ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
//	// byteArrayOutputStream.toByteArray());
//	// setDecryptedDataToBLOBAsUpdate(byteArrayInputStream, id);
//	// byteArrayInputStream.close();
//	// System.out.println(dfOut.format(new Date()) + " > INFO XML-Dokument mit
//	// der ID " + id
//	// + " wurde erfolgreich in Zieldatenbank generiert (UPDATE)");
//	// }
//	//
//	// if ("1".equals(props.getProperty("DecryptToDBTableAsInsert"))) {
//	// ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
//	// byteArrayOutputStream.toByteArray());
//	// setDecryptedDataToBLOBAsInsert(byteArrayInputStream, id);
//	// byteArrayInputStream.close();
//	// System.out.println(dfOut.format(new Date()) + " > INFO XML-Dokument mit
//	// der ID " + id
//	// + " wurde erfolgreich in Zieldatenbank generiert (INSERT)");
//	// }
//	//
//	// inputStream.close();
//	// byteArrayOutputStream.close();
//	//
//	// } else {
//	// System.out.println(dfOut.format(new Date()) + " > INFO Kein BLOB im
//	// Datensatz mit der ID " + id
//	// + " vorhanden");
//	// }
//	//
//	// } catch (FileNotFoundException e) {
//	// e.printStackTrace();
//	// } catch (IOException e) {
//	// e.printStackTrace();
//	// } catch (SQLException e) {
//	// e.printStackTrace();
//	// } catch (Exception e) {
//	// e.printStackTrace();
//	// }
//	// }
//	//
//	// statement.close();
//	// sourceConnection.close();
//	//
//	// } catch (SQLException e) {
//	// e.printStackTrace();
//	// }
//	// }
//
//	/**
//	 * ###### Threading and parallel processing of medungen blob data - start
//	 * ##################################
//	 */
//
//	// private static int offset = 0;
//	private ExecutorService processAndWriteExecutor = Executors.newFixedThreadPool(40);
//	private ExecutorService writeExecutor = Executors.newFixedThreadPool(100);
//	private static int sizeOfTheReadingChunk = 50;
//	private static int sizeOfTheWritingChunk = 25;
//	private static int numberOfRecordsReadFromDB = 100;
//	private static int executorThreadPoolSize = 40;
//	private static String selectTotalMeldCountQuery = null;
//	private static String insertDecryptdataPreparedStatementSQL = null;
//	private static String insertDecryptdataStatusPreparedStatementSQL = null;
//	private static String selectCryptDataAndIdPagingQuery = null;
//
//	private static boolean detailedLogsEnabled = true;
//
//	private static void doInitialSettings() {
//		/** Initial settings - start */
//		executorThreadPoolSize = Integer.parseInt(props.getProperty("ProcessAndWriteExecutorsThreadPoolSize"));
//
//		// processAndWriteExecutor =
//		// Executors.newFixedThreadPool(executorThreadPoolSize);
//		//
//		// writeExecutor = Executors
//		// .newFixedThreadPool(Integer.parseInt(props.getProperty("WriteExecutorsThreadPoolSize")));
//
//		sizeOfTheReadingChunk = Integer.parseInt(props.getProperty("sizeOfTheReadingChunkFromMeldungen"));
//
//		sizeOfTheWritingChunk = Integer.parseInt(props.getProperty("sizeOfTheWritingChunkIntoMeldungen"));
//
//		numberOfRecordsReadFromDB = Integer.parseInt(props.getProperty("NumberOfRecordsToBeReadFromMeldungen"));
//
//		selectTotalMeldCountQuery = props.getProperty("SelectTotalMeldCountQuery");
//
//		insertDecryptdataPreparedStatementSQL = props.getProperty("InsertDecryptdataQuery");
//
//		insertDecryptdataStatusPreparedStatementSQL = props.getProperty("insertDecryptdataStatusPreparedStatementSQL");
//
//		selectCryptDataAndIdPagingQuery = props.getProperty("SelectCryptDataAndIdPagingQuery");
//
//		detailedLogsEnabled = "1".equalsIgnoreCase(props.getProperty("EnableDetailedLogs"));
//
//		DataSource.setProps(props);
//
//		/** Initial settings - end */
//	}
//
//	private static Boolean continueProgram = true;
//
//	private static void entryForProcessing() {
//
//		Date startTime = new Date();
//		Date endTime = null;
//		int totalMeldCount = getMeldCount();
//		int totalIteration = (totalMeldCount / numberOfRecordsReadFromDB)
//				+ ((totalMeldCount % numberOfRecordsReadFromDB) > 0 ? 1 : 0);
//
//		System.out.println("\n*******\n" + dfOut.format(new Date()) + " > " + " TRP = Total records processed "
//				+ " | ETTM = Elapsed total time in minutes " + " | ARP = Actual Record Processed "
//				+ " | ETAS = Elapsed time actual recordset in seconds " + " | RPM = Records Per Minute "
//				+ "\n***********\n");
//
//		if (detailedLogsEnabled)
//			System.out.println("totalMeldCount == " + totalMeldCount + " totalIteration == " + totalIteration);
//		for (int iteration = 0; iteration < totalIteration; iteration++) {
//			DokumenteChunkRead bnotkObj = new DokumenteChunkRead();
//			bnotkObj.intermediate(iteration, numberOfRecordsReadFromDB, executorThreadPoolSize, startTime);
//		}
//
//		continueProgram = false;
//
//		endTime = new Date();
//
//		long milli = (endTime.getTime() - startTime.getTime());
//
//		double totalTimeInMin = (((double) milli / 1000) / 60);
//		double averageSpeed = ((double) totalMeldCount / totalTimeInMin);
//		// if(detailedLogsEnabled)
//		System.out.printf(dfOut.format(new Date()) + " > "
//				+ "\n Total Records Processed : %d | Total Time Elapsed : %.3f Minutes | Average Speed : %.3f Records/Min."
//				+ "\n", totalMeldCount, totalTimeInMin, averageSpeed);
//
//	}
//
//	private int iteration;
//
//	private void intermediate(int iteration, int range, int executorThreadPoolSize, Date totalProcessStartTime) {
//
//		// Date startTime = new Date();
//		// Date endTime = null;
//
//		this.iteration = iteration;
//		// iterationOverMeld(/* iteration, */ range, executorThreadPoolSize,
//		// totalProcessStartTime);
//
//		Date iterationStartTime = new Date();
//		// Date endTime = null;
//
//		processAndWriteExecutor = Executors.newFixedThreadPool(executorThreadPoolSize);
//
//		writeExecutor = Executors
//				.newFixedThreadPool(Integer.parseInt(props.getProperty("WriteExecutorsThreadPoolSize")));
//
//		if (detailedLogsEnabled)
//			System.out.println("iteration == " + iteration + " iteration*range == new offset == " + (iteration * range)
//					+ " range == " + range);
//
//		int partsOfList = (range / sizeOfTheReadingChunk) + ((range % sizeOfTheReadingChunk) > 0 ? 1 : 0);
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > Number Of Records To Be Read From Meldungen == " + range
//					+ " sizeOfTheReadingChunk == " + sizeOfTheReadingChunk + " partsOfList == " + partsOfList);
//
//		int[] parts = new int[partsOfList];
//		int total = 0;
//		for (int i = 0; i < partsOfList; i++) {
//			parts[i] = sizeOfTheReadingChunk;
//			total += parts[i];
//			if (total >= range) {
//				parts[i] = sizeOfTheReadingChunk - (total - range);
//			}
//		}
//
//		for (int i = 0; i < parts.length; i++) {
//
//			final int a = i;
//			final int[] partss=parts;
//			final Date iterationStartTimee =iterationStartTime;
//			final Date totalProcessStartTimee =iterationStartTime;
//
//			Runnable worker = new Runnable() {
//				@Override
//				public void run() {
//					// int calculatedOffset = (iteration * range) + a *
//					// sizeOfTheReadingChunk;
//					/** Changing logic for rerunnability feature */
//					int calculatedOffset = /* (iteration * range) + */a * sizeOfTheReadingChunk;
//					int calculatedRange = partss[a];
//					if (detailedLogsEnabled)
//						System.out.println(dfOut.format(new Date()) + " > calculatedOffset == " + calculatedOffset
//								+ " calculatedRange == "+calculatedRange);
//					processAndWrite(calculatedOffset, calculatedRange, iterationStartTimee, totalProcessStartTimee);
//				}
//			};
//			processAndWriteExecutor.execute(worker);
//		}
//
//		processAndWriteExecutor.shutdown();
//
//		boolean b = true;
//		while (!writeExecutor.isTerminated()) {
//
//			if (b && (processAndWriteExecutor.isTerminated())) {
//				writeExecutor.shutdown();
//				b = false;
//			}
//		}
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > iteration == " + iteration + "Finished all threads");
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > Processing of data from offset " + iteration * range
//					+ " and range " + range + " has been started. ");
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > New offset is :- " + (iteration + 1) * range);
//
//	}
//
//	private static int totalRecordsProcessed = 0;
//
//	private synchronized static int getTotalRecordsProcessed() {
//		return totalRecordsProcessed;
//	}
//
//	private synchronized static void setTotalRecordsProcessed(int arp) {
//		totalRecordsProcessed += arp;
//	}
//
//	private void iterationOverMeld(final int range, int executorThreadPoolSize, Date totalProcessStartTime) {
//
//		Date iterationStartTime = new Date();
//		Date endTime = null;
//
//		processAndWriteExecutor = Executors.newFixedThreadPool(executorThreadPoolSize);
//
//		writeExecutor = Executors
//				.newFixedThreadPool(Integer.parseInt(props.getProperty("WriteExecutorsThreadPoolSize")));
//
//		if (detailedLogsEnabled)
//			System.out.println("iteration == " + iteration + " iteration*range == new offset == " + (iteration * range)
//					+ " range == " + range);
//
//		int partsOfList = (range / sizeOfTheReadingChunk) + ((range % sizeOfTheReadingChunk) > 0 ? 1 : 0);
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > Number Of Records To Be Read From Meldungen == " + range
//					+ " sizeOfTheReadingChunk == " + sizeOfTheReadingChunk + " partsOfList == " + partsOfList);
//
//		int[] parts = new int[partsOfList];
//		int total = 0;
//		for (int i = 0; i < partsOfList; i++) {
//			parts[i] = sizeOfTheReadingChunk;
//			total += parts[i];
//			if (total >= range) {
//				parts[i] = sizeOfTheReadingChunk - (total - range);
//			}
//		}
//
//		for (int i = 0; i < parts.length; i++) {
//
//			final int a = i;
//			final Date totalProcessStartTimee=totalProcessStartTime;
//			final Date iterationStartTimee =iterationStartTime;
//			final int[] partss =parts;
//
//			Runnable worker = new Runnable() {
//				@Override
//				public void run() {
//					int calculatedOffset = (iteration * range) + a * sizeOfTheReadingChunk;
//					int calculatedRange = partss[a];
//
//					processAndWrite(calculatedOffset, calculatedRange, iterationStartTimee, totalProcessStartTimee);
//				}
//			};
//			processAndWriteExecutor.execute(worker);
//		}
//
//		processAndWriteExecutor.shutdown();
//
//		boolean b = true;
//		while (!writeExecutor.isTerminated()) {
//
//			if (b && (processAndWriteExecutor.isTerminated())) {
//				writeExecutor.shutdown();
//				b = false;
//			}
//		}
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > iteration == " + iteration + "Finished all threads");
//
//	}
//
//	private static int getMeldCount() {
//		int count = 0;
//		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
//				PreparedStatement statement = sourceConnection.prepareStatement(selectTotalMeldCountQuery);) {
//
//			ResultSet rs = statement.executeQuery();
//			if (rs.next()) {
//				count = rs.getInt("TOTAL_COUNT");
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (PropertyVetoException e) {
//			e.printStackTrace();
//		}
//		return count;
//	}
//
//	private List<DokumenteVO> read(int offset, int range) {
//
//		if (detailedLogsEnabled)
//			System.out.println(
//					dfOut.format(new Date()) + " > Reading data from offset == " + offset + " range == " + range);
//
//		List<DokumenteVO> list = new ArrayList<DokumenteVO>();
//		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
//				PreparedStatement statement = sourceConnection.prepareStatement(selectCryptDataAndIdPagingQuery);) {
//
//			statement.setInt(1, offset + range);
//			statement.setInt(2, offset);
//			ResultSet rs = statement.executeQuery();
//
//			while (rs.next()) {
//				String id = rs.getString("ID");
//				Blob cryptData = rs.getBlob("BLOBDATA");
//				byte[] blobByteArray = cryptData.getBytes((long) 1, (int) (cryptData.length()));
//				BigDecimal meldnr = rs.getBigDecimal("MELDNR");
//				DokumenteVO mv = null;//new DokumenteVO(id, cryptData, meldnr);
//				mv.setCryptDataByteArray(blobByteArray);
//				list.add(mv);
//
//			}
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > Reading data from offset == " + offset + " range == "
//						+ range + "Total records found :- " + list.size());
//			// statement.close();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (PropertyVetoException e) {
//			e.printStackTrace();
//		}
//
//		return list;
//	}
//
//	private boolean processAndWrite(int offset, int range, Date iterationStartTime, Date totalProcessStartTime) {
//
//		boolean completed = false;
//
//		Date readingStartTime = new Date();
//
//		List<DokumenteVO> mvlist = read(offset, range);
//
//		Date readingEndTime = new Date();
//
//		long totalTimeInMillSecInReading = (readingEndTime.getTime() - readingStartTime.getTime());
//
//		// if (detailedLogsEnabled)
//		System.out.println(dfOut.format(new Date()) + " > Time elapsed to read "
//				+ ((mvlist != null && mvlist.size() > 0) ? mvlist.size() : 0) + " records == "
//				+ ((double) totalTimeInMillSecInReading / 1000) + " seconds. ");
//
//		if (mvlist == null) {
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > " + "mv is null");
//			return completed;
//		}
//
//		if (mvlist.size() <= 0) {
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > " + "mvlist size is zero");
//			return completed;
//		}
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > " + Thread.currentThread().getName()
//					+ " Processing started for = " + mvlist.size() + " records. ");
//
//		Date processingStartTime = new Date();
//
//		List<DokumenteVO> listMV = new ArrayList<DokumenteVO>();
//
//		for (DokumenteVO mv : mvlist) {
//			DokumenteVO mvWithStream = process(mv);
//			listMV.add(mvWithStream);
//		}
//
//		Date processingEndTime = new Date();
//
//		long totalTimeInMillSecInProcessing = (processingEndTime.getTime() - processingStartTime.getTime());
//
//		// if (detailedLogsEnabled)
//		System.out.println(dfOut.format(new Date()) + " > Time elapsed to process "
//				+ ((mvlist != null && mvlist.size() > 0) ? mvlist.size() : 0) + " records == "
//				+ ((double) totalTimeInMillSecInProcessing / 1000) + " seconds. ");
//
//		int partsOfList = (listMV.size() / sizeOfTheWritingChunk)
//				+ ((listMV.size() % sizeOfTheWritingChunk) > 0 ? 1 : 0);
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > listMV.size() == " + listMV.size()
//					+ " sizeOfTheWritingChunk == " + sizeOfTheWritingChunk + " partsOfList == " + partsOfList);
//		for (int i = 0; i < partsOfList; i++) {
//			final int a = i;
//			final List<DokumenteVO> listMVv =listMV;
//			final Date totalProcessStartTimee=totalProcessStartTime;
//			
//			Runnable worker = new Runnable() {
//				@Override
//				public void run() {
//					int startIndex = a * sizeOfTheWritingChunk;
//					int lastIndex = startIndex + sizeOfTheWritingChunk;
//
//					if (lastIndex >= listMVv.size()) {
//						lastIndex = listMVv.size();
//					}
//
//					if (detailedLogsEnabled)
//						System.out.println(dfOut.format(new Date()) + " > " + a + ". listMV.size() == " + listMVv.size()
//								+ " startIndex == " + startIndex + " lastIndex == " + lastIndex);
//					write(listMVv.subList(startIndex, lastIndex), totalProcessStartTimee);
//				}
//			};
//			writeExecutor.execute(worker);
//		}
//
//		return completed;
//	}
//
//	private DokumenteVO process(DokumenteVO mv) {
//
//		ByteArrayInputStream byteArrayInputStream = null;
//		try {
//			// InputStream inputStream = mv.getCryptData().getBinaryStream();
//			InputStream inputStream = new ByteArrayInputStream(mv.getCryptDataByteArray());
//			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//			decryptStreamData(inputStream, byteArrayOutputStream);
//
//			byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
//
//			mv.setByteArrayInputStream(byteArrayInputStream);
//			// mv.setInputStream(inputStream);
//			// mv.setByteArrayOutputStream(byteArrayOutputStream);
//			inputStream.close();
//			byteArrayOutputStream.close();
//
//		} /*
//			 * catch (SQLException e) { e.printStackTrace(); }
//			 */catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return mv;
//	}
//
//	private boolean write(List<DokumenteVO> listMV, Date totalProcessStartTime) {
//
//		Date startTime = new Date();
//		Date endTime = null;
//
//		int actualRecordsProcessed = 0;
//
//		try {
//			if (listMV == null || listMV.size() <= 0) {
//
//				actualRecordsProcessed = 0;
//				return false;
//			}
//
//			actualRecordsProcessed = listMV.size();
//
//			try (Connection targetConnection = DataSource.getInstance().getTargetConnection();
//					PreparedStatement insertDecryptdataPreparedStatement = targetConnection
//							.prepareStatement(insertDecryptdataPreparedStatementSQL);
//					Connection sourceConnection = DataSource.getInstance().getSourceConnection();
//					PreparedStatement insertDecryptdataStatusPreparedStatement = sourceConnection
//							.prepareStatement(insertDecryptdataStatusPreparedStatementSQL);) {
//
//				startTime = new Date();
//
//				// PreparedStatement insertDecryptdataPreparedStatement =
//				// targetConnection
//				// .prepareStatement(insertDecryptdataPreparedStatementSQL);
//
//				for (DokumenteVO mv : listMV) {
//
//					insertDecryptdataPreparedStatement.setString(1, mv.getId());
//
//					// ByteArrayInputStream byteArrayInputStream = new
//					// ByteArrayInputStream(mv.getByteArrayOutputStream().toByteArray());
//					// insertDecryptdataPreparedStatement.setBinaryStream(2,
//					// byteArrayInputStream);
//					insertDecryptdataPreparedStatement.setBinaryStream(2, mv.getByteArrayInputStream());
//					insertDecryptdataPreparedStatement.setBigDecimal(3, mv.getMeldnr());
//					insertDecryptdataPreparedStatement.addBatch();
//				}
//
//				int[] response = insertDecryptdataPreparedStatement.executeBatch();
//				StringBuffer sb = new StringBuffer();
//				for (int i : response) {
//					sb.append(i + ",");
//				}
//
//				if (detailedLogsEnabled)
//					System.out.println(dfOut.format(new Date()) + " > Response of save the blob data : " + sb.toString()
//							+ " listMV.size() == " + listMV.size());
//
//				// insertRecordsIntoStatusTable(listMV, response);
//				////////////////////
//
//				int index = 0;
//
//				for (DokumenteVO mv : listMV) {
//
//					if (response[index] > 0) {
//						insertDecryptdataStatusPreparedStatement.setBigDecimal(1, mv.getMeldnr());
//						insertDecryptdataStatusPreparedStatement.setBigDecimal(2, new BigDecimal(1));
//						insertDecryptdataStatusPreparedStatement.addBatch();
//					}
//				}
//
//				int[] response1 = insertDecryptdataStatusPreparedStatement.executeBatch();
//				StringBuffer sb1 = new StringBuffer();
//				for (int i : response1) {
//					sb1.append(i + ",");
//				}
//				if (detailedLogsEnabled)
//					System.out.println(dfOut.format(new Date()) + " > Response of save status of the blob data : "
//							+ sb1.toString() + " listMV.size() == " + listMV.size());
//
//				///////////////////
//
//				targetConnection.commit();
//				sourceConnection.commit();
//				// insertDecryptdataPreparedStatement.close();
//			} catch (SQLException  e) {
//				e.printStackTrace();
//
//			}catch(PropertyVetoException e){
//				e.printStackTrace();
//			}
//
//			for (DokumenteVO mv : listMV) {
//				if (mv.getByteArrayInputStream() != null) {
//					mv.getByteArrayInputStream().close();
//				}
//				if (mv.getInputStream() != null) {
//					mv.getInputStream().close();
//				}
//				if (mv.getByteArrayOutputStream() != null) {
//					mv.getByteArrayOutputStream().close();
//				}
//			}
//
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > INFO  XML-Dokument mit der ID " + listMV.size()
//						+ " meld ids " + " wurde erfolgreich in Zieldatenbank generiert (BATCH-INSERT)");
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			endTime = new Date();
//
//			long totalTimeInMillSecFromProcessStart = (endTime.getTime() - totalProcessStartTime.getTime());
//
//			setTotalRecordsProcessed(actualRecordsProcessed);
//			int totalRecProc = getTotalRecordsProcessed();
//			double ettm = ((double) totalTimeInMillSecFromProcessStart / 1000) / 60;
//
//			System.out.printf(
//					dfOut.format(new Date())
//							+ " > TRP : %d | ETTM : %.3f minutes | ARP : %d | ETAS : %.3f seconds. | RPM : %.3f records/min \n",
//					totalRecProc, ettm, actualRecordsProcessed,
//					(((double) (endTime.getTime() - startTime.getTime()) / 1000)), ((double) totalRecProc / ettm));
//
//			// System.out.printf(
//			// dfOut.format(new Date())
//			// + " > TRP : %d | ETTM : %.3f minutes | ARP : %d | ETAS : %.3f
//			// seconds. \n",
//			// totalRecordsProcessed, ettm, actualRecordsProcessed,
//			// (((double) (endTime.getTime() - startTime.getTime()) / 1000)));
//		}
//
//		return true;
//	}
//
//	private boolean insertRecordsIntoStatusTable(List<DokumenteVO> listMV, int[] response) {
//
//		Date startTime = new Date();
//		Date endTime = null;
//		int actualRecordsProcessed = 0;
//
//		try {
//			if (listMV == null || listMV.size() <= 0 || response == null) {
//
//				System.out.println(" response length == 0");
//				actualRecordsProcessed = 0;
//				return false;
//			}
//
//			System.out.println(" response length == " + response.length);
//
//			actualRecordsProcessed = listMV.size();
//
//			try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();
//					PreparedStatement insertDecryptdataStatusPreparedStatement = sourceConnection
//							.prepareStatement(insertDecryptdataStatusPreparedStatementSQL);) {
//
//				startTime = new Date();
//
//				int index = 0;
//
//				for (DokumenteVO mv : listMV) {
//
//					if (response[index] > 0) {
//						insertDecryptdataStatusPreparedStatement.setString(1, mv.getId());
//						insertDecryptdataStatusPreparedStatement.setBigDecimal(2, new BigDecimal(1));
//						insertDecryptdataStatusPreparedStatement.addBatch();
//					}
//				}
//
//				int[] response1 = insertDecryptdataStatusPreparedStatement.executeBatch();
//				StringBuffer sb = new StringBuffer();
//				for (int i : response1) {
//					sb.append(i + ",");
//				}
//				if (detailedLogsEnabled)
//					System.out.println(dfOut.format(new Date()) + " > Response of save status of the blob data : "
//							+ sb.toString() + " listMV.size() == " + listMV.size());
//
//				sourceConnection.commit();
//
//			} catch (SQLException e) {
//				e.printStackTrace();
//			} catch (PropertyVetoException e) {
//				e.printStackTrace();
//			}
//
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > INFO  Status for  " + listMV.size() + " meld ids "
//						+ " have been saved in database (BATCH-INSERT)");
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			endTime = new Date();
//
//			long totalTimeInMillSecFromProcessStart = (endTime.getTime() - startTime.getTime());
//
//			System.out.println(dfOut.format(new Date()) + " > Total time elapsed in saving status : "
//					+ ((double) (totalTimeInMillSecFromProcessStart / 1000)) + " seconds.");
//
//		}
//
//		return true;
//
//	}
//
//	private boolean processAndWrite(DokumenteVO mv) {
//
//		boolean completed = false;
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > " + Thread.currentThread().getName()
//					+ " Processing started for " + mv + " records.");
//
//		if (mv == null || mv.getCryptDataByteArray() == null) {
//			if (mv == null)
//				System.out.println(dfOut.format(new Date()) + " > mv is null");
//			if (mv.getCryptDataByteArray() == null)
//				System.out.println(dfOut.format(new Date()) + " > mv.getCryptData() is null");
//			return completed;
//		}
//
//		try {
//			// InputStream inputStream = mv.getCryptData().getBinaryStream();
//			InputStream inputStream = new ByteArrayInputStream(mv.getCryptDataByteArray());
//			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//			decryptStreamData(inputStream, byteArrayOutputStream);
//
//			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
//
//			try (Connection targetConnection = DataSource.getInstance().getTargetConnection();
//					PreparedStatement updateBLOBPreparedStatement = targetConnection
//							.prepareStatement(insertDecryptdataPreparedStatementSQL);) {
//
//				updateBLOBPreparedStatement.setBinaryStream(2, byteArrayInputStream);
//				updateBLOBPreparedStatement.setString(1, mv.getId());
//				updateBLOBPreparedStatement.executeUpdate();
//				targetConnection.commit();
//				// updateBLOBPreparedStatement.close();
//			} catch (SQLException e) {
//				e.printStackTrace();
//			} catch (PropertyVetoException e) {
//				e.printStackTrace();
//			}
//
//			byteArrayInputStream.close();
//			inputStream.close();
//			byteArrayOutputStream.close();
//
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > INFO  XML-Dokument mit der ID " + mv.getId()
//						+ " wurde erfolgreich in Zieldatenbank generiert (INSERT)");
//
//			if (detailedLogsEnabled)
//				System.out.println(Thread.currentThread().getName() + " End.");
//		} /*
//			 * catch (SQLException e) { e.printStackTrace(); }
//			 */ catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return completed;
//	}
//
//	/**
//	 * ###### Threading and parallel processing of medungen blob data - end
//	 * ##################################
//	 */
//
//	private void decryptStreamData(InputStream is, OutputStream os) {
//
//		try {
//			final int UNZIP_BUFFER_SIZE = 1024;
//			final int DECRYPT_BUFFER_SIZE = 100;
//			byte[] keyByteArray = { (byte) 0x1E, (byte) 0x54, (byte) 0x35, (byte) 0x43, (byte) 0x43, (byte) 0xF4,
//					(byte) 0x8C, (byte) 0x9A };
//			byte[] ivBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0xAB,
//					(byte) 0xCD, (byte) 0xEF };
//			DESKeySpec desks = new DESKeySpec(keyByteArray);
//			SecretKey skey = null;
//			skey = SecretKeyFactory.getInstance("DES").generateSecret(desks);
//
//			IvParameterSpec ivps = new IvParameterSpec(ivBytes);
//			Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
//			cipher.init(Cipher.DECRYPT_MODE, skey, ivps);
//
//			int count;
//
//			byte[] input = new byte[DECRYPT_BUFFER_SIZE];
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			CipherOutputStream cos = new CipherOutputStream(baos, cipher);
//			BufferedInputStream bis = new BufferedInputStream(is);
//			while ((count = bis.read(input, 0, DECRYPT_BUFFER_SIZE)) != -1) {
//				cos.write(input, 0, count);
//			}
//			cos.flush();
//			cos.close();
//
//			// System.out.println(dfOut.format(new Date()) + " > INFO
//			// Deschifrierung erfolgreich durchgef�hrt");
//
//			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bais));
//			ZipEntry entry = zis.getNextEntry();
//			if (entry != null) {
//				entry.getName();
//			}
//			BufferedOutputStream dest = new BufferedOutputStream(os);
//			byte[] data = new byte[UNZIP_BUFFER_SIZE];
//			while ((count = zis.read(data, 0, UNZIP_BUFFER_SIZE)) != -1) {
//				dest.write(data, 0, count);
//			}
//			dest.flush();
//			dest.close();
//			zis.close();
//			baos.close();
//			bais.close();
//
//			// System.out.println(dfOut.format(new Date()) + " > INFO
//			// Dearchivierung erfolgreich durchgef�hrt");
//		} catch (InvalidKeySpecException e) {
//			e.printStackTrace();
//		} catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (InvalidAlgorithmParameterException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void encryptStreamData(InputStream is, OutputStream os) {
//
//		try {
//			final int ZIP_BUFFER_SIZE = 1024;
//			final int ENCRYPT_BUFFER_SIZE = 100;
//			byte[] keyByteArray = { (byte) 0x1E, (byte) 0x54, (byte) 0x35, (byte) 0x43, (byte) 0x43, (byte) 0xF4,
//					(byte) 0x8C, (byte) 0x9A };
//			byte[] ivBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0xAB,
//					(byte) 0xCD, (byte) 0xEF };
//			DESKeySpec desks = new DESKeySpec(keyByteArray);
//			SecretKey skey = null;
//
//			int count;
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(baos));
//			ZipEntry zipEntry = new ZipEntry("entryname");
//			zip.putNextEntry(zipEntry);
//			byte[] inputForZip = new byte[ZIP_BUFFER_SIZE];
//			while ((count = is.read(inputForZip, 0, ZIP_BUFFER_SIZE)) != -1) {
//				zip.write(inputForZip, 0, count);
//			}
//			zip.flush();
//			zip.close();
//			// System.out.println(dfOut.format(new Date()) + " > INFO
//			// Archivierung erfolgreich durchgef�hrt");
//
//			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//			baos.close();
//
//			skey = SecretKeyFactory.getInstance("DES").generateSecret(desks);
//
//			IvParameterSpec ivps = new IvParameterSpec(ivBytes);
//			Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
//			cipher.init(Cipher.ENCRYPT_MODE, skey, ivps);
//			CipherOutputStream cos = new CipherOutputStream(os, cipher);
//
//			byte[] inputForEncrypt = new byte[ENCRYPT_BUFFER_SIZE];
//			while ((count = bais.read(inputForEncrypt, 0, ENCRYPT_BUFFER_SIZE)) != -1) {
//				cos.write(inputForEncrypt, 0, count);
//			}
//			cos.flush();
//			cos.close();
//			System.out.println(dfOut.format(new Date()) + " > INFO  Schifrierung erfolgreich durchgef�hrt");
//
//		} catch (InvalidKeySpecException e) {
//			e.printStackTrace();
//		} catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (InvalidAlgorithmParameterException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private int setEncryptedDataToBLOB(InputStream is, String id) {
//		int result = 0;
//		try {
//			PreparedStatement updateBLOBPreparedStatement = getUpdateEncryptdataPreparedStatement();
//			updateBLOBPreparedStatement.setBinaryStream(1, is);
//			updateBLOBPreparedStatement.setString(2, id);
//			result = UPDATE_ENCRYPTDATA_PreparedStatement.executeUpdate();
//			System.out.println(
//					dfOut.format(new Date()) + " > INFO  UPDATE_ENCRYPTDATA_PreparedStatement Result: " + result);
//
//			targetConnection.commit();
//			updateBLOBPreparedStatement.close();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return result;
//	}
//
//	private PreparedStatement getUpdateEncryptdataPreparedStatement() {
//		try {
//			if (UPDATE_ENCRYPTDATA_PreparedStatement == null) {
//				Connection targetConnection = DataSource.getInstance().getTargetConnection();
//				UPDATE_ENCRYPTDATA_PreparedStatement = targetConnection
//						.prepareStatement(props.getProperty("UPDATE_ENCRYPTDATA_PreparedStatement"));
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (PropertyVetoException e) {
//			e.printStackTrace();
//		}
//		return UPDATE_ENCRYPTDATA_PreparedStatement;
//	}
//
//	private PreparedStatement getUpdateDataAfterEncryptPreparedStatement() {
//		try {
//			if (UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement == null) {
//				Connection targetConnection = getTargetConnection();
//				UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement = targetConnection
//						.prepareStatement(props.getProperty("UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement"));
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement;
//	}
//
//	private PreparedStatement getUpdateDecryptdataPreparedStatement() {
//		try {
//			if (UPDATE_DECRYPTDATA_PreparedStatement == null) {
//				Connection targetConnection = getTargetConnection();
//				UPDATE_DECRYPTDATA_PreparedStatement = targetConnection
//						.prepareStatement(props.getProperty("UPDATE_DECRYPTDATA_PreparedStatement"));
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return UPDATE_DECRYPTDATA_PreparedStatement;
//	}
//
//	private PreparedStatement getInsertDecryptdataPreparedStatement() {
//		try {
//			if (INSERT_DECRYPTDATA_PreparedStatement == null) {
//				Connection targetConnection = getTargetConnection();
//				INSERT_DECRYPTDATA_PreparedStatement = targetConnection
//						.prepareStatement(props.getProperty("INSERT_DECRYPTDATA_PreparedStatement"));
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return INSERT_DECRYPTDATA_PreparedStatement;
//	}
//
//	private int setDataAfterEncryptToBLOB(InputStream is, String id) {
//		int result = 0;
//		try {
//			PreparedStatement updateBLOBPreparedStatement = getUpdateDataAfterEncryptPreparedStatement();
//			updateBLOBPreparedStatement.setBinaryStream(1, is);
//			updateBLOBPreparedStatement.setString(2, id);
//			result = UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement.executeUpdate();
//			System.out.println(dfOut.format(new Date())
//					+ " > INFO  UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement Result: " + result);
//			targetConnection.commit();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return result;
//	}
//
//	private void setDecryptedDataToBLOBAsUpdate(InputStream is, String id) {
//		try {
//			PreparedStatement updateBLOBPreparedStatement = getUpdateDecryptdataPreparedStatement();
//			updateBLOBPreparedStatement.setBinaryStream(1, is);
//			updateBLOBPreparedStatement.setString(2, id);
//			UPDATE_DECRYPTDATA_PreparedStatement.executeUpdate();
//			targetConnection.commit();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void setDecryptedDataToBLOBAsInsert(InputStream is, String id) {
//		try {
//			PreparedStatement updateBLOBPreparedStatement = getInsertDecryptdataPreparedStatement();
//			updateBLOBPreparedStatement.setBinaryStream(2, is);
//			updateBLOBPreparedStatement.setString(1, id);
//			updateBLOBPreparedStatement.executeUpdate();
//			targetConnection.commit();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static void close() {
//		try {
//
//			if (UPDATE_ENCRYPTDATA_PreparedStatement != null) {
//				UPDATE_ENCRYPTDATA_PreparedStatement.close();
//			}
//			if (UPDATE_DECRYPTDATA_PreparedStatement != null) {
//				UPDATE_DECRYPTDATA_PreparedStatement.close();
//			}
//			if (UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement != null) {
//				UPDATE_DATA_AFTER_ENCRYPT_PreparedStatement.close();
//			}
//			if (INSERT_DECRYPTDATA_PreparedStatement != null) {
//				INSERT_DECRYPTDATA_PreparedStatement.close();
//			}
//			if (targetConnection != null) {
//				targetConnection.close();
//			}
//			if (sourceConnection != null) {
//				sourceConnection.close();
//			}
//
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void entschlssleDateienAusEinemVerzeichnis() {
//		int counter = 0;
//		int renameCounter = 0;
//
//		File directory = new File(props.getProperty("InputFolder"));
//
//		if (directory.isDirectory()) {
//
//			File[] currentFiles = directory.listFiles();
//
//			if (currentFiles != null && currentFiles.length != 0) {
//
//				for (File inputFile : currentFiles) {
//
//					if (inputFile.isFile() && inputFile.getName().endsWith(props.getProperty("InputFileSuffix"))) {
//
//						String inputFileName = inputFile.getPath();
//						System.out.println(dfOut.format(new Date()) + " > INFO  Beginne die Verarbeitung Der Datei "
//								+ inputFileName);
//						String outputFileName = props.getProperty("InputFolder")
//								+ props.getProperty("OutputFileNamePrefix") + inputFile.getName()
//								+ props.getProperty("OutputFileNameSuffix");
//
//						try {
//							FileInputStream fileinputstream = new FileInputStream(inputFileName);
//							FileOutputStream fileoutputstream = new FileOutputStream(outputFileName);
//							/* BNOTKBLOBDataParallelDecrypterNewChangesNonStatic. */decryptStreamData(fileinputstream,
//									fileoutputstream);
//							counter++;
//							fileinputstream.close();
//							fileoutputstream.close();
//							System.out.println(dfOut.format(new Date()) + " > INFO  Die Datei " + inputFileName
//									+ " wurde erfolgreich verarbeitet");
//						} catch (FileNotFoundException e) {
//							e.printStackTrace();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//
//						if ("1".equals(props.getProperty("Archivieren"))) {
//							File archivFolder = new File(props.getProperty("ArchivFolder"));
//							if (archivFolder.exists() && archivFolder.isDirectory()) {
//								File archivFile = new File(props.getProperty("ArchivFolder") + inputFile.getName());
//								boolean renameSuccess = false;
//								if (!archivFile.exists()) {
//									renameSuccess = inputFile.renameTo(archivFile);
//								} else {
//									if (detailedLogsEnabled)
//										System.out.println(dfOut.format(new Date())
//												+ " > ERROR Eine Datei mit dem Namen " + archivFile.getPath()
//												+ " existiert bereits und kann nicht verschoben werden");
//								}
//								if (renameSuccess) {
//									renameCounter++;
//									if (detailedLogsEnabled)
//										System.out.println(dfOut.format(new Date()) + " > INFO  Die InputDatei "
//												+ inputFileName + " wurde in " + archivFile.getPath()
//												+ " erfolgreich umbenannt (verschoben)");
//								} else {
//									if (detailedLogsEnabled)
//										System.out.println(dfOut.format(new Date()) + " > ERROR Die InputDatei "
//												+ inputFileName + " konnte nicht in " + archivFile.getPath()
//												+ " umbenannt werden");
//								}
//							} else {
//								if (detailedLogsEnabled)
//									System.out.println(dfOut.format(new Date()) + " > ERROR Der Verzeichniss "
//											+ props.getProperty("ArchivFolder")
//											+ " existiert nicht, oder ist kein verzeichniss");
//							}
//						}
//
//					} else {
//						if (detailedLogsEnabled)
//							System.out.println(dfOut.format(new Date()) + " > WARN  Die Datei " + inputFile.getName()
//									+ " wird ignorriert, da sie dem Dateisuffixfilter nicht entspricht");
//					}
//				}
//			} else {
//				if (detailedLogsEnabled)
//					System.out.println(dfOut.format(new Date()) + " > WARN  Keine Dateien im Input Ordner "
//							+ props.getProperty("InputFolder"));
//			}
//
//		} else {
//			if (detailedLogsEnabled)
//				System.out.println(dfOut.format(new Date()) + " > ERROR Input Ordner "
//						+ props.getProperty("InputFolder") + " ist kein Ornder");
//		}
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > INFO  Es wurden " + counter
//					+ " Dateien erfolgreich entschl�sselt und " + renameCounter
//					+ " Dateien erfolgreich nach Archive umbenannt (verschoben)");
//	}
//
//	private void verschlssleDateienAusEinemVerzeichnis() {
//		int counter = 0;
//		int renameCounter = 0;
//
//		File directory = new File(props.getProperty("InputFolderVerschluesseln"));
//
//		if (directory.isDirectory()) {
//
//			File[] currentFiles = directory.listFiles();
//
//			if (currentFiles != null && currentFiles.length != 0) {
//
//				for (File inputFile : currentFiles) {
//
//					String inputFileSuffix = props.getProperty("InputFileSuffixVerschluesseln");
//					if (inputFile.isFile() && inputFile.getName().endsWith(inputFileSuffix)) {
//
//						String inputFileName = inputFile.getPath();
//						System.out.println(dfOut.format(new Date()) + " > INFO  Beginne die Verschl�sselung Der Datei "
//								+ inputFileName);
//						try {
//
//							ByteArrayOutputStream baos = new ByteArrayOutputStream();
//							FileInputStream fileinputstream = new FileInputStream(inputFileName);
//							/* BNOTKBLOBDataParallelDecrypterNewChangesNonStatic. */encryptStreamData(fileinputstream,
//									baos);
//
//							String dataSetID = inputFile.getName().substring(0,
//									inputFile.getName().length() - inputFileSuffix.length());
//							System.out.println(dfOut.format(new Date()) + " > INFO  die Testdatei " + inputFile
//									+ " wird in den Datensatz " + dataSetID + " als BLOB geschrieben");
//
//							if ("1".equals(props.getProperty("EncryptToDB"))) {
//								try {
//									ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//									int result = setDataAfterEncryptToBLOB(bais, dataSetID);
//									bais.close();
//									if (result > 0) {
//										System.out.println(dfOut.format(new Date()) + " > INFO  die Testdatei "
//												+ inputFile + " wurde in den Datensatz " + dataSetID
//												+ " als BLOB unver�ndert erfolgreich geschrieben");
//									} else {
//										System.out.println(dfOut.format(new Date()) + " > INFO  die Testdatei "
//												+ inputFile + " wurde NICHT in den Datensatz " + dataSetID
//												+ " als BLOB unver�ndert geschrieben");
//									}
//								} catch (FileNotFoundException e) {
//									e.printStackTrace();
//								} catch (IOException e) {
//									e.printStackTrace();
//								}
//							}
//
//							if ("1".equals(props.getProperty("EncryptToXMLFile"))) {
//								String outputFile = new String(props.getProperty("OutputFolderVerschluesseln")
//										+ props.getProperty("OutputFileNamePrefixVerschluesseln") + dataSetID
//										+ props.getProperty("OutputFileNameSuffixVerschluesseln"));
//								BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(
//										new FileOutputStream(outputFile));
//								bufferedFileOutputStream.write(baos.toByteArray());
//								bufferedFileOutputStream.flush();
//								bufferedFileOutputStream.close();
//								System.out.println(dfOut.format(new Date()) + " > INFO  XML-Datei " + outputFile
//										+ " wurde erfolgreich generiert");
//							}
//
//							baos.close();
//							fileinputstream.close();
//							counter++;
//							System.out.println(dfOut.format(new Date()) + " > INFO  Die Datei " + inputFileName
//									+ " wurde erfolgreich verarbeitet");
//						} catch (FileNotFoundException e) {
//							e.printStackTrace();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//
//						if ("1".equals(props.getProperty("ArchivierenVerschluesselung"))) {
//							File archivFolder = new File(props.getProperty("ArchivFolderVerschluesseln"));
//							if (archivFolder.exists() && archivFolder.isDirectory()) {
//								File archivFile = new File(
//										props.getProperty("ArchivFolderVerschluesseln") + inputFile.getName());
//								boolean renameSuccess = false;
//								if (!archivFile.exists()) {
//									renameSuccess = inputFile.renameTo(archivFile);
//								} else {
//									System.out.println(dfOut.format(new Date()) + " > ERROR Eine Datei mit dem Namen "
//											+ archivFile.getPath()
//											+ " existiert bereits und kann nicht verschoben werden");
//								}
//								if (renameSuccess) {
//									renameCounter++;
//									System.out.println(dfOut.format(new Date()) + " > INFO  Die InputDatei "
//											+ inputFileName + " wurde in " + archivFile.getPath()
//											+ " erfolgreich umbenannt (verschoben)");
//								} else {
//									System.out.println(
//											dfOut.format(new Date()) + " > ERROR Die InputDatei " + inputFileName
//													+ " konnte nicht in " + archivFile.getPath() + " umbenannt werden");
//								}
//							} else {
//								System.out.println(dfOut.format(new Date()) + " > ERROR Der Verzeichniss "
//										+ props.getProperty("ArchivFolder")
//										+ " existiert nicht, oder ist kein verzeichniss");
//							}
//						}
//
//					} else {
//						System.out.println(dfOut.format(new Date()) + " > WARN  Die Datei " + inputFile.getName()
//								+ " wird ignorriert, da sie dem Dateisuffixfilter nicht entspricht");
//					}
//				}
//			} else {
//				System.out.println(dfOut.format(new Date()) + " > WARN  Keine Dateien im Input Ordner "
//						+ props.getProperty("InputFolder"));
//			}
//
//		} else {
//			System.out.println(dfOut.format(new Date()) + " > ERROR Input Ordner " + props.getProperty("InputFolder")
//					+ " ist kein Ornder");
//		}
//
//		System.out.println(
//				dfOut.format(new Date()) + " > INFO  Es wurden " + counter + " Dateien erfolgreich entschl�sselt und "
//						+ renameCounter + " Dateien erfolgreich nach Archive umbenannt (verschoben)");
//	}
//
//	public static void main(String[] args) {
//
//		if (detailedLogsEnabled)
//			System.out.println(dfOut.format(new Date()) + " > INFO  START "
//					+ DokumenteChunkRead.class.getName());
//		try {
//			if (args != null && args.length == 1) {
//				initialisiere(args);
//
//				if ("1".equals(props.getProperty("FromDatabaseParallelDecrypt"))) {
//					doInitialSettings();
//					entryForProcessing();
//				}
//
//			} else {
//				System.out.println(dfOut.format(new Date())
//						+ " > Please enter only one parameter with the full path and name of the properties file");
//			}
//		} finally {
//			close();
//			System.out.println(dfOut.format(new Date()) + " > INFO  END DokumenteBLOBDecryptor");
//		}
//
//	}
//
//}
