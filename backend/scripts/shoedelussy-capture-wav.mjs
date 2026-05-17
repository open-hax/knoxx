#!/usr/bin/env node
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { chromium } from 'playwright'

const [renderUrlArg, outputPathArg, timeoutArg] = process.argv.slice(2)

if (!renderUrlArg || !outputPathArg) {
  console.error('Usage: node shoedelussy-capture-wav.mjs <render-url> <output-path> [timeout-ms]')
  process.exit(1)
}

const timeoutMs = Number.parseInt(timeoutArg || '120000', 10)
const safeTimeoutMs = Number.isFinite(timeoutMs) ? Math.max(1000, timeoutMs) : 120000
const outputPath = path.resolve(outputPathArg)
const executablePath = process.env.KNOXX_HEADLESS_CHROMIUM || process.env.CHROMIUM_PATH || '/snap/bin/chromium'
const renderUrl = (() => {
  const url = new URL(renderUrlArg)
  url.searchParams.set('headless', '1')
  return url.toString()
})()

const browser = await chromium.launch({
  headless: true,
  executablePath,
  args: [
    '--autoplay-policy=no-user-gesture-required',
    '--no-sandbox',
    '--disable-setuid-sandbox',
    '--disable-dev-shm-usage',
  ],
})

try {
  const context = await browser.newContext()
  const page = await context.newPage()
  
  // ADDED: Capture console logs
  page.on('console', msg => {
    console.log(`[BROWSER] ${msg.type()}: ${msg.text()}`)
  })
  page.on('pageerror', err => {
    console.log(`[BROWSER ERROR] ${err.message}`)
  })

  await page.goto(renderUrl, { waitUntil: 'domcontentloaded', timeout: safeTimeoutMs })
  await page.waitForLoadState('networkidle', { timeout: Math.min(safeTimeoutMs, 15000) }).catch(() => undefined)
  await page.mouse.click(16, 16).catch(() => undefined)

  await page.waitForSelector('[data-testid="render-audio-btn"]', { timeout: safeTimeoutMs })
  await page.click('[data-testid="render-audio-btn"]')

  await page.waitForFunction(() => {
    const result = window.__shoedelussyHeadlessExport
    return result && typeof result.ok === 'boolean'
  }, { timeout: safeTimeoutMs })

  const result = await page.evaluate(() => window.__shoedelussyHeadlessExport)
  if (!result?.ok) {
    throw new Error(result?.error || 'Headless Shoedelussy export failed')
  }

  const dataUrl = String(result.dataUrl || '')
  const match = dataUrl.match(/^data:audio\/wav;base64,(.+)$/)
  if (!match) {
    throw new Error('Headless Shoedelussy export did not return a valid audio/wav data URL')
  }

  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, Buffer.from(match[1], 'base64'))

  console.log(JSON.stringify({
    ok: true,
    outputPath,
    suggestedFilename: result.fileName || path.basename(outputPath),
    sourceUrl: renderUrlArg,
    headlessUrl: renderUrl,
  }))
} catch (error) {
  const message = error instanceof Error ? error.message : String(error)
  console.error(message)
  process.exit(2)
} finally {
  await browser.close()
}
