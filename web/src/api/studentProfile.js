import http from '@/utils/http.js'

export function getStudentProfile() {
    return http.get('/profile/get-student-profile')
}

export function getDashboard() {
    return http.get('/profile/dashboard')
}

export function analyzeProfile() {
    return http.post('/student-profile/analyze')
}

export function analyzeProfileFromBehavior() {
    return http.post('/student-profile/analyze-behavior')
}

export function deleteStudentProfile() {
    return http.delete('/profile/delete-student-profile')
}
