"""
Serviço de integração com a API Gemini do Google (BIaaS Engine).

Responsabilidade única: receber um contexto estruturado do negócio (construído
pela SectorStrategy correta via SectorRegistry) e uma pergunta do usuário,
montar o prompt e retornar a resposta do modelo.

O serviço opera em modo "business-scoped": o system prompt instrui o modelo a
responder exclusivamente com base nos dados fornecidos no contexto, sem
inventar informações ou extrapolar para assuntos externos ao negócio.

A construção do contexto (build_bi_context) é responsabilidade de cada
SectorStrategy — ver app/core/sectors/. Este módulo é agnóstico de setor.
"""

import logging
from typing import Optional, List, Any

from google import genai
from google.genai import types

from app.core.config import settings

logger = logging.getLogger(__name__)

# System prompt base — define o "persona" e restringe o escopo
# O placeholder {context} é preenchido pelo SectorStrategy ativo do tenant.
_SYSTEM_PROMPT = """Você é o Copiloto de Negócios do sistema Gestor SaaS.
Seu papel é responder perguntas do gestor com base EXCLUSIVAMENTE nos dados
reais da empresa fornecidos abaixo. Não invente números, não extrapole para
assuntos externos, não dê opiniões genéricas de mercado.

Se a pergunta estiver fora do escopo dos dados disponíveis, informe educadamente
que não tem essa informação no sistema e sugira onde o usuário pode encontrá-la.

Responda sempre em Português do Brasil, de forma direta e profissional.
Use markdown simples (negrito, listas) para estruturar a resposta quando útil.
Não use emojis excessivos.

=== CONTEXTO DO NEGÓCIO (dados em tempo real, gerados pela Strategy do setor) ===
{context}
=== FIM DO CONTEXTO ===
"""

# Modelo padrão — alterado para gemini-1.5-flash pois a cota gratuita do 2.0-flash se esgota rapidamente
_DEFAULT_MODEL = "gemini-1.5-flash"


async def ask_gemini(question: str, business_context: str, history: Optional[List[Any]] = None) -> Optional[str]:
    """
    Chama a API Gemini com o contexto do negócio e a pergunta do usuário.

    O `business_context` é construído externamente pela SectorStrategy adequada
    (via SectorRegistry.get(tenant.sector_type).build_bi_context(...)).

    Retorna a resposta em texto ou None se a chave não estiver configurada
    (nesse caso o caller deve usar o fallback heurístico por intenção).
    """
    api_key: Optional[str] = getattr(settings, "GEMINI_API_KEY", None)
    if not api_key:
        logger.warning("GEMINI_API_KEY não configurada — copilot usará modo heurístico.")
        return None

    try:
        client = genai.Client(api_key=api_key)
        system_with_context = _SYSTEM_PROMPT.format(context=business_context)

        # Constrói o histórico da conversa no formato do Google GenAI
        contents = []
        if history:
            for msg in history:
                # msg.role is 'user' or 'model'
                contents.append(types.Content(role=msg.role, parts=[types.Part.from_text(text=msg.content)]))
                
        # Adiciona a pergunta atual por último
        contents.append(types.Content(role="user", parts=[types.Part.from_text(text=question)]))

        response = client.models.generate_content(
            model=_DEFAULT_MODEL,
            contents=contents,
            config=types.GenerateContentConfig(
                system_instruction=system_with_context,
                temperature=0.2,        # baixo para respostas factuais
                max_output_tokens=1024,
            ),
        )
        return response.text

    except Exception as exc:
        logger.error("Erro ao chamar Gemini API: %s", exc)
        return None

