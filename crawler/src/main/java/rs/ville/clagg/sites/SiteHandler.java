package rs.ville.clagg.sites;

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
	public boolean processSearchURL(long jobID, String url);
}
