# Supabase account sync

The production project is configured in `webApp/src/wasmJsMain/resources/supabase-config.js`. Apply future schema changes as append-only migrations in this directory.

Authentication uses email and password. If email confirmation is enabled, add the deployed site URL and local development URL to the Supabase Auth redirect allow list. The browser stores its refresh token locally. Row Level Security restricts each learning entity and browser push subscription to its authenticated owner.

Android and web use the incremental entity-sync API. The old snapshot API is disabled for client access.

## Closed-app web reminders

`web_push_subscriptions` holds each signed-in browser's Push API endpoint, encryption keys, and time zone. Its RLS policy permits only the owning user to manage a subscription. The `claim_due_web_push_reminders` function is callable only by `service_role`; it counts incomplete tasks due through the browser's local date under active schedules and claims at most one delivery per browser and local date. The `daily-web-push` Edge Function sends those claims. A `pg_cron` job calls it every minute through `pg_net` with a secret held in Vault. The function rejects calls without that secret.

Set `WEB_PUSH_VAPID_PUBLIC_KEY`, `WEB_PUSH_VAPID_PRIVATE_KEY`, and `WEB_PUSH_CRON_SECRET` as Edge Function secrets. The public VAPID key must also match `webApp/src/wasmJsMain/resources/supabase-config.js`. Store the cron secret under the Vault name `veshinantam_web_push_cron_secret`. Rotate the VAPID pair only with a plan to resubscribe existing browsers. Deploy the function with JWT verification disabled because it authenticates the cron-specific secret itself; the subscription table remains protected by RLS.

The service accepts `{ "dryRun": true }` with the cron secret for a health check that does not claim or send notifications. Without a valid secret it returns 401. The browser keeps its existing open-tab reminder fallback for users without an account or Push API support. PDFs selected for the local Mishnah Berurah reader are stored only in browser IndexedDB and are never uploaded as part of account sync.
