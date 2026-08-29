import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('states that an unstructured curiosity is a valid starting point', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', {
        name: '아직 질문이 아니어도 괜찮아요.',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('원문은 보존합니다.')).toBeInTheDocument()
    expect(
      screen.getByText('AI의 해석은 제안일 뿐입니다.'),
    ).toBeInTheDocument()
  })
})
