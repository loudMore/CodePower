/** 题目与题解 API — 题目增删改查、标签、题解管理 */
import api from './index';

interface ProblemQueryParams {
  page?: number;
  size?: number;
  difficulty?: string;
  category?: string;
  search?: string;
}

interface SubmissionData {
  code: string;
  language: string;
}

interface ProblemData {
  title: string;
  difficulty: string;
  categories: string[];
  description: string;
  inputFormat: string;
  outputFormat: string;
  examples?: Array<{
    input: string;
    output: string;
    explanation?: string;
  }>;
  inputExample: string;
  outputExample: string;
  debugInputExample: string;
  debugOutputExample: string;
  hint?: string;
  testCases: Array<{
    input: string;
    output: string;
    score: number;
  }>;
  solution?: string;
  solutionCode?: Record<string, string>;
  timeLimit: number;
  memoryLimit: number;
  isPublic: boolean;
}

/** 获取题目列表（分页） */
export function getProblems(params?: any) {
  return api.get('/api/problems', { params })
}

/** 获取题目详情 */
export function getProblemById(id: number) {
  return api.get(`/api/problems/${id}`)
}

/** 获取指定作者的题目 */
export function getProblemsByAuthor(authorId: number) {
  return api.get(`/api/problems/author/${authorId}`)
}

/** 更新题目可见性 */
export function updateProblemVisibility(id: number, visibility: string) {
  return api.put(`/api/problems/${id}/visibility`, { visibility })
}

/** 根据ID获取标签 */
export function getTagById(id: number) {
  return api.get(`/api/tags/${id}`)
}

/** 创建标签 */
export function createTag(tagData: any) {
  return api.post('/api/tags', tagData)
}

/** 获取题目关联的标签 */
export function getTagsByProblemId(problemId: number) {
  return api.get(`/api/tags/problem/${problemId}`)
}

/** 获取题目测试用例 */
export function getProblemTestCases(id: number) {
  return api.get(`/api/problems/${id}/test-cases`);
}

/** 提交题目代码 */
export function submitProblemCode(problemId: number, submissionData: any) {
  return api.post(`/api/problems/${problemId}/submit`, submissionData);
}

/** AI生成题目（旧接口） */
export function generateProblem(prompt: string) {
  return api.post('/api/problems/generate', { prompt });
}

/** AI生成题解（旧接口） */
export function generateSolution(prompt: string, problemData: any) {
  return api.post('/api/problems/generate-solution', { prompt, problemData });
}

/** 创建题目 */
export function createProblem(data: any) {
  return api.post('/api/problems', data)
}

/** 更新题目 */
export function updateProblem(id: number, data: any) {
  return api.put(`/api/problems/${id}`, data)
}

/** 删除题目 */
export function deleteProblem(id: number) {
  return api.delete(`/api/problems/${id}`)
}

/** 获取全部标签 */
export function getAllTags() {
  return api.get('/api/tags')
}

/** 获取题目的所有题解 */
export function getProblemSolutions(problemId: number) {
  return api.get(`/api/solutions/problem/${problemId}`)
}

/** 获取题目的官方题解 */
export function getOfficialSolutions(problemId: number) {
  return api.get(`/api/solutions/problem/${problemId}/official`)
}

/** 按语言获取题目题解 */
export function getProblemSolutionsByLanguage(problemId: number, language: string) {
  return api.get(`/api/solutions/problem/${problemId}/language/${language}`)
}

/** 创建题解 */
export function createSolution(data: any) {
  return api.post('/api/solutions', data)
}

/** 更新题解 */
export function updateSolution(id: number, data: any) {
  return api.put(`/api/solutions/${id}`, data)
}

/** 删除题解 */
export function deleteSolution(id: number) {
  return api.delete(`/api/solutions/${id}`)
}
