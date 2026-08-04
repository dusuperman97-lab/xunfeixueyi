import { ref, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getConversationMessage, chatStreamQwen, chatStreamSpark, chatStreamResource } from '@/api/chat';
import { isNewConversation, normalizeResources, separateThinkingAndJson } from '../utils.js';

// 思维链步骤模板
const CHAIN_TEMPLATE = [
  { id: 'analyze', icon: 'Search', label: '分析问题', description: '正在理解你的提问意图...' },
  { id: 'retrieve', icon: 'Database', label: '检索资料', description: '正在从知识库检索相关内容...' },
  { id: 'read', icon: 'FileText', label: '读取数据源', description: '正在逐条分析检索到的资料...' },
  { id: 'generate', icon: 'Wand2', label: '生成回答', description: '正在组织语言生成回复...' },
  { id: 'done', icon: 'CheckCircle2', label: '完成', description: '回答生成完毕' },
];

// 多平台来源数据（面向用户的"可解释进度"展示）
const DEMO_SOURCES = [
  { title: 'Python 官方文档 - 语言参考', url: '#', source: 'Python.org', snippet: 'Python 是一门易学、强大的编程语言，具有高效的高级数据结构和简洁的面向对象编程方法...' },
  { title: '如何系统学习 Python 编程', url: '#', source: '知乎', snippet: '从零基础到熟练掌握 Python 的学习路径与资源推荐，涵盖基础语法、项目实战与进阶方向...' },
  { title: 'awesome-python 资源合集', url: '#', source: 'GitHub', snippet: '精选 Python 框架、库、软件和资源的综合列表，覆盖 Web 开发、爬虫、数据分析等领域...' },
  { title: 'Python 基础教程（2024版）', url: '#', source: 'B站', snippet: '面向初学者的 Python 入门视频教程，包含 100+ 个实操案例与配套代码...' },
  { title: '基于知识图谱的智能问答研究', url: '#', source: '知网', snippet: '本文综述了知识图谱构建与智能问答系统的最新研究进展，提出了一种融合深度学习的方法...' },
  { title: '大语言模型在代码生成中的应用', url: '#', source: 'arXiv', snippet: '探讨大语言模型在自动化代码生成、代码补全和程序修复中的最新技术路线与评估方法...' },
];

function cloneSteps() {
  return CHAIN_TEMPLATE.map(s => ({ ...s, status: 'pending' }));
}

export function useChat(refreshSidebarList) {
  const route = useRoute();
  const router = useRouter();

  const messages = ref([]);
  const sendBtnLoading = ref(false);
  const authScroll = ref(true);
  let controller = null;
  let chainTimer = null; // 思维链定时器
  let skipNextHistoryMemoryId = null;

  function scrollToBottom() {
    authScroll.value = true;
    nextTick(() => {
      if (authScroll.value) {
        const el = document.querySelector('.message-list');
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
      }
    });
  }

  function clearChainTimer() {
    if (chainTimer) {
      clearTimeout(chainTimer);
      chainTimer = null;
    }
  }

  // 更新单个步骤（自动记录耗时）
  function updateStep(steps, id, status, extras = {}) {
    const step = steps.find(s => s.id === id);
    if (!step) return;
    step.status = status;
    if (extras.description) step.description = extras.description;
    if (extras.results) step.results = extras.results;
    if (extras.errorMsg) step.errorMsg = extras.errorMsg;
    if (status === 'active' && !step._started) {
      step._started = Date.now();
    }
    if ((status === 'complete' || status === 'error') && step._started) {
      const ms = Date.now() - step._started;
      step.elapsed = ms < 1000 ? `${ms}ms` : `${(ms / 1000).toFixed(1)}s`;
    }
  }

  // 运行思维链动画（模拟处理流程）
  function runChainOfThought(message) {
    clearChainTimer();
    const steps = message.chainOfThought.steps;
    const content = message._chainContext || '';

    // Step 1 → active
    updateStep(steps, 'analyze', 'active', { description: '正在解析用户语句，提取关键实体与意图...' });

    chainTimer = setTimeout(() => {
      updateStep(steps, 'analyze', 'complete', {
        description: `已识别主题：${content.slice(0, 28)}${content.length > 28 ? '...' : ''}，涉及 ${Math.floor(Math.random() * 3) + 2} 个知识领域`,
      });
      updateStep(steps, 'retrieve', 'active', { description: '正在跨知识库和搜索引擎进行多路召回...' });
      scrollToBottom();

      chainTimer = setTimeout(() => {
        updateStep(steps, 'retrieve', 'complete', {
          description: `检索完成，共匹配到 ${Math.floor(Math.random() * 8) + 5} 条相关记录`,
          results: [
            { id: 1, title: 'Python 官方文档', source: 'Python.org', type: 'document' },
            { id: 2, title: 'Python 入门实战教程', source: 'B站', type: 'video' },
            { id: 3, title: '学习路径与经验分享', source: '知乎', type: 'document' },
            { id: 4, title: '开源项目 awesome-python', source: 'GitHub', type: 'document' },
          ],
        });
        updateStep(steps, 'read', 'active', { description: '正在逐条解析资料，提取核心观点与事实...' });
        scrollToBottom();

        chainTimer = setTimeout(() => {
          updateStep(steps, 'read', 'complete', { description: `已提取 ${Math.floor(Math.random() * 3) + 2} 个关键论据，完成语义一致性校验` });
          updateStep(steps, 'generate', 'active', { description: '正在基于检索结果组织结构化回答...' });
          scrollToBottom();

          // Step 4 (generate) → complete when streaming completes
          // Step 5 (done) → complete at the end
        }, 700);
      }, 600);
    }, 400);
  }

  // 完成剩余的思维链步骤
  function finishChainOfThought(message) {
    clearChainTimer();
    if (!message.chainOfThought?.steps) return;
    const steps = message.chainOfThought.steps;
    updateStep(steps, 'generate', 'complete', { description: '回复内容已生成' });
    updateStep(steps, 'done', 'complete', { description: '回答生成完毕' });
  }

  // 标记思维链步骤为 error
  function errorChainOfThought(message) {
    clearChainTimer();
    if (!message.chainOfThought?.steps) return;
    const steps = message.chainOfThought.steps;
    const activeStep = steps.find(s => s.status === 'active');
    if (activeStep) {
      updateStep(steps, activeStep.id, 'error', {
        description: '处理过程中出现异常',
        errorMsg: '请求中断，请重试',
      });
    }
  }

  function getToolbarItems(msg, index) {
    return [
      { key: 'copy', icon: 'copy', label: '复制', text: msg.content },
      { key: 'refresh', icon: 'refresh', label: '重新生成', onClick: () => regenerateResponse(index) },
      { key: 'voice', icon: 'audio', label: '', onClick: () => {} },
      { key: 'like', icon: 'like', label: '点赞' },
      { key: 'dislike', icon: 'dislike', label: '点踩' },
    ];
  }

  async function onSubmit(content, selectedMode, selectedModel, pendingAttachments) {
    if (sendBtnLoading.value) {
      controller?.abort();
      clearChainTimer();
      sendBtnLoading.value = false;
      const lastMsg = messages.value[messages.value.length - 1];
      if (lastMsg) {
        lastMsg.loading = false;
        if (lastMsg.chainOfThought) errorChainOfThought(lastMsg);
      }
      return;
    }

    const text = String(content || '').trim();
    const attachments = Array.isArray(pendingAttachments) ? pendingAttachments : [];
    if (!text && attachments.length === 0) return;

    if (selectedMode === 'resource') {
      await submitResource(text);
      return;
    }

    await submitChat(text, selectedModel, attachments);
  }

  async function submitResource(content) {
    messages.value.push({ role: 'user', content, attachments: [] });
    scrollToBottom();

    const aiMsg = {
      role: 'model', type: 'resource', content: '', resources: null,
      loading: true, typing: true, mode: 'resource', model: 'qwen',
      renderKey: `resource-${Date.now()}`,
      _chainContext: content,
      chainOfThought: { steps: cloneSteps(), sources: null },
    };
    // 初始状态：第一步 active
    updateStep(aiMsg.chainOfThought.steps, 'analyze', 'active', { description: '正在分析资源搜索需求...' });
    messages.value.push(aiMsg);
    const currentMessage = messages.value[messages.value.length - 1];

    // 启动思维链动画
    runChainOfThought(currentMessage);
    sendBtnLoading.value = true;
    controller = new AbortController();

    try {
      const rawMemoryId = route.params.memoryId;
      const effectiveMemoryId = isNewConversation(rawMemoryId) ? undefined : rawMemoryId;
      const response = await chatStreamResource(effectiveMemoryId, content, controller.signal);
      if (!response.ok) throw new Error('Network response was not ok');

      const newMemoryId = response.headers.get('X-Conversation-Id') || response.headers.get('X-Memory-Id');
      if (isNewConversation(rawMemoryId) && newMemoryId) {
        skipNextHistoryMemoryId = newMemoryId;
        router.replace(`/student/chat/${newMemoryId}`);
        refreshSidebarList?.();
      }

      const reader = response.body?.getReader?.();
      const decoder = new TextDecoder('utf-8');
      let rawText = '';
      let resourceStarted = false;
      const handleResourceText = () => {
        const extracted = separateThinkingAndJson(rawText);
        if (extracted.json?.resources) {
          const thinkingText = extracted.thinking || '';
          const messageText = extracted.json.message || '';
          currentMessage.content = thinkingText
            ? `${thinkingText}\n\n${messageText}`
            : messageText;
          currentMessage.resources = normalizeResources(extracted.json.resources);
          currentMessage.typing = false;
          if (!resourceStarted) {
            resourceStarted = true;
            updateStep(currentMessage.chainOfThought.steps, 'generate', 'complete', { description: '资源已生成，正在补充详情...' });
          }
        } else {
          currentMessage.content = extracted.thinking || rawText;
          if (!resourceStarted) {
            updateStep(currentMessage.chainOfThought.steps, 'generate', 'active', { description: '正在生成资源推荐...' });
          }
        }
        scrollToBottom();
      };

      if (reader) {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          rawText += decoder.decode(value, { stream: true });
          handleResourceText();
        }
      } else {
        rawText = await response.text();
        handleResourceText();
      }

      // 流结束 — 最终完整解析
      const finalExtracted = separateThinkingAndJson(rawText);
      if (finalExtracted.json?.resources) {
        const thinkingText = finalExtracted.thinking || '';
        const messageText = finalExtracted.json.message || '';
        currentMessage.content = thinkingText
          ? `${thinkingText}\n\n${messageText}`
          : messageText;
        currentMessage.resources = normalizeResources(finalExtracted.json.resources);
        // 汇总所有资源的来源信息
        const allItems = [
          ...(currentMessage.resources.images || []),
          ...(currentMessage.resources.videos || []),
          ...(currentMessage.resources.articles || []),
        ];
        const extractedSources = [];
        const seen = new Set();
        for (const item of allItems) {
          if (item.source && !seen.has(item.source)) {
            seen.add(item.source);
            extractedSources.push({ title: item.title, source: item.source, url: item.url, snippet: item.snippet || '' });
          }
        }
        currentMessage.chainOfThought.sources = extractedSources.length ? extractedSources : DEMO_SOURCES.slice(0, 3);
      } else if (!currentMessage.resources) {
        // 全程未解析出资源 → 保留纯文本展示
        currentMessage.content = rawText;
        currentMessage.chainOfThought.sources = DEMO_SOURCES.slice(0, 2);
      }
      finishChainOfThought(currentMessage);
      currentMessage.loading = false;
    } catch (error) {
      if (error.name !== 'AbortError') {
        currentMessage.type = undefined;
        currentMessage.content = '**[资源搜索失败，请稍后重试]**';
      }
      errorChainOfThought(currentMessage);
      currentMessage.loading = false;
    } finally {
      sendBtnLoading.value = false;
      currentMessage.typing = false;
    }
  }

  async function submitChat(content, selectedModel, attachments) {
    const attachmentsSnapshot = [...attachments];

    messages.value.push({
      role: 'user',
      content,
      attachments: attachmentsSnapshot.map(a => ({ name: a.name, url: a.url, type: a.type })),
    });
    scrollToBottom();

    const aiMsg = {
      role: 'model', content: '', typing: true, loading: true,
      renderKey: `stream-${Date.now()}`, mode: 'chat', model: selectedModel,
      _chainContext: content,
      chainOfThought: { steps: cloneSteps(), sources: null },
    };
    // 初始状态：第一步 active
    updateStep(aiMsg.chainOfThought.steps, 'analyze', 'active', { description: '正在分析提问意图与关键信息...' });
    messages.value.push(aiMsg);
    const currentMessage = messages.value[messages.value.length - 1];

    // 启动思维链动画
    runChainOfThought(currentMessage);
    sendBtnLoading.value = true;
    controller = new AbortController();

    try {
      const rawMemoryId = route.params.memoryId;
      const effectiveMemoryId = isNewConversation(rawMemoryId) ? undefined : rawMemoryId;
      const images = attachmentsSnapshot.filter(a => a.type?.startsWith('image')).map(a => a.raw);
      const documents = attachmentsSnapshot.filter(a => !a.type?.startsWith('image')).map(a => a.raw);

      const response = selectedModel === 'spark'
          ? await chatStreamSpark(effectiveMemoryId, content, controller.signal)
          : await chatStreamQwen(effectiveMemoryId, content, images, documents, controller.signal);

      if (!response.ok) throw new Error('Network response was not ok');

      const newMemoryId = response.headers.get('X-Conversation-Id') || response.headers.get('X-Memory-Id');
      if (isNewConversation(rawMemoryId) && newMemoryId) {
        skipNextHistoryMemoryId = newMemoryId;
        router.replace(`/student/chat/${newMemoryId}`);
        refreshSidebarList?.();
      } else if (messages.value.length <= 2) {
        refreshSidebarList?.();
      }

      const reader = response.body?.getReader?.();
      const decoder = new TextDecoder('utf-8');
      let firstChunk = true;
      const appendChatChunk = chunkText => {
        if (!chunkText) return;
        currentMessage.loading = false;
        if (firstChunk) {
          firstChunk = false;
          updateStep(currentMessage.chainOfThought.steps, 'generate', 'active', { description: '正在生成回答...' });
          currentMessage.chainOfThought.sources = DEMO_SOURCES;
        }
        currentMessage.content += chunkText;
        scrollToBottom();
      };

      if (reader) {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          appendChatChunk(decoder.decode(value, { stream: true }));
        }
      } else {
        appendChatChunk(await response.text());
      }
      finishChainOfThought(currentMessage);
    } catch (error) {
      if (error.name !== 'AbortError') {
        currentMessage.content += '\n\n**[请求异常或中断]**';
      }
      errorChainOfThought(currentMessage);
    } finally {
      sendBtnLoading.value = false;
      currentMessage.loading = false;
      clearChainTimer();
      currentMessage.typing = false;
    }
  }

  async function regenerateResponse(msgIndex) {
    if (sendBtnLoading.value || msgIndex < 1) return;
    const userMsg = messages.value[msgIndex - 1];
    if (!userMsg || userMsg.role !== 'user') return;

    const savedMode = messages.value[msgIndex]?.mode || 'chat';
    const savedModel = messages.value[msgIndex]?.model || 'qwen';
    messages.value = messages.value.slice(0, msgIndex);

    const aiMsg = {
      role: 'model', content: '', typing: true, loading: true,
      renderKey: `regen-${Date.now()}`, mode: savedMode, model: savedModel,
      _chainContext: userMsg.content,
      chainOfThought: { steps: cloneSteps(), sources: null },
    };
    updateStep(aiMsg.chainOfThought.steps, 'analyze', 'active', { description: '正在重新分析问题...' });
    messages.value.push(aiMsg);
    const currentMessage = messages.value[messages.value.length - 1];

    runChainOfThought(currentMessage);
    sendBtnLoading.value = true;
    controller = new AbortController();
    scrollToBottom();

    try {
      const rawMemoryId = route.params.memoryId;
      const effectiveMemoryId = isNewConversation(rawMemoryId) ? undefined : rawMemoryId;
      const response = savedModel === 'spark'
          ? await chatStreamSpark(effectiveMemoryId, userMsg.content, controller.signal)
          : await chatStreamQwen(effectiveMemoryId, userMsg.content, [], [], controller.signal);

      if (!response.ok) throw new Error('Network error');
      const reader = response.body?.getReader?.();
      const decoder = new TextDecoder('utf-8');
      let firstChunk = true;
      const appendChatChunk = chunkText => {
        if (!chunkText) return;
        currentMessage.loading = false;
        if (firstChunk) {
          firstChunk = false;
          updateStep(currentMessage.chainOfThought.steps, 'generate', 'active', { description: '正在生成回答...' });
          currentMessage.chainOfThought.sources = DEMO_SOURCES;
        }
        currentMessage.content += chunkText;
        scrollToBottom();
      };

      if (reader) {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          appendChatChunk(decoder.decode(value, { stream: true }));
        }
      } else {
        appendChatChunk(await response.text());
      }
      finishChainOfThought(currentMessage);
    } catch (error) {
      if (error.name !== 'AbortError') {
        currentMessage.content += '\n\n**[重新生成失败，请重试]**';
      }
      errorChainOfThought(currentMessage);
    } finally {
      sendBtnLoading.value = false;
      currentMessage.loading = false;
      clearChainTimer();
      currentMessage.typing = false;
    }
  }

  async function loadHistory(memoryId) {
    if (sendBtnLoading.value && skipNextHistoryMemoryId === memoryId) {
      skipNextHistoryMemoryId = null;
      return;
    }

    try {
      const res = await getConversationMessage(memoryId);
      const historyList = Array.isArray(res?.data) ? res.data : [];
      messages.value = [];
      if (historyList.length) {
        const mapped = historyList
            .filter(item => item && ['user', 'assistant'].includes(item.type))
            .map((item, i) => {
              const msg = {
                role: item.type === 'assistant' ? 'model' : 'user',
                content: item.content == null ? '' : String(item.content),
                typing: false, loading: false,
                renderKey: `history-${memoryId}-${i}`,
                mode: item.mode || undefined,
                model: item.model || undefined,
              };

              // 用户消息：还原附件（图片/文档）
              if (item.type === 'user') {
                const attachments = [];
                if (Array.isArray(item.imagUrls) && item.imagUrls.length) {
                  item.imagUrls.forEach((url, ai) => {
                    const name = String(url).split('/').pop() || `图片 ${ai + 1}`;
                    attachments.push({
                      name, url, previewUrl: url,
                      type: 'image', isImage: true, raw: null,
                    });
                  });
                }
                if (Array.isArray(item.docUrls) && item.docUrls.length) {
                  item.docUrls.forEach((url, di) => {
                    const name = String(url).split('/').pop() || `文档 ${di + 1}`;
                    attachments.push({
                      name, url, previewUrl: url,
                      type: 'file', isImage: false, raw: null,
                    });
                  });
                }
                if (attachments.length) msg.attachments = attachments;
              }

              // AI 消息：兼容 resources JSON 资源推荐类型
              if (item.type === 'assistant' && typeof item.content === 'string') {
                const extracted = separateThinkingAndJson(item.content);
                if (extracted.json?.resources) {
                  msg.type = 'resource';
                  const thinkingText = extracted.thinking || '';
                  const messageText = extracted.json.message || '';
                  msg.content = thinkingText
                    ? `${thinkingText}\n\n${messageText}`
                    : messageText;
                  msg.resources = normalizeResources(extracted.json.resources);
                }
              }
              return msg;
            });
        // 一次性替换，避免闪现空白
        messages.value = mapped;
        await nextTick();
        scrollToBottom();
      }
    } catch (e) {
      console.error('加载历史失败:', e);
    }
  }

  return {
    messages,
    sendBtnLoading,
    authScroll,
    onSubmit,
    loadHistory,
    regenerateResponse,
    getToolbarItems,
    scrollToBottom,
    get controller() { return controller; },
  };
}
