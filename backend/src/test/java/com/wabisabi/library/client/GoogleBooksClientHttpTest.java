package com.wabisabi.library.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.wabisabi.library.entity.Book;

/**
 * 單元測試：GoogleBooksClient 實際送出的請求（網址、金鑰）與錯誤處理。
 * MockRestServiceServer 是「假的 Google」：不連網，由測試決定它要回什麼。
 */
class GoogleBooksClientHttpTest {

	private static final String ISBN = "9780306406157";
	private static final String BASE = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + ISBN;
	private static final String ONE_BOOK_JSON = """
			{"totalItems": 1, "items": [{"volumeInfo": {"title": "測試書", "authors": ["某人"], "publishedDate": "2004"}}]}
			""";

	/** 建立一個「接到假 Google」的 GoogleBooksClient */
	private record Setup(GoogleBooksClient client, MockRestServiceServer server) {
	}

	private Setup setup(String apiKey) {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		return new Setup(new GoogleBooksClient(builder, apiKey), server);
	}

	@Test
	@DisplayName("有設定金鑰時，網址會加上 &key=金鑰")
	void sendsApiKeyWhenConfigured() {
		Setup s = setup("TEST_KEY");
		s.server().expect(requestTo(BASE + "&key=TEST_KEY"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(ONE_BOOK_JSON, MediaType.APPLICATION_JSON));

		Optional<Book> book = s.client().findByIsbn(ISBN);

		assertThat(book).isPresent();
		assertThat(book.get().getTitle()).isEqualTo("測試書");
		s.server().verify(); // 確認真的送出了上面預期的請求
	}

	@Test
	@DisplayName("沒有設定金鑰時，網址不帶 key 參數")
	void noApiKeyWhenBlank() {
		Setup s = setup("   ");
		s.server().expect(requestTo(BASE))
				.andRespond(withSuccess(ONE_BOOK_JSON, MediaType.APPLICATION_JSON));

		assertThat(s.client().findByIsbn(ISBN)).isPresent();
		s.server().verify();
	}

	@Test
	@DisplayName("Google 查無資料（totalItems 0）→ 回傳空的 Optional")
	void googleHasNoSuchBook() {
		Setup s = setup("TEST_KEY");
		s.server().expect(requestTo(BASE + "&key=TEST_KEY"))
				.andRespond(withSuccess("{\"totalItems\": 0}", MediaType.APPLICATION_JSON));

		assertThat(s.client().findByIsbn(ISBN)).isEmpty();
	}

	@Test
	@DisplayName("Google 回 429（額度用完）→ 丟出 GoogleBooksUnavailableException")
	void quotaExceeded() {
		Setup s = setup("TEST_KEY");
		s.server().expect(requestTo(BASE + "&key=TEST_KEY"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThatThrownBy(() -> s.client().findByIsbn(ISBN))
				.isInstanceOf(GoogleBooksUnavailableException.class);
	}

	@Test
	@DisplayName("紀錄錯誤時會把金鑰遮成 ***")
	void maskKeyHidesSecret() {
		String message = "I/O error on GET request for \"https://www.googleapis.com/books/v1/volumes?q=isbn:1&key=SECRET123\"";

		assertThat(GoogleBooksClient.maskKey(message, "SECRET123"))
				.doesNotContain("SECRET123")
				.contains("key=***");
		// 沒有金鑰時原樣回傳
		assertThat(GoogleBooksClient.maskKey(message, "")).isEqualTo(message);
	}
}
