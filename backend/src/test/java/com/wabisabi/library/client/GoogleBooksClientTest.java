package com.wabisabi.library.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wabisabi.library.entity.Book;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 單元測試：檢查「Google 回傳的 JSON → Book」的轉換規則（設計決策 12）。
 * 不啟動 Spring、不連網，直接餵假的 JSON 字串給 toBook。
 */
class GoogleBooksClientTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private Optional<Book> parse(String json) {
		JsonNode root = jsonMapper.readTree(json);
		return GoogleBooksClient.toBook("9780306406157", root);
	}

	@Test
	@DisplayName("書名只取 title，多位作者用「、」連接，出版日期照原樣")
	void mapsFields() {
		Optional<Book> book = parse("""
				{"totalItems": 1, "items": [{"volumeInfo": {
				  "title": "主書名", "subtitle": "副標題",
				  "authors": ["作者甲", "作者乙", "作者丙"],
				  "publishedDate": "2020-05"
				}}]}
				""");

		assertThat(book).isPresent();
		assertThat(book.get().getIsbn()).isEqualTo("9780306406157");
		assertThat(book.get().getTitle()).isEqualTo("主書名");
		assertThat(book.get().getAuthor()).isEqualTo("作者甲、作者乙、作者丙");
		assertThat(book.get().getPublishDate()).isEqualTo("2020-05");
	}

	@Test
	@DisplayName("出版日期只有年份時照原樣")
	void yearOnlyDate() {
		Optional<Book> book = parse("""
				{"items": [{"volumeInfo": {"title": "書", "authors": ["某人"], "publishedDate": "2004"}}]}
				""");
		assertThat(book.get().getPublishDate()).isEqualTo("2004");
	}

	@Test
	@DisplayName("沒有作者或出版日期時存空字串")
	void missingAuthorAndDate() {
		Optional<Book> book = parse("""
				{"items": [{"volumeInfo": {"title": "只有書名"}}]}
				""");
		assertThat(book.get().getAuthor()).isEmpty();
		assertThat(book.get().getPublishDate()).isEmpty();
	}

	@Test
	@DisplayName("Google 查無資料（totalItems 0、沒有 items）→ 查不到")
	void noItems() {
		assertThat(parse("""
				{"kind": "books#volumes", "totalItems": 0}
				""")).isEmpty();
	}

	@Test
	@DisplayName("沒有書名 → 當作查不到")
	void missingTitle() {
		assertThat(parse("""
				{"items": [{"volumeInfo": {"authors": ["某人"]}}]}
				""")).isEmpty();
	}
}
