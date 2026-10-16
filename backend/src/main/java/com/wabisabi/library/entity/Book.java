package com.wabisabi.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 書籍：對應資料表 books。
 */
@Entity
@Table(name = "books")
public class Book {

	/** ISBN：13 碼數字，直接當主鍵（每本書的 ISBN 本來就不重複） */
	@Id
	@Column(name = "isbn", length = 13)
	private String isbn;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	/** 作者：多位作者用「、」連接 */
	@Column(name = "author", length = 255)
	private String author;

	/** 出版日期：用字串，因為 Google Books 可能只給「2020」或「2020-05」 */
	@Column(name = "publish_date", length = 20)
	private String publishDate;

	public Book() {
	}

	public Book(String isbn, String title, String author, String publishDate) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publishDate = publishDate;
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
