package rs.ville.clagg;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.*;

import rs.ville.clagg.database.DBConnection;
import rs.ville.clagg.database.Listing;
import rs.ville.clagg.database.SiteQuery;
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
	
	private static void crawl(DBConnection db, Properties props) throws SQLException, InstantiationException, IllegalAccessException
	{
    	long crawlID = db.createJob(DBConnection.JOBTYPE_CRAWL);
    	
    	log.debug("New job ID: " + crawlID);

    	List<SiteQuery> queries = db.getQueryURLs();
    	
    	for(SiteQuery q: queries)
    	{
    		SiteHandler handler = (SiteHandler)handlers.get(q.getSiteName()).newInstance();
    		
    		handler.processSearchURL(db, crawlID, q.getFeedURL());
    	}
    	
    	db.finishJob(crawlID);
	}
	

	private static void validate(final DBConnection db, Properties props, final boolean all) throws SQLException, InstantiationException, IllegalAccessException
	{
		final long PAGE_SIZE = 100;
		
    	long validateID = db.createJob(DBConnection.JOBTYPE_VALIDATE);
    	
    	log.debug("Validation job ID: " + validateID);

    	long total = db.getUnvalidatedListingCount(all);

    	log.debug(String.format("%d total items, will use %d pages", total, total / PAGE_SIZE));
    	
		// FIXME we don't actually have a way to relate a listing to the query it came from
		// so for now, hardcode craigslist, but somehow that will need to get resolved
		// most likely option is to set it going forward, and to assume craigslist for null
		final SiteHandler handler = (SiteHandler)handlers.get("craigslist").newInstance();
    		
    	class ValidatorThread implements Runnable
    	{
    		private long page;
    		//private final static Logger log = LogManager.getLogger(ValidatorThread.class);
    		
    		public ValidatorThread(long page)
    		{
    			this.page = page;    			
    		}
    		
			public void run()
			{
				try
				{
		    		List<Listing> listings = db.getUnvalidatedListings(all, PAGE_SIZE, page);
		    		
		    		List<Long> toDelete = new LinkedList<Long>();
		    		
			    	for(Listing l: listings)
			    	{
			    		if(!handler.validateListingURL(db, l.getURL()))
			    			toDelete.add(l.getID());
			    	}
			    	
		    		log.info(String.format("Out of %d listings, deleting %d", PAGE_SIZE, toDelete.size()));
		    		log.info("---------------------------------------------------");
		    		
		    		db.deleteListings(toDelete);
				}
				catch(SQLException ex)
				{
					log.error(String.format("Error while processing page %d", page), ex);
				}
			}
    	}

    	ExecutorService pool = Executors.newFixedThreadPool(Integer.parseInt(props.getProperty("clagg.numthreads")));
    	
    	for(long page = 0; page <= total / PAGE_SIZE; page++)
    	{
    		ValidatorThread thread = new ValidatorThread(page);
    		
    		pool.execute(thread);
    	}
    	
    	// wait for all of the threads
    	pool.shutdown();
    	
    	while(!pool.isTerminated());
    	
    	db.finishJob(validateID);
	}
	
    public static void main(String[] args)
    {
        Properties props = new Properties();
        String configPath;
        
        if(System.getProperty("clagg.config") == null)
        {
        	configPath = "clagg.properties";
        }
        else
        {
        	configPath = System.getProperty("clagg.config");
        }
        
        try
        {
        	props.load(new FileInputStream(configPath));
        }
        catch(FileNotFoundException ex)
        {
        	log.fatal("Could not find properties file", ex);
        } 
        catch(IOException ex)
        {
			log.fatal("Exception while loading properties file", ex);
		}
        
        DBConnection.init(props);
        
        try
        {
        	// crawl then validate
        	DBConnection db = new DBConnection();
        	
        	if(args.length > 0 && args[0].equals("crawl"))
        		crawl(db, props);
        	else if(args.length > 0 && args[0].equals("validate"))
        		validate(db, props, false);
        	else if(args.length > 0 && args[0].equals("validate-all"))
        		validate(db, props, true);
        	else
        	{
        		crawl(db, props);
        		validate(db, props, false);
        	}
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
