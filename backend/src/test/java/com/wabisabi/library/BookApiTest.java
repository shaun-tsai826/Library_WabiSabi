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

import com.wabisabi.library.client.GoogleBooksClient;
import com.wabisabi.library.client.GoogleBooksUnavailableException;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.repository.BookRepository;

/**
 * 需求 1：首頁查詢書籍（GET /api/books/{isbn}）。
 * Google Books 用 Mockito 模擬（@MockitoBean），測試時不會真的連網。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiTest {

	/** Google Books 有收錄、但本機沒有的測試用 ISBN */
	private static final String GOOGLE_ONLY_ISBN = "9780306406157";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookRepository bookRepository;

	/** 用假的 GoogleBooksClient 取代真的，可以自由決定它要回傳什麼 */
	@MockitoBean
	private GoogleBooksClient googleBooksClient;

	@BeforeEach
	void setUp() {
		// 每個測試開始前，確保這本書不在本機（避免被上一個測試影響）
		bookRepository.deleteById(GOOGLE_ONLY_ISBN);
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

		// 本機就有，不應該去問 Google
		verify(googleBooksClient, never()).findByIsbn(anyString());
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
	}

	@Test
	@DisplayName("Scenario: 本機沒有且 Google Books 無法連線時顯示錯誤")
	void googleUnavailable() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000001"))
				.thenThrow(new GoogleBooksUnavailableException(new RuntimeException("模擬連線失敗")));

		mockMvc.perform(get("/api/books/{isbn}", "9780000000001"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("查詢失敗，請稍後再試"))
				.andExpect(jsonPath("$.title").doesNotExist());

		assertThat(bookRepository.existsById("9780000000001")).isFalse();
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 9780000000000 → 找不到此書籍")
	void bookNotFound() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000000")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/books/{isbn}", "9780000000000"))
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
