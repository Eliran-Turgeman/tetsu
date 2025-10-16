# Share consistency feature

This package provides an end-to-end flow for rendering the shareable consistency heatmap and exporting it as an image.

## Rendering & Exporting

1. Collect `DailyCount` items for the range the user selects and prepare `PreparedAchievement` entries (icons should already be loaded into `ImageBitmap`).
2. Build a `HeatmapExportRequest` with the user options (range, theme, accent, aspect, etc.).
3. Call `ShareConsistency.render(request, dailyCounts, achievements)` from a background coroutine to generate the `ImageBitmap`.
4. Save the bitmap to cache with `ShareConsistency.exportToCache(context, bitmap)` and obtain the `Uri`.
5. Use `ShareConsistency.share(context, uri)` to present the Android sharesheet.

The renderer works entirely offline and never reaches into persistence layers; callers must provide the prepared data.

## Preview data

See `SharePreview.kt` for a simple set of fake data used in Compose previews.

