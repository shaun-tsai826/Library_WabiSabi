package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.wabisabi.library.entity.Favorite;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.repository.FavoriteRepository;
import com.wabisabi.library.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * 需求 2、3、4、8：加入收藏、收藏清單、刪除收藏、未登入的權限控管。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteApiTest {

	private static final String SEED_MOBILE = "0900123456";
	private static final String WABI_SABI_ISBN = "9789861343722";
	private static final String QUESTION_ISBN = "9789863980315";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FavoriteRepository favoriteRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@BeforeEach
	void setUp() {
		// 每個測試都從「只有種子會員 Cooqle、沒有任何收藏」的狀態開始
		favoriteRepository.deleteAll();
		for (User user : userRepository.findAll()) {
			if (!user.getMobile().equals(SEED_MOBILE)) {
				userRepository.delete(user);
			}
		}
	}

	// ===== 小工具 =====

	/** 登入（或註冊）並取得 Session */
	private MockHttpSession login(String nickname, String mobile) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("nickname", nickname, "mobile", mobile));
		return (MockHttpSession) mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
	}

	/** 加入收藏 */
	private ResultActions addFavorite(MockHttpSession session, String isbn) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("isbn", isbn));
		return mockMvc.perform(post("/api/favorites/add")
				.session(session).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	/** 修改收藏種類（BDD v1.4.0 需求 9） */
	private ResultActions updateCategory(MockHttpSession session, Long favoriteId, String category) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("category", category));
		return mockMvc.perform(put("/api/favorites/{id}/category", favoriteId)
				.session(session).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	/** 找出某位會員收藏某本書的那一筆 */
	private Favorite favoriteOf(String mobile, String isbn) {
		Long userId = userRepository.findByMobile(mobile).orElseThrow().getId();
		return favoriteRepository.findByUserIdOrderByIdAsc(userId).stream()
				.filter(f -> f.getBook().getIsbn().equals(isbn))
				.findFirst().orElseThrow();
	}

	private long favoriteCountOf(String mobile) {
		Long userId = userRepository.findByMobile(mobile).orElseThrow().getId();
		return favoriteRepository.findByUserIdOrderByIdAsc(userId).size();
	}

	// ===== 需求 2：加入收藏 =====

	@Test
	@DisplayName("Scenario: 已登入會員成功將書籍加入個人收藏")
	void addFavoriteSuccess() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		addFavorite(cooqle, WABI_SABI_ISBN)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("已成功加入收藏"));

		Long cooqleId = userRepository.findByMobile(SEED_MOBILE).orElseThrow().getId();
		assertThat(favoriteRepository.existsByUserIdAndBookIsbn(cooqleId, WABI_SABI_ISBN)).isTrue();
	}

	@Test
	@DisplayName("Scenario: 已登入會員重複收藏書籍時跳出提示")
	void addFavoriteDuplicate() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		addFavorite(cooqle, WABI_SABI_ISBN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("此書籍已在收藏清單中，請勿重複收藏"));

		assertThat(favoriteCountOf(SEED_MOBILE)).isEqualTo(1); // 數量沒有增加
	}

	@Test
	@DisplayName("規則：加入本機沒有的書 → 404 找不到此書籍")
	void addFavoriteBookNotFound() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		addFavorite(cooqle, "9780000000000")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此書籍"));
	}

	@Test
	@DisplayName("規則：加入收藏時 ISBN 格式錯誤 → 400")
	void addFavoriteInvalidIsbn() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		addFavorite(cooqle, "INVALID_ISBN")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ISBN 碼格式錯誤或無效"));
	}

	@Test
	@DisplayName("規則：不相信前端傳來的 user_id，收藏一律記在登入者名下")
	void ignoreUserIdFromClient() throws Exception {
		login("Reader_B", "0911222333"); // 先註冊 Reader_B
		Long readerBId = userRepository.findByMobile("0911222333").orElseThrow().getId();
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		// Cooqle 登入中，卻在內容裡偷塞 Reader_B 的 id
		String json = jsonMapper.writeValueAsString(Map.of("isbn", WABI_SABI_ISBN, "userId", readerBId));
		mockMvc.perform(post("/api/favorites/add").session(cooqle).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());

		assertThat(favoriteCountOf(SEED_MOBILE)).isEqualTo(1);
		assertThat(favoriteCountOf("0911222333")).isZero();
	}

	// ===== 需求 3：收藏清單 =====

	@Test
	@DisplayName("Scenario: 已登入會員在收藏頁面檢視自己的收藏清單")
	void listOwnFavorites() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		addFavorite(cooqle, QUESTION_ISBN).andExpect(status().isCreated());

		// 別的會員的收藏不應該出現在 Cooqle 的清單
		MockHttpSession readerB = login("Reader_B", "0911222333");
		addFavorite(readerB, QUESTION_ISBN).andExpect(status().isCreated());

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].isbn").value(WABI_SABI_ISBN))
				.andExpect(jsonPath("$[0].title").value("侘寂：尋找日常中的不完美之美"))
				.andExpect(jsonPath("$[0].author").value("雷納·克魯克"))
				.andExpect(jsonPath("$[0].id").isNumber()) // 「刪除」按鈕要用的收藏編號
				.andExpect(jsonPath("$[1].isbn").value(QUESTION_ISBN))
				.andExpect(jsonPath("$[1].title").value("提問的設計"));
	}

	@Test
	@DisplayName("規則：還沒有收藏時回傳空陣列")
	void listEmpty() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// ===== 需求 8：未登入的權限控管 =====

	@Test
	@DisplayName("Scenario: 未登入直接呼叫收藏相關 API 被拒絕 (查詢、新增、刪除、修改種類都回 401)")
	void favoritesRequireLogin() throws Exception {
		// 先讓 Cooqle 有一筆收藏，確認未登入的回應裡不會洩漏它
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		Long favoriteId = favoriteRepository.findAll().get(0).getId();

		mockMvc.perform(get("/api/favorites"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"))
				.andExpect(content().string(not(containsString("侘寂"))));

		mockMvc.perform(post("/api/favorites/add").contentType(MediaType.APPLICATION_JSON)
				.content("{\"isbn\":\"" + QUESTION_ISBN + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));

		mockMvc.perform(delete("/api/favorites/{id}", favoriteId))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));

		// v1.4.0：修改種類也要登入
		mockMvc.perform(put("/api/favorites/{id}/category", favoriteId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"散文\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));

		assertThat(favoriteRepository.count()).isEqualTo(1); // 什麼都沒被改到
		assertThat(favoriteRepository.findAll().get(0).getCategory()).isNull(); // 種類也沒被改
	}

	@Test
	@DisplayName("Scenario: 在個人頁面登出後無法再查看收藏內容")
	void cannotSeeFavoritesAfterLogout() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/logout").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已成功登出"));

		// 帶著登出前的 Session 再來查收藏
		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(not(containsString("侘寂"))));
	}

	// ===== 需求 4：刪除收藏 =====

	@Test
	@DisplayName("Scenario: 會員從收藏頁面成功刪除書籍")
	void removeFavoriteSuccess() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		Long favoriteId = favoriteRepository.findAll().get(0).getId();

		mockMvc.perform(delete("/api/favorites/{id}", favoriteId).session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已移除收藏"));

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Scenario: 會員無法刪除其他會員的收藏（回 404）")
	void cannotRemoveOthersFavorite() throws Exception {
		MockHttpSession readerB = login("Reader_B", "0911222333");
		addFavorite(readerB, QUESTION_ISBN).andExpect(status().isCreated());
		Favorite readerBFavorite = favoriteRepository.findAll().get(0);

		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		mockMvc.perform(delete("/api/favorites/{id}", readerBFavorite.getId()).session(cooqle))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此收藏"));

		// Reader_B 的收藏清單維持不變
		assertThat(favoriteRepository.existsById(readerBFavorite.getId())).isTrue();
		assertThat(favoriteCountOf("0911222333")).isEqualTo(1);
	}

	@Test
	@DisplayName("規則：刪除時 id 不是數字 → 400（不是 500）")
	void removeWithNonNumericId() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		mockMvc.perform(delete("/api/favorites/{id}", "abc").session(cooqle))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("無法處理此請求"));
	}

	@Test
	@DisplayName("規則：刪除不存在的收藏 → 404")
	void removeNonExisting() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		mockMvc.perform(delete("/api/favorites/{id}", 999999).session(cooqle))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此收藏"));
	}

	// ===== 需求 9：收藏書籍種類（BDD v1.4.0） =====

	@Test
	@DisplayName("Scenario: 新加入的收藏預設為「未分類」")
	void newFavoriteIsUncategorized() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].category").value("未分類"));
	}

	@Test
	@DisplayName("Scenario: 會員為收藏的書籍設定種類（重新查詢後仍保留）")
	void updateCategorySuccess() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		Long favoriteId = favoriteOf(SEED_MOBILE, WABI_SABI_ISBN).getId();

		updateCategory(cooqle, favoriteId, "散文")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已更新書籍種類"));

		// 「重新整理頁面」＝再查一次清單：種類應該已經存進資料庫
		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(jsonPath("$[0].category").value("散文"));
		assertThat(favoriteOf(SEED_MOBILE, WABI_SABI_ISBN).getCategory()).isEqualTo("散文");
	}

	@Test
	@DisplayName("Scenario: 依種類檢視收藏（清單回傳每本書的種類，供前端篩選與計算本數）")
	void listReturnsEachCategory() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		addFavorite(cooqle, QUESTION_ISBN).andExpect(status().isCreated());
		updateCategory(cooqle, favoriteOf(SEED_MOBILE, WABI_SABI_ISBN).getId(), "散文").andExpect(status().isOk());
		updateCategory(cooqle, favoriteOf(SEED_MOBILE, QUESTION_ISBN).getId(), "心理勵志").andExpect(status().isOk());

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].isbn").value(WABI_SABI_ISBN))
				.andExpect(jsonPath("$[0].category").value("散文"))
				.andExpect(jsonPath("$[1].isbn").value(QUESTION_ISBN))
				.andExpect(jsonPath("$[1].category").value("心理勵志"));
	}

	@Test
	@DisplayName("Scenario: 種類只能從清單中選擇（武俠 → 400 書籍種類不正確）")
	void updateCategoryRejectsUnknown() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		Long favoriteId = favoriteOf(SEED_MOBILE, WABI_SABI_ISBN).getId();

		updateCategory(cooqle, favoriteId, "武俠")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("書籍種類不正確"));

		assertThat(favoriteOf(SEED_MOBILE, WABI_SABI_ISBN).getCategory()).isNull(); // 維持不變
	}

	@Test
	@DisplayName("規則：種類空白 → 400 書籍種類不正確")
	void updateCategoryRejectsBlank() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		updateCategory(cooqle, favoriteOf(SEED_MOBILE, WABI_SABI_ISBN).getId(), "   ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("書籍種類不正確"));
	}

	@Test
	@DisplayName("Scenario: 會員無法修改其他會員收藏的種類（回 404）")
	void cannotUpdateOthersCategory() throws Exception {
		MockHttpSession readerB = login("Reader_B", "0911222333");
		addFavorite(readerB, QUESTION_ISBN).andExpect(status().isCreated());
		Long readerBFavoriteId = favoriteOf("0911222333", QUESTION_ISBN).getId();

		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		updateCategory(cooqle, readerBFavoriteId, "散文")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此收藏"));

		assertThat(favoriteOf("0911222333", QUESTION_ISBN).getCategory()).isNull(); // Reader_B 的維持不變
	}

	@Test
	@DisplayName("規則：修改不存在的收藏種類 → 404")
	void updateCategoryOfNonExisting() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		updateCategory(cooqle, 999999L, "散文")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此收藏"));
	}

	// ===== 需求 11：收藏統計 Dashboard（BDD v1.5.0） =====

	@Test
	@DisplayName("Scenario: 收藏清單回傳收藏日期 (createdAt，ISO 格式，前 10 碼是今天)")
	void listIncludesCreatedAt() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isOk())
				// 例如 2026-10-24T10:15:30.123456：前端取前 7 個字（2026-10）當月份
				.andExpect(jsonPath("$[0].createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")))
				.andExpect(jsonPath("$[0].createdAt").value(startsWith(LocalDate.now().toString())));
	}
}
