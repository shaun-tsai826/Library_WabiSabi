package com.wabisabi.library.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.wabisabi.library.common.Categories;
import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.FavoriteResponse;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.Favorite;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.repository.BookRepository;
import com.wabisabi.library.repository.FavoriteRepository;
import com.wabisabi.library.repository.UserRepository;

/**
 * 收藏的規則。每個方法的第一個參數 userId 都來自 Session（Controller 傳進來），不是前端給的。
 */
@Service
public class FavoriteService {

	private final FavoriteRepository favoriteRepository;
	private final BookRepository bookRepository;
	private final UserRepository userRepository;

	public FavoriteService(FavoriteRepository favoriteRepository, BookRepository bookRepository,
			UserRepository userRepository) {
		this.favoriteRepository = favoriteRepository;
		this.bookRepository = bookRepository;
		this.userRepository = userRepository;
	}

	/**
	 * 加入收藏。順序：ISBN 格式 → 書存在嗎 → 已經收藏過嗎 → 新增。
	 */
	public void add(Long userId, String rawIsbn) {
		String isbn = BookService.normalizeIsbn(rawIsbn);

		// 書一定要先在本機 books 表（使用者是先查詢、再按加入收藏，查詢時就已經存進來了）
		Book book = bookRepository.findById(isbn)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND));

		// Session 裡的會員已經不在資料庫：當作未登入
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Messages.LOGIN_REQUIRED));

		if (favoriteRepository.existsByUserIdAndBookIsbn(userId, isbn)) {
			throw new ApiException(HttpStatus.CONFLICT, Messages.FAVORITE_DUPLICATE);
		}

		try {
			// saveAndFlush：立刻寫進資料庫，若違反唯一鍵會馬上在這裡丟出例外
			favoriteRepository.saveAndFlush(new Favorite(user, book));
		} catch (DataIntegrityViolationException e) {
			// 極少見：同一時間按了兩次，兩個請求都通過上面的檢查，資料庫的唯一鍵擋下第二筆
			throw new ApiException(HttpStatus.CONFLICT, Messages.FAVORITE_DUPLICATE);
		}
	}

	/** 取得這位會員自己的收藏清單 */
	public List<FavoriteResponse> list(Long userId) {
		List<FavoriteResponse> result = new ArrayList<>();
		for (Favorite favorite : favoriteRepository.findByUserIdOrderByIdAsc(userId)) {
			result.add(new FavoriteResponse(favorite));
		}
		return result;
	}

	/**
	 * 刪除收藏：只能刪自己的。
	 * 收藏不存在、或不是自己的，一律回 404（不透露「這筆收藏存在但屬於別人」）。
	 */
	public void remove(Long userId, Long favoriteId) {
		Favorite favorite = favoriteRepository.findByIdAndUserId(favoriteId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Messages.FAVORITE_NOT_FOUND));
		favoriteRepository.delete(favorite);
	}

	/**
	 * 修改收藏的種類（BDD v1.4.0 需求 9）。順序：種類是否在清單中 → 收藏是不是自己的 → 存檔。
	 * 收藏不存在、或不是自己的，一律回 404（和刪除的規則相同）。
	 */
	public void updateCategory(Long userId, Long favoriteId, String rawCategory) {
		String category = (rawCategory == null) ? "" : rawCategory.trim();
		if (!Categories.isValid(category)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.CATEGORY_INVALID);
		}

		Favorite favorite = favoriteRepository.findByIdAndUserId(favoriteId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Messages.FAVORITE_NOT_FOUND));

		favorite.setCategory(category);
		favoriteRepository.save(favorite);
	}
}
