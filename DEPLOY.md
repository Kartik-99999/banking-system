# Deploy Guide — Render (backend + DB) + Vercel (frontend)

Vercel cannot run Java or MySQL, so the split is:
- **Render**: Java API + MySQL database (also serves the UI same-origin as backup)
- **Vercel**: static frontend (`web/`) calling the Render API

## Step 0 — Push latest code

```bash
cd "/Users/kartikbhardwaj/IdeaProjects/Java Project"
git add -A && git commit -m "Deploy-ready: CORS, env DB config, Dockerfile" && git push
```

## Step 1 — Render (backend + MySQL)

1. Go to https://dashboard.render.com → **New → Blueprint** →
   select the `banking-system` repo (it reads `render.yaml`) → **Apply**.
2. Wait ~5–10 min for `bank-api` and `bank-mysql` to go live.
   (First boot retries the DB connection for ~60s — that is normal.)
3. Open the `bank-api` service → copy its URL, e.g.
   `https://bank-api-xxxx.onrender.com`. Test it:
   - `GET /` shows the UI, `GET /api/history` without login → 401.
4. You can already use the app at the Render URL (same-origin mode).

Notes:
- **Free tier spins down** after inactivity; the first request takes ~30–60s.
- **Free MySQL loses data on restart.** For persistence, upgrade `bank-mysql`
  to Starter, add a 1 GB disk at `/var/lib/mysql` (see commented block in
  `render.yaml`), redeploy.
- If deploy fails on the DB service, check its logs for the MySQL
  password/host; env vars are auto-wired from the Blueprint.

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
| Data gone after a while | Free MySQL restarted without a disk; add disk (paid) or re-register |
| `Address already in use` locally | Unrelated to deploy; stop local server or use another port |
