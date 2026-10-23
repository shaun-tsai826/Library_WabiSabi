package com.wabisabi.library.dto;

import java.time.LocalDateTime;

import com.wabisabi.library.common.Categories;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.Favorite;

/**
 * 收藏清單中的一筆（含書籍資料）：
 * {"id": 5, "isbn": "9789861343722", "title": "...", "author": "...", "publishDate": "2020-05-01",
 *  "category": "散文", "createdAt": "2026-10-24T10:15:30.123456"}
 * id 是「收藏」的編號，前端按「刪除」時用它呼叫 DELETE /api/favorites/{id}。
 */
public class FavoriteResponse {

	private Long id;
	private String isbn;
	private String title;
	private String author;
	private String publishDate;
	/** 書籍種類；資料庫是空值時回傳「未分類」 */
	private String category;
	/** 收藏時間（BDD v1.5.0）：Jackson 會轉成 ISO 格式字串，前端取前 7 個字（例如 2026-10）當月份 */
	private LocalDateTime createdAt;

	public FavoriteResponse() {
	}

	public FavoriteResponse(Favorite favorite) {
		Book book = favorite.getBook();
		this.id = favorite.getId();
		this.isbn = book.getIsbn();
		this.title = book.getTitle();
		this.author = book.getAuthor();
		this.publishDate = book.getPublishDate();
		this.category = Categories.orDefault(favorite.getCategory());
		this.createdAt = favorite.getCreatedAt();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
