package rs.ville.clagg.sites;

public class SiteQuery
{
	private String siteName;
	
	private String url;

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
	public String getURL()
	{
		return url;
	}

	public SiteQuery(String siteName, String url)
	{
		this.siteName = siteName;
		this.url = url;
	}
}
