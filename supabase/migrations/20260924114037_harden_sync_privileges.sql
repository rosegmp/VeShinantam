-- The entity mutation function only writes rows owned by auth.uid(). RLS now
-- enforces the same invariant while SECURITY INVOKER removes the exposed
-- SECURITY DEFINER surface reported by the Supabase security advisor.
create policy "Users can create their learning entities"
on public.learning_entities for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy "Users can update their learning entities"
on public.learning_entities for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "Users can create their mutation receipts"
on public.learning_mutations for insert
to authenticated
with check ((select auth.uid()) = user_id);

grant insert, update on public.learning_entities to authenticated;
grant insert on public.learning_mutations to authenticated;
grant usage on sequence public.learning_entity_revision_seq to authenticated;

alter function public.apply_learning_mutation(uuid, text, text, bigint, jsonb, boolean)
  security invoker;

-- Legacy snapshot sync is retired. Remove every remaining table privilege,
-- including non-DML defaults such as TRIGGER, REFERENCES, and TRUNCATE.
revoke all on table public.learning_snapshots from anon, authenticated;
