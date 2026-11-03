# Deploy Guide — Render (backend + DB) + Vercel (frontend)

Vercel cannot run Java or MySQL, so the split is:
- **Render**: Java API + MySQL database (also serves the UI same-origin as backup)
- **Vercel**: static frontend (`web/`) calling the Render API

## Step 0 — Push latest code

```bash
cd "/Users/kartikbhardwaj/IdeaProjects/Java Project"
git add -A && git commit -m "Deploy-ready: CORS, env DB config, Dockerfile" && git push
```

## Step 1 — Render (backend + MySQL, $0)

The API and MySQL run in ONE free web service (the free plan forbids a
separate private-service MySQL). Already deployed via API; to redo from the
dashboard: **New → Blueprint** → select `banking-system` → **Apply**.

1. Wait ~5–10 min for `bank-api` to go live (first boot initializes MySQL).
2. Open the service → copy its URL, e.g. `https://bank-api-xxxx.onrender.com`.
3. You can already use the app at the Render URL (same-origin mode).

Notes:
- **Free tier spins down** after inactivity; the first request takes ~30–60s.
- **Data resets on restart.** Free plan has no disk; restarts, sleep/wake
  cycles, and redeploys start with an empty bank. For persistence, upgrade
  to Starter + disk.

## Step 2 — Vercel (frontend)

1. Edit `web/config.js`, set your Render URL (no trailing slash):
   ```js
   window.BANK_API_BASE = "https://bank-api-xxxx.onrender.com";
   ```
2. Commit and push: `git add web/config.js && git commit -m "Point frontend at Render" && git push`
3. Go to https://vercel.com → **Add New → Project** → import
   `banking-system` → set **Root Directory = `web`** → **Deploy**.
   (CLI alternative: `cd web && npx -y vercel`, then `npx -y vercel --prod`.)
4. Open the Vercel URL → register an account → data lands in Render MySQL.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Vercel page loads but login fails | `config.js` URL wrong / has trailing slash; Render service asleep (wait 60s, retry) |
| CORS error in browser console | Update backend: this repo already sends `Access-Control-Allow-Origin: *`; redeploy `bank-api` |
| Render `bank-api` crash loop | Check logs: usually DB not ready yet (retries 60s) or wrong `DB_*` vars |
| Data gone after a while | Free service restarted (no disk on free plan); re-register — expected |
| `Address already in use` locally | Unrelated to deploy; stop local server or use another port |
