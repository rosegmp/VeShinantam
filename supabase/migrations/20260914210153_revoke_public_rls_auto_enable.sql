-- Event-trigger functions are invoked by PostgreSQL, not by API clients.
-- Remove the default PUBLIC execute grant so this SECURITY DEFINER helper
-- cannot be exposed as an RPC to anonymous or authenticated clients.
revoke all on function public.rls_auto_enable() from public, anon, authenticated;
