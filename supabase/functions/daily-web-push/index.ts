import { createClient } from 'npm:@supabase/supabase-js@2.57.4';
import { sendNotification } from 'npm:web-push@3.6.7';

type Claim = {
  subscription_id: string;
  endpoint: string;
  p256dh: string;
  auth_secret: string;
  due_count: number;
  app_language: string;
  local_date: string;
};

const required = (name: string): string => {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`${name} is not configured`);
  return value;
};

const supabase = createClient(required('SUPABASE_URL'), required('SUPABASE_SERVICE_ROLE_KEY'), {
  auth: { persistSession: false },
});

Deno.serve(async request => {
  if (request.method !== 'POST') return new Response('Method not allowed', { status: 405 });
  if (request.headers.get('x-veshinantam-cron-secret') !== required('WEB_PUSH_CRON_SECRET')) {
    return new Response('Unauthorized', { status: 401 });
  }
  const input = await request.json().catch(() => ({}));
  if (input.dryRun === true) return Response.json({ ready: true, dryRun: true });

  const { data, error } = await supabase.rpc('claim_due_web_push_reminders');
  if (error) {
    console.error('Could not claim web reminders', error);
    return new Response('Reminder query failed', { status: 500 });
  }

  const claims = (data || []) as Claim[];
  let sent = 0;
  let failed = 0;
  for (let offset = 0; offset < claims.length; offset += 8) {
    await Promise.all(claims.slice(offset, offset + 8).map(async claim => {
      const hebrew = claim.app_language === 'he';
      const payload = JSON.stringify({
        title: hebrew ? 'הגיע זמן הלימוד' : 'Time for today’s learning',
        body: hebrew
          ? `${claim.due_count} משימות לימוד וחזרה ממתינות לך.`
          : `${claim.due_count} learning and chazarah ${claim.due_count === 1 ? 'task is' : 'tasks are'} waiting.`,
        url: './?view=today',
      });
      try {
        await sendNotification(
          { endpoint: claim.endpoint, keys: { p256dh: claim.p256dh, auth: claim.auth_secret } },
          payload,
          {
            TTL: 86400,
            vapidDetails: {
              subject: 'https://rosegmp.github.io/VeShinantam/',
              publicKey: required('WEB_PUSH_VAPID_PUBLIC_KEY'),
              privateKey: required('WEB_PUSH_VAPID_PRIVATE_KEY'),
            },
          },
        );
        sent += 1;
      } catch (cause) {
        failed += 1;
        const status = Number((cause as { statusCode?: number }).statusCode || 0);
        if (status === 404 || status === 410) {
          await supabase.from('web_push_subscriptions').delete().eq('id', claim.subscription_id);
        } else {
          // Allow a later cron run to retry transient delivery failures.
          await supabase.from('web_push_subscriptions')
            .update({ last_sent_local_date: null })
            .eq('id', claim.subscription_id)
            .eq('last_sent_local_date', claim.local_date);
          console.error('Web reminder delivery failed', { status, cause });
        }
      }
    }));
  }
  return Response.json({ claimed: claims.length, sent, failed });
});
