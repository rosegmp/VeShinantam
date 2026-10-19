create extension if not exists pg_cron;
create extension if not exists pg_net with schema extensions;

select cron.schedule(
  'veshinantam-daily-web-push',
  '* * * * *',
  $web_push$
    select net.http_post(
      url := 'https://tyzembsyzzjrdhmmmfln.supabase.co/functions/v1/daily-web-push',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'apikey', 'sb_publishable_VpyDD3COjLuAKkNmaPaBgQ_sKGiIGXe',
        'x-veshinantam-cron-secret',
          (select decrypted_secret from vault.decrypted_secrets
           where name = 'veshinantam_web_push_cron_secret')
      ),
      body := '{}'::jsonb
    );
  $web_push$
);
