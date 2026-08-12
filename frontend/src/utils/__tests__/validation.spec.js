import { describe, it, expect } from 'vitest'
import {
  validateRequired,
  validateEmail,
  validateTCKN,
  validateTaxNumber,
  validateIBAN,
  validateMinLength
} from '../validation.js'

describe('validation.js', () => {
  it('validateRequired', () => {
    expect(validateRequired(null)).toBe(false)
    expect(validateRequired(undefined)).toBe(false)
    expect(validateRequired('')).toBe(false)
    expect(validateRequired('   ')).toBe(false)
    expect(validateRequired('x')).toBe(true)
    expect(validateRequired(0)).toBe(true)
  })

  it('validateEmail', () => {
    expect(validateEmail('')).toBe(true)
    expect(validateEmail(null)).toBe(true)
    expect(validateEmail('test@example.com')).toBe(true)
    expect(validateEmail('not-an-email')).toBe(false)
  })

  it('validateTCKN accepts empty', () => {
    expect(validateTCKN('')).toBe(true)
    expect(validateTCKN(null)).toBe(true)
    expect(validateTCKN('12345')).toBe(false)
    expect(validateTCKN('01234567890')).toBe(false)
  })

  it('validateTCKN validates a real T.C. number', () => {
    expect(validateTCKN('10000000146')).toBe(true)
    expect(validateTCKN('10000000147')).toBe(false)
  })

  it('validateTaxNumber', () => {
    expect(validateTaxNumber('')).toBe(true)
    expect(validateTaxNumber('1234567890')).toBe(true)
    expect(validateTaxNumber('12345678901')).toBe(true)
    expect(validateTaxNumber('1234')).toBe(false)
  })

  it('validateIBAN', () => {
    expect(validateIBAN('')).toBe(true)
    expect(validateIBAN('TR33 0006 1005 1978 6457 8413 26')).toBe(true)
    expect(validateIBAN('TR12')).toBe(false)
  })

  it('validateMinLength', () => {
    expect(validateMinLength('abc', 2)).toBe(true)
    expect(validateMinLength('a', 2)).toBe(false)
    expect(validateMinLength('', 1)).toBe(false)
  })
})
