-- This deployed baseline combines the original snapshot schema and the entity
-- sync migration. Snapshot sync is retained only for migration/rollback data;
-- later migrations revoke all client access to it.
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

create policy "Users can create their learning snapshot"
on public.learning_snapshots for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy "Users can update their learning snapshot"
on public.learning_snapshots for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

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
grant select, insert, update on public.learning_snapshots to authenticated;

create sequence if not exists public.learning_entity_revision_seq;

revoke all on sequence public.learning_entity_revision_seq from anon, authenticated;

create table if not exists public.learning_entities (
  user_id uuid not null references auth.users(id) on delete cascade,
  entity_type text not null check (entity_type in ('SCHEDULE', 'MATERIAL_UNIT', 'TASK', 'EXCLUSION', 'GOAL', 'PREFERENCES')),
  entity_id text not null check (length(entity_id) between 1 and 500),
  payload jsonb,
  deleted boolean not null default false,
  revision bigint not null check (revision > 0),
  updated_at timestamptz not null default now(),
  primary key (user_id, entity_type, entity_id),
  check ((deleted and payload is null) or (not deleted and payload is not null))
);

create index if not exists learning_entities_user_revision_idx
  on public.learning_entities(user_id, revision);

create table if not exists public.learning_mutations (
  user_id uuid not null references auth.users(id) on delete cascade,
  mutation_id uuid not null,
  entity_type text not null,
  entity_id text not null,
  applied_revision bigint not null,
  created_at timestamptz not null default now(),
  primary key (user_id, mutation_id)
);

alter table public.learning_entities enable row level security;
alter table public.learning_mutations enable row level security;

revoke all on public.learning_entities from anon, authenticated;
revoke all on public.learning_mutations from anon, authenticated;

create policy "Users can read their learning entities"
on public.learning_entities for select
to authenticated
using ((select auth.uid()) = user_id);

create policy "Users can read their mutation receipts"
on public.learning_mutations for select
to authenticated
using ((select auth.uid()) = user_id);

grant select on public.learning_entities to authenticated;
grant select on public.learning_mutations to authenticated;

create or replace function public.apply_learning_mutation(
  p_mutation_id uuid,
  p_entity_type text,
  p_entity_id text,
  p_base_revision bigint,
  p_payload jsonb,
  p_deleted boolean default false
)
returns table (
  revision bigint,
  conflict boolean,
  remote_payload jsonb,
  remote_deleted boolean
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_current public.learning_entities%rowtype;
  v_receipt_revision bigint;
  v_next_revision bigint;
begin
  if v_user_id is null then
    raise exception 'Authentication required';
  end if;
  if p_entity_type not in ('SCHEDULE', 'MATERIAL_UNIT', 'TASK', 'EXCLUSION', 'GOAL', 'PREFERENCES') then
    raise exception 'Invalid entity type';
  end if;
  if p_entity_id is null or length(p_entity_id) not between 1 and 500 then
    raise exception 'Invalid entity ID';
  end if;
  if p_base_revision < 0 then
    raise exception 'Invalid base revision';
  end if;
  if (p_deleted and p_payload is not null) or (not p_deleted and p_payload is null) then
    raise exception 'Invalid mutation payload';
  end if;

  -- A row cannot be locked before its first insert. This transaction-scoped lock
  -- serializes every mutation for one logical entity, including concurrent creates.
  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtextextended(
      v_user_id::text || ':' || p_entity_type || ':' || p_entity_id,
      0
    )
  );

  select m.applied_revision into v_receipt_revision
  from public.learning_mutations m
  where m.user_id = v_user_id and m.mutation_id = p_mutation_id;

  if v_receipt_revision is not null then
    return query select v_receipt_revision, false, null::jsonb, p_deleted;
    return;
  end if;

  select * into v_current
  from public.learning_entities e
  where e.user_id = v_user_id
    and e.entity_type = p_entity_type
    and e.entity_id = p_entity_id
  for update;

  if (v_current.revision is null and p_base_revision <> 0)
    or (v_current.revision is not null and v_current.revision <> p_base_revision) then
    return query select
      coalesce(v_current.revision, 0),
      true,
      v_current.payload,
      coalesce(v_current.deleted, false);
    return;
  end if;

  v_next_revision := nextval('public.learning_entity_revision_seq');
  insert into public.learning_entities(user_id, entity_type, entity_id, payload, deleted, revision, updated_at)
  values (v_user_id, p_entity_type, p_entity_id, p_payload, p_deleted, v_next_revision, now())
  on conflict (user_id, entity_type, entity_id) do update
  set payload = excluded.payload,
      deleted = excluded.deleted,
      revision = excluded.revision,
      updated_at = excluded.updated_at;

  insert into public.learning_mutations(user_id, mutation_id, entity_type, entity_id, applied_revision)
  values (v_user_id, p_mutation_id, p_entity_type, p_entity_id, v_next_revision);

  return query select v_next_revision, false, null::jsonb, p_deleted;
end;
$$;

revoke all on function public.apply_learning_mutation(uuid, text, text, bigint, jsonb, boolean)
  from public, anon, authenticated;
grant execute on function public.apply_learning_mutation(uuid, text, text, bigint, jsonb, boolean) to authenticated;

comment on table public.learning_entities is
  'Canonical per-user records for incremental Android/web synchronization. Deleted records remain as tombstones.';
comment on table public.learning_mutations is
  'Idempotency receipts for canonical sync writes.';
