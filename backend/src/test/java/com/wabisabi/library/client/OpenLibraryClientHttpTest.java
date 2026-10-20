package com.wabisabi.library.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.wabisabi.library.entity.Book;

/**
 * 單元測試：OpenLibraryClient 實際送出的請求（網址、User-Agent）與錯誤處理。
 * MockRestServiceServer 是「假的 Open Library」：不連網，由測試決定它要回什麼。
 */
class OpenLibraryClientHttpTest {

	private static final String ISBN = "9780132350884";
	private static final String URL = "https://openlibrary.org/api/books?bibkeys=ISBN:" + ISBN + "&format=json&jscmd=data";

	/** 建立一個「接到假 Open Library」的 OpenLibraryClient */
	private record Setup(OpenLibraryClient client, MockRestServiceServer server) {
	}

	private Setup setup() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		return new Setup(new OpenLibraryClient(builder), server);
	}

	@Test
	@DisplayName("送出正確的網址，並在 User-Agent 帶上程式名稱")
	void sendsExpectedRequest() {
		Setup s = setup();
		s.server().expect(requestTo(URL))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.USER_AGENT, OpenLibraryClient.USER_AGENT))
				.andRespond(withSuccess("""
						{"ISBN:9780132350884": {"title": "Clean Code", "authors": [{"name": "Robert C. Martin"}], "publish_date": "July 2008"}}
						""", MediaType.APPLICATION_JSON));

		Optional<Book> book = s.client().findByIsbn(ISBN);

		assertThat(book).isPresent();
		assertThat(book.get().getTitle()).isEqualTo("Clean Code");
		s.server().verify(); // 確認真的送出了上面預期的請求
	}

	@Test
	@DisplayName("Open Library 查無資料（{}）→ 回傳空的 Optional")
	void notFound() {
		Setup s = setup();
		s.server().expect(requestTo(URL))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		assertThat(s.client().findByIsbn(ISBN)).isEmpty();
	}

	@Test
	@DisplayName("Open Library 回 500（伺服器錯誤）→ 丟出 BookSourceUnavailableException")
	void serverError() {
		Setup s = setup();
		s.server().expect(requestTo(URL))
				.andRespond(withServerError());

		assertThatThrownBy(() -> s.client().findByIsbn(ISBN))
				.isInstanceOf(BookSourceUnavailableException.class)
				.hasMessageContaining("Open Library");
	}
}
