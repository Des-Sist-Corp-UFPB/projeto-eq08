import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  Shield, Building2, User, Mail, Lock, ArrowRight, Loader2,
  AlertCircle, Eye, EyeOff, UtensilsCrossed, Shirt, LayoutGrid, Check
} from 'lucide-react';

/* ─── Sector cards config ──────────────────────────── */
interface SectorOption {
  value: 'food_service' | 'retail_apparel' | 'generic';
  label: string;
  description: string;
  icon: React.ReactNode;
  accent: string;
  accentBg: string;
  accentBorder: string;
  placeholder: string;
}

const SECTOR_OPTIONS: SectorOption[] = [
  {
    value: 'food_service',
    label: 'Restaurante / Pizzaria',
    description: 'Insumos, Receitas, Estoque e CMV integrados',
    icon: <UtensilsCrossed className="w-6 h-6" />,
    accent: '#f59e0b',
    accentBg: 'rgba(245,158,11,0.1)',
    accentBorder: 'rgba(245,158,11,0.3)',
    placeholder: 'Pizzaria Bella Napoli',
  },
  {
    value: 'retail_apparel',
    label: 'Loja de Roupas',
    description: 'SKUs com variantes de cor, tamanho e grade',
    icon: <Shirt className="w-6 h-6" />,
    accent: '#a78bfa',
    accentBg: 'rgba(167,139,250,0.1)',
    accentBorder: 'rgba(167,139,250,0.3)',
    placeholder: 'Boutique Elegance',
  },
  {
    value: 'generic',
    label: 'Outro Negócio',
    description: 'Módulos universais de vendas e fornecedores',
    icon: <LayoutGrid className="w-6 h-6" />,
    accent: '#94a3b8',
    accentBg: 'rgba(148,163,184,0.08)',
    accentBorder: 'rgba(148,163,184,0.2)',
    placeholder: 'Minha Empresa',
  },
];

/* ─── Component ──────────────────────────────────── */
export const RegisterTenant: React.FC = () => {
  const { registerTenant } = useAuth();
  const navigate = useNavigate();

  const [sectorType, setSectorType] = useState<SectorOption['value']>('food_service');
  const [companyName, setCompanyName] = useState('');
  const [slug, setSlug] = useState('');
  const [adminName, setAdminName] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const [showPassword, setShowPassword] = useState(false);
  const [companyTouched, setCompanyTouched] = useState(false);
  const [slugTouched, setSlugTouched] = useState(false);
  const [emailTouched, setEmailTouched] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);

  const activeSector = SECTOR_OPTIONS.find(s => s.value === sectorType)!;

  // Auto-generate slug from company name
  const handleCompanyNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setCompanyName(value);
    setCompanyTouched(true);
    setSlugTouched(true);
    const generatedSlug = value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .trim()
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');
    setSlug(generatedSlug);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!companyName || !slug || !adminName || !adminEmail || !adminPassword) return;

    setError(null);
    setIsLoading(true);

    try {
      await registerTenant({
        company_name: companyName,
        slug,
        sector_type: sectorType,
        admin_name: adminName,
        admin_email: adminEmail,
        admin_password: adminPassword,
      });
      navigate('/');
    } catch (err: any) {
      const detail = err.response?.data?.detail;
      if (Array.isArray(detail)) {
        const fieldLabel: Record<string, string> = {
          company_name: 'Nome da Empresa',
          slug: 'Identificador Slug',
          sector_type: 'Setor de Negócio',
          admin_name: 'Seu Nome',
          admin_email: 'E-mail',
          admin_password: 'Senha de Acesso',
        };
        const errorMessages = detail.map((d: any) => {
          const field = d.loc[d.loc.length - 1];
          const label = fieldLabel[field] || field;
          let msg = d.msg;
          if (msg.includes('at least 6 characters')) msg = 'deve ter pelo menos 6 caracteres';
          else if (msg.includes('at least 2 characters')) msg = 'deve ter pelo menos 2 caracteres';
          else if (msg.includes('valid email address')) msg = 'deve ser um e-mail válido';
          return `${label}: ${msg}`;
        });
        setError(errorMessages.join(', '));
      } else if (typeof detail === 'string') {
        setError(detail);
      } else {
        setError('Erro ao registrar empresa. Tente outro identificador slug ou e-mail.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center p-4 overflow-hidden">
      {/* Background glow */}
      <div
        className="absolute top-1/4 left-1/4 w-96 h-96 rounded-full filter blur-[100px] animate-pulse-glow pointer-events-none"
        style={{ background: `radial-gradient(circle, ${activeSector.accentBg}, transparent 70%)`, transition: 'background 0.5s ease' }}
      />
      <div
        className="absolute bottom-1/4 right-1/4 w-96 h-96 rounded-full filter blur-[100px] animate-pulse-glow pointer-events-none"
        style={{ background: 'rgba(99,102,241,0.06)', animationDelay: '1.5s' }}
      />

      <div className="w-full max-w-xl z-10 my-8">
        {/* Brand header */}
        <div className="flex flex-col items-center mb-6">
          <div className="p-3 bg-purple-600/10 border border-purple-500/20 rounded-2xl mb-4 shadow-lg shadow-purple-900/10">
            <Shield className="w-8 h-8 text-purple-400 animate-pulse" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white mb-2">
            Gestor<span className="text-purple-400 font-medium">SaaS</span>
          </h1>
          <p className="text-sm text-muted-foreground text-center">
            Abra a conta do seu negócio em segundos
          </p>
        </div>

        {/* Glassmorphic Register Card */}
        <div className="glass-panel p-8 rounded-3xl shadow-2xl relative overflow-hidden">
          <h2 className="text-xl font-semibold text-white mb-2">Cadastre sua Empresa</h2>
          <p className="text-xs text-slate-500 mb-6">Selecione o seu setor e configure as informações da conta</p>

          {error && (
            <div className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-2xl flex items-start gap-3 text-destructive-foreground text-sm">
              <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">

            {/* ── Sector selector ── */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
                Tipo de Negócio
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {SECTOR_OPTIONS.map((s) => {
                  const isActive = sectorType === s.value;
                  return (
                    <button
                      key={s.value}
                      type="button"
                      onClick={() => setSectorType(s.value)}
                      className="relative flex flex-col items-start gap-2 p-4 rounded-2xl text-left transition-all duration-200 hover:scale-[1.02]"
                      style={{
                        background: isActive ? s.accentBg : 'rgba(255,255,255,0.03)',
                        border: `1.5px solid ${isActive ? s.accentBorder : 'rgba(255,255,255,0.07)'}`,
                        boxShadow: isActive ? `0 0 20px ${s.accentBg}` : 'none',
                      }}
                    >
                      {/* Check mark */}
                      {isActive && (
                        <span
                          className="absolute top-2.5 right-2.5 w-4 h-4 rounded-full flex items-center justify-center"
                          style={{ background: s.accent }}
                        >
                          <Check className="w-2.5 h-2.5 text-white" strokeWidth={3} />
                        </span>
                      )}
                      <span style={{ color: isActive ? s.accent : 'rgba(148,163,184,0.6)' }}>
                        {s.icon}
                      </span>
                      <div>
                        <p className={`text-xs font-semibold leading-tight ${isActive ? 'text-white' : 'text-slate-400'}`}>
                          {s.label}
                        </p>
                        <p className="text-[10px] text-slate-600 leading-tight mt-0.5">
                          {s.description}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* ── Company name + slug ── */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                  Nome da Empresa
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-muted-foreground">
                    <Building2 className="w-5 h-5" />
                  </div>
                  <input
                    type="text"
                    required
                    placeholder={activeSector.placeholder}
                    value={companyName}
                    onChange={handleCompanyNameChange}
                    onBlur={() => setCompanyTouched(true)}
                    disabled={isLoading}
                    className={`w-full pl-11 pr-4 py-3 bg-white/5 border rounded-2xl text-white placeholder-muted-foreground focus:outline-none focus:ring-2 transition-all text-sm ${
                      companyTouched && companyName.length < 6
                        ? 'border-rose-500/40 focus:border-rose-500/50 focus:ring-rose-500/10'
                        : companyTouched && companyName.length >= 6
                        ? 'border-emerald-500/30 focus:border-emerald-500/45 focus:ring-emerald-500/10'
                        : 'border-white/5 focus:border-purple-500/30 focus:ring-purple-500/20'
                    }`}
                  />
                </div>
                {companyTouched && companyName.length < 6 && (
                  <p className="text-[11px] text-rose-400/80 mt-1.5 flex items-center gap-1 font-sans">
                    <span>●</span> Mínimo de 6 caracteres
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                  Slug Identificador
                </label>
                <div className="relative">
                  <input
                    type="text"
                    required
                    placeholder="minha-empresa"
                    value={slug}
                    onChange={(e) => { setSlug(e.target.value); setSlugTouched(true); }}
                    onBlur={() => setSlugTouched(true)}
                    disabled={isLoading}
                    className={`w-full px-4 py-3 bg-white/5 border rounded-2xl text-white placeholder-muted-foreground focus:outline-none focus:ring-2 transition-all text-sm font-mono ${
                      slugTouched && slug.length < 2
                        ? 'border-rose-500/40 focus:border-rose-500/50 focus:ring-rose-500/10'
                        : slugTouched && slug.length >= 2
                        ? 'border-emerald-500/30 focus:border-emerald-500/45 focus:ring-emerald-500/10'
                        : 'border-white/5 focus:border-purple-500/30 focus:ring-purple-500/20'
                    }`}
                  />
                </div>
                {slugTouched && slug.length < 2 && (
                  <p className="text-[11px] text-rose-400/80 mt-1.5 flex items-center gap-1 font-sans">
                    <span>●</span> Mínimo de 2 caracteres
                  </p>
                )}
              </div>
            </div>

            {/* ── Owner data ── */}
            <div className="border-t border-white/5 pt-5">
              <h3 className="text-sm font-semibold text-white/70 mb-4">Dados do Proprietário</h3>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                Seu Nome Completo
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-muted-foreground">
                  <User className="w-5 h-5" />
                </div>
                <input
                  type="text"
                  required
                  placeholder="Seu Nome"
                  value={adminName}
                  onChange={(e) => setAdminName(e.target.value)}
                  disabled={isLoading}
                  className="w-full pl-11 pr-4 py-3 bg-white/5 border border-white/5 focus:border-purple-500/30 rounded-2xl text-white placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-purple-500/20 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                E-mail Administrativo
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-muted-foreground">
                  <Mail className="w-5 h-5" />
                </div>
                <input
                  type="email"
                  required
                  placeholder="admin@empresa.com"
                  value={adminEmail}
                  onChange={(e) => { setAdminEmail(e.target.value); setEmailTouched(true); }}
                  onBlur={() => setEmailTouched(true)}
                  disabled={isLoading}
                  className={`w-full pl-11 pr-4 py-3 bg-white/5 border rounded-2xl text-white placeholder-muted-foreground focus:outline-none focus:ring-2 transition-all text-sm ${
                    emailTouched && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminEmail)
                      ? 'border-rose-500/40 focus:border-rose-500/50 focus:ring-rose-500/10'
                      : emailTouched && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminEmail)
                      ? 'border-emerald-500/30 focus:border-emerald-500/45 focus:ring-emerald-500/10'
                      : 'border-white/5 focus:border-purple-500/30 focus:ring-purple-500/20'
                  }`}
                />
              </div>
              {emailTouched && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminEmail) && (
                <p className="text-[11px] text-rose-400/80 mt-1.5 flex items-center gap-1">
                  <span>●</span> Insira um formato de e-mail válido
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                Sua Senha de Acesso
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-muted-foreground">
                  <Lock className="w-5 h-5" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  placeholder="Mínimo de 6 caracteres"
                  value={adminPassword}
                  onChange={(e) => { setAdminPassword(e.target.value); setPasswordTouched(true); }}
                  onBlur={() => setPasswordTouched(true)}
                  disabled={isLoading}
                  className={`w-full pl-11 pr-10 py-3 bg-white/5 border rounded-2xl text-white placeholder-muted-foreground focus:outline-none focus:ring-2 transition-all text-sm ${
                    passwordTouched && adminPassword.length < 6
                      ? 'border-rose-500/40 focus:border-rose-500/50 focus:ring-rose-500/10'
                      : passwordTouched && adminPassword.length >= 6
                      ? 'border-emerald-500/30 focus:border-emerald-500/45 focus:ring-emerald-500/10'
                      : 'border-white/5 focus:border-purple-500/30 focus:ring-purple-500/20'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-muted-foreground hover:text-white transition-colors"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
              {passwordTouched && adminPassword.length < 6 && (
                <p className="text-[11px] text-rose-400/80 mt-1.5 flex items-center gap-1">
                  <span>●</span> A senha deve ter pelo menos 6 caracteres
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3.5 text-white font-semibold rounded-2xl shadow-xl flex items-center justify-center gap-2 transition-all hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:hover:scale-100"
              style={{
                background: `linear-gradient(135deg, ${activeSector.accent}bb, ${activeSector.accent}80)`,
                boxShadow: `0 12px 32px ${activeSector.accentBg}`,
                transition: 'background 0.3s ease, box-shadow 0.3s ease',
              }}
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  <span>Cadastrando Empresa...</span>
                </>
              ) : (
                <>
                  <span>Cadastrar e Iniciar</span>
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-white/5 text-center text-sm text-muted-foreground">
            Já possui uma empresa cadastrada?{' '}
            <Link to="/login" className="text-purple-400 hover:text-purple-300 font-medium transition-colors">
              Efetuar Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
