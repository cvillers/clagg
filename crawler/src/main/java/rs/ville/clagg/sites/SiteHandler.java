package rs.ville.clagg.sites;

import rs.ville.clagg.crawler.database.DBConnection;

/**
 * Interface to be implemented by all site handlers.
 * @author cmv
 */
public interface SiteHandler
{
	/**
	 * Crawls a particular URL.
	 * @param url
	 * @return Success or failure in crawling this particular URL. 
	 */
	public boolean processSearchURL(DBConnection database, long jobID, String url);
}
