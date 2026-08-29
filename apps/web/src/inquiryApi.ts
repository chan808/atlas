export interface CapturedInquiry {
  id: string
  rawText: string
  status: 'CAPTURED'
  createdAt: string
}

export async function captureInquiry(
  rawText: string,
  idempotencyKey: string,
): Promise<CapturedInquiry> {
  const response = await fetch('/api/inquiries', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify({ rawText }),
  })

  if (!response.ok) {
    throw new Error(`Inquiry capture failed with status ${response.status}`)
  }

  return (await response.json()) as CapturedInquiry
}
