import request from './request'

export function getWrongQuestions(bankId = null, questionType = null, keyword = null) {
  return request.get('/wrong/list', {
    params: { bankId, questionType, keyword }
  })
}

export function getWrongStats() {
  return request.get('/wrong/stats')
}

export function removeWrongQuestion(questionId) {
  return request.delete(`/wrong/${questionId}`)
}

export function batchRemoveWrongQuestions(questionIds) {
  return request.post('/wrong/batch-remove', questionIds)
}
