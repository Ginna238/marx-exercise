import request from './request'

export function startExam(bankId) {
  return request.post('/exam/start', null, {
    params: { bankId }
  })
}

export function submitExam(data) {
  return request.post('/exam/submit', data)
}

export function getExamRecords(bankId = null) {
  return request.get('/exam/records', {
    params: { bankId }
  })
}

export function getExamStats() {
  return request.get('/exam/stats')
}
