import './App.css'

const startingPoints = [
  '자주 보이지만 설명하지 못하는 단어',
  '언젠가 만들고 싶은 것',
  '장애가 나면 두려울 것 같은 부분',
  '안다고 생각했지만 막상 설명하기 어려운 개념',
]

function App() {
  return (
    <main className="page-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Project Atlas 홈">
          <span className="brand-mark" aria-hidden="true">
            A
          </span>
          <span>Project Atlas</span>
        </a>
        <span className="status-badge">Product foundation</span>
      </header>

      <section className="hero" aria-labelledby="hero-title">
        <p className="eyebrow">CURIOSITY, BEFORE THE SEARCH</p>
        <h1 id="hero-title">아직 질문이 아니어도 괜찮아요.</h1>
        <p className="hero-copy">
          생각나는 단어와 경험을 그대로 꺼내놓으세요. Atlas는 정답부터
          말하지 않고, 당신이 정말 알고 싶은 것을 함께 질문으로 만듭니다.
        </p>

        <div className="thought-card" aria-label="정리되지 않은 생각의 예시">
          <div className="thought-card-header">
            <span className="signal-dot" aria-hidden="true" />
            <span>정리되지 않은 생각도 충분한 시작입니다</span>
          </div>
          <blockquote>
            “Kafka도 궁금하고 대규모 시스템도 배우고 싶은데, 뭘 모르는지
            몰라서 어디서부터 찾아야 할지 막막해요.”
          </blockquote>
          <div className="flow-preview" aria-label="Atlas의 학습 흐름">
            <span>생각</span>
            <span aria-hidden="true">→</span>
            <span>질문</span>
            <span aria-hidden="true">→</span>
            <span>작은 행동</span>
            <span aria-hidden="true">→</span>
            <span>증거</span>
          </div>
        </div>
      </section>

      <section className="starting-section" aria-labelledby="starting-title">
        <div>
          <p className="section-kicker">어디서 시작할지 모르겠다면</p>
          <h2 id="starting-title">이 중 하나만 떠올려도 충분합니다.</h2>
        </div>
        <ul className="starting-grid">
          {startingPoints.map((startingPoint, index) => (
            <li key={startingPoint}>
              <span>{String(index + 1).padStart(2, '0')}</span>
              <p>{startingPoint}</p>
            </li>
          ))}
        </ul>
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
