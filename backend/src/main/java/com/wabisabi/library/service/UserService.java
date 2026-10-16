package com.wabisabi.library.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.LoginRequest;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.repository.UserRepository;

/**
 * 會員的規則：登入/註冊合一（沒有密碼，只用暱稱＋手機）。
 */
@Service
public class UserService {

	/** 暱稱只能是英文字母、數字、- 與 _ */
	private static final String NICKNAME_PATTERN = "^[A-Za-z0-9_-]+$";
	private static final int NICKNAME_MAX_LENGTH = 20;
	/** 手機：剛好 10 碼數字 */
	private static final String MOBILE_PATTERN = "^[0-9]{10}$";

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * 登入/註冊合一。判斷順序：
	 * a. 欄位驗證
	 * b. 用手機找會員：找到且暱稱（不分大小寫）相同 → 登入；暱稱不同 → 「暱稱與手機號碼不符」
	 * c. 手機找不到：暱稱已被別人使用 → 「此暱稱已被使用」；否則建立新會員
	 *
	 * @return 登入（或剛註冊）的會員
	 */
	public User login(LoginRequest request) {
		// 先去除前後空白（null 當作空字串）
		String nickname = trim(request.getNickname());
		String mobile = trim(request.getMobile());

		// a. 欄位驗證
		validate(nickname, mobile);

		String nicknameLower = nickname.toLowerCase(Locale.ROOT);

		// b. 用手機找會員
		Optional<User> found = userRepository.findByMobile(mobile);
		if (found.isPresent()) {
			User user = found.get();
			if (user.getNicknameLower().equals(nicknameLower)) {
				return user; // 暱稱相同（不分大小寫）→ 登入
			}
			throw new ApiException(HttpStatus.CONFLICT, Messages.NICKNAME_MOBILE_MISMATCH);
		}

		// c. 手機是新的：檢查暱稱有沒有被別人用走
		if (userRepository.existsByNicknameLower(nicknameLower)) {
			throw new ApiException(HttpStatus.CONFLICT, Messages.NICKNAME_TAKEN);
		}

		// 全新的暱稱＋手機 → 自動註冊
		return userRepository.save(new User(nickname, mobile));
	}

	/** 依 id 取得會員（GET /api/auth/me 用）；找不到回傳空的 Optional */
	public Optional<User> findById(Long id) {
		return userRepository.findById(id);
	}

	/**
	 * 欄位驗證，順序：暱稱空白 → 手機空白 → 暱稱格式 → 暱稱長度 → 手機格式。
	 * 一發現錯誤就丟出例外，所以使用者一次只會看到一則訊息。
	 */
	private void validate(String nickname, String mobile) {
		if (nickname.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.NICKNAME_REQUIRED);
		}
		if (mobile.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.MOBILE_REQUIRED);
		}
		if (!nickname.matches(NICKNAME_PATTERN)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.NICKNAME_INVALID);
		}
		if (nickname.length() > NICKNAME_MAX_LENGTH) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.NICKNAME_TOO_LONG);
		}
		if (!mobile.matches(MOBILE_PATTERN)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.MOBILE_INVALID);
		}
	}

	private String trim(String value) {
		return (value == null) ? "" : value.trim();
	}
}
