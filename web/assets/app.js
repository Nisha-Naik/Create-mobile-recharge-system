const form = document.getElementById('rechargeForm');
const mobileNumber = document.getElementById('mobileNumber');
const customerName = document.getElementById('customerName');
const operatorSelect = document.getElementById('operator');
const planSelect = document.getElementById('rechargePlan');
const rechargeAmount = document.getElementById('rechargeAmount');
const paymentMethod = document.getElementById('paymentMethod');
const currentBalance = document.getElementById('currentBalance');
const formError = document.getElementById('formError');
const resetFormButton = document.getElementById('resetForm');
const planList = document.getElementById('planList');
const planOperatorTitle = document.getElementById('planOperatorTitle');
const receiptCard = document.getElementById('receiptCard');
const resultSection = document.getElementById('result');
const loadingOverlay = document.getElementById('loadingOverlay');
const successModal = document.getElementById('successModal');
const closeModal = document.getElementById('closeModal');
const successMessage = document.getElementById('successMessage');
const historyList = document.getElementById('historyList');
const emptyHistory = document.getElementById('emptyHistory');
const historySearch = document.getElementById('historySearch');
const operatorFilter = document.getElementById('operatorFilter');
const themeToggle = document.getElementById('themeToggle');
const themeIcon = document.getElementById('themeIcon');
const statTransactions = document.getElementById('statTransactions');
const statSpent = document.getElementById('statSpent');
const statOperator = document.getElementById('statOperator');
const previewOperator = document.getElementById('previewOperator');
const previewBalance = document.getElementById('previewBalance');
const previewTxn = document.getElementById('previewTxn');

const state = {
    plans: [],
    history: []
};

const currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
});

const icons = {
    moon: '<svg viewBox="0 0 24 24"><path d="M21 14.6A8.5 8.5 0 0 1 9.4 3a7 7 0 1 0 11.6 11.6Z"/></svg>',
    sun: '<svg viewBox="0 0 24 24"><path d="M12 5a7 7 0 1 1 0 14 7 7 0 0 1 0-14Zm0-4h2v3h-2V1Zm0 19h2v3h-2v-3ZM1 11h3v2H1v-2Zm19 0h3v2h-3v-2ZM4.2 2.8l2.1 2.1-1.4 1.4-2.1-2.1 1.4-1.4Zm14.9 14.9 2.1 2.1-1.4 1.4-2.1-2.1 1.4-1.4Zm.7-14.9 1.4 1.4-2.1 2.1-1.4-1.4 2.1-2.1ZM4.9 17.7l1.4 1.4-2.1 2.1-1.4-1.4 2.1-2.1Z"/></svg>',
    check: '<svg viewBox="0 0 24 24"><path d="M9.4 16.6 4.8 12l1.4-1.4 3.2 3.2 8.4-8.4L19.2 7l-9.8 9.6Z"/></svg>'
};

document.addEventListener('DOMContentLoaded', () => {
    setupTheme();
    loadPlans(operatorSelect.value);
    loadHistory();
});

operatorSelect.addEventListener('change', () => {
    previewOperator.textContent = operatorSelect.value;
    loadPlans(operatorSelect.value);
});

planSelect.addEventListener('change', () => {
    const selected = planSelect.options[planSelect.selectedIndex];
    if (selected && selected.dataset.amount) {
        rechargeAmount.value = Number(selected.dataset.amount).toFixed(2);
        highlightActivePlan(selected.value);
    }
});

currentBalance.addEventListener('input', () => {
    const value = Number(currentBalance.value || 0);
    previewBalance.textContent = formatCurrency(value);
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError();

    const payload = getPayload();
    const clientError = validatePayload(payload);
    if (clientError) {
        showError(clientError);
        return;
    }

    setLoading(true);
    try {
        await wait(850);
        const response = await fetch('/api/recharge', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (!response.ok || data.status !== 'success') {
            throw new Error(data.message || 'Recharge failed.');
        }

        const recharge = data.recharge;
        currentBalance.value = Number(recharge.remainingBalance).toFixed(2);
        previewBalance.textContent = formatCurrency(recharge.remainingBalance);
        previewOperator.textContent = recharge.operator;
        previewTxn.textContent = recharge.transactionId;
        renderReceipt(recharge);
        openSuccessModal(recharge);
        await loadHistory();
    } catch (error) {
        showError(error.message || 'Unable to complete recharge.');
    } finally {
        setLoading(false);
    }
});

resetFormButton.addEventListener('click', () => {
    form.reset();
    operatorSelect.value = 'Jio';
    currentBalance.value = '1000';
    clearError();
    resultSection.classList.add('hidden');
    previewOperator.textContent = 'Jio';
    previewBalance.textContent = formatCurrency(1000);
    previewTxn.textContent = 'Ready';
    loadPlans('Jio');
});

planList.addEventListener('click', (event) => {
    const tile = event.target.closest('.plan-tile');
    if (!tile) {
        return;
    }
    planSelect.value = tile.dataset.planName;
    rechargeAmount.value = Number(tile.dataset.amount).toFixed(2);
    highlightActivePlan(tile.dataset.planName);
});

historySearch.addEventListener('input', renderHistory);
operatorFilter.addEventListener('change', renderHistory);

closeModal.addEventListener('click', () => successModal.classList.add('hidden'));
successModal.addEventListener('click', (event) => {
    if (event.target === successModal) {
        successModal.classList.add('hidden');
    }
});

themeToggle.addEventListener('click', () => {
    const nextTheme = document.body.dataset.theme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
});

async function loadPlans(operator) {
    planOperatorTitle.textContent = `${operator} Packs`;
    planSelect.innerHTML = '<option value="">Loading plans...</option>';
    planList.innerHTML = '<div class="empty-state"><strong>Loading plans</strong><span>Please wait a moment.</span></div>';

    try {
        const response = await fetch(`/api/plans?operator=${encodeURIComponent(operator)}`);
        const data = await response.json();
        if (!response.ok || data.status !== 'success') {
            throw new Error(data.message || 'Unable to load plans.');
        }
        state.plans = data.plans;
        renderPlans();
    } catch (error) {
        state.plans = [];
        planSelect.innerHTML = '<option value="">Plans unavailable</option>';
        planList.innerHTML = `<div class="empty-state"><strong>Plans unavailable</strong><span>${escapeHtml(error.message)}</span></div>`;
    }
}

function renderPlans() {
    planSelect.innerHTML = '<option value="">Select a plan</option>';
    state.plans.forEach((plan) => {
        const option = document.createElement('option');
        option.value = plan.name;
        option.dataset.amount = plan.amount;
        option.textContent = `${plan.name} | ${formatCurrency(plan.amount)} | ${plan.validity}`;
        planSelect.appendChild(option);
    });

    planList.innerHTML = state.plans.map((plan) => `
        <button class="plan-tile" type="button" data-plan-name="${escapeAttr(plan.name)}" data-amount="${escapeAttr(plan.amount)}">
            <span class="plan-topline">
                <strong>${escapeHtml(plan.name)}</strong>
                <span class="plan-badge">${escapeHtml(plan.badge)}</span>
            </span>
            <span class="history-amount">${formatCurrency(plan.amount)}</span>
            <span class="plan-meta">
                <span>${escapeHtml(plan.validity)}</span>
                <span>${escapeHtml(plan.data)}</span>
            </span>
        </button>
    `).join('');
}

async function loadHistory() {
    try {
        const response = await fetch('/api/history');
        const data = await response.json();
        if (!response.ok || data.status !== 'success') {
            throw new Error(data.message || 'Unable to load history.');
        }
        state.history = data.history;
        renderHistory();
        updateStats();
    } catch (error) {
        state.history = [];
        renderHistory();
    }
}

function renderHistory() {
    const search = historySearch.value.trim().toLowerCase();
    const operator = operatorFilter.value;

    const filtered = state.history.filter((item) => {
        const matchesOperator = operator === 'All' || item.operator === operator;
        const matchesSearch = !search
            || item.mobileNumber.toLowerCase().includes(search)
            || item.transactionId.toLowerCase().includes(search)
            || item.customerName.toLowerCase().includes(search);
        return matchesOperator && matchesSearch;
    });

    emptyHistory.classList.toggle('hidden', filtered.length > 0);
    historyList.innerHTML = filtered.map((item) => `
        <article class="history-card">
            <div class="history-top">
                <span class="operator-pill">${escapeHtml(item.operator)}</span>
                <span class="history-amount">${formatCurrency(item.amount)}</span>
            </div>
            <p class="history-mobile">${escapeHtml(item.mobileNumber)}</p>
            <div class="history-bottom">
                <span class="history-date">${escapeHtml(item.dateTimeDisplay)}</span>
            </div>
            <p class="history-txn">${escapeHtml(item.transactionId)}</p>
        </article>
    `).join('');
}

function updateStats() {
    const total = state.history.reduce((sum, item) => sum + Number(item.amount || 0), 0);
    const latest = state.history[0];

    statTransactions.textContent = state.history.length;
    statSpent.textContent = formatCurrency(total);
    statOperator.textContent = latest ? latest.operator : 'Ready';
    if (latest) {
        previewOperator.textContent = latest.operator;
        previewTxn.textContent = latest.transactionId;
    }
}

function getPayload() {
    return {
        mobileNumber: mobileNumber.value.trim(),
        customerName: customerName.value.trim(),
        operator: operatorSelect.value,
        rechargeAmount: rechargeAmount.value,
        rechargePlan: planSelect.value,
        paymentMethod: paymentMethod.value,
        currentBalance: currentBalance.value
    };
}

function validatePayload(payload) {
    if (!payload.mobileNumber || !payload.customerName || !payload.operator || !payload.rechargePlan || !payload.paymentMethod || !payload.currentBalance) {
        return 'All fields are required.';
    }
    if (!/^\d{10}$/.test(payload.mobileNumber)) {
        return 'Mobile number must contain exactly 10 digits.';
    }

    const amount = Number(payload.rechargeAmount);
    const balance = Number(payload.currentBalance);
    if (!Number.isFinite(amount) || amount <= 0) {
        return 'Recharge amount must be positive.';
    }
    if (!Number.isFinite(balance) || balance < 0) {
        return 'Current balance must be a valid non-negative amount.';
    }
    if (balance < amount) {
        return 'Current balance is not sufficient for this recharge.';
    }
    return '';
}

function renderReceipt(recharge) {
    receiptCard.innerHTML = `
        <div class="receipt-status">
            <span class="success-mark" aria-hidden="true">${icons.check}</span>
            <div>
                <h3>Recharge Successful <span aria-hidden="true">&#10003;</span></h3>
                <p>Balance updated and transaction history saved.</p>
            </div>
        </div>
        <div class="receipt-grid">
            ${receiptRow('Mobile Number', recharge.mobileNumber)}
            ${receiptRow('Operator', recharge.operator)}
            ${receiptRow('Recharge Amount', formatCurrency(recharge.amount))}
            ${receiptRow('Recharge Plan', recharge.rechargePlan)}
            ${receiptRow('Transaction ID', recharge.transactionId)}
            ${receiptRow('Date & Time', recharge.dateTimeDisplay)}
            ${receiptRow('Payment Method', recharge.paymentMethod)}
            ${receiptRow('Remaining Balance', formatCurrency(recharge.remainingBalance))}
        </div>
    `;
    resultSection.classList.remove('hidden');
    resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function receiptRow(label, value) {
    return `
        <div class="receipt-row">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(String(value))}</strong>
        </div>
    `;
}

function openSuccessModal(recharge) {
    successMessage.textContent = `${recharge.operator} recharge for ${formatCurrency(recharge.amount)} is complete. Transaction ID: ${recharge.transactionId}`;
    successModal.classList.remove('hidden');
}

function highlightActivePlan(planName) {
    document.querySelectorAll('.plan-tile').forEach((tile) => {
        tile.classList.toggle('active', tile.dataset.planName === planName);
    });
}

function setLoading(isLoading) {
    loadingOverlay.classList.toggle('hidden', !isLoading);
}

function showError(message) {
    formError.textContent = message;
}

function clearError() {
    formError.textContent = '';
}

function setupTheme() {
    const savedTheme = localStorage.getItem('recharge-theme') || 'dark';
    setTheme(savedTheme);
}

function setTheme(theme) {
    document.body.dataset.theme = theme;
    localStorage.setItem('recharge-theme', theme);
    themeIcon.innerHTML = theme === 'dark' ? icons.sun : icons.moon;
}

function formatCurrency(value) {
    return currency.format(Number(value || 0)).replace('\u20b9', 'Rs. ');
}

function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function escapeAttr(value) {
    return escapeHtml(value);
}
