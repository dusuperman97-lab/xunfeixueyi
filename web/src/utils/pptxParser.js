/**
 * Browser-side PPTX parser using JSZip.
 * PPTX is a ZIP of XML files — parse slide text, images, and layout.
 */
import JSZip from 'jszip'

const EMU_PER_PX = 9525

function emuToPx(emu) {
  return Math.round(emu / EMU_PER_PX)
}

const XML_NS = {
  a: 'http://schemas.openxmlformats.org/drawingml/2006/main',
  r: 'http://schemas.openxmlformats.org/officeDocument/2006/relationships',
  p: 'http://schemas.openxmlformats.org/presentationml/2006/main',
}

function nsResolver(prefix) {
  return XML_NS[prefix] || null
}

function xpath(el, path) {
  const parts = path.split('/')
  let node = el
  for (const part of parts) {
    const [prefix, local] = part.includes(':') ? part.split(':') : ['', part]
    const ns = prefix ? XML_NS[prefix] : null
    const children = Array.from(node.children || []).filter(
      c => c.nodeName === local || (ns && c.namespaceURI === ns && c.localName === local)
    )
    if (children.length === 0) return null
    node = children[0]
  }
  return node
}

function xpathAll(el, path) {
  const parts = path.split('/')
  function descend(node, depth) {
    if (depth >= parts.length) return [node]
    const [prefix, local] = parts[depth].includes(':')
      ? parts[depth].split(':')
      : ['', parts[depth]]
    const ns = prefix ? XML_NS[prefix] : null
    const results = []
    for (const child of Array.from(node.children || [])) {
      const match =
        child.nodeName === local ||
        (ns && child.namespaceURI === ns && child.localName === local)
      if (match) {
        results.push(...descend(child, depth + 1))
      }
    }
    return results
  }
  return descend(el, 0)
}

function getAttr(node, localName, nsUri) {
  if (!node) return null
  if (nsUri) {
    return node.getAttributeNS(nsUri, localName) || node.getAttribute(localName)
  }
  return node.getAttribute(localName)
}

function parseRuns(txBodyEl) {
  if (!txBodyEl) return ''
  const paragraphs = xpathAll(txBodyEl, 'a:p')
  if (!paragraphs.length) return ''

  let html = ''
  for (let pi = 0; pi < paragraphs.length; pi++) {
    const p = paragraphs[pi]
    const runs = xpathAll(p, 'a:r')
    if (!runs.length) {
      html += '<br>'
      continue
    }
    for (const r of runs) {
      const rPr = xpath(r, 'a:rPr')
      let style = ''
      if (rPr) {
        const sz = getAttr(rPr, 'sz', XML_NS.a)
        if (sz) style += `font-size:${parseInt(sz) / 100}pt;`
        const b = getAttr(rPr, 'b', XML_NS.a)
        if (b === '1' || b === 'true') style += 'font-weight:bold;'
        const i = getAttr(rPr, 'i', XML_NS.a)
        if (i === '1' || i === 'true') style += 'font-style:italic;'
        const clr = xpath(rPr, 'a:solidFill/a:srgbClr')
        if (clr) {
          const val = getAttr(clr, 'val', XML_NS.a)
          if (val) style += `color:#${val};`
        }
        const latin = xpath(rPr, 'a:latin')
        if (latin) {
          const typeface = getAttr(latin, 'typeface', XML_NS.a)
          if (typeface) style += `font-family:${typeface};`
        }
      }
      const t = xpath(r, 'a:t')
      if (t && t.textContent !== null) {
        let text = t.textContent
        text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        // Preserve newlines
        text = text.replace(/\n/g, '<br>')
        html += style ? `<span style="${style}">${text}</span>` : text
      }
    }
    if (pi < paragraphs.length - 1) {
      html += '<br>'
    }
  }
  return html
}

function parseSlideBackground(slideEl) {
  const bg = xpath(slideEl, 'p:cSld/p:bg/p:bgPr')
  if (!bg) return null
  const solidFill = xpath(bg, 'a:solidFill')
  if (solidFill) {
    const srgbClr = xpath(solidFill, 'a:srgbClr')
    if (srgbClr) {
      const val = getAttr(srgbClr, 'val', XML_NS.a)
      if (val) return `#${val}`
    }
  }
  return null
}

function extractImageRels(relsXml) {
  const map = {}
  if (!relsXml) return map
  const relationships = Array.from(relsXml.querySelectorAll('Relationship'))
  for (const rel of relationships) {
    const target = rel.getAttribute('Target')
    const id = rel.getAttribute('Id')
    if (target && id) {
      map[id] = target
    }
  }
  return map
}

export async function parsePptx(arrayBuffer, { onProgress } = {}) {
  const zip = await JSZip.loadAsync(arrayBuffer)
  const slides = []
  const imageCache = {}

  // Build slide list
  const slideFiles = Object.keys(zip.files)
    .filter(name => /^ppt\/slides\/slide\d+\.xml$/i.test(name))
    .sort((a, b) => {
      const na = parseInt(a.match(/slide(\d+)/i)[1])
      const nb = parseInt(b.match(/slide(\d+)/i)[1])
      return na - nb
    })

  const total = slideFiles.length

  // Get presentation dimensions (convert from EMU to px)
  let slideWidth = emuToPx(9144000)
  let slideHeight = emuToPx(5143500)
  const presXmlStr = await zip.file('ppt/presentation.xml')?.async('string')
  if (presXmlStr) {
    const parser = new DOMParser()
    const presDoc = parser.parseFromString(presXmlStr, 'text/xml')
    const sldSz = presDoc.querySelector('p\\:sldSz, sldSz')
    if (sldSz) {
      const cx = parseInt(sldSz.getAttribute('cx')) || 0
      const cy = parseInt(sldSz.getAttribute('cy')) || 0
      if (cx) slideWidth = emuToPx(cx)
      if (cy) slideHeight = emuToPx(cy)
    }
  }

  for (let idx = 0; idx < total; idx++) {
    const filePath = slideFiles[idx]
    const slideNum = parseInt(filePath.match(/slide(\d+)/i)[1])
    const slideXmlStr = await zip.file(filePath)?.async('string')
    if (!slideXmlStr) continue

    const parser = new DOMParser()
    const slideDoc = parser.parseFromString(slideXmlStr, 'text/xml')

    // Parse relationships for this slide
    const relsPath = `ppt/slides/_rels/slide${slideNum}.xml.rels`
    const relsXmlStr = await zip.file(relsPath)?.async('string')
    let relsMap = {}
    if (relsXmlStr) {
      const relsDoc = parser.parseFromString(relsXmlStr, 'text/xml')
      relsMap = extractImageRels(relsDoc)
    }

    const background = parseSlideBackground(slideDoc)

    // Find the shape tree
    const spTree = slideDoc.querySelector('p\\:spTree, spTree')
    if (!spTree) {
      slides.push({
        index: idx,
        number: slideNum,
        width: slideWidth,
        height: slideHeight,
        background,
        elements: [],
      })
      continue
    }

    const elements = []
    const allShapes = Array.from(spTree.children)

    for (const shape of allShapes) {
      const localName = shape.localName || shape.nodeName

      if (localName === 'sp' || localName === 'cxnSp') {
        // Text shape or connector
        const xfrm = xpath(shape, 'p:spPr/a:xfrm')
        const off = xpath(xfrm, 'a:off')
        const ext = xpath(xfrm, 'a:ext')
        const txBody = xpath(shape, 'p:txBody')

        const html = parseRuns(txBody)
        if (!html) continue

        const left = off ? emuToPx(parseInt(getAttr(off, 'x', XML_NS.a)) || 0) : 0
        const top = off ? emuToPx(parseInt(getAttr(off, 'y', XML_NS.a)) || 0) : 0
        const width = ext ? emuToPx(parseInt(getAttr(ext, 'cx', XML_NS.a)) || 0) : 100
        const height = ext ? emuToPx(parseInt(getAttr(ext, 'cy', XML_NS.a)) || 0) : 100

        elements.push({
          type: 'textbox',
          left,
          top,
          width,
          height,
          html,
        })
      } else if (localName === 'pic') {
        // Picture shape
        const xfrm = xpath(shape, 'p:spPr/a:xfrm')
        const off = xpath(xfrm, 'a:off')
        const ext = xpath(xfrm, 'a:ext')
        const blip = shape.querySelector('a\\:blip, blip')
        const embedId = blip ? blip.getAttribute('r:embed') || blip.getAttribute('embed') : null

        const left = off ? emuToPx(parseInt(getAttr(off, 'x', XML_NS.a)) || 0) : 0
        const top = off ? emuToPx(parseInt(getAttr(off, 'y', XML_NS.a)) || 0) : 0
        const width = ext ? emuToPx(parseInt(getAttr(ext, 'cx', XML_NS.a)) || 0) : 100
        const height = ext ? emuToPx(parseInt(getAttr(ext, 'cy', XML_NS.a)) || 0) : 100

        let dataUrl = null
        if (embedId && relsMap[embedId]) {
          const imgPath = `ppt/slides/${relsMap[embedId]}`
          if (imageCache[imgPath]) {
            dataUrl = imageCache[imgPath]
          } else {
            const imgData = await zip.file(imgPath)?.async('base64')
            if (imgData) {
              const ext = imgPath.split('.').pop().toLowerCase()
              const mime = ext === 'svg' ? 'image/svg+xml' : ext === 'png' ? 'image/png' : ext === 'gif' ? 'image/gif' : 'image/jpeg'
              dataUrl = `data:${mime};base64,${imgData}`
              imageCache[imgPath] = dataUrl
            }
          }
        }

        if (dataUrl) {
          elements.push({
            type: 'image',
            left,
            top,
            width,
            height,
            dataUrl,
          })
        }
      } else if (localName === 'grpSp') {
        // Group shape - recursively extract text and images
        const groupChildren = Array.from(shape.children)
        for (const gChild of groupChildren) {
          const gln = gChild.localName || gChild.nodeName
          if (gln === 'sp') {
            const xfrm = xpath(gChild, 'p:spPr/a:xfrm')
            const off = xpath(xfrm, 'a:off')
            const ext = xpath(xfrm, 'a:ext')
            const txBody = xpath(gChild, 'p:txBody')
            const html = parseRuns(txBody)
            if (!html) continue
            const left = off ? emuToPx(parseInt(getAttr(off, 'x', XML_NS.a)) || 0) : 0
            const top = off ? emuToPx(parseInt(getAttr(off, 'y', XML_NS.a)) || 0) : 0
            const width = ext ? emuToPx(parseInt(getAttr(ext, 'cx', XML_NS.a)) || 0) : 100
            const height = ext ? emuToPx(parseInt(getAttr(ext, 'cy', XML_NS.a)) || 0) : 100
            elements.push({ type: 'textbox', left, top, width, height, html })
          } else if (gln === 'pic') {
            const xfrm = xpath(gChild, 'p:spPr/a:xfrm')
            const off = xpath(xfrm, 'a:off')
            const ext = xpath(xfrm, 'a:ext')
            const blip = gChild.querySelector('a\\:blip, blip')
            const embedId = blip ? blip.getAttribute('r:embed') || blip.getAttribute('embed') : null
            const left = off ? emuToPx(parseInt(getAttr(off, 'x', XML_NS.a)) || 0) : 0
            const top = off ? emuToPx(parseInt(getAttr(off, 'y', XML_NS.a)) || 0) : 0
            const width = ext ? emuToPx(parseInt(getAttr(ext, 'cx', XML_NS.a)) || 0) : 100
            const height = ext ? emuToPx(parseInt(getAttr(ext, 'cy', XML_NS.a)) || 0) : 100
            let dataUrl = null
            if (embedId && relsMap[embedId]) {
              const imgPath = `ppt/slides/${relsMap[embedId]}`
              if (imageCache[imgPath]) {
                dataUrl = imageCache[imgPath]
              } else {
                const imgData = await zip.file(imgPath)?.async('base64')
                if (imgData) {
                  const ext = imgPath.split('.').pop().toLowerCase()
                  const mime = ext === 'svg' ? 'image/svg+xml' : ext === 'png' ? 'image/png' : ext === 'gif' ? 'image/gif' : 'image/jpeg'
                  dataUrl = `data:${mime};base64,${imgData}`
                  imageCache[imgPath] = dataUrl
                }
              }
            }
            if (dataUrl) {
              elements.push({ type: 'image', left, top, width, height, dataUrl })
            }
          }
        }
      }
    }

    slides.push({
      index: idx,
      number: slideNum,
      width: slideWidth,
      height: slideHeight,
      background,
      elements,
    })

    if (onProgress) {
      onProgress(idx + 1, total)
    }
  }

  return { slides, slideWidth, slideHeight }
}

/**
 * Convert EMU to pixels at a given scale.
 */
export function emuToPxAtScale(emu, scale) {
  return Math.round((emu / EMU_PER_PX) * scale)
}

/**
 * 从 HTML 字符串中提取纯文本（去除所有标签）
 */
export function stripHtml(html) {
  if (!html) return ''
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&nbsp;/g, ' ')
    .replace(/&quot;/g, '"')
}

/**
 * 从解析后的 slides 数组中提取所有纯文本
 * @returns {Array<{index: number, text: string}>}
 */
export function extractSlidesText(slides) {
  if (!slides || !slides.length) return []
  return slides.map((slide, i) => {
    const textParts = (slide.elements || [])
      .filter(el => el.type === 'textbox')
      .map(el => stripHtml(el.html))
      .filter(t => t.trim())
    return {
      index: i,
      number: slide.number || (i + 1),
      text: textParts.join('\n'),
    }
  }).filter(s => s.text.trim())
}

/**
 * 将提取的幻灯片文字拼接成一段上下文文本，供 AI 对话使用
 */
export function buildSlidesContext(slidesTextList) {
  if (!slidesTextList || !slidesTextList.length) return ''
  return slidesTextList
    .map(s => `第${s.number}页：\n${s.text}`)
    .join('\n\n')
}
