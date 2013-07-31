package rs.ville.clagg.crawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.postgresql.Driver;

public class DBConnectionManager
{
	private static final Logger log = LogManager.getLogger(DBConnectionManager.class);
	
	private static String host;
	private static Boolean ssl;
	private static Integer port;
	private static String dbName;
	private static String username;
	private static String password;
	
	public static void init(Properties props)
	{
		host = props.getProperty("db.host");
		ssl = new Boolean(props.getProperty("db.ssl"));
		port = new Integer(props.getProperty("db.port"));
		
		dbName = props.getProperty("db.name");
		username = props.getProperty("db.username");
		password = props.getProperty("db.password");
		
		try
		{
			Class.forName("org.postgresql.Driver");
		}
		catch(ClassNotFoundException ex)
		{
			log.fatal("Could not find PostgreSQL JDBC class", ex);
		}
	}
	
	private static Connection getJDBCConnection() throws SQLException
	{
		String url = String.format("jdbc:postgresql://%s:%d/%s?sslfactory=org.postgresql.ssl.NonValidatingFactory", host, port, dbName);
		
		Properties props = new Properties();
		
		props.setProperty("user", username);
		props.setProperty("password", password);
		props.setProperty("ssl", ssl.toString());
		
		return DriverManager.getConnection(url, props);
	}
	
	public static DBConnection getConnection() throws SQLException
	{
		return new DBConnection(getJDBCConnection());
	}
}
