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
	 *    （舊資料從沒補過出版社、頁數、封面時，先向外部來源補一次，見 fillDetailsOfLocalBook）
	 * 3. 本機沒有 → 問 Google Books；查到就存回本機再回傳
	 *    （Google 缺出版社或頁數時，先問 Open Library 補上缺的欄位再存）
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
			Book book = localBook.get();
			if (book.getPublisher() == null) {
				fillDetailsOfLocalBook(book);
			}
			return new BookResponse(book);
		}

		// 記錄「有沒有哪個外部來源無法使用」，決定最後要回 404 還是 503
		boolean anySourceUnavailable = false;

		// 3. 問 Google Books
		try {
			Optional<Book> googleBook = googleBooksClient.findByIsbn(isbn);
			if (googleBook.isPresent()) {
				fillMissingFromOpenLibrary(googleBook.get());
				return saveAndRespond(googleBook.get());
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

	/**
	 * Google 查到的書缺出版社或頁數時，問 Open Library 補上（只補缺的欄位，其他欄位以 Google 為準）。
	 * Open Library 查無或無法使用都沒關係，就照 Google 的資料存。
	 */
	private void fillMissingFromOpenLibrary(Book book) {
		boolean missingPublisher = book.getPublisher() == null || book.getPublisher().isEmpty();
		boolean missingPageCount = book.getPageCount() == null;
		if (!missingPublisher && !missingPageCount) {
			return; // 都有了，不用多問一次
		}

		Optional<Book> openLibraryBook;
		try {
			openLibraryBook = openLibraryClient.findByIsbn(book.getIsbn());
		} catch (BookSourceUnavailableException e) {
			return;
		}
		if (openLibraryBook.isEmpty()) {
			return;
		}

		if (missingPublisher) {
			book.setPublisher(openLibraryBook.get().getPublisher());
		}
		if (missingPageCount) {
			book.setPageCount(openLibraryBook.get().getPageCount());
		}
	}

	/**
	 * 本機的舊資料（加入出版社、頁數、封面功能之前存的）這三個欄位都是 NULL，在這裡補一次。
	 * 只補這三個欄位，書名、作者、出版日期維持原樣（收藏過的書資料不會變成別的版本）。
	 *
	 * 怎麼分辨「補過了沒」：外部來源查到的書，出版社至少會是空字串 ""，只有舊資料才是 NULL。
	 * 補完後出版社一定不是 NULL，所以每本書只會補一次。
	 * 兩個來源都無法使用時不存檔，出版社維持 NULL，下次查詢再試。
	 */
	private void fillDetailsOfLocalBook(Book book) {
		boolean anySourceAnswered = false;

		// 1. 問 Google Books（順序與查新書相同：以 Google 為準）
		try {
			Optional<Book> googleBook = googleBooksClient.findByIsbn(book.getIsbn());
			anySourceAnswered = true;
			if (googleBook.isPresent()) {
				book.setPublisher(googleBook.get().getPublisher());
				book.setPageCount(googleBook.get().getPageCount());
				book.setCoverUrl(googleBook.get().getCoverUrl());
			}
		} catch (BookSourceUnavailableException e) {
			// 繼續問 Open Library
		}

		// 2. 出版社或頁數還缺 → 問 Open Library 補
		boolean missingPublisher = book.getPublisher() == null || book.getPublisher().isEmpty();
		if (missingPublisher || book.getPageCount() == null) {
			try {
				Optional<Book> openLibraryBook = openLibraryClient.findByIsbn(book.getIsbn());
				anySourceAnswered = true;
				if (openLibraryBook.isPresent()) {
					if (missingPublisher) {
						book.setPublisher(openLibraryBook.get().getPublisher());
					}
					if (book.getPageCount() == null) {
						book.setPageCount(openLibraryBook.get().getPageCount());
					}
				}
			} catch (BookSourceUnavailableException e) {
				// 沒關係，有拿到多少算多少
			}
		}

		if (!anySourceAnswered) {
			return; // 兩邊都連不上：不存檔，下次再補
		}
		if (book.getPublisher() == null) {
			book.setPublisher(""); // 兩邊都查無：標記成「補過了」，以後不再重複查
		}
		bookRepository.save(book);
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
