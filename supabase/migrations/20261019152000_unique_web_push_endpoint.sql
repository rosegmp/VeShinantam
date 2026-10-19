-- A Push API endpoint must never be attached to two accounts at once.
create unique index web_push_subscriptions_endpoint_unique
  on public.web_push_subscriptions(endpoint);
