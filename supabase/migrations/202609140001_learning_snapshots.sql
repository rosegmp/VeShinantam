create table if not exists public.learning_snapshots (
  user_id uuid primary key references auth.users(id) on delete cascade,
  payload jsonb not null,
  revision bigint not null default 1 check (revision > 0),
  updated_at timestamptz not null default now()
);

alter table public.learning_snapshots enable row level security;

create policy "Users can read their learning snapshot"
on public.learning_snapshots for select
to authenticated
using ((select auth.uid()) = user_id);

create or replace function public.sync_learning_snapshot(
  expected_revision bigint,
  new_payload jsonb
)
returns bigint
language plpgsql
security invoker
set search_path = ''
as $$
declare
  current_revision bigint;
  next_revision bigint;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select revision into current_revision
  from public.learning_snapshots
  where user_id = auth.uid()
  for update;

  if current_revision is null then
    if expected_revision <> 0 then
      raise exception 'Sync conflict' using errcode = '40001';
    end if;
    insert into public.learning_snapshots(user_id, payload, revision)
    values (auth.uid(), new_payload, 1);
    return 1;
  end if;

  if current_revision <> expected_revision then
    raise exception 'Sync conflict' using errcode = '40001';
  end if;

  next_revision := current_revision + 1;
  update public.learning_snapshots
  set payload = new_payload, revision = next_revision, updated_at = now()
  where user_id = auth.uid();
  return next_revision;
end;
$$;

revoke all on function public.sync_learning_snapshot(bigint, jsonb) from public;
grant execute on function public.sync_learning_snapshot(bigint, jsonb) to authenticated;
grant select on public.learning_snapshots to authenticated;
