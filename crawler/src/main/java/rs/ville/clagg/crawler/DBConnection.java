package rs.ville.clagg.crawler;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import rs.ville.clagg.sites.SiteQuery;

public class DBConnection
{
	private Connection connection;

	public DBConnection(Connection conn)
	{
		connection = conn;
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
}
