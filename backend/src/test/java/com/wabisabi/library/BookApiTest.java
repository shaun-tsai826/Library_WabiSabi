package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.wabisabi.library.client.BookSourceUnavailableException;
import com.wabisabi.library.client.GoogleBooksClient;
import com.wabisabi.library.client.OpenLibraryClient;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.repository.BookRepository;

/**
 * 需求 1：首頁查詢書籍（GET /api/books/{isbn}）。
 * Google Books 與 Open Library 都用 Mockito 模擬（@MockitoBean），測試時不會真的連網。
 * 沒有特別設定時，模擬物件的 findByIsbn 會回傳空的 Optional（也就是「查無」）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiTest {

	/** Google Books 有收錄、但本機沒有的測試用 ISBN */
	private static final String GOOGLE_ONLY_ISBN = "9780306406157";

	/** 只有 Open Library 有收錄（本機與 Google 都沒有）的測試用 ISBN */
	private static final String OPEN_LIBRARY_ONLY_ISBN = "9780132350884";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookRepository bookRepository;

	/** 用假的 GoogleBooksClient 取代真的，可以自由決定它要回傳什麼 */
	@MockitoBean
	private GoogleBooksClient googleBooksClient;

	/** 用假的 OpenLibraryClient 取代真的（BDD v1.2.0 的備援來源） */
	@MockitoBean
	private OpenLibraryClient openLibraryClient;

	@BeforeEach
	void setUp() {
		// 每個測試開始前，確保這些書不在本機（避免被上一個測試影響）
		bookRepository.deleteById(GOOGLE_ONLY_ISBN);
		bookRepository.deleteById(OPEN_LIBRARY_ONLY_ISBN);
	}

	@Test
	@DisplayName("Scenario: 成功查詢到存在的書籍資訊")
	void findExistingBook() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "9789861343722"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isbn").value("9789861343722"))
				.andExpect(jsonPath("$.title").value("侘寂：尋找日常中的不完美之美"))
				.andExpect(jsonPath("$.author").value("雷納·克魯克"))
				.andExpect(jsonPath("$.publishDate").value("2020-05-01"));

		// 本機就有，不應該去問任何外部來源
		verify(googleBooksClient, never()).findByIsbn(anyString());
		verify(openLibraryClient, never()).findByIsbn(anyString());
	}

	@Test
	@DisplayName("Scenario: 本機沒有但 Google Books 有收錄的書籍，可查詢並存回本機（出版日期只有年，照原樣顯示）")
	void findBookFromGoogleAndSave() throws Exception {
		when(googleBooksClient.findByIsbn(GOOGLE_ONLY_ISBN))
				.thenReturn(Optional.of(new Book(GOOGLE_ONLY_ISBN, "Google 測試書", "作者甲、作者乙", "2004")));

		mockMvc.perform(get("/api/books/{isbn}", GOOGLE_ONLY_ISBN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Google 測試書"))
				.andExpect(jsonPath("$.author").value("作者甲、作者乙"))
				.andExpect(jsonPath("$.publishDate").value("2004"));

		// 應該已經存進本機 books 表
		Book saved = bookRepository.findById(GOOGLE_ONLY_ISBN).orElseThrow();
		assertThat(saved.getTitle()).isEqualTo("Google 測試書");
		assertThat(saved.getPublishDate()).isEqualTo("2004");

		// Google 查到了，系統不需再查詢 Open Library
		verify(openLibraryClient, never()).findByIsbn(anyString());
	}

	@Test
	@DisplayName("Scenario: 本機與 Google Books 都沒有、但 Open Library 有收錄的書籍，可查詢並存回本機（出版日期照原樣）")
	void findBookFromOpenLibraryAndSave() throws Exception {
		when(googleBooksClient.findByIsbn(OPEN_LIBRARY_ONLY_ISBN)).thenReturn(Optional.empty());
		when(openLibraryClient.findByIsbn(OPEN_LIBRARY_ONLY_ISBN))
				.thenReturn(Optional.of(new Book(OPEN_LIBRARY_ONLY_ISBN, "Clean Code", "Robert C. Martin", "July 2008")));

		mockMvc.perform(get("/api/books/{isbn}", OPEN_LIBRARY_ONLY_ISBN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Clean Code"))
				.andExpect(jsonPath("$.author").value("Robert C. Martin"))
				.andExpect(jsonPath("$.publishDate").value("July 2008"));

		Book saved = bookRepository.findById(OPEN_LIBRARY_ONLY_ISBN).orElseThrow();
		assertThat(saved.getTitle()).isEqualTo("Clean Code");
		assertThat(saved.getPublishDate()).isEqualTo("July 2008");
	}

	@Test
	@DisplayName("Scenario: Google Books 無法連線但 Open Library 有收錄，仍可查詢")
	void googleUnavailableButOpenLibraryHasBook() throws Exception {
		when(googleBooksClient.findByIsbn(OPEN_LIBRARY_ONLY_ISBN))
				.thenThrow(new BookSourceUnavailableException("Google Books", new RuntimeException("模擬連線失敗")));
		when(openLibraryClient.findByIsbn(OPEN_LIBRARY_ONLY_ISBN))
				.thenReturn(Optional.of(new Book(OPEN_LIBRARY_ONLY_ISBN, "Clean Code", "Robert C. Martin", "July 2008")));

		mockMvc.perform(get("/api/books/{isbn}", OPEN_LIBRARY_ONLY_ISBN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Clean Code"));

		assertThat(bookRepository.existsById(OPEN_LIBRARY_ONLY_ISBN)).isTrue();
	}

	@Test
	@DisplayName("Scenario Outline: 本機沒有且外部來源無法確認時顯示錯誤 | Google 無法連線、Open Library 無法連線")
	void bothSourcesUnavailable() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000001"))
				.thenThrow(new BookSourceUnavailableException("Google Books", new RuntimeException("模擬連線失敗")));
		when(openLibraryClient.findByIsbn("9780000000001"))
				.thenThrow(new BookSourceUnavailableException("Open Library", new RuntimeException("模擬連線失敗")));

		mockMvc.perform(get("/api/books/{isbn}", "9780000000001"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("查詢失敗，請稍後再試"))
				.andExpect(jsonPath("$.title").doesNotExist());

		assertThat(bookRepository.existsById("9780000000001")).isFalse();
	}

	@Test
	@DisplayName("Scenario Outline: 本機沒有且外部來源無法確認時顯示錯誤 | Google 查無、Open Library 無法連線")
	void googleNotFoundOpenLibraryUnavailable() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000001")).thenReturn(Optional.empty());
		when(openLibraryClient.findByIsbn("9780000000001"))
				.thenThrow(new BookSourceUnavailableException("Open Library", new RuntimeException("模擬連線失敗")));

		mockMvc.perform(get("/api/books/{isbn}", "9780000000001"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("查詢失敗，請稍後再試"))
				.andExpect(jsonPath("$.title").doesNotExist());
	}

	@Test
	@DisplayName("Scenario Outline: 本機沒有且外部來源無法確認時顯示錯誤 | Google 無法連線、Open Library 查無")
	void googleUnavailableOpenLibraryNotFound() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000001"))
				.thenThrow(new BookSourceUnavailableException("Google Books", new RuntimeException("模擬連線失敗")));
		when(openLibraryClient.findByIsbn("9780000000001")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/books/{isbn}", "9780000000001"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("查詢失敗，請稍後再試"))
				.andExpect(jsonPath("$.title").doesNotExist());
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 9789999999999 → 找不到此書籍（兩個來源都查無）")
	void bookNotFound() throws Exception {
		when(googleBooksClient.findByIsbn("9789999999999")).thenReturn(Optional.empty());
		when(openLibraryClient.findByIsbn("9789999999999")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/books/{isbn}", "9789999999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此書籍"))
				.andExpect(jsonPath("$.title").doesNotExist());
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | INVALID_ISBN → ISBN 碼格式錯誤或無效")
	void invalidIsbnLetters() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "INVALID_ISBN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ISBN 碼格式錯誤或無效"))
				.andExpect(jsonPath("$.title").doesNotExist());

		// 格式錯誤就不該去問 Google
		verify(googleBooksClient, never()).findByIsbn(anyString());
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 978986134372（12 碼）→ ISBN 碼格式錯誤或無效")
	void invalidIsbnTwelveDigits() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "978986134372"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ISBN 碼格式錯誤或無效"));
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 空白 → 請輸入 ISBN 碼（後端第二道防線，正常由前端擋下）")
	void blankIsbn() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("請輸入 ISBN 碼"));
	}

	@Test
	@DisplayName("規則：ISBN 前後空白會先去除")
	void isbnIsTrimmed() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "  9789861343722  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isbn").value("9789861343722"));
	}
}
