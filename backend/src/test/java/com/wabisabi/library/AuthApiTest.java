package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.repository.FavoriteRepository;
import com.wabisabi.library.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * 需求 5～8：登入/註冊、個人頁（/api/auth/me）、登出。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

	private static final String SEED_MOBILE = "0900123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FavoriteRepository favoriteRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@BeforeEach
	void setUp() {
		// 每個測試都從「只有種子會員 Cooqle」的狀態開始
		favoriteRepository.deleteAll();
		for (User user : userRepository.findAll()) {
			if (!user.getMobile().equals(SEED_MOBILE)) {
				userRepository.delete(user);
			}
		}
	}

	// ===== 小工具 =====

	/** 送出登入請求 */
	private ResultActions login(String nickname, String mobile) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("nickname", nickname, "mobile", mobile));
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	/** 登入成功後，取出這次登入的 Session（之後的請求帶著它，就等於「已登入」） */
	private MockHttpSession loginAndGetSession(String nickname, String mobile) throws Exception {
		MvcResult result = login(nickname, mobile).andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	/** 這次請求結束後，Session 裡有沒有登入者 id */
	private boolean isLoggedIn(MvcResult result) {
		var session = result.getRequest().getSession(false);
		return session != null && session.getAttribute(SessionKeys.USER_ID) != null;
	}

	// ===== 登入 / 註冊 =====

	@Test
	@DisplayName("Scenario: 使用暱稱與手機號碼成功登入")
	void loginSuccess() throws Exception {
		MvcResult result = login("Cooqle", "0900123456")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("登入成功"))
				.andExpect(jsonPath("$.nickname").value("Cooqle"))
				.andReturn();

		Long cooqleId = userRepository.findByMobile(SEED_MOBILE).orElseThrow().getId();
		assertThat(result.getRequest().getSession().getAttribute(SessionKeys.USER_ID)).isEqualTo(cooqleId);
	}

	@Test
	@DisplayName("Scenario: 暱稱大小寫視為相同，可登入同一個帳號")
	void loginCaseInsensitive() throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login("cooqle", "0900123456")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("登入成功"))
				.andExpect(jsonPath("$.nickname").value("Cooqle")) // 顯示的是註冊時的原樣
				.andReturn();

		assertThat(isLoggedIn(result)).isTrue();
		assertThat(userRepository.count()).isEqualTo(countBefore); // 沒有建立新會員
	}

	@Test
	@DisplayName("Scenario: 使用全新暱稱與手機號碼自動完成註冊並登入")
	void registerNewUser() throws Exception {
		MvcResult result = login("Wabi_Sabi", "0987654321")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("登入成功"))
				.andExpect(jsonPath("$.nickname").value("Wabi_Sabi"))
				.andReturn();

		User created = userRepository.findByMobile("0987654321").orElseThrow();
		assertThat(created.getNickname()).isEqualTo("Wabi_Sabi");
		assertThat(created.getNicknameLower()).isEqualTo("wabi_sabi");
		assertThat(isLoggedIn(result)).isTrue();
	}

	@ParameterizedTest(name = "Scenario Outline: 登入/註冊時輸入無效資料 | 暱稱「{0}」手機「{1}」→ {2}")
	@CsvSource({
			"'',                          0900123456, 請輸入暱稱",
			"Cooqle,                      '',         請輸入手機號碼",
			"Cooqle,                      12345,      請輸入有效的手機號碼格式 (10碼)",
			"小明,                         0900123456, 暱稱僅限英文、數字、- 與 _",
			"Coo qle,                     0900123456, 暱稱僅限英文、數字、- 與 _",
			"Cooqle!,                     0900123456, 暱稱僅限英文、數字、- 與 _",
			"ThisNicknameIsWayTooLong123, 0900123456, 暱稱長度不可超過 20 個字元"
	})
	void invalidInput(String nickname, String mobile, String expectedMessage) throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login(nickname, mobile)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(expectedMessage))
				.andReturn();

		assertThat(isLoggedIn(result)).isFalse(); // 不應執行登入
		assertThat(userRepository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Scenario: 手機號碼已註冊但暱稱不同")
	void mobileExistsButNicknameDiffers() throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login("Another_One", "0900123456")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("暱稱與手機號碼不符"))
				.andReturn();

		assertThat(isLoggedIn(result)).isFalse();
		assertThat(userRepository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Scenario: 手機號碼是新的但暱稱已被使用 (不分大小寫)")
	void nicknameTaken() throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login("COOQLE", "0955000111")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("此暱稱已被使用"))
				.andReturn();

		assertThat(isLoggedIn(result)).isFalse();
		assertThat(userRepository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("規則：暱稱與手機前後空白會先去除")
	void inputIsTrimmed() throws Exception {
		login("  Cooqle  ", " 0900123456 ")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("Cooqle"));
	}

	@Test
	@DisplayName("規則：暱稱剛好 20 個字元可以註冊，21 個字元不行")
	void nicknameLengthBoundary() throws Exception {
		login("A2345678901234567890", "0911000001").andExpect(status().isOk());
		login("A23456789012345678901", "0911000002")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("暱稱長度不可超過 20 個字元"));
	}

	@Test
	@DisplayName("規則：登入成功後會更換 Session ID（防止 Session 固定攻擊）")
	void sessionIdChangesOnLogin() throws Exception {
		MockHttpSession oldSession = new MockHttpSession();
		String oldId = oldSession.getId();

		String json = jsonMapper.writeValueAsString(Map.of("nickname", "Cooqle", "mobile", "0900123456"));
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.session(oldSession).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getSession().getId()).isNotEqualTo(oldId);
	}

	// ===== 個人頁 =====

	@Test
	@DisplayName("Scenario: 在個人頁面檢視個人資訊（GET /api/auth/me）")
	void meReturnsProfile() throws Exception {
		MockHttpSession session = loginAndGetSession("Cooqle", "0900123456");

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("Cooqle"))
				.andExpect(jsonPath("$.mobile").value("0900123456"));
	}

	@Test
	@DisplayName("Scenario: 未登入使用者嘗試訪問個人頁面被拒絕（API 回 401）")
	void meRequiresLogin() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"))
				.andExpect(jsonPath("$.nickname").doesNotExist());
	}

	// ===== 登出 =====

	@Test
	@DisplayName("Scenario: 在個人頁面登出後無法再查看個人資訊（Session 已清除）")
	void logoutClearsSession() throws Exception {
		MockHttpSession session = loginAndGetSession("Cooqle", "0900123456");

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已成功登出"));

		assertThat(session.isInvalid()).isTrue();

		// 拿舊的 Session 再來，應該被當作未登入
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("規則：未登入時呼叫登出 API 回 401")
	void logoutRequiresLogin() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));
	}
}
