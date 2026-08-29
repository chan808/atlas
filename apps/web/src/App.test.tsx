import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import type { CapturedInquiry } from './inquiryApi'

const capturedAt = '2026-08-29T06:00:00Z'

function capturedInquiry(rawText: string): CapturedInquiry {
  return {
    id: '4ee1572a-ecf1-4ce4-a661-e58094f9255e',
    rawText,
    status: 'CAPTURED',
    createdAt: capturedAt,
  }
}

describe('App', () => {
  it('presents an explicitly labelled Brain Dump capture form', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', {
        name: '아직 질문이 아니어도 괜찮아요.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
    ).toBeInTheDocument()
    expect(screen.getByText('원문은 보존합니다.')).toBeInTheDocument()
  })

  it('rejects whitespace-only input without sending a request', async () => {
    const user = userEvent.setup()
    const capture = vi.fn()
    render(<App capture={capture} />)
    const input = screen.getByRole('textbox', { name: '정리되지 않은 생각' })
    fireEvent.change(input, { target: { value: ' \t\n ' } })

    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))

    expect(capture).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveAttribute('id', 'brain-dump-error')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAttribute(
      'aria-describedby',
      expect.stringContaining('brain-dump-error'),
    )
    expect(input).toHaveValue(' \t\n ')
  })

  it('uses the same Unicode whitespace boundary as the API', async () => {
    const user = userEvent.setup()
    const capture = vi.fn()
    render(<App capture={capture} />)
    const input = screen.getByRole('textbox', { name: '정리되지 않은 생각' })
    fireEvent.change(input, { target: { value: '\u001c\u00a0\u3000' } })

    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))

    expect(capture).not.toHaveBeenCalled()
    expect(input).toHaveAttribute('aria-invalid', 'true')
  })

  it('adds starter prompts to the draft without submitting or overwriting it', async () => {
    const user = userEvent.setup()
    const capture = vi.fn()
    render(<App capture={capture} />)
    const input = screen.getByRole('textbox', { name: '정리되지 않은 생각' })

    await user.click(
      screen.getByRole('button', {
        name: '자주 보이지만 설명하지 못하는 단어',
      }),
    )
    await user.click(
      screen.getByRole('button', { name: '장애가 나면 두려울 것 같은 부분' }),
    )

    expect(input).toHaveValue(
      '자주 보이지만 설명하지 못하는 단어\n장애가 나면 두려울 것 같은 부분',
    )
    expect(capture).not.toHaveBeenCalled()
  })

  it('submits the exact draft and renders the server-returned raw text', async () => {
    const user = userEvent.setup()
    const rawText = '  Kafka\n한글\t😀e\u0301  '
    const serverRawText = `${rawText}\nserver-preserved`
    const capture = vi.fn().mockResolvedValue(capturedInquiry(serverRawText))
    const createIdempotencyKey = vi.fn().mockReturnValue('capture-key-one')
    render(
      <App
        capture={capture}
        createIdempotencyKey={createIdempotencyKey}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: rawText } },
    )

    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))

    expect(await screen.findByRole('status')).toBeInTheDocument()
    expect(capture).toHaveBeenCalledWith(rawText, 'capture-key-one')
    expect(screen.getByLabelText('저장된 원문').textContent).toBe(serverRawText)
    expect(screen.getByText('CAPTURED')).toBeInTheDocument()
  })

  it('keeps the draft and key after failure, then changes the key after editing', async () => {
    const user = userEvent.setup()
    const rawText = '  실패해도 보존할 생각\n두 번째 줄  '
    const editedText = `${rawText}\n새로 추가한 내용`
    const capture = vi.fn().mockRejectedValue(new Error('offline'))
    const createIdempotencyKey = vi
      .fn()
      .mockReturnValueOnce('retry-key-one')
      .mockReturnValueOnce('retry-key-two')
    render(
      <App
        capture={capture}
        createIdempotencyKey={createIdempotencyKey}
      />,
    )
    const input = screen.getByRole('textbox', { name: '정리되지 않은 생각' })
    const submit = screen.getByRole('button', { name: '이 생각부터 보관하기' })
    fireEvent.change(input, { target: { value: rawText } })

    await user.click(submit)
    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(input).toHaveValue(rawText)
    expect(input).toHaveAttribute('aria-invalid', 'false')
    expect(capture).toHaveBeenNthCalledWith(1, rawText, 'retry-key-one')

    await user.click(submit)
    expect(capture).toHaveBeenNthCalledWith(2, rawText, 'retry-key-one')
    expect(createIdempotencyKey).toHaveBeenCalledTimes(1)

    fireEvent.change(input, { target: { value: editedText } })
    await user.click(submit)
    expect(capture).toHaveBeenNthCalledWith(3, editedText, 'retry-key-two')
    expect(createIdempotencyKey).toHaveBeenCalledTimes(2)
  })

  it('prevents duplicate submissions while a request is in flight', async () => {
    const user = userEvent.setup()
    let resolveCapture: (inquiry: CapturedInquiry) => void = () => undefined
    const pendingCapture = new Promise<CapturedInquiry>((resolve) => {
      resolveCapture = resolve
    })
    const capture = vi.fn().mockReturnValue(pendingCapture)
    render(
      <App
        capture={capture}
        createIdempotencyKey={() => 'in-flight-key'}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: '동시 제출을 막아야 한다' } },
    )
    const submit = screen.getByRole('button', { name: '이 생각부터 보관하기' })

    await user.click(submit)
    expect(screen.getByRole('button', { name: '원문을 보관하는 중…' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: '원문을 보관하는 중…' }))
    expect(capture).toHaveBeenCalledTimes(1)

    resolveCapture(capturedInquiry('동시 제출을 막아야 한다'))
    expect(await screen.findByRole('status')).toBeInTheDocument()
  })

  it('counts Unicode code points and rejects 10,001 before the request', async () => {
    const user = userEvent.setup()
    const capture = vi.fn()
    render(<App capture={capture} />)
    const tooLong = '😀'.repeat(10_001)
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: tooLong } },
    )

    expect(screen.getByText('10,001 / 10,000')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))

    expect(capture).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('10,000자')
  })
})
