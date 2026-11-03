const state = { token: null, adminToken: null, name: "", accountNo: "", type: "", mode: "deposit" };
const API = (window.BANK_API_BASE || "").replace(/\/$/, ""); // "" locally, Render URL on Vercel

const $ = (id) => document.getElementById(id);
const views = ["loginView", "registerView", "adminLoginView", "dashView", "adminView", "detailsView"];

function show(id) {
  views.forEach((v) => $(v).classList.toggle("hidden", v !== id));
  $("userBar").classList.toggle("hidden", id !== "dashView" && id !== "detailsView");
}

function toast(msg, ok = true) {
  const t = $("toast");
  $("toastMsg").textContent = msg;
  t.className = ok ? "show ok" : "show err";
}
$("toast").addEventListener("click", () => { $("toast").className = "hidden"; });

async function api(path, method = "GET", body = null, token = state.token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["X-Token"] = token;
  const res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : null });
  const data = await res.json().catch(() => ({}));
  if (!res.ok || data.ok === false) throw new Error(data.error || ("Request failed (" + res.status + ")"));
  return data;
}

function setUser(d) {
  state.token = d.token;
  state.name = d.name;
  state.accountNo = d.accountNo;
  state.type = d.type;
  $("dashName").textContent = d.name;
  $("dashAc").textContent = d.accountNo;
  $("dashType").textContent = d.type;
  $("userLabel").textContent = d.name + " (A/c " + d.accountNo + ")";
  $("interestBtn").style.display = d.type === "SAVINGS" ? "" : "none";
}

function fillDetails(d) {
  $("detBank").textContent = d.bank;
  $("detBranch").textContent = d.branch;
  $("detAcNo").textContent = "A/c " + d.accountNo;
  $("detCustId").textContent = d.customerId;
  $("detName").textContent = d.name;
  $("detPhone").textContent = d.phone;
  $("detEmail").textContent = d.email;
  $("detType").textContent = d.type;
  $("detBalance").textContent = "Rs. " + d.balance;
  $("detOpened").textContent = d.opened;
}

// ---------- Auth ----------

$("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const d = await api("/api/login", "POST", { accountNo: $("loginAc").value.trim(), pin: $("loginPin").value }, null);
    setUser(d);
    show("dashView");
    await refreshAll();
    toast("Welcome, " + d.name + "!");
  } catch (err) { toast(err.message, false); }
});

$("registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const d = await api("/api/register", "POST", {
      fullName: $("regName").value.trim(), email: $("regEmail").value.trim(),
      phone: $("regPhone").value.trim(), type: $("regType").value,
      pin: $("regPin").value, deposit: $("regDeposit").value.trim(),
    }, null);
    setUser(d);
    fillDetails(d);
    $("registerForm").reset();
    $("detailsBack").textContent = "Go to Dashboard";
    show("detailsView");
  } catch (err) { toast(err.message, false); }
});

$("adminForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const d = await api("/api/admin/login", "POST", { user: $("adminUser").value, pass: $("adminPass").value }, null);
    state.adminToken = d.token;
    show("adminView");
    await loadAdmin();
  } catch (err) { toast(err.message, false); }
});

function logout() {
  state.token = null;
  state.adminToken = null;
  $("loginPin").value = "";
  show("loginView");
}
$("topLogout").addEventListener("click", logout);
$("adminLogout").addEventListener("click", logout);

$("goRegister").addEventListener("click", (e) => { e.preventDefault(); show("registerView"); });
$("goAdmin").addEventListener("click", (e) => { e.preventDefault(); show("adminLoginView"); });
$("backToLogin").addEventListener("click", (e) => { e.preventDefault(); show("loginView"); });
$("detailsBtn").addEventListener("click", async () => {
  try {
    fillDetails(await api("/api/account"));
    $("detailsBack").textContent = "Back to Dashboard";
    show("detailsView");
  } catch (err) { toast(err.message, false); }
});
$("detailsBack").addEventListener("click", async () => { await refreshAll(); show("dashView"); });
$("detailsPrint").addEventListener("click", () => window.print());
$("backToLogin2").addEventListener("click", (e) => { e.preventDefault(); show("loginView"); });

// ---------- Dashboard ----------

document.querySelectorAll("[data-act]").forEach((btn) => {
  btn.addEventListener("click", () => {
    const act = btn.dataset.act;
    $("moneyForm").classList.toggle("hidden", act === "pin");
    $("pinForm").classList.toggle("hidden", act !== "pin");
    $("moneyTo").classList.toggle("hidden", act !== "transfer");
    if (act !== "pin") {
      state.mode = act;
      const label = act[0].toUpperCase() + act.slice(1);
      $("moneyTitle").textContent = label;
      $("moneySubmit").textContent = label;
    }
  });
});

$("moneyForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const amount = $("moneyAmount").value.trim();
    let d;
    if (state.mode === "deposit") d = await api("/api/deposit", "POST", { amount });
    else if (state.mode === "withdraw") d = await api("/api/withdraw", "POST", { amount });
    else d = await api("/api/transfer", "POST", { to: $("moneyTo").value.trim(), amount });
    $("moneyAmount").value = "";
    $("moneyTo").value = "";
    await refreshAll();
    toast(state.mode[0].toUpperCase() + state.mode.slice(1) + " successful. Balance Rs." + d.balance);
  } catch (err) { toast(err.message, false); }
});

$("pinForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    await api("/api/change-pin", "POST", { oldPin: $("oldPin").value, newPin: $("newPin").value });
    $("oldPin").value = "";
    $("newPin").value = "";
    toast("PIN changed successfully.");
  } catch (err) { toast(err.message, false); }
});

$("interestBtn").addEventListener("click", async () => {
  try {
    const d = await api("/api/interest", "POST", {});
    await refreshAll();
    toast("Interest credited. Balance Rs." + d.balance);
  } catch (err) { toast(err.message, false); }
});

$("refreshBtn").addEventListener("click", refreshAll);

async function refreshAll() {
  try {
    const b = await api("/api/balance");
    $("dashBalance").textContent = b.balance;
    const h = await api("/api/history?limit=50");
    const body = $("txnBody");
    body.innerHTML = "";
    if (h.transactions.length === 0) {
      body.innerHTML = '<tr><td colspan="5" class="muted">No transactions yet. Make a deposit to begin.</td></tr>';
      return;
    }
    h.transactions.forEach((t) => {
      const credit = t.type === "DEPOSIT" || t.type === "TRANSFER_IN" || t.type === "INTEREST";
      const row = body.insertRow();
      row.insertCell().textContent = t.date;
      const typeCell = row.insertCell();
      const badge = document.createElement("span");
      badge.className = "badge " + (credit ? "in" : "out");
      badge.textContent = t.type;
      typeCell.appendChild(badge);
      const amt = row.insertCell();
      amt.textContent = (credit ? "+" : "-") + t.amount;
      amt.className = "num " + (credit ? "in" : "out");
      const bal = row.insertCell();
      bal.textContent = t.balanceAfter;
      bal.className = "num";
      row.insertCell().textContent = t.description || "";
    });
  } catch (err) { toast(err.message, false); }
}

async function loadAdmin() {
  try {
    const d = await api("/api/admin/accounts", "GET", null, state.adminToken);
    const body = $("adminBody");
    body.innerHTML = "";
    d.accounts.forEach((a) => {
      const row = body.insertRow();
      row.insertCell().textContent = a.accountNo;
      row.insertCell().textContent = a.holder;
      row.insertCell().textContent = a.type;
      const bal = row.insertCell();
      bal.textContent = a.balance;
      bal.className = "num";
    });
  } catch (err) { toast(err.message, false); }
}
