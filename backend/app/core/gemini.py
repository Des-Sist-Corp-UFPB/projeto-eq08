"""
Serviço de integração com a API Gemini do Google.

Responsabilidade única: receber um contexto estruturado do negócio (dados reais
extraídos do banco) e uma pergunta do usuário, montar o prompt e retornar a
resposta do modelo.

O serviço opera em modo "business-scoped": o system prompt instrui o modelo a
responder exclusivamente com base nos dados fornecidos no contexto, sem
inventar informações ou extrapolar para assuntos externos ao negócio.
"""

import logging
from typing import Optional

from google import genai
from google.genai import types

from app.core.config import settings

logger = logging.getLogger(__name__)

# System prompt base — define o "persona" e restringe o escopo
_SYSTEM_PROMPT = """Você é o Copiloto de Negócios do sistema Gestor SaaS.
Seu papel é responder perguntas do gestor com base EXCLUSIVAMENTE nos dados
reais da empresa fornecidos abaixo. Não invente números, não extrapole para
assuntos externos, não dê opiniões genéricas de mercado.

Se a pergunta estiver fora do escopo dos dados disponíveis, informe educadamente
que não tem essa informação no sistema e sugira onde o usuário pode encontrá-la.

Responda sempre em Português do Brasil, de forma direta e profissional.
Use markdown simples (negrito, listas) para estruturar a resposta quando útil.
Não use emojis excessivos.

=== CONTEXTO DO NEGÓCIO (dados em tempo real) ===
{context}
=== FIM DO CONTEXTO ===
"""

# Modelo padrão — gemini-2.0-flash é rápido e tem contexto largo suficiente
_DEFAULT_MODEL = "gemini-2.0-flash"


def build_business_context(
    revenue: float,
    orders_count: int,
    avg_ticket: float,
    critical_insumos: list[dict],
    suppliers_count: int,
    today_schedules: list[dict],
    tenant_name: str,
) -> str:
    """
    Monta a string de contexto estruturado que será injetada no prompt.
    Cada seção corresponde a um módulo do sistema.
    """
    lines = [
        f"Empresa: {tenant_name}",
        "",
        "# Financeiro / Vendas",
        f"- Total de pedidos: {orders_count}",
        f"- Faturamento bruto: R$ {revenue:,.2f}",
        f"- Ticket médio: R$ {avg_ticket:,.2f}",
        "",
        "# Estoque Crítico (abaixo do mínimo de segurança)",
    ]

    if critical_insumos:
        for item in critical_insumos:
            lines.append(
                f"  - {item['name']}: estoque atual={item['current_stock']:.1f} "
                f"{item['unit']} | mínimo={item['minimum_stock']:.1f} {item['unit']}"
            )
    else:
        lines.append("  - Nenhum insumo em situação crítica.")

    lines += [
        "",
        f"# Fornecedores cadastrados: {suppliers_count}",
        "",
        "# Escalas de hoje",
    ]

    if today_schedules:
        for s in today_schedules:
            lines.append(
                f"  - {s['employee_name']} ({s['role']}): "
                f"{s['start_time']} até {s['end_time']}"
            )
    else:
        lines.append("  - Nenhum colaborador escalado para hoje.")

    return "\n".join(lines)


async def ask_gemini(question: str, business_context: str) -> Optional[str]:
    """
    Chama a API Gemini com o contexto do negócio e a pergunta do usuário.

    Retorna a resposta em texto ou None se a chave não estiver configurada
    (nesse caso o caller deve usar o fallback heurístico).
    """
    api_key: Optional[str] = getattr(settings, "GEMINI_API_KEY", None)
    if not api_key:
        logger.warning("GEMINI_API_KEY não configurada — copilot usará modo heurístico.")
        return None

    try:
        client = genai.Client(api_key=api_key)
        system_with_context = _SYSTEM_PROMPT.format(context=business_context)

        response = client.models.generate_content(
            model=_DEFAULT_MODEL,
            contents=question,
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
