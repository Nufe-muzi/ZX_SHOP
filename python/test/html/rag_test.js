// RAG 知识库管理系统 JavaScript

// API 基础地址
let API_BASE = 'http://localhost:8000';
let currentKbId = null;

// ==================== 工具函数 ====================

async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({ detail: '请求失败' }));
            throw new Error(error.detail || '请求失败');
        }
        return await response.json();
    } catch (error) {
        console.error('API请求错误:', error);
        throw error;
    }
}

function showStatus(elementId, message, isError = false) {
    const el = document.getElementById(elementId);
    el.textContent = message;
    el.className = isError ? 'status-error' : 'status-ok';
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN');
}

// ==================== Tab 切换 ====================

function switchTab(tabId) {
    // 隐藏所有 tab 内容
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.remove('active');
    });

    // 移除所有 tab 激活状态
    document.querySelectorAll('.nav-tab').forEach(el => {
        el.classList.remove('active');
    });

    // 显示当前 tab
    document.getElementById(tabId).classList.add('active');

    // 激活当前 tab 按钮
    document.querySelector(`.nav-tab[data-tab="${tabId}"]`).classList.add('active');

    // 加载数据
    if (tabId === 'kb-list') {
        loadKbList();
    } else if (tabId === 'search') {
        loadKbSelect();
    } else if (tabId === 'config') {
        loadConfig();
    }
}

// ==================== 连接测试 ====================

async function testConnection() {
    API_BASE = document.getElementById('apiBase').value;
    try {
        const result = await apiRequest('/rag/supported-formats');
        showStatus('connectionStatus', `连接成功 (支持 ${result.formats.length} 种格式)`);
    } catch (error) {
        showStatus('connectionStatus', `连接失败: ${error.message}`, true);
    }
}

// ==================== 知识库管理 ====================

async function loadKbList() {
    const container = document.getElementById('kbList');
    container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">📚</div><p>加载中...</p></div>';

    try {
        const result = await apiRequest('/rag/kb/list');

        if (!result.kbs || result.kbs.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <p>暂无知识库，点击"新建知识库"创建</p>
                </div>
            `;
            return;
        }

        container.innerHTML = result.kbs.map(kb => `
            <div class="kb-card" onclick="openKbDetail(${kb.id})">
                <div class="kb-card-header">
                    <div class="kb-card-title">${escapeHtml(kb.name)}</div>
                    <div class="kb-card-actions">
                        <button class="btn btn-danger btn-sm" onclick="event.stopPropagation(); deleteKb(${kb.id})">删除</button>
                    </div>
                </div>
                <div class="kb-card-desc">${escapeHtml(kb.description || '暂无描述')}</div>
                <div class="kb-card-stats">
                    <span>📄 ${kb.document_count || 0} 文档</span>
                    <span>📝 ${kb.total_chunks || 0} 片段</span>
                    <span>📅 ${formatDate(kb.created_at)}</span>
                </div>
            </div>
        `).join('');
    } catch (error) {
        container.innerHTML = `<div class="empty-state"><p style="color: #dc3545;">加载失败: ${error.message}</p></div>`;
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function showCreateKbModal() {
    document.getElementById('kbName').value = '';
    document.getElementById('kbDescription').value = '';
    document.getElementById('kbEmbeddingModel').value = 'text-embedding-v2';
    document.getElementById('kbChunkSize').value = 800;
    document.getElementById('kbChunkOverlap').value = 10;
    document.getElementById('kbSeparatorType').value = 'newline';
    openModal('createKbModal');
}

async function createKb() {
    const name = document.getElementById('kbName').value.trim();
    const description = document.getElementById('kbDescription').value.trim();
    const embeddingModel = document.getElementById('kbEmbeddingModel').value.trim();
    const chunkSize = parseInt(document.getElementById('kbChunkSize').value);
    const chunkOverlapPercent = parseInt(document.getElementById('kbChunkOverlap').value);
    const separatorType = document.getElementById('kbSeparatorType').value;

    if (!name) {
        alert('请输入知识库名称');
        return;
    }

    const chunkOverlap = Math.floor(chunkSize * chunkOverlapPercent / 100);

    try {
        await apiRequest('/rag/kb/create', {
            method: 'POST',
            body: JSON.stringify({
                name,
                description,
                embedding_model: embeddingModel,
                chunk_size: chunkSize,
                chunk_overlap: chunkOverlap,
                separator_type: separatorType
            })
        });

        closeModal('createKbModal');
        loadKbList();
    } catch (error) {
        alert('创建失败: ' + error.message);
    }
}

async function deleteKb(kbId) {
    if (!confirm('确定要删除此知识库吗？所有文档和片段都将被删除！')) {
        return;
    }

    try {
        await apiRequest(`/rag/kb/${kbId}`, { method: 'DELETE' });
        loadKbList();
    } catch (error) {
        alert('删除失败: ' + error.message);
    }
}

async function openKbDetail(kbId) {
    currentKbId = kbId;

    // 显示文档管理 tab
    document.getElementById('kbDetailTab').style.display = 'block';
    switchTab('kb-detail');

    // 加载知识库信息
    try {
        const kb = await apiRequest(`/rag/kb/${kbId}`);
        document.getElementById('kbDetailTitle').textContent = kb.name + ' - 文档管理';
        document.getElementById('kbInfo').innerHTML = `
            <div class="kb-info-row">
                <div class="kb-info-item"><strong>描述:</strong> ${escapeHtml(kb.description || '暂无')}</div>
                <div class="kb-info-item"><strong>Embedding:</strong> ${kb.embedding_model}</div>
                <div class="kb-info-item"><strong>分段大小:</strong> ${kb.chunk_size}</div>
                <div class="kb-info-item"><strong>重叠:</strong> ${kb.chunk_overlap}</div>
                <div class="kb-info-item"><strong>创建时间:</strong> ${formatDate(kb.created_at)}</div>
            </div>
        `;
    } catch (error) {
        document.getElementById('kbInfo').innerHTML = `<p style="color: #dc3545;">加载失败: ${error.message}</p>`;
    }

    // 加载文档列表
    loadDocumentList(kbId);
}

function backToKbList() {
    currentKbId = null;
    document.getElementById('kbDetailTab').style.display = 'none';
    switchTab('kb-list');
}

async function loadDocumentList(kbId) {
    const container = document.getElementById('documentList');
    container.innerHTML = '<div class="empty-state"><p>加载中...</p></div>';

    try {
        const result = await apiRequest(`/rag/kb/${kbId}/documents`);

        if (!result.documents || result.documents.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <p>暂无文档，点击"上传文档"添加</p>
                </div>
            `;
            return;
        }

        container.innerHTML = result.documents.map(doc => `
            <div class="doc-item">
                <div class="doc-info">
                    <div class="doc-name">${escapeHtml(doc.filename)}</div>
                    <div class="doc-meta">
                        ${doc.file_type} | ${formatFileSize(doc.file_size)} |
                        ${doc.chunk_count} 片段 | ${formatDate(doc.created_at)}
                    </div>
                </div>
                <div class="doc-actions">
                    <button class="btn btn-sm btn-info" onclick="showDocDetail(${doc.id})">详情</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteDocument(${doc.id})">删除</button>
                </div>
            </div>
        `).join('');
    } catch (error) {
        container.innerHTML = `<div class="empty-state"><p style="color: #dc3545;">加载失败: ${error.message}</p></div>`;
    }
}

async function showDocDetail(docId) {
    try {
        const doc = await apiRequest(`/rag/document/${docId}`);
        const chunks = await apiRequest(`/rag/document/${docId}/chunks`);

        document.getElementById('docDetailTitle').textContent = doc.filename;
        document.getElementById('docInfo').innerHTML = `
            <div class="doc-info-row">
                <div class="doc-info-item"><strong>文件类型:</strong> ${doc.file_type}</div>
                <div class="doc-info-item"><strong>文件大小:</strong> ${formatFileSize(doc.file_size)}</div>
                <div class="doc-info-item"><strong>总字符:</strong> ${doc.total_chars}</div>
                <div class="doc-info-item"><strong>片段数:</strong> ${doc.chunk_count}</div>
                <div class="doc-info-item"><strong>上传时间:</strong> ${formatDate(doc.created_at)}</div>
            </div>
        `;

        document.getElementById('chunkList').innerHTML = chunks.chunks.map((chunk, i) => `
            <div class="chunk-item">
                <div class="chunk-header">
                    <span>片段 #${chunk.chunk_index + 1}</span>
                    <span>${chunk.char_count} 字符</span>
                </div>
                <div class="chunk-content">${escapeHtml(chunk.content)}</div>
            </div>
        `).join('');

        openModal('docDetailModal');
    } catch (error) {
        alert('加载失败: ' + error.message);
    }
}

async function deleteDocument(docId) {
    if (!confirm('确定要删除此文档吗？')) {
        return;
    }

    try {
        await apiRequest(`/rag/document/${docId}`, { method: 'DELETE' });
        loadDocumentList(currentKbId);
    } catch (error) {
        alert('删除失败: ' + error.message);
    }
}

// ==================== 文档上传 ====================

function showUploadModal() {
    if (!currentKbId) {
        alert('请先选择知识库');
        return;
    }

    document.getElementById('fileInput').value = '';
    document.getElementById('fileInfo').style.display = 'none';
    document.getElementById('previewContainer').style.display = 'none';
    document.getElementById('commitBtn').disabled = true;
    openModal('uploadModal');
}

function handleFileSelect() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];

    if (file) {
        document.getElementById('selectedFileName').textContent = file.name;
        document.getElementById('fileTypeBadge').textContent = file.name.split('.').pop().toUpperCase();
        document.getElementById('fileInfo').style.display = 'block';
    } else {
        document.getElementById('fileInfo').style.display = 'none';
    }

    document.getElementById('previewContainer').style.display = 'none';
    document.getElementById('commitBtn').disabled = true;
}

function toggleCustomSeparator() {
    const select = document.getElementById('separator_type');
    const customBox = document.getElementById('customSeparatorBox');
    customBox.style.display = select.value === 'custom' ? 'block' : 'none';
}

// 更新重叠字符数显示
document.getElementById('upload_chunk_overlap_percent')?.addEventListener('input', function() {
    const chunkSize = parseInt(document.getElementById('upload_chunk_size').value) || 800;
    const percent = parseInt(this.value) || 10;
    document.getElementById('overlap_chars').textContent = Math.floor(chunkSize * percent / 100);
});

async function previewDocument() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];

    if (!file) {
        alert('请选择文件');
        return;
    }

    const mode = 'recursive';
    const chunkSize = parseInt(document.getElementById('upload_chunk_size').value);
    const chunkOverlapPercent = parseInt(document.getElementById('upload_chunk_overlap_percent').value);
    const separatorType = document.getElementById('separator_type').value;
    const customSeparators = document.getElementById('customSeparators').value;

    // 清洗设置
    const cleanSettings = {
        compress_spaces: document.getElementById('cleanCompressSpaces').checked,
        compress_newlines: document.getElementById('cleanCompressNewlines').checked,
        remove_empty_lines: document.getElementById('cleanRemoveEmptyLines').checked,
        trim_lines: document.getElementById('cleanTrimLines').checked
    };

    const formData = new FormData();
    formData.append('file', file);
    formData.append('mode', mode);
    formData.append('chunk_size', chunkSize);
    formData.append('chunk_overlap_percent', chunkOverlapPercent);
    formData.append('separator_type', separatorType);
    if (separatorType === 'custom' && customSeparators) {
        formData.append('separators', customSeparators);
    }
    formData.append('clean_settings', JSON.stringify(cleanSettings));

    try {
        const response = await fetch(`${API_BASE}/rag/preview`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ detail: '预览失败' }));
            throw new Error(error.detail || '预览失败');
        }

        const result = await response.json();

        // 显示预览
        const previewList = document.getElementById('previewList');
        previewList.innerHTML = result.previews.map((p, i) => `
            <div class="preview-item">
                <div class="preview-header">
                    <span>片段 #${i + 1}</span>
                    <span>${p.char_count} 字符 | 分隔符: ${p.separator_hint || '-'}</span>
                </div>
                <div class="preview-content">${escapeHtml(p.content)}</div>
            </div>
        `).join('');

        document.getElementById('previewContainer').style.display = 'block';
        document.getElementById('commitBtn').disabled = false;

        // 缓存文件名用于提交
        window.currentPreviewFile = file.name;

    } catch (error) {
        alert('预览失败: ' + error.message);
    }
}

async function commitDocument() {
    if (!currentKbId) {
        alert('请先选择知识库');
        return;
    }

    const filename = window.currentPreviewFile;
    if (!filename) {
        alert('请先预览文件');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/rag/commit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `filename=${encodeURIComponent(filename)}&kb_id=${currentKbId}`
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ detail: '入库失败' }));
            throw new Error(error.detail || '入库失败');
        }

        const result = await response.json();
        alert(result.msg);
        closeModal('uploadModal');
        loadDocumentList(currentKbId);

    } catch (error) {
        alert('入库失败: ' + error.message);
    }
}

// ==================== 知识检索 ====================

async function loadKbSelect() {
    const select = document.getElementById('searchKbSelect');
    select.innerHTML = '<option value="">请选择知识库</option>';

    try {
        const result = await apiRequest('/rag/kb/list');
        if (result.kbs) {
            result.kbs.forEach(kb => {
                const option = document.createElement('option');
                option.value = kb.id;
                option.textContent = `${kb.name} (${kb.document_count || 0} 文档)`;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('加载知识库列表失败:', error);
    }
}

async function searchKnowledge() {
    const kbId = document.getElementById('searchKbSelect').value;
    const query = document.getElementById('searchQuery').value.trim();
    const k = parseInt(document.getElementById('search_k').value);

    if (!kbId) {
        alert('请选择知识库');
        return;
    }

    if (!query) {
        alert('请输入查询内容');
        return;
    }

    try {
        const result = await apiRequest('/rag/search', {
            method: 'POST',
            body: JSON.stringify({ query, k, kb_id: parseInt(kbId) })
        });

        // 显示召回率信息
        if (result.recall_info) {
            document.getElementById('recallTotal').textContent = result.recall_info.total_docs || 0;
            document.getElementById('recallRetrieved').textContent = result.recall_info.retrieved || 0;
            document.getElementById('recallRate').textContent = result.recall_info.recall_rate || '0%';
            document.getElementById('recallInfo').style.display = 'flex';
        }

        // 显示检索结果
        if (result.results && result.results.length > 0) {
            document.getElementById('resultsList').innerHTML = result.results.map((r, i) => `
                <div class="result-item">
                    <div class="result-header">
                        <span>#${i + 1}</span>
                        <span class="result-score">相似度: ${(r.score * 100).toFixed(1)}%</span>
                    </div>
                    <div class="result-content">${escapeHtml(r.content)}</div>
                    <div class="result-meta">来源: ${r.source || '-'}</div>
                </div>
            `).join('');
            document.getElementById('resultsContainer').style.display = 'block';
        } else {
            document.getElementById('resultsList').innerHTML = '<p>未找到相关内容</p>';
            document.getElementById('resultsContainer').style.display = 'block';
        }

        // 显示完整响应
        document.getElementById('searchResponse').textContent = JSON.stringify(result, null, 2);

    } catch (error) {
        document.getElementById('searchResponse').textContent = '检索失败: ' + error.message;
    }
}

async function runWorkflow() {
    const query = document.getElementById('searchQuery').value.trim();

    if (!query) {
        alert('请输入查询内容');
        return;
    }

    try {
        document.getElementById('searchResponse').textContent = '正在运行工作流...';

        const result = await apiRequest('/workflow/run', {
            method: 'POST',
            body: JSON.stringify({ question: query })
        });

        document.getElementById('searchResponse').textContent = JSON.stringify(result, null, 2);

    } catch (error) {
        document.getElementById('searchResponse').textContent = '工作流运行失败: ' + error.message;
    }
}

// ==================== 系统配置 ====================

async function loadConfig() {
    try {
        const config = await apiRequest('/admin/rag-config');

        document.getElementById('embedding_model').value = config.embedding_model || '';
        document.getElementById('rerank_model').value = config.rerank_model || '';
        document.getElementById('config_chunk_size').value = config.chunk_size || 800;
        document.getElementById('config_chunk_overlap').value = config.chunk_overlap || 80;
        document.getElementById('chunk_mode').value = config.chunk_mode || 'recursive';
        document.getElementById('search_type').value = config.search_type || 'hybrid';
        document.getElementById('weight_semantic').value = config.weight_semantic || 0.5;
        document.getElementById('use_rerank').value = config.use_rerank ? 'true' : 'false';
        document.getElementById('index_path').value = config.index_path || 'faiss_index';
        document.getElementById('retriever_k').value = config.retriever_k || 10;
        document.getElementById('bm25_k').value = config.bm25_k || 10;

        document.getElementById('configResponse').textContent = JSON.stringify(config, null, 2);

    } catch (error) {
        document.getElementById('configResponse').textContent = '加载配置失败: ' + error.message;
    }
}

async function updateConfig() {
    const config = {
        embedding_model: document.getElementById('embedding_model').value,
        rerank_model: document.getElementById('rerank_model').value,
        chunk_size: parseInt(document.getElementById('config_chunk_size').value),
        chunk_overlap: parseInt(document.getElementById('config_chunk_overlap').value),
        chunk_mode: document.getElementById('chunk_mode').value,
        search_type: document.getElementById('search_type').value,
        weight_semantic: parseFloat(document.getElementById('weight_semantic').value),
        use_rerank: document.getElementById('use_rerank').value === 'true',
        index_path: document.getElementById('index_path').value,
        retriever_k: parseInt(document.getElementById('retriever_k').value),
        bm25_k: parseInt(document.getElementById('bm25_k').value)
    };

    try {
        const result = await apiRequest('/admin/rag-config/update', {
            method: 'POST',
            body: JSON.stringify(config)
        });

        document.getElementById('configResponse').textContent = JSON.stringify(result, null, 2);

    } catch (error) {
        document.getElementById('configResponse').textContent = '更新配置失败: ' + error.message;
    }
}

// ==================== 弹窗控制 ====================

function openModal(modalId) {
    document.getElementById(modalId).classList.add('show');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// 点击弹窗外部关闭
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', function(e) {
        if (e.target === this) {
            this.classList.remove('show');
        }
    });
});

// ==================== 初始化 ====================

document.addEventListener('DOMContentLoaded', function() {
    // Tab 切换事件
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            switchTab(this.dataset.tab);
        });
    });

    // 加载知识库列表
    loadKbList();
});
