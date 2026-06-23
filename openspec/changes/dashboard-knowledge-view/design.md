# Design

`KnowledgeView` is a dense operator page with three panels:

- Knowledge bases: current bases plus create form.
- Add document: selected base, document name, text content, and stored chunk count feedback.
- Search test: query, optional selected base, limit, and scored result snippets.

The view uses local component state and the typed `dashboardApi`; global Dashboard store is not expanded until knowledge metrics become part of the overview.

Nested/non-primary routes use `meta.nav = false` so the sidebar only shows stable top-level modules.
