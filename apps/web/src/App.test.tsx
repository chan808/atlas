import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import {
  InquiryRequestError,
  type CapturedInquiry,
  type ClarificationTurn,
} from './inquiryApi'

const capturedAt = '2026-08-29T06:00:00Z'

function capturedInquiry(rawText: string): CapturedInquiry {
  return {
    id: '4ee1572a-ecf1-4ce4-a661-e58094f9255e',
    rawText,
    status: 'CAPTURED',
    version: 0,
    createdAt: capturedAt,
  }
}

function clarificationTurn(inquiryId: string): ClarificationTurn {
  return {
    id: 'bd2d0fe4-e7e7-47e6-8cbe-fbc5d8abce9a',
    inquiryId,
    sequence: 1,
    question:
      '원리를 설명하는 것과 작은 예제로 확인하는 것 중 무엇이 먼저인가요?',
    reason: '답에 따라 다음 명확화 방향과 필요한 증거가 달라집니다.',
    inquiryVersion: 1,
    createdAt: '2026-08-29T06:01:00Z',
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

  it('starts only after an explicit action and keeps the raw text beside one question and reason', async () => {
    const user = userEvent.setup()
    const rawText = '  timeout과 retry\r\n한글\t😀e\u0301  '
    const inquiry = capturedInquiry(rawText)
    const turn = clarificationTurn(inquiry.id)
    const capture = vi.fn().mockResolvedValue(inquiry)
    const startClarificationRequest = vi.fn().mockResolvedValue(turn)
    render(
      <App
        capture={capture}
        startClarificationRequest={startClarificationRequest}
        createIdempotencyKey={() => 'capture-key'}
        createClarificationIdempotencyKey={() => 'clarification-key'}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: rawText } },
    )

    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))

    const startButton = await screen.findByRole('button', {
      name: '명확화 질문 하나 받기',
    })
    expect(startClarificationRequest).not.toHaveBeenCalled()
    expect(screen.getByLabelText('저장된 원문').textContent).toBe(rawText)

    await user.click(startButton)

    expect(startClarificationRequest).toHaveBeenCalledWith(
      inquiry.id,
      inquiry.version,
      'clarification-key',
    )
    expect(
      await screen.findByRole('heading', { name: turn.question }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: '왜 이 답이 중요한가요?' }),
    ).toBeInTheDocument()
    expect(screen.getByText(turn.reason)).toBeInTheDocument()
    expect(screen.getByLabelText('저장된 원문').textContent).toBe(rawText)
    expect(
      screen.getByText(/답변과 다음 학습 단계는 아직 시작하지 않습니다/),
    ).toBeInTheDocument()
  })

  it('disables duplicate clarification starts while the request is in flight', async () => {
    const user = userEvent.setup()
    const inquiry = capturedInquiry('동시 질문 요청을 막는다')
    let resolveStart: (turn: ClarificationTurn) => void = () => undefined
    const pendingStart = new Promise<ClarificationTurn>((resolve) => {
      resolveStart = resolve
    })
    const startClarificationRequest = vi.fn().mockReturnValue(pendingStart)
    render(
      <App
        capture={vi.fn().mockResolvedValue(inquiry)}
        startClarificationRequest={startClarificationRequest}
        createClarificationIdempotencyKey={() => 'pending-start-key'}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: inquiry.rawText } },
    )
    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))
    await user.click(
      await screen.findByRole('button', { name: '명확화 질문 하나 받기' }),
    )

    const pendingButton = screen.getByRole('button', {
      name: '질문을 준비하는 중…',
    })
    expect(pendingButton).toBeDisabled()
    await user.click(pendingButton)
    expect(startClarificationRequest).toHaveBeenCalledTimes(1)

    resolveStart(clarificationTurn(inquiry.id))
    expect(
      await screen.findByText('왜 이 답이 중요한가요?'),
    ).toBeInTheDocument()
  })

  it('reuses one key and version after ambiguous and 5xx failures', async () => {
    const user = userEvent.setup()
    const inquiry = capturedInquiry('재시도 계약을 확인한다')
    const startClarificationRequest = vi
      .fn()
      .mockRejectedValueOnce(new InquiryRequestError('start', null))
      .mockRejectedValueOnce(new InquiryRequestError('start', 503))
      .mockResolvedValueOnce(clarificationTurn(inquiry.id))
    const createClarificationIdempotencyKey = vi
      .fn()
      .mockReturnValue('one-retry-key')
    render(
      <App
        capture={vi.fn().mockResolvedValue(inquiry)}
        startClarificationRequest={startClarificationRequest}
        createClarificationIdempotencyKey={createClarificationIdempotencyKey}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: inquiry.rawText } },
    )
    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))

    await user.click(
      await screen.findByRole('button', { name: '명확화 질문 하나 받기' }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('같은 요청')
    await user.click(
      screen.getByRole('button', { name: '같은 요청으로 다시 시도하기' }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('같은 요청')
    await user.click(
      screen.getByRole('button', { name: '같은 요청으로 다시 시도하기' }),
    )

    expect(
      await screen.findByRole('heading', {
        name: clarificationTurn(inquiry.id).question,
      }),
    ).toBeInTheDocument()
    expect(startClarificationRequest).toHaveBeenCalledTimes(3)
    for (const call of startClarificationRequest.mock.calls) {
      expect(call).toEqual([inquiry.id, 0, 'one-retry-key'])
    }
    expect(createClarificationIdempotencyKey).toHaveBeenCalledTimes(1)
    expect(screen.getByLabelText('저장된 원문')).toHaveTextContent(
      inquiry.rawText,
    )
  })

  it.each([400, 404])(
    'treats status %s as terminal without repeating the start',
    async (status) => {
      const user = userEvent.setup()
      const inquiry = capturedInquiry(`terminal ${status}`)
      const startClarificationRequest = vi
        .fn()
        .mockRejectedValue(new InquiryRequestError('start', status))
      render(
        <App
          capture={vi.fn().mockResolvedValue(inquiry)}
          startClarificationRequest={startClarificationRequest}
        />,
      )
      fireEvent.change(
        screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
        { target: { value: inquiry.rawText } },
      )
      await user.click(
        screen.getByRole('button', { name: '이 생각부터 보관하기' }),
      )
      await user.click(
        await screen.findByRole('button', { name: '명확화 질문 하나 받기' }),
      )

      expect(await screen.findByRole('alert')).toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: /명확화|같은 요청/ }),
      ).not.toBeInTheDocument()
      expect(startClarificationRequest).toHaveBeenCalledTimes(1)
      expect(screen.getByLabelText('저장된 원문')).toHaveTextContent(
        inquiry.rawText,
      )
    },
  )

  it('recovers the committed current turn once after a 409 winner', async () => {
    const user = userEvent.setup()
    const inquiry = capturedInquiry('경쟁에서 이긴 질문을 읽는다')
    const turn = clarificationTurn(inquiry.id)
    const startClarificationRequest = vi
      .fn()
      .mockRejectedValue(new InquiryRequestError('start', 409))
    const getCurrentTurn = vi.fn().mockResolvedValue(turn)
    render(
      <App
        capture={vi.fn().mockResolvedValue(inquiry)}
        startClarificationRequest={startClarificationRequest}
        getCurrentTurn={getCurrentTurn}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: inquiry.rawText } },
    )
    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))
    await user.click(
      await screen.findByRole('button', { name: '명확화 질문 하나 받기' }),
    )

    expect(
      await screen.findByRole('heading', { name: turn.question }),
    ).toBeInTheDocument()
    expect(startClarificationRequest).toHaveBeenCalledTimes(1)
    expect(getCurrentTurn).toHaveBeenCalledTimes(1)
    expect(getCurrentTurn).toHaveBeenCalledWith(inquiry.id)
  })

  it('stops after one failed current-turn read following a 409', async () => {
    const user = userEvent.setup()
    const inquiry = capturedInquiry('현재 질문이 없으면 충돌을 끝낸다')
    const startClarificationRequest = vi
      .fn()
      .mockRejectedValue(new InquiryRequestError('start', 409))
    const getCurrentTurn = vi
      .fn()
      .mockRejectedValue(new InquiryRequestError('current', 404))
    render(
      <App
        capture={vi.fn().mockResolvedValue(inquiry)}
        startClarificationRequest={startClarificationRequest}
        getCurrentTurn={getCurrentTurn}
      />,
    )
    fireEvent.change(
      screen.getByRole('textbox', { name: '정리되지 않은 생각' }),
      { target: { value: inquiry.rawText } },
    )
    await user.click(screen.getByRole('button', { name: '이 생각부터 보관하기' }))
    await user.click(
      await screen.findByRole('button', { name: '명확화 질문 하나 받기' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('현재 질문')
    expect(
      screen.queryByRole('button', { name: /명확화|같은 요청/ }),
    ).not.toBeInTheDocument()
    expect(startClarificationRequest).toHaveBeenCalledTimes(1)
    expect(getCurrentTurn).toHaveBeenCalledTimes(1)
  })
})
