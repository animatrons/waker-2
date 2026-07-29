/**
 * Cross-cutting infrastructure shared by feature modules: security filter chain, ProblemDetail
 * advice, CORS properties, and injectable {@link java.time.Clock}.
 *
 * <p>HTTP business APIs live under {@code /api/v1/...} (AD-12). Actuator stays under {@code
 * /actuator/**}. Controllers return {@code ResponseEntity<T>} — never a legacy ResponseDTO
 * envelope.
 */
package com.waker.common;
