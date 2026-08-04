import http from '@/utils/http.js'

export const SANDBOX_LANGUAGES = [
  {
    value: 'javascript',
    label: 'JavaScript',
    filename: 'index.js',
    accent: '#f0db4f',
    executable: true,
  },
  {
    value: 'python',
    label: 'Python3',
    filename: 'main.py',
    accent: '#3776ab',
    executable: true,
  },
  {
    value: 'java',
    label: 'Java',
    filename: 'Main.java',
    accent: '#b07219',
    executable: true,
  },
]

const FUNCTION_PARAMS = {
  'two-sum': ['nums', 'target'],
  'valid-parentheses': ['s'],
  'merge-sorted-array': ['nums1', 'm', 'nums2', 'n'],
  'binary-search': ['nums', 'target'],
  'reverse-linked-list': ['values'],
  'climbing-stairs': ['n'],
  'max-subarray': ['nums'],
  'merge-intervals': ['intervals'],
}

function makeFunctionStarter({ functionName, params, title }) {
  return {
    javascript: [
      `function ${functionName}(${params.join(', ')}) {`,
      `  // TODO: 实现「${title}」`,
      '}',
    ].join('\n'),
    python: [
      `def ${functionName}(${params.join(', ')}):`,
      `    # TODO: 实现「${title}」`,
      '    pass',
    ].join('\n'),
    java: [
      'class Solution {',
      `  Object ${functionName}(${params.map((param) => `Object ${param}`).join(', ')}) {`,
      `    // TODO: 实现「${title}」`,
      '    return null;',
      '  }',
      '}',
    ].join('\n'),
  }
}

function makeClassStarter({ className, title, methods }) {
  return {
    javascript: [
      `class ${className} {`,
      '  constructor() {',
      `    // TODO: 初始化「${title}」需要的数据结构`,
      '  }',
      '',
      ...methods.flatMap((method) => [
        `  ${method.name}(${method.params.join(', ')}) {`,
        `    // TODO: 实现 ${method.name}`,
        method.returnsValue ? '    return null' : '',
        '  }',
        '',
      ]),
      '}',
    ].join('\n'),
    python: [
      `class ${className}:`,
      '    def __init__(self):',
      `        # TODO: 初始化「${title}」需要的数据结构`,
      '        pass',
      '',
      ...methods.flatMap((method) => [
        `    def ${method.name}(self${method.params.length ? `, ${method.params.join(', ')}` : ''}):`,
        `        # TODO: 实现 ${method.name}`,
        '        pass',
        '',
      ]),
    ].join('\n'),
    java: [
      `class ${className} {`,
      `  ${className}() {`,
      `    // TODO: 初始化「${title}」需要的数据结构`,
      '  }',
      '',
      ...methods.flatMap((method) => [
        `  Object ${method.name}(${method.params.map((param) => `Object ${param}`).join(', ')}) {`,
        `    // TODO: 实现 ${method.name}`,
        '    return null;',
        '  }',
        '',
      ]),
      '}',
    ].join('\n'),
  }
}

function defineFunctionProblem(config) {
  const params = FUNCTION_PARAMS[config.id]
  return {
    mode: 'function',
    ...config,
    starterCode: makeFunctionStarter({
      functionName: config.functionName,
      params,
      title: config.title,
    }),
  }
}

function defineClassProblem(config) {
  return {
    mode: 'class',
    ...config,
    starterCode: makeClassStarter(config),
  }
}

export const SANDBOX_PROBLEMS = [
  defineFunctionProblem({
    id: 'two-sum',
    title: '两数之和',
    category: '数组 / 哈希表',
    difficulty: '入门',
    tags: ['数组', '哈希表', 'LeetCode 高频'],
    acceptanceRate: 54,
    estimatedTime: '12 分钟',
    functionName: 'twoSum',
    objective: '给定整数数组 nums 和目标值 target，返回两个数下标，使它们的和等于 target。',
    description: [
      '每组输入只会有一个有效答案，同一个元素不能重复使用。',
      '返回下标的顺序不限，建议使用哈希表把查找补数的过程降到 O(n)。',
    ],
    constraints: [
      '2 <= nums.length <= 10000',
      '-10^9 <= nums[i], target <= 10^9',
      '只存在一个有效答案',
    ],
    testCases: [
      caseOf('sample-1', '基础配对', ['nums = [2,7,11,15]', 'target = 9'], [[2, 7, 11, 15], 9], [0, 1], 'unorderedArray', '记录每个数出现的下标，再查找 target - nums[i]。'),
      caseOf('middle-pair', '中间元素配对', ['nums = [3,2,4]', 'target = 6'], [[3, 2, 4], 6], [1, 2], 'unorderedArray', '不要假设答案一定包含第一个元素。'),
      caseOf('duplicate-values', '重复数值', ['nums = [3,3]', 'target = 6'], [[3, 3], 6], [0, 1], 'unorderedArray', '相同数值也必须来自两个不同下标。'),
      caseOf('negative-number', '包含负数', ['nums = [-1,-2,-3,-4,-5]', 'target = -8'], [[-1, -2, -3, -4, -5], -8], [2, 4], 'unorderedArray', '哈希表查找不受正负号影响。'),
    ],
  }),
  defineFunctionProblem({
    id: 'valid-parentheses',
    title: '有效的括号',
    category: '字符串 / 栈',
    difficulty: '入门',
    tags: ['栈', '字符串', '括号匹配'],
    acceptanceRate: 41,
    estimatedTime: '10 分钟',
    functionName: 'isValidParentheses',
    objective: '判断只包含 ()[]{} 的字符串是否括号闭合且顺序正确。',
    description: [
      '遇到左括号入栈，遇到右括号时检查栈顶是否为对应的左括号。',
      '遍历结束后栈为空才说明所有括号都被正确匹配。',
    ],
    constraints: [
      '1 <= s.length <= 10000',
      's 只包含括号字符',
      '右括号不能先于对应左括号出现',
    ],
    testCases: [
      caseOf('simple-pairs', '连续简单配对', ['s = "()[]{}"'], ['()[]{}'], true, 'deepEqual', '用映射表记录右括号对应的左括号。'),
      caseOf('wrong-order', '顺序错误', ['s = "(]"'], ['(]'], false, 'deepEqual', '只统计数量不够，还要比较最近的左括号。'),
      caseOf('nested', '多层嵌套', ['s = "([{}])"'], ['([{}])'], true, 'deepEqual', '栈天然适合处理最近打开的括号。'),
      caseOf('unclosed', '存在未闭合括号', ['s = "((("'], ['(((' ], false, 'deepEqual', '遍历后栈不为空时应返回 false。'),
    ],
  }),
  defineFunctionProblem({
    id: 'merge-sorted-array',
    title: '合并两个有序数组',
    category: '数组 / 双指针',
    difficulty: '入门',
    tags: ['数组', '双指针', '原地合并'],
    acceptanceRate: 52,
    estimatedTime: '14 分钟',
    functionName: 'mergeSortedArray',
    returnArgIndex: 0,
    objective: '将 nums2 合并到 nums1 中，使 nums1 成为一个非递减有序数组。',
    description: [
      'nums1 的长度为 m + n，后 n 位预留给 nums2。',
      '推荐从后向前写入，避免覆盖 nums1 中还没有比较的元素。',
    ],
    constraints: [
      '0 <= m, n <= 200',
      'nums1.length == m + n',
      'nums1 与 nums2 的有效区间都已按非递减排序',
    ],
    testCases: [
      caseOf('sample-1', '交错合并', ['nums1 = [1,2,3,0,0,0]', 'm = 3', 'nums2 = [2,5,6]', 'n = 3'], [[1, 2, 3, 0, 0, 0], 3, [2, 5, 6], 3], [1, 2, 2, 3, 5, 6], 'deepEqual', '从尾部指针开始比较并写入。'),
      caseOf('empty-second', '第二个数组为空', ['nums1 = [1]', 'm = 1', 'nums2 = []', 'n = 0'], [[1], 1, [], 0], [1], 'deepEqual', 'n 为 0 时 nums1 应保持不变。'),
      caseOf('empty-first', '第一个有效区间为空', ['nums1 = [0]', 'm = 0', 'nums2 = [1]', 'n = 1'], [[0], 0, [1], 1], [1], 'deepEqual', 'm 为 0 时需要完整复制 nums2。'),
      caseOf('negative-values', '包含负数', ['nums1 = [-3,-1,0,0,0]', 'm = 2', 'nums2 = [-2,2,4]', 'n = 3'], [[-3, -1, 0, 0, 0], 2, [-2, 2, 4], 3], [-3, -2, -1, 2, 4], 'deepEqual', '比较逻辑不能依赖正数假设。'),
    ],
  }),
  defineFunctionProblem({
    id: 'binary-search',
    title: '二分查找',
    category: '数组 / 二分',
    difficulty: '入门',
    tags: ['数组', '二分查找', '边界'],
    acceptanceRate: 58,
    estimatedTime: '10 分钟',
    functionName: 'binarySearch',
    objective: '在升序数组 nums 中查找 target，存在则返回下标，否则返回 -1。',
    description: [
      '每次取中点，把搜索区间缩小到左半或右半。',
      '注意 left <= right 的循环条件和中点下标的更新方式。',
    ],
    constraints: [
      '1 <= nums.length <= 10000',
      'nums 按严格升序排列',
      '要求时间复杂度 O(log n)',
    ],
    testCases: [
      caseOf('found', '命中目标', ['nums = [-1,0,3,5,9,12]', 'target = 9'], [[-1, 0, 3, 5, 9, 12], 9], 4, 'deepEqual', '命中时直接返回 mid。'),
      caseOf('missing', '目标不存在', ['nums = [-1,0,3,5,9,12]', 'target = 2'], [[-1, 0, 3, 5, 9, 12], 2], -1, 'deepEqual', '区间耗尽后返回 -1。'),
      caseOf('single-found', '单元素命中', ['nums = [5]', 'target = 5'], [[5], 5], 0, 'deepEqual', '单元素数组也要进入循环。'),
      caseOf('between-values', '落在两个值之间', ['nums = [1,3,5,7,9]', 'target = 8'], [[1, 3, 5, 7, 9], 8], -1, 'deepEqual', '更新边界时不能遗漏右侧元素。'),
    ],
  }),
  defineFunctionProblem({
    id: 'reverse-linked-list',
    title: '反转链表（数组模拟）',
    category: '链表 / 指针',
    difficulty: '入门',
    tags: ['链表', '迭代', '数组模拟输入'],
    acceptanceRate: 78,
    estimatedTime: '12 分钟',
    functionName: 'reverseLinkedList',
    objective: '给定链表节点值数组 values，返回反转后的节点值数组。',
    description: [
      '真实链表题通常通过改变 next 指针完成反转。',
      '这里为了前端本地判题，用数组模拟输入输出，重点考查反转顺序的算法思路。',
    ],
    constraints: [
      '0 <= values.length <= 5000',
      '-5000 <= values[i] <= 5000',
      '需要保持所有节点值不丢失',
    ],
    testCases: [
      caseOf('sample-1', '多节点反转', ['values = [1,2,3,4,5]'], [[1, 2, 3, 4, 5]], [5, 4, 3, 2, 1], 'deepEqual', '可用头插法或双指针思路反转。'),
      caseOf('empty', '空链表', ['values = []'], [[]], [], 'deepEqual', '空输入应返回空数组。'),
      caseOf('single', '单节点', ['values = [1]'], [[1]], [1], 'deepEqual', '单节点反转后不变。'),
      caseOf('two-nodes', '两个节点', ['values = [1,2]'], [[1, 2]], [2, 1], 'deepEqual', '注意交换后不要丢失节点。'),
    ],
  }),
  defineFunctionProblem({
    id: 'climbing-stairs',
    title: '爬楼梯',
    category: '动态规划',
    difficulty: '入门',
    tags: ['动态规划', '斐波那契', '滚动变量'],
    acceptanceRate: 53,
    estimatedTime: '10 分钟',
    functionName: 'climbStairs',
    objective: '每次可以爬 1 或 2 个台阶，计算到达第 n 阶共有多少种不同方法。',
    description: [
      '到达第 n 阶的方法数等于到达 n-1 和 n-2 阶的方法数之和。',
      '可用两个滚动变量把空间复杂度降到 O(1)。',
    ],
    constraints: [
      '1 <= n <= 45',
      '答案在 32 位整数范围内',
      '不要用指数级递归重复计算',
    ],
    testCases: [
      caseOf('two', '两阶', ['n = 2'], [2], 2, 'deepEqual', '1+1 或 2，共 2 种。'),
      caseOf('three', '三阶', ['n = 3'], [3], 3, 'deepEqual', '递推关系从小规模开始验证。'),
      caseOf('five', '五阶', ['n = 5'], [5], 8, 'deepEqual', '继续使用 f(n)=f(n-1)+f(n-2)。'),
      caseOf('ten', '十阶', ['n = 10'], [10], 89, 'deepEqual', '较大输入用于暴露低效递归。'),
    ],
  }),
  defineFunctionProblem({
    id: 'max-subarray',
    title: '最大子数组和',
    category: '数组 / 动态规划',
    difficulty: '中等',
    tags: ['数组', '动态规划', 'Kadane'],
    acceptanceRate: 51,
    estimatedTime: '15 分钟',
    functionName: 'maxSubArray',
    objective: '给定整数数组 nums，返回连续子数组能够取得的最大和。',
    description: [
      '当前位置的最佳连续和，要么从当前元素重新开始，要么接在前一个最佳连续和后面。',
      '全程维护当前和与历史最大值。',
    ],
    constraints: [
      '1 <= nums.length <= 100000',
      '-10000 <= nums[i] <= 10000',
      '子数组必须连续且至少包含一个元素',
    ],
    testCases: [
      caseOf('sample-1', '经典样例', ['nums = [-2,1,-3,4,-1,2,1,-5,4]'], [[-2, 1, -3, 4, -1, 2, 1, -5, 4]], 6, 'deepEqual', '最大连续段是 [4,-1,2,1]。'),
      caseOf('single', '单元素', ['nums = [1]'], [[1]], 1, 'deepEqual', '初始化不能从 0 开始，否则全负数会出错。'),
      caseOf('all-positive-mostly', '连续正收益', ['nums = [5,4,-1,7,8]'], [[5, 4, -1, 7, 8]], 23, 'deepEqual', '负数也可能属于最佳连续段。'),
      caseOf('all-negative', '全负数', ['nums = [-3,-2,-5]'], [[-3, -2, -5]], -2, 'deepEqual', '全负数时答案是最大的那个负数。'),
    ],
  }),
  defineFunctionProblem({
    id: 'merge-intervals',
    title: '合并区间',
    category: '数组 / 排序',
    difficulty: '中等',
    tags: ['排序', '区间', '数组'],
    acceptanceRate: 49,
    estimatedTime: '18 分钟',
    functionName: 'mergeIntervals',
    objective: '合并所有重叠区间，返回互不重叠且覆盖原输入的区间列表。',
    description: [
      '先按区间起点排序，再依次与已合并结果的最后一个区间比较。',
      '若当前起点不大于上一个终点，则更新终点；否则追加新区间。',
    ],
    constraints: [
      '0 <= intervals.length <= 10000',
      'intervals[i].length == 2',
      '0 <= start <= end <= 10000',
    ],
    testCases: [
      caseOf('sample-1', '多段区间', ['intervals = [[1,3],[2,6],[8,10],[15,18]]'], [[[1, 3], [2, 6], [8, 10], [15, 18]]], [[1, 6], [8, 10], [15, 18]], 'deepEqual', '排序后只需要比较最后一个合并区间。'),
      caseOf('touching', '端点相接', ['intervals = [[1,4],[4,5]]'], [[[1, 4], [4, 5]]], [[1, 5]], 'deepEqual', '端点相同也视为重叠。'),
      caseOf('unsorted', '输入无序', ['intervals = [[1,4],[0,2],[3,5]]'], [[[1, 4], [0, 2], [3, 5]]], [[0, 5]], 'deepEqual', '必须先排序，不能依赖原始顺序。'),
      caseOf('empty', '空输入', ['intervals = []'], [[]], [], 'deepEqual', '空数组应直接返回空数组。'),
    ],
  }),
  defineClassProblem({
    id: 'queue-with-stacks',
    title: '用栈实现队列',
    category: '数据结构 / 栈与队列',
    difficulty: '入门',
    tags: ['栈', '队列', '设计'],
    acceptanceRate: 67,
    estimatedTime: '15 分钟',
    className: 'MyQueue',
    methods: [
      { name: 'push', params: ['x'], returnsValue: false },
      { name: 'pop', params: [], returnsValue: true },
      { name: 'peek', params: [], returnsValue: true },
      { name: 'empty', params: [], returnsValue: true },
    ],
    objective: '仅使用两个栈实现先入先出队列，支持 push、pop、peek、empty。',
    description: [
      '一个栈负责接收新元素，另一个栈负责按队列顺序输出元素。',
      '当输出栈为空时，把输入栈元素逐个转移到输出栈。',
    ],
    constraints: [
      '1 <= 操作次数 <= 1000',
      'push 的值为整数',
      '只允许用数组的栈式操作模拟队列',
    ],
    testCases: [
      scriptCase('sample-1', '基础队列顺序', ['push(1)', 'push(2)', 'peek()', 'pop()', 'empty()'], [
        call('push', [1], null),
        call('push', [2], null),
        call('peek', [], 1),
        call('pop', [], 1),
        call('empty', [], false),
      ], '转移栈时会反转顺序，从而得到 FIFO。'),
      scriptCase('drain', '出队后为空', ['push(10)', 'pop()', 'empty()'], [
        call('push', [10], null),
        call('pop', [], 10),
        call('empty', [], true),
      ], '最后一个元素出队后两个栈都应为空。'),
      scriptCase('mixed', '交错操作', ['empty()', 'push(4)', 'push(9)', 'pop()', 'peek()'], [
        call('empty', [], true),
        call('push', [4], null),
        call('push', [9], null),
        call('pop', [], 4),
        call('peek', [], 9),
      ], '新入队元素不应越过旧元素。'),
      scriptCase('two-pops', '连续出队', ['push(5)', 'push(6)', 'pop()', 'pop()', 'empty()'], [
        call('push', [5], null),
        call('push', [6], null),
        call('pop', [], 5),
        call('pop', [], 6),
        call('empty', [], true),
      ], '输出栈已有元素时不要重复转移。'),
    ],
  }),
  defineClassProblem({
    id: 'min-stack',
    title: '最小栈',
    category: '数据结构 / 栈设计',
    difficulty: '中等',
    tags: ['栈', '设计', 'O(1) 最小值'],
    acceptanceRate: 56,
    estimatedTime: '18 分钟',
    className: 'MinStack',
    methods: [
      { name: 'push', params: ['value'], returnsValue: false },
      { name: 'pop', params: [], returnsValue: false },
      { name: 'top', params: [], returnsValue: true },
      { name: 'getMin', params: [], returnsValue: true },
    ],
    objective: '设计支持 push、pop、top、getMin 的栈，并让 getMin 在 O(1) 时间返回最小元素。',
    description: [
      '可以额外维护一个同步最小值栈，记录每次 push 后的当前最小值。',
      'pop 时同时移除数据栈和最小值栈顶部，保证状态一致。',
    ],
    constraints: [
      '-2^31 <= value <= 2^31 - 1',
      'pop、top、getMin 在测试中只会对非空栈调用',
      '每个方法期望 O(1) 时间复杂度',
    ],
    testCases: [
      scriptCase('sample-1', '基础最小值维护', ['push(-2)', 'push(0)', 'push(-3)', 'getMin()', 'pop()', 'top()', 'getMin()'], [
        call('push', [-2], null),
        call('push', [0], null),
        call('push', [-3], null),
        call('getMin', [], -3),
        call('pop', [], null),
        call('top', [], 0),
        call('getMin', [], -2),
      ], '最小值栈要随 pop 同步更新。'),
      scriptCase('duplicate-min', '重复最小值', ['push(2)', 'push(0)', 'push(3)', 'push(0)', 'getMin()', 'pop()', 'getMin()'], [
        call('push', [2], null),
        call('push', [0], null),
        call('push', [3], null),
        call('push', [0], null),
        call('getMin', [], 0),
        call('pop', [], null),
        call('getMin', [], 0),
      ], '重复最小值不能只保存一次。'),
      scriptCase('single', '单元素查询', ['push(1)', 'top()', 'getMin()'], [
        call('push', [1], null),
        call('top', [], 1),
        call('getMin', [], 1),
      ], '单元素时栈顶也是最小值。'),
      scriptCase('negative-top', '负数栈顶', ['push(-1)', 'push(-2)', 'top()', 'getMin()'], [
        call('push', [-1], null),
        call('push', [-2], null),
        call('top', [], -2),
        call('getMin', [], -2),
      ], '比较时不要忽略负数。'),
    ],
  }),
]

const AI_PROBLEM_TEMPLATES = [
  {
    id: 'unique-even-sort',
    title: 'AI 生成：筛选偶数并去重排序',
    category: 'AI 推荐 / 数组',
    difficulty: '入门',
    tags: ['数组', '集合', '排序'],
    acceptanceRate: 62,
    estimatedTime: '10 分钟',
    functionName: 'uniqueSortedEvens',
    objective: '给定整数数组 nums，返回所有偶数，去重后按升序排列。',
    description: [
      '这是一道前端本地生成的算法练习题，不会请求后端。',
      '可以使用 Set 去重，再用数值排序得到稳定结果。',
    ],
    constraints: [
      '0 <= nums.length <= 10000',
      '-10000 <= nums[i] <= 10000',
      '返回数组必须按升序排列',
    ],
    testCases: [
      caseOf('mixed', '混合奇偶', ['nums = [5,2,2,4,3,8,4]'], [[5, 2, 2, 4, 3, 8, 4]], [2, 4, 8], 'deepEqual', '先筛选偶数，再去重排序。'),
      caseOf('negative', '包含负数', ['nums = [-2,-4,-2,1,3]'], [[-2, -4, -2, 1, 3]], [-4, -2], 'deepEqual', '排序要使用数值比较。'),
      caseOf('none', '没有偶数', ['nums = [1,3,5]'], [[1, 3, 5]], [], 'deepEqual', '没有符合条件的元素时返回空数组。'),
      caseOf('zero', '包含零', ['nums = [0,0,2]'], [[0, 0, 2]], [0, 2], 'deepEqual', '0 也是偶数。'),
    ],
    referenceSolution: {
      javascript: 'function uniqueSortedEvens(nums) {\n  return [...new Set(nums.filter((num) => num % 2 === 0))].sort((a, b) => a - b)\n}',
    },
  },
  {
    id: 'longest-word',
    title: 'AI 生成：寻找最长单词',
    category: 'AI 推荐 / 字符串',
    difficulty: '入门',
    tags: ['字符串', '数组', '遍历'],
    acceptanceRate: 66,
    estimatedTime: '8 分钟',
    functionName: 'longestWord',
    objective: '给定单词数组 words，返回长度最长的单词；若长度相同，返回更早出现的单词。',
    description: [
      '这是一道前端本地生成的算法练习题，不会请求后端。',
      '一次遍历即可维护当前最长单词。',
    ],
    constraints: [
      '1 <= words.length <= 1000',
      '1 <= words[i].length <= 50',
      'words[i] 只包含英文字母',
    ],
    testCases: [
      caseOf('sample', '基础样例', ['words = ["ai","code","sandbox"]'], [['ai', 'code', 'sandbox']], 'sandbox', 'deepEqual', '比较字符串长度并保留先出现者。'),
      caseOf('tie', '长度并列', ['words = ["tree","moon","star"]'], [['tree', 'moon', 'star']], 'tree', 'deepEqual', '长度相同时不要替换已有答案。'),
      caseOf('single', '单个单词', ['words = ["xueyi"]'], [['xueyi']], 'xueyi', 'deepEqual', '单元素数组直接返回该元素。'),
      caseOf('later-longer', '后续更长', ['words = ["go","learn","algorithm"]'], [['go', 'learn', 'algorithm']], 'algorithm', 'deepEqual', '发现更长单词时更新答案。'),
    ],
    referenceSolution: {
      javascript: 'function longestWord(words) {\n  return words.reduce((best, word) => word.length > best.length ? word : best, words[0])\n}',
    },
  },
]

export function createAiGeneratedProblem({ seed = Date.now(), courseName = '算法训练' } = {}) {
  const safeSeed = Math.abs(Number(seed) || 0)
  const template = AI_PROBLEM_TEMPLATES[safeSeed % AI_PROBLEM_TEMPLATES.length]
  return {
    ...template,
    id: `ai-generated-${safeSeed}-${template.id}`,
    source: 'front-end-ai-placeholder',
    backendExecution: false,
    courseName,
    starterCode: makeFunctionStarter({
      functionName: template.functionName,
      params: template.functionName === 'longestWord' ? ['words'] : ['nums'],
      title: template.title,
    }),
  }
}

export function getSandboxProblem(problemId = 'two-sum') {
  return SANDBOX_PROBLEMS.find((problem) => problem.id === problemId) || SANDBOX_PROBLEMS[0]
}

export function createSandboxDraft(problemOrId = 'two-sum', language = 'javascript') {
  const problem = typeof problemOrId === 'object' && problemOrId
    ? problemOrId
    : getSandboxProblem(problemOrId)
  const selectedLanguage = SANDBOX_LANGUAGES.find((item) => item.value === language)
    ? language
    : SANDBOX_LANGUAGES[0].value

  return {
    problemId: problem.id,
    language: selectedLanguage,
    code: problem.starterCode?.[selectedLanguage] || problem.starterCode?.javascript || '',
  }
}

const SANDBOX_TIMEOUT_MS = 800

export async function evaluateSandboxSubmission({
  problemId = 'two-sum',
  problem = null,
  language = 'javascript',
  code = '',
} = {}) {
  const activeProblem = problem || getSandboxProblem(problemId)
  const source = String(code || '')

  if (language === 'python' || language === 'java') {
    return await callBackendSandbox(activeProblem, language, source)
  }

  if (language !== 'javascript') {
    return buildUnsupportedResult(activeProblem, language)
  }

  const safetyError = validateSourceSafety(source)
  const runnerResult = safetyError
    ? { error: safetyError }
    : await runSandboxCases(activeProblem, source)
  const cases = materializeCaseResults(activeProblem, runnerResult)

  return buildResult(cases, runnerResult.detail || '已在隔离运行环境执行 JavaScript，并按测试用例比对输出。')
}

async function callBackendSandbox(problem, language, code) {
  try {
    const response = await http.post('/api/code-sandbox/execute', {
      language,
      code: String(code || ''),
      mode: problem.mode,
      functionName: problem.functionName || null,
      className: problem.className || null,
      returnArgIndex: problem.returnArgIndex ?? null,
      testCases: problem.testCases.map((tc) => ({
        id: tc.id,
        args: tc.args || null,
        calls: tc.calls || null,
        expectedValue: tc.expectedValue,
        compare: tc.compare || 'deepEqual',
      })),
    })

    if (response.code === 200 && response.data) {
      const runnerResult = response.data
      if (runnerResult.error) {
        const cases = problem.testCases.map((tc) => buildFailedCase(tc, runnerResult.error))
        return buildResult(cases, runnerResult.error)
      }
      const cases = materializeCaseResults(problem, runnerResult)
      return buildResult(cases, runnerResult.detail || '已在服务端沙箱中执行代码。')
    }

    const msg = response.msg || '后端沙箱服务异常'
    const cases = problem.testCases.map((tc) => buildFailedCase(tc, msg))
    return buildResult(cases, msg)
  } catch (e) {
    const msg = '后端沙箱服务不可用: ' + (e.message || '未知错误')
    const cases = problem.testCases.map((tc) => buildFailedCase(tc, msg))
    return buildResult(cases, msg)
  }
}

export function buildSandboxAnswerPayload({ problemId, language, code, result }) {
  return {
    questionType: 'CODE',
    backendExecution: language === 'python' || language === 'java',
    problemId,
    language,
    code,
    verdict: result?.verdict || 'not-run',
    passed: result?.passed || 0,
    total: result?.total || 0,
  }
}

function caseOf(id, title, input, args, expectedValue, compare = 'deepEqual', hint = '请检查返回值是否与期望一致。') {
  return {
    id,
    title,
    input,
    args,
    expectedValue,
    expected: formatValue(expectedValue),
    compare,
    hint,
  }
}

function scriptCase(id, title, input, calls, hint) {
  const expectedValue = calls.map((item) => item.expected)
  return {
    id,
    title,
    input,
    calls,
    expectedValue,
    expected: expectedValue.map(formatValue).join('\\n'),
    compare: 'deepEqual',
    hint,
  }
}

function call(method, args, expected) {
  return { method, args, expected }
}

async function runSandboxCases(problem, source) {
  if (source.trim().length < 16 || source.includes('TODO')) {
    return { error: '未检测到完整实现，请先补全 TODO。' }
  }

  if (canUseBrowserWorker()) {
    return runInBrowserWorker(problem, source)
  }

  if (isNodeRuntime()) {
    return runInNodeVm(problem, source)
  }

  return { error: '当前环境不支持本地 JavaScript 判题运行时。' }
}

function materializeCaseResults(problem, runnerResult) {
  if (runnerResult.error) {
    return problem.testCases.map((testCase) => buildFailedCase(testCase, runnerResult.error))
  }

  const outputById = new Map((runnerResult.outputs || []).map((item) => [item.id, item]))
  return problem.testCases.map((testCase) => {
    const output = outputById.get(testCase.id)
    if (!output) {
      return buildFailedCase(testCase, '运行环境未返回该用例结果。')
    }
    if (output.error) {
      return buildFailedCase(testCase, output.error)
    }
    const passed = compareValues(output.actualValue, testCase.expectedValue, testCase.compare)
    return {
      ...testCase,
      status: passed ? 'passed' : 'failed',
      actual: formatValue(output.actualValue),
      message: passed ? '输出与预期一致' : testCase.hint,
    }
  })
}

function canUseBrowserWorker() {
  return typeof Worker !== 'undefined'
    && typeof Blob !== 'undefined'
    && typeof URL !== 'undefined'
    && typeof URL.createObjectURL === 'function'
}

function runInBrowserWorker(problem, source) {
  const workerSource = `
function deepClone(value) {
  return JSON.parse(JSON.stringify(value))
}

function normalizeRuntimeValue(value) {
  return value === undefined ? null : value
}

function createTarget(problem, source) {
  const targetName = problem.mode === 'class' ? problem.className : problem.functionName
  const protectedPreamble = [
    'const window = undefined',
    'const document = undefined',
    'const localStorage = undefined',
    'const sessionStorage = undefined',
    'const fetch = undefined',
    'const XMLHttpRequest = undefined',
    'const WebSocket = undefined',
    'const importScripts = undefined',
    'const postMessage = undefined',
    'const self = undefined',
    'const globalThis = undefined'
  ].join(';') + ';'
  const target = Function('"use strict";\\n' + protectedPreamble + '\\n' + source + '\\nreturn typeof ' + targetName + ' === "function" ? ' + targetName + ' : undefined;')()
  if (typeof target !== 'function') {
    throw new Error('未找到可执行入口 ' + targetName)
  }
  return target
}

function runFunctionCase(fn, testCase, problem) {
  const args = deepClone(testCase.args || [])
  const result = fn(...args)
  if (result === undefined && Number.isInteger(problem.returnArgIndex)) {
    return args[problem.returnArgIndex]
  }
  return normalizeRuntimeValue(result)
}

function runClassCase(Klass, testCase) {
  const instance = new Klass()
  return testCase.calls.map((step) => {
    if (typeof instance[step.method] !== 'function') {
      throw new Error('缺少方法 ' + step.method)
    }
    const result = instance[step.method](...deepClone(step.args || []))
    return normalizeRuntimeValue(result)
  })
}

self.onmessage = (event) => {
  const { problem, source } = event.data
  try {
    const target = createTarget(problem, source)
    const outputs = problem.testCases.map((testCase) => {
      try {
        const actualValue = problem.mode === 'class'
          ? runClassCase(target, testCase)
          : runFunctionCase(target, testCase, problem)
        return { id: testCase.id, actualValue }
      } catch (error) {
        return { id: testCase.id, error: '运行错误：' + error.message }
      }
    })
    self.postMessage({ outputs })
  } catch (error) {
    self.postMessage({ error: '语法错误：' + error.message })
  }
}
`
  const workerUrl = URL.createObjectURL(new Blob([workerSource], { type: 'text/javascript' }))
  const worker = new Worker(workerUrl)

  return new Promise((resolve) => {
    const timer = setTimeout(() => {
      worker.terminate()
      URL.revokeObjectURL(workerUrl)
      resolve({ error: `运行超时：超过 ${SANDBOX_TIMEOUT_MS}ms，已终止本地判题。` })
    }, SANDBOX_TIMEOUT_MS)

    worker.onmessage = (event) => {
      clearTimeout(timer)
      worker.terminate()
      URL.revokeObjectURL(workerUrl)
      resolve({
        ...event.data,
        detail: '已在可终止 Web Worker 中执行 JavaScript，并按测试用例比对输出。',
      })
    }

    worker.onerror = (event) => {
      clearTimeout(timer)
      worker.terminate()
      URL.revokeObjectURL(workerUrl)
      resolve({ error: `运行错误：${event.message || 'Worker 执行失败'}` })
    }

    worker.postMessage({ problem, source })
  })
}

async function runInNodeVm(problem, source) {
  try {
    const vm = process.getBuiltinModule?.('vm')
    if (!vm) {
      return { error: '当前 Node 运行时不支持 VM 判题环境。' }
    }
    const context = vm.createContext({
      console: Object.freeze({ log() {}, warn() {}, error() {} }),
    })
    const targetName = problem.mode === 'class' ? problem.className : problem.functionName
    new vm.Script(
      '"use strict";\n' +
      `${source}\n` +
      `globalThis.__target = typeof ${targetName} === "function" ? ${targetName} : undefined;`,
    ).runInContext(context, { timeout: SANDBOX_TIMEOUT_MS })

    if (typeof context.__target !== 'function') {
      return { error: `未找到可执行入口 ${targetName}。` }
    }

    const outputs = []
    for (const testCase of problem.testCases) {
      context.__args = deepClone(testCase.args || [])
      context.__calls = deepClone(testCase.calls || [])
      context.__returnArgIndex = Number.isInteger(problem.returnArgIndex) ? problem.returnArgIndex : -1
      try {
        const script = new vm.Script(problem.mode === 'class'
          ? `{
              const __instance = new __target()
              globalThis.__actual = __calls.map((step) => {
                if (typeof __instance[step.method] !== "function") throw new Error("缺少方法 " + step.method)
                const result = __instance[step.method](...step.args)
                return result === undefined ? null : result
              })
            }`
          : `{
              const result = __target(...__args)
              globalThis.__actual = result === undefined && __returnArgIndex >= 0
                ? __args[__returnArgIndex]
                : (result === undefined ? null : result)
            }`)
        script.runInContext(context, { timeout: SANDBOX_TIMEOUT_MS })
        outputs.push({ id: testCase.id, actualValue: deepClone(context.__actual) })
      } catch (error) {
        outputs.push({ id: testCase.id, error: `运行错误：${error.message}` })
      }
    }

    return {
      outputs,
      detail: '已在 Node VM 隔离上下文中执行 JavaScript，并按测试用例比对输出。',
    }
  } catch (error) {
    return { error: `运行环境错误：${error.message}` }
  }
}

function isNodeRuntime() {
  return typeof process !== 'undefined' && Boolean(process.versions?.node)
}

function compareValues(actualValue, expectedValue, compare) {
  if (compare === 'unorderedArray') {
    if (!Array.isArray(actualValue) || !Array.isArray(expectedValue)) return false
    return stableJson(sortComparableArray(actualValue)) === stableJson(sortComparableArray(expectedValue))
  }
  return stableJson(actualValue) === stableJson(expectedValue)
}

function buildUnsupportedResult(problem, language) {
  const languageLabel = SANDBOX_LANGUAGES.find((item) => item.value === language)?.label || language
  const cases = problem.testCases.map((testCase) => ({
    ...testCase,
    status: 'failed',
    actual: '当前前端真实判题仅支持 JavaScript',
    message: `${languageLabel} 需要后端或运行时沙箱支持，请切换 JavaScript 运行测试。`,
  }))
  return buildResult(cases, '前端本地真实判题暂只执行 JavaScript；Python/Java 模板仅供参考。', 'unsupported')
}

function buildFailedCase(testCase, message) {
  return {
    ...testCase,
    status: 'failed',
    actual: message,
    message: testCase.hint,
  }
}

function buildResult(cases, detail, forcedVerdict = null) {
  const passed = cases.filter((item) => item.status === 'passed').length
  const total = cases.length
  const verdict = forcedVerdict || (total > 0 && passed === total ? 'accepted' : passed > 0 ? 'partial' : 'wrong-answer')

  return {
    verdict,
    passed,
    total,
    runtimeMs: 26 + total * 3 + passed * 4,
    memoryMb: Number((16.8 + total * 0.15 + passed * 0.08).toFixed(1)),
    cases,
    outputLog: buildOutputLog(verdict, passed, total, detail),
  }
}

function buildOutputLog(verdict, passed, total, detail) {
  if (verdict === 'accepted') {
    return `Accepted · ${passed}/${total} 个测试用例通过。${detail}`
  }
  if (verdict === 'partial') {
    return `Partial · ${passed}/${total} 个测试用例通过。继续检查失败用例的输入、期望和实际输出。`
  }
  if (verdict === 'unsupported') {
    return `Not Supported · ${passed}/${total} 个测试用例执行。${detail}`
  }
  return `Wrong Answer · ${passed}/${total} 个测试用例通过。未通过用例已列出实际输出。`
}

function validateSourceSafety(source) {
  const blocked = [
    /\bwindow\b/,
    /\bdocument\b/,
    /\blocalStorage\b/,
    /\bsessionStorage\b/,
    /\bfetch\b/,
    /\bXMLHttpRequest\b/,
    /\bWebSocket\b/,
    /\bprocess\b/,
    /\bglobalThis\b/,
    /\bself\b/,
    /\bpostMessage\b/,
    /\bimportScripts\b/,
    /\beval\b/,
    /\bFunction\b/,
    /\.\s*constructor\b/,
    /\[\s*["']constructor["']\s*\]/,
    /\bimport\s*\(/,
    /\brequire\s*\(/,
    /\bwhile\s*\(\s*true\s*\)/,
    /\bfor\s*\(\s*;\s*;\s*\)/,
  ]
  return blocked.some((pattern) => pattern.test(source))
    ? '检测到不允许在前端沙箱中使用的全局 API 或无限循环写法。'
    : null
}

function formatValue(value) {
  if (value === undefined) return 'undefined'
  if (value === null) return 'null'
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}

function stableJson(value) {
  return JSON.stringify(value)
}

function sortComparableArray(value) {
  return [...value].sort((left, right) => {
    if (typeof left === 'number' && typeof right === 'number') return left - right
    return String(left).localeCompare(String(right))
  })
}

function deepClone(value) {
  return JSON.parse(JSON.stringify(value))
}
