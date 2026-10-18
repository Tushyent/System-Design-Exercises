/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        vidya: {
          navy: '#0C2340',
          blue: '#1460A3',
          'blue-hover': '#0E4B82',
          'blue-interactive': '#0275B8',
          'blue-deep': '#092B4C',
          'blue-light': '#E8F2FA',
          'blue-surface': '#F0F6FB',
          'text-main': '#0F172A',
          'text-secondary': '#334155',
          'text-muted': '#475569',
          border: '#CBD5E1',
          divider: '#E2E8F0',
          bg: '#F8FAFC',
          surface: '#FFFFFF',
          success: '#15803D',
          'success-bg': '#DCFCE7',
          warning: '#B45309',
          'warning-bg': '#FEF3C7',
          error: '#BE123C',
          'error-bg': '#FFE4E6',
          info: '#1460A3',
          'info-bg': '#E8F2FA',
        },
        brand: {
          50: '#EAF4FC',
          100: '#D4E9F9',
          200: '#A9D2F3',
          300: '#7EBBED',
          400: '#53A4E7',
          500: '#1769AA',
          600: '#12558B',
          700: '#0E426C',
          800: '#102A43',
          900: '#0A1C2E',
        }
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'Inter', '-apple-system', 'sans-serif'],
      },
      borderRadius: {
        'control-sm': '8px',
        'control': '10px',
        'card': '16px',
        'dialog': '20px',
      },
      boxShadow: {
        'subtle': '0 1px 3px 0 rgba(16, 42, 67, 0.04), 0 1px 2px -1px rgba(16, 42, 67, 0.04)',
        'card': '0 2px 6px -1px rgba(16, 42, 67, 0.06), 0 1px 4px -2px rgba(16, 42, 67, 0.04)',
        'card-hover': '0 8px 20px -3px rgba(16, 42, 67, 0.08), 0 4px 6px -4px rgba(16, 42, 67, 0.03)',
        'elevated': '0 12px 32px -4px rgba(16, 42, 67, 0.12), 0 4px 12px -2px rgba(16, 42, 67, 0.06)',
      }
    },
  },
  plugins: [],
}
