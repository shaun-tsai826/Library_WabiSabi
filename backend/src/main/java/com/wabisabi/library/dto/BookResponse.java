package com.wabisabi.library.dto;

import com.wabisabi.library.entity.Book;

/**
 * 回傳給前端的書籍資料。轉成 JSON 後長這樣：
 * {"isbn": "9789861343722", "title": "...", "author": "...", "publishDate": "2020-05-01"}
 */
public class BookResponse {

	private String isbn;
	private String title;
	private String author;
	private String publishDate;

	public BookResponse() {
	}

	/** 從 Entity 複製資料過來 */
	public BookResponse(Book book) {
		this.isbn = book.getIsbn();
		this.title = book.getTitle();
		this.author = book.getAuthor();
		this.publishDate = book.getPublishDate();
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}
}
