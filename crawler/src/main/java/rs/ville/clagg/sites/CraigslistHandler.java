package rs.ville.clagg.sites;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.*;

import rs.ville.clagg.crawler.database.DBConnection;

import com.colorfulsoftware.rss.Item;
import com.colorfulsoftware.rss.RSS;
import com.colorfulsoftware.rss.RSSDoc;

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
	public boolean processSearchURL(DBConnection db, long jobID, String url) 
	{
		log.debug(String.format("Processing URL %s", url));
		
		try
		{
			RSS feed = new RSSDoc().readRSSToBean(new URL(url));
			LinkedList<String> listingsToProcess = new LinkedList<String>();
					
			// quickly determine what we've already seen before
			for(Item i : feed.getChannel().getItems())
			{
				if(db.listingExists(i.getLink().getLink()))
				{
					continue;
				}
				else
				{
					listingsToProcess.add(i.getLink().getLink());
				}
			}
			
			for(String listing : listingsToProcess)
			{
				Document doc = Jsoup.connect(listing).get();
				
				Element mapNode = doc.select("div#leaflet").first();
				// TODO
				Element addrNode = doc.select("section.cltags p.mapaddress").first();
				
				String address;
				double lat, lng;
				
				if(mapNode == null && addrNode != null)
				{
					// fall back to determining lat and long using geocoder
					log.warn("TODO: fall back to geocoder to find lat and long");
				}
				else if(mapNode != null && addrNode != null)
				{
					lat = Double.parseDouble(mapNode.attr("data-latitude"));
					lng = Double.parseDouble(mapNode.attr("data-longitude"));
					address = addrNode.html().replaceAll("<(.*)>", "");		// remove inner map links
					
					log.debug(String.format("creating listing: url=%s, lat=%f, lng=%f, address=%s", url, lat, lng, address));
					
					db.createListing(url, lat, lng, address);
				}
				else
				{
					log.warn(String.format("Cannot yet parse %s", listing));
				}
			}
		}
		catch (MalformedURLException ex)
		{
			log.error(String.format("Malformed URL: %s", url), ex);
		}
		catch (Exception ex)
		{
			log.error("Exception while loading RSS feed", ex);
		}
		
		return true;
	}

}
