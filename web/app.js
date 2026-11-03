const state = { token: null, adminToken: null, name: "", accountNo: "", type: "", mode: "deposit" };
const API = (window.BANK_API_BASE || "").replace(/\/$/, ""); // "" locally, Render URL on Vercel

const $ = (id) => document.getElementById(id);
const views = ["landingView", "loginView", "registerView", "adminLoginView", "dashView", "adminView", "detailsView"];
const LOGGED_OUT = ["landingView", "loginView", "registerView", "adminLoginView"];

function show(id) {
  views.forEach((v) => $(v).classList.toggle("hidden", v !== id));
  $("navBar").classList.toggle("hidden", !LOGGED_OUT.includes(id));
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

// ---------- Money math (display previews; server is authoritative) ----------

const fmt = (n) => Number(n).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fdRateFor = (m) => (m > 24 ? 7.5 : m >= 12 ? 7.0 : 6.5);
const fdMaturity = (p, m) => p * (1 + fdRateFor(m) / 100 * m / 12);
const LOAN_RATES = { HOME: 8.5, EDUCATION: 9.0, PERSONAL: 12.0 };

function emiFor(p, annual, n) {
  const r = annual / 1200;
  const pow = Math.pow(1 + r, n);
  return p * r * pow / (pow - 1);
}

function updateCalcFd() {
  const p = parseFloat($("calcFdP").value);
  const m = parseInt($("calcFdT").value, 10);
  $("calcFdOut").innerHTML = "";
  if (!(p > 0)) return;
  const el = document.createElement("div");
  el.innerHTML = "";
  const strong = document.createElement("strong");
  strong.textContent = "Rs." + fmt(fdMaturity(p, m));
  el.append("Matures to ", strong, " @ " + fdRateFor(m).toFixed(2) + "%");
  $("calcFdOut").appendChild(el);
}

function updateCalcEmi() {
  const p = parseFloat($("calcEmiP").value);
  const n = parseInt($("calcEmiT").value, 10);
  $("calcEmiOut").innerHTML = "";
  if (!(p > 0) || !(n > 0)) return;
  const emi = emiFor(p, LOAN_RATES[$("calcEmiType").value], n);
  const el = document.createElement("div");
  const strong = document.createElement("strong");
  strong.textContent = "Rs." + fmt(emi) + "/month";
  el.append("EMI ", strong, " · total Rs." + fmt(emi * n));
  $("calcEmiOut").appendChild(el);
}

["calcFdP", "calcFdT"].forEach((id) => $(id).addEventListener("input", updateCalcFd));
["calcEmiType", "calcEmiP", "calcEmiT"].forEach((id) => $(id).addEventListener("input", updateCalcEmi));
updateCalcFd();
updateCalcEmi();

// ---------- Navigation ----------

$("navLogin").addEventListener("click", () => show("loginView"));
$("navRegister").addEventListener("click", () => show("registerView"));
$("ctaLogin").addEventListener("click", () => show("loginView"));
$("ctaRegister").addEventListener("click", () => show("registerView"));

function showTab(name) {
  document.querySelectorAll("[data-tab]").forEach((b) =>
    b.classList.toggle("active", b.dataset.tab === name));
  ["overview", "fd", "loan", "card"].forEach((t) =>
    $("tab-" + t).classList.toggle("hidden", t !== name));
  $("cardReveal").classList.add("hidden");
  if (name === "fd") loadFDs();
  if (name === "loan") loadLoans();
  if (name === "card") loadCard();
}
document.querySelectorAll("[data-tab]").forEach((b) =>
  b.addEventListener("click", () => showTab(b.dataset.tab)));

// ---------- Auth ----------

$("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const d = await api("/api/login", "POST", { accountNo: $("loginAc").value.trim(), pin: $("loginPin").value }, null);
    setUser(d);
    show("dashView");
    showTab("overview");
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
  show("landingView");
}
$("topLogout").addEventListener("click", logout);
$("adminLogout").addEventListener("click", logout);

$("goRegister").addEventListener("click", (e) => { e.preventDefault(); show("registerView"); });
$("goAdmin").addEventListener("click", (e) => { e.preventDefault(); show("adminLoginView"); });
$("backToLogin").addEventListener("click", (e) => { e.preventDefault(); show("loginView"); });
$("backToLogin2").addEventListener("click", (e) => { e.preventDefault(); show("loginView"); });

// ---------- Overview ----------

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

const CREDIT_TYPES = ["DEPOSIT", "TRANSFER_IN", "INTEREST", "FD_CLOSE", "LOAN_DISBURSE"];

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
      const credit = CREDIT_TYPES.includes(t.type);
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

// ---------- Fixed deposits ----------

function updateFdPreview() {
  const p = parseFloat($("fdP").value);
  const m = parseInt($("fdT").value, 10);
  if (!(p > 0)) { $("fdPreview").textContent = ""; return; }
  const mat = new Date();
  mat.setMonth(mat.getMonth() + m);
  $("fdPreview").textContent = "Matures to Rs." + fmt(fdMaturity(p, m)) + " on "
    + mat.toLocaleDateString("en-IN") + " @ " + fdRateFor(m).toFixed(2) + "%";
}
$("fdP").addEventListener("input", updateFdPreview);
$("fdT").addEventListener("input", updateFdPreview);

$("fdForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const d = await api("/api/fd/create", "POST", { principal: $("fdP").value.trim(), months: $("fdT").value });
    $("fdP").value = "";
    $("fdPreview").textContent = "";
    await loadFDs();
    await refreshAll();
    toast("FD opened. Matures to Rs." + d.fd.value);
  } catch (err) { toast(err.message, false); }
});

async function loadFDs() {
  try {
    const d = await api("/api/fd/list");
    const body = $("fdBody");
    body.innerHTML = "";
    if (d.fds.length === 0) {
      body.innerHTML = '<tr><td colspan="8" class="muted">No fixed deposits yet.</td></tr>';
      return;
    }
    d.fds.forEach((f) => {
      const row = body.insertRow();
      row.insertCell().textContent = f.fdId;
      const p = row.insertCell();
      p.textContent = f.principal;
      p.className = "num";
      row.insertCell().textContent = f.rate + "%";
      row.insertCell().textContent = f.months + " mo";
      row.insertCell().textContent = f.maturity;
      const v = row.insertCell();
      v.textContent = f.value;
      v.className = "num";
      const s = row.insertCell();
      const badge = document.createElement("span");
      const open = f.status === "OPEN";
      badge.className = "badge " + (open ? "in" : "mut");
      badge.textContent = f.status;
      s.appendChild(badge);
      const act = row.insertCell();
      if (open) {
        const btn = document.createElement("button");
        btn.className = "secondary";
        btn.textContent = "Close";
        btn.dataset.close = f.fdId;
        act.appendChild(btn);
      }
    });
  } catch (err) { toast(err.message, false); }
}

$("fdBody").addEventListener("click", async (e) => {
  const fdId = e.target.dataset ? e.target.dataset.close : null;
  if (!fdId) return;
  try {
    const d = await api("/api/fd/close", "POST", { fdId });
    await loadFDs();
    await refreshAll();
    toast("FD closed. Rs." + d.credited + " credited.");
  } catch (err) { toast(err.message, false); }
});

// ---------- Loans ----------

function updateLoanPreview() {
  const p = parseFloat($("loanP").value);
  const n = parseInt($("loanT").value, 10);
  if (!(p > 0) || !(n > 0)) { $("loanPreview").textContent = ""; return; }
  const emi = emiFor(p, LOAN_RATES[$("loanType").value], n);
  $("loanPreview").textContent = "EMI Rs." + fmt(emi) + "/month · total payable Rs." + fmt(emi * n);
}
["loanType", "loanP", "loanT"].forEach((id) => $(id).addEventListener("input", updateLoanPreview));

$("loanForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const d = await api("/api/loan/apply", "POST", {
      loanType: $("loanType").value, principal: $("loanP").value.trim(), months: $("loanT").value.trim(),
    });
    $("loanP").value = "";
    $("loanT").value = "";
    $("loanPreview").textContent = "";
    await loadLoans();
    await refreshAll();
    toast("Loan disbursed. EMI Rs." + d.loan.emi);
  } catch (err) { toast(err.message, false); }
});

async function loadLoans() {
  try {
    const d = await api("/api/loan/list");
    const body = $("loanBody");
    body.innerHTML = "";
    if (d.loans.length === 0) {
      body.innerHTML = '<tr><td colspan="7" class="muted">No loans yet.</td></tr>';
      return;
    }
    d.loans.forEach((l) => {
      const row = body.insertRow();
      row.insertCell().textContent = l.loanId;
      row.insertCell().textContent = l.type;
      const p = row.insertCell();
      p.textContent = l.principal;
      p.className = "num";
      const emi = row.insertCell();
      emi.textContent = l.emi;
      emi.className = "num";
      const o = row.insertCell();
      o.textContent = l.outstanding;
      o.className = "num";
      const s = row.insertCell();
      const badge = document.createElement("span");
      const active = l.status === "ACTIVE";
      badge.className = "badge " + (active ? "out" : "mut");
      badge.textContent = l.status;
      s.appendChild(badge);
      const act = row.insertCell();
      if (active) {
        const wrap = document.createElement("div");
        wrap.className = "repay-cell";
        const input = document.createElement("input");
        input.placeholder = "Amount";
        input.dataset.amt = l.loanId;
        const btn = document.createElement("button");
        btn.className = "secondary";
        btn.textContent = "Pay";
        btn.dataset.pay = l.loanId;
        wrap.appendChild(input);
        wrap.appendChild(btn);
        act.appendChild(wrap);
      }
    });
  } catch (err) { toast(err.message, false); }
}

$("loanBody").addEventListener("click", async (e) => {
  const loanId = e.target.dataset ? e.target.dataset.pay : null;
  if (!loanId) return;
  const input = e.target.closest("tr").querySelector('input[data-amt="' + loanId + '"]');
  try {
    const d = await api("/api/loan/repay", "POST", { loanId, amount: input.value.trim() });
    await loadLoans();
    await refreshAll();
    toast(d.outstanding === "0.00" ? "Loan fully repaid. Closed!" : "Paid. Outstanding Rs." + d.outstanding);
  } catch (err) { toast(err.message, false); }
});

// ---------- Debit card ----------

async function loadCard() {
  try {
    const d = await api("/api/card");
    const has = d.card !== null && d.card !== undefined;
    $("cardEmpty").classList.toggle("hidden", has);
    $("cardHave").classList.toggle("hidden", !has);
    if (!has) return;
    $("cardNum").textContent = d.card.masked;
    $("cardExp").textContent = d.card.expiry;
    $("cardHolder").textContent = state.name;
    $("cardState").textContent = d.card.active ? "ACTIVE" : "BLOCKED";
    $("debitCard").classList.toggle("blocked", !d.card.active);
    $("cardToggle").textContent = d.card.active ? "Block" : "Unblock";
    $("cardToggle").dataset.active = d.card.active ? "1" : "0";
  } catch (err) { toast(err.message, false); }
}

$("cardIssue").addEventListener("click", async () => {
  try {
    const d = await api("/api/card/issue", "POST", {});
    $("revNum").textContent = d.grouped;
    $("revExp").textContent = d.expiry;
    $("revCvv").textContent = d.cvv;
    $("cardReveal").classList.remove("hidden");
    await loadCard();
    toast("Card issued.");
  } catch (err) { toast(err.message, false); }
});

$("cardToggle").addEventListener("click", async () => {
  try {
    const active = $("cardToggle").dataset.active === "1";
    await api(active ? "/api/card/block" : "/api/card/unblock", "POST", {});
    await loadCard();
    toast(active ? "Card blocked." : "Card unblocked.");
  } catch (err) { toast(err.message, false); }
});

// ---------- Details + admin ----------

$("detailsBtn").addEventListener("click", async () => {
  try {
    fillDetails(await api("/api/account"));
    $("detailsBack").textContent = "Back to Dashboard";
    show("detailsView");
  } catch (err) { toast(err.message, false); }
});
$("detailsBack").addEventListener("click", async () => { await refreshAll(); show("dashView"); });
$("detailsPrint").addEventListener("click", () => window.print());

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
