const NODE_LABEL = {
  COURSE: 'Course',
  CHAPTER: 'Chapter',
  SECTION: 'Section',
  KNOWLEDGE_POINT: 'KnowledgePoint',
}

const RELATION = {
  HAS_CHAPTER: 'HAS_CHAPTER',
  HAS_SECTION: 'HAS_SECTION',
  HAS_KNOWLEDGE_POINT: 'HAS_KNOWLEDGE_POINT',
}

export function getNodeId(node) {
  return node?.uuid ?? node?.nodeId ?? node?.id ?? null
}

export function getNodeType(node) {
  return node?._type || node?.labels?.[0] || node?.type || ''
}

export function getNodeName(node) {
  return node?.name || node?.entityName || ''
}

export function getBusinessId(node) {
  return node?.businessId ?? node?.bizId ?? node?.sourceId ?? null
}

export function getEdgeSourceId(edge) {
  if (!edge) return null
  if (edge.sourceId != null) return edge.sourceId
  if (typeof edge.source === 'object') return getNodeId(edge.source)
  return edge.source ?? null
}

export function getEdgeTargetId(edge) {
  if (!edge) return null
  if (edge.targetId != null) return edge.targetId
  if (typeof edge.target === 'object') return getNodeId(edge.target)
  return edge.target ?? null
}

function getEdgeType(edge) {
  return edge?._type || edge?.type || ''
}

function sameId(a, b) {
  return a != null && b != null && String(a) === String(b)
}

function uniqueBy(items, keyFn) {
  const seen = new Set()
  const result = []
  for (const item of items) {
    const key = keyFn(item)
    if (key == null || seen.has(String(key))) continue
    seen.add(String(key))
    result.push(item)
  }
  return result
}

function uniqueText(items) {
  return [...new Set(items.filter(Boolean).map(item => String(item).trim()).filter(Boolean))]
}

export function normalizeKnowledgePoints(value) {
  if (Array.isArray(value)) {
    return uniqueText(value)
  }
  if (typeof value === 'string') {
    return uniqueText(value.split(','))
  }
  return []
}

export function mergeKnowledgeList(available = [], preferred = []) {
  return uniqueText([
    ...normalizeKnowledgePoints(preferred),
    ...normalizeKnowledgePoints(available),
  ])
}

function findNodeById(nodes, id) {
  return (nodes || []).find(node => sameId(getNodeId(node), id)) || null
}

function findChapterByBusinessId(chapterId, chapters = []) {
  return chapters.find(chapter => sameId(chapter.id, chapterId)) || null
}

export function findSectionPathById(sectionId, chapters = []) {
  for (const chapter of chapters || []) {
    const section = (chapter.sectionList || []).find(item => sameId(item.id, sectionId))
    if (section) return { chapter, section }
  }
  return null
}

export function findSectionPathByNode(node, chapters = []) {
  return findSectionPathById(getBusinessId(node), chapters)
}

function flattenSectionPaths(chapters = []) {
  return (chapters || []).flatMap(chapter =>
    (chapter.sectionList || []).map(section => ({ chapter, section }))
  )
}

function getOutgoingTargetNodes(node, nodes, edges, relationType) {
  const nodeId = getNodeId(node)
  return (edges || [])
    .filter(edge => getEdgeType(edge) === relationType && sameId(getEdgeSourceId(edge), nodeId))
    .map(edge => findNodeById(nodes, getEdgeTargetId(edge)))
    .filter(Boolean)
}

function getIncomingSourceNodes(node, nodes, edges, relationType) {
  const nodeId = getNodeId(node)
  return (edges || [])
    .filter(edge => getEdgeType(edge) === relationType && sameId(getEdgeTargetId(edge), nodeId))
    .map(edge => findNodeById(nodes, getEdgeSourceId(edge)))
    .filter(Boolean)
}

function getKnowledgePointNamesForSectionNode(sectionNode, nodes, edges) {
  return uniqueText(
    getOutgoingTargetNodes(sectionNode, nodes, edges, RELATION.HAS_KNOWLEDGE_POINT)
      .filter(node => getNodeType(node) === NODE_LABEL.KNOWLEDGE_POINT)
      .map(getNodeName)
  )
}

function getSectionPathsForKnowledgePoint(node, nodes, edges, chapters) {
  const sectionNodes = getIncomingSourceNodes(node, nodes, edges, RELATION.HAS_KNOWLEDGE_POINT)
    .filter(item => getNodeType(item) === NODE_LABEL.SECTION)

  return uniqueBy(
    sectionNodes
      .map(sectionNode => findSectionPathByNode(sectionNode, chapters))
      .filter(Boolean),
    item => item.section?.id
  )
}

function getSectionPathsForChapter(node, nodes, edges, chapters) {
  const chapter = findChapterByBusinessId(getBusinessId(node), chapters)
  if (chapter) {
    return (chapter.sectionList || []).map(section => ({ chapter, section }))
  }

  const sectionNodes = getOutgoingTargetNodes(node, nodes, edges, RELATION.HAS_SECTION)
    .filter(item => getNodeType(item) === NODE_LABEL.SECTION)
  return uniqueBy(
    sectionNodes
      .map(sectionNode => findSectionPathByNode(sectionNode, chapters))
      .filter(Boolean),
    item => item.section?.id
  )
}

function getKnowledgePointNamesForSectionPaths(paths, nodes, edges) {
  const sectionIds = new Set(paths.map(item => String(item.section?.id)))
  const sectionNodes = (nodes || []).filter(node =>
    getNodeType(node) === NODE_LABEL.SECTION && sectionIds.has(String(getBusinessId(node)))
  )
  return uniqueText(sectionNodes.flatMap(node => getKnowledgePointNamesForSectionNode(node, nodes, edges)))
}

function buildTitles(nodeType, nodeName, primarySection) {
  if (nodeType === NODE_LABEL.SECTION && primarySection?.section?.sectionName) {
    return {
      learningTitle: `学习：${primarySection.section.sectionName}`,
      practiceTitle: `练习：${primarySection.section.sectionName}`,
    }
  }
  if (nodeType === NODE_LABEL.KNOWLEDGE_POINT && nodeName) {
    return {
      learningTitle: primarySection?.section?.sectionName
        ? `学习来源：${primarySection.section.sectionName}`
        : '查看相关课程内容',
      practiceTitle: `练习：${nodeName}`,
    }
  }
  if (nodeType === NODE_LABEL.CHAPTER && nodeName) {
    return {
      learningTitle: `学习：${nodeName}`,
      practiceTitle: `练习：${nodeName}`,
    }
  }
  return {
    learningTitle: '继续课程学习',
    practiceTitle: nodeName ? `综合练习：${nodeName}` : '课程综合练习',
  }
}

export function buildKnowledgeGraphLearningContext(selectedNode, nodes = [], edges = [], chapters = []) {
  if (!selectedNode) {
    return {
      nodeType: '',
      nodeName: '',
      sourceSections: [],
      primarySection: null,
      knowledgePoints: [],
      canOpenLearning: false,
      canOpenPractice: false,
      learningTitle: '继续课程学习',
      practiceTitle: '课程综合练习',
    }
  }

  const nodeType = getNodeType(selectedNode)
  const nodeName = getNodeName(selectedNode)
  let sourceSections = []
  let knowledgePoints = []

  if (nodeType === NODE_LABEL.KNOWLEDGE_POINT) {
    sourceSections = getSectionPathsForKnowledgePoint(selectedNode, nodes, edges, chapters)
    knowledgePoints = uniqueText([nodeName])
  } else if (nodeType === NODE_LABEL.SECTION) {
    const path = findSectionPathByNode(selectedNode, chapters)
    sourceSections = path ? [path] : []
    knowledgePoints = getKnowledgePointNamesForSectionNode(selectedNode, nodes, edges)
  } else if (nodeType === NODE_LABEL.CHAPTER) {
    sourceSections = getSectionPathsForChapter(selectedNode, nodes, edges, chapters)
    knowledgePoints = getKnowledgePointNamesForSectionPaths(sourceSections, nodes, edges)
  } else if (nodeType === NODE_LABEL.COURSE) {
    sourceSections = flattenSectionPaths(chapters)
    knowledgePoints = uniqueText(
      (nodes || [])
        .filter(node => getNodeType(node) === NODE_LABEL.KNOWLEDGE_POINT)
        .map(getNodeName)
    )
  }

  sourceSections = uniqueBy(sourceSections, item => item.section?.id)
  const primarySection = sourceSections[0] || null
  const titles = buildTitles(nodeType, nodeName, primarySection)

  return {
    nodeType,
    nodeName,
    sourceSections,
    primarySection,
    knowledgePoints,
    canOpenLearning: Boolean(primarySection?.section),
    canOpenPractice: knowledgePoints.length > 0,
    ...titles,
  }
}
