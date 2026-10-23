---
name: library-project-prompt
version: v1.6.0
date: 2026-10-23
requires: BDD.md v1.5.0, DESIGN.md v1.4.0
---

# LIBRARY / 侘寂書閣：Claude Code 專案提示詞

使用方式：把下方程式碼區塊內的文字整段貼進 Claude Code。
貼之前請先確認專案根目錄已有 `documents/BDD.md`、`documents/DESIGN.md`、`design/`（7 張線稿），以及空的 `frontend/`、`backend/`，且文件版本與上方 `requires` 一致。
版本紀錄在本文件最下方，不需要貼進去。

```text
【提示詞版本】v1.6.0（2026-10-23）
【對應文件版本】BDD.md v1.5.0、DESIGN.md v1.4.0。若專案內文件的版本與此不同，請先告訴我再開始。

請幫我從零建立「圖書收藏與會員系統」，網站名稱 LIBRARY（侘寂書閣）。

【總原則】
我是 Java 初學者，所有框架與技術都以「新手友善、概念少、好除錯」為優先。
遇到多種做法時，選最簡單、最常見的那種，並在 STEPS.md 說明原因；
不要引入額外的套件或抽象層。

【驗收標準與文件】
- documents/BDD.md：行為規格（流程、規則、提示訊息文字），所有功能以其中的 Scenario 為驗收標準。
- documents/DESIGN.md 與 design/ 資料夾（7 張線稿）：前端外觀與版面，視覺以它們為準；
  文字與圖片有出入時以圖片為準。
- 文件之間，或與下方設計決策有衝突時，請在計畫階段先列出來問我，不要自行決定。
  DESIGN.md 第 7 節的事項已全部確認，請照其內容實作。

【專案結構】
專案根目錄（Library_WabiSabi/）已建好以下資料夾，請沿用，不要改名或搬移：
  documents/   文件：BDD.md、DESIGN.md、PROMPT.md，以及你要維護的 STEPS.md
  design/      設計線稿（7 張 PNG），只讀取、不修改
  frontend/    前端（Vue）。目前是空資料夾，請直接在裡面建立專案
  backend/     後端（Spring Boot）。目前是空資料夾，請直接在裡面建立專案
  .gitignore   放在根目錄
這個資料夾已經是 git 儲存庫（已執行 git init，預設分支 main，尚未有任何 commit）。
git 操作規則：
  - commit：只有我要求時才執行。commit 前先用 git status 確認 application-local.properties 沒有被加入，
    並把要提交的檔案清單與提交訊息寫在回覆內文給我確認。
  - 分支：新功能從 main 開 feature/ 分支，一個階段一個分支。
  - push：設定好遠端（GitHub）之後，push 功能分支的時機由你判斷；不要使用 force push（--force）。
  - 合併：不開 Pull Request。每個階段的文件 commit 之後，由你在本機用 git merge --no-ff 把分支合併進 main
    （保留合併 commit）、跑過測試再 push main，最後刪除本機與 GitHub 上的分支。
請建立 .gitignore，內容至少涵蓋：
  backend/target/
  backend/.classpath
  backend/.project
  backend/.settings/
  backend/src/main/resources/application-local.properties
  frontend/node_modules/
  frontend/dist/
  frontend/.project
  frontend/.settings/
  .DS_Store

【後端技術】
- JDK 21、Spring Boot 4.1.x（建立時使用 4.1 的最新修正版，計畫中請說明版本號）、Maven（含 Maven Wrapper）
  網路教學多為 Spring Boot 3.x；4.x 寫法不同的地方（例如 Jackson 3 的 package 名稱、@MockitoBean、starter 名稱）
  請在 STEPS.md 特別標註。
- Spring Web（REST API）、Spring Data JPA；MVC 分層（Controller / Service / Repository / Entity）
- 不使用 Spring Security、不使用 Lombok（我用 Eclipse 開發）。
  Entity 與 DTO 用一般 Java 類別手寫 getter/setter。
  專案必須能在 Eclipse 以 Existing Maven Projects 匯入。
- 欄位驗證：不用 Bean Validation，在 Service 內手寫驗證，並依 BDD.md 的順序檢查。
- 錯誤處理：統一回傳 JSON {"message": "..."}，用一個 @RestControllerAdvice 的
  GlobalExceptionHandler 處理，並在 STEPS.md 解釋它的作用。
- 呼叫外部書籍來源（Google Books、Open Library）：用 Spring 內建的 RestClient，JSON 解析用內建的 Jackson。
  逾時用 application.properties 的 spring.http.clients.* 設定（連線 3 秒、讀取 10 秒）。
- 建表：spring.jpa.hibernate.ddl-auto=update（僅限練習，STEPS.md 請註明）。
  種子資料用一個 CommandLineRunner，資料不存在才寫入。
- 測試：JUnit 5 + MockMvc，Google Books 與 Open Library 以 Mockito 模擬（Client 的 HTTP 行為用 MockRestServiceServer），不使用 Cucumber。
  測試時使用 H2 記憶體資料庫（只以 test scope 加入依賴，測試設定放 src/test/resources），
  測試不連 MySQL，也不需要 application-local.properties。
  依 BDD.md 的 Scenario 為後端 API 寫測試（測試名稱標註對應的 Scenario）。
  前端行為請在 STEPS.md 提供一份對照 BDD.md 的手動測試清單。

【資料庫】
MySQL 8.0，字元集 utf8mb4。資料庫（library）與專用帳號我會自己先建好，不會使用 admin/root 帳號。
資料庫連線設定（網址、帳號、密碼）與 Google Books API 金鑰（google.books.api-key）放在
backend/src/main/resources/application-local.properties（不進 git），由我自己建立並填寫。
請不要讀取、輸出或修改這個檔案的內容，也不要把密碼或金鑰寫死在程式碼或其他會進 git 的檔案裡；錯誤紀錄中若出現金鑰，要先遮成 ***。
請提供 application-local.properties.example 當範本（欄位留空，可進 git），
並在 application.properties 設定 spring.profiles.active=local。
需要連線資料庫測試時，直接執行應用程式即可，它會自己讀取該檔案。

【前端技術】
- Vue 3 + Vite + Vue Router，使用 JavaScript（不用 TypeScript）與 <script setup> 寫法
- 用內建 fetch 呼叫 API（不裝 axios）；不使用 Pinia 或其他狀態管理套件，登入狀態用簡單的共用變數
- 純 CSS，不使用 UI 套件
- 用 create-vue 建立時：不選 TypeScript、JSX、Pinia、Vitest、E2E；選 Vue Router
- Vite 開發時用 proxy 把 /api 轉到後端，避免處理 CORS
- 顏色與尺寸用 CSS 變數，依 DESIGN.md 第 2 節定義，不要把色碼散寫在各元件裡。
- Toast 提示自己寫一個小元件；刪除確認直接用 window.confirm()，都不安裝額外套件。
- 版面採 RWD（響應式設計），依 DESIGN.md 第 3.1 節的斷點與規則，用純 CSS 的 @media 實作；
  每完成一個頁面，至少在 375px、768px、1280px 三種寬度檢查版面。

【已確認的設計決策】
1. 書籍來源：本機 books 表 → Google Books → Open Library（備援），任一來源查到就存回 books 表，之後的來源不再查詢。
   所有外部來源都明確查無才回「找不到此書籍」；任一來源無法使用且都沒查到時回「查詢失敗，請稍後再試」。
   Google Books 無法連線時仍要改查 Open Library。
   外部來源的出版日期可能只有年或年月，或是英文格式（例如 July 2008），publish_date 請用字串欄位、照原樣存。
2. ISBN 只驗格式：必須是 13 碼數字（先去除前後空白），不驗檢查碼。
3. 暱稱規則：只接受英文字母、數字、- 與 _（正規式 ^[A-Za-z0-9_-]+$），長度 1～20 字元，
   大小寫視為相同。資料庫存兩欄：nickname 保留使用者輸入的原樣（用於顯示），
   nickname_lower 為小寫版本且唯一（用於比對）。前後端都要驗證。
4. 手機號碼：10 碼數字，唯一；欄位用字串型態（VARCHAR(10)），避免開頭的 0 消失。
5. 登入/註冊合一，沒有密碼（簡化設計，請在 STEPS.md 註明這個限制）。判斷順序：
   a. 欄位驗證（空白、暱稱格式與長度、手機格式）。
   b. 用手機找會員：找到且暱稱（不分大小寫）相同 → 登入；找到但暱稱不同 → 「暱稱與手機號碼不符」。
   c. 手機找不到：暱稱已被別人使用 → 「此暱稱已被使用」；否則建立新會員並登入。
6. 登入狀態：使用 HttpSession ＋ HandlerInterceptor，不使用 Spring Security。
   - 登入成功後，把會員 id 存進 Session，並先呼叫 request.changeSessionId() 換一個新的 Session ID。
   - 寫一個 LoginInterceptor，保護 /api/favorites/**、/api/auth/me、/api/auth/logout；
     Session 內沒有會員 id 時，回傳 401 JSON，不導向任何登入頁。
   - 在 WebMvcConfigurer 註冊這個攔截器，並說明為什麼要這樣設定路徑。
   - Controller 一律從 Session 取得登入者 id，不可相信前端傳來的 user_id。
   - 登出時呼叫 session.invalidate()。
   - 請在 STEPS.md 用白話說明 Session 與攔截器的運作，並註明：此做法沒有 CSRF 防護，
     僅適合練習與作業，正式上線需補強。
7. 所有提示訊息（BDD.md 內的固定文字）集中放在同一處管理，不要散落在各檔案。
8. 未登入時的頁面保護（Vue Router 路由守衛）：
   - 進 /favorites：提示「請先登入後再檢視收藏內容」，並導向 /login。
   - 進 /profile：提示「請先註冊/登入」，並導向 /login。
9. 顏色暫時使用 DESIGN.md 第 2 節的現有色碼，不需再微調。
10. ISBN 空白時，「請輸入 ISBN 碼」由前端直接提示、不呼叫 API（網址路徑放不了空值）；格式錯誤則前後端都要檢查。
11. 全新暱稱與手機自動註冊後，提示文字與一般登入相同：「登入成功」。
12. 外部書籍資料對應（Google Books、Open Library 相同）：書名只取 title（不含副標題）；多位作者用「、」連接；出版日期照原樣存。
    Google Books 可設定 API 金鑰（放在 application-local.properties）；沒設定時不帶金鑰呼叫（Google 的匿名上限為 0，會回 429）。
    Open Library 不需要金鑰，請在 User-Agent 帶上程式名稱（不含個人 email）。
13. 前端打開網頁時先呼叫 GET /api/auth/me 取得登入狀態，路由守衛等這個結果出來才判斷是否放行，
    避免重新整理頁面時被誤判為未登入。
14. 書籍封面：查詢結果與收藏清單顯示封面，由前端用 ISBN 組出 Open Library 封面網址
    （https://covers.openlibrary.org/b/isbn/{ISBN}-M.jpg?default=false），後端不用儲存。
    沒有封面或載入失敗時顯示同尺寸的空白框，不顯示錯誤；規格見 DESIGN.md 4.10。
15. 收藏書籍種類：種類記在「收藏」上（favorites.category，可為空值，空值視為「未分類」），
    只能從固定清單選擇：未分類、小說、散文、詩、人文歷史、心理勵志、商業理財、科學、電腦資訊、藝術設計、其他。
    清單只在後端定義一份，前端透過 GET /api/categories 取得；在收藏卡片上用下拉選單修改，選完立即存檔。
    收藏頁用種類按鈕篩選，只顯示「全部」＋有書的種類（規格見 DESIGN.md 4.11、4.12）。
16. 搜尋收藏：純前端篩選已載入的收藏，範圍為書名、作者、ISBN；不分大小寫、去除前後空白、邊輸入邊篩選；
    與種類篩選同時成立。「共 N 本」顯示總數，篩選中另外顯示「顯示 X 本」；沒有符合的顯示「沒有符合條件的收藏」。
17. 收藏統計 Dashboard：收藏頁可切換「清單／統計」（狀態記在網址 /favorites?view=stats）。統計在前端以全部收藏計算，
    不受搜尋與種類篩選影響：3 張總覽數字卡（收藏總數、本月新增、最多的種類）、種類比例甜甜圈圖（CSS conic-gradient）、
    最近 12 個月每月新增本數折線圖（手寫 SVG）。不使用圖表套件；色票與版面見 DESIGN.md 第 2 節、4.13～4.16、5.3-c。

【資料表】
- users：id、nickname、nickname_lower（唯一）、mobile（唯一）、created_at
- books：isbn（主鍵，13 碼）、title、author、publish_date（字串）
- favorites：id、user_id（外鍵）、isbn（外鍵）、category（VARCHAR(20)，可為空值）、created_at，(user_id, isbn) 設唯一鍵
- 種子資料：BDD.md 的 2 本書，以及會員 Cooqle / 0900123456

【API】（REST 一律加 /api 前綴，避免與前端頁面網址衝突）
- GET /api/books/{isbn}：公開。200 書籍資料；400 格式錯誤；404 所有外部來源都查無；503 有外部來源無法使用且都沒查到
- POST /api/auth/login：公開。登入/註冊合一。200 成功；400 欄位驗證錯誤；409 暱稱與手機號碼不符、此暱稱已被使用
- POST /api/auth/logout：需登入，清除 Session
- GET /api/auth/me：需登入，回傳暱稱與手機（個人頁與判斷登入狀態用）
- POST /api/favorites/add：需登入，user_id 取自登入者；201 成功；409 重複收藏；404 無此書
- GET /api/favorites：需登入，只回傳自己的收藏（含書籍資料）
- DELETE /api/favorites/{id}：需登入，須驗證屬於本人；不存在或不是本人的收藏一律回 404
- GET /api/categories：公開。回傳種類清單（依固定順序）
- PUT /api/favorites/{id}/category：需登入，內容 {"category": "小說"}；200 已更新書籍種類；400 書籍種類不正確；
  404 不存在或不是本人的收藏
- GET /api/favorites 的每筆資料加上 category（空值時回「未分類」）與 createdAt（收藏時間，ISO 格式）

【前端頁面】
共 4 個路由、7 張線稿，詳見 DESIGN.md 第 5 節：
- 首頁 /：初始、查詢成功、查詢錯誤三種狀態
- 登入/註冊 /login
- 收藏 /favorites：已登入清單；未登入提示畫面僅作為 Session 失效時的備用
- 個人頁 /profile
/favorites 與 /profile 需登入，使用 Vue Router 的路由守衛；
未登入時依 BDD.md 的訊息提示並導向 /login。
所有共用元件（導覽列、卡片、輸入框、按鈕、錯誤框、Toast、書籍封面）請做成可重用的 Vue 元件。

【做事方式】
1. 先給我完整計畫：技術與版本、資料表設計、每個 Scenario 由哪個類別或 API 實作，等我確認再開始寫。
2. 一步一步做，每完成一步就編譯、啟動並實際測試，並說明哪些 Scenario 已通過；不要一次寫完全部。
3. 同時維護 documents/STEPS.md，每一步寫：目標、建立或修改的檔案、完整程式碼、
   每個重要區塊的白話解釋、對應的 Scenario、怎麼驗證成功。
4. 說明以 Java 初學者為對象：多用生活化比喻和小範例，避免艱深術語，遇到名詞先解釋
   （尤其是 Session、攔截器、JPA、全域例外處理這幾個概念）。
5. STEPS.md 結尾寫「如何快速上手」，教我之後怎麼照著步驟自己重做一遍，不要寫成考試複習清單。
6. 之後新增功能時：先給計畫與待決定的問題 → 確認後先改 BDD.md／DESIGN.md（並更新 PROMPT.md 的版本紀錄）
   → 開 feature/ 分支一步一步實作 → STEPS.md 新增一節、MANUAL_TEST.md 新增測試項目 → 合併進 main 並 push。
```

## 版本紀錄

版本號規則（MAJOR.MINOR.PATCH）：
- MAJOR：技術棧或整體架構改變（例如換前端框架、換資料庫）
- MINOR：新增或調整功能規則、設計決策
- PATCH：只修正文字、錯字、說明

### v1.6.0（2026-10-23）收藏統計 Dashboard
- 新增：設計決策 17 收藏統計 Dashboard（清單／統計切換、總覽數字、甜甜圈圖、每月折線圖；前端計算、不使用圖表套件）。
- 新增：GET /api/favorites 回傳 createdAt。
- 對應文件：BDD.md v1.5.0、DESIGN.md v1.4.0。

### v1.5.0（2026-10-21）收藏書籍種類與搜尋
- 新增：設計決策 15 收藏書籍種類（11 種、預設未分類、清單由後端提供、卡片下拉選單修改、種類按鈕篩選）。
- 新增：設計決策 16 搜尋收藏（純前端；書名、作者、ISBN；不分大小寫；與種類同時成立）。
- 新增：favorites.category 欄位；API GET /api/categories、PUT /api/favorites/{id}/category；GET /api/favorites 回傳 category。
- 修改：git 規則改為不開 Pull Request；每個階段文件 commit 後，由 Claude 在本機 git merge --no-ff 合併進 main、push main、刪除分支（2026-10-23）。
- 對應文件：BDD.md v1.4.0、DESIGN.md v1.3.0。

### v1.4.0（2026-10-21）同步開發後的實際狀態（Google 金鑰、Open Library、封面、git 流程）
- 修改：設計決策 1 書籍來源改為「本機 → Google Books → Open Library」，並定義 404／503 的判斷規則（BDD v1.2.0）。
- 修改：設計決策 12 改為 Google Books 可設定 API 金鑰（application-local.properties，不進 git，紀錄檔遮罩）；
  原因是 Google 給不帶金鑰的呼叫的每日上限為 0（2026-10-19 確認）。Open Library 不需金鑰。
- 新增：設計決策 14 書籍封面（Open Library 封面服務，前端處理，沒有封面顯示空白框；BDD v1.3.0、DESIGN v1.2.0）。
- 新增：外部來源逾時設定（連線 3 秒、讀取 10 秒）；Client 的 HTTP 行為以 MockRestServiceServer 測試。
- 修改：git 規則改為「commit 經我確認；新功能用 feature/ 分支；階段完成由 Claude 直接 push 功能分支（不 push main、不 force）；
  PR 經我同意可用 gh 建立，合併由我操作」。.gitignore 補上 frontend/.project、frontend/.settings/。
- 新增：【做事方式】第 6 點，新功能的固定流程；PROMPT.md 的版本紀錄改由 Claude 一併維護。
- 對應文件：BDD.md v1.3.0、DESIGN.md v1.2.0。

### v1.3.0（2026-10-15）確認開發計畫細節
- 修改：git 已執行 git init（分支 main），不再需要詢問。
- 新增：Spring Boot 版本定為 4.1.x；測試使用 H2 記憶體資料庫（test scope）。
- 新增：各 API 的 HTTP 狀態碼；刪除別人的收藏一律回 404。
- 新增：設計決策 10～13（ISBN 空白由前端提示、註冊後提示「登入成功」、Google Books 欄位對應、啟動時先取得登入狀態）。
- 對應文件不變：BDD.md v1.1.0、DESIGN.md v1.1.0。

### v1.2.0（2026-10-15）確認目錄結構與待確認事項
- 修改：【專案結構】改為 documents/（文件）、design/（線稿）、frontend/、backend/ 四個資料夾；STEPS.md 放在 documents/。
- 修改：git 儲存庫尚未初始化時，先問我是否執行 git init。
- 新增：前端採 RWD 響應式設計（DESIGN.md 3.1 節）。
- 新增：未登入進 /profile 的提示文字「請先註冊/登入」；未登入進 /favorites 維持提示後導向登入頁。
- 新增：顏色暫時使用現有色碼。
- 對應文件：BDD.md v1.1.0、DESIGN.md v1.1.0。

### v1.1.1（2026-10-11）釐清 git 操作規則
- 修改：git 預設不自行 commit 或 push，只有我明確要求時才執行（取代 v1.1.0 的「不替我 commit 或 push」）。
- 新增：commit 前先用 git status 確認 application-local.properties 沒被加入，並列出檔案讓我確認；
  push 不使用 force push。
- 對應文件不變：BDD.md v1.0.0、DESIGN.md v1.0.0。

### v1.1.0（2026-10-11）資料庫密碼不進 git
- 新增：資料庫連線設定改放 application-local.properties（不進 git），並提供 .example 範本；
  Claude Code 不得讀取、輸出或修改該檔案內容。
- 新增：資料庫改用專用帳號，不使用 admin/root。
- 新增：【專案結構】要求建立 .gitignore，並註明由我自己管理 git（不替我 commit 或 push）。
- 對應文件不變：BDD.md v1.0.0、DESIGN.md v1.0.0。

### v1.0.0（2026-10-11）首次納入版本管理
- 技術棧：Spring Boot（Web、JPA）＋ MySQL 8.0 ＋ Vue 3（Vite、Vue Router、JavaScript）；全部以新手友善為原則。
- 登入狀態：HttpSession ＋ HandlerInterceptor，不使用 Spring Security；登入/註冊合一、無密碼。
- 暱稱規則：僅英文、數字、- 與 _，大小寫視為相同；種子會員為 Cooqle / 0900123456。
- 書籍來源：先查本機 books 表，查不到再查 Google Books 並存回；ISBN 只驗 13 碼數字格式。
- 納入 DESIGN.md 與 7 張線稿（design/）；前端以 CSS 變數、自製 Toast、window.confirm() 實作。
- 對應文件：BDD.md v1.0.0、DESIGN.md v1.0.0。
