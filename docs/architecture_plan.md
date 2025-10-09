# Workout Logger Architecture Plan

## Modules
- `app`: Android app module hosting UI, DI setup, WorkManager, and navigation. Depends on domain and data modules.
- `data`: Room database, repositories, data mappers, and WorkManager helpers.
- `domain`: Use cases, models shared with presentation; platform-free.

## Layers & Responsibilities
- **Data**
  - Entities mirror Room schema from prompt (`WorkoutTemplate`, `TemplateItem`, `WorkoutSession`, `SessionExercise`, `SessionSetLog`, `WorkoutSchedule`).
  - DAO interfaces with suspend functions and Flow streams for live updates.
  - Repository implementations provide abstraction for use cases.
- **Domain**
  - Models mirror data but trimmed for app logic.
  - Use cases cover template CRUD, starting/finishing sessions, logging sets, fetching previous performance, schedule management, and heatmap calculation.
- **Presentation**
  - MVVM with `ViewModel`s per feature (Templates, ActiveSession, Heatmap, Schedule, Settings).
  - Jetpack Compose screens for primary flows.
  - Navigation graph handles main destinations (Dashboard, Templates, Session, Heatmap, Settings).

## Background Work & Notifications
- WorkManager enqueues daily schedule checks per template.
- AlarmManager fallback triggers exact alarms when necessary (API < 31).
- Boot & timezone receivers reschedule WorkManager tasks.

## Testing
- Unit tests in `domain` for reps range parsing, previous performance lookup, heatmap computation, and schedule calculations.
- Basic UI test in `app` for session flow using Compose testing APIs.

## Additional Notes
- Use Kotlin DSL for build scripts.
- Hilt for dependency injection across modules.
- Compose Material3 for UI components.
- KDoc on public functions for clarity.
