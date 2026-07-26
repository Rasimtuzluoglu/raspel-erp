/**
 * RasPel ERP Form Validation Helper Utility
 */

export const validateRequired = (val) => {
  if (val === null || val === undefined) return false
  if (typeof val === 'string') return val.trim().length > 0
  return true
}

export const validateEmail = (email) => {
  if (!email) return true // null/empty allowed if optional
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(String(email).toLowerCase())
}

export const validateTCKN = (tc) => {
  if (!tc) return true
  if (tc.length !== 11 || !/^\d+$/.test(tc)) return false
  if (tc[0] === '0') return false

  let digits = tc.split('').map(Number)
  let oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8]
  let evenSum = digits[1] + digits[3] + digits[5] + digits[7]
  let digit10 = (oddSum * 7 - evenSum) % 10
  let digit11 = (digits.slice(0, 10).reduce((a, b) => a + b, 0)) % 10

  return digits[9] === digit10 && digits[10] === digit11
}

export const validateTaxNumber = (vkn) => {
  if (!vkn) return true
  return /^\d{10}$/.test(vkn) || /^\d{11}$/.test(vkn)
}

export const validateIBAN = (iban) => {
  if (!iban) return true
  const clean = iban.replace(/\s+/g, '').toUpperCase()
  return /^TR\d{24}$/.test(clean)
}

export const validateMinLength = (str, min) => {
  if (!str) return false
  return String(str).trim().length >= min
}
