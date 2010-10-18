package com.ociweb.sencha;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name="bookList")
public class BookList {
    private List<Book> books;
    private String error;
}
