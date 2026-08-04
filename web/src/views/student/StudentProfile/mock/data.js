// ── 学生基本信息 ──
export const studentInfo = {
  college: '电气与信息工程学院',
  grade: '2021级本科生',
  id: '20263647282',
  gender: '男性',
  birthday: '1999-11-25',
  hometown: '河南',
  political: '',
  ethnicity: '汉族',
  credits: 10,
  gpa: 3.34,
  username: 'zhangweichen',
  nickname: '晨光微曦',
  major: '计算机科学与技术',
  email: 'zhang.weichen@edu.cn',
}

// ── 校园信息 ──
export const campusInfo = {
  enrollDate: '2018-09',
  graduateDate: '2022-07',
  grade: '2018',
  college: '电气与信息工程学院',
  major: '电气工程',
  classId: '206263732',
  dorm: '主校区xx园xx号楼x层xx房',
}

// ── 荣誉列表 ──
export const honors = [
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
  'XXXXXXXXXXXXXX竞赛优秀奖',
]

// ── 课程学分列表 ──
export const courseList = [
  { id: 1, label1: '公共必修课学分数', val1: '2.3', label2: '公共必修课学分数', val2: '53' },
  { id: 2, label1: '专业选修课学分数', val1: '3.4', label2: '专业选修课学分数', val2: '2.2' },
  { id: 3, label1: '创新创业教育学分数', val1: '4', label2: '创新创业教育学分数', val2: '2' },
]

// ── 低分课程 ──
export const lowScores = [
  { score: '0.8', pct: 30, name: '概率论与数理统计' },
  { score: '1', pct: 40, name: '外科学总论' },
  { score: '·', pct: 55, name: '神经生物学' },
  { score: '10', pct: 70, name: '实验诊断学B', color: 'linear-gradient(90deg,#ff6384,#ff9f40)' },
  { score: '1.1', pct: 45, name: '超声诊断学' },
]

// ── 图书馆 & 消费 ──
export const libraryInfo = { overdueCount: 108, overdueDays: 12 }
export const spendCategories = ['风华', '桂', '松园一楼', '自选餐厅', '松园围饭厅']

// ── 易错偏好 ──
export const errorPreferences = [
  {
    type: '逻辑跳跃型', severity: '高', frequency: 78,
    color: '#e05c5c', bgColor: 'rgba(220,60,60,0.06)',
    description: '在推导过程中常省略中间步骤，导致因果关系断裂，尤其表现在证明题和多步计算题中。',
    tags: ['证明题', '多步计算', '推理链断裂'],
  },
  {
    type: '概念混淆型', severity: '高', frequency: 65,
    color: '#e07a5c', bgColor: 'rgba(220,120,60,0.06)',
    description: '易混淆相似定义（如"死锁避免"与"死锁预防"），或在不同学科中使用相同术语时产生歧义。',
    tags: ['定义混淆', '跨学科歧义', '术语辨析'],
  },
  {
    type: '审题偏差型', severity: '中', frequency: 52,
    color: '#e8a045', bgColor: 'rgba(230,150,30,0.06)',
    description: '快速阅读时遗漏关键条件（如"非负整数"误为"正整数"），或误解题目隐含约束。',
    tags: ['条件遗漏', '隐含约束', '快速阅题'],
  },
  {
    type: '计算粗心型', severity: '中', frequency: 48,
    color: '#6272d8', bgColor: 'rgba(91,111,212,0.06)',
    description: '符号错误、进位失误、公式代入错位等低级计算错误，尤其在复杂表达式中频率升高。',
    tags: ['符号错误', '进位失误', '公式代入'],
  },
  {
    type: '过度拟合型', severity: '低', frequency: 30,
    color: '#5bb891', bgColor: 'rgba(91,184,145,0.06)',
    description: '偏好套用熟悉的解题模板，遇到新题型时缺乏灵活变形能力，容易生搬硬套。',
    tags: ['模板依赖', '灵活度不足', '新题型'],
  },
]

// ── 知识获取偏好 ──
export const processingPreferences = [
  { type: '偏文字', value: 45, color: '#6272d8', description: '更倾向于通过阅读文本来理解和吸收信息，善于从书面描述中提取要点。' },
  { type: '偏图像', value: 78, color: '#e8a045', description: '通过视觉元素（图表、图像、示意图）更容易理解复杂概念，图像记忆能力强。' },
  { type: '偏示例', value: 65, color: '#5bb891', description: '通过具体实例和案例学习效果更佳，需要实际例子来加深对抽象概念的理解。' },
  { type: '偏视频', value: 52, color: '#e05c5c', description: '通过动态演示和视频讲解能更好掌握知识，喜欢观看操作过程来学习。' },
]
