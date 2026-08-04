// src/api/mindmap.js
import http from '@/utils/http.js'

/** 获取课程列表 */
export function getCourseList() {
    return http.get('/chapter/courseList')
}

/** 根据课程ID获取章节列表 */
export function getChapterListByCourseId(courseId) {
    return http.get(`/chapter/listByCourseId/${courseId}`)
}

/**
 * 根据文字消息生成思维导图
 */
export function generateMindMapFromMessage(message) {
    return http.post('/xunfei/mindmap/generate-from-message', { message })
}

/**
 * 根据课程章节生成思维导图
 */
export function generateMindMapByCourse(params) {
    return http.post('/xunfei/mindmap/generate', null, {
        params: {
            chapterId: params.chapterId,
            sectionId: params.sectionId
        }
    })
}

/**
 * 根据章节ID查询思维导图历史记录
 */
export function getMindMapsByChapter(chapterId) {
    return http.get('/mindmap/mindmap/chapter', { params: { chapterId } })
}

/**
 * 查询用户的所有思维导图历史记录
 */
export function getUserMindMaps() {
    return http.get('/mindmap/mindmap/list')
}

/**
 * 根据文件URL获取思维导图的markdown内容
 */
export function getMindMapContent(fileUrl) {
    return fetch(fileUrl).then(res => res.text())
}