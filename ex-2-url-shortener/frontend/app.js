const API_BASE = 'http://localhost:8080';

const longUrlInput = document.getElementById('longUrl');
const shortenBtn = document.getElementById('shortenBtn');
const errorDiv = document.getElementById('error');
const resultDiv = document.getElementById('result');
const shortUrlInput = document.getElementById('shortUrl');
const copyBtn = document.getElementById('copyBtn');

shortenBtn.addEventListener('click', shortenUrl);
longUrlInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') shortenUrl();
});

copyBtn.addEventListener('click', copyToClipboard);

async function shortenUrl() {
  const longUrl = longUrlInput.value.trim();
  if (!longUrl) {
    showError('Please enter a URL');
    return;
  }

  setLoading(true);
  hideError();
  hideResult();

  try {
    const res = await fetch(`${API_BASE}/shorten`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ longUrl })
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || 'Failed to shorten URL');
    }

    const data = await res.json();
    shortUrlInput.value = data.shortUrl;
    showResult();
  } catch (err) {
    showError(err.message);
  } finally {
    setLoading(false);
  }
}

function copyToClipboard() {
  shortUrlInput.select();
  navigator.clipboard.writeText(shortUrlInput.value).then(() => {
    copyBtn.textContent = 'Copied!';
    copyBtn.classList.add('copied');
    setTimeout(() => {
      copyBtn.textContent = 'Copy';
      copyBtn.classList.remove('copied');
    }, 2000);
  });
}

function setLoading(loading) {
  shortenBtn.disabled = loading;
  shortenBtn.textContent = loading ? 'Shortening...' : 'Shorten';
}

function showError(msg) {
  errorDiv.textContent = msg;
  errorDiv.classList.remove('hidden');
}

function hideError() {
  errorDiv.classList.add('hidden');
}

function showResult() {
  resultDiv.classList.remove('hidden');
}

function hideResult() {
  resultDiv.classList.add('hidden');
}
