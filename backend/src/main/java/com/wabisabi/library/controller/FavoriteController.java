package com.wabisabi.library.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.dto.AddFavoriteRequest;
import com.wabisabi.library.dto.FavoriteResponse;
import com.wabisabi.library.dto.MessageResponse;
import com.wabisabi.library.dto.UpdateCategoryRequest;
import com.wabisabi.library.service.FavoriteService;

import jakarta.servlet.http.HttpSession;

/**
 * 收藏 API：全部需要登入（LoginInterceptor 已經在門口檢查過）。
 * 「是誰」一律從 Session 取得，不相信前端傳來的任何 user_id。
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

	private final FavoriteService favoriteService;

	public FavoriteController(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	/** GET /api/favorites：自己的收藏清單 */
	@GetMapping
	public List<FavoriteResponse> list(HttpSession session) {
		return favoriteService.list(loginUserId(session));
	}

	/**
	 * POST /api/favorites/add，內容：{"isbn": "9789861343722"}
	 * 成功 201；書不存在 404；重複收藏 409。
	 */
	@PostMapping("/add")
	public ResponseEntity<MessageResponse> add(@RequestBody AddFavoriteRequest request, HttpSession session) {
		favoriteService.add(loginUserId(session), request.getIsbn());
		return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(Messages.FAVORITE_ADDED));
	}

	/** DELETE /api/favorites/5：刪除自己的一筆收藏；不存在或不是自己的回 404 */
	@DeleteMapping("/{id}")
	public MessageResponse remove(@PathVariable Long id, HttpSession session) {
		favoriteService.remove(loginUserId(session), id);
		return new MessageResponse(Messages.FAVORITE_REMOVED);
	}

	/**
	 * PUT /api/favorites/5/category，內容：{"category": "散文"}
	 * 成功 200；種類不在清單中 400；收藏不存在或不是自己的 404。
	 * 網址符合 /api/favorites/**，所以 LoginInterceptor 會自動保護，未登入回 401。
	 */
	@PutMapping("/{id}/category")
	public MessageResponse updateCategory(@PathVariable Long id, @RequestBody UpdateCategoryRequest request,
			HttpSession session) {
		favoriteService.updateCategory(loginUserId(session), id, request.getCategory());
		return new MessageResponse(Messages.CATEGORY_UPDATED);
	}

	/** 從 Session 取出登入者 id */
	private Long loginUserId(HttpSession session) {
		return (Long) session.getAttribute(SessionKeys.USER_ID);
	}
}
