# Waker (waker-back) - API Contracts

**Date:** 2026-07-27
**Base path:** `/` (embedded Jetty, context path `/`); all application routes live under `/auth/api/*` despite most of them not actually requiring authentication (see Authentication section).
**Format:** JSON in/out via Gson; every response is wrapped in a common `ResponseDTO<T>` envelope.

## Response Envelope

All endpoints return `com.waker.model.dto.ResponseDTO<T>`:

```json
{
  "data": { /* T, or null on error */ },
  "status": 200,
  "message": "human-readable status/error message",
  "page": null,
  "pageSize": null,
  "start": null,
  "total": null
}
```

`status` mirrors the HTTP status code set on the response (`resp.setStatus(response.getStatus())`), and is also duplicated inside the JSON body — clients must be prepared to read status from the body, not just the HTTP status line, since some paths only set the HTTP status but the pattern is inconsistent across servlets.

## Authentication

- **Mechanism:** Custom HMAC-SHA256-signed JWT (not a standard library — hand-built in `Crypt`/`UserService`/`Tools`). Header format: `Authorization: Bearer <token>`.
- **Token contents:** `sub` (email), `name`, `iat`, `exp` (10-day expiry, `UserService.TOKEN_AGE`).
- **Enforcement:** `AuthFilter` (`@WebFilter("/auth/*")`) runs on every request under `/auth/*`. It always sets CORS headers (`Access-Control-Allow-Origin: *`, permissive methods/headers), then validates the `Authorization` header via `UserApp.validateRequest`. If validation fails **and** the request path isn't in the hardcoded whitelist, the filter short-circuits with the validation error response and never reaches the servlet.
- **Whitelist (no token required):** `/auth/api/registration`, `/auth/api/login`, `/auth/api/test`. The inclusion of `/auth/api/test` here is a bug/oversight — it whitelists a route that triggers a real outbound email send (see below).
- **How the servlet receives identity:** Somewhat unusually, servlets recover the caller's email not from the incoming `request`, but by calling `resp.getHeader("logged-in-user")` — because `AuthFilter` added that header onto the shared `HttpServletResponse` object before calling `filterChain.doFilter(...)`. This works because it's the same response instance flowing through the chain, but it is a fragile, non-idiomatic pattern (reading a header back off the outbound response rather than propagating identity via a request attribute).

## Global CORS/Preflight Handling

`Filter` (`@WebFilter(urlPatterns = "/*")`) intercepts every request path in the app. For `OPTIONS` requests it short-circuits with `200` and an `Allow` header, without invoking `AuthFilter` or any servlet. For all other methods it sets UTF-8 response encoding and passes through.

## Endpoints

### `POST /auth/api/registration`

**Auth required:** No (whitelisted)
**Servlet:** `com.waker.web.auth.RegisterUserServlet`
**Request body:** `UserDTO`

```json
{
  "firstName": "string (required)",
  "lastName": "string (required)",
  "email": "string (required)",
  "password": "string (required, plaintext over the wire — no HTTPS enforced by the app itself)",
  "country": "string",
  "address": { "city": "string", "address1": "string", "address2": "string", "address3": "string", "zipCode": "string", "number": "string" },
  "birthDay": "date",
  "phone": "string"
}
```

**Behavior:** Validates required fields (`UserDTO.validate()`), rejects if the email already exists (`409`-style `BusinessException` via `ALREADY_EXISTS`, though the actual HTTP status returned is whatever `BusinessErrorCodesAndMessages` specifies — `409`), hashes the password with PBKDF2, persists the user, clears the password before returning it, and fires a registration confirmation email via `GmailApiService` using the `user_registration_confirmation_email.hbs` template. Email send failures are **not surfaced to the caller** — the response is still built as success (`201`) even if the subsequent mail send throws/fails, since the mail call result is never checked (`ResponseDTO<MailDTO> mailStatusResponse = mailService.send(...)` is assigned but unused).

**Response (`201`):** `ResponseDTO<UserDTO>` with `password` blanked out and `key` populated.

**Errors:** `400` (missing/invalid fields), `409` (email already registered).

### `POST /auth/api/login`

**Auth required:** No (whitelisted)
**Servlet:** `com.waker.web.auth.LoginServlet`
**Request body:** `UserDTO` (only `email` + `password` required — validated via `UserDTO.validate("login")`)

**Behavior:** Looks up user by email, validates password against the stored PBKDF2 hash, issues a JWT via `UserService.buildToken`.

**Response (`200`):** `ResponseDTO<UserOutputDTO>`:

```json
{
  "data": {
    "firstName": "string",
    "lastName": "string",
    "subject": "email (mapped from UserDTO.email via UserOutputMapper)",
    "accessToken": "JWT string",
    "refreshToken": null,
    "issuedAt": 1234567890
  },
  "status": 200,
  "message": "Login successful"
}
```

Note: `refreshToken` is a field on the DTO but is **never populated anywhere** — there is no refresh-token flow implemented.

**Errors:** `400` (missing fields / user not registered / invalid password — all currently mapped to the same `LOGIN_ERROR` code, which is a UX/security consideration: the API does not distinguish "no such user" from "wrong password" in its error *code*, though the error *message* text does differ).

### `POST /auth/api/test`

**Auth required:** No (whitelisted — **should not be**)
**Servlet:** `com.waker.web.TestWithMeServlet`
**Request body:** `MailDTO` (`mailFrom`, `mailFromName`, `mailTo`, `mailToName`, `subject`, `text`, `html`)

**Behavior:** Directly calls `GmailApiService.send(mailDto, fromUs=true)`, i.e. sends a real email through the app's Gmail service account using attacker-controlled subject/body if this endpoint is reachable in a deployed environment. This is clearly a developer convenience endpoint left in the main servlet set; it must be removed or gated before any real deployment.

**Response:** `ResponseDTO<MailDTO>` (`200` on send, `500` on failure).

### `POST /auth/api/reminder`

**Auth required:** Yes (valid JWT)
**Servlet:** `com.waker.web.ReminderServlet`
**Request body:** `ReminderDTO`, deserialized via a **custom Gson adapter** (`ReminderJsonAdapter`) that resolves `penaltySetting._class` to the correct concrete `com.waker.model.penalty.impl.*` class before deserializing (Gson has no native polymorphic-type support, unlike Jackson).

```json
{
  "user": { "email": "string (required — must match the authenticated caller)" },
  "name": "string (required)",
  "description": "string",
  "notifyTime": "yyyy-MM-dd HH:mm:ss (required, must be before deadline)",
  "deadline": "yyyy-MM-dd HH:mm:ss (required)",
  "fulfillmentMethod": { "name": "WRITE_SOMETHING", "setting": {} },
  "penaltySetting": {
    "_class": "GetScolded | EmbarrassingText | EmbarrassingEmail",
    "...type-specific fields (e.g. scoldingMessage, text, email)": "..."
  },
  "status": "-1 | 0 | 1"
}
```

**Behavior:** Validates the DTO (`ReminderDTO.validate()` — checks user email present, name/notifyTime/deadline present with `notifyTime < deadline`, penalty and fulfillment method both present and individually valid, status in `{-1,0,1}`), checks that `reminder.user.email` matches the caller's identity (`isOperationAllowed`), then persists.

**Note (ownership check bug):** identity comparison is done via `reminderDto.getUser().getEmail().equals(loggedInUsersEmail)`, but `loggedInUsersEmail` is read via `resp.getHeader("logged-in-user")` in the **`doPost`**, where `resp` is the *response* object — this relies entirely on `AuthFilter` having already written that header onto the shared response earlier in the chain; if that ordering assumption is ever broken (e.g. by an async dispatch, a different filter order, or a servlet container that doesn't share the same response object across the chain the way Jetty happens to here) the identity check silently receives `null`, and `null.equals(loggedInUsersEmail)` — actually `reminderDto.getUser().getEmail().equals(null)` — would just return `false`, denying the request; still, this coupling between filter and servlet via response headers is fragile and should be replaced with a request attribute in any rewrite.

**Response (`200`):** `ResponseDTO<ReminderDTO>` with `key` populated.

**Errors:** `400` (invalid fields), `401` (attempting to create a reminder for a different user's email than the caller).

### `GET /auth/api/reminder?id={id}`

**Auth required:** Yes
**Servlet:** `com.waker.web.ReminderServlet`

**Behavior:** Fetches by id, then checks ownership the same way as the `POST`.

**Response (`200`):** `ResponseDTO<ReminderDTO>`.
**Errors:** `400` (missing id), `401` (not the owner).

### `POST /auth/api/reminder/fulfilled?id={id}`

**Auth required:** Yes
**Servlet:** `com.waker.web.ReminderFulfilledServlet`

**Behavior:** Sets `status = 1` (fulfilled) via `ReminderApp.updateStatus`. **No verification of the actual fulfillment content happens** — calling this endpoint is sufficient to mark any reminder fulfilled; there is no mission-specific check (no QR validation, no location check, no math-answer check, etc.) because no such mission subsystem exists yet.

**Response (`200`):** `ResponseDTO<ReminderDTO>`.

### `POST /auth/api/reminder/missed?id={id}`

**Auth required:** Yes
**Servlet:** `com.waker.web.ReminderMissedServlet`

**Behavior:** This is the **only** path in the entire codebase that triggers a penalty. It calls `ReminderApp.takeAction(toPunish=true, id, callerEmail)`, which — if the reminder's `status == 0` (still pending) — invokes `PenaltyApp.takeAction` → `PenaltyFactory.getService(...)` → the matching `IPenaltyService.penalize(...)`, then updates the reminder's `status` to `-1` (not fulfilled).

**Critical gap:** since this must be called by a client, and there is no server-side scheduler, a user who never opens the app (or whose app is deleted, or who is offline) after missing a deadline is **never actually penalized** by the system as it stands today. This is flagged as the top-priority architectural fix in `ASSESSMENT_AND_ROADMAP.md`.

**Response (`200`):** `ResponseDTO<ReminderDTO>`, with a message concatenating the penalty outcome and the status-update outcome.

## Endpoints Not Yet Implemented (per product roadmap)

None of the following exist in code today; they are listed here only to make the gap explicit for planning purposes (see `ASSESSMENT_AND_ROADMAP.md`):

- Any endpoint for the "pay $" penalty (charge/payout initiation or confirmation)
- Any endpoint for social-media-post penalties (OAuth connect flows for X/Instagram/Facebook, post-on-behalf-of triggers)
- Any endpoint for WhatsApp messaging penalties
- Any endpoint for leaderboard read/write
- Any endpoint for mission types beyond the placeholder `WRITE_SOMETHING` (QR scan, location, math game, writing, speech)
- Any Pro/Free plan or entitlement/subscription endpoint
- Any account-deletion endpoint (relevant to the "deleting the app doesn't cancel commitments" business rule — there is currently no delete-user flow at all)
- Token refresh endpoint (the `refreshToken` field exists on `UserOutputDTO` but is dead)

---

_Generated using the BMAD Method `document-project` workflow_
