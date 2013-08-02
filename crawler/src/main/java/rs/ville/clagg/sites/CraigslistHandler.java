package rs.ville.clagg.sites;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.*;

import rs.ville.clagg.crawler.database.DBConnection;

/*import com.colorfulsoftware.rss.Item;
import com.colorfulsoftware.rss.RSS;
import com.colorfulsoftware.rss.RSSDoc;*/

import com.sun.syndication.feed.*;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author cmv
 * Processes Craigslist queries.
 */
public class CraigslistHandler implements SiteHandler 
{
	private final static Logger log = LogManager.getLogger(CraigslistHandler.class);
	
	/**
	 * @see rs.ville.clagg.sites.SiteHandler#processURL(java.lang.String)
	 */
	public boolean processSearchURL(DBConnection db, long jobID, String urlPath) 
	{
		log.debug(String.format("Processing URL %s", urlPath));
		
		try
		{
			// TODO paging - right now we only read the top 100 rows

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
				
				// first, determine if the listing disappeared, and mark it for deletion if so
				try
				{
					Jsoup.connect(fullURL).get();
				}
				catch(HttpStatusException ex)
				{
					if(ex.getStatusCode() == 404)
					{
						log.info(String.format("Removing listing %s", fullURL));
						listingsToDelete.add(fullURL);
						continue;
					}
				}
				
				log.info(String.format("Adding listing %s", fullURL));
				
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

			db.deleteListings(listingsToDelete);
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

}
