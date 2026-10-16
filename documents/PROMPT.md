---
name: library-project-prompt
version: v1.3.0
date: 2026-10-15
requires: BDD.md v1.1.0, DESIGN.md v1.1.0
---

# LIBRARY / 侘寂書閣：Claude Code 專案提示詞

使用方式：把下方程式碼區塊內的文字整段貼進 Claude Code。
貼之前請先確認專案根目錄已有 `documents/BDD.md`、`documents/DESIGN.md`、`design/`（7 張線稿），以及空的 `frontend/`、`backend/`，且文件版本與上方 `requires` 一致。
版本紀錄在本文件最下方，不需要貼進去。

```text
【提示詞版本】v1.3.0（2026-10-15）
【對應文件版本】BDD.md v1.1.0、DESIGN.md v1.1.0。若專案內文件的版本與此不同，請先告訴我再開始。

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
預設請不要自行 commit 或 push；只有我明確要求時才執行。
commit 前，先用 git status 確認 application-local.properties 沒有被加入，並列出要提交的檔案給我確認；
push 時不要使用 force push（--force）。
請建立 .gitignore，內容至少涵蓋：
  backend/target/
  backend/.classpath
  backend/.project
  backend/.settings/
  backend/src/main/resources/application-local.properties
  frontend/node_modules/
  frontend/dist/
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
- 呼叫 Google Books：用 Spring 內建的 RestClient，JSON 解析用內建的 Jackson。
- 建表：spring.jpa.hibernate.ddl-auto=update（僅限練習，STEPS.md 請註明）。
  種子資料用一個 CommandLineRunner，資料不存在才寫入。
- 測試：JUnit 5 + MockMvc，Google Books 以 Mockito 模擬，不使用 Cucumber。
  測試時使用 H2 記憶體資料庫（只以 test scope 加入依賴，測試設定放 src/test/resources），
  測試不連 MySQL，也不需要 application-local.properties。
  依 BDD.md 的 Scenario 為後端 API 寫測試（測試名稱標註對應的 Scenario）。
  前端行為請在 STEPS.md 提供一份對照 BDD.md 的手動測試清單。

【資料庫】
MySQL 8.0，字元集 utf8mb4。資料庫（library）與專用帳號我會自己先建好，不會使用 admin/root 帳號。
資料庫連線設定（網址、帳號、密碼）放在
backend/src/main/resources/application-local.properties（不進 git），由我自己建立並填寫。
請不要讀取、輸出或修改這個檔案的內容，也不要把密碼寫死在程式碼或其他會進 git 的檔案裡。
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
1. 書籍來源：先查本機 books 表；查不到再呼叫 Google Books API，查到就存回 books 表。
   Google 查不到才回「找不到此書籍」；Google 無法連線時回「查詢失敗，請稍後再試」。
   Google 的出版日期可能只有年或年月，publish_date 請用字串欄位。
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
12. Google Books 資料對應：書名只取 title（不含副標題）；多位作者用「、」連接；出版日期照原樣存。
    呼叫時不使用 API 金鑰。
13. 前端打開網頁時先呼叫 GET /api/auth/me 取得登入狀態，路由守衛等這個結果出來才判斷是否放行，
    避免重新整理頁面時被誤判為未登入。

【資料表】
- users：id、nickname、nickname_lower（唯一）、mobile（唯一）、created_at
- books：isbn（主鍵，13 碼）、title、author、publish_date（字串）
- favorites：id、user_id（外鍵）、isbn（外鍵）、created_at，(user_id, isbn) 設唯一鍵
- 種子資料：BDD.md 的 2 本書，以及會員 Cooqle / 0900123456

【API】（REST 一律加 /api 前綴，避免與前端頁面網址衝突）
- GET /api/books/{isbn}：公開。200 書籍資料；400 格式錯誤；404 找不到；503 Google Books 無法連線
- POST /api/auth/login：公開。登入/註冊合一。200 成功；400 欄位驗證錯誤；409 暱稱與手機號碼不符、此暱稱已被使用
- POST /api/auth/logout：需登入，清除 Session
- GET /api/auth/me：需登入，回傳暱稱與手機（個人頁與判斷登入狀態用）
- POST /api/favorites/add：需登入，user_id 取自登入者；201 成功；409 重複收藏；404 無此書
- GET /api/favorites：需登入，只回傳自己的收藏（含書籍資料）
- DELETE /api/favorites/{id}：需登入，須驗證屬於本人；不存在或不是本人的收藏一律回 404

【前端頁面】
共 4 個路由、7 張線稿，詳見 DESIGN.md 第 5 節：
- 首頁 /：初始、查詢成功、查詢錯誤三種狀態
- 登入/註冊 /login
- 收藏 /favorites：已登入清單；未登入提示畫面僅作為 Session 失效時的備用
- 個人頁 /profile
/favorites 與 /profile 需登入，使用 Vue Router 的路由守衛；
未登入時依 BDD.md 的訊息提示並導向 /login。
所有共用元件（導覽列、卡片、輸入框、按鈕、錯誤框、Toast）請做成可重用的 Vue 元件。

【做事方式】
1. 先給我完整計畫：技術與版本、資料表設計、每個 Scenario 由哪個類別或 API 實作，等我確認再開始寫。
2. 一步一步做，每完成一步就編譯、啟動並實際測試，並說明哪些 Scenario 已通過；不要一次寫完全部。
3. 同時維護 documents/STEPS.md，每一步寫：目標、建立或修改的檔案、完整程式碼、
   每個重要區塊的白話解釋、對應的 Scenario、怎麼驗證成功。
4. 說明以 Java 初學者為對象：多用生活化比喻和小範例，避免艱深術語，遇到名詞先解釋
   （尤其是 Session、攔截器、JPA、全域例外處理這幾個概念）。
5. STEPS.md 結尾寫「如何快速上手」，教我之後怎麼照著步驟自己重做一遍，不要寫成考試複習清單。
```

## 版本紀錄

版本號規則（MAJOR.MINOR.PATCH）：
- MAJOR：技術棧或整體架構改變（例如換前端框架、換資料庫）
- MINOR：新增或調整功能規則、設計決策
- PATCH：只修正文字、錯字、說明

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
