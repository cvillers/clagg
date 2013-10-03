package rs.ville.clagg.sites;

import rs.ville.clagg.database.DBConnection;

/**
 * Interface to be implemented by all site handlers.
 * @author cmv
 */
public interface SiteHandler
{
	/**
	 * Crawls a particular URL.
	 * @param database Database handle.
	 * @param jobID ID of the executing job.
	 * @param url URL of the results page to process.
	 * @return Success or failure in crawling this particular URL. 
	 */
	public boolean processSearchURL(DBConnection database, long jobID, String url);
	
	/**
	 * Validates a particular URL.
	 * @param database Database handle.
	 * @param url URL of the listing to validate.
	 * @return True if the URL is still valid, false otherwise.
	 */
	public boolean validateListingURL(DBConnection database, String url);
}
