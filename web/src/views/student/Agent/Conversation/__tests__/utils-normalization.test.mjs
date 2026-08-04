import assert from 'node:assert/strict';
import {
  hasAnyResource,
  normalizeMermaidDiagram,
  normalizeResources,
  normalizeTableData,
  parseMarkdownSegments,
  separateThinkingAndJson,
} from '../utils.js';

const mixedResources = normalizeResources({
  images: [{ href: 'https://example.com/a.png', name: 'Diagram', desc: 'Flow chart', platform: 'Docs' }],
  videos: [{ link: 'https://example.com/v', label: 'Lesson', summary: 'Intro video', provider: 'Bilibili' }],
  articles: [{ resourceUrl: 'https://example.com/p', title: '', content: 'Long article text' }],
});

assert.equal(mixedResources.images[0].url, 'https://example.com/a.png');
assert.equal(mixedResources.images[0].title, 'Diagram');
assert.equal(mixedResources.images[0].description, 'Flow chart');
assert.equal(mixedResources.videos[0].source, 'Bilibili');
assert.equal(mixedResources.articles[0].title, 'Untitled resource');
assert.equal(mixedResources.articles[0].snippet, 'Long article text');
assert.equal(hasAnyResource(mixedResources), true);
assert.equal(hasAnyResource(null), false);

const table = normalizeTableData(['Name', '', 'Remark'], [
  ['Alice', null],
  ['Bob', 'Pass', 'Long\nnote'],
  'invalid row',
]);

assert.deepEqual(table.headers, ['Name', 'Column 2', 'Remark']);
assert.deepEqual(table.rows, [
  ['Alice', '-', '-'],
  ['Bob', 'Pass', 'Long\nnote'],
]);

const extracted = separateThinkingAndJson('Summary\n```json\n{"message":"ok","resources":{"articles":[{"url":"u","title":"t"}]}}\n```');
assert.equal(extracted.thinking, 'Summary');
assert.equal(extracted.json.resources.articles[0].title, 't');

const complexMarkdown = [
  'before',
  '',
  '特性 | 说明',
  '--- | ---',
  '**时间复杂度** | 支持 `a | b` 和 A\\|B',
  '**稳定性** | ✅ 不稳定（交换可能改变相等元素的相对顺序）',
  '',
  '```mermaid title',
  'graph TD',
  'A --> B',
  '```',
].join('\n');
const complexSegments = parseMarkdownSegments(complexMarkdown);
assert.equal(complexSegments[0].type, 'text');
assert.equal(complexSegments[1].type, 'table');
assert.deepEqual(complexSegments[1].headers, ['特性', '说明']);
assert.deepEqual(complexSegments[1].rows, [
  ['**时间复杂度**', '支持 `a | b` 和 A|B'],
  ['**稳定性**', '✅ 不稳定（交换可能改变相等元素的相对顺序）'],
]);
assert.equal(complexSegments[2].type, 'mermaid');
assert.match(complexSegments[2].content, /graph TD/);

const timelineDiagram = [
  'timeline',
  '  title Java learning path',
  '  section Week 1-2',
  '  2024-06-01 : Variables -> JAV-01<br/>Can write BMI calculator',
  '  2024-06-07 : Flow control -> JAV-02<br/>Guess number game',
].join('\n');
const timelineFlowchart = normalizeMermaidDiagram(timelineDiagram);
assert.match(timelineFlowchart, /^flowchart LR/);
assert.match(timelineFlowchart, /Java learning path/);
assert.match(timelineFlowchart, /2024-06-01<br\/>Variables -&gt; JAV-01/);
assert.doesNotMatch(timelineFlowchart, /^timeline/);

const flowchartDiagram = 'flowchart TD\n    A["Start"] --> B["End"]';
assert.equal(normalizeMermaidDiagram(flowchartDiagram), flowchartDiagram);

const falsePositiveSegments = parseMarkdownSegments('这只是 a | b 的普通句子\n---\n不是表格。');
assert.equal(falsePositiveSegments.length, 1);
assert.equal(falsePositiveSegments[0].type, 'text');

console.log('utils normalization tests passed');
