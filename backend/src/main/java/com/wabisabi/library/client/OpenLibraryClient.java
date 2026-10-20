package com.wabisabi.library.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.wabisabi.library.entity.Book;

import tools.jackson.databind.JsonNode;

/**
 * 呼叫 Open Library API，用 ISBN 查書（Google Books 查不到或無法使用時的備援來源）。
 * 免費、不需要 API 金鑰。
 *
 * 呼叫的網址：https://openlibrary.org/api/books?bibkeys=ISBN:9780132350884&format=json&jscmd=data
 * 逾時時間與 Google Books 共用 application.properties 的 spring.http.clients.* 設定。
 */
@Component
public class OpenLibraryClient {

	private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);

	/** Open Library 希望呼叫的程式在 User-Agent 標頭寫上自己的名字，方便他們辨識流量 */
	static final String USER_AGENT = "LibraryWabiSabi/1.0 (student project)";

	private final RestClient restClient;

	public OpenLibraryClient(RestClient.Builder builder) {
		this.restClient = builder
				.baseUrl("https://openlibrary.org")
				.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
				.build();
	}

	/**
	 * 用 ISBN 查書。
	 *
	 * @return 查到 → 裝著 Book 的 Optional；Open Library 沒有這本書 → 空的 Optional
	 * @throws BookSourceUnavailableException Open Library 無法使用時
	 */
	public Optional<Book> findByIsbn(String isbn) {
		JsonNode root;
		try {
			root = restClient.get()
					.uri("/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data", isbn)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientException e) {
			log.warn("Open Library 查詢失敗（ISBN {}）：{}", isbn, e.getMessage());
			throw new BookSourceUnavailableException("Open Library", e);
		}
		return toBook(isbn, root);
	}

	/**
	 * 把 Open Library 回傳的 JSON 轉成 Book。（package 層級可見，方便單元測試直接呼叫）
	 *
	 * 查到時的 JSON 大概長這樣（只列出我們用到的部分）；查不到時是空的 {}：
	 * {
	 *   "ISBN:9780132350884": {
	 *     "title": "Clean Code",
	 *     "subtitle": "A Handbook of Agile Software Craftsmanship",
	 *     "authors": [ { "name": "Robert C. Martin" } ],
	 *     "publish_date": "July 2008"
	 *   }
	 * }
	 */
	static Optional<Book> toBook(String isbn, JsonNode root) {
		if (root == null) {
			return Optional.empty();
		}
		// 外層的欄位名稱就是我們送出的 "ISBN:xxxx"
		JsonNode data = root.path("ISBN:" + isbn);
		if (data.isMissingNode() || data.isNull()) {
			return Optional.empty(); // Open Library 也查不到
		}

		// 書名只取 title，不含副標題（subtitle），規則與 Google Books 相同
		String title = text(data.path("title"));
		if (title.isEmpty()) {
			return Optional.empty(); // 沒有書名的資料不可靠，當作查不到
		}

		// 多位作者用「、」連接；Open Library 的作者是 [{ "name": "..." }] 的格式
		List<String> authors = new ArrayList<>();
		for (JsonNode author : data.path("authors")) {
			String name = text(author.path("name"));
			if (!name.isEmpty()) {
				authors.add(name);
			}
		}

		// 出版日期照原樣（可能是 "1999"、"July 2008"、"December 27, 2017"）
		String publishDate = text(data.path("publish_date"));

		// 長度不超過資料表欄位上限，避免存檔失敗
		return Optional.of(new Book(isbn, limit(title, 255), limit(String.join("、", authors), 255),
				limit(publishDate, 20)));
	}

	/** 超過長度就截斷 */
	private static String limit(String value, int maxLength) {
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}

	/** 取出文字；欄位不存在或是 null 時回傳空字串 */
	private static String text(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return "";
		}
		return node.asString().trim();
	}
}
