import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as profileApi from './api'
import { ProfilePage } from './ProfilePage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/profile']}>
        <Routes>
          <Route path="/profile" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads profile data and saves updates', async () => {
    const getSpy = vi.spyOn(profileApi, 'getProfile').mockResolvedValue({
      userId: 'u-1',
      preferredLanguage: 'English',
      responseStyle: 'Concise and practical',
      technicalBackground: 'Java and Spring Boot',
      careerGoal: 'Senior backend engineer',
      customInstructions: 'Use examples',
    })
    const updateSpy = vi.spyOn(profileApi, 'updateProfile').mockResolvedValue({
      userId: 'u-1',
      preferredLanguage: 'Spanish',
      responseStyle: 'Warm and direct',
      technicalBackground: 'Java and Spring Boot',
      careerGoal: 'Staff engineer',
      customInstructions: 'Use examples',
    })

    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(screen.getByDisplayValue('English')).toBeInTheDocument())
    expect(getSpy).toHaveBeenCalled()

    const languageField = screen.getByLabelText(/preferred language/i)
    await user.clear(languageField)
    await user.type(languageField, 'Spanish')
    await user.click(screen.getByRole('button', { name: /save profile/i }))

    await waitFor(() => expect(updateSpy).toHaveBeenCalledWith({
      preferredLanguage: 'Spanish',
      responseStyle: 'Concise and practical',
      technicalBackground: 'Java and Spring Boot',
      careerGoal: 'Senior backend engineer',
      customInstructions: 'Use examples',
    }))
  })
})
