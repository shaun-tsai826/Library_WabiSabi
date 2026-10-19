package com.wabisabi.library.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.wabisabi.library.entity.Book;

import tools.jackson.databind.JsonNode;

/**
 * 呼叫 Google Books API，用 ISBN 查書。
 *
 * API 金鑰放在 application-local.properties 的 google.books.api-key（不進 git）：
 * - 有填：網址加上 &key=...，使用自己專案的額度
 * - 沒填：不帶金鑰呼叫（Google 給匿名呼叫的額度上限是 0，所以一定會回 429，畫面顯示「查詢失敗，請稍後再試」）
 *
 * 呼叫的網址：https://www.googleapis.com/books/v1/volumes?q=isbn:9789861343722&key=你的金鑰
 * 逾時時間在 application.properties 設定（spring.http.clients.connect-timeout / read-timeout）。
 */
@Component
public class GoogleBooksClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);

	private final RestClient restClient;
	private final String apiKey;

	/**
	 * @param builder Spring Boot 準備好的 RestClient 組裝器（已套用 application.properties 的逾時設定）
	 * @param apiKey  設定檔的 google.books.api-key；「:」後面沒寫東西代表沒設定時用空字串
	 */
	public GoogleBooksClient(RestClient.Builder builder, @Value("${google.books.api-key:}") String apiKey) {
		this.restClient = builder.baseUrl("https://www.googleapis.com/books/v1").build();
		this.apiKey = (apiKey == null) ? "" : apiKey.trim();
	}

	/**
	 * 用 ISBN 查書。
	 *
	 * @return 查到 → 裝著 Book 的 Optional；Google 沒有這本書 → 空的 Optional
	 * @throws GoogleBooksUnavailableException Google 無法使用時
	 */
	public Optional<Book> findByIsbn(String isbn) {
		JsonNode root;
		try {
			RestClient.RequestHeadersSpec<?> request = apiKey.isEmpty()
					? restClient.get().uri("/volumes?q=isbn:{isbn}", isbn)
					: restClient.get().uri("/volumes?q=isbn:{isbn}&key={key}", isbn, apiKey);
			root = request.retrieve().body(JsonNode.class);
		} catch (RestClientException e) {
			// 把原因印在後端主控台（例如 429 Too Many Requests、timed out），方便除錯。
			// 錯誤訊息裡可能含有完整網址，所以先把金鑰遮掉，避免金鑰出現在紀錄檔裡
			log.warn("Google Books 查詢失敗（ISBN {}）：{}", isbn, maskKey(e.getMessage(), apiKey));
			throw new GoogleBooksUnavailableException(e);
		}
		return toBook(isbn, root);
	}

	/** 把文字裡的金鑰換成 ***（package 層級可見，方便單元測試） */
	static String maskKey(String text, String key) {
		if (text == null || key == null || key.isEmpty()) {
			return text;
		}
		return text.replace(key, "***");
	}

	/**
	 * 把 Google 回傳的 JSON 轉成 Book。（package 層級可見，方便單元測試直接呼叫）
	 *
	 * Google 回傳的 JSON 大概長這樣（只列出我們用到的部分）：
	 * {
	 *   "totalItems": 1,
	 *   "items": [
	 *     { "volumeInfo": { "title": "書名", "subtitle": "副標題",
	 *                       "authors": ["作者甲", "作者乙"], "publishedDate": "2020-05" } }
	 *   ]
	 * }
	 */
	static Optional<Book> toBook(String isbn, JsonNode root) {
		if (root == null) {
			return Optional.empty();
		}
		JsonNode items = root.path("items");
		if (!items.isArray() || items.size() == 0) {
			return Optional.empty(); // Google 也查不到
		}

		JsonNode info = items.path(0).path("volumeInfo");

		// 書名只取 title，不含副標題（subtitle）
		String title = text(info.path("title"));
		if (title.isEmpty()) {
			return Optional.empty(); // 沒有書名的資料不可靠，當作查不到
		}

		// 多位作者用「、」連接
		List<String> authors = new ArrayList<>();
		for (JsonNode author : info.path("authors")) {
			String name = text(author);
			if (!name.isEmpty()) {
				authors.add(name);
			}
		}

		// 出版日期照原樣（可能是 "2020"、"2020-05" 或 "2020-05-01"）
		String publishDate = text(info.path("publishedDate"));

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
