package rs.ville.clagg.sites;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import rs.ville.clagg.database.DBConnection;


/**
 * @author cmv
 * Processes Craigslist queries.
 */
public class CraigslistHandler implements SiteHandler 
{
	private final static Logger log = LogManager.getLogger(CraigslistHandler.class);
	
	/**
	 * @see rs.ville.clagg.sites.SiteHandler#processSearchURL(DBConnection, long, String)
	 */
	public boolean processSearchURL(DBConnection db, long jobID, String urlPath) 
	{
		log.debug(String.format("Processing URL %s", urlPath));
		
		try
		{
			// TODO paging - right now we only read the top 100 rows
			// and set up the queries table to have query URLs for each oage

			URL url = new URL(urlPath);
			
			Connection listingConn = Jsoup.connect(urlPath.toString()).ignoreHttpErrors(false);
			
			Document listing = listingConn.get();
			
			LinkedList<String> listingsToDelete = new LinkedList<String>();
			
			// process each row individually
			Elements listings = listing.select("p.row[data-pid]");
			
			for(Element el : listings)
			{
				Element link = el.select("span.pl a").first();
				String fullURL = String.format("%s://%s%s", url.getProtocol(), url.getHost(), link.attr("href"));
				
				if(db.listingExists(fullURL))
				{
					log.debug(String.format("Skipping existing listing %s", fullURL));
					// TODO open the page and see if there's a modification date, if so process it in this loop for safety
					continue;
				}
				
				log.debug(String.format("Adding listing %s", fullURL));
				
				String latData = el.attr("data-latitude");
				String lngData = el.attr("data-longitude");
				
				if(latData.length() == 0)
					latData = "0";
				
				if(lngData.length() == 0)
					lngData = "0";
				
				double lat = Double.parseDouble(latData);
				double lng = Double.parseDouble(lngData);
				
				String title = link.html();
				
				// TODO handle empty string here
				String priceData = el.select("span.price").html().replaceAll("[^0-9]", "");
				
				if(priceData.length() == 0)
					priceData = "0";
				
				int price = Integer.parseInt(priceData);
				
				// TODO reverse-geocode lat/lng into address
				db.createListing(jobID, fullURL, title, price, lat, lng, "x");
			}
		}
		/*catch (MalformedURLException ex)
		{
			log.error(String.format("Malformed URL: %s", urlPath), ex);
		}*/
		catch (Exception ex)
		{
			log.error("Exception while scraping results page", ex);
		}
		
		return true;
	}

	/**
	 * @see rs.ville.clagg.sites.SiteHandler#validateURL(DBConnection, String)
	 */
	public boolean validateListingURL(DBConnection database, String url)
	{
		log.debug(String.format("Validating listing URL %s", url));

		try
		{
			Jsoup.connect(url).get();
			return true;
		}
		catch(HttpStatusException ex)
		{
			if(ex.getStatusCode() == 404)
			{
				return false;
			}
		}
		catch (IOException ex)
		{
			log.warn(String.format("Got IOException while validating listing URL %s", url), ex);
		}
		
		// return true so its status doesn't change and maybe it can be recovered from next time
		return true;
	}

}
