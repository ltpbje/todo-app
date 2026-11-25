import test from 'node:test'
import assert from 'node:assert/strict'
import { dateKey, completedOn, completedSince, addDays, monthCells } from './plan.js'

test('dateKey 生成连续本地日期', () => {
  const today = dateKey()
  assert.match(today, /^\d{4}-\d{2}-\d{2}$/)
  assert.equal(completedOn({ completedAt: today + 'T08:30:00' }, today), true)
  assert.equal(completedOn({ completedAt: dateKey(-1) + 'T08:30:00' }, today), false)
})

test('completedSince 按自然日计算最近 N 天', () => {
  assert.equal(completedSince({ completedAt: dateKey() + 'T23:00:00' }, 7), true)
  assert.equal(completedSince({ completedAt: dateKey(-8) + 'T23:00:00' }, 7), false)
  assert.equal(completedSince({ completedAt: null }, 7), false)
})

test('addDays 跨月正确', () => {
  assert.equal(addDays('2026-01-31', 1), '2026-02-01')
  assert.equal(addDays('2026-12-31', 1), '2027-01-01')
})

test('monthCells 给出完整周网格', () => {
  const cells = monthCells(2026, 10)
  assert.equal(cells.length, 42)
  assert.equal(cells[0], '2026-11-01')
  assert.ok(cells.includes('2026-11-20'))
})
