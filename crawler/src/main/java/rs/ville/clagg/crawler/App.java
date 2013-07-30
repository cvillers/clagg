package rs.ville.clagg.crawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.*;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final Logger log = LogManager.getLogger(App.class);
	
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
    }
}
