package de.wps.brav.migration.dokumente.db;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DataSource {

	private static DataSource thisInstance;
	private ComboPooledDataSource sourceCpds;
	private ComboPooledDataSource targetCpds;

	private static DateFormat dfOut = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	private static Properties props;

	private static boolean detailedLogsEnabled = true;

	static {
		// try {
		// String propertiesfilePathName =
		// "C:/Users/premendra.kumar/git/bnotk-zvr-migration/BNotKLegacyConverter/src/de/wps/brav/migration/DokumenteBLOBDecryptor.properties";//
		// args[0];
		// props = PropertiesLoader.loadProperties(propertiesfilePathName);
		// } catch (Exception e) {
		// }

	}

	private DataSource() throws IOException, SQLException, PropertyVetoException {

		/** Source connection pooling */

		sourceCpds = new ComboPooledDataSource("SourceDatabaseConnection");
		sourceCpds.setDriverClass(props.getProperty("SourceDatabaseDriver"));
		sourceCpds.setJdbcUrl(props.getProperty("SourceDatabaseUrl"));
		sourceCpds.setUser(props.getProperty("SourceDatabaseUsername"));
		sourceCpds.setPassword(props.getProperty("SourceDatabasePassword"));

		

		sourceCpds.setInitialPoolSize(Integer.parseInt(props.getProperty("SourceDatabaseInitialPoolSize")));
		sourceCpds.setMinPoolSize(Integer.parseInt(props.getProperty("SourceDatabaseMinPoolSize")));
		sourceCpds.setAcquireIncrement(Integer.parseInt(props.getProperty("SourceDatabaseAcquireIncrement")));
		sourceCpds.setMaxPoolSize(Integer.parseInt(props.getProperty("SourceDatabaseAcquireIncrement")));
		sourceCpds.setMaxStatements(0/*Integer.parseInt(props.getProperty("SourceDatabaseMaxStatements"))*/);
		sourceCpds.setStatementCacheNumDeferredCloseThreads(1);
//		sourceCpds.setNumHelperThreads(10);
//		sourceCpds.setIdleConnectionTestPeriod(30);
//		sourceCpds.setTestConnectionOnCheckout(true);
//		sourceCpds.setPreferredTestQuery("Select 1");

		/** Target connection pooling */

		targetCpds = new ComboPooledDataSource("TargetDatabaseConnection");
		targetCpds.setDriverClass(props.getProperty("TargetDatabaseDriver"));
		targetCpds.setJdbcUrl(props.getProperty("TargetDatabaseUrl"));
		targetCpds.setUser(props.getProperty("TargetDatabaseUsername"));
		targetCpds.setPassword(props.getProperty("TargetDatabasePassword"));

		targetCpds.setInitialPoolSize(Integer.parseInt(props.getProperty("TargetDatabaseInitialPoolSize")));
		targetCpds.setMinPoolSize(Integer.parseInt(props.getProperty("TargetDatabaseMinPoolSize")));
		targetCpds.setAcquireIncrement(Integer.parseInt(props.getProperty("TargetDatabaseAcquireIncrement")));
		targetCpds.setMaxPoolSize(Integer.parseInt(props.getProperty("TargetDatabaseMaxPoolSize")));
		targetCpds.setMaxStatements(0/*Integer.parseInt(props.getProperty("TargetDatabaseMaxStatements"))*/);
		targetCpds.setStatementCacheNumDeferredCloseThreads(1);
//		targetCpds.setNumHelperThreads(10);
//		targetCpds.setIdleConnectionTestPeriod(30);
//		targetCpds.setTestConnectionOnCheckout(true);
//		targetCpds.setPreferredTestQuery("Select 1");

	}

	public static DataSource getInstance() throws IOException, SQLException, PropertyVetoException {

		if (thisInstance == null) {
			thisInstance = new DataSource();
			return thisInstance;
		}
		return thisInstance;

	}

	public static void setProps(Properties props) {
		DataSource.props = props;

		detailedLogsEnabled = "1".equalsIgnoreCase(props.getProperty("EnableDetailedLogs"));
	}

	public Connection getSourceConnection() throws SQLException {

		Connection sourceConnection = null;
		sourceConnection = thisInstance.sourceCpds.getConnection();
//		sourceConnection.setReadOnly(true);
		sourceConnection.setAutoCommit(false);

		if (detailedLogsEnabled)
			System.out.println(dfOut.format(new Date()) + " > Source Connection retrieved from connection pool.");

		return sourceConnection;		
	}

	public Connection getTargetConnection() throws SQLException {

		Connection targetConnection = null;
		targetConnection = thisInstance.targetCpds.getConnection();
		targetConnection.setAutoCommit(false);

		if (detailedLogsEnabled)
			System.out.println(dfOut.format(new Date()) + " > Target Connection retrieved from connection pool.");

		return targetConnection;
	}

}