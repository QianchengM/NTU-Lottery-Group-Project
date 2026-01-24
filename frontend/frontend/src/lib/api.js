// A tiny fetch wrapper for the backend's ApiResponse<T>

async function request(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });

  let body;
  try {
    body = await res.json();
  } catch {
    throw new Error(`Invalid JSON response (${res.status})`);
  }

  if (!res.ok) {
    // backend typically still returns ApiResponse on errors, but be defensive
    throw new Error(body?.message || `HTTP ${res.status}`);
  }

  if (typeof body?.code !== 'number') {
    throw new Error('Unexpected response format');
  }
  if (body.code !== 0) {
    throw new Error(body.message || `Error code: ${body.code}`);
  }
  return body.data;
}

export const api = {
  get: (url) => request(url),
  post: (url, data) => request(url, { method: 'POST', body: JSON.stringify(data ?? {}) }),
};
