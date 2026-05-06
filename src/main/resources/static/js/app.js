const API_URL = '/api';

let priorityChartInst = null;
let statusChartInst = null;
let currentHistory = [];
let counterTimers = { 1: 0, 2: 0, 3: 0 };
let timerInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    initCharts();
    fetchState();
    updatePreview();
    
    // Poll state every 2 seconds
    setInterval(fetchState, 2000);
    
    // Timer for currently serving
    timerInterval = setInterval(updateTimers, 1000);
});

function initCharts() {
    const ctxPri = document.getElementById('priorityChart').getContext('2d');
    priorityChartInst = new Chart(ctxPri, {
        type: 'pie',
        data: { labels: ['Emergency', 'Elderly', 'Regular'], datasets: [{ data: [0, 0, 0], backgroundColor: ['#C97A7E', '#9A8C98', '#415A77'] }] },
        options: { responsive: true, maintainAspectRatio: false }
    });

    const ctxStat = document.getElementById('statusChart').getContext('2d');
    statusChartInst = new Chart(ctxStat, {
        type: 'doughnut',
        data: { labels: ['Served', 'Cancelled', 'No Show'], datasets: [{ data: [0, 0, 0], backgroundColor: ['#28a745', '#dc3545', '#ffc107'] }] },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function showToast(message) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = "Notification sent: " + message;
    container.appendChild(toast);
    
    // Trigger reflow
    void toast.offsetWidth;
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function updatePreview() {
    const serviceType = document.querySelector('input[name="serviceType"]:checked').value;
    const category = document.querySelector('input[name="customerCategory"]:checked').value;
    // document.getElementById('prev-service').textContent = serviceType;
    // document.getElementById('prev-cat').textContent = category;
}

async function issueTicket() {
    const serviceType = document.querySelector('input[name="serviceType"]:checked').value;
    const priority = document.querySelector('input[name="customerCategory"]:checked').value;
    
    try {
        const response = await fetch(`${API_URL}/queue/ticket?priority=${priority}&serviceType=${encodeURIComponent(serviceType)}`, {
            method: 'POST'
        });
        const data = await response.json();
        
        document.getElementById('modal-service').textContent = data.serviceType;
        document.getElementById('modal-num').textContent = data.ticketNumber;
        document.getElementById('modal-priority').textContent = data.priority;
        document.getElementById('modal-est').textContent = `~${data.estimatedWaitMinutes} mins`;
        document.getElementById('modal-pos').textContent = data.queuePosition;
        
        document.getElementById('ticket-modal').classList.add('active');
        showToast(`Your ticket ${data.ticketNumber} has been created for ${data.serviceType}.`);
        fetchState();
    } catch (e) {
        console.error(e);
        alert("Error issuing ticket");
    }
}

function closeModal() {
    document.getElementById('ticket-modal').classList.remove('active');
}

async function serveNext() {
    try {
        const res = await fetch(`${API_URL}/queue/serve-next`, { method: 'POST' });
        const data = await res.json();
        if (data.message) {
            showToast(data.message);
        } else if (data.error) {
            alert(data.error);
        }
        fetchState();
    } catch (e) { console.error(e); }
}

async function completeTicket(counterId) {
    const display = document.getElementById(`c${counterId}-display`).textContent;
    if (display === 'IDLE') return;
    try {
        const res = await fetch(`${API_URL}/queue/complete/${counterId}/${display}`, { method: 'POST' });
        const data = await res.json();
        if (data.message) showToast(`Ticket ${display} has been completed. Thank you.`);
        fetchState();
    } catch (e) { console.error(e); }
}

async function markNoShow(counterId) {
    const display = document.getElementById(`c${counterId}-display`).textContent;
    if (display === 'IDLE') return;
    try {
        const res = await fetch(`${API_URL}/queue/no-show/${counterId}/${display}`, { method: 'POST' });
        const data = await res.json();
        if (data.message) showToast(`Ticket ${display} marked as no-show.`);
        fetchState();
    } catch (e) { console.error(e); }
}

async function cancelTicket(ticketNumber) {
    try {
        const res = await fetch(`${API_URL}/queue/cancel/${ticketNumber}`, { method: 'POST' });
        const data = await res.json();
        if (data.message) showToast(`Ticket ${ticketNumber} has been cancelled.`);
        fetchState();
    } catch (e) { console.error(e); }
}

async function updateTicketPriority(ticketNumber) {
    const newPriority = prompt("Enter new priority (EMERGENCY, ELDERLY, REGULAR):");
    if (!newPriority) return;
    try {
        const res = await fetch(`${API_URL}/queue/update/${ticketNumber}?priority=${newPriority}`, { method: 'PUT' });
        const data = await res.json();
        if (data.message) showToast(`Ticket ${ticketNumber} priority updated.`);
        else alert(data.error);
        fetchState();
    } catch (e) { console.error(e); }
}

async function demoControl(action) {
    try {
        await fetch(`${API_URL}/demo/${action}`, { method: 'POST' });
        showToast(`Demo action: ${action}`);
        fetchState();
    } catch (e) { console.error(e); }
}

function updateTimers() {
    for (let i = 1; i <= 3; i++) {
        if (counterTimers[i] > 0) {
            let secs = Math.floor((Date.now() - counterTimers[i]) / 1000);
            let m = Math.floor(secs / 60).toString().padStart(2, '0');
            let s = (secs % 60).toString().padStart(2, '0');
            document.getElementById(`c${i}-timer`).textContent = `${m}:${s}`;
        } else {
            document.getElementById(`c${i}-timer`).textContent = "00:00";
        }
    }
}

async function fetchState() {
    try {
        const response = await fetch(`${API_URL}/queue/state`);
        const data = await response.json();
        renderState(data);
    } catch (e) { console.error("Error fetching state"); }
}

function renderState(data) {
    // Stats
    document.getElementById('stat-issued').textContent = data.stats.totalIssued;
    document.getElementById('stat-waiting').textContent = data.queueSize;
    document.getElementById('stat-served').textContent = data.stats.totalServed;
    document.getElementById('stat-cancelled').textContent = data.stats.totalCancelled;
    document.getElementById('stat-noshow').textContent = data.stats.totalNoShow;
    document.getElementById('stat-avg-act').textContent = data.stats.averageActualWaitTime;
    document.getElementById('stat-avg-dur').textContent = data.stats.averageServiceDuration;

    // Queue
    const qList = document.getElementById('queue-list');
    qList.innerHTML = '';
    if (data.queue.length === 0) {
        qList.innerHTML = '<li class="queue-item" style="justify-content:center;color:#999;">Queue is empty</li>';
    } else {
        data.queue.forEach(q => {
            const li = document.createElement('li');
            li.className = 'queue-item';
            li.innerHTML = `
                <div>
                    <div class="q-ticket">${q.ticketNumber}</div>
                    <div class="q-cat q-priority-${q.priority}">${q.priority}</div>
                </div>
                <div>
                    <span class="badge-waiting">WAITING</span>
                    <button class="btn btn-warning btn-sm" style="padding:2px 5px; margin-left:5px" onclick="updateTicketPriority('${q.ticketNumber}')">Edit</button>
                    <button class="btn btn-danger btn-sm" style="padding:2px 5px; margin-left:2px" onclick="cancelTicket('${q.ticketNumber}')">X</button>
                </div>
            `;
            qList.appendChild(li);
        });
    }

    // Counters
    for (let i = 1; i <= 3; i++) {
        const counter = data.counters[i];
        const display = document.getElementById(`c${i}-display`);
        const state = document.getElementById(`c${i}-state`);
        const actions = document.getElementById(`c${i}-actions`);

        if (counter.status === 'SERVING' && counter.serving) {
            display.textContent = counter.serving;
            state.textContent = 'Serving';
            state.className = 'counter-state state-serving';
            actions.style.display = 'flex';
            if (!counterTimers[i] && counter.serviceStartTime) {
                counterTimers[i] = new Date(counter.serviceStartTime).getTime();
            }
        } else {
            display.textContent = 'IDLE';
            state.textContent = 'Available';
            state.className = 'counter-state state-idle';
            actions.style.display = 'none';
            counterTimers[i] = 0;
        }
    }

    // Charts
    const history = data.history || [];
    currentHistory = [...history].reverse();
    renderHistory(); // render table

    // Update charts data
    let pE=0, pP=0, pR=0;
    let sS=0, sC=0, sN=0;

    history.forEach(h => {
        if (h.priority === 'EMERGENCY') pE++;
        else if (h.priority === 'ELDERLY') pP++;
        else pR++;

        if (h.status === 'SERVED') sS++;
        else if (h.status === 'CANCELLED') sC++;
        else if (h.status === 'NO_SHOW') sN++;
    });

    data.queue.forEach(q => {
        if (q.priority === 'EMERGENCY') pE++;
        else if (q.priority === 'ELDERLY') pP++;
        else pR++;
    });

    priorityChartInst.data.datasets[0].data = [pE, pP, pR];
    priorityChartInst.update();

    statusChartInst.data.datasets[0].data = [sS, sC, sN];
    statusChartInst.update();
}

function renderHistory() {
    const search = document.getElementById('filter-search').value.toLowerCase();
    const priority = document.getElementById('filter-priority').value;
    const status = document.getElementById('filter-status').value;

    const tbody = document.getElementById('history-tbody');
    tbody.innerHTML = '';

    const filtered = currentHistory.filter(h => {
        const matchSearch = h.ticketNumber.toLowerCase().includes(search) || h.serviceType.toLowerCase().includes(search);
        const matchPriority = priority === 'ALL' || h.priority === priority;
        const matchStatus = status === 'ALL' || h.status === status;
        return matchSearch && matchPriority && matchStatus;
    });

    filtered.forEach(h => {
        const tr = document.createElement('tr');
        
        let badgeClass = 'badge-waiting';
        if (h.status === 'SERVED') badgeClass = 'badge-served';
        if (h.status === 'CANCELLED') badgeClass = 'badge-cancelled';
        if (h.status === 'NO_SHOW') badgeClass = 'badge-noshow';

        let duration = h.serviceDurationSeconds ? h.serviceDurationSeconds : 0;
        let created = h.createdTime ? new Date(h.createdTime).toLocaleTimeString() : '';

        tr.innerHTML = `
            <td><strong>${h.ticketNumber}</strong></td>
            <td class="q-priority-${h.priority}">${h.priority}</td>
            <td>${h.serviceType}</td>
            <td><span class="${badgeClass}">${h.status}</span></td>
            <td>${h.counterNumber > 0 ? 'C'+h.counterNumber : '-'}</td>
            <td>${h.actualWaitTimeSeconds || 0}</td>
            <td>${duration}</td>
            <td>${created}</td>
        `;
        tbody.appendChild(tr);
    });
}

function exportCSV() {
    window.location.href = `${API_URL}/queue/export`;
}
