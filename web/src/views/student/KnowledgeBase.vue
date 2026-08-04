<template>
  <div class="ke-root" @click="closeContextMenu">
    <!-- ====== Sidebar ====== -->
    <aside class="ke-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="ke-brand">
        <div class="ke-brand-mark">
          <svg viewBox="0 0 24 24" fill="none" class="ke-brand-svg">
            <rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/>
            <rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/>
            <rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/>
            <rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/>
            <path d="M6.5 6.5v4M17.5 6.5v4M6.5 17.5v-4M17.5 17.5L14 14" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="ke-brand-text" v-show="!sidebarCollapsed">
          <span class="ke-brand-name">知识引擎</span>
          <span class="ke-brand-sub">Knowledge Engine</span>
        </div>
      </div>

      <button class="ke-sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'">
        <svg viewBox="0 0 16 16" fill="none"><path d="M5 3L10 8L5 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>

      <nav class="ke-nav" v-show="!sidebarCollapsed">
        <div class="ke-nav-section">
          <span class="ke-nav-label">工作空间</span>
          <span class="ke-nav-label-action" @click="openAddKb">+ 新建</span>
        </div>
        <div
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="ke-nav-item"
          :class="{ active: activeKbId === kb.id }"
          @click="switchKb(kb)"
          @contextmenu.prevent="showKbContextMenu($event, kb)"
        >
          <span class="ke-nav-dot" :style="{ background: kb.color }"></span>
          <span class="ke-nav-text">{{ kb.name }}</span>
          <span class="ke-nav-count">{{ kb.count }}</span>
        </div>
      </nav>

      <div class="ke-sidebar-footer" v-show="!sidebarCollapsed">
        <div class="ke-storage">
          <div class="ke-storage-row">
            <svg viewBox="0 0 16 16" fill="none" class="ke-storage-icon"><rect x="1.5" y="2.5" width="13" height="11" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M5 7h6" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            <span class="ke-storage-label">存储用量</span>
            <span class="ke-storage-val">{{ storageUsed }}<span class="ke-storage-total"> / {{ storageTotal }}</span></span>
          </div>
          <div class="ke-storage-bar">
            <div class="ke-storage-fill" :style="{ width: storagePercent + '%' }"></div>
          </div>
        </div>
      </div>
    </aside>

    <!-- ====== Main ====== -->
    <main class="ke-main">
      <!-- Header -->
      <header class="ke-header">
        <div class="ke-header-row">
          <div class="ke-header-left">
            <h1 class="ke-title">{{ activeKb?.name || '全部文档' }}</h1>
            <span class="ke-badge" v-if="activeKb?.description" :title="activeKb.description">{{ activeKb.description }}</span>
            <span class="ke-count">{{ documents.length }} 份文档</span>
          </div>
          <div class="ke-header-right">
            <div class="ke-search-wrap">
              <svg viewBox="0 0 20 20" fill="none" class="ke-search-icon"><circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" stroke-width="1.4"/><path d="M14 14l3.5 3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              <input v-model="searchQuery" class="ke-search" placeholder="搜索文档…" />
              <span v-if="searchQuery" class="ke-search-clear" @click="searchQuery = ''">
                <svg viewBox="0 0 16 16" fill="none"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </span>
            </div>
            <div class="ke-view-toggle">
              <button :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'" title="网格视图">
                <svg viewBox="0 0 16 16" fill="currentColor"><rect x="1.5" y="1.5" width="5" height="5" rx="1"/><rect x="9.5" y="1.5" width="5" height="5" rx="1"/><rect x="1.5" y="9.5" width="5" height="5" rx="1"/><rect x="9.5" y="9.5" width="5" height="5" rx="1"/></svg>
              </button>
              <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'" title="列表视图">
                <svg viewBox="0 0 16 16" fill="currentColor"><rect x="1" y="2" width="14" height="2.5" rx="1"/><rect x="1" y="7" width="14" height="2.5" rx="1"/><rect x="1" y="12" width="14" height="2.5" rx="1"/></svg>
              </button>
            </div>
            <label class="ke-btn ke-btn--primary" :class="{ loading: uploading }">
              <svg v-if="!uploading" viewBox="0 0 16 16" fill="none" class="ke-btn-icon"><path d="M8 11V3M8 3L5 6M8 3l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M2 12v1a2 2 0 002 2h8a2 2 0 002-2v-1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              <span class="ke-spinner" v-else></span>
              {{ uploading ? '上传中…' : '上传文档' }}
              <input type="file" accept=".pdf,.doc,.docx,.txt,.md" @change="handleUpload" :disabled="uploading" />
            </label>
          </div>
        </div>

        <!-- Stats Bar -->
        <div class="ke-stats">
          <div class="ke-stat" v-for="s in statCards" :key="s.label">
            <span class="ke-stat-icon">{{ s.icon }}</span>
            <div class="ke-stat-body">
              <div class="ke-stat-val">{{ s.value }}</div>
              <div class="ke-stat-label">{{ s.label }}</div>
            </div>
          </div>
        </div>
      </header>

      <!-- Content -->
      <div class="ke-content" :class="viewMode">
        <transition-group name="ke-card" tag="div" class="ke-cards" :class="viewMode" v-if="filteredDocuments.length > 0">
          <div
            v-for="doc in filteredDocuments"
            :key="doc.id"
            class="ke-card"
            :class="{ 'ke-card--selected': selectedDoc?.id === doc.id }"
            @click="selectedDoc = selectedDoc?.id === doc.id ? null : doc"
          >
            <div class="ke-card-preview">
              <div class="ke-card-thumb" :class="'type-' + doc.fileType">
                <span class="ke-card-thumb-icon">{{ fileTypeIcon(doc.fileType) }}</span>
                <span class="ke-card-thumb-ext">{{ doc.fileType }}</span>
              </div>
              <div class="ke-card-status" :class="embeddingClass(doc.embeddingStatus)">
                <span class="ke-status-dot"></span>
                {{ embeddingLabel(doc.embeddingStatus) }}
              </div>
            </div>
            <div class="ke-card-body">
              <div class="ke-card-name" :title="doc.fileName">{{ doc.fileName }}</div>
              <div class="ke-card-summary" v-if="viewMode === 'grid'">{{ doc.contentSummary?.slice(0, 100) || '暂无摘要' }}</div>
              <div class="ke-card-meta">
                <span>{{ doc.segmentCount }} 片段</span>
                <span class="ke-meta-sep">·</span>
                <span>{{ formatSize(doc.fileSize) }}</span>
                <span class="ke-meta-sep">·</span>
                <span>{{ formatDate(doc.createdAt) }}</span>
              </div>
            </div>
            <div class="ke-card-actions" @click.stop>
              <button class="ke-icon-btn" @click="openDocument(doc)" title="查看详情">
                <svg viewBox="0 0 16 16" fill="none"><path d="M2 8s2.5-5 6-5 6 5 6 5-2.5 5-6 5-6-5-6-5z" stroke="currentColor" stroke-width="1.3"/><circle cx="8" cy="8" r="1.8" stroke="currentColor" stroke-width="1.3"/></svg>
              </button>
              <button class="ke-icon-btn" @click="editDocument(doc)" title="编辑摘要">
                <svg viewBox="0 0 16 16" fill="none"><path d="M10.5 2.5l3 3-8 8H2.5v-3l8-8z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
              </button>
              <button class="ke-icon-btn danger" @click="confirmDelete(doc)" title="删除">
                <svg viewBox="0 0 16 16" fill="none"><path d="M2.5 4h11M5.5 4V2.5h5V4M4.5 4l.6 9h5.8l.6-9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </button>
            </div>
          </div>
        </transition-group>

        <!-- Empty State -->
        <div v-if="filteredDocuments.length === 0 && !loading" class="ke-empty">
          <div class="ke-empty-visual">
            <svg viewBox="0 0 80 80" fill="none" class="ke-empty-svg">
              <rect x="12" y="8" width="56" height="64" rx="4" stroke="currentColor" stroke-width="1.5" stroke-dasharray="3 3"/>
              <path d="M28 30h24M28 40h20M28 50h16" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="58" cy="58" r="12" fill="currentColor" fill-opacity="0.06" stroke="currentColor" stroke-width="1.2"/>
              <path d="M54 58h8M58 54v8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h3 class="ke-empty-title">{{ searchQuery ? '未找到匹配文档' : '知识库为空' }}</h3>
          <p class="ke-empty-desc">{{ searchQuery ? '尝试其他关键词搜索' : '上传 PDF、Word、Markdown 文件来构建你的知识引擎' }}</p>
          <label class="ke-btn ke-btn--primary" v-if="!searchQuery">
            <svg viewBox="0 0 16 16" fill="none" class="ke-btn-icon"><path d="M8 11V3M8 3L5 6M8 3l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M2 12v1a2 2 0 002 2h8a2 2 0 002-2v-1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            上传第一篇文档
            <input type="file" accept=".pdf,.doc,.docx,.txt,.md" @change="handleUpload" />
          </label>
        </div>
      </div>

      <!-- ====== RAG Quick Test Panel ====== -->
      <div class="ke-rag-panel" :class="{ expanded: ragExpanded }">
        <button class="ke-rag-toggle" @click="ragExpanded = !ragExpanded">
          <svg viewBox="0 0 20 20" fill="none" class="ke-rag-toggle-icon"><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.3"/><path d="M7 10.5c1-3 2-5.5 5-5.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><circle cx="9" cy="9" r="1.2" fill="currentColor"/></svg>
          <span>快速测试</span>
          <span class="ke-rag-badge" v-if="ragResult">结果就绪</span>
          <svg viewBox="0 0 16 16" fill="none" class="ke-rag-chevron" :class="{ up: ragExpanded }"><path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>

        <div class="ke-rag-body" v-show="ragExpanded">
          <div class="ke-rag-input-row">
            <input
              v-model="ragQuestion"
              class="ke-rag-input"
              placeholder="输入问题，测试知识库检索效果…"
              @keydown.enter="runRagTest"
              :disabled="ragLoading"
            />
            <div class="ke-rag-mode-select">
              <button
                v-for="m in ragModes"
                :key="m.key"
                class="ke-rag-mode-btn"
                :class="{ active: ragMode === m.key }"
                @click="ragMode = m.key"
                :title="m.tip"
              >{{ m.label }}</button>
            </div>
            <button class="ke-btn ke-btn--primary ke-rag-run" @click="runRagTest" :disabled="ragLoading || !ragQuestion.trim()">
              <span class="ke-spinner sm" v-if="ragLoading"></span>
              <svg v-else viewBox="0 0 16 16" fill="none" class="ke-btn-icon"><path d="M3 8l3.5 3.5L13 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
              {{ ragLoading ? '检索中…' : '检索' }}
            </button>
          </div>

          <!-- Pipeline Timeline -->
          <div class="ke-pipeline" v-if="ragResult">
            <div class="ke-pipeline-header">
              <span class="ke-pipeline-title">Pipeline 流水线</span>
              <span class="ke-pipeline-total">总耗时 {{ ragResult.totalTime }}ms</span>
            </div>
            <div class="ke-pipeline-bar">
              <div
                v-for="stage in ragResult.stages"
                :key="stage.name"
                class="ke-pipeline-seg"
                :class="stage.status"
                :style="{ flex: stage.time || 0.1 }"
                :title="`${stage.label}: ${stage.time}ms`"
              ></div>
            </div>
            <div class="ke-pipeline-stages">
              <div v-for="stage in ragResult.stages" :key="stage.name" class="ke-pipeline-stage" :class="stage.status">
                <span class="ke-stage-dot"></span>
                <span class="ke-stage-label">{{ stage.label }}</span>
                <span class="ke-stage-time">{{ stage.time }}ms</span>
                <span class="ke-stage-detail" v-if="stage.detail">{{ stage.detail }}</span>
              </div>
            </div>
          </div>

          <!-- RAG Results -->
          <div class="ke-rag-results" v-if="ragResult">
            <div class="ke-rag-section-label">
              检索结果
              <span class="ke-rag-result-count">{{ ragResult.sources?.length || 0 }} 条</span>
            </div>
            <div class="ke-rag-source-list" v-if="ragResult.sources?.length > 0">
              <div v-for="(src, i) in ragResult.sources" :key="i" class="ke-rag-source">
                <div class="ke-rag-source-header">
                  <span class="ke-rag-source-index">{{ i + 1 }}</span>
                  <span class="ke-rag-source-file">{{ src.fileName || '未知文件' }}</span>
                  <span class="ke-rag-source-score" :class="scoreClass(src.score)">
                    {{ (src.score * 100).toFixed(0) }}%
                  </span>
                </div>
                <div class="ke-rag-source-snippet">{{ src.snippet }}</div>
              </div>
            </div>
            <div class="ke-rag-empty" v-else>
              <span>未检索到相关内容，尝试换个问题或上传更多文档</span>
            </div>

            <!-- Answer -->
            <div class="ke-rag-answer" v-if="ragResult.answer">
              <div class="ke-rag-section-label">AI 回答</div>
              <div class="ke-rag-answer-text">{{ ragResult.answer }}</div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- ====== Document Viewer Modal ====== -->
    <transition name="ke-modal">
      <div v-if="viewerDoc" class="ke-overlay" @click.self="viewerDoc = null">
        <div class="ke-modal ke-modal--viewer">
          <div class="ke-modal-header">
            <div class="ke-modal-title-row">
              <span class="ke-modal-icon" :class="'type-' + viewerDoc.fileType">{{ fileTypeIcon(viewerDoc.fileType) }}</span>
              <div>
                <div class="ke-modal-title">{{ viewerDoc.fileName }}</div>
                <div class="ke-modal-subtitle">{{ formatSize(viewerDoc.fileSize) }} · {{ viewerDoc.segmentCount }} 片段 · {{ formatDate(viewerDoc.createdAt) }}</div>
              </div>
            </div>
            <div class="ke-modal-header-actions">
              <button class="ke-btn ke-btn--ghost sm" @click="editDocument(viewerDoc)">
                <svg viewBox="0 0 16 16" fill="none"><path d="M10.5 2.5l3 3-8 8H2.5v-3l8-8z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
                编辑
              </button>
              <button class="ke-icon-btn" @click="viewerDoc = null">✕</button>
            </div>
          </div>
          <div class="ke-modal-body ke-modal-body--viewer">
            <div class="ke-viewer-sidebar">
              <div class="ke-viewer-section-label">文档信息</div>
              <div class="ke-info-grid">
                <div class="ke-info-item"><span class="ke-info-key">类型</span><span class="ke-info-val">{{ viewerDoc.fileType?.toUpperCase() }}</span></div>
                <div class="ke-info-item"><span class="ke-info-key">大小</span><span class="ke-info-val">{{ formatSize(viewerDoc.fileSize) }}</span></div>
                <div class="ke-info-item"><span class="ke-info-key">片段</span><span class="ke-info-val">{{ viewerDoc.segmentCount }}</span></div>
                <div class="ke-info-item"><span class="ke-info-key">状态</span><span class="ke-tag" :class="embeddingClass(viewerDoc.embeddingStatus)">{{ embeddingLabel(viewerDoc.embeddingStatus) }}</span></div>
                <div class="ke-info-item"><span class="ke-info-key">创建</span><span class="ke-info-val">{{ formatDate(viewerDoc.createdAt) }}</span></div>
                <div class="ke-info-item"><span class="ke-info-key">更新</span><span class="ke-info-val">{{ formatDate(viewerDoc.updatedAt) }}</span></div>
              </div>
              <div class="ke-viewer-section-label" style="margin-top:20px">存储路径</div>
              <div class="ke-file-path">{{ viewerDoc.filePath || '—' }}</div>
            </div>
            <div class="ke-viewer-main">
              <div class="ke-viewer-section-label">内容摘要</div>
              <div class="ke-viewer-content">{{ viewerDoc.contentSummary || '暂无摘要内容' }}</div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ====== Editor Modal ====== -->
    <transition name="ke-modal">
      <div v-if="editorDoc" class="ke-overlay" @click.self="closeEditor">
        <div class="ke-modal ke-modal--editor">
          <div class="ke-modal-header">
            <div class="ke-modal-title-row">
              <span class="ke-modal-icon type-edit">✎</span>
              <div>
                <div class="ke-modal-title">编辑摘要</div>
                <div class="ke-modal-subtitle">{{ editorDoc.fileName }}</div>
              </div>
            </div>
            <button class="ke-icon-btn" @click="closeEditor">✕</button>
          </div>
          <div class="ke-toolbar">
            <div class="ke-toolbar-group">
              <button class="ke-tb-btn" @click="execCmd('bold')" title="粗体"><b>B</b></button>
              <button class="ke-tb-btn" @click="execCmd('italic')" title="斜体"><i>I</i></button>
              <button class="ke-tb-btn" @click="execCmd('underline')" title="下划线"><u>U</u></button>
              <button class="ke-tb-btn" @click="execCmd('strikeThrough')" title="删除线"><s>S</s></button>
            </div>
            <span class="ke-toolbar-sep"></span>
            <div class="ke-toolbar-group">
              <button class="ke-tb-btn" @click="execCmd('formatBlock', 'h2')" title="标题2">H2</button>
              <button class="ke-tb-btn" @click="execCmd('formatBlock', 'h3')" title="标题3">H3</button>
              <button class="ke-tb-btn" @click="execCmd('formatBlock', 'p')" title="正文">¶</button>
            </div>
            <span class="ke-toolbar-sep"></span>
            <div class="ke-toolbar-group">
              <button class="ke-tb-btn" @click="execCmd('insertUnorderedList')" title="无序列表">≡</button>
              <button class="ke-tb-btn" @click="execCmd('insertOrderedList')" title="有序列表">①</button>
              <button class="ke-tb-btn" @click="execCmd('formatBlock', 'blockquote')" title="引用">❝</button>
            </div>
            <span class="ke-toolbar-sep"></span>
            <div class="ke-toolbar-group">
              <button class="ke-tb-btn" @click="execCmd('justifyLeft')" title="左对齐">▌</button>
              <button class="ke-tb-btn" @click="execCmd('justifyCenter')" title="居中">▐</button>
            </div>
            <span class="ke-toolbar-sep"></span>
            <div class="ke-toolbar-group">
              <button class="ke-tb-btn" @click="insertLink" title="插入链接">🔗</button>
              <button class="ke-tb-btn" @click="execCmd('removeFormat')" title="清除格式">✕</button>
              <button class="ke-tb-btn" @click="execCmd('undo')" title="撤销">↩</button>
              <button class="ke-tb-btn" @click="execCmd('redo')" title="重做">↪</button>
            </div>
          </div>
          <div class="ke-modal-body ke-modal-body--editor">
            <div ref="editorRef" class="ke-editor-area" contenteditable="true" @input="onEditorInput" v-html="editorContent"></div>
            <div class="ke-editor-status">
              <span>{{ charCount }} 字符</span>
              <span v-if="editorDirty" class="ke-unsaved">● 未保存</span>
            </div>
          </div>
          <div class="ke-modal-footer">
            <button class="ke-btn ke-btn--ghost" @click="closeEditor">取消</button>
            <button class="ke-btn ke-btn--primary" @click="saveEditor" :disabled="saving">
              <span class="ke-spinner sm" v-if="saving"></span>
              {{ saving ? '保存中…' : '保存更改' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ====== Delete Confirm ====== -->
    <transition name="ke-modal">
      <div v-if="deleteTarget" class="ke-overlay" @click.self="deleteTarget = null">
        <div class="ke-modal ke-modal--confirm">
          <div class="ke-confirm-icon">
            <svg viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="1.5" stroke-dasharray="3 3"/><path d="M18 18l12 12M30 18l-12 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <h3 class="ke-confirm-title">确认删除</h3>
          <p class="ke-confirm-desc">将永久删除 <strong>{{ deleteTarget.fileName }}</strong>，该操作不可撤销。</p>
          <div class="ke-confirm-actions">
            <button class="ke-btn ke-btn--ghost" @click="deleteTarget = null">取消</button>
            <button class="ke-btn ke-btn--danger" @click="executeDelete" :disabled="deleting">
              <span class="ke-spinner sm" v-if="deleting"></span>
              {{ deleting ? '删除中…' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ====== KB Form Modal ====== -->
    <transition name="ke-modal">
      <div v-if="showAddKb" class="ke-overlay" @click.self="showAddKb = false">
        <div class="ke-modal ke-modal--form">
          <div class="ke-modal-header">
            <div class="ke-modal-title-row">
              <span class="ke-modal-icon">{{ editKb ? '✎' : '+' }}</span>
              <div>
                <div class="ke-modal-title">{{ editKb ? '编辑知识库' : '新建知识库' }}</div>
                <div class="ke-modal-subtitle">{{ editKb ? '修改知识库信息' : '创建专属知识空间' }}</div>
              </div>
            </div>
            <button class="ke-icon-btn" @click="showAddKb = false">✕</button>
          </div>
          <div class="ke-modal-body ke-modal-body--form">
            <div class="ke-form-group">
              <label class="ke-form-label">名称 <span class="ke-required">*</span></label>
              <input v-model="kbForm.name" class="ke-form-input" placeholder="例如：机器学习笔记" maxlength="50" @keydown.enter="submitKbForm" />
              <div class="ke-form-hint">{{ kbForm.name.length }}/50</div>
            </div>
            <div class="ke-form-group">
              <label class="ke-form-label">描述 <span class="ke-form-optional">可选</span></label>
              <textarea v-model="kbForm.description" class="ke-form-textarea" placeholder="简要描述知识库用途…" rows="4" maxlength="200"></textarea>
              <div class="ke-form-hint">{{ kbForm.description.length }}/200</div>
            </div>
          </div>
          <div class="ke-modal-footer">
            <button class="ke-btn ke-btn--ghost" @click="showAddKb = false">取消</button>
            <button class="ke-btn ke-btn--primary" @click="submitKbForm" :disabled="kbSubmitting">
              <span class="ke-spinner sm" v-if="kbSubmitting"></span>
              {{ kbSubmitting ? '保存中…' : (editKb ? '保存更改' : '创建知识库') }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ====== Context Menu ====== -->
    <transition name="ke-context">
      <div v-if="contextMenu.show" class="ke-context-menu" :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }" @click.stop>
        <div class="ke-context-item" @click="openEditKb(contextMenu.kb); closeContextMenu()">
          <svg viewBox="0 0 16 16" fill="none"><path d="M10.5 2.5l3 3-8 8H2.5v-3l8-8z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
          编辑知识库
        </div>
        <div class="ke-context-sep"></div>
        <div class="ke-context-item danger" @click="handleDeleteKb(contextMenu.kb); closeContextMenu()">
          <svg viewBox="0 0 16 16" fill="none"><path d="M2.5 4h11M5.5 4V2.5h5V4M4.5 4l.6 9h5.8l.6-9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
          删除知识库
        </div>
      </div>
    </transition>

    <!-- ====== Toast ====== -->
    <transition name="ke-toast">
      <div v-if="toast.show" class="ke-toast" :class="toast.type">{{ toast.message }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import {
  createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase,
  getKnowledgeBases, uploadDocument, getDocumentsByKbId, deleteDocument, getAllDocuments
} from '@/api/Knowledge.js'

// ── State ────────────────────────────────────────────────────
const studentId = ref(1)
const activeKbId = ref(null)
const activeKb = ref(null)
const documents = ref([])
const viewMode = ref('grid')
const searchQuery = ref('')
const uploading = ref(false)
const deleting = ref(false)
const saving = ref(false)
const loading = ref(false)
const viewerDoc = ref(null)
const editorDoc = ref(null)
const editorRef = ref(null)
const editorContent = ref('')
const editorDirty = ref(false)
const deleteTarget = ref(null)
const selectedDoc = ref(null)
const showAddKb = ref(false)
const editKb = ref(null)
const kbForm = ref({ name: '', description: '' })
const kbSubmitting = ref(false)
const sidebarCollapsed = ref(false)
const contextMenu = ref({ show: false, x: 0, y: 0, kb: null })
const toast = ref({ show: false, message: '', type: 'success' })
const knowledgeBases = ref([])

// RAG quick test
const ragExpanded = ref(false)
const ragQuestion = ref('')
const ragMode = ref('default')
const ragLoading = ref(false)
const ragResult = ref(null)

const ragModes = [
  { key: 'fast', label: '快速', tip: '关闭改写/HyDE/重排，优先响应速度' },
  { key: 'default', label: '标准', tip: '开启改写和重排，均衡效果与速度' },
  { key: 'high-quality', label: '深度', tip: '开启所有增强环节，优先答案质量' }
]

// ── Computed ─────────────────────────────────────────────────
const filteredDocuments = computed(() => {
  const q = searchQuery.value.toLowerCase().trim()
  if (!q) return documents.value
  return documents.value.filter(d =>
    d.fileName.toLowerCase().includes(q) ||
    (d.contentSummary || '').toLowerCase().includes(q)
  )
})

const embeddedCount = computed(() => documents.value.filter(d => d.embeddingStatus === 2).length)
const totalSegments = computed(() => documents.value.reduce((s, d) => s + (d.segmentCount || 0), 0))
const totalSize = computed(() => documents.value.reduce((s, d) => s + (d.fileSize || 0), 0))
const storageUsed = computed(() => formatSize(totalSize.value))
const storageTotal = '5 GB'
const storagePercent = computed(() => Math.min((totalSize.value / (5 * 1024 * 1024 * 1024)) * 100, 100))
const charCount = computed(() => editorRef.value?.innerText?.length || 0)

const statCards = computed(() => [
  { icon: '📄', value: documents.value.length, label: '文档总数' },
  { icon: '✓', value: embeddedCount.value, label: '已向量化' },
  { icon: '⊞', value: totalSegments.value, label: '知识片段' },
  { icon: '⊡', value: formatSize(totalSize.value), label: '总大小' }
])

// ── Lifecycle ────────────────────────────────────────────────
onMounted(async () => {
  await fetchKnowledgeBases()
  if (knowledgeBases.value.length > 0) {
    activeKbId.value = knowledgeBases.value[0].id
    activeKb.value = knowledgeBases.value[0]
    await fetchDocuments()
  }
  window.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  window.removeEventListener('click', closeContextMenu)
})

// ── Knowledge Base ───────────────────────────────────────────
async function fetchKnowledgeBases() {
  try {
    const res = await getKnowledgeBases(studentId.value)
    knowledgeBases.value = (res.data || []).map(kb => ({
      ...kb,
      color: kb.color || randomColor(),
      count: kb.documentCount || 0
    }))
  } catch (e) {
    showToast('获取知识库列表失败', 'error')
  }
}

async function fetchDocuments() {
  if (!activeKbId.value) return
  loading.value = true
  try {
    const res = await getDocumentsByKbId(activeKbId.value, studentId.value)
    documents.value = res.data || []
    const kb = knowledgeBases.value.find(k => k.id === activeKbId.value)
    if (kb) kb.count = documents.value.length
  } catch (e) {
    showToast('获取文档列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function switchKb(kb) {
  if (activeKbId.value === kb.id) return
  activeKbId.value = kb.id
  activeKb.value = kb
  selectedDoc.value = null
  ragResult.value = null
  fetchDocuments()
}

function randomColor() {
  const colors = ['#5B5FC7', '#1A8C5E', '#D4853F', '#D64045', '#7C5CBF', '#C7527A', '#2E8BC0']
  return colors[Math.floor(Math.random() * colors.length)]
}

async function handleUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    await uploadDocument(activeKbId.value, studentId.value, file)
    showToast('文档上传成功，正在向量化…')
    await fetchDocuments()
  } catch (err) {
    showToast('上传失败：' + (err.message || '未知错误'), 'error')
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

// ── KB Form ──────────────────────────────────────────────────
function openAddKb() {
  editKb.value = null
  kbForm.value = { name: '', description: '' }
  showAddKb.value = true
}

async function submitKbForm() {
  if (!kbForm.value.name.trim()) {
    showToast('请输入知识库名称', 'error')
    return
  }
  kbSubmitting.value = true
  try {
    if (editKb.value) {
      await updateKnowledgeBase(editKb.value.id, studentId.value, kbForm.value.name, kbForm.value.description)
      showToast('知识库更新成功')
    } else {
      const res = await createKnowledgeBase(studentId.value, kbForm.value.name, kbForm.value.description)
      showToast('知识库创建成功')
      const newKb = { ...res.data, color: randomColor(), count: 0 }
      knowledgeBases.value.push(newKb)
      activeKbId.value = newKb.id
      activeKb.value = newKb
      documents.value = []
    }
    showAddKb.value = false
    await fetchKnowledgeBases()
  } catch (e) {
    showToast(editKb.value ? '更新失败' : '创建失败', 'error')
  } finally {
    kbSubmitting.value = false
  }
}

function openEditKb(kb) {
  editKb.value = kb
  kbForm.value = { name: kb.name, description: kb.description || '' }
  showAddKb.value = true
}

async function handleDeleteKb(kb) {
  if (!confirm(`确认删除知识库「${kb.name}」吗？此操作将同时删除所有文档。`)) return
  try {
    await deleteKnowledgeBase(kb.id, studentId.value)
    showToast('知识库已删除')
    knowledgeBases.value = knowledgeBases.value.filter(k => k.id !== kb.id)
    if (activeKbId.value === kb.id) {
      if (knowledgeBases.value.length > 0) {
        activeKbId.value = knowledgeBases.value[0].id
        activeKb.value = knowledgeBases.value[0]
        await fetchDocuments()
      } else {
        activeKbId.value = null
        activeKb.value = null
        documents.value = []
      }
    }
  } catch (e) { showToast('删除失败', 'error') }
}

function showKbContextMenu(event, kb) {
  contextMenu.value = { show: true, x: event.clientX, y: event.clientY, kb }
}
function closeContextMenu() { contextMenu.value.show = false }

// ── Document Actions ─────────────────────────────────────────
function openDocument(doc) { viewerDoc.value = doc }
function editDocument(doc) {
  editorDoc.value = doc
  editorContent.value = doc.contentSummary || ''
  editorDirty.value = false
  viewerDoc.value = null
  nextTick(() => editorRef.value?.focus())
}
function onEditorInput() { editorDirty.value = true }
function closeEditor() {
  if (editorDirty.value && !confirm('有未保存的更改，确认关闭？')) return
  editorDoc.value = null
  editorDirty.value = false
}
async function saveEditor() {
  saving.value = true
  try {
    const content = editorRef.value?.innerHTML || ''
    await new Promise(r => setTimeout(r, 600))
    const doc = documents.value.find(d => d.id === editorDoc.value.id)
    if (doc) doc.contentSummary = editorRef.value?.innerText || content
    editorDirty.value = false
    showToast('摘要已保存')
    editorDoc.value = null
  } catch (e) { showToast('保存失败', 'error') } finally { saving.value = false }
}
function execCmd(cmd, value = null) {
  document.execCommand(cmd, false, value)
  editorRef.value?.focus()
  editorDirty.value = true
}
function insertLink() {
  const url = prompt('链接地址：', 'https://')
  if (url) execCmd('createLink', url)
}
function confirmDelete(doc) { deleteTarget.value = doc }
async function executeDelete() {
  deleting.value = true
  try {
    await deleteDocument(deleteTarget.value.id, studentId.value)
    documents.value = documents.value.filter(d => d.id !== deleteTarget.value.id)
    if (selectedDoc.value?.id === deleteTarget.value.id) selectedDoc.value = null
    showToast('文档已删除')
    deleteTarget.value = null
  } catch (e) { showToast('删除失败', 'error') } finally { deleting.value = false }
}

// ── RAG Quick Test ───────────────────────────────────────────
const RAG_API = '/knowledge/rag'

async function runRagTest() {
  if (!ragQuestion.value.trim() || ragLoading.value) return
  ragLoading.value = true
  ragResult.value = null

  const startTime = performance.now()
  const stages = [
    { name: 'rewrite', label: '查询改写', status: 'running', time: 0, detail: '' },
    { name: 'hyde', label: 'HyDE', status: 'pending', time: 0, detail: '' },
    { name: 'retrieval', label: '向量检索', status: 'pending', time: 0, detail: '' },
    { name: 'filter', label: '权限过滤', status: 'pending', time: 0, detail: '' },
    { name: 'rerank', label: '重排序', status: 'pending', time: 0, detail: '' },
    { name: 'citation', label: '引用生成', status: 'pending', time: 0, detail: '' }
  ]

  ragResult.value = { stages, sources: [], answer: '', totalTime: 0 }

  try {
    const endpoint = ragMode.value === 'fast' ? `${RAG_API}/query/fast`
      : ragMode.value === 'high-quality' ? `${RAG_API}/query/high-quality`
      : `${RAG_API}/query`

    // Dynamic import http util
    const { default: http } = await import('@/utils/http.js')
    const res = await http.post(endpoint, {
      question: ragQuestion.value,
      kbId: activeKbId.value,
      maxResults: 5,
      minScore: 0.7
    })

    const data = res.data || res
    const totalTime = Math.round(performance.now() - startTime)

    // Parse pipeline timing from backend response
    if (data.timeSummary) {
      const ms = (name) => {
        const m = data.timeSummary.match(new RegExp(`${name}:(\\d+)ms`))
        return m ? parseInt(m[1]) : 0
      }
      stages[0].time = ms('改写') || Math.round(totalTime * 0.03)
      stages[1].time = ms('HyDE') || (ragMode.value === 'high-quality' ? Math.round(totalTime * 0.30) : 0)
      stages[2].time = ms('检索') || data.retrievalTime || Math.round(totalTime * 0.15)
      stages[3].time = ms('过滤') || Math.round(totalTime * 0.01)
      stages[4].time = ms('重排') || (ragMode.value !== 'fast' ? Math.round(totalTime * 0.12) : 0)
      stages[5].time = ms('引用') || Math.round(totalTime * 0.01)
      stages[5].label = '引用'
    } else {
      // Fallback: estimate based on mode
      const estimate = ragMode.value === 'fast'
        ? [0, 0, Math.round(totalTime * 0.8), Math.round(totalTime * 0.05), 0, Math.round(totalTime * 0.02)]
        : ragMode.value === 'high-quality'
        ? [Math.round(totalTime * 0.05), Math.round(totalTime * 0.30), Math.round(totalTime * 0.10), Math.round(totalTime * 0.02), Math.round(totalTime * 0.12), Math.round(totalTime * 0.02)]
        : [Math.round(totalTime * 0.05), 0, Math.round(totalTime * 0.15), Math.round(totalTime * 0.02), Math.round(totalTime * 0.12), Math.round(totalTime * 0.02)]
      stages.forEach((s, i) => { s.time = estimate[i] || 0 })
    }

    // Mark all as completed
    stages.forEach(s => {
      s.status = s.time > 0 ? 'done' : 'skipped'
      if (s.status === 'skipped' && s.name === 'rewrite' && data.rewrittenQuestion) {
        s.status = 'done'
        s.detail = `"${data.rewrittenQuestion}"`
      }
    })
    if (data.rewrittenQuestion && data.rewrittenQuestion !== ragQuestion.value) {
      stages[0].detail = `→ "${data.rewrittenQuestion}"`
    }
    if (data.actualSearchQuery && data.actualSearchQuery !== data.rewrittenQuestion) {
      const hydeStage = stages.find(s => s.name === 'hyde')
      if (hydeStage) hydeStage.detail = '已生成假设文档'
    }

    ragResult.value = {
      stages,
      sources: (data.sources || []).map(s => ({
        fileName: s.fileName,
        score: s.score || 0,
        snippet: s.snippet || '',
        kbName: s.kbName
      })),
      answer: data.answer || '',
      totalTime,
      rewrittenQuestion: data.rewrittenQuestion
    }

  } catch (e) {
    stages.forEach(s => { if (s.status === 'running') s.status = 'error' })
    ragResult.value = { stages, sources: [], answer: '', totalTime: Math.round(performance.now() - startTime) }
    showToast('检索请求失败: ' + (e.message || '网络错误'), 'error')
  } finally {
    ragLoading.value = false
  }
}

// ── Helpers ──────────────────────────────────────────────────
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
function formatDate(str) {
  if (!str) return '—'
  return str.slice(0, 10)
}
function fileTypeIcon(type) {
  return { pdf: '📄', doc: '📝', docx: '📝', txt: '📃', md: '📋', ppt: '📊', pptx: '📊', xls: '📈', xlsx: '📈' }[type] || '📎'
}
function embeddingLabel(status) {
  return { 0: '待处理', 1: '处理中', 2: '已完成' }[status] || '未知'
}
function embeddingClass(status) {
  return { 0: 'pending', 1: 'processing', 2: 'done' }[status] || ''
}
function scoreClass(score) {
  if (score >= 0.9) return 'high'
  if (score >= 0.75) return 'mid'
  return 'low'
}
function showToast(message, type = 'success') {
  toast.value = { show: true, message, type }
  setTimeout(() => (toast.value.show = false), 3000)
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════════
   Knowledge Engine — Design System
   Palette: warm paper + iris violet accent
   ═══════════════════════════════════════════════════════════════ */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@350;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap');

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

/* ── Root ──────────────────────────────────────────────────── */
.ke-root {
  display: flex;
  height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #FAF9F6;
  color: #1B1C20;
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ── Sidebar ───────────────────────────────────────────────── */
.ke-sidebar {
  width: 248px;
  flex-shrink: 0;
  background: #FFFFFF;
  border-right: 1px solid #E7E5E0;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.22, 0.61, 0.36, 1);
  position: relative;
}
.ke-sidebar.collapsed {
  width: 60px;
}

.ke-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  border-bottom: 1px solid #E7E5E0;
}
.ke-brand-mark {
  width: 32px; height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ke-brand-svg {
  width: 24px; height: 24px;
  color: #5B5FC7;
}
.ke-brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
}
.ke-brand-name {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: #1B1C20;
  white-space: nowrap;
}
.ke-brand-sub {
  font-size: 10px;
  font-weight: 500;
  color: #B0ADA6;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.ke-sidebar-toggle {
  position: absolute;
  top: 22px;
  right: -11px;
  width: 22px; height: 22px;
  border-radius: 6px;
  border: 1px solid #E7E5E0;
  background: #FFFFFF;
  color: #8C8983;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  z-index: 2;
  transition: color 0.15s, box-shadow 0.15s;
}
.ke-sidebar-toggle:hover { color: #5B5FC7; box-shadow: 0 2px 8px rgba(91,95,199,0.12); }
.ke-sidebar-toggle svg { width: 12px; height: 12px; }
.ke-sidebar.collapsed .ke-sidebar-toggle svg { transform: rotate(180deg); }

/* Nav */
.ke-nav {
  flex: 1;
  overflow-y: auto;
  padding: 14px 12px;
}
.ke-nav::-webkit-scrollbar { width: 3px; }
.ke-nav::-webkit-scrollbar-thumb { background: #E7E5E0; border-radius: 2px; }

.ke-nav-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
  margin-bottom: 8px;
}
.ke-nav-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: #B0ADA6;
}
.ke-nav-label-action {
  font-size: 11px;
  font-weight: 600;
  color: #5B5FC7;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.15s;
}
.ke-nav-label-action:hover { background: #F3F2FD; }

.ke-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
  margin-bottom: 1px;
  user-select: none;
}
.ke-nav-item:hover { background: #F5F4F1; }
.ke-nav-item.active {
  background: #F3F2FD;
  box-shadow: inset 2px 0 0 #5B5FC7;
}
.ke-nav-dot {
  width: 7px; height: 7px;
  border-radius: 2px;
  flex-shrink: 0;
}
.ke-nav-text {
  font-size: 13px;
  font-weight: 500;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #5C5A55;
}
.ke-nav-item.active .ke-nav-text { color: #5B5FC7; font-weight: 600; }
.ke-nav-count {
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 500;
  color: #B0ADA6;
  background: #F5F4F1;
  padding: 1px 6px;
  border-radius: 4px;
  min-width: 22px;
  text-align: center;
}
.ke-nav-item.active .ke-nav-count { background: #E8E6FD; color: #5B5FC7; }

/* Sidebar Footer */
.ke-sidebar-footer {
  padding: 14px 16px;
  border-top: 1px solid #E7E5E0;
}
.ke-storage-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.ke-storage-icon { width: 13px; height: 13px; color: #B0ADA6; flex-shrink: 0; }
.ke-storage-label { font-size: 11px; color: #8C8983; font-weight: 500; }
.ke-storage-val {
  margin-left: auto;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
  color: #5C5A55;
}
.ke-storage-total { font-weight: 400; color: #B0ADA6; }
.ke-storage-bar {
  height: 3px;
  background: #EEECE6;
  border-radius: 2px;
  overflow: hidden;
}
.ke-storage-fill {
  height: 100%;
  background: linear-gradient(90deg, #5B5FC7, #989BED);
  border-radius: 2px;
  transition: width 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* ── Main ──────────────────────────────────────────────────── */
.ke-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* Header */
.ke-header {
  background: #FFFFFF;
  border-bottom: 1px solid #E7E5E0;
  flex-shrink: 0;
}
.ke-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
}
.ke-header-left { display: flex; align-items: baseline; gap: 10px; min-width: 0; }
.ke-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.4px;
  color: #1B1C20;
  white-space: nowrap;
}
.ke-badge {
  font-size: 11px;
  color: #8C8983;
  background: #F5F4F1;
  padding: 2px 8px;
  border-radius: 4px;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: default;
}
.ke-count {
  font-size: 12px;
  color: #B0ADA6;
  font-weight: 500;
}
.ke-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Search */
.ke-search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.ke-search-icon {
  position: absolute;
  left: 10px;
  width: 15px; height: 15px;
  color: #B0ADA6;
  pointer-events: none;
}
.ke-search {
  background: #F5F4F1;
  border: 1px solid #E7E5E0;
  border-radius: 8px;
  padding: 7px 28px 7px 32px;
  font-size: 13px;
  font-family: inherit;
  color: #1B1C20;
  width: 200px;
  transition: all 0.2s;
  outline: none;
}
.ke-search::placeholder { color: #C4C0B8; }
.ke-search:focus {
  background: #FFFFFF;
  border-color: #5B5FC7;
  box-shadow: 0 0 0 3px rgba(91,95,199,0.08);
}
.ke-search-clear {
  position: absolute;
  right: 6px;
  width: 18px; height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #B0ADA6;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}
.ke-search-clear:hover { color: #5C5A55; background: #E7E5E0; }
.ke-search-clear svg { width: 12px; height: 12px; }

/* View toggle */
.ke-view-toggle {
  display: flex;
  background: #F5F4F1;
  border: 1px solid #E7E5E0;
  border-radius: 8px;
  overflow: hidden;
}
.ke-view-toggle button {
  background: none;
  border: none;
  padding: 7px 9px;
  cursor: pointer;
  color: #B0ADA6;
  display: flex;
  align-items: center;
  transition: all 0.15s;
}
.ke-view-toggle button svg { width: 14px; height: 14px; }
.ke-view-toggle button.active { background: #FFFFFF; color: #5B5FC7; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.ke-view-toggle button:hover:not(.active) { color: #8C8983; }

/* Stats */
.ke-stats {
  display: flex;
  padding: 0 28px 14px;
  gap: 16px;
}
.ke-stat {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #FAF9F6;
  border: 1px solid #EEECE6;
  border-radius: 10px;
  flex: 1;
  transition: border-color 0.2s;
}
.ke-stat:hover { border-color: #D8D5CE; }
.ke-stat-icon { font-size: 18px; opacity: 0.7; line-height: 1; }
.ke-stat-body { min-width: 0; }
.ke-stat-val {
  font-size: 17px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  color: #1B1C20;
  letter-spacing: -0.3px;
}
.ke-stat-label { font-size: 11px; color: #8C8983; font-weight: 500; margin-top: 1px; }

/* ── Content ───────────────────────────────────────────────── */
.ke-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 28px 28px;
}
.ke-content::-webkit-scrollbar { width: 4px; }
.ke-content::-webkit-scrollbar-track { background: transparent; }
.ke-content::-webkit-scrollbar-thumb { background: #D8D5CE; border-radius: 2px; }

.ke-cards.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.ke-cards.list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* Card */
.ke-card {
  background: #FFFFFF;
  border: 1px solid #E7E5E0;
  border-radius: 12px;
  transition: all 0.2s;
  cursor: default;
  display: flex;
  overflow: hidden;
}
.ke-card:hover {
  border-color: #D5D1F8;
  box-shadow: 0 4px 24px rgba(91,95,199,0.06);
  transform: translateY(-1px);
}
.ke-card--selected {
  border-color: #5B5FC7 !important;
  box-shadow: 0 0 0 3px rgba(91,95,199,0.10) !important;
}

.grid .ke-card { flex-direction: column; }
.list .ke-card { flex-direction: row; align-items: center; }

/* Card Preview (thumb area) */
.ke-card-preview {
  position: relative;
}
.grid .ke-card-preview {
  padding: 20px 18px 8px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.list .ke-card-preview {
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.ke-card-thumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #FAF9F6;
}
.ke-card-thumb-icon { font-size: 20px; line-height: 1; }
.ke-card-thumb-ext {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  color: #B0ADA6;
  letter-spacing: 0.5px;
}
.list .ke-card-thumb { padding: 4px 8px; background: transparent; }
.list .ke-card-thumb-icon { font-size: 18px; }
.list .ke-card-thumb-ext { display: none; }

.ke-card-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 5px;
  white-space: nowrap;
  letter-spacing: 0.2px;
}
.grid .ke-card-status { position: absolute; top: 10px; right: 14px; }
.ke-card-status.done { background: #E6F4ED; color: #1A8C5E; }
.ke-card-status.processing { background: #FDF3E4; color: #D4853F; }
.ke-card-status.pending { background: #F3F2FD; color: #5B5FC7; }
.ke-status-dot {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: currentColor;
}

/* Card Body */
.ke-card-body {
  padding: 0 18px 14px;
  min-width: 0;
}
.list .ke-card-body {
  flex: 1;
  padding: 10px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}
.ke-card-name {
  font-size: 13px;
  font-weight: 600;
  color: #1B1C20;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
  letter-spacing: -0.2px;
}
.list .ke-card-name { margin-bottom: 0; flex: 1; }

.ke-card-summary {
  font-size: 12px;
  line-height: 1.6;
  color: #8C8983;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.ke-card-meta {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  color: #B0ADA6;
}
.list .ke-card-meta { width: 260px; flex-shrink: 0; }
.ke-meta-sep { margin: 0 4px; color: #D8D5CE; }

/* Card Actions */
.ke-card-actions {
  display: flex;
  gap: 4px;
  padding: 0 18px 14px;
}
.list .ke-card-actions { padding: 10px 14px 10px 0; }

/* Icon Button */
.ke-icon-btn {
  width: 30px; height: 30px;
  border-radius: 7px;
  border: 1px solid #E7E5E0;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #B0ADA6;
  transition: all 0.15s;
  font-size: 14px;
}
.ke-icon-btn svg { width: 13px; height: 13px; }
.ke-icon-btn:hover { background: #F5F4F1; color: #5B5FC7; border-color: #D5D1F8; }
.ke-icon-btn.danger:hover { background: #FDF2F0; color: #D64045; border-color: #F5C6C2; }

/* ── Buttons ───────────────────────────────────────────────── */
.ke-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  white-space: nowrap;
  position: relative;
  line-height: 1.4;
}
.ke-btn.sm { padding: 6px 12px; font-size: 12px; }
.ke-btn-icon { width: 14px; height: 14px; flex-shrink: 0; }
.ke-btn--primary {
  background: #5B5FC7;
  color: #FFFFFF;
  border-color: #5B5FC7;
  box-shadow: 0 1px 4px rgba(91,95,199,0.2), 0 2px 8px rgba(91,95,199,0.1);
}
.ke-btn--primary:hover { background: #4E51B5; box-shadow: 0 2px 8px rgba(91,95,199,0.25), 0 4px 16px rgba(91,95,199,0.12); }
.ke-btn--primary:active { transform: scale(0.98); }
.ke-btn--primary:disabled { opacity: 0.55; cursor: not-allowed; box-shadow: none; transform: none; }
.ke-btn--primary input { position: absolute; inset: 0; opacity: 0; cursor: pointer; font-size: 0; }

.ke-btn--ghost {
  background: transparent;
  border-color: #E7E5E0;
  color: #6F6E69;
}
.ke-btn--ghost:hover { background: #F5F4F1; color: #1B1C20; }
.ke-btn--danger {
  background: #FDF2F0;
  color: #D64045;
  border-color: #F5C6C2;
}
.ke-btn--danger:hover { background: #FBE8E5; color: #C1353A; border-color: #EEAAA5; }
.ke-btn--danger:disabled { opacity: 0.55; cursor: not-allowed; }
.ke-btn.loading { opacity: 0.7; pointer-events: none; }

/* Spinner */
.ke-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: ke-spin 0.6s linear infinite;
  display: inline-block;
}
.ke-spinner.sm { width: 11px; height: 11px; border-width: 1.5px; }

/* ── RAG Quick Test Panel ──────────────────────────────────── */
.ke-rag-panel {
  border-top: 1px solid #E7E5E0;
  background: #FFFFFF;
  flex-shrink: 0;
  transition: all 0.25s ease;
}
.ke-rag-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 28px;
  border: none;
  background: #FAF9F6;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: #5C5A55;
  cursor: pointer;
  transition: background 0.15s;
}
.ke-rag-toggle:hover { background: #F3F2FD; }
.ke-rag-toggle-icon { width: 16px; height: 16px; color: #5B5FC7; }
.ke-rag-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 3px;
  background: #E6F4ED;
  color: #1A8C5E;
  margin-left: 4px;
}
.ke-rag-chevron {
  width: 14px; height: 14px;
  margin-left: auto;
  color: #B0ADA6;
  transition: transform 0.25s ease;
}
.ke-rag-chevron.up { transform: rotate(180deg); }

.ke-rag-body {
  padding: 0 28px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ke-rag-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.ke-rag-input {
  flex: 1;
  padding: 9px 14px;
  border: 1px solid #E7E5E0;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  color: #1B1C20;
  background: #FAF9F6;
  outline: none;
  transition: all 0.2s;
}
.ke-rag-input:focus { border-color: #5B5FC7; box-shadow: 0 0 0 3px rgba(91,95,199,0.08); background: #FFFFFF; }
.ke-rag-input::placeholder { color: #C4C0B8; }

.ke-rag-mode-select {
  display: flex;
  background: #F5F4F1;
  border: 1px solid #E7E5E0;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.ke-rag-mode-btn {
  padding: 7px 12px;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
  border: none;
  background: none;
  color: #8C8983;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.ke-rag-mode-btn.active { background: #FFFFFF; color: #5B5FC7; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.ke-rag-mode-btn:hover:not(.active) { color: #5C5A55; }
.ke-rag-run { flex-shrink: 0; }

/* Pipeline */
.ke-pipeline {
  background: #FAF9F6;
  border: 1px solid #EEECE6;
  border-radius: 10px;
  padding: 14px 16px;
}
.ke-pipeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.ke-pipeline-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: #B0ADA6;
}
.ke-pipeline-total {
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  color: #8C8983;
  font-weight: 500;
}
.ke-pipeline-bar {
  display: flex;
  height: 4px;
  border-radius: 2px;
  overflow: hidden;
  background: #E7E5E0;
  gap: 1px;
  margin-bottom: 10px;
}
.ke-pipeline-seg {
  height: 100%;
  border-radius: 2px;
  transition: flex 0.5s ease;
}
.ke-pipeline-seg.done { background: #5B5FC7; }
.ke-pipeline-seg.running { background: #D4853F; animation: ke-pulse 1.2s ease-in-out infinite; }
.ke-pipeline-seg.skipped { background: #D8D5CE; }
.ke-pipeline-seg.error { background: #D64045; }

.ke-pipeline-stages {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ke-pipeline-stage {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 5px;
  background: #FFFFFF;
}
.ke-stage-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.ke-pipeline-stage.done .ke-stage-dot { background: #5B5FC7; }
.ke-pipeline-stage.done { color: #5B5FC7; }
.ke-pipeline-stage.running .ke-stage-dot { background: #D4853F; animation: ke-pulse 1.2s ease-in-out infinite; }
.ke-pipeline-stage.running { color: #D4853F; }
.ke-pipeline-stage.skipped .ke-stage-dot { background: #D8D5CE; }
.ke-pipeline-stage.skipped { color: #B0ADA6; }
.ke-pipeline-stage.error .ke-stage-dot { background: #D64045; }
.ke-pipeline-stage.error { color: #D64045; }
.ke-stage-label { font-weight: 600; white-space: nowrap; }
.ke-stage-time {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  opacity: 0.7;
}
.ke-stage-detail {
  font-size: 10px;
  color: #8C8983;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* RAG Results */
.ke-rag-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ke-rag-section-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: #B0ADA6;
  display: flex;
  align-items: center;
  gap: 8px;
}
.ke-rag-result-count {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  color: #5B5FC7;
  background: #F3F2FD;
  padding: 1px 6px;
  border-radius: 3px;
}

.ke-rag-source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ke-rag-source {
  background: #FAF9F6;
  border: 1px solid #EEECE6;
  border-radius: 8px;
  padding: 12px 14px;
}
.ke-rag-source-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.ke-rag-source-index {
  width: 20px; height: 20px;
  border-radius: 5px;
  background: #F3F2FD;
  color: #5B5FC7;
  font-size: 11px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ke-rag-source-file {
  font-size: 12px;
  font-weight: 600;
  color: #1B1C20;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ke-rag-source-score {
  font-size: 11px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  padding: 2px 7px;
  border-radius: 4px;
}
.ke-rag-source-score.high { background: #E6F4ED; color: #1A8C5E; }
.ke-rag-source-score.mid { background: #FDF3E4; color: #D4853F; }
.ke-rag-source-score.low { background: #FDF2F0; color: #D64045; }
.ke-rag-source-snippet {
  font-size: 12px;
  line-height: 1.6;
  color: #6F6E69;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ke-rag-answer {
  background: #F3F2FD;
  border: 1px solid #E0DEF8;
  border-radius: 10px;
  padding: 14px 16px;
}
.ke-rag-answer-text {
  font-size: 13px;
  line-height: 1.8;
  color: #3D3B58;
  margin-top: 6px;
}
.ke-rag-empty {
  text-align: center;
  padding: 20px;
  font-size: 12px;
  color: #B0ADA6;
}

/* ── Empty State ───────────────────────────────────────────── */
.ke-empty {
  text-align: center;
  padding: 64px 20px;
}
.ke-empty-visual { margin-bottom: 20px; }
.ke-empty-svg { width: 72px; height: 72px; color: #C4C0B8; }
.ke-empty-title { font-size: 15px; font-weight: 700; color: #6F6E69; margin-bottom: 8px; letter-spacing: -0.2px; }
.ke-empty-desc { font-size: 13px; color: #B0ADA6; margin-bottom: 24px; max-width: 320px; margin-inline: auto; line-height: 1.6; }

/* ── Modal ─────────────────────────────────────────────────── */
.ke-overlay {
  position: fixed;
  inset: 0;
  background: rgba(27,28,32,0.35);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 24px;
}
.ke-modal {
  background: #FFFFFF;
  border: 1px solid #E7E5E0;
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(27,28,32,0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
}
.ke-modal--viewer { max-width: 780px; max-height: 85vh; }
.ke-modal--editor { max-width: 840px; max-height: 90vh; }
.ke-modal--confirm { max-width: 400px; padding: 40px; text-align: center; align-items: center; }
.ke-modal--form { max-width: 480px; }

.ke-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #EEECE6;
  flex-shrink: 0;
  background: #FAF9F6;
}
.ke-modal-title-row { display: flex; align-items: center; gap: 12px; }
.ke-modal-icon { font-size: 22px; }
.ke-modal-title { font-size: 15px; font-weight: 700; color: #1B1C20; letter-spacing: -0.3px; }
.ke-modal-subtitle { font-size: 12px; color: #B0ADA6; margin-top: 1px; }
.ke-modal-header-actions { display: flex; align-items: center; gap: 8px; }

.ke-modal-body { overflow: hidden; }
.ke-modal-body--viewer {
  display: flex;
  overflow: hidden;
}
.ke-modal-body--editor {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.ke-modal-body--form { padding: 24px; }

/* Viewer */
.ke-viewer-sidebar {
  width: 210px;
  flex-shrink: 0;
  padding: 20px;
  border-right: 1px solid #EEECE6;
  overflow-y: auto;
  background: #FAF9F6;
}
.ke-viewer-main {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}
.ke-viewer-section-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: #B0ADA6;
  margin-bottom: 10px;
}
.ke-info-grid { display: flex; flex-direction: column; gap: 2px; }
.ke-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 12px;
  gap: 8px;
}
.ke-info-key { color: #B0ADA6; flex-shrink: 0; }
.ke-info-val { color: #5C5A55; font-weight: 500; text-align: right; }
.ke-file-path {
  font-size: 10px;
  font-family: 'JetBrains Mono', monospace;
  color: #B0ADA6;
  word-break: break-all;
  line-height: 1.6;
}
.ke-viewer-content {
  font-size: 13px;
  line-height: 1.8;
  color: #5C5A55;
  white-space: pre-wrap;
}
.ke-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  background: #F5F4F1;
  color: #8C8983;
}
.ke-tag.done { background: #E6F4ED; color: #1A8C5E; }
.ke-tag.processing { background: #FDF3E4; color: #D4853F; }
.ke-tag.pending { background: #F3F2FD; color: #5B5FC7; }

/* Toolbar */
.ke-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 6px 12px;
  background: #FAF9F6;
  border-bottom: 1px solid #EEECE6;
  flex-shrink: 0;
}
.ke-toolbar-group { display: flex; align-items: center; gap: 1px; }
.ke-toolbar-sep { width: 1px; height: 18px; background: #E7E5E0; margin: 0 3px; }
.ke-tb-btn {
  min-width: 26px; height: 26px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: #8C8983;
  cursor: pointer;
  font-size: 12px;
  font-family: inherit;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  transition: all 0.15s;
}
.ke-tb-btn:hover { background: #EEECE6; color: #5B5FC7; }

/* Editor */
.ke-editor-area {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  outline: none;
  font-size: 14px;
  line-height: 1.8;
  color: #3D3B45;
  min-height: 260px;
  max-height: 400px;
}
.ke-editor-area::-webkit-scrollbar { width: 4px; }
.ke-editor-area::-webkit-scrollbar-thumb { background: #E7E5E0; border-radius: 2px; }
.ke-editor-area:focus { outline: none; }
.ke-editor-area h2 { font-size: 18px; font-weight: 700; color: #1B1C20; margin: 14px 0 6px; letter-spacing: -0.3px; }
.ke-editor-area h3 { font-size: 15px; font-weight: 600; color: #3D3B45; margin: 10px 0 4px; }
.ke-editor-area p { margin: 0 0 8px; }
.ke-editor-area ul, .ke-editor-area ol { padding-left: 18px; margin-bottom: 8px; }
.ke-editor-area blockquote {
  border-left: 3px solid #5B5FC7;
  padding: 4px 14px;
  margin: 10px 0;
  color: #8C8983;
  font-style: italic;
  background: #FAF9F6;
  border-radius: 0 6px 6px 0;
}
.ke-editor-area a { color: #5B5FC7; text-decoration: underline; }

.ke-editor-status {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding: 5px 14px;
  font-size: 11px;
  color: #B0ADA6;
  border-top: 1px solid #EEECE6;
  background: #FAF9F6;
  flex-shrink: 0;
}
.ke-unsaved { color: #D4853F; font-weight: 600; }

.ke-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 18px;
  border-top: 1px solid #EEECE6;
  background: #FAF9F6;
  flex-shrink: 0;
}

/* Confirm */
.ke-confirm-icon {
  width: 56px; height: 56px;
  margin: 0 auto 16px;
  color: #D64045;
  opacity: 0.7;
}
.ke-confirm-icon svg { width: 100%; height: 100%; }
.ke-confirm-title { font-size: 16px; font-weight: 700; color: #1B1C20; margin-bottom: 8px; letter-spacing: -0.3px; }
.ke-confirm-desc { font-size: 13px; color: #8C8983; line-height: 1.7; margin-bottom: 24px; }
.ke-confirm-desc strong { color: #3D3B45; font-weight: 600; }
.ke-confirm-actions { display: flex; gap: 8px; justify-content: center; }

/* Form */
.ke-form-group { margin-bottom: 18px; }
.ke-form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1B1C20;
  margin-bottom: 6px;
}
.ke-required { color: #D64045; margin-left: 2px; }
.ke-form-optional { font-weight: 400; color: #B0ADA6; font-size: 11px; margin-left: 4px; }
.ke-form-input, .ke-form-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #E7E5E0;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  color: #1B1C20;
  background: #FFFFFF;
  transition: all 0.2s;
  outline: none;
}
.ke-form-input:focus, .ke-form-textarea:focus {
  border-color: #5B5FC7;
  box-shadow: 0 0 0 3px rgba(91,95,199,0.08);
}
.ke-form-input::placeholder, .ke-form-textarea::placeholder { color: #C4C0B8; }
.ke-form-textarea { resize: vertical; min-height: 80px; }
.ke-form-hint { font-size: 11px; color: #B0ADA6; margin-top: 4px; text-align: right; }

/* Context Menu */
.ke-context-menu {
  position: fixed;
  background: #FFFFFF;
  border: 1px solid #E7E5E0;
  border-radius: 10px;
  box-shadow: 0 12px 40px rgba(27,28,32,0.1);
  padding: 4px;
  z-index: 150;
  min-width: 150px;
}
.ke-context-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #5C5A55;
  font-weight: 500;
  transition: all 0.1s;
}
.ke-context-item svg { width: 13px; height: 13px; flex-shrink: 0; }
.ke-context-item:hover { background: #F5F4F1; color: #1B1C20; }
.ke-context-item.danger { color: #D64045; }
.ke-context-item.danger:hover { background: #FDF2F0; }
.ke-context-sep { height: 1px; background: #EEECE6; margin: 3px 8px; }

/* ── Toast ─────────────────────────────────────────────────── */
.ke-toast {
  position: fixed;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  z-index: 200;
  box-shadow: 0 8px 32px rgba(27,28,32,0.1);
  pointer-events: none;
}
.ke-toast.success { background: #E6F4ED; color: #1A8C5E; border: 1px solid #B8E4CE; }
.ke-toast.error { background: #FDF2F0; color: #D64045; border: 1px solid #F5C6C2; }

/* ── Transitions ───────────────────────────────────────────── */
.ke-modal-enter-active, .ke-modal-leave-active { transition: opacity 0.2s ease; }
.ke-modal-enter-active .ke-modal, .ke-modal-leave-active .ke-modal {
  transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}
.ke-modal-enter-from, .ke-modal-leave-to { opacity: 0; }
.ke-modal-enter-from .ke-modal { transform: scale(0.96) translateY(8px); }
.ke-modal-leave-to .ke-modal { transform: scale(0.97) translateY(4px); }

.ke-card-enter-active { transition: all 0.25s ease; }
.ke-card-leave-active { transition: all 0.2s ease; }
.ke-card-enter-from { opacity: 0; transform: translateY(8px); }
.ke-card-leave-to { opacity: 0; transform: scale(0.96); }

.ke-toast-enter-active { transition: all 0.25s ease; }
.ke-toast-leave-active { transition: all 0.2s ease; }
.ke-toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(10px); }
.ke-toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(6px); }

.ke-context-enter-active { transition: opacity 0.12s ease; }
.ke-context-leave-active { transition: opacity 0.1s ease; }
.ke-context-enter-from, .ke-context-leave-to { opacity: 0; }

@keyframes ke-spin { to { transform: rotate(360deg); } }
@keyframes ke-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
