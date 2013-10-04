package rs.ville.clagg.database;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.postgresql.ds.PGPoolingDataSource;


public class DBConnection
{
	private static Properties properties;
	private static boolean inited;
	private static PGPoolingDataSource source;
	
	public static void init(Properties props)
	{
		properties = props;
		
		source = new PGPoolingDataSource();
		
		source.setDataSourceName("clagg");
		
		source.setSsl(new Boolean(properties.getProperty("db.ssl")));
		source.setSslfactory(properties.getProperty("db.ssl-factory", null));
		
		source.setServerName(properties.getProperty("db.host"));
		source.setPortNumber(new Integer(properties.getProperty("db.port")));
		
		source.setDatabaseName(properties.getProperty("db.name"));
		source.setUser(properties.getProperty("db.username"));
		source.setPassword(properties.getProperty("db.password"));
		
		source.setMaxConnections(10);
		
		inited = true;
	}
	
	private Connection getConnection() throws SQLException
	{
		if(!inited)
			throw new RuntimeException("Tried to get a connection before the connection class was initialized");
		
		return source.getConnection();
	}
	
	/**
	 * Creates a new job definition.
	 * @return The new job's ID.
	 */
	public static final short JOBTYPE_CRAWL = 1;
	public static final short JOBTYPE_VALIDATE = 2;
	public long createJob(short type) throws SQLException
	{
		Connection connection = getConnection();
		
		long retVal = 0;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call job_create(?, 'job_cursor') }");
		proc.registerOutParameter(1, Types.OTHER);
		proc.setShort(2, type);
		proc.execute();
		
		connection.commit();
		
		ResultSet jobResults = (ResultSet) proc.getObject(1);
		if(jobResults.next())
		{
		    retVal = jobResults.getLong("id");
		}
		jobResults.close();
		proc.close();
		
		connection.close();
		
		return retVal;
	}
	
	/**
	 * Marks a job as completed.
	 * @param jobID ID of the job.
	 * @throws SQLException
	 */
	public void finishJob(long jobID) throws SQLException
	{
		Connection connection = getConnection();
		
		long retVal = 0;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ call job_finish(?) }");
		proc.setLong(1, jobID);
		proc.execute();
		
		connection.commit();
		
		connection.close();
		
		proc.close();
	}
	
	/**
	 * Gets all active URLs for querying.
	 * @return A list of pairs mapping a site identifier to a URL 
	 * @throws SQLException
	 */
	public List<SiteQuery> getQueryURLs() throws SQLException
	{
		Connection connection = getConnection();
		
		List<SiteQuery> retVal = new LinkedList<SiteQuery>();
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call query_get_all('query_cursor') }");
		proc.registerOutParameter(1, Types.OTHER);
		proc.execute();
	
		ResultSet results = (ResultSet) proc.getObject(1);
		while(results.next())
		{
		    retVal.add(new SiteQuery(results.getString("site_name"), results.getString("url")));
		}
		
		connection.commit();
		
		results.close();
		proc.close();
		
		connection.close();
		
		return retVal;
	}
	
	public boolean listingExists(String url) throws SQLException
	{
		Connection connection = getConnection();
		
		boolean retVal = false;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_exists_for_url(?) }");
		proc.registerOutParameter(1, Types.BOOLEAN);
		proc.setString(2, url);
		proc.execute();
	
		retVal = proc.getBoolean(1);

		connection.commit();
		
		proc.close();
		
		connection.close();
		
		return retVal;
	}
	
	public void createListing(long jobID, String url, String title, int price, double lat, double lng, String address) throws SQLException
	{
		Connection connection = getConnection();
		
		connection.setAutoCommit(false);

		//CallableStatement proc = connection.prepareCall("{ ? = call listing_create(?, ?, ?, ?, ?, ?, ?) }");
		PreparedStatement proc = connection.prepareStatement("select * from listing_create(?, ?, ?, ?, ?, ?, ?)");
		//proc.registerOutParameter(1, Types.OTHER);
		proc.setLong(1, jobID);
		proc.setString(2, url);
		proc.setString(3, title);
		proc.setInt(4, price);
		proc.setBigDecimal(5, new BigDecimal(lat));
		proc.setBigDecimal(6, new BigDecimal(lng));
		proc.setString(7, address);
		proc.execute();
		
		// the results row has two cursors: the inserted listing and the location that was used (existing or created)
		ResultSet results = (ResultSet) proc.getResultSet();
		if(results.next())
		{
			ResultSet listingRes = (ResultSet)results.getObject(1);
			ResultSet locationRes = (ResultSet)results.getObject(2);
			
			if(listingRes.next())
			{
				//listingRes.get
			}
		}
		
		connection.commit();
		
		results.close();
		proc.close();
		
		connection.close();
	}
	
	public void deleteListings(List<Long> urls) throws SQLException
	{
		Connection connection = getConnection();
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ call listing_delete(?) }");
		proc.setArray(1, connection.createArrayOf("bigint", urls.toArray()));
		proc.execute();
		
		connection.commit();
		
		proc.close();
		
		connection.close();
	}
	
	/**
	 * Gets all listings.
	 * @param includeDeleted Set to <pre>true</pre> to also get ones marked as deleted.
	 * @return A list of listing objects. 
	 * @throws SQLException
	 */
	public List<Listing> getListings(boolean includeDeleted, long pageSize, long pageNumber) throws SQLException
	{
		Connection connection = getConnection();
		
		List<Listing> retVal = new LinkedList<Listing>();
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_get_all(?, 'listings', ?, ?) }");
		proc.registerOutParameter(1, Types.OTHER);
		proc.setBoolean(2, includeDeleted);
		proc.setLong(3, pageSize);
		proc.setLong(4, pageNumber);
		proc.execute();
	
		ResultSet results = (ResultSet) proc.getObject(1);
		while(results.next())
		{
		    retVal.add(new Listing(results.getLong("id"), results.getString("url")));
		}
		
		connection.commit();
		
		results.close();
		proc.close();
		
		connection.close();
		
		return retVal;
	}
	
	/**
	 * Gets all listings added since the last validation job.
	 * @param includeDeleted Set to <pre>true</pre> to also get ones marked as deleted.
	 * @return A list of listing objects. 
	 * @throws SQLException
	 */
	public List<Listing> getUnvalidatedListings(boolean includeDeleted, long pageSize, long pageNumber) throws SQLException
	{
		Connection connection = getConnection();
		
		List<Listing> retVal = new LinkedList<Listing>();
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_get_unvalidated(?, 'listings', ?, ?) }");
		proc.registerOutParameter(1, Types.OTHER);
		proc.setBoolean(2, includeDeleted);
		proc.setLong(3, pageSize);
		proc.setLong(4, pageNumber);
		proc.execute();
	
		ResultSet results = (ResultSet) proc.getObject(1);
		while(results.next())
		{
		    retVal.add(new Listing(results.getLong("id"), results.getString("url")));
		}
		
		connection.commit();
		
		results.close();
		proc.close();
		
		connection.close();
		
		return retVal;
	}
	
	public long getListingCount(boolean includeDeleted) throws SQLException
	{
		Connection connection = getConnection();
		
		long retVal = 0;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_get_count(?) }");
		proc.registerOutParameter(1, Types.BIGINT);
		proc.setBoolean(2, includeDeleted);
		proc.execute();
	
		retVal = proc.getLong(1);
		
		connection.commit();

		proc.close();
		
		connection.close();
		
		return retVal;
	}
	
	public long getUnvalidatedListingCount(boolean includeDeleted) throws SQLException
	{
		Connection connection = getConnection();
		
		long retVal = 0;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_get_unvalidated_count(?) }");
		proc.registerOutParameter(1, Types.BIGINT);
		proc.setBoolean(2, includeDeleted);
		proc.execute();
	
		retVal = proc.getLong(1);
		
		connection.commit();

		proc.close();
		
		connection.close();
		
		return retVal;
	}
}
