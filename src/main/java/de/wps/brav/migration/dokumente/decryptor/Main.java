package de.wps.brav.migration.dokumente.decryptor;

import java.beans.PropertyVetoException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import de.bnotk.zvr.interfaces.sds.storedocument.Attributes;
import de.bnotk.zvr.interfaces.sds.storedocument.Content;
import de.bnotk.zvr.interfaces.sds.storedocument.DocumentDetails;
import de.bnotk.zvr.interfaces.sds.storedocument.SdsEncoding;
import de.bnotk.zvr.interfaces.sds.storedocument.StoreDocument;
import de.bnotk.zvr.interfaces.sds.storedocument.StoreDocumentRequest;
import de.wps.brav.migration.dokumente.PropertiesLoader;
import de.wps.brav.migration.dokumente.db.DataSource;
import de.wps.brav.migration.dokumente.db.DokumenteVO;
import de.wps.brav.migration.dokumente.db.TMPDokumenteVO;

public class Main {
	// private static Connection sourceConnection = null;
	// private static Connection targetConnection = null;
	private static boolean detailedLogsEnabled = true;
	private static DateFormat dfOut = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private static Properties props;

	/**
	 * Inizialisiert das Properties-Object aus der Properties-datei
	 */
	private static void initialisiere(String[] args) {
		String propertiesfilePathName = args[0];
		if (detailedLogsEnabled)
			System.out.println(
					dfOut.format(new Date()) + " > INFO  Properties Datei " + propertiesfilePathName + " wird geladen");
		props = PropertiesLoader.loadProperties(propertiesfilePathName);

		if (detailedLogsEnabled)
			System.out.println(dfOut.format(new Date()) + " > INFO  Properties Datei " + propertiesfilePathName
					+ " wurde erfolgreich geladen");
	}

	private static void doInitialSettings() {
		/** Initial settings - start */
		// executorThreadPoolSize =
		// Integer.parseInt(props.getProperty("ProcessAndWriteExecutorsThreadPoolSize"));

		// sizeOfTheReadingChunk =
		// Integer.parseInt(props.getProperty("sizeOfTheReadingChunkFromMeldungen"));

		// sizeOfTheWritingChunk =
		// Integer.parseInt(props.getProperty("sizeOfTheWritingChunkIntoMeldungen"));

		// numberOfRecordsReadFromDB =
		// Integer.parseInt(props.getProperty("NumberOfRecordsToBeReadFromMeldungen"));

		// selectTotalMeldCountQuery =
		// props.getProperty("SelectTotalMeldCountQuery");

		// insertDecryptdataPreparedStatementSQL =
		// props.getProperty("InsertDecryptdataQuery");

		// insertDecryptdataStatusPreparedStatementSQL =
		// props.getProperty("insertDecryptdataStatusPreparedStatementSQL");

		// selectCryptDataAndIdPagingQuery =
		// props.getProperty("SelectCryptDataAndIdPagingQuery");
		
		
		updateSDSIDPreparedStatementSQL = props.getProperty("updateSDSIDQuery");

		insertSDSIDStatusPreparedStatementSQL = props.getProperty("insertSDSIDStatusPreparedStatementSQL");

		detailedLogsEnabled = "1".equalsIgnoreCase(props.getProperty("EnableDetailedLogs"));

		DataSource.setProps(props);

		/** Initial settings - end */
	}

	private static List<DokumenteVO> read() {

		List<DokumenteVO> list = new ArrayList<DokumenteVO>();

		try (Connection sourceConnection = DataSource.getInstance().getSourceConnection();

				PreparedStatement statement = sourceConnection.prepareStatement("select * from dokumente");) {

			// statement.setInt(1, offset + range);

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

				DokumenteVO mv = new DokumenteVO(dokid, doktyp, cretms, drucktms, alfrescokz, gelesentms, meldid, vmid,
						anzseiten, bevollmnr, dokbez, dokdata, landkz);

				byte[] blobByteArray = null;
				if (dokdata != null) {
					blobByteArray = dokdata.getBytes((long) 1, (int) (dokdata.length()));
				}

				mv.setCryptDataByteArray(blobByteArray);

				list.add(mv);

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

	private static void entryForProcessing() {

		Date startTime = new Date();
		Date endTime = null;

		if (detailedLogsEnabled)
			System.out.println("\n*******\n" + dfOut.format(new Date()) + " > " + " TRP = Total records processed "
					+ " | ETTM = Elapsed total time in minutes " + " | ARP = Actual Record Processed "
					+ " | ETAS = Elapsed time actual recordset in seconds " + " | RPM = Records Per Minute "
					+ "\n***********\n");

		/// Read All dokumente
		/// process/decrypt data one by one
		/// send web service request one by one
		/// recieve response
		/// write the access key in transformation table against which id?

		try (/*
				 * Connection sourceConnection =
				 * DataSource.getInstance().getSourceConnection();
				 */
				Connection targetConnection = DataSource.getInstance().getSourceConnection();) {

			List<DokumenteVO> dokList = read();

			List<DokumenteVO> listMV = new ArrayList<DokumenteVO>();

			for (DokumenteVO mv : dokList) {
				DokumenteVO mvWithStream = process(mv);
				listMV.add(mvWithStream);
			}

			// List<String> keyList = new ArrayList<String>();

			List<TMPDokumenteVO> keyList = new ArrayList<TMPDokumenteVO>();

			for (DokumenteVO mv : listMV) {
				String accessKey = uploadDocument(mv);
				// keyList.add("{ \"dokid\":\"" + mv.getDokid() +
				// "\",\"accessKey\":\"" + accessKey + "\"}");
				keyList.add(new TMPDokumenteVO(mv.getDokid(), accessKey));
				// System.out.println("Access key " + accessKey + " DokumenteVO
				// " + mv);
			}
			
			write(keyList, startTime);
			//
			// if (detailedLogsEnabled)
			System.out.println("List obtained :- ");
			for (DokumenteVO d : listMV) {
				System.out.print("[ d.getDokid() = " + d.getDokid() + " ],");
			}
			System.out.println();
			System.out.println("KeyList obtained :- " + keyList);

			if (detailedLogsEnabled)
				System.out.println("targetConnection " + targetConnection);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		endTime = new Date();

		long milli = (endTime.getTime() - startTime.getTime());

		double totalTimeInMin = (((double) milli / 1000) / 60);

		System.out.println("Total time elapsed" + ((double) totalTimeInMin) + " min.");

		// if(detailedLogsEnabled)
		System.out.println(dfOut.format(new Date()) + " > "
				+ "\n Total Records Processed : %d | Total Time Elapsed : %.3f Minutes | Average Speed : %.3f Records/Min."
				+ "\n");

	}
	private static String updateSDSIDPreparedStatementSQL = null;
	private static String insertSDSIDStatusPreparedStatementSQL = null;
	
	private static boolean write(List<TMPDokumenteVO> listMV, Date totalProcessStartTime) {

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
					System.out.println(dfOut.format(new Date()) + " > Response of save the blob data : " + sb.toString()
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

				int[] response1 = insertDecryptdataStatusPreparedStatement.executeBatch();
				StringBuffer sb1 = new StringBuffer();
				for (int i : response1) {
					sb1.append(i + ",");
				}
				if (detailedLogsEnabled)
					System.out.println(dfOut.format(new Date()) + " > Response of save status of the blob data : "
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
				System.out.println(dfOut.format(new Date()) + " > INFO  XML-Dokument mit der ID " + listMV.size()
						+ " meld ids " + " wurde erfolgreich in Zieldatenbank generiert (BATCH-INSERT)");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			endTime = new Date();

			long totalTimeInMillSecFromProcessStart = (endTime.getTime() - totalProcessStartTime.getTime());

//			setTotalRecordsProcessed(actualRecordsProcessed);
//			int totalRecProc = getTotalRecordsProcessed();
			double ettm = ((double) totalTimeInMillSecFromProcessStart / 1000) / 60;

//			System.out.printf(
//					dfOut.format(new Date())
//							+ " > TRP : %d | ETTM : %.3f minutes | ARP : %d | ETAS : %.3f seconds. | RPM : %.3f records/min \n",
//					totalRecProc, ettm, actualRecordsProcessed,
//					(((double) (endTime.getTime() - startTime.getTime()) / 1000)), ((double) totalRecProc / ettm));

			// System.out.printf(
			// dfOut.format(new Date())
			// + " > TRP : %d | ETTM : %.3f minutes | ARP : %d | ETAS : %.3f
			// seconds. \n",
			// totalRecordsProcessed, ettm, actualRecordsProcessed,
			// (((double) (endTime.getTime() - startTime.getTime()) / 1000)));
		}

		return true;
	}

	// private

	private static String uploadDocument(DokumenteVO fileName) {

		String accessKey = null;
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
			if (detailedLogsEnabled)
				System.out.println(date1);
			// file name replace with byte array of docgen
			storeDocumentRequest.setContent(new Content());
			storeDocumentRequest.getContent().setSource(SdsEncoding.generateBase64(fileName.getCryptDataByteArray()));
			if (detailedLogsEnabled)
				System.out.println(storeDocumentRequest.getContent().getSource());
			storeDocumentRequest.getContent().setChecksum(SdsEncoding.generateMd5(fileName.getCryptDataByteArray()));

			if (detailedLogsEnabled)
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
			if (res != null) {
				if (detailedLogsEnabled) {
					System.out.println(res.getDocument_access_key());
				}
			}

			accessKey = res.getDocument_access_key();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("DONE");

		return accessKey;

	}

	private static DokumenteVO process(DokumenteVO mv) {

		ByteArrayInputStream byteArrayInputStream = null;
		try {
			// InputStream inputStream = mv.getCryptData().getBinaryStream();
			InputStream inputStream = new ByteArrayInputStream(mv.getCryptDataByteArray());
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			decryptStreamData(inputStream, byteArrayOutputStream);

			byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

			mv.setByteArrayInputStream(byteArrayInputStream);
			// mv.setInputStream(inputStream);
			// mv.setByteArrayOutputStream(byteArrayOutputStream);
			inputStream.close();
			byteArrayOutputStream.close();

		} /*
			 * catch (SQLException e) { e.printStackTrace(); }
			 */catch (IOException e) {
			e.printStackTrace();
		}

		return mv;
	}

	private static void decryptStreamData(InputStream is, OutputStream os) {

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

			// System.out.println(dfOut.format(new Date()) + " > INFO
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

			// System.out.println(dfOut.format(new Date()) + " > INFO
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

	public static void main(String[] args) {
		if (detailedLogsEnabled)
			System.out.println(dfOut.format(new Date()) + " > INFO  START " + Main.class.getName());
		try {
			if (args != null && args.length == 1) {
				initialisiere(args);

				if ("1".equals(props.getProperty("FromDatabaseParallelDecrypt"))) {
					doInitialSettings();
					entryForProcessing();
				}

			} else {
				if (detailedLogsEnabled)
					System.out.println(dfOut.format(new Date())
							+ " > Please enter only one parameter with the full path and name of the properties file");
			}
		} finally {
			// close();
			if (detailedLogsEnabled)
				System.out.println(dfOut.format(new Date()) + " > INFO  END DokumenteBLOBDecryptor");
		}

	}

}
