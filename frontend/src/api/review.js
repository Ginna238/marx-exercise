import request from './request'

export function getReviewQuestions(bankId, type = '', keyword = '') {
  return request.get('/review/questions', { params: { bankId, type, keyword } })
}
