package rs.ville.clagg.sites;

/**
 * @author cmv
 * Processes Craigslist queries.
 */
public class CraigslistHandler implements SiteHandler 
{

	/**
	 * @see rs.ville.clagg.sites.SiteHandler#processURL(java.lang.String)
	 */
	@Override
	public boolean processSearchURL(long jobID, String url) 
	{
		return false;
	}

}
