import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  captureInquiry,
  getCurrentClarificationTurn,
  InquiryRequestError,
  startClarification,
  type CapturedInquiry,
  type ClarificationTurn,
} from './inquiryApi'

const clarificationTurn: ClarificationTurn = {
  id: 'bd2d0fe4-e7e7-47e6-8cbe-fbc5d8abce9a',
  inquiryId: '4ee1572a-ecf1-4ce4-a661-e58094f9255e',
  sequence: 1,
  question: '원리를 설명하는 것과 작은 예제로 확인하는 것 중 무엇이 먼저인가요?',
  reason: '답에 따라 다음 명확화 방향과 필요한 증거가 달라집니다.',
  inquiryVersion: 1,
  createdAt: '2026-08-29T06:01:00Z',
}

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
      version: 0,
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

  it('starts clarification with the captured version and a dedicated key', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => clarificationTurn,
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      startClarification(
        clarificationTurn.inquiryId,
        0,
        'clarification-start-key',
      ),
    ).resolves.toEqual(clarificationTurn)
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/inquiries/${clarificationTurn.inquiryId}/clarification-turns`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': 'clarification-start-key',
        },
        body: JSON.stringify({ inquiryVersion: 0 }),
      },
    )
  })

  it('retrieves the current clarification turn for conflict recovery', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => clarificationTurn,
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      getCurrentClarificationTurn(clarificationTurn.inquiryId),
    ).resolves.toEqual(clarificationTurn)
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/inquiries/${clarificationTurn.inquiryId}/clarification-turns/current`,
    )
  })

  it('classifies response and transport failures without reading their bodies', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({ ok: false, status: 503 })
      .mockRejectedValueOnce(new TypeError('offline private details'))
    vi.stubGlobal('fetch', fetchMock)

    const serverFailure = await startClarification(
      clarificationTurn.inquiryId,
      0,
      'server-failure-key',
    ).catch((error: unknown) => error)
    const transportFailure = await startClarification(
      clarificationTurn.inquiryId,
      0,
      'transport-failure-key',
    ).catch((error: unknown) => error)

    expect(serverFailure).toBeInstanceOf(InquiryRequestError)
    expect(serverFailure).toMatchObject({ status: 503 })
    expect(transportFailure).toBeInstanceOf(InquiryRequestError)
    expect(transportFailure).toMatchObject({ status: null })
    expect(String(transportFailure)).not.toContain('private details')
  })
})
