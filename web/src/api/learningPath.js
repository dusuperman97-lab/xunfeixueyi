import http from '@/utils/http.js';

/**
 * 学习路径相关API
 */
export default {
    /**
     * 生成专业推荐学习路线
     * @param {String} major 专业名称
     */
    generateMajor(major) {
        return http.post('/xunfei/learning-path/generate-major', null, {
            params: { major }
        });
    },

    /**
     * 生成个性化学习路线
     */
    generatePersonal() {
        return http.post('/xunfei/learning-path/generate-personal');
    },

    /**
     * 查询用户的学习路线
     * @param {String} pathType 路线类型（可选）：major-专业推荐, personal-个性化
     */
    getLearningPath(pathType) {
        return http.get('/xunfei/learning-path/get', {
            params: pathType ? { pathType } : {}
        });
    },

    /**
     * 删除学习路线
     * @param {Integer} id 路线ID
     */
    deleteLearningPath(id) {
        return http.delete('/xunfei/learning-path/delete', {
            params: { id }
        });
    }
};
