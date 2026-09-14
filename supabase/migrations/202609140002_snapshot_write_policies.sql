create policy "Users can create their learning snapshot"
on public.learning_snapshots for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy "Users can update their learning snapshot"
on public.learning_snapshots for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

grant insert, update on public.learning_snapshots to authenticated;
