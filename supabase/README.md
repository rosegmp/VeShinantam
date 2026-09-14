# Supabase account sync

The production project is configured in `webApp/src/wasmJsMain/resources/supabase-config.js`. Apply future schema changes as append-only migrations in this directory.

Authentication uses email magic links. Add the deployed site URL and local development URL to the Supabase Auth redirect allow list. The browser stores its refresh token locally, while Row Level Security restricts each snapshot to its authenticated owner.

This first synchronization slice uses an explicitly resolved, revision-checked snapshot. When two devices have diverged, neither copy is overwritten until the user chooses the cloud or device copy.
