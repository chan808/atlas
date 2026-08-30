export interface CapturedInquiry {
  id: string
  rawText: string
  status: 'CAPTURED'
  version: number
  createdAt: string
}

export interface ClarificationTurn {
  id: string
  inquiryId: string
  sequence: 1
  question: string
  reason: string
  inquiryVersion: number
  createdAt: string
}

export class InquiryRequestError extends Error {
  readonly status: number | null

  constructor(operation: string, status: number | null, options?: ErrorOptions) {
    super(
      status === null
        ? `${operation} failed before a response was received`
        : `${operation} failed with status ${status}`,
      options,
    )
    this.name = 'InquiryRequestError'
    this.status = status
  }
}

export async function captureInquiry(
  rawText: string,
  idempotencyKey: string,
): Promise<CapturedInquiry> {
  let response: Response
  try {
    response = await fetch('/api/inquiries', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify({ rawText }),
    })
  } catch (cause) {
    throw new InquiryRequestError('Inquiry capture', null, { cause })
  }

  if (!response.ok) {
    throw new InquiryRequestError('Inquiry capture', response.status)
  }

  return (await response.json()) as CapturedInquiry
}

export async function startClarification(
  inquiryId: string,
  inquiryVersion: number,
  idempotencyKey: string,
): Promise<ClarificationTurn> {
  let response: Response
  try {
    response = await fetch(`/api/inquiries/${inquiryId}/clarification-turns`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify({ inquiryVersion }),
    })
  } catch (cause) {
    throw new InquiryRequestError('Clarification start', null, { cause })
  }

  if (!response.ok) {
    throw new InquiryRequestError('Clarification start', response.status)
  }

  return (await response.json()) as ClarificationTurn
}

export async function getCurrentClarificationTurn(
  inquiryId: string,
): Promise<ClarificationTurn> {
  let response: Response
  try {
    response = await fetch(
      `/api/inquiries/${inquiryId}/clarification-turns/current`,
    )
  } catch (cause) {
    throw new InquiryRequestError('Current clarification retrieval', null, {
      cause,
    })
  }

  if (!response.ok) {
    throw new InquiryRequestError(
      'Current clarification retrieval',
      response.status,
    )
  }

  return (await response.json()) as ClarificationTurn
}
