package rs.ville.clagg.crawler.database;

public class SiteQuery
{
	private String siteName;
	
	private String feedURL;

	/**
	 * @return The name of the site (for internal usage)
	 */
	public String getSiteName()
	{
		return siteName;
	}

	/**
	 * @return The URL
	 */
	public String getFeedURL()
	{
		return feedURL;
	}

	public SiteQuery(String siteName, String feedURL)
	{
		this.siteName = siteName;
		this.feedURL = feedURL;
	}
}
