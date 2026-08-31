import apiClient, { unwrap } from '../../shared/api/axiosClient'
import type { ApiResponse } from '../../shared/api/types'

export type Profile = {
  userId: string
  preferredLanguage: string | null
  responseStyle: string | null
  technicalBackground: string | null
  careerGoal: string | null
  customInstructions: string | null
}

export type ProfileDraft = {
  preferredLanguage: string
  responseStyle: string
  technicalBackground: string
  careerGoal: string
  customInstructions: string
}

export async function getProfile(): Promise<Profile> {
  return unwrap<Profile>(apiClient.get<ApiResponse<Profile>>('/users/me/profile'))
}

export async function updateProfile(payload: ProfileDraft): Promise<Profile> {
  return unwrap<Profile>(apiClient.put<ApiResponse<Profile>>('/users/me/profile', payload))
}
