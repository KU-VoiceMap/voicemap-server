import { useEffect, useRef } from 'react';
import { setPendingGoogleIdToken } from './tokenStorage';

const GOOGLE_CLIENT_ID = '449433791049-276ln0ta9r768dddnjftjq1qleerq231.apps.googleusercontent.com';

interface AuthViewProps {
  statusMessage: string;
}

declare global {
  var google: {
    accounts: {
      id: {
        initialize: (config: { client_id: string; callback: (response: { credential?: string }) => void }) => void;
        renderButton: (element: HTMLElement, config: Record<string, unknown>) => void;
      };
    };
  } | undefined;
}

export default function AuthView({ statusMessage }: AuthViewProps) {
  const buttonRef = useRef<HTMLDivElement>(null);
  const scriptLoaded = useRef(false);

  useEffect(() => {
    if (!document.querySelector('script[src*="accounts.google.com/gsi/client"]')) {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }

    const tryRender = () => {
      if (!globalThis.google?.accounts?.id || !buttonRef.current) {
        setTimeout(tryRender, 300);
        return;
      }
      if (scriptLoaded.current) return;
      scriptLoaded.current = true;

      globalThis.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: (response) => {
          if (!response.credential) return;
          setPendingGoogleIdToken(response.credential);
          globalThis.location.assign('/oauth/callback');
        },
      });

      globalThis.google.accounts.id.renderButton(buttonRef.current, {
        theme: 'outline',
        size: 'large',
        shape: 'pill',
        text: 'signin_with',
        width: 300,
      });
    };

    tryRender();
  }, []);

  return (
    <section className="auth-view">
      <div className="auth-card">
        <h1>VoiceMap</h1>
        <div ref={buttonRef} className="google-signin-slot" />
        {statusMessage && <p className="auth-status">{statusMessage}</p>}
      </div>
    </section>
  );
}
