export function login(data: { username: string; password: string }): Promise<{ code: number; data: { token: string } }>
