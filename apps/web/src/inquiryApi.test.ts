import { afterEach, describe, expect, it, vi } from 'vitest'
import { captureInquiry, type CapturedInquiry } from './inquiryApi'

describe('captureInquiry', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends the exact raw text with its idempotency key', async () => {
    const rawText = '  Kafka\r\n한글\t😀e\u0301  '
    const inquiry: CapturedInquiry = {
      id: '4ee1572a-ecf1-4ce4-a661-e58094f9255e',
      rawText,
      status: 'CAPTURED',
      createdAt: '2026-08-29T06:00:00Z',
    }
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => inquiry,
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(captureInquiry(rawText, 'exact-request-key')).resolves.toEqual(
      inquiry,
    )
    expect(fetchMock).toHaveBeenCalledWith('/api/inquiries', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': 'exact-request-key',
      },
      body: JSON.stringify({ rawText }),
    })
  })

  it('rejects non-success responses without exposing the response body', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 409 }),
    )

    await expect(captureInquiry('private thought', 'private-key')).rejects.toThrow(
      'Inquiry capture failed with status 409',
    )
  })
})
