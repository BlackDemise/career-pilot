import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PageMessage } from '../../shared/components/PageMessage'
import { getProfile, updateProfile, type ProfileDraft } from './api'

const emptyProfile: ProfileDraft = {
  preferredLanguage: '',
  responseStyle: '',
  technicalBackground: '',
  careerGoal: '',
  customInstructions: '',
}

function toDraft(profile?: { preferredLanguage: string | null; responseStyle: string | null; technicalBackground: string | null; careerGoal: string | null; customInstructions: string | null } | null): ProfileDraft {
  return {
    ...emptyProfile,
    preferredLanguage: profile?.preferredLanguage ?? '',
    responseStyle: profile?.responseStyle ?? '',
    technicalBackground: profile?.technicalBackground ?? '',
    careerGoal: profile?.careerGoal ?? '',
    customInstructions: profile?.customInstructions ?? '',
  }
}

function ProfileForm({ profile }: { profile?: { preferredLanguage: string | null; responseStyle: string | null; technicalBackground: string | null; careerGoal: string | null; customInstructions: string | null } | null }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<ProfileDraft>(() => toDraft(profile))
  const [notice, setNotice] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['profile'] })
      setNotice('Profile updated successfully.')
      setErrorMessage(null)
    },
    onError: (reason) => {
      setErrorMessage(reason instanceof Error ? reason.message : 'Could not save your profile.')
      setNotice(null)
    },
  })

  function handleChange(field: keyof ProfileDraft, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await mutation.mutateAsync(form)
  }

  return <div className="profile-panel"><h2>Profile</h2><p className="lede">Shape how CareerPilot responds in future conversations.</p>{notice && <PageMessage tone="success">{notice}</PageMessage>}{errorMessage && <PageMessage tone="error">{errorMessage}</PageMessage>}<form onSubmit={handleSubmit}><div className="field-grid"><div className="field-group"><label htmlFor="preferred-language">Preferred language</label><input id="preferred-language" value={form.preferredLanguage} onChange={(event) => handleChange('preferredLanguage', event.target.value)} /></div><div className="field-group"><label htmlFor="response-style">Response style</label><input id="response-style" value={form.responseStyle} onChange={(event) => handleChange('responseStyle', event.target.value)} /></div></div><div className="field-group"><label htmlFor="technical-background">Technical background</label><textarea id="technical-background" className="form-textarea" value={form.technicalBackground} onChange={(event) => handleChange('technicalBackground', event.target.value)} /></div><div className="field-group"><label htmlFor="career-goal">Career goal</label><textarea id="career-goal" className="form-textarea" value={form.careerGoal} onChange={(event) => handleChange('careerGoal', event.target.value)} /></div><div className="field-group"><label htmlFor="custom-instructions">Custom instructions</label><textarea id="custom-instructions" className="form-textarea" value={form.customInstructions} onChange={(event) => handleChange('customInstructions', event.target.value)} /></div><div className="form-actions"><button type="submit" className="primary-button" disabled={mutation.isPending}>Save profile</button></div></form></div>
}

export function ProfilePage() {
  const { data: profile, isLoading, isError, error } = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
  })

  if (isLoading) return <section className="workspace"><div className="profile-panel"><p className="empty-state">Loading profile...</p></div></section>
  if (isError) return <section className="workspace"><div className="profile-panel"><PageMessage tone="error">{error instanceof Error ? error.message : 'Could not load your profile.'}</PageMessage></div></section>

  return <section className="workspace"><ProfileForm key={profile?.userId ?? 'profile'} profile={profile} /></section>
}
