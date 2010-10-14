package com.ociweb.sencha;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public interface FeedDao {
    public Document retrieveFeed(String url) throws MalformedURLException,
        ParserConfigurationException, SAXException, IOException;
}
