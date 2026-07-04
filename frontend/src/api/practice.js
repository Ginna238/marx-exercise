import request from './request'

export function startPractice(bankId, questionType, roundNum = 1) {
  return request.get('/practice/start', {
    params: { bankId, questionType, roundNum }
  })
}

export function getPracticeQuestions(bankId, questionType, roundNum = 1) {
  return request.get('/practice/questions', {
    params: { bankId, questionType, roundNum }
  })
}

export function submitAnswer(data) {
  return request.post('/practice/submit', data)
}

export function checkRoundComplete(bankId, questionType, roundNum = 1) {
  return request.get('/practice/check-complete', {
    params: { bankId, questionType, roundNum }
  })
}

export function getSuggestedRound(bankId, questionType) {
  return request.get('/practice/suggest-round', {
    params: { bankId, questionType }
  })
}

export function startWrongPractice(bankId) {
  return request.get('/practice/wrong-start', {
    params: { bankId }
  })
}
