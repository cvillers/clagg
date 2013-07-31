package rs.ville.clagg.crawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.*;

import rs.ville.clagg.crawler.database.DBConnection;
import rs.ville.clagg.crawler.database.DBConnectionManager;
import rs.ville.clagg.crawler.database.SiteQuery;
import rs.ville.clagg.sites.CraigslistHandler;
import rs.ville.clagg.sites.SiteHandler;

public class App 
{
	private final static Logger log = LogManager.getLogger(App.class);
	
	// maps to queries.site_name
	private final static HashMap<String, Class> handlers;
	
	static
	{
		handlers = new HashMap<String, Class>();
		
		handlers.put("craigslist", CraigslistHandler.class);
	}
	
    public static void main(String[] args)
    {
        Properties props = new Properties();
        String configPath;
        
        if(System.getProperty("clagg.crawlerconfig") == null)
        {
        	configPath = "crawler.properties";
        }
        else
        {
        	configPath = System.getProperty("clagg.crawlerconfig");
        }
        
        try
        {
        	props.load(new FileInputStream(configPath));
        }
        catch(FileNotFoundException ex)
        {
        	log.fatal("Could not find crawler.properties", ex);
        } 
        catch(IOException ex)
        {
			log.fatal("Exception while loading crawler.properties", ex);
		}
        
        DBConnectionManager.init(props);
        
        try
        {
        	DBConnection db = DBConnectionManager.getConnection();
        	
        	long id = db.createJob(DBConnection.JOBTYPE_CRAWL);
        	
        	log.debug("New job ID: " + id);

        	List<SiteQuery> queries = db.getQueryURLs();
        	
        	for(SiteQuery q: queries)
        	{
        		SiteHandler handler = (SiteHandler)handlers.get(q.getSiteName()).newInstance();
        		
        		handler.processSearchURL(db, id, q.getURL());
        	}
        	
        	db.finishJob(id);
        	
        	db.close();
        }
        catch(SQLException ex)
        {
        	log.fatal("Could not create new job", ex);
        }
		catch (InstantiationException ex)
		{
			log.fatal("Could not instantiate handler class", ex);
		}
		catch (IllegalAccessException ex)
		{
			log.fatal("Illegal access during handler instantiation", ex);
		}
        
    }
}
