import { useRef, useState, type FormEvent } from 'react'
import './App.css'
import {
  captureInquiry,
  getCurrentClarificationTurn,
  InquiryRequestError,
  startClarification,
  type CapturedInquiry,
  type ClarificationTurn,
} from './inquiryApi'

const MAX_CODE_POINTS = 10_000

const startingPoints = [
  '자주 보이지만 설명하지 못하는 단어',
  '언젠가 만들고 싶은 것',
  '장애가 나면 두려울 것 같은 부분',
  '안다고 생각했지만 막상 설명하기 어려운 개념',
]

interface CaptureAttempt {
  rawText: string
  idempotencyKey: string
}

interface FormError {
  message: string
  invalidInput: boolean
}

interface ClarificationAttempt {
  inquiryId: string
  inquiryVersion: number
  idempotencyKey: string
}

interface ClarificationError {
  message: string
  terminal: boolean
}

interface AppProps {
  capture?: typeof captureInquiry
  startClarificationRequest?: typeof startClarification
  getCurrentTurn?: typeof getCurrentClarificationTurn
  createIdempotencyKey?: () => string
  createClarificationIdempotencyKey?: () => string
}

function codePointCount(value: string) {
  return Array.from(value).length
}

// Mirrors the API's Character.isWhitespace || Character.isSpaceChar boundary.
function isAtlasWhitespace(codePoint: number) {
  return (
    (codePoint >= 0x0009 && codePoint <= 0x000d) ||
    (codePoint >= 0x001c && codePoint <= 0x0020) ||
    codePoint === 0x00a0 ||
    codePoint === 0x1680 ||
    (codePoint >= 0x2000 && codePoint <= 0x200a) ||
    codePoint === 0x2028 ||
    codePoint === 0x2029 ||
    codePoint === 0x202f ||
    codePoint === 0x205f ||
    codePoint === 0x3000
  )
}

function hasNonWhitespace(value: string) {
  for (const character of value) {
    const codePoint = character.codePointAt(0)
    if (codePoint !== undefined && !isAtlasWhitespace(codePoint)) {
      return true
    }
  }

  return false
}

function validationMessage(rawText: string) {
  if (!hasNonWhitespace(rawText)) {
    return '정리되지 않아도 괜찮아요. 떠오르는 단어나 문장을 하나만 적어주세요.'
  }

  if (codePointCount(rawText) > MAX_CODE_POINTS) {
    return '생각을 10,000자 안으로 나눠 적어주세요.'
  }

  return null
}

function App({
  capture = captureInquiry,
  startClarificationRequest = startClarification,
  getCurrentTurn = getCurrentClarificationTurn,
  createIdempotencyKey = () => crypto.randomUUID(),
  createClarificationIdempotencyKey = () => crypto.randomUUID(),
}: AppProps) {
  const [rawText, setRawText] = useState('')
  const [attempt, setAttempt] = useState<CaptureAttempt | null>(null)
  const [capturedInquiry, setCapturedInquiry] =
    useState<CapturedInquiry | null>(null)
  const [error, setError] = useState<FormError | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [clarificationAttempt, setClarificationAttempt] =
    useState<ClarificationAttempt | null>(null)
  const [clarificationTurn, setClarificationTurn] =
    useState<ClarificationTurn | null>(null)
  const [clarificationError, setClarificationError] =
    useState<ClarificationError | null>(null)
  const [isStartingClarification, setIsStartingClarification] = useState(false)
  const submittingRef = useRef(false)
  const startingClarificationRef = useRef(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (submittingRef.current) {
      return
    }

    const invalidReason = validationMessage(rawText)
    if (invalidReason) {
      setError({ message: invalidReason, invalidInput: true })
      return
    }

    const activeAttempt =
      attempt?.rawText === rawText
        ? attempt
        : { rawText, idempotencyKey: createIdempotencyKey() }

    setAttempt(activeAttempt)
    setError(null)
    submittingRef.current = true
    setIsSubmitting(true)

    try {
      const inquiry = await capture(
        activeAttempt.rawText,
        activeAttempt.idempotencyKey,
      )
      setCapturedInquiry(inquiry)
      setClarificationAttempt(null)
      setClarificationTurn(null)
      setClarificationError(null)
    } catch {
      setError({
        message:
          '지금은 생각을 저장하지 못했어요. 입력은 그대로 두었으니 다시 시도해 주세요.',
        invalidInput: false,
      })
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  const updateRawText = (value: string) => {
    setRawText(value)
    setError(null)
  }

  const addStartingPoint = (startingPoint: string) => {
    updateRawText(rawText.length === 0 ? startingPoint : `${rawText}\n${startingPoint}`)
  }

  const handleStartClarification = async () => {
    if (
      capturedInquiry === null ||
      startingClarificationRef.current ||
      clarificationError?.terminal
    ) {
      return
    }

    const activeAttempt =
      clarificationAttempt?.inquiryId === capturedInquiry.id &&
      clarificationAttempt.inquiryVersion === capturedInquiry.version
        ? clarificationAttempt
        : {
            inquiryId: capturedInquiry.id,
            inquiryVersion: capturedInquiry.version,
            idempotencyKey: createClarificationIdempotencyKey(),
          }

    setClarificationAttempt(activeAttempt)
    setClarificationError(null)
    startingClarificationRef.current = true
    setIsStartingClarification(true)

    try {
      const turn = await startClarificationRequest(
        activeAttempt.inquiryId,
        activeAttempt.inquiryVersion,
        activeAttempt.idempotencyKey,
      )
      setClarificationTurn(turn)
    } catch (startError) {
      const status =
        startError instanceof InquiryRequestError ? startError.status : null

      if (status === 409) {
        try {
          const currentTurn = await getCurrentTurn(activeAttempt.inquiryId)
          setClarificationTurn(currentTurn)
        } catch {
          setClarificationError({
            message:
              '다른 요청이 먼저 처리됐지만 현재 질문을 확인하지 못했어요. 원문은 그대로 보관되어 있습니다.',
            terminal: true,
          })
        }
      } else if (status === null || status >= 500) {
        setClarificationError({
          message:
            '질문 요청의 결과를 확인하지 못했어요. 원문은 그대로 있으며 같은 요청으로 다시 시도할 수 있습니다.',
          terminal: false,
        })
      } else if (status === 404) {
        setClarificationError({
          message:
            '저장된 생각을 찾지 못해 질문을 시작할 수 없어요. 이 요청은 다시 보내지 않습니다.',
          terminal: true,
        })
      } else {
        setClarificationError({
          message:
            '질문 시작 요청이 유효하지 않아 다시 보내지 않습니다. 원문은 그대로 보관되어 있습니다.',
          terminal: true,
        })
      }
    } finally {
      startingClarificationRef.current = false
      setIsStartingClarification(false)
    }
  }

  return (
    <main className="page-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Project Atlas 홈">
          <span className="brand-mark" aria-hidden="true">
            A
          </span>
          <span>Project Atlas</span>
        </a>
        <span className="status-badge">M1.2 · Clarify</span>
      </header>

      <section className="hero" aria-labelledby="hero-title">
        <div className="hero-intro">
          <p className="eyebrow">CURIOSITY, BEFORE THE SEARCH</p>
          <h1 id="hero-title">아직 질문이 아니어도 괜찮아요.</h1>
          <p className="hero-copy">
            생각나는 단어와 경험을 그대로 꺼내놓으세요. 먼저 원문을 안전하게
            보관하고, 해석은 당신의 확인을 거쳐 나중에 시작합니다.
          </p>
        </div>

        {capturedInquiry ? (
          <section
            className="capture-card capture-success"
            aria-labelledby="capture-success-title"
            role="status"
          >
            <div className="capture-card-heading">
              <span className="signal-dot" aria-hidden="true" />
              <p>CAPTURED</p>
            </div>
            <h2 id="capture-success-title">원문을 그대로 보관했습니다.</h2>
            <pre aria-label="저장된 원문">{capturedInquiry.rawText}</pre>
            <p className="success-note">
              이 원문은 질문 제안과 구분해 계속 보관합니다.
            </p>

            {clarificationTurn ? (
              <section
                className="clarification-result"
                aria-labelledby="clarification-question-title"
              >
                <p className="clarification-label">첫 명확화 질문</p>
                <h3 id="clarification-question-title">
                  {clarificationTurn.question}
                </h3>
                <div
                  className="clarification-reason"
                  aria-labelledby="clarification-reason-title"
                >
                  <h4 id="clarification-reason-title">왜 이 답이 중요한가요?</h4>
                  <p>{clarificationTurn.reason}</p>
                </div>
                <p className="slice-boundary">
                  이 조각에서는 질문을 보여주는 데까지만 진행합니다. 답변과 다음
                  학습 단계는 아직 시작하지 않습니다.
                </p>
              </section>
            ) : (
              <section
                className="clarification-start"
                aria-labelledby="clarification-start-title"
              >
                <h3 id="clarification-start-title">
                  방향을 좁힐 질문 하나를 확인할까요?
                </h3>
                <p>
                  답에 따라 학습 방향이나 확인할 증거가 달라지는 질문 하나만
                  제안합니다. 시작은 자동으로 진행되지 않습니다.
                </p>

                {clarificationError && (
                  <p role="alert" className="form-error clarification-error">
                    {clarificationError.message}
                  </p>
                )}

                {!clarificationError?.terminal && (
                  <button
                    className="submit-button clarification-button"
                    type="button"
                    disabled={isStartingClarification}
                    onClick={handleStartClarification}
                  >
                    {isStartingClarification
                      ? '질문을 준비하는 중…'
                      : clarificationError
                        ? '같은 요청으로 다시 시도하기'
                        : '명확화 질문 하나 받기'}
                  </button>
                )}
              </section>
            )}
          </section>
        ) : (
          <section className="capture-card" aria-labelledby="capture-title">
            <div className="capture-card-heading">
              <span className="signal-dot" aria-hidden="true" />
              <p>정리되지 않은 생각도 충분한 시작입니다</p>
            </div>
            <h2 id="capture-title">지금 궁금한 것을 있는 그대로 남겨보세요.</h2>

            <div className="starter-prompts" aria-labelledby="starter-title">
              <p id="starter-title">막막하다면 시작 문구를 골라도 좋아요.</p>
              <div className="starter-grid">
                {startingPoints.map((startingPoint) => (
                  <button
                    key={startingPoint}
                    type="button"
                    disabled={isSubmitting}
                    onClick={() => addStartingPoint(startingPoint)}
                  >
                    {startingPoint}
                  </button>
                ))}
              </div>
            </div>

            <form onSubmit={handleSubmit} noValidate>
              <label htmlFor="brain-dump">정리되지 않은 생각</label>
              <textarea
                id="brain-dump"
                value={rawText}
                disabled={isSubmitting}
                aria-describedby={
                  error
                    ? 'brain-dump-help brain-dump-count brain-dump-error'
                    : 'brain-dump-help brain-dump-count'
                }
                aria-invalid={error?.invalidInput ?? false}
                onChange={(event) => updateRawText(event.target.value)}
                placeholder="예: Kafka는 궁금한데, retry나 중복 같은 말을 들어도 내가 뭘 모르는지 모르겠어요."
                rows={9}
              />
              <div className="input-meta">
                <p id="brain-dump-help">공백과 줄바꿈을 포함한 원문 그대로 저장합니다.</p>
                <p
                  id="brain-dump-count"
                  className={
                    codePointCount(rawText) > MAX_CODE_POINTS
                      ? 'count count-over'
                      : 'count'
                  }
                >
                  {codePointCount(rawText).toLocaleString()} / 10,000
                </p>
              </div>

              {error && (
                <p id="brain-dump-error" role="alert" className="form-error">
                  {error.message}
                </p>
              )}

              <button className="submit-button" type="submit" disabled={isSubmitting}>
                {isSubmitting ? '원문을 보관하는 중…' : '이 생각부터 보관하기'}
              </button>
            </form>
          </section>
        )}
      </section>

      <footer className="principles" aria-label="Project Atlas 제품 원칙">
        <p>원문은 보존합니다.</p>
        <p>AI의 해석은 제안일 뿐입니다.</p>
        <p>전체 계획보다 다음 행동 하나를 먼저 만듭니다.</p>
      </footer>
    </main>
  )
}

export default App
