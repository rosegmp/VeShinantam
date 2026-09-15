-- Snapshot sync was replaced by per-entity sync in Android 0.1.6 and the web
-- app. Retire the old endpoint so stale clients cannot lock the single
-- snapshot row and exhaust PostgREST's connection pool.
revoke all on function public.sync_learning_snapshot(bigint, jsonb)
  from public, anon, authenticated;

revoke select, insert, update on table public.learning_snapshots
  from anon, authenticated;
