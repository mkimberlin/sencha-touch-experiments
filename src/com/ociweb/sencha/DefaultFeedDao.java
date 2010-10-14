package com.ociweb.sencha;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DefaultFeedDao implements FeedDao {
    public Document retrieveFeed(String url) throws MalformedURLException,
            ParserConfigurationException, SAXException, IOException {
        final URL feedUrl = new URL(url);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder();
        Document doc = builder.parse(feedUrl.openStream());
        return doc;
    }
}
