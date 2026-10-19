create table public.web_push_subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  endpoint text not null,
  p256dh text not null,
  auth_secret text not null,
  time_zone text not null,
  last_sent_local_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, endpoint),
  check (length(endpoint) between 20 and 4096 and endpoint like 'https://%'),
  check (length(p256dh) between 40 and 300),
  check (length(auth_secret) between 12 and 100),
  check (length(time_zone) between 1 and 100)
);

create index web_push_subscriptions_user_idx on public.web_push_subscriptions(user_id);
alter table public.web_push_subscriptions enable row level security;

create function public.validate_web_push_time_zone()
returns trigger language plpgsql set search_path = '' as $$
begin
  if not exists (select 1 from pg_catalog.pg_timezone_names where name = new.time_zone) then
    raise exception 'Invalid time zone';
  end if;
  return new;
end;
$$;

create trigger validate_web_push_time_zone
before insert or update of time_zone on public.web_push_subscriptions
for each row execute function public.validate_web_push_time_zone();

create policy "Users manage their own push subscriptions"
on public.web_push_subscriptions for all to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

revoke all on public.web_push_subscriptions from anon, authenticated;
grant select, insert, update, delete on public.web_push_subscriptions to authenticated;

-- Only the service role can claim deliveries. A claim is stamped before sending,
-- so concurrent cron invocations cannot notify the same browser twice.
create or replace function public.claim_due_web_push_reminders(p_now timestamptz default now())
returns table (
  subscription_id uuid,
  endpoint text,
  p256dh text,
  auth_secret text,
  due_count integer,
  app_language text,
  local_date date
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_subscription public.web_push_subscriptions%rowtype;
  v_preferences jsonb;
  v_local_now timestamp;
  v_due_count integer;
  v_hour integer;
  v_minute integer;
begin
  for v_subscription in
    select s.* from public.web_push_subscriptions s
    where s.last_sent_local_date is distinct from (p_now at time zone s.time_zone)::date
    order by s.id
    limit 500
    for update skip locked
  loop
    select e.payload into v_preferences
      from public.learning_entities e
      where e.user_id = v_subscription.user_id
        and e.entity_type = 'PREFERENCES' and not e.deleted
      order by e.revision desc limit 1;
    if coalesce(v_preferences->>'reminderEnabled', 'false') <> 'true' then continue; end if;
    v_hour := greatest(0, least(23, coalesce((v_preferences->>'reminderHour')::integer, 20)));
    v_minute := greatest(0, least(59, coalesce((v_preferences->>'reminderMinute')::integer, 0)));
    v_local_now := p_now at time zone v_subscription.time_zone;
    if v_local_now::time < make_time(v_hour, v_minute, 0) then continue; end if;

    select count(*)::integer into v_due_count
      from public.learning_entities t
      join public.learning_entities s
        on s.user_id = t.user_id and s.entity_type = 'SCHEDULE'
        and s.entity_id = t.payload->>'scheduleId'
        and not s.deleted and s.payload->>'state' = 'ACTIVE'
      where t.user_id = v_subscription.user_id and t.entity_type = 'TASK'
        and not t.deleted
        and t.payload->>'completedAt' is null
        and t.payload->>'plannedDate' <= v_local_now::date::text;
    if v_due_count = 0 then continue; end if;

    update public.web_push_subscriptions
      set last_sent_local_date = v_local_now::date, updated_at = p_now
      where id = v_subscription.id;
    subscription_id := v_subscription.id;
    endpoint := v_subscription.endpoint;
    p256dh := v_subscription.p256dh;
    auth_secret := v_subscription.auth_secret;
    due_count := v_due_count;
    app_language := coalesce(v_preferences->>'appLanguage', 'en');
    local_date := v_local_now::date;
    return next;
  end loop;
end;
$$;

revoke all on function public.claim_due_web_push_reminders(timestamptz) from public, anon, authenticated;
grant execute on function public.claim_due_web_push_reminders(timestamptz) to service_role;
