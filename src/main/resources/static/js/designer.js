/* ═══════════════════════════════════════════════════
   Workflow Designer – Main Logic
   Drawflow canvas + property panel + backend API
   ═══════════════════════════════════════════════════ */

// ─── Constants ────────────────────────────────────
const NODE_ICONS = {
    START:    'fa-solid fa-play',
    END:      'fa-solid fa-stop',
    TASK:     'fa-solid fa-gear',
    BRANCH:   'fa-solid fa-code-branch',
    WAIT:     'fa-solid fa-hourglass-half',
    PARALLEL: 'fa-solid fa-arrows-split-up-and-left',
    LOOP:     'fa-solid fa-rotate',
    DELAY:    'fa-solid fa-clock'
};

const NODE_LABELS = {
    START: '开始', END: '结束',
    TASK: '任务', BRANCH: '分支', WAIT: '等待',
    PARALLEL: '并行', LOOP: '循环', DELAY: '延时'
};

const API = {
    definitions: '/api/definitions',
    engine:      '/api/engine'
};

// ─── State ────────────────────────────────────────
let editor;                       // Drawflow instance
let selectedNodeId = null;        // currently selected drawflow node id (number)
let nodeDataMap = {};             // drawflow-node-id → NodeDefinition data
let loadedDefinitionId = null;    // DB entity ID if loaded from backend
let nodeCounter = 0;              // auto-increment for node IDs

// ─── Init ─────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    initDrawflow();
    bindToolbar();
    bindPaletteDragDrop();
    bindPropertiesPanel();
    bindModals();
});

// ═══════════════════════════════════════════════════
//  DRAWFLOW EDITOR
// ═══════════════════════════════════════════════════

function initDrawflow() {
    const container = document.getElementById('drawflow');
    editor = new Drawflow(container);
    editor.reroute = true;
    editor.start();

    editor.on('nodeSelected', (id) => {
        selectedNodeId = id;
        showProperties(id);
    });
    editor.on('nodeUnselected', () => {
        selectedNodeId = null;
        hideProperties();
    });
    editor.on('nodeRemoved', (id) => {
        delete nodeDataMap[id];
        if (selectedNodeId === id) {
            selectedNodeId = null;
            hideProperties();
        }
    });
}

// ═══════════════════════════════════════════════════
//  PALETTE DRAG & DROP
// ═══════════════════════════════════════════════════

function bindPaletteDragDrop() {
    document.querySelectorAll('.palette-node').forEach(el => {
        el.addEventListener('dragstart', (ev) => {
            ev.dataTransfer.setData('nodeType', el.dataset.type);
            ev.dataTransfer.effectAllowed = 'move';
        });
    });

    const canvas = document.getElementById('drawflow');
    canvas.addEventListener('dragover', (ev) => {
        ev.preventDefault();
        ev.dataTransfer.dropEffect = 'move';
    });
    canvas.addEventListener('drop', (ev) => {
        ev.preventDefault();
        const nodeType = ev.dataTransfer.getData('nodeType');
        if (!nodeType) return;
        addNodeAtPosition(nodeType, ev.clientX, ev.clientY);
    });
}

function addNodeAtPosition(type, clientX, clientY) {
    const canvasEl = document.getElementById('drawflow');
    const rect = canvasEl.getBoundingClientRect();
    // Convert screen coords → canvas coords accounting for zoom & pan
    const x = (clientX - rect.left) / editor.zoom - editor.canvas_x;
    const y = (clientY - rect.top)  / editor.zoom - editor.canvas_y;

    nodeCounter++;
    const nodeId = 'node_' + nodeCounter;
    const data = createDefaultNodeData(type, nodeId);
    const dfId = createDrawflowNode(type, data, x, y);
    nodeDataMap[dfId] = data;
}

function createDefaultNodeData(type, id) {
    const base = { id: id, name: NODE_LABELS[type] + '节点', type: type, next: null };
    switch (type) {
        case 'START':
            return { ...base, id: '__start__', name: 'Start' };
        case 'END':
            return { ...base, id: '__end__' + (++nodeCounter), name: 'End', next: undefined };
        case 'TASK':
            return { ...base, activityName: '', input: {}, outputKey: '' };
        case 'BRANCH':
            return { ...base, branches: [{ condition: '', next: '' }], defaultNext: '', next: undefined };
        case 'WAIT':
            return { ...base, signalName: '', outputKey: '', timeoutSeconds: 0, timeoutNext: '' };
        case 'PARALLEL':
            return { ...base, parallelBranches: [{ branchId: 'branch_1', nodes: [] }] };
        case 'LOOP':
            return { ...base, condition: '', maxIterations: 100, loopBody: [] };
        case 'DELAY':
            return { ...base, delaySeconds: 30 };
        default:
            return base;
    }
}

/**
 * Creates a Drawflow node and returns its numeric ID.
 */
function createDrawflowNode(type, data, x, y) {
    let inputs  = 1;
    let outputs = 1;
    if (type === 'START') {
        inputs = 0;   // START has no incoming
        outputs = 1;
    }
    if (type === 'END') {
        inputs = 1;   // END has no outgoing
        outputs = 0;
    }
    if (type === 'BRANCH') {
        outputs = (data.branches ? data.branches.length : 1) + 1; // conditions + default
    }
    if (type === 'WAIT') {
        outputs = 2; // normal + timeout
    }

    const html = buildNodeHtml(type, data);
    return editor.addNode(
        type,       // name
        inputs,     // num inputs
        outputs,    // num outputs
        x, y,       // position
        'node-type-' + type,  // CSS class
        {},         // data (unused, we track in nodeDataMap)
        html        // inner HTML
    );
}

function buildNodeHtml(type, data) {
    const icon = NODE_ICONS[type] || 'fa-solid fa-circle';
    let detail = '';
    switch (type) {
        case 'START':    detail = '流程入口'; break;
        case 'END':      detail = '流程终点'; break;
        case 'TASK':     detail = data.activityName || '(未配置)'; break;
        case 'BRANCH':   detail = (data.branches ? data.branches.length : 0) + ' 条件'; break;
        case 'WAIT':     detail = data.signalName || '(未配置)'; break;
        case 'PARALLEL': detail = (data.parallelBranches ? data.parallelBranches.length : 0) + ' 分支'; break;
        case 'LOOP':     detail = data.condition ? truncate(data.condition, 25) : '(未配置)'; break;
        case 'DELAY':    detail = (data.delaySeconds || 0) + ' 秒'; break;
    }
    return `<div class="df-node">
        <div class="df-node-header"><i class="${icon}"></i> ${escapeHtml(data.name || type)}</div>
        <div class="df-node-body">
            <div class="node-id-label">${escapeHtml(data.id)}</div>
            <div class="node-detail">${escapeHtml(detail)}</div>
        </div>
    </div>`;
}

function updateNodeVisual(dfId) {
    const data = nodeDataMap[dfId];
    if (!data) return;
    const html = buildNodeHtml(data.type, data);
    const nodeEl = document.querySelector('#node-' + dfId + ' .drawflow_content_node');
    if (nodeEl) nodeEl.innerHTML = html;
}

// ═══════════════════════════════════════════════════
//  PROPERTIES PANEL
// ═══════════════════════════════════════════════════

function bindPropertiesPanel() {
    document.getElementById('btn-close-props').addEventListener('click', () => {
        hideProperties();
        if (selectedNodeId) {
            // Deselect the node in drawflow too
            editor.dispatch('nodeUnselected', true);
        }
        selectedNodeId = null;
    });
    document.getElementById('btn-delete-node').addEventListener('click', () => {
        if (selectedNodeId) {
            editor.removeNodeId('node-' + selectedNodeId);
        }
    });
}

function showProperties(dfId) {
    const data = nodeDataMap[dfId];
    if (!data) return;

    const panel = document.getElementById('properties');
    const content = document.getElementById('props-content');
    panel.classList.remove('hidden');

    let html = `<span class="prop-type-badge ${data.type}">${data.type}</span>`;

    // Common fields
    html += propGroup('节点 ID', inputField('prop-node-id', data.id, 'text', '唯一标识'));
    html += propGroup('节点名称', inputField('prop-node-name', data.name, 'text', '显示名称'));
    html += '<hr class="prop-divider">';

    // Type-specific fields
    switch (data.type) {
        case 'START':
            html += `<div class="prop-group"><div class="prop-hint">开始节点是流程入口，连接到第一个业务节点</div></div>`;
            break;

        case 'END':
            html += `<div class="prop-group"><div class="prop-hint">结束节点是流程终点，执行到此处工作流结束</div></div>`;
            break;

        case 'TASK':
            html += propGroup('Activity 名称', inputField('prop-activity', data.activityName, 'text', 'e.g. recordVisit'));
            html += propGroup('输入参数 (JSON)', textareaField('prop-input', jsonPretty(data.input || {}), '{ "key": "#variable" }'));
            html += propGroup('输出映射', textareaField('prop-output-key', formatOutputKey(data.outputKey), '单个变量名 或 Map: {"var": "field"}'));
            html += `<div class="prop-group"><div class="prop-hint">字符串: 整个结果存为一个变量<br>Map: {"变量名": "结果字段"}, "*" 表示整个结果</div></div>`;
            break;

        case 'BRANCH':
            html += buildBranchEditor(data);
            html += propGroup('默认 Next', inputField('prop-default-next', data.defaultNext || '', 'text', '无匹配时跳转节点ID'));
            break;

        case 'WAIT':
            html += propGroup('信号名称', inputField('prop-signal', data.signalName, 'text', 'e.g. labResult'));
            html += propGroup('输出映射', textareaField('prop-output-key', formatOutputKey(data.outputKey), '单个变量名 或 Map: {"var": "field"}'));
            html += propGroup('超时 (秒)', inputField('prop-timeout', data.timeoutSeconds, 'number', '0 = 无限等待'));
            html += propGroup('超时跳转', inputField('prop-timeout-next', data.timeoutNext || '', 'text', '超时后跳转节点'));
            break;

        case 'PARALLEL':
            html += propGroup('并行分支 (JSON)',
                textareaField('prop-parallel', jsonPretty(data.parallelBranches || []),
                    '[{"branchId":"b1","nodes":[...]}]'));
            html += `<div class="prop-group"><div class="prop-hint">每个分支包含 branchId 和 nodes 数组</div></div>`;
            break;

        case 'LOOP':
            html += propGroup('循环条件 (SpEL)', inputField('prop-condition', data.condition, 'text', '#count < #max'));
            html += propGroup('最大迭代', inputField('prop-max-iter', data.maxIterations, 'number', '安全上限'));
            html += propGroup('循环体 (JSON)',
                textareaField('prop-loop-body', jsonPretty(data.loopBody || []),
                    '[{ "id":..., "type":"TASK", ... }]'));
            break;

        case 'DELAY':
            html += propGroup('延时 (秒)', inputField('prop-delay', data.delaySeconds, 'number', ''));
            break;
    }

    content.innerHTML = html;
    bindPropertyInputs(dfId);
}

function hideProperties() {
    document.getElementById('properties').classList.add('hidden');
}

function buildBranchEditor(data) {
    let html = '<div class="prop-group"><label>分支条件</label><div class="branch-list" id="branch-list">';
    (data.branches || []).forEach((b, i) => {
        html += `<div class="branch-item">
            <input class="branch-cond" data-idx="${i}" value="${escapeAttr(b.condition)}" placeholder="SpEL 条件">
            <input class="branch-next" data-idx="${i}" value="${escapeAttr(b.next || '')}" placeholder="目标节点" style="width:80px">
            <button class="branch-remove" data-idx="${i}" title="删除"><i class="fa-solid fa-xmark"></i></button>
        </div>`;
    });
    html += '</div>';
    html += '<div class="btn-add-branch" id="btn-add-branch">+ 添加条件</div></div>';
    return html;
}

/**
 * Bind input change listeners for the property panel.
 */
function bindPropertyInputs(dfId) {
    const data = nodeDataMap[dfId];
    if (!data) return;

    // Helper: bind a plain text/number input to a data field
    const bind = (elId, key, parser) => {
        const el = document.getElementById(elId);
        if (!el) return;
        el.addEventListener('change', () => {
            data[key] = parser ? parser(el.value) : el.value;
            updateNodeVisual(dfId);
        });
    };

    bind('prop-node-id', 'id');
    bind('prop-node-name', 'name');

    switch (data.type) {
        case 'TASK':
            bind('prop-activity', 'activityName');
            bindOutputKey('prop-output-key', data);
            bindJson('prop-input', (val) => { data.input = val; });
            break;

        case 'BRANCH':
            bindBranchEditor(dfId);
            bind('prop-default-next', 'defaultNext');
            break;

        case 'WAIT':
            bind('prop-signal', 'signalName');
            bindOutputKey('prop-output-key', data);
            bind('prop-timeout', 'timeoutSeconds', v => parseInt(v) || 0);
            bind('prop-timeout-next', 'timeoutNext');
            break;

        case 'PARALLEL':
            bindJson('prop-parallel', (val) => {
                data.parallelBranches = val;
                updateNodeVisual(dfId);
            });
            break;

        case 'LOOP':
            bind('prop-condition', 'condition');
            bind('prop-max-iter', 'maxIterations', v => parseInt(v) || 100);
            bindJson('prop-loop-body', (val) => {
                data.loopBody = val;
                updateNodeVisual(dfId);
            });
            break;

        case 'DELAY':
            bind('prop-delay', 'delaySeconds', v => parseInt(v) || 0);
            break;
    }
}

function bindJson(elId, setter) {
    const el = document.getElementById(elId);
    if (!el) return;
    el.addEventListener('change', () => {
        try {
            setter(JSON.parse(el.value));
        } catch (e) {
            toast('JSON 格式错误', 'error');
        }
    });
}

/**
 * Format outputKey for display: plain string → as-is, Map → pretty JSON.
 */
function formatOutputKey(val) {
    if (val == null || val === '') return '';
    if (typeof val === 'object') return JSON.stringify(val, null, 2);
    return String(val);
}

/**
 * Bind outputKey textarea: auto-detect String vs JSON Map.
 */
function bindOutputKey(elId, data) {
    const el = document.getElementById(elId);
    if (!el) return;
    el.addEventListener('change', () => {
        const raw = el.value.trim();
        if (!raw) {
            data.outputKey = '';
            return;
        }
        if (raw.startsWith('{')) {
            try {
                data.outputKey = JSON.parse(raw);
            } catch (e) {
                toast('outputKey JSON 格式错误', 'error');
            }
        } else {
            data.outputKey = raw;
        }
    });
}

function bindBranchEditor(dfId) {
    const data = nodeDataMap[dfId];

    document.querySelectorAll('.branch-cond').forEach(el => {
        el.addEventListener('change', () => {
            const idx = parseInt(el.dataset.idx);
            if (data.branches[idx]) {
                data.branches[idx].condition = el.value;
                updateNodeVisual(dfId);
            }
        });
    });

    document.querySelectorAll('.branch-next').forEach(el => {
        el.addEventListener('change', () => {
            const idx = parseInt(el.dataset.idx);
            if (data.branches[idx]) {
                data.branches[idx].next = el.value;
            }
        });
    });

    document.querySelectorAll('.branch-remove').forEach(el => {
        el.addEventListener('click', () => {
            const idx = parseInt(el.dataset.idx);
            data.branches.splice(idx, 1);
            showProperties(dfId); // re-render
        });
    });

    const addBtn = document.getElementById('btn-add-branch');
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            data.branches.push({ condition: '', next: '' });
            showProperties(dfId); // re-render
        });
    }
}

// ═══════════════════════════════════════════════════
//  TOOLBAR ACTIONS
// ═══════════════════════════════════════════════════

function bindToolbar() {
    document.getElementById('btn-new').addEventListener('click', actionNew);
    document.getElementById('btn-import').addEventListener('click', actionLoad);
    document.getElementById('btn-save').addEventListener('click', actionSave);
    document.getElementById('btn-publish').addEventListener('click', actionPublish);
    document.getElementById('btn-export').addEventListener('click', actionExport);
    document.getElementById('btn-import-json').addEventListener('click', actionImportJson);
    document.getElementById('btn-run').addEventListener('click', actionRun);

    document.getElementById('btn-zoom-in').addEventListener('click', () => editor.zoom_in());
    document.getElementById('btn-zoom-out').addEventListener('click', () => editor.zoom_out());
    document.getElementById('btn-zoom-reset').addEventListener('click', () => editor.zoom_reset());
}

function actionNew() {
    if (Object.keys(nodeDataMap).length > 0) {
        if (!confirm('确定要新建吗？当前画布内容将被清除。')) return;
    }
    editor.clear();
    nodeDataMap = {};
    nodeCounter = 0;
    selectedNodeId = null;
    loadedDefinitionId = null;
    hideProperties();
    document.getElementById('wf-type').value = '';
    document.getElementById('wf-name').value = '';
    toast('已新建空白画布', 'info');
}

// ─── Load from backend ───────────────────────────

function actionLoad() {
    showModal('modal-load');
    const list = document.getElementById('load-list');
    list.innerHTML = '<p class="loading">加载中...</p>';

    fetch(API.definitions)
        .then(r => r.json())
        .then(items => {
            if (!items || items.length === 0) {
                list.innerHTML = '<p class="loading">暂无已保存的定义</p>';
                return;
            }
            list.innerHTML = '';
            items.forEach(item => {
                const div = document.createElement('div');
                div.className = 'load-item';
                div.innerHTML = `
                    <div class="load-info">
                        <span class="load-type">${escapeHtml(item.type)}</span>
                        <span class="load-meta">v${item.version} · ${item.name || '(无名称)'}</span>
                    </div>
                    <span class="load-status ${item.status}">${item.status}</span>`;
                div.addEventListener('click', () => loadDefinition(item));
                list.appendChild(div);
            });
        })
        .catch(err => {
            list.innerHTML = '<p class="loading">加载失败: ' + escapeHtml(String(err.message)) + '</p>';
        });
}

function loadDefinition(item) {
    // Fetch the full detail (includes definitionJson)
    fetch(`${API.definitions}/id/${item.id}`)
        .then(r => r.json())
        .then(detail => {
            const defJson = typeof detail.definitionJson === 'string'
                ? JSON.parse(detail.definitionJson)
                : detail.definitionJson;
            importWorkflowDefinition(defJson);
            loadedDefinitionId = item.id;
            document.getElementById('wf-type').value = item.type || defJson.id || '';
            document.getElementById('wf-name').value = item.name || defJson.name || '';
            hideModal('modal-load');
            toast('已加载: ' + item.type + ' v' + item.version, 'success');
        })
        .catch(err => toast('加载失败: ' + err.message, 'error'));
}

// ─── Save to backend ──────────────────────────────

function actionSave() {
    const type = document.getElementById('wf-type').value.trim();
    const name = document.getElementById('wf-name').value.trim();
    if (!type) {
        toast('请填写 Flow Type', 'error');
        return;
    }

    const def = exportWorkflowDefinition();
    const defJson = JSON.stringify(def);

    if (loadedDefinitionId) {
        // Try to update existing DRAFT
        fetch(`${API.definitions}/${loadedDefinitionId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: name, definitionJson: defJson })
        })
        .then(r => {
            if (!r.ok) return r.json().then(e => { throw new Error(e.error || r.statusText); });
            return r.json();
        })
        .then(res => toast('已更新 v' + res.version, 'success'))
        .catch(() => {
            // If update fails (e.g. not DRAFT), create a new version
            createNewVersion(type, name, defJson);
        });
    } else {
        createNewVersion(type, name, defJson);
    }
}

function createNewVersion(type, name, defJson) {
    fetch(API.definitions, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: type, name: name, definitionJson: defJson })
    })
    .then(r => {
        if (!r.ok) return r.json().then(e => { throw new Error(e.error || r.statusText); });
        return r.json();
    })
    .then(res => {
        loadedDefinitionId = res.id;
        toast('已保存 ' + type + ' v' + res.version, 'success');
    })
    .catch(err => toast('保存失败: ' + err.message, 'error'));
}

// ─── Publish ──────────────────────────────────────

function actionPublish() {
    if (!loadedDefinitionId) {
        toast('请先保存再发布', 'error');
        return;
    }
    fetch(`${API.definitions}/${loadedDefinitionId}/publish`, { method: 'POST' })
        .then(r => {
            if (!r.ok) return r.json().then(e => { throw new Error(e.error || r.statusText); });
            return r.json();
        })
        .then(res => toast('已发布 ' + res.type + ' v' + res.version, 'success'))
        .catch(err => toast('发布失败: ' + err.message, 'error'));
}

// ─── Export / Import JSON ─────────────────────────

function actionExport() {
    const def = exportWorkflowDefinition();
    document.getElementById('modal-json-title').textContent = '导出 JSON';
    document.getElementById('json-editor').value = JSON.stringify(def, null, 2);
    document.getElementById('json-editor').readOnly = true;
    document.getElementById('btn-json-apply').classList.add('hidden');
    showModal('modal-json');
}

function actionImportJson() {
    document.getElementById('modal-json-title').textContent = '导入 JSON';
    document.getElementById('json-editor').value = '';
    document.getElementById('json-editor').readOnly = false;
    document.getElementById('btn-json-apply').classList.remove('hidden');
    showModal('modal-json');
}

// ─── Run ──────────────────────────────────────────

function actionRun() {
    const type = document.getElementById('wf-type').value.trim();
    if (!type) {
        toast('请填写 Flow Type 以启动', 'error');
        return;
    }
    document.getElementById('run-result').classList.add('hidden');
    document.getElementById('run-result').className = 'hidden';
    showModal('modal-run');
}


// ═══════════════════════════════════════════════════
//  JSON ↔ DRAWFLOW CONVERSION
// ═══════════════════════════════════════════════════

/**
 * Export the current canvas as a WorkflowDefinition JSON object.
 */
function exportWorkflowDefinition() {
    const type = document.getElementById('wf-type').value.trim() || 'untitled';
    const name = document.getElementById('wf-name').value.trim() || type;

    const drawflowData = editor.export().drawflow.Home.data;
    const nodes = [];
    const dfIdToNodeId = {};

    // Build mapping: drawflow ID → logical node ID
    Object.keys(drawflowData).forEach(dfId => {
        const data = nodeDataMap[dfId];
        if (data) dfIdToNodeId[dfId] = data.id;
    });

    // Build nodes with connection-based "next" resolution
    Object.keys(drawflowData).forEach(dfId => {
        const dfNode = drawflowData[dfId];
        const data = nodeDataMap[dfId];
        if (!data) return;

        const node = JSON.parse(JSON.stringify(data)); // deep clone
        const outputs = dfNode.outputs || {};

        if (data.type === 'BRANCH') {
            // BRANCH: output_1..N → conditions, output_(N+1) → defaultNext
            const branchCount = (node.branches || []).length;
            Object.keys(outputs).forEach(outKey => {
                const conns = outputs[outKey].connections || [];
                if (conns.length === 0) return;
                const targetNodeId = dfIdToNodeId[conns[0].node];
                const outIdx = parseInt(outKey.replace('output_', '')) - 1;
                if (outIdx < branchCount) {
                    node.branches[outIdx].next = targetNodeId || '';
                } else {
                    node.defaultNext = targetNodeId || '';
                }
            });
            delete node.next; // BRANCH doesn't use "next"

        } else if (data.type === 'WAIT') {
            // WAIT: output_1 → normal next, output_2 → timeout next
            Object.keys(outputs).forEach(outKey => {
                const conns = outputs[outKey].connections || [];
                if (conns.length === 0) return;
                const targetNodeId = dfIdToNodeId[conns[0].node];
                if (outKey === 'output_1') {
                    node.next = targetNodeId || null;
                } else if (outKey === 'output_2') {
                    node.timeoutNext = targetNodeId || null;
                }
            });

        } else {
            // All other types: output_1 → next
            const out1 = outputs['output_1'];
            if (out1 && out1.connections && out1.connections.length > 0) {
                node.next = dfIdToNodeId[out1.connections[0].node] || null;
            } else {
                node.next = null;
            }
        }

        // Clean up undefined / empty fields
        cleanNode(node);
        nodes.push(node);
    });

    // Determine startNode: look for the START node first
    let startNode = null;
    for (const n of nodes) {
        if (n.type === 'START') {
            startNode = n.id;
            break;
        }
    }
    // Fallback: node with no incoming connections
    if (!startNode) {
        const hasIncoming = new Set();
        Object.values(drawflowData).forEach(dfNode => {
            Object.values(dfNode.outputs || {}).forEach(out => {
                (out.connections || []).forEach(c => hasIncoming.add(String(c.node)));
            });
        });
        for (const dfId of Object.keys(drawflowData)) {
            if (!hasIncoming.has(dfId) && nodeDataMap[dfId]) {
                startNode = nodeDataMap[dfId].id;
                break;
            }
        }
    }
    if (!startNode && nodes.length > 0) startNode = nodes[0].id;

    return {
        id: type,
        name: name,
        version: 1,
        startNode: startNode,
        initialVariables: {},
        nodes: nodes
    };
}

/**
 * Import a WorkflowDefinition JSON and render on the canvas.
 */
function importWorkflowDefinition(def) {
    editor.clear();
    nodeDataMap = {};
    nodeCounter = 0;
    selectedNodeId = null;
    hideProperties();

    if (!def || !def.nodes || def.nodes.length === 0) return;

    document.getElementById('wf-type').value = def.id || '';
    document.getElementById('wf-name').value = def.name || '';

    const nodeIdToDfId = {};
    const GRID_X = 280;
    const GRID_Y = 150;
    const START_X = 100;
    const START_Y = 100;
    const nodeMap = {};
    def.nodes.forEach(n => nodeMap[n.id] = n);

    // BFS from startNode for automatic layout
    const visited = new Set();
    const positions = {};
    const queue = [];
    const rowPerCol = {};

    if (def.startNode && nodeMap[def.startNode]) {
        queue.push({ id: def.startNode, col: 0 });
    }

    while (queue.length > 0) {
        const { id, col } = queue.shift();
        if (visited.has(id)) continue;
        visited.add(id);

        if (!rowPerCol[col]) rowPerCol[col] = 0;
        positions[id] = {
            x: START_X + col * GRID_X,
            y: START_Y + rowPerCol[col] * GRID_Y
        };
        rowPerCol[col]++;

        const node = nodeMap[id];
        if (!node) continue;

        // Collect all "next" targets for BFS
        const nexts = [];
        if (node.type === 'BRANCH') {
            (node.branches || []).forEach(b => { if (b.next) nexts.push(b.next); });
            if (node.defaultNext) nexts.push(node.defaultNext);
        } else if (node.type === 'WAIT') {
            if (node.next) nexts.push(node.next);
            if (node.timeoutNext) nexts.push(node.timeoutNext);
        } else {
            if (node.next) nexts.push(node.next);
        }
        nexts.forEach(nid => {
            if (!visited.has(nid)) queue.push({ id: nid, col: col + 1 });
        });
    }

    // Handle orphan nodes (not reachable from startNode)
    def.nodes.forEach(n => {
        if (!visited.has(n.id)) {
            const maxCol = Object.keys(rowPerCol).length > 0
                ? Math.max(...Object.keys(rowPerCol).map(Number)) + 1 : 0;
            if (!rowPerCol[maxCol]) rowPerCol[maxCol] = 0;
            positions[n.id] = {
                x: START_X + maxCol * GRID_X,
                y: START_Y + rowPerCol[maxCol] * GRID_Y
            };
            rowPerCol[maxCol]++;
        }
    });

    // Create all drawflow nodes
    def.nodes.forEach(n => {
        nodeCounter++;
        const pos = positions[n.id] || { x: START_X, y: START_Y };
        const dfId = createDrawflowNode(n.type, n, pos.x, pos.y);
        nodeDataMap[dfId] = n;
        nodeIdToDfId[n.id] = dfId;
    });

    // Create connections based on node definitions
    def.nodes.forEach(n => {
        const fromDfId = nodeIdToDfId[n.id];
        if (!fromDfId) return;

        if (n.type === 'BRANCH') {
            (n.branches || []).forEach((b, i) => {
                const toDfId = nodeIdToDfId[b.next];
                if (toDfId) {
                    safeConnect(fromDfId, toDfId, 'output_' + (i + 1), 'input_1');
                }
            });
            if (n.defaultNext) {
                const toDfId = nodeIdToDfId[n.defaultNext];
                const outIdx = (n.branches || []).length + 1;
                if (toDfId) {
                    safeConnect(fromDfId, toDfId, 'output_' + outIdx, 'input_1');
                }
            }
        } else if (n.type === 'WAIT') {
            if (n.next) {
                const t = nodeIdToDfId[n.next];
                if (t) safeConnect(fromDfId, t, 'output_1', 'input_1');
            }
            if (n.timeoutNext) {
                const t = nodeIdToDfId[n.timeoutNext];
                if (t) safeConnect(fromDfId, t, 'output_2', 'input_1');
            }
        } else {
            if (n.next) {
                const t = nodeIdToDfId[n.next];
                if (t) safeConnect(fromDfId, t, 'output_1', 'input_1');
            }
        }
    });
}

function safeConnect(from, to, outputClass, inputClass) {
    try {
        editor.addConnection(from, to, outputClass, inputClass);
    } catch (e) {
        console.warn('Connection failed:', from, '→', to, e);
    }
}

// ═══════════════════════════════════════════════════
//  MODALS
// ═══════════════════════════════════════════════════

function bindModals() {
    // Close buttons
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', () => {
            btn.closest('.modal').classList.add('hidden');
        });
    });

    // Click backdrop to close
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) modal.classList.add('hidden');
        });
    });

    // JSON modal: copy
    document.getElementById('btn-json-copy').addEventListener('click', () => {
        const text = document.getElementById('json-editor').value;
        navigator.clipboard.writeText(text)
            .then(() => toast('已复制到剪贴板', 'success'))
            .catch(() => {
                // Fallback for older browsers
                document.getElementById('json-editor').select();
                document.execCommand('copy');
                toast('已复制到剪贴板', 'success');
            });
    });

    // JSON modal: apply (import)
    document.getElementById('btn-json-apply').addEventListener('click', () => {
        const raw = document.getElementById('json-editor').value.trim();
        if (!raw) { toast('请粘贴 JSON 内容', 'error'); return; }
        try {
            const def = JSON.parse(raw);
            importWorkflowDefinition(def);
            hideModal('modal-json');
            toast('已导入工作流定义', 'success');
        } catch (e) {
            toast('JSON 格式错误: ' + e.message, 'error');
        }
    });

    // Run modal: confirm
    document.getElementById('btn-run-confirm').addEventListener('click', () => {
        const type = document.getElementById('wf-type').value.trim();
        const workflowId = document.getElementById('run-workflow-id').value.trim() || undefined;
        let inputVars = {};
        try {
            inputVars = JSON.parse(document.getElementById('run-input-vars').value || '{}');
        } catch (e) {
            toast('输入变量 JSON 格式错误', 'error');
            return;
        }

        const resultEl = document.getElementById('run-result');
        resultEl.className = '';
        resultEl.classList.remove('hidden');
        resultEl.textContent = '启动中...';

        fetch(`${API.engine}/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                type: type,
                workflowId: workflowId,
                inputVariables: inputVars
            })
        })
        .then(r => r.json())
        .then(res => {
            if (res.error) {
                resultEl.className = 'error';
                resultEl.textContent = '错误: ' + res.error;
            } else {
                resultEl.className = 'success';
                resultEl.textContent = `✓ 已启动 workflowId: ${res.workflowId} (v${res.version}, source: ${res.source})`;
            }
        })
        .catch(err => {
            resultEl.className = 'error';
            resultEl.textContent = '请求失败: ' + err.message;
        });
    });
}

function showModal(id) {
    document.getElementById(id).classList.remove('hidden');
}

function hideModal(id) {
    document.getElementById(id).classList.add('hidden');
}

// ═══════════════════════════════════════════════════
//  UTILITY HELPERS
// ═══════════════════════════════════════════════════

function propGroup(label, inputHtml) {
    return `<div class="prop-group"><label>${label}</label>${inputHtml}</div>`;
}

function inputField(id, value, type, placeholder) {
    return `<input id="${id}" type="${type || 'text'}" value="${escapeAttr(value ?? '')}" placeholder="${escapeAttr(placeholder || '')}">`;
}

function textareaField(id, value, placeholder) {
    return `<textarea id="${id}" placeholder="${escapeAttr(placeholder || '')}">${escapeHtml(value ?? '')}</textarea>`;
}

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function escapeAttr(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function truncate(str, maxLen) {
    if (!str || str.length <= maxLen) return str;
    return str.substring(0, maxLen) + '…';
}

function jsonPretty(obj) {
    try { return JSON.stringify(obj, null, 2); }
    catch (e) { return String(obj); }
}

/**
 * Remove undefined/null/empty fields from a node for cleaner JSON export.
 */
function cleanNode(node) {
    const deleteIfEmpty = (key) => {
        const v = node[key];
        if (v === undefined || v === null || v === '' || v === 0) delete node[key];
    };
    // START/END are simple — only keep id, name, type, and next (for START)
    if (node.type === 'START' || node.type === 'END') {
        delete node.activityName;
        delete node.input;
        delete node.outputKey;
        delete node.branches;
        delete node.defaultNext;
        delete node.signalName;
        delete node.timeoutSeconds;
        delete node.timeoutNext;
        delete node.parallelBranches;
        delete node.condition;
        delete node.maxIterations;
        delete node.loopBody;
        delete node.delaySeconds;
        if (node.type === 'END') delete node.next;
        return;
    }
    // Type-specific cleanup
    if (node.type !== 'TASK' && node.type !== 'WAIT') {
        delete node.outputKey;
    }
    if (node.type !== 'TASK') {
        delete node.activityName;
        delete node.input;
    }
    if (node.type !== 'BRANCH') {
        delete node.branches;
        delete node.defaultNext;
    }
    if (node.type !== 'WAIT') {
        delete node.signalName;
        delete node.timeoutSeconds;
        delete node.timeoutNext;
    }
    if (node.type !== 'PARALLEL') {
        delete node.parallelBranches;
    }
    if (node.type !== 'LOOP') {
        delete node.condition;
        delete node.maxIterations;
        delete node.loopBody;
    }
    if (node.type !== 'DELAY') {
        delete node.delaySeconds;
    }
}

function toast(msg, type) {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.className = 'toast show ' + (type || 'info');
    clearTimeout(el._timer);
    el._timer = setTimeout(() => {
        el.className = 'toast hidden';
    }, 3000);
}
