package com.wabisabi.library.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.wabisabi.library.client.BookSourceUnavailableException;
import com.wabisabi.library.client.GoogleBooksClient;
import com.wabisabi.library.client.OpenLibraryClient;
import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.BookResponse;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.repository.BookRepository;

/**
 * 書籍查詢的規則都在這裡。
 */
@Service
public class BookService {

	private final BookRepository bookRepository;
	private final GoogleBooksClient googleBooksClient;
	private final OpenLibraryClient openLibraryClient;

	public BookService(BookRepository bookRepository, GoogleBooksClient googleBooksClient,
			OpenLibraryClient openLibraryClient) {
		this.bookRepository = bookRepository;
		this.googleBooksClient = googleBooksClient;
		this.openLibraryClient = openLibraryClient;
	}

	/**
	 * 用 ISBN 查書。順序（BDD v1.2.0）：
	 * 1. 去除前後空白，檢查是不是空的、是不是 13 碼數字
	 * 2. 先查本機 books 表，有就直接回傳
	 * 3. 本機沒有 → 問 Google Books；查到就存回本機再回傳
	 * 4. Google 查無或無法使用 → 問 Open Library；查到就存回本機再回傳
	 * 5. 都沒查到：
	 *    - 兩個來源都明確回答「查無」→ 404 找不到此書籍
	 *    - 有任一來源無法使用 → 503 查詢失敗，請稍後再試（無法使用的那個來源可能有這本書）
	 */
	public BookResponse findByIsbn(String rawIsbn) {
		// 1. 格式檢查（正常情況前端已經擋掉空白，這裡是第二道防線）
		String isbn = normalizeIsbn(rawIsbn);

		// 2. 先查本機
		Optional<Book> localBook = bookRepository.findById(isbn);
		if (localBook.isPresent()) {
			return new BookResponse(localBook.get());
		}

		// 記錄「有沒有哪個外部來源無法使用」，決定最後要回 404 還是 503
		boolean anySourceUnavailable = false;

		// 3. 問 Google Books
		try {
			Optional<Book> googleBook = googleBooksClient.findByIsbn(isbn);
			if (googleBook.isPresent()) {
				return saveAndRespond(googleBook.get()); // 查到了就不用再問 Open Library
			}
		} catch (BookSourceUnavailableException e) {
			anySourceUnavailable = true; // 連不上沒關係，繼續問下一個來源
		}

		// 4. 問 Open Library
		try {
			Optional<Book> openLibraryBook = openLibraryClient.findByIsbn(isbn);
			if (openLibraryBook.isPresent()) {
				return saveAndRespond(openLibraryBook.get());
			}
		} catch (BookSourceUnavailableException e) {
			anySourceUnavailable = true;
		}

		// 5. 都沒查到
		if (anySourceUnavailable) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, Messages.BOOK_LOOKUP_FAILED);
		}
		throw new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND);
	}

	/** 存回本機（下次就不用再問外部來源），再轉成回應 */
	private BookResponse saveAndRespond(Book book) {
		Book saved = bookRepository.save(book);
		return new BookResponse(saved);
	}

	/**
	 * ISBN 格式規則（查書與加入收藏共用）：去除前後空白後，必須是 13 碼數字。
	 *
	 * @return 去除空白後的 ISBN
	 * @throws ApiException 空白或格式錯誤時（400）
	 */
	public static String normalizeIsbn(String rawIsbn) {
		String isbn = (rawIsbn == null) ? "" : rawIsbn.trim();
		if (isbn.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.ISBN_REQUIRED);
		}
		if (!isbn.matches("[0-9]{13}")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.ISBN_INVALID);
		}
		return isbn;
	}
}
