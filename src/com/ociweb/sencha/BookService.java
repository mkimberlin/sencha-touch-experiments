package com.ociweb.sencha;

/**
 * A service for accessing information on any of the titles at Podiobooks.
 * 
 * @author mkimberlin
 */
public interface BookService {

    public BookList getBooks();
    
    public void storeBooks(BookList books);
    
    public void deleteBooks(BookList books);
}