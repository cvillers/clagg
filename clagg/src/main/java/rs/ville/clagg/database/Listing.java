package rs.ville.clagg.database;

public class Listing
{
	private long id;
	private String url;
	
	/**
	 * @return The listing's internal ID
	 */
	public long getID()
	{
		return id;
	}

	/**
	 * @return The URL
	 */
	public String getURL()
	{
		return url;
	}
	
	public Listing(long id, String url)
	{
		this.id = id;
		this.url = url;
	}
}
