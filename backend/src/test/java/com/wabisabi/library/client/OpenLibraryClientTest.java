package com.wabisabi.library.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wabisabi.library.entity.Book;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 單元測試：檢查「Open Library 回傳的 JSON → Book」的轉換規則。
 * 不啟動 Spring、不連網，直接餵假的 JSON 字串給 toBook。
 */
class OpenLibraryClientTest {

	private static final String ISBN = "9780132350884";

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private Optional<Book> parse(String json) {
		JsonNode root = jsonMapper.readTree(json);
		return OpenLibraryClient.toBook(ISBN, root);
	}

	@Test
	@DisplayName("書名只取 title（不含 subtitle），多位作者用「、」連接，出版日期照原樣")
	void mapsFields() {
		Optional<Book> book = parse("""
				{"ISBN:9780132350884": {
				  "title": "Clean Code",
				  "subtitle": "A Handbook of Agile Software Craftsmanship",
				  "authors": [{"name": "Robert C. Martin"}, {"name": "某位共同作者"}],
				  "publish_date": "July 2008"
				}}
				""");

		assertThat(book).isPresent();
		assertThat(book.get().getIsbn()).isEqualTo(ISBN);
		assertThat(book.get().getTitle()).isEqualTo("Clean Code");
		assertThat(book.get().getAuthor()).isEqualTo("Robert C. Martin、某位共同作者");
		assertThat(book.get().getPublishDate()).isEqualTo("July 2008");
	}

	@Test
	@DisplayName("Open Library 查無資料（回傳空的 {}）→ 查不到")
	void emptyObject() {
		assertThat(parse("{}")).isEmpty();
	}

	@Test
	@DisplayName("回傳的是別的 ISBN（欄位名稱對不上）→ 查不到")
	void differentIsbnKey() {
		assertThat(parse("""
				{"ISBN:9999999999999": {"title": "別本書"}}
				""")).isEmpty();
	}

	@Test
	@DisplayName("沒有書名 → 當作查不到")
	void missingTitle() {
		assertThat(parse("""
				{"ISBN:9780132350884": {"authors": [{"name": "某人"}]}}
				""")).isEmpty();
	}

	@Test
	@DisplayName("沒有作者或出版日期時存空字串")
	void missingAuthorAndDate() {
		Optional<Book> book = parse("""
				{"ISBN:9780132350884": {"title": "只有書名"}}
				""");
		assertThat(book.get().getAuthor()).isEmpty();
		assertThat(book.get().getPublishDate()).isEmpty();
	}

	@Test
	@DisplayName("出版日期超過 20 字元時截斷，避免存檔失敗")
	void longPublishDateIsTruncated() {
		Optional<Book> book = parse("""
				{"ISBN:9780132350884": {"title": "書", "publish_date": "Wednesday, September 30, 2015"}}
				""");
		assertThat(book.get().getPublishDate()).hasSize(20);
	}
}
