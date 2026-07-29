# Waker (waker-back) - Data Models

**Date:** 2026-07-27
**Database:** MongoDB (accessed via the legacy `mongo-java-driver` 3.12.11 + Jongo 1.5.0 ODM)
**Status:** Current state. See "Planned Target Schema" below and `ASSESSMENT_AND_ROADMAP.md` for the PostgreSQL migration direction.

## Overview

There is no schema-migration tool (no Flyway/Liquibase equivalent for Mongo) and no explicit schema definition beyond the Java classes themselves — Jongo/Jackson serialize POJOs directly to/from BSON documents. Two collections exist: `users` and `reminders`. A third conceptual "entity" (`Address`) is embedded inside `User`, not a top-level collection (`Address.getCollectionName()` returns `null`).

All Mongo-backed entities extend the abstract base class `AModel`:

```java
// com.waker.model.AModel
public abstract class AModel {
    @MongoId @MongoObjectId
    private String key;          // Mongo ObjectId, exposed as a String
    private Date createdAt;      // set in the no-arg constructor at instantiation time
    private Date updatedAt;      // never actually set anywhere in the codebase today
    private String code;         // unused by any current entity
    private String label;        // unused by any current entity
    private boolean enabled = false; // unused by any current entity
    public abstract String getCollectionName();
}
```

Note: `createdAt` is set to "now" in the constructor rather than at persistence time, which means it is reset every time an existing document is deserialized into a new Java object (e.g. on every `find()`) unless carefully overwritten — a latent bug worth fixing during migration rather than carrying forward.

## Collections

### `users` (backed by `User extends AModel`)

| Field | Type | Notes |
|---|---|---|
| `_id` / `key` | ObjectId / String | Primary key (via `AModel`) |
| `firstName` | String | |
| `lastName` | String | |
| `email` | String | Used as the effective unique identifier / login handle; **no unique index is created anywhere in code** — uniqueness is only enforced application-side via `UserService.emailExists()` before insert, which is race-condition-prone |
| `password` | String | PBKDF2WithHmacSHA1 hash, format `iterationCount:saltHex:hashHex` (see `Crypt.createHash`) |
| `country` | String | |
| `address` | `Address` (embedded) | city/address1/address2/address3/zipCode/number |
| `birthDay` | Date | |
| `phone` | String | |
| ~~`stripeCustomerId`~~ | String | **Commented out** in both `User` and `UserDTO` — an abandoned Stripe integration attempt |

### `reminders` (backed by `Reminder extends AModel`)

This is the current stand-in for the product's core "**commitment**" concept.

| Field | Type | Notes |
|---|---|---|
| `_id` / `key` | ObjectId / String | Primary key (via `AModel`) |
| `user` | `User` (embedded, full document) | **Embeds the entire `User` document**, not a reference/foreign key — denormalized by construction (Jongo has no relations/joins) |
| `name` | String | |
| `description` | String | |
| `notifyTime` | Date | When the alarm/reminder should fire |
| `deadline` | Date | When the fulfillment window closes |
| `fulfillmentMethod` | `FulfillmentMethod` (embedded) | `{ name: String, setting: Map<Object,Object> }` — `name` must match a `Fulfillments` enum value |
| `penaltySetting` | `APenalty` (embedded, polymorphic) | Discriminated by a `_class` field (Jackson `@JsonTypeInfo`); one of `GetScolded` / `EmbarrassingText` / `EmbarrassingEmail` |
| `status` | int | `1` = fulfilled, `0` = pending, `-1` = not fulfilled (magic-number tri-state, not an enum) |
| `active` | boolean | Present on the model but **not read or written anywhere** in the current service/app layer logic |

### Embedded/value types (not top-level collections)

- **`Address`**: `city`, `address1`, `address2`, `address3`, `zipCode`, `number`. Extends `AModel` but overrides `getCollectionName()` to return `null` — it is only ever embedded inside `User`, never queried directly.
- **`FulfillmentMethod`**: `name` (String, validated against `Fulfillments` enum) + `setting` (`Map<Object, Object>` — untyped, so any per-fulfillment-type configuration is currently stored as an opaque, unvalidated map).
- **`Email`** (`com.waker.model.Email`): plain POJO (`from`, `to`, `subject`, `body`, `date`) used only as the payload for the `EmbarrassingEmail` penalty and `EmailService`'s console-stub sender — **not** a Mongo entity.

### Polymorphic penalty hierarchy (embedded inside `Reminder.penaltySetting`)

```java
// com.waker.model.penalty.APenalty
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_class")
public abstract class APenalty {
    private String _class;   // must match a value in the Penalties enum, validated via APenalty.validate()
}
```

| Concrete type | `_class` value (`Penalties` enum) | Extra fields | Current runtime behavior |
|---|---|---|---|
| `GetScolded` | `GetScolded` | `scoldingMessage: String` | Prints the message to stdout (`GetScoldedService`) |
| `EmbarrassingText` | `EmbarrassingText` | `text: String` | Prints the text to stdout (`EmbarrassingTextService`) |
| `EmbarrassingEmail` | `EmbarrassingEmail` | `email: Email` | Prints the `Email` fields to stdout via `EmailService.sendEmail()` — **does not actually send an email**, despite the name (real sending only happens via `GmailApiService`, wired to `UserApp`'s registration flow, not to any penalty) |

None of the product-vision penalty types (pay $, social media post, leaderboard, WhatsApp message) exist in code yet — only these 3 placeholder/demo types.

## Relationships

- `Reminder.user` → embeds a full `User` document (no reference/foreign key semantics; Jongo/MongoDB have no native joins). Any change to a user's profile after a reminder was created does **not** propagate to previously-saved reminders.
- `Reminder.penaltySetting` → embeds exactly one polymorphic `APenalty` subtype document per reminder (a reminder has exactly one penalty configured, not a list).
- `Reminder.fulfillmentMethod` → embeds exactly one `FulfillmentMethod` value object (name + untyped settings map).

There are no cross-collection queries, no aggregation pipelines, and no indexes defined anywhere in the codebase beyond MongoDB's automatic `_id` index.

## Migration Strategy (current state)

**None.** There is no schema versioning tool. Any structural change to `User`/`Reminder`/`APenalty` today is a silent, backward-incompatible change applied only at the next write — existing documents in MongoDB are not migrated. This is acceptable only because the system has no real users/data yet.

## Planned Target Schema (see ASSESSMENT_AND_ROADMAP.md for full rationale)

The roadmap calls for replacing this MongoDB/Jongo layer with **PostgreSQL + JSONB**, managed via Flyway or Liquibase migrations, with roughly:

- `users` — normalized relational columns (no embedded address; a separate `addresses` table or JSONB column)
- `commitments` (renamed from `reminders`) — relational columns for `notify_time`/`deadline`/`status`/foreign key to `users`, with `mission_config` and `penalty_config` as typed JSONB columns instead of Jongo's polymorphic Java-class embedding
- `missions` reference/lookup data — to support the QR/location/math/writing/speech mission types from the product vision, each with its own validation rules
- `penalties` reference/lookup data plus a `penalty_events`/`ledger` table for auditability of triggered penalties (especially important once real money or reputational-impact penalties like social posting exist)
- Explicit unique constraint on `users.email` (currently only enforced application-side, racily, via a pre-check)

---

_Generated using the BMAD Method `document-project` workflow_
