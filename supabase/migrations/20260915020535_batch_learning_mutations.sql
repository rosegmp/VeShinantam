create or replace function public.apply_learning_mutations(p_mutations jsonb)
returns table (
  mutation_id uuid,
  entity_type text,
  entity_id text,
  revision bigint,
  conflict boolean,
  remote_payload jsonb,
  remote_deleted boolean
)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_mutation jsonb;
  v_mutation_id uuid;
  v_entity_type text;
  v_entity_id text;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;
  if p_mutations is null or pg_catalog.jsonb_typeof(p_mutations) <> 'array' then
    raise exception 'Mutations must be a JSON array';
  end if;
  if pg_catalog.jsonb_array_length(p_mutations) < 1
    or pg_catalog.jsonb_array_length(p_mutations) > 100 then
    raise exception 'Mutation batch size must be between 1 and 100';
  end if;

  for v_mutation in
    select value from pg_catalog.jsonb_array_elements(p_mutations)
  loop
    if pg_catalog.jsonb_typeof(v_mutation) <> 'object'
      or not (v_mutation ?& array['mutation_id', 'entity_type', 'entity_id', 'base_revision', 'deleted']) then
      raise exception 'Each mutation must contain mutation_id, entity_type, entity_id, base_revision, and deleted';
    end if;

    v_mutation_id := (v_mutation ->> 'mutation_id')::uuid;
    v_entity_type := v_mutation ->> 'entity_type';
    v_entity_id := v_mutation ->> 'entity_id';

    return query
    select
      v_mutation_id,
      v_entity_type,
      v_entity_id,
      result.revision,
      result.conflict,
      result.remote_payload,
      result.remote_deleted
    from public.apply_learning_mutation(
      v_mutation_id,
      v_entity_type,
      v_entity_id,
      (v_mutation ->> 'base_revision')::bigint,
      v_mutation -> 'payload',
      (v_mutation ->> 'deleted')::boolean
    ) as result;
  end loop;
end;
$$;

revoke all on function public.apply_learning_mutations(jsonb)
  from public, anon, authenticated;
grant execute on function public.apply_learning_mutations(jsonb) to authenticated;

comment on function public.apply_learning_mutations(jsonb) is
  'Applies up to 100 idempotent per-user learning mutations in one API round trip.';
