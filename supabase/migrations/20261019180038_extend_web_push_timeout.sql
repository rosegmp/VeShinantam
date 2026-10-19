-- Allow a cold Edge Function and the push provider time to finish before
-- pg_net abandons the response. Keep the existing named cron job.
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
      body := '{}'::jsonb,
      timeout_milliseconds := 30000
    );
  $web_push$
);
