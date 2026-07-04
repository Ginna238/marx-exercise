import request from './request'

export function getAdminInfo() {
  return request.get('/admin/me')
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getUsers() {
  return request.get('/admin/users')
}

export function resetPassword(userId) {
  return request.put(`/admin/users/${userId}/reset-password`)
}

export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}

export function getAdminBanks() {
  return request.get('/admin/banks')
}

export function reloadBanks() {
  return request.post('/admin/banks/reload')
}

export function getAdminQuestions(bankId = null, type = null) {
  return request.get('/admin/questions', { params: { bankId, type } })
}

/** GitHub 题库更新 */
export function listGitHubBanks() {
  return request.get('/admin/github-banks')
}

export function importFromGitHub(fileInfo) {
  return request.post('/admin/github-import', fileInfo)
}

export function syncAllGitHub() {
  return request.post('/admin/github-sync-all')
}

/** 获取用户刷题情况 */
export function getUserStats() {
  return request.get('/admin/user-stats')
}
