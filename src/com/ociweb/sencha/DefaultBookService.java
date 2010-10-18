package com.ociweb.sencha;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.jersey.spi.resource.Singleton;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * The default implementation of the book service, exposing the methods as a
 * RESTful web service via the Jersey JAX-RS framework.
 * 
 * Most of the data extraction in this service is done by "screen scraping" the
 * current Podiobooks.com site (January, 2010) and is likely to break with any
 * significant changes to the site.  Future implementations of these services
 * will be done in tandem with the development of the new Podiobooks.com site.
 * 
 * @author mkimberlin
 */
@Path("books")
@Singleton
public class DefaultBookService extends SiteParsingService implements BookService {
    private static final Logger log = Logger.getLogger(DefaultBookService.class.getName());
     
    FeedDao feedDao = new DefaultFeedDao();
    Random random = new Random(new Date().getTime());

    private static final String MAIN_URL = "http://www.podiobooks.com/";
    
    @GET
    @Produces("application/json")
    @Override public BookList getBooks() {
        BookList list = new BookList();
        List<Book> books = new ArrayList<Book>();
        try {
            URL url = new URL(MAIN_URL);
            StringBuilder result = readContents(url);
            
            //Snip out the recent updates section...
            result.delete(0, result.indexOf("<p>Recent Updates</p>"));
            result.delete(result.indexOf("</ul>"), result.length());
            books = extractRecentUpdates(result);
        } catch (IOException e) {
            log.severe("An unexpected error occurred while retrieving recent book updates: " + e.getMessage());
            list.setError("An error occurred while loading the recently updated titles.  "+
                    "This is likely because of slowness on the site.  Please go back and try again.  "+
                    "If the problem persists please <a href='mailto:mkimberlin@gmail.com'>let me know</a>.");
        }
        List<Book> fullBooks = new ArrayList<Book>();
        for(Book book: books) {
			Book fullBook = getBookByFeedUrl(book.getFeedUrl());
			fullBook.setLastUpdated(book.getLastUpdated());
			fullBook.setTitle(book.getTitle());
			fullBooks.add(fullBook);
        }
        list.setBooks(fullBooks);
        return list;
    }
    
    @POST
    public void createBooks(BookList books) {
    	storeBooks(books);
    }
    
    @PUT
    @Override public void storeBooks(BookList books) {
    	for(Book book: books.getBooks()) {
    		log.info("Storing a book (ok, not really): " + book);
    	}
    }
    
    @DELETE
    @Override public void deleteBooks(BookList books) {
    	for(Book book: books.getBooks()) {
    		log.info("Deleting a book (ok, not really): " + book);
    	}
    }
    
    /**
     * Retrieves a <code>Book</code> from its RSS feed at the provided URL. If
     * an error occurs during the retrieval or processing of the feed, then
     * an empty book will be returned.
     * 
     * @param url  the URL of the RSS feed
     * @return the <code>Book</code> fully populated with information extracted
     *         from the RSS feed
     */
    private Book getBookByFeedUrl(String url) {
        Book book = null;
        try {
            Document doc = feedDao.retrieveFeed(url);
            book = constructBookFromDetailedFeed(doc);
            book.setFeedUrl(url);
        } catch (Exception e) {
            log.severe("An error occurred while retrieving or parsing the RSS feed \""
                + url + "\": " + e.getMessage());
            book = new Book();
            book.setError("An error occurred while loading the requested title.  "+
                    "This is likely because of slowness on the site.  Please go back and try again.  "+
                    "If the problem persists please <a href='mailto:mkimberlin@gmail.com'>let me know</a>.");
        }

        return book;
    }

    /**
     * Constructs a book object populated by the data taken from the
     * provided RSS document.
     * 
     * @param doc  the <code>Document</code> containing the parsed RSS feed
     * @return the <code>Book</code> fully populated with information extracted
     *         from the RSS feed
     * @throws FeedException  if the feed could not be parsed
     */
    @SuppressWarnings("unchecked")
    private Book constructBookFromDetailedFeed(Document doc)
            throws FeedException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(doc);
        
        Book book = new Book();
        book.setTitle(feed.getTitle().substring(0,
            feed.getTitle().indexOf(" - A free audiobook by")));
        book.setCopyright(feed.getCopyright());
        book.setDescription(feed.getDescription());
        book.setImageUrl(feed.getImage().getUrl());
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
        book.setLastUpdated(formatter.format(feed.getPublishedDate()));
        book.setCategories(parseCategoryNames(feed.getCategories()));
        book.setUrl(feed.getLink());

        book.setAuthors(parseAuthors(doc));
        scrubBook(book);
        return book;
    }
    
    /**
     * Parses any authors from the iTunes specific author tags, ignoring
     * "Podiobooks Staff" that occurs for the "The End" or "Your caught up"
     * files.
     * 
     * @param doc  the parsed RSS feed
     * @return  a list of author names
     */
    private List<String> parseAuthors(Document doc) {
        // Author is not provided as a part of the core RSS portion of the
        // feed.  As such, it must be taken from the iTunes portion.
        NodeList authorNodes = doc.getElementsByTagName("itunes:author");
        
        List<String> authors = new ArrayList<String>();
        for(int i=0; i<authorNodes.getLength(); i++) {
            Node authorNode = authorNodes.item(i);
            if (authorNode != null) {
                String author = authorNode.getTextContent();
                if(!authors.contains(author) && !"Podiobooks Staff".equalsIgnoreCase(author))
                    authors.add(author);
            }    
        }
        return authors;
    }

    /**
     * Parses a the list of category names from those in the feed.
     * 
     * @param categories  a list of syndicated categories parsed by ROME
     * @return a list of category names
     */
    private List<String> parseCategoryNames(List<SyndCategory> categories) {
        List<String> catNames = new ArrayList<String>();
        for (SyndCategory cat : categories) {
            catNames.add(cat.getName());
        }
        return catNames;
    }

    /**
     * Extracts a list of <code>Book</code>s recently updated from the site's
     * front page.  The number of episodes updated is appended to the book's
     * title.
     * 
     * @param result  the recent updates section of the podiobooks.com main page 
     * @return the list of <code>Book</code>s recently updated, with each
     *         book's title, last updated date and feed url populated and
     *         the number of episodes updated appended to the book's title
     */
    private List<Book> extractRecentUpdates(StringBuilder result) {
        List<Book> books = new ArrayList<Book>();        
        Pattern datePattern = Pattern.compile("(0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])[- /.]\\d\\d");
        Pattern episodePattern = Pattern.compile("\\s\\d+\\D");
        while(result.indexOf("/title/") >= 0) {
            //Match the date and number of episodes
            String date = getFirstMatch(result, datePattern);
            String episodes = getFirstMatch(result, episodePattern);
            episodes = episodes.substring(0, episodes.length()-1).trim();
            
            //Extract the title fragment from the href
            int nextPos = result.indexOf("/title/")+7;
            int endPos = result.indexOf("/\"", nextPos);
            String titleUrlFragment = result.substring(nextPos, endPos);
            result.delete(0, endPos);

            //Extract the book title
            nextPos = result.indexOf(">")+1;
            endPos = result.indexOf("</a");
            String title = result.substring(nextPos, endPos);
            result.delete(0, endPos);
            
            StringBuilder titleBuilder = new StringBuilder(title);
            titleBuilder.append(" - ").append(episodes).append(" Episodes");
            Book book = constructBook(date, titleUrlFragment, titleBuilder.toString());
            books.add(book);
        }
        return books;
    }

}
