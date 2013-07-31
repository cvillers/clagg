package rs.ville.clagg.crawler.database;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class DBConnection
{
	private Connection connection;

	public DBConnection(Connection conn)
	{
		connection = conn;
	}
	
	public void close() throws SQLException
	{
		this.connection.close();
	}
	
	/**
	 * Creates a new job definition.
	 * @return The new job's ID.
	 */
	public static final short JOBTYPE_CRAWL = 1;
	public long createJob(short type) throws SQLException
	{
		long retVal = 0;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call job_create(?, 'job_cursor') }");
		proc.registerOutParameter(1, Types.OTHER);
		proc.setShort(2, type);
		proc.execute();
		
		ResultSet jobResults = (ResultSet) proc.getObject(1);
		if(jobResults.next())
		{
		    retVal = jobResults.getLong("id");
		}
		jobResults.close();
		proc.close();
		
		return retVal;
	}
	
	/**
	 * Marks a job as completed.
	 * @param jobID ID of the job.
	 * @throws SQLException
	 */
	public void finishJob(long jobID) throws SQLException
	{
		long retVal = 0;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ call job_finish(?) }");
		proc.setLong(1, jobID);
		proc.execute();
		
		proc.close();
	}
	
	/**
	 * Gets all active URLs for querying.
	 * @return A list of pairs mapping a site identifier to a URL 
	 * @throws SQLException
	 */
	public List<SiteQuery> getQueryURLs() throws SQLException
	{
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
		results.close();
		proc.close();
		
		return retVal;
	}
	
	public boolean listingExists(String url) throws SQLException
	{
		boolean retVal = false;
		
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_exists_for_url(?) }");
		proc.registerOutParameter(1, Types.BOOLEAN);
		proc.setString(2, url);
		proc.execute();
		
		ResultSet results = (ResultSet) proc.getObject(1);
		if(results.next())
		{
		    retVal = results.getBoolean("listing_exists");
		}
		results.close();
		proc.close();
		
		return retVal;
	}
	
	public void createListing(String url, double lat, double lng, String address) throws SQLException
	{	
		connection.setAutoCommit(false);

		CallableStatement proc = connection.prepareCall("{ ? = call listing_create(?, ?, ?, ?) }");
		proc.registerOutParameter(1, Types.OTHER);
		proc.setString(2, url);
		proc.setDouble(3, lat);
		proc.setDouble(4, lng);
		proc.setString(5, address);
		proc.execute();
		
		// the results row has two cursors: the inserted listing and the location that was used (existing or created)
		ResultSet results = (ResultSet) proc.getObject(1);
		if(results.next())
		{
			ResultSet listingRes = (ResultSet)results.getObject(0);
			ResultSet locationRes = (ResultSet)results.getObject(1);
			
			if(listingRes.next())
			{
				//listingRes.get
			}
		}
		results.close();
		proc.close();
	}
}
