# Relatório de Avaliação — EQ08 (DSC)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/des-sist-corp-ufpb/projeto-eq08 |
| **Aplicação** | https://eq08.dsc.rodrigor.com |
| **Período de atividade** | 2026-06-10 → 2026-06-25 |
| **Total de commits** (sem merges, branch main) | 3 |
| **Integrantes** | Marcos Vinicius Satro Avelino (@smurlocky), Joao Pedro Martins Pernambuco (@jpsenec) |

---

## 1. Tecnologias

- Javalin
- Thymeleaf
- Flyway (5 migrations)
- JWT
- MinIO/S3

---

## 2. Análise Funcional

### Endpoints REST (52 mapeados)

| Método | Path | Arquivo |
|--------|------|---------|
| `DELETE` | `/categorias/{id}` | `Main.java` |
| `DELETE` | `/escalas/{id}` | `Main.java` |
| `DELETE` | `/fornecedores/{id}` | `Main.java` |
| `DELETE` | `/insumos/{id}` | `Main.java` |
| `DELETE` | `/produtos/{id}` | `Main.java` |
| `DELETE` | `/usuarios/{id}` | `Main.java` |
| `GET` | `/` | `Main.java` |
| `GET` | `/analytics` | `Main.java` |
| `GET` | `/auditoria` | `Main.java` |
| `GET` | `/categorias` | `Main.java` |
| `GET` | `/compras` | `Main.java` |
| `GET` | `/dashboard` | `Main.java` |
| `GET` | `/escalas` | `Main.java` |
| `GET` | `/escalas/afastamentos` | `Main.java` |
| `GET` | `/escalas/trocas` | `Main.java` |
| `GET` | `/fornecedores` | `Main.java` |
| `GET` | `/fornecedores/{id}/performance` | `Main.java` |
| `GET` | `/insumos` | `Main.java` |
| `GET` | `/insumos/movimentacoes` | `Main.java` |
| `GET` | `/login` | `Main.java` |
| `GET` | `/logout` | `Main.java` |
| `GET` | `/pdv` | `Main.java` |
| `GET` | `/ping` | `Main.java` |
| `GET` | `/produtos` | `Main.java` |
| `GET` | `/register` | `Main.java` |
| `GET` | `/usuarios` | `Main.java` |
| `GET` | `/vendas` | `Main.java` |
| `POST` | `/analytics/gerar-previsoes` | `Main.java` |
| `POST` | `/analytics/recomendacoes/{id}/aplicar` | `Main.java` |
| `POST` | `/analytics/recomendacoes/{id}/dispensar` | `Main.java` |
| `POST` | `/categorias` | `Main.java` |
| `POST` | `/compras` | `Main.java` |
| `POST` | `/compras/{id}/cancelar` | `Main.java` |
| `POST` | `/compras/{id}/concluir` | `Main.java` |
| `POST` | `/escalas` | `Main.java` |
| `POST` | `/escalas/afastamentos` | `Main.java` |
| `POST` | `/escalas/afastamentos/{id}/aprovar` | `Main.java` |
| `POST` | `/escalas/trocas` | `Main.java` |
| `POST` | `/escalas/trocas/{id}/aprovar` | `Main.java` |
| `POST` | `/escalas/trocas/{id}/rejeitar` | `Main.java` |
| `POST` | `/fornecedores` | `Main.java` |
| `POST` | `/insumos` | `Main.java` |
| `POST` | `/insumos/{id}/movimentacao` | `Main.java` |
| `POST` | `/login` | `Main.java` |
| `POST` | `/produtos` | `Main.java` |
| `POST` | `/register` | `Main.java` |
| `POST` | `/usuarios` | `Main.java` |
| `POST` | `/vendas` | `Main.java` |
| `PUT` | `/fornecedores/{id}` | `Main.java` |
| `PUT` | `/insumos/{id}` | `Main.java` |
| `PUT` | `/produtos/{id}` | `Main.java` |
| `PUT` | `/usuarios/{id}` | `Main.java` |

### Entidades / Tabelas (19 encontradas)

- `employee_schedules (via V4__create_schedule_tables.sql)`
- `shift_trades (via V4__create_schedule_tables.sql)`
- `absences (via V4__create_schedule_tables.sql)`
- `categories (via V2__create_stock_tables.sql)`
- `insumos (via V2__create_stock_tables.sql)`
- `products (via V2__create_stock_tables.sql)`
- `product_ingredients (via V2__create_stock_tables.sql)`
- `orders (via V2__create_stock_tables.sql)`
- `order_items (via V2__create_stock_tables.sql)`
- `stock_movements (via V2__create_stock_tables.sql)`
- `suppliers (via V3__create_purchase_tables.sql)`
- `purchase_orders (via V3__create_purchase_tables.sql)`
- `purchase_items (via V3__create_purchase_tables.sql)`
- `tenants (via V1__create_base_tables.sql)`
- `users (via V1__create_base_tables.sql)`
- `refresh_tokens (via V1__create_base_tables.sql)`
- `audit_logs (via V1__create_base_tables.sql)`
- `demand_forecasts (via V5__create_ai_tables.sql)`
- `ai_recommendations (via V5__create_ai_tables.sql)`

### Migrations (5 arquivos)

- `V1__create_base_tables.sql`
- `V2__create_stock_tables.sql`
- `V3__create_purchase_tables.sql`
- `V4__create_schedule_tables.sql`
- `V5__create_ai_tables.sql`

---

## 3. Análise Arquitetural

| Aspecto | Status | Observação |
|---------|--------|-----------|
| Arquitetura em camadas | ✅ | controller=✅  service=✅  repository=✅ |
| Testes automatizados | ❌ | não encontrados |
| Migrations versionadas | ✅ | 5 migration(s) |
| Logging | ✅ | @Slf4j / LoggerFactory / logging.getLogger detectado |
| Autenticação / Segurança | ❌ | não detectado |
| DTOs / Separação de dados | ❌ | não detectado |
| Tratamento global de exceções | ❌ | não detectado |
| Documentação de API (OpenAPI) | ❌ | não detectado |
| Variáveis de ambiente | ❌ | não detectado |
| Dockerfile / docker-compose | ✅ | presente |

---

## 4. Contribuição por Usuário

### Resumo

| Usuário | Commits (main) | Commits (GitHub API) | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------------|---------------------|-------------------|----------------------|----------------|
| Marcos Vinicius Satro Avelino (@smurlocky) | 1 | **28** ⚠️ | 9.031 | 7.798 | 100% |
| Joao Pedro Martins Pernambuco (@jpsenec) | 0 | **4** | 0 | 0 | 0% |
| *(sem login GitHub)* | 2 | 67% | — | — | — |

> **⚠️ Divergência entre commits locais e GitHub API:**
> - **@smurlocky**: 1 commit(s) na branch `main` vs **28** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
>

### Contribuição por Camada

| Camada | Total linhas | Marcos Vinicius Satro Avelino (@smurlocky) | Joao Pedro Martins Pernambuco (@jpsenec) |
|--------|-------------|---------|---------|
| Controller | 3.738 | 100% | 0% |
| Repository | 1.797 | 100% | 0% |
| Service | 788 | 100% | 0% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Marcos Vinicius Satro Avelino (@smurlocky) | Joao Pedro Martins Pernambuco (@jpsenec) |
|---------|-------------|---------|---------|
| `dashboard.html` | 252 | 100% | 0% |
| `app.css` | 244 | 100% | 0% |
| `app.js` | 184 | 100% | 0% |
| `AiService.java` | 154 | 100% | 0% |
| `InsumoService.java` | 128 | 100% | 0% |
| `V2__create_stock_tables.sql` | 125 | 100% | 0% |
| `afastamentos.html` | 123 | 100% | 0% |
| `register.html` | 119 | 100% | 0% |
| `escalas.html` | 117 | 100% | 0% |
| `analytics.html` | 110 | 100% | 0% |
| `insumos.html` | 109 | 100% | 0% |
| `usuarios.html` | 109 | 100% | 0% |
| `layout.html` | 107 | 100% | 0% |
| `produtos.html` | 106 | 100% | 0% |
| `AuthService.java` | 104 | 100% | 0% |
| `ScheduleController.java` | 103 | 100% | 0% |
| `AuthController.java` | 99 | 100% | 0% |
| `login.html` | 97 | 100% | 0% |
| `compras.html` | 93 | 100% | 0% |
| `categorias.html` | 92 | 100% | 0% |
| `purchases-table.html` | 88 | 100% | 0% |
| `UserService.java` | 86 | 100% | 0% |
| `pdv.html` | 86 | 100% | 0% |
| `V1__create_base_tables.sql` | 83 | 100% | 0% |
| `InsumoController.java` | 80 | 100% | 0% |
| `PurchaseController.java` | 78 | 100% | 0% |
| `insumos-table.html` | 77 | 100% | 0% |
| `ScheduleService.java` | 72 | 100% | 0% |
| `ProductController.java` | 72 | 100% | 0% |
| `fornecedores.html` | 70 | 100% | 0% |
| `V4__create_schedule_tables.sql` | 68 | 100% | 0% |
| `trocas.html` | 66 | 100% | 0% |
| `navbar.html` | 66 | 100% | 0% |
| `OrderController.java` | 62 | 100% | 0% |
| `UserController.java` | 60 | 100% | 0% |
| `V3__create_purchase_tables.sql` | 60 | 100% | 0% |
| `SupplierController.java` | 57 | 100% | 0% |
| `SupplierService.java` | 56 | 100% | 0% |
| `fornecedor-performance.html` | 56 | 100% | 0% |
| `ProductService.java` | 55 | 100% | 0% |
| `DashboardController.java` | 52 | 100% | 0% |
| `auditoria.html` | 51 | 100% | 0% |
| `PurchaseService.java` | 50 | 100% | 0% |
| `V5__create_ai_tables.sql` | 49 | 100% | 0% |
| `AiController.java` | 45 | 100% | 0% |
| `OrderService.java` | 43 | 100% | 0% |
| `ControllerHelpers.java` | 42 | 100% | 0% |
| `CategoryService.java` | 40 | 100% | 0% |
| `suppliers-table.html` | 40 | 100% | 0% |
| `CategoryController.java` | 38 | 100% | 0% |
| `products-table.html` | 36 | 100% | 0% |
| `error.html` | 27 | 100% | 0% |
| `AuditController.java` | 21 | 100% | 0% |
| `movements-table.html` | 19 | 100% | 0% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*