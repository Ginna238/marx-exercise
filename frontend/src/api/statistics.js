import request from './request'

export function getStatistics(bankId = null) {
  return request.get('/statistics', {
    params: { bankId }
  })
}

export function getProgress() {
  return request.get('/statistics/progress')
}
