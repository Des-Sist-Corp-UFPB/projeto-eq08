/**
 * Utilitário de tracking Umami — dispara eventos customizados
 * para atender o critério de observabilidade "Umami (eventos)".
 *
 * O script Umami carrega via <script defer> no index.html e expõe
 * `window.umami.track()`. Se o script ainda não carregou ou está
 * ausente (dev local), as chamadas são silenciosamente ignoradas.
 */

// Tipagem global para o Umami
declare global {
  interface Window {
    umami?: {
      track: (event: string, data?: Record<string, string | number | boolean>) => void;
    };
  }
}

/**
 * Dispara um evento customizado no Umami.
 * Falha silenciosamente se o Umami não estiver carregado.
 */
export function trackEvent(
  event: string,
  data?: Record<string, string | number | boolean>
): void {
  try {
    window.umami?.track(event, data);
  } catch {
    // Silencia erros — tracking nunca deve quebrar a UX
  }
}
