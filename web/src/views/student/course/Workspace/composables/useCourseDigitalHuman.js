import { ref } from 'vue'
import { streamCourseDigitalHuman } from '@/api/courseDigitalHuman.js'

function unrefValue(value) {
  return typeof value === 'function' ? value() : value?.value ?? value
}

function parsePayload(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return raw
  }
}

function payloadText(payload) {
  if (typeof payload === 'string') return payload
  return payload?.delta ?? payload?.content ?? payload?.text ?? ''
}

/** 按 SSE 空行事件边界解析，兼容 CRLF、分块拆帧、多行 data 与无 event 字段。 */
async function consumeEventStream(response, { signal, onEvent }) {
  if (!response.body) throw new Error('浏览器未提供可读取的流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatch = (frame) => {
    const lines = frame.replace(/\r/g, '').split('\n')
    let event = 'message'
    const data = []
    for (const line of lines) {
      if (!line || line.startsWith(':')) continue
      const separator = line.indexOf(':')
      const field = separator < 0 ? line : line.slice(0, separator)
      let value = separator < 0 ? '' : line.slice(separator + 1)
      if (value.startsWith(' ')) value = value.slice(1)
      if (field === 'event') event = value || 'message'
      if (field === 'data') data.push(value)
    }
    if (data.length || event !== 'message') onEvent(event, data.join('\n'))
  }

  try {
    while (true) {
      if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
      const { done, value } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      buffer = buffer.replace(/\r\n/g, '\n')

      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        dispatch(buffer.slice(0, boundary))
        buffer = buffer.slice(boundary + 2)
        boundary = buffer.indexOf('\n\n')
      }
      if (done) break
    }
    if (buffer.trim()) dispatch(buffer)
  } finally {
    reader.releaseLock()
  }
}

export function useCourseDigitalHuman(options = {}) {
  const messages = ref([])
  const streaming = ref(false)
  const meta = ref(null)
  const lastError = ref(null)
  const memoryId = ref(null)
  let controller = null
  let generation = 0

  const notifyError = (error) => {
    lastError.value = error
    options.onError?.(error)
  }

  function interrupt() {
    generation += 1
    controller?.abort()
    controller = null
    streaming.value = false
    const pending = [...messages.value].reverse().find(item => item.role === 'assistant' && item.loading)
    if (pending) {
      pending.loading = false
      pending.typing = false
      pending.interrupted = true
    }
  }

  function clear() {
    interrupt()
    messages.value = []
    meta.value = null
    memoryId.value = null
    lastError.value = null
  }

  function resetContext({ label = '新小节', preserveMemory = true, preserveMessages = true } = {}) {
    interrupt()
    meta.value = null
    lastError.value = null
    if (!preserveMemory) memoryId.value = null
    if (!preserveMessages) {
      messages.value = []
      return
    }
    if (messages.value.length) {
      messages.value.push({
        id: `course-dh-context-${Date.now()}`,
        role: 'system',
        content: `已切换至：${label}`,
        separator: true,
      })
      options.onUpdate?.()
    }
  }

  async function ask(question, askOptions = {}) {
    const text = String(question ?? '').trim()
    if (!text) return null

    const digitalHumanId = unrefValue(options.digitalHumanId)
    const course = unrefValue(options.course)
    const chapter = unrefValue(options.currentChapter)
    const section = unrefValue(options.currentSection)
    if (!digitalHumanId) throw new Error('请选择课程数字人')
    if (!course?.id) throw new Error('课程信息尚未加载')
    if (!chapter?.id) throw new Error('请先选择课程章节')
    if (!section?.id) throw new Error('请先选择课程小节')

    interrupt()
    const requestGeneration = ++generation
    controller = new AbortController()
    const activeController = controller
    streaming.value = true
    lastError.value = null

    const displayText = String(askOptions.displayText ?? text).trim()
    if (askOptions.showUser !== false) {
      messages.value.push({ role: 'user', content: displayText })
    }
    const assistantMessage = {
      role: 'assistant',
      content: '',
      loading: true,
      typing: true,
      id: `course-dh-${Date.now()}-${requestGeneration}`,
    }
    messages.value.push(assistantMessage)
    options.onUpdate?.()

    const isCurrent = () => generation === requestGeneration && !activeController.signal.aborted
    let completed = false

    try {
      const response = await streamCourseDigitalHuman({
        memoryId: memoryId.value,
        courseId: course.id,
        chapterId: chapter?.id ?? null,
        sectionId: section?.id ?? null,
        digitalHumanId: String(digitalHumanId),
        type: askOptions.type || 'FREE_QA',
        message: text,
        ...(askOptions.selection ? { selection: askOptions.selection } : {}),
        ...(askOptions.practice ? { practice: askOptions.practice } : {}),
      }, { signal: activeController.signal })

      if (!response.ok) {
        const detail = await response.text().catch(() => '')
        throw new Error(detail || `课程数字人请求失败（HTTP ${response.status}）`)
      }

      await consumeEventStream(response, {
        signal: activeController.signal,
        onEvent(eventName, rawData) {
          if (!isCurrent()) return
          const payload = parsePayload(rawData)
          const event = eventName === 'message'
            ? (payload?.event || payload?.type || 'delta')
            : eventName

          if (event === 'meta') {
            meta.value = typeof payload === 'object' && payload ? payload : { value: payload }
            if (meta.value.memoryId) memoryId.value = meta.value.memoryId
          } else if (event === 'delta') {
            assistantMessage.content += payloadText(payload)
            assistantMessage.loading = false
          } else if (event === 'done') {
            const finalText = payloadText(payload)
            if (finalText && !assistantMessage.content) assistantMessage.content = finalText
            completed = true
          } else if (event === 'error') {
            const message = payload?.message || payload?.error || payloadText(payload) || '课程数字人响应异常'
            throw new Error(message)
          }
          options.onUpdate?.()
        },
      })

      if (!isCurrent()) return null
      completed = true
      assistantMessage.loading = false
      assistantMessage.typing = false
      if (!assistantMessage.content) assistantMessage.content = '暂未生成有效回答，请稍后重试。'
      await options.onComplete?.(assistantMessage.content, { message: assistantMessage, meta: meta.value })
      return assistantMessage.content
    } catch (error) {
      if (error?.name === 'AbortError' || !isCurrent()) return null
      assistantMessage.loading = false
      assistantMessage.typing = false
      assistantMessage.error = true
      assistantMessage.content ||= '请求失败，请稍后重试。'
      notifyError(error)
      throw error
    } finally {
      if (generation === requestGeneration) {
        assistantMessage.loading = false
        assistantMessage.typing = false
        streaming.value = false
        controller = null
        if (!completed && activeController.signal.aborted) assistantMessage.interrupted = true
        options.onUpdate?.()
      }
    }
  }

  return { messages, streaming, meta, memoryId, lastError, ask, interrupt, clear, resetContext }
}
