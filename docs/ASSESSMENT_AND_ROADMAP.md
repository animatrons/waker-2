# Waker — Technical Assessment & Resumption Roadmap

Date: 2026-07-27
Status: **Draft for discussion** — open decisions are flagged with 🔶 and tracked in "Open Decisions" at the end.

---

## 1. Executive Summary

- The backend (`waker-back`) is **not Spring** — it's a hand-rolled Jetty 11 servlet app with a manual DAO/Service/App layering, MongoDB via the legacy 3.x driver + Jongo (unmaintained since ~2016), and a home-grown JWT. ~71 Java files, **zero tests**, no Docker, no scheduler.
- It is **less "over-engineered" than you remember, and more "unfinished."** The Penalty abstraction (abstract class + enum + factory + strategy service) is a reasonable, working pattern. But it only has 3 trivial penalties implemented (scold, embarrassing text/email — all console-log stubs except email). There is **no Mission abstraction at all** yet — "missions" don't exist in code; there's only a `Fulfillments` enum with a single value (`WRITE_SOMETHING`).
- The single biggest functional gap relative to your own stated core rule ("deleting the app doesn't cancel commitments") is that **there is no backend scheduler**. Today, a client must call `/reminder/missed` for a penalty to fire. That's the opposite of what the product needs — enforcement must be autonomous and server-side.
- `waker-front` is explicitly out of scope per your instruction — we're going native Android, so it will not be assessed or referenced further.
- This document covers: architecture evaluation, dependency/tech-stack evaluation, a database decision (Mongo vs. relational), a breakdown of every rule/mission/penalty you listed with feasibility notes and refined rules, a payment/billing strategy for the "pay $" penalty, and a phased action plan to a shippable v1.

---

## 2. Current State — What Actually Exists

### 2.1 Stack (as built)

| Layer | Reality |
|---|---|
| Runtime | Java 17, embedded **Jetty 11** (`javax`/`jakarta` servlets), not Spring |
| DB access | `mongo-java-driver` **3.12.11** (legacy sync driver, deprecated) + **Jongo 1.5.0** (dead project) |
| Auth | Hand-rolled HMAC-SHA256 JWT + PBKDF2 password hashing, hardcoded secret in source |
| Email | Gmail API (primary, domain-delegated service account), Postmark SDK (wired but unused), MailSlurp (marked "deprecated" in its own javadoc), raw JavaMail SMTP (stub, unfinished) |
| Templating | Handlebars, one template (registration email) |
| Mapping | MapStruct + Lombok |
| JSON | Gson, with a custom polymorphic adapter for penalties |
| Scheduler | **None** |
| Tests | **None** |
| Docker | **None** |
| Payments | **None** (only a commented-out `stripeCustomerId` field) |
| Social/SMS | **None** |

### 2.2 Architecture (as built)

```
Servlet (web/)  →  App (app/, orchestration + auth checks)  →  Service (service/)  →  DAO (dao/)  →  Jongo/Mongo
                                                                      ↓
                                                            PenaltyFactory → IPenaltyService<T>
```

- `AModel` — abstract base for all Mongo documents (`key`, `createdAt`, `updatedAt`, `code`, `label`, `enabled`, abstract `getCollectionName()`).
- `Reminder` — the actual core entity today (not "Commitment" or "Mission"): `user`, `name`, `description`, `notifyTime`, `deadline`, `fulfillmentMethod`, `penaltySetting`, `status` (1/0/-1), `active`.
- `APenalty` (abstract, `@JsonTypeInfo` polymorphic) → `GetScolded`, `EmbarrassingText`, `EmbarrassingEmail`. Matched to a handler via `Penalties` enum + `PenaltyFactory.getService(signature)` (an `if/else` chain returning `new XyzService()`, unchecked-cast to a generic type).
- `IGenericDao<T>` / `AGenericDao<T>` — a hand-written generic CRUD layer using reflection (`targetClass.getDeclaredConstructor().newInstance()`) to work around Java type erasure, re-implementing what Spring Data / a JPA repository gives you for free.
- `IBaseService<T>` / `BaseService<T>` — same idea one layer up.

**Verdict on the abstraction:** the *business-differentiating* abstraction — polymorphic Penalty (and, once built, Mission) types dispatched through a strategy/factory — is the right idea and worth keeping conceptually. It's what lets you add a new penalty or mission without touching core commitment logic. What's not worth keeping is the *generic infrastructure* abstraction (`AGenericDao`, `IBaseService`, the manual singleton factories) — that's 2015-era Java EE boilerplate that a modern framework (Spring Data repositories + Spring DI) eliminates almost entirely. So: **not "too complicated" conceptually, but built on infrastructure that makes it look and feel heavier than it needs to be.**

### 2.3 What's implemented vs. stubbed vs. absent

| Feature | State |
|---|---|
| User registration/login, JWT auth | Implemented (needs hardening) |
| Create/read a Reminder | Implemented |
| Mark fulfilled | Implemented (manual client call) |
| Mark missed → trigger penalty | Implemented, but **only reachable via manual client call**, not autonomous |
| Penalty: scold / embarrassing text / embarrassing email | Implemented as **console `println` stubs** (no real delivery except the registration email pathway) |
| Missions (QR, location, math, writing, speech) | **Not built** — only a placeholder enum |
| Pay-money penalty | **Not built** |
| Social share penalty (X/IG/FB) | **Not built** |
| WhatsApp/SMS penalty | **Not built** |
| Official leaderboard / "wall of shame" | **Not built** |
| Background scheduler / autonomous enforcement | **Not built** |
| Free/Pro plans, entitlements, subscriptions | **Not built** |
| Docker / one-command local run | **Not built** |
| Tests | **Not built** |

Full raw technical inventory (endpoints, file tree, exact snippets) is preserved in the appendix at the end of this doc.

---

## 3. Architecture & Tech Stack Evaluation

### 3.1 Backend framework: move to Spring Boot

Recommendation: **Spring Boot 3.x on Java 21 (LTS)**, replacing the hand-rolled Jetty/servlet/DAO/factory stack.

Why this is a big net win here specifically:
- Eliminates `AGenericDao`/`IBaseService`/manual singleton factories — Spring Data repositories + constructor-injected `@Service`s replace ~all of that.
- Gives you `@Scheduled` (or a proper job runner) for free — directly fixes the "no autonomous enforcement" gap, which is your most important structural requirement.
- Spring Security replaces the hand-rolled JWT/crypto (fewer places to get security wrong — you currently have a hardcoded JWT secret and hardcoded DB credentials in `pom.xml`).
- Spring's `Map<String, MissionHandler>` / `List<PenaltyHandler>` autowiring gives you the strategy/factory pattern you wanted for missions & penalties *without* hand-written factories — you keep the extensibility goal, drop the boilerplate.
- Testability (Testcontainers, `@SpringBootTest`, MockMvc) — you have 0 tests today; a framework with first-class test support matters a lot for a system that moves money and applies real-world penalties.
- Docker: trivial multi-stage build with Spring Boot's layered jars / buildpacks.

This is a rewrite of the wiring layer, not the business logic — and given there's no test suite and the business logic itself is thin (3 stub penalties, no missions), now is the cheapest point in the project's life to do this.

🔶 **Open decision:** stay in Java, or move to Kotlin for the backend? Kotlin pairs naturally with a Kotlin/native-Android app (shared mental model, coroutines feel similar to Android-side coroutines), and Spring Boot's Kotlin support is first-class. Java is lower migration risk and still an excellent choice with Spring Boot. Either is fine technically — this is a preference call, see Open Decisions.

### 3.2 Database: MongoDB vs. relational

Your question: *"is Mongo a good choice, can we easily change it?"*

**Yes, you can easily change it — you're at the best possible point to do so** (small codebase, 0 tests, no production data, Jongo is already a liability regardless of what you decide).

The deeper question is which is *right* for this domain, and here I'd actually push back on Mongo:

**Arguments against Mongo for Waker specifically:**
- The core of this product is **money movement and enforcement guarantees** (penalty charges, refunds, caps, audit trails, leaderboard aggregation, "did this fire exactly once"). This is a textbook case for **ACID transactions and relational integrity** — you need to atomically move a commitment from "active" → "penalized" → "charged," and never double-charge or double-post on a retry/crash. Mongo *can* do multi-document transactions since v4, but it's fighting the tool; Postgres does this natively and simply.
- Reporting needs (leaderboard, admin dashboards, "how much has been donated to charity this month," per-user risk exposure) are naturally relational/aggregate queries — trivial in SQL, more awkward in Mongo's aggregation pipeline.
- The one thing Mongo is genuinely good at here — storing heterogeneous per-type config (a `LocationMission` has different fields than a `QRCodeMission`) — is just as well solved in Postgres with a `JSONB` column for the type-specific config, while keeping the core entities (`User`, `Commitment`, `Payment`, `LedgerEntry`) fully relational and transactional.

**Recommendation: PostgreSQL, with JSONB columns for polymorphic mission/penalty configuration.** You get the flexibility you wanted from Mongo for extensibility, plus the transactional guarantees you need for money and enforcement, plus one operational dependency instead of two, plus first-class support in Spring Data JPA + Flyway for versioned migrations (important once you have real users and can't just wipe the DB).

🔶 **Open decision:** confirm Postgres, or you'd rather keep MongoDB (e.g., with modern Spring Data MongoDB, dropping Jongo) for other reasons (team familiarity, existing ops tooling). See Open Decisions.

### 3.3 Docker

None exists today. Plan: multi-stage `Dockerfile` (Maven/Gradle build stage → slim JRE runtime stage) plus a `docker-compose.yml` that brings up the backend + Postgres (or Mongo) + a mail-catcher (e.g. MailHog/Mailpit) for local dev, so nobody needs a local JVM/DB install — matches your ask directly. This is Phase 1 work (see roadmap).

### 3.4 Dependency modernization checklist

- Java 17 → **21 LTS**.
- `mongo-java-driver` 3.x + Jongo → **either** modern `mongodb-driver-sync`/Spring Data MongoDB **or** dropped entirely in favor of Postgres (per 3.2).
- Hand-rolled JWT/crypto → **Spring Security + a maintained JWT lib** (or Spring Authorization Server if you want OAuth2/social login down the line).
- `javax.mail` 1.5.0-b01 (2013-era) → **Jakarta Mail** (current) or drop in favor of a single transactional email provider.
- MailSlurp → remove (author's own comment marks it dead code).
- Pick **one** email provider for production sends (Postmark is the cleanest fit; Gmail API via domain delegation is fragile/quota-limited for transactional volume) and delete the rest.
- Add: Flyway/Liquibase (migrations), Testcontainers + JUnit5 + Mockito (tests), Micrometer/Actuator (health/metrics), a structured logger (Logback + JSON encoder) instead of `slf4j-simple`.
- Remove hardcoded credentials from `pom.xml`/source; move to env vars / secrets manager (even for local dev, use `.env` + docker-compose, not values baked into build files).

---

## 4. Native Android Decision

Agreed: **drop Ionic/Capacitor hybrid, go native Android first**, `waker-front` is ignored entirely.

This isn't just a preference call — it's the right call *for this specific product*. Waker's two riskiest technical requirements are:
1. **Exact, reliable alarms** that fire even under Doze/App Standby/battery optimization.
2. **Background enforcement signals** (e.g., geofencing for location missions) that survive OS process death.

Both of these are notoriously unreliable through a WebView/hybrid bridge (Capacitor plugins for exact alarms & background geofencing are thin wrappers around the same OS APIs, but add a layer of indirection, plugin-maintenance risk, and WebView process lifecycle quirks). Native gives direct access to `AlarmManager.setExactAndAllowWhileIdle`, `WorkManager`, `Foreground Services`, and the Fused Location Provider's geofencing API, with full control over battery-optimization exemption prompts — all first-class citizens.

🔶 **Open decision:** Kotlin + Jetpack Compose (recommended — modern, less boilerplate, Google's current default) vs. Kotlin + XML/View-based UI. See Open Decisions.

---

## 5. Business Rules — Breakdown, Feasibility, and Suggested Refinements

Going through each rule/idea you listed:

### 5.1 "Can only commit within the next 24h"

**Agreed, and important.** Refinements:
- Enforce server-side (not just UI): reject commitment creation if `notifyTime > now + 24h`, at the API layer.
- Add a **cool-down/edit window** immediately after creation (e.g., 5–10 minutes) during which the user can cancel or edit penalty-free — covers "I fat-fingered the stake amount," without undermining commitment (people don't forget something they set 5 minutes ago).
- After the cool-down, require a tiered confirmation flow before lock-in, scaled to penalty severity:
  - Low stakes (leaderboard/self-only): single confirm.
  - Medium (social share, message to a contact): confirm + re-show exact text/content that will be posted/sent.
  - High stakes (money, "appear on official Waker account"): explicit re-authentication or a "type the amount to confirm" pattern (like GitHub's "type the repo name to delete"), plus a mandatory short explainer of exactly what happens and when.
- Cap **concurrent active commitments per user** (e.g., 3–5) — prevents a user from stacking risk and forgetting about older ones, and reduces support/abuse surface early on.

### 5.2 Once committed, cannot cancel (after warnings)

Agreed as the core mechanic (this *is* the product). Implementation notes:
- The "cannot cancel" guarantee must be enforced by the backend scheduler, not the client — this is exactly why the current "client calls /missed" design is broken and must be replaced with an autonomous sweep job (see 3.1 and roadmap Phase 3).
- Define explicit **exception paths** you'll need eventually for support/legal reasons even if "no cancel" is the default: fraud/abuse, payment method failure through no fault of the user (e.g., bank outage), app store policy disputes, legal (chargebacks). Have an internal admin override from day one, logged/audited, not exposed to end users.

### 5.3 Deleting the app doesn't cancel commitments

Agreed, and this is a direct consequence of 5.2 — commitments live and are enforced entirely server-side; the app is just a UI. Concretely this means:
- Push notifications degrade gracefully (uninstalled → no reminder ping) but the **penalty still fires on schedule** via the backend job, independent of any client being present.
- You'll want a "reinstall and see your penalty history / outstanding commitments" flow, since account state persists independent of the device.

### 5.4 Payment penalty cap & general penalty limits

**This is the highest-risk area of the whole product and deserves the most care.** Concrete recommendations:
- **Per-commitment stake cap**, e.g. Free: $10–20 max; Pro: higher but still capped (e.g. $100), never unlimited.
- **Total-at-risk cap** across all *simultaneously active* commitments (e.g. never more than $50 at risk at once), independent of per-commitment caps — this is what actually protects a user from compounding stacked losses.
- **Trust-based ramp-up**: new accounts start at a low cap (e.g. $5) and it increases gradually with a track record of fulfilled commitments (mirrors how real fintech/lending risk models work, and limits your fraud/chargeback exposure while you have no reputation data on a user).
- Similar "severity caps" should exist for non-money penalties too — e.g. cap how many "official leaderboard/shame" appearances or how personal/embarrassing a pre-written message can be, since reputational harm has real-world consequences too.
- 🔶 Worth flagging explicitly: a "pay if you fail a bet on yourself" mechanic sits adjacent to gambling regulation in some jurisdictions, *even though there's no chance element and no counterparty payout* (this is a commitment device, not a wager — you're not betting against another user or the house, and you don't win anything by "succeeding," you just avoid a loss). This exact model has real-world precedent — **stickK.com** and **Beeminder** have operated "put money on the line, pay a stakeholder/charity if you fail" products for over a decade in the US — which is a good feasibility signal, but you should get a real legal opinion before launch (especially before expanding to other countries), and keep the "no chance element, no counterparty upside" framing explicit in your ToS.

### 5.5 Penalty types — feasibility per type

| Penalty | Feasibility for v1 | Notes |
|---|---|---|
| **Pay $ (charity)** | Medium complexity, high value — build carefully | See dedicated section 6 below |
| **Feature on official Waker social/website "losers leaderboard"** | **Easiest — good v1 candidate** | You control the posting (your own account), no OAuth/API review needed from the user's side, just needs user consent captured at commitment time |
| **Email to a contact** | Easy | Infra already exists (Postmark) |
| **WhatsApp message to a contact** | Medium | Needs WhatsApp Business API (via Meta or a BSP like Twilio) — per-message cost, template pre-approval required for anything beyond a 24h session window. **Suggest**: launch with SMS (Twilio, simple, no template approval) or email as the v1 "message a contact" penalty, add WhatsApp post-v1 |
| **Post on the user's own Twitter/X** | Hard now, reconsider for v1 | X API access changed materially since 2023 — posting on a user's behalf requires OAuth 2.0 user-context auth and a paid API tier; cost and review friction is non-trivial for a small team |
| **Post on the user's own Instagram/Facebook** | Hard for v1 | Meta Graph API posting requires a Business/Creator-linked account and Meta App Review for the relevant permissions — slow, and Meta can revoke/change terms |
| **Suggested v1 fallback for social penalties**: "share intent" instead of full API automation — the app pre-fills the post/caption and *forces* the native share sheet at the exact penalty moment (user still must tap "post," but can't dismiss without acknowledging the miss, and the app can verify via callback/deep-link where the platform supports it). Lower engineering cost, no app review dependency, defer true automated on-behalf-of posting to a later phase once there's a user base to justify the API costs and review overhead. |

### 5.6 Mission types — feasibility per type

| Mission | Feasibility for v1 | Notes |
|---|---|---|
| **QR code scan** | **Easy — good v1 candidate** | Standard on-device barcode scanning (ML Kit / CameraX), pair with a physical QR code placed somewhere deliberate (e.g., the kitchen) |
| **Math games** | **Easy — good v1 candidate** | Fully on-device, no external API |
| **Write something (typing task)** | **Easy — good v1 candidate** | Already has a rough equivalent in the current code (`WRITE_SOMETHING`) |
| **Speak a preset phrase** | Medium | On-device `SpeechRecognizer` (Android) is workable but accuracy varies with accents/noise — needs a fuzzy-match threshold, not exact string match |
| **Location-based** | Medium-hard | Requires background location permission — Android 10+ requires a clear in-flow justification and is heavily scrutinized in Play Store review for background location; geofencing battery/accuracy trade-offs. Your own rule ("must be >5km from home/current location") is good and should be enforced server-side at mission-configuration time using a stored "home location," not just client-side |
| **Rules that should generalize across all mission types**: a max-time-to-fulfill window (as you already specified), a minimum config lead time (can't set a mission up 30 seconds before the alarm), and — importantly — **anti-cheat basics** appropriate to each type (e.g., location spoofing detection via mock-location flags on Android; QR codes tied to a specific registered code, not a photo of any QR code; math/write tasks server-validated, not just client-validated) since penalties have real stakes and someone *will* try to game it. |

### 5.7 Free vs. Pro plan

Agreed as a standard freemium shape. Suggested v1 split:
- **Free**: 1 active commitment at a time, a small subset of missions (e.g. QR + writing + math) and penalties (e.g. leaderboard + email), low stake cap.
- **Pro**: all mission/penalty types, higher stake caps, multiple concurrent commitments, higher trust ramp-up ceiling, maybe analytics/history.
- Billing mechanism 🔶 (see Open Decisions): Google Play Billing (required if sold as an in-app subscription on Android) vs. Stripe Billing directly (simpler backend, but Play Store policy generally requires Play Billing for digital subscription content — this needs to be resolved before Play Store submission, not after).

---

## 6. Payment Penalty — Detailed Strategy (the hardest part)

This deserves its own section since you flagged it as the least clear and most complex.

### 6.0 Processor selection — updated after country constraint

Stripe is unavailable without standing up an offshore US entity + bank account (too costly for this stage), so Stripe is **ruled out** for now. Two commonly-suggested alternatives — **Paddle** and **Lemon Squeezy** — were evaluated and are **recommended for the Pro subscription only, not for the "pay $" penalty**:

- Both are Merchant-of-Record platforms built for checkout-based digital product/SaaS sales — a great, low-friction fit for a **fixed-price recurring Pro subscription** (no local entity or payment license needed, they handle global tax/VAT).
- Both explicitly **prohibit gambling/betting/wagering-type products** in their Acceptable Use Policies (bets, sweepstakes, contests, games of chance, pay-to-play, etc.). Waker's penalty mechanic is arguably *not* gambling in a legal sense (no chance element, no prize, proceeds go to charity rather than to a counterparty or "the house"), but compliance/KYB reviewers commonly pattern-match "pay if you fail a bet on yourself" to wagering and reject or later suspend the account — which would jeopardize the Pro subscription revenue too, since it would likely be the same merchant account.
- Both are also architecturally built around a "checkout" transaction (customer buys a specific product at a specific price, in the moment) rather than a backend-triggered, variable-amount, unpredictably-timed debit against a saved card following a failure event — a fundamentally different transaction shape than what they're designed and risk-modeled for.

**Recommendation: split the two payment needs onto different rails, and split the "pay $" penalty itself into two stages (personal/dogfood, then multi-user).**

- **Pro subscription** → Lemon Squeezy (simpler self-serve onboarding, sufficient for a v1 subscription) or Paddle (more mature subscription/billing features, but has an upfront business-review/approval process) — either is a legitimate, low-risk fit for this specific use case.
- **"Pay $" penalty, Stage A — personal/dogfood (decided, see §6.1 below)**: while Waker has one real user (you), the "collect from arbitrary customers" problem doesn't exist yet — it's just "push my own money to a payee I already agreed with." This is solved with **PayPal Payouts**, is far lower-risk than the MoR options above, and can be built now without waiting on a multi-user processor decision.
- **"Pay $" penalty, Stage B — multi-user (deferred to Phase 7+)**: once other users need to stake their *own* money, the earlier problem returns (collecting from many different people, vaulting many cards, KYC at scale) and still needs a dedicated processor-selection spike with proactive, written disclosure of the business model — candidates remain PayPal (if a "collect" flow can be approved) or a regional/local PSP built for merchant-initiated transactions (Checkout.com, Rapyd, dLocal, or a country-specific acquirer) 🔶 pending confirmation of which country(ies) you'll be accepting users from. Not blocking for v1.

### 6.1 Stage A (decided): Personal automated payout via PayPal Payouts

Since it's currently just you (payer) and a charity you've already agreed with (payee), this sidesteps card vaulting, PCI scope, and KYC entirely — it's "programmatically push my own money to a fixed recipient when triggered," which PayPal's **Payouts API** (formerly Mass Pay) is built for directly, and which sits in a materially safer risk category than the MoR/checkout discussion above (you're not collecting wagers from customers, you're using standard business disbursement tooling to move your own money out — the same mechanism used for automated refunds/commissions/reimbursements).

**Setup (one-time):**
1. Open a free PayPal Business account; verify identity and link your bank.
2. Apply for Payouts API access with a plain, honest use-case description ("personal automation: send a fixed amount to \[charity] when I miss a personal commitment; single sender, single recipient, low volume").
3. Store the charity's PayPal email (or bank details) as a fixed recipient in config — no per-user recipient management needed at this stage.

**Ongoing operation:**
- Payouts are drawn from your **PayPal balance**, not pulled live from a card/bank per transaction, so you top up a balance periodically (e.g. monthly) to cover your own at-risk exposure. This pre-funded balance doubles as a hard safety ceiling: even a scheduler bug can never move more money than what's sitting in it. Recommend sizing it to match the total-at-risk cap from §5.4 (e.g. keep ~$50 loaded).
- **The trigger itself is fully automated**: when the backend's autonomous scheduler (Phase 3) confirms a miss, it calls the Payouts API with the stake amount and a unique idempotency key tied to the commitment ID (`PayPal-Request-Id` header) — guarantees exactly-once payout even under retries/crashes, with zero human interaction at the moment of penalty.
- Region note: **Wise** was evaluated as an alternative and can do the same thing with a personal API token, but Wise can only automate the final "fund the transfer" step without a manual tap for accounts based in the US, Canada, Australia, New Zealand, Singapore, or Malaysia — elsewhere (UK/EEA included) that step currently requires a manual click per transfer due to local payment regulation, which breaks full automation. PayPal Payouts has no such restriction, so it's the primary recommendation regardless of country; Wise is a fine substitute only if you're incorporated/based in one of those six countries.

**Safety guardrails to build in from day one (this moves real money):**
- **Sandbox/dry-run mode** first — let the scheduler log "would have paid $X to \[charity] for commitment Y" for a burn-in period before enabling real transfers.
- **Append-only ledger table** recording every trigger attempt and outcome, independent of what PayPal reports back (fits the Postgres audit-trail design from §3.2/§6.2).
- **A manual kill-switch** (config flag) to freeze automated payouts instantly without a deploy.
- Wrap this behind a `PenaltyPaymentGateway`-style interface so this PayPal implementation is swappable for Stage B later without touching commitment/enforcement logic — the same strategy-pattern extensibility already used for missions/penalties, one layer deeper.

### 6.2 General mechanics for the automated flow (applies once Stage B's processor is chosen)

The shape below still applies when multi-user "pay $" penalties are built (Stage B) — it's processor-agnostic, described against Stripe-style primitives for clarity, but the same shape applies to PayPal reference transactions or a regional PSP's vaulting + MIT APIs. Not needed for Stage A, since Stage A has no per-user card vaulting at all.

1. **Onboarding the payment method** (once per user, not per-commitment): a `SetupIntent`-style flow to save a card with no upfront charge. Same pattern subscription apps use for "charge later, off-session."
2. **At commitment time**: no charge happens. Store the intended stake amount against the commitment, validated against the per-commitment and total-at-risk caps (section 5.4).
3. **On confirmed miss** (backend scheduler determines this autonomously — not client-driven): create an off-session charge for the stake amount using the saved payment method. Handle failure modes explicitly:
   - Card declined/expired/insufficient funds → retry policy (e.g., 3 attempts over 48h), then lock new commitment creation until resolved.
   - Requires additional authentication (3DS/SCA, common in EU) → the trickiest edge case for off-session charges; needs a fallback (email the user a payment link) since interactive 3DS can't be forced on an off-session charge.
4. **Charity flow — keep it decoupled from the individual charge.** Don't route each penalty directly to a charity in real time (fees, latency, reconciliation complexity). Instead: penalty charges land in a dedicated Waker balance → an internal ledger tracks "amount owed to charity" → a **monthly (or quarterly) batch payout**, with a public transparency page showing cumulative amounts donated (also your advertising asset, as you mentioned). Matches how stickK/similar products operate. Stage A already implements a version of this pattern directly (payout instead of collect-then-batch), since there's no intermediary collection step needed with a single user.
5. **Legal/compliance groundwork before Stage B launch**: Terms of Service that clearly frame this as a self-commitment/donation mechanism (not a wager, no counterparty payout, no chance element), a refund policy, and — if you want to be extra safe — routing to a registered 501(c)(3) or local equivalent so Waker itself never "keeps" the penalty money.
6. **Data model implication**: needs a proper `Payment`/`LedgerEntry`/`CharityPayout` set of relational tables with immutable audit rows (append-only, never update-in-place) — another strong argument for Postgres over Mongo for this slice of the domain specifically.

---

## 7. Action Plan — Phased Roadmap to v1

Phasing prioritizes: (a) fixing the one architectural gap that breaks your core promise (autonomous enforcement), (b) getting a full simple loop working end-to-end before adding the hardest integrations (money, social APIs), (c) isolating the highest-risk/compliance-heavy work (payments) into its own dedicated phase so it doesn't block everything else.

**Phase 0 — Decisions & groundwork** (this doc's Open Decisions, resolved together)
- ✅ Decided: Java + Spring Boot, PostgreSQL, Kotlin + Jetpack Compose, share-intent for v1 social penalties, PayPal Payouts for the personal "pay $" penalty (Phase 4.5), Lemon Squeezy/Paddle for the Pro subscription.
- Remaining before Phase 0 fully closes: Play Billing vs. MoR-subscription policy check (§8, item 6); Stage B multi-user payment processor can be deferred to Phase 7.
- Set up new repo hygiene: CI (build + lint + test on PR), `.env`-based local config, secrets out of source control.

**Phase 1 — Backend foundation rewrite**
- Spring Boot skeleton, Postgres + Flyway, Spring Security + JWT, Dockerfile + docker-compose (backend + DB + mail-catcher), replace `User` module first (register/login) as the walking skeleton, basic test setup (Testcontainers).

**Phase 2 — Core commitment loop (no money, no social APIs yet)**
- `Commitment` (rename from Reminder) + `Mission` abstraction (interface/strategy, mirroring the good parts of the old Penalty design) with the 3 easiest missions: QR code, writing task, math game.
- `Penalty` abstraction with the 2 easiest penalties: email-to-contact, official leaderboard/shame page.
- Full create → notify → fulfill/miss → penalize loop working end-to-end, driven entirely by the backend (no client callback required to detect a miss).

**Phase 3 — Autonomous enforcement & native alarm integration**
- Backend scheduler (sweep job on a short interval, idempotent, safe under multi-instance later) that detects overdue commitments and triggers penalties — this fixes the current architecture's biggest gap.
- Push notifications (FCM).
- Native Android: `AlarmManager` exact alarms, `WorkManager` for resilient background checks, battery-optimization exemption UX, foreground service for the "fulfillment window" countdown.

**Phase 4 — Native Android app core UX**
- Onboarding, commitment-creation wizard (with the tiered confirmation flow from 5.1), mission fulfillment screens for the 3 v1 missions, notification/miss handling, basic history.

**Phase 4.5 — Personal automated "pay $" penalty (decided: PayPal Payouts)**
- Per §6.1: PayPal Business account + Payouts API access, `PenaltyPaymentGateway` abstraction + `PayPalPayoutsGateway` implementation, append-only ledger table, sandbox/dry-run burn-in period, manual kill-switch, pre-funded balance sized to your personal total-at-risk cap.
- This gets the hardest penalty type fully real for dogfooding without waiting on a multi-user processor decision — single-user only, not yet exposed to other accounts.

**Phase 5 — Remaining missions & penalties**
- Location mission (with anti-spoofing + the >5km rule enforced server-side), speech-phrase mission.
- Social share-intent penalties (X/IG/FB), SMS/WhatsApp message penalty.

**Phase 6 — Monetization**
- Free/Pro entitlements, subscription billing (Lemon Squeezy/Paddle, resolving the Play Billing policy question per Open Decisions), plan-based caps wired to section 5.4/5.7 limits.

**Phase 7 — Multi-user "pay $" penalty (Stage B)**
- Per §6.0/§6.2: dedicated processor-selection spike with proactive written disclosure, per-user payment method vaulting, off-session charging, retry/dunning, charity batch payout + public transparency page. Only needed once Waker has users beyond you.

**Phase 8 — Closed beta → launch**
- Dogfood with yourself + a small trusted group first (given the stakes involved, this is not optional), monitor for edge cases in the enforcement scheduler and payment retries specifically, then wider launch.

---

## 8. Open Decisions

These need your input before Phase 0 can be considered closed. I've marked my recommendation first in each.

1. ~~**Database**~~ — **Decided: PostgreSQL** + JSONB for polymorphic mission/penalty config.
2. ~~**Backend language**~~ — **Decided: Java** + Spring Boot.
3. **Payment processor** — Stripe is unavailable without an offshore US entity (ruled out for cost reasons). ~~Decided: Lemon Squeezy or Paddle for the Pro subscription~~ (Lemon Squeezy recommended first for simpler onboarding). ~~Decided: PayPal Payouts for the personal/dogfood "pay $" penalty~~ (§6.1, Phase 4.5). **Still open, but deferred/non-blocking: Stage B multi-user "pay $" penalty processor** (§6.0/§6.2, Phase 7) — shortlist remains PayPal (a "collect" flow, pending written approval) vs. a regional/local PSP 🔶 pending confirmation of which country(ies) you'll accept users from.
4. ~~**V1 social penalty approach**~~ — **Decided: forced share-intent / user-confirmed post.**
5. ~~**Android UI toolkit**~~ — **Decided: Kotlin + Jetpack Compose.**
6. **Subscription billing rail**: given Lemon Squeezy/Paddle is now the plan for web-initiated subscriptions, this still needs a Play Store policy check before launch — Google generally requires Google Play Billing for digital subscription content purchased *through the Android app itself*; a MoR-based subscription may be viable if positioned as a purchase made outside the app (e.g. via a web portal), similar to how some apps handle "manage your subscription on our website." Needs explicit verification against current Play Store policy before Phase 7.

---

## Appendix — Raw Technical Inventory (backend)

*(Preserved from the codebase deep-dive for reference; see sections above for the interpreted assessment.)*

### Endpoints (Jakarta `@WebServlet`)

| Method | Path | Servlet | Role |
|---|---|---|---|
| POST | `/auth/api/registration` | `RegisterUserServlet` | Register |
| POST | `/auth/api/login` | `LoginServlet` | Login → JWT |
| POST | `/auth/api/reminder` | `ReminderServlet` | Create/update reminder |
| GET | `/auth/api/reminder?id=` | `ReminderServlet` | Get reminder |
| POST | `/auth/api/reminder/fulfilled?id=` | `ReminderFulfilledServlet` | Set status=1 |
| POST | `/auth/api/reminder/missed?id=` | `ReminderMissedServlet` | Trigger penalty (client-driven — to be replaced) |
| POST | `/auth/api/test` | `TestWithMeServlet` | Test email send (currently auth-whitelisted — should not be) |

### Directory structure (`src/main/java/com/waker`)

```
Main.java
app/            UserApp, ReminderApp, PenaltyApp
web/            ReminderServlet, ReminderFulfilledServlet, ReminderMissedServlet, TestWithMeServlet
web/auth/       LoginServlet, RegisterUserServlet
web/filter/     Filter (CORS), AuthFilter (JWT gate)
service/        I*Service interfaces, EmailService (console stub)
service/impl/   UserService, ReminderService, GmailApiService, PostmarkMailService, MailSlurpService, SmtpMailService, HandlebarsTemplatingService, BaseService
service/penalty/        IPenaltyService, PenaltyFactory
service/penalty/impl/   EmbarrassingTextService, EmbarrassingEmailService, GetScoldedService
dao/            IGenericDao, AGenericDao, MongodbManagerFactory, IMongodbManager
dao/impl/       UserDao, ReminderDao
model/          AModel, User, Reminder, Address, Email, FulfillmentMethod, Fulfillments
model/penalty/           APenalty, Penalties
model/penalty/impl/      GetScolded, EmbarrassingText, EmbarrassingEmail
model/dto/ + mapper/     DTOs + MapStruct mappers
model/serialization/     ReminderJsonAdapter (Gson polymorphic deserializer for penalties)
model/exception/         TechnicalException, BusinessException + error-code enums
util/           ConfigProperties, Tools, security/Crypt
```

### Known hardening items before any real users touch this

- JWT secret is hardcoded in source (`UserService`) — must move to secret management.
- Mongo credentials hardcoded in `pom.xml` Maven profiles (`wakerman` / `1111`).
- CORS currently `Access-Control-Allow-Origin: *`.
- `/auth/api/test` (email test endpoint) is on the auth whitelist — remove before any deployment.
