The phases in the roadmap in [Assessment & Roadmap](./ASSESSMENT_AND_ROADMAP.md) are **milestones**, not epics. They're too coarse-grained to track/implement as a single epic each; every phase should break into several epics, and each epic into stories (the standard BMAD unit: `Epic N` → `Story N.1, N.2, ...`, tracked via `sprint-status.yaml`).

For example:

- **Phase 1 (Backend foundation rewrite)** → probably 3-4 epics: "Spring Boot + Postgres skeleton," "Spring Security/JWT auth rewrite," "Docker/compose + CI," "Test infrastructure (Testcontainers)."
- **Phase 2 (Core commitment loop)** → separate epics for "Commitment entity + CRUD," "Mission abstraction + 3 missions," "Penalty abstraction + 2 penalties," "End-to-end create→fulfill/miss loop."
- **Phase 4 (Android core UX)** → separate epics per screen area (onboarding, commitment wizard, mission-fulfillment screens, history), not one giant "build the app" epic.

Rule of thumb: if a phase item can't be described as a handful of related user-facing/technical stories that a dev could pick up one at a time (per the epic→story→backlog flow), it's a phase/milestone, not an epic — split it further.

If you want, once you're back in Agent mode I can run the epic-generation step (from the PRD/architecture, if those exist yet) or draft an `epics.md` breaking each phase down this way.
