const API_URL = '/api';

document.addEventListener('DOMContentLoaded', () => {
    setInterval(updateClock, 1000);
    fetchState();
    setInterval(fetchState, 2000);
});

function updateClock() {
    const now = new Date();
    document.getElementById('clock').textContent = now.toLocaleTimeString();
}

async function fetchState() {
    try {
        const response = await fetch(`${API_URL}/queue/state`);
        const data = await response.json();
        renderDisplay(data);
    } catch (e) {
        console.error("Error fetching state");
    }
}

function renderDisplay(data) {
    // Counters
    const container = document.getElementById('counters-container');
    container.innerHTML = '';
    
    for (let i = 1; i <= 3; i++) {
        const c = data.counters[i];
        if (c.status === 'SERVING' && c.serving) {
            container.innerHTML += `
                <div class="counter-card" style="border-left: 10px solid var(--clr-royal);">
                    <div class="c-num">Counter ${i}</div>
                    <div class="c-ticket">${c.serving}</div>
                </div>
            `;
        } else {
            container.innerHTML += `
                <div class="counter-card" style="opacity: 0.5;">
                    <div class="c-num">Counter ${i}</div>
                    <div class="c-ticket" style="font-size: 3rem; color: var(--txt-muted);">AVAILABLE</div>
                </div>
            `;
        }
    }

    // Waiting queue
    const wList = document.getElementById('waiting-list');
    wList.innerHTML = '';
    const toShow = data.queue.slice(0, 8); // show max 8
    
    if (toShow.length === 0) {
        wList.innerHTML = '<li class="waiting-item" style="color: rgba(255,255,255,0.5);">No tickets waiting</li>';
    } else {
        toShow.forEach(q => {
            wList.innerHTML += `
                <li class="waiting-item">
                    <span class="w-ticket">${q.ticketNumber}</span>
                    <span class="w-cat">${q.priority}</span>
                </li>
            `;
        });
    }
}
