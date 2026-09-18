package thz.lang.interpretador;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import thz.lang.ast.CampoEstruturaAst;
import thz.lang.ast.ClausulaContratoAst;
import thz.lang.ast.ComandoAst;
import thz.lang.ast.EnumeracaoAst;
import thz.lang.ast.EstruturaAst;
import thz.lang.ast.ExprAst;
import thz.lang.ast.FuncaoAst;
import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ParametroOperacaoAst;
import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;
import thz.lang.runtime.BlocoMemoria;
import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.RegistroIdempotencia;

/**
 * Interpretador Tree-Walking THZ — port exato de src/interpretador.ts (51477
 * bytes).
 * Package thz.lang.interpretador, JDK 25, sem dependências externas.
 */
public class InterpretadorThz {

    private final ProgramaAst ast;
    private final Consumer<String> emitir;
    private final int maxIteracoes;
    private final Supplier<String> lerEntrada;
    private final RegistroIdempotencia registroIdempotencia = new RegistroIdempotencia();
    private final ValidadorContratos validadorContratos;
    private final thz.lang.inventario.InventarioServico inventario;

    private static final int LIMITE_PADRAO_ITERACOES = 10_000_000;

    // ---- Opções ----

    public record OpcoesInterpretador(
            Consumer<String> saida,
            Supplier<String> entrada,
            Integer maxIteracoes) {
    }

    public record OperacaoResolvida(RegraNegocioAst regra, OperacaoAst operacao) {
    }

    // ---- Construtores ----

    public InterpretadorThz(ProgramaAst ast, OpcoesInterpretador opcoes) {
        this(ast, opcoes, null);
    }

    /** O serviço autenticado é uma capacidade do host, nunca um argumento do programa THZ. */
    public InterpretadorThz(ProgramaAst ast, OpcoesInterpretador opcoes, thz.lang.inventario.InventarioServico inventario) {
        this.ast = ast;
        this.inventario = inventario;
        if (opcoes != null && opcoes.saida() != null) {
            this.emitir = opcoes.saida();
        } else {
            this.emitir = System.out::println;
        }
        if (opcoes != null && opcoes.maxIteracoes() != null) {
            this.maxIteracoes = opcoes.maxIteracoes();
        } else {
            this.maxIteracoes = LIMITE_PADRAO_ITERACOES;
        }
        if (opcoes != null && opcoes.entrada() != null) {
            this.lerEntrada = opcoes.entrada();
        } else {
            this.lerEntrada = () -> {
                throw new ErroExecucao(
                        "[Erro de Execução] LER exige provedor de entrada (use --arg ou modo interativo).");
            };
        }
        this.validadorContratos = new ValidadorContratos(ast, this::avaliar);
    }

    public InterpretadorThz(ProgramaAst ast) {
        this(ast, null);
    }

    // Convenience constructor with direct Consumer/Supplier
    public InterpretadorThz(ProgramaAst ast, Consumer<String> saida, Supplier<String> entrada) {
        this(ast, new OpcoesInterpretador(saida, entrada, null));
    }

    public ProgramaAst ast() {
        return ast;
    }

    public thz.lang.inventario.InventarioServico inventario() {
        if (inventario == null) throw new ErroExecucao("Inventário exige um host autenticado; use o comando inventario.");
        return inventario;
    }

    private thz.lang.dap.ThzDebugListener debugListener;
    private String operacaoAtual = "Principal";

    public void setDebugListener(thz.lang.dap.ThzDebugListener debugListener) {
        this.debugListener = debugListener;
    }

    public thz.lang.dap.ThzDebugListener getDebugListener() {
        return debugListener;
    }

    // ---- Helpers STDLIB ----

    public static boolean ehStdlib(String nome) {
        return BibliotecaPadrao.ehStdlib(nome);
    }

    public boolean ehStdlibInstancia(String nome) {
        return BibliotecaPadrao.ehStdlib(nome);
    }

    // ---- Resolução de operações ----

    public List<OperacaoResolvida> listarOperacoesExecutaveis() {
        List<OperacaoResolvida> encontradas = new ArrayList<>();
        for (RegraNegocioAst regra : ast.regras()) {
            for (OperacaoAst op : regra.operacoes()) {
                if (!op.corpo().isEmpty())
                    encontradas.add(new OperacaoResolvida(regra, op));
            }
        }
        return encontradas;
    }

    public List<ProcedimentoAst> listarProcedimentos() {
        return ast.procedimentos() != null ? ast.procedimentos() : List.of();
    }

    public List<FuncaoAst> listarFuncoes() {
        return ast.funcoes() != null ? ast.funcoes() : List.of();
    }

    public ValorThz executarFuncao(String nome, Map<String, ValorThz> argumentos) {
        FuncaoAst funcao = listarFuncoes().stream().filter(f -> f.nome().equals(nome)).findFirst()
                .orElseThrow(() -> new ErroExecucao("[Erro de Execução] Função '" + nome + "' não encontrada."));
        Escopo escopo = new Escopo();
        Map<String, ValorThz> args = argumentos != null ? argumentos : Map.of();
        for (ParametroOperacaoAst p : funcao.parametros()) {
            ValorThz valor = args.get(p.nome());
            if (valor == null) throw new ErroExecucao("[Erro de Execução] Parâmetro '" + p.nome() + "' não fornecido para função '" + nome + "'.");
            escopo.definir(p.nome(), valor);
        }
        try {
            executarComandos(funcao.corpo(), escopo);
        } catch (SinalRetorne retorno) {
            if (retorno.getValor() == null) throw new ErroExecucao("[Erro de Execução] FUNCAO deve retornar um valor.");
            return retorno.getValor();
        } catch (SinalFalhar falha) {
            throw new ErroExecucao("[Erro de Execução] FALHAR_COM não permitido em FUNCAO.");
        }
        throw new ErroExecucao("[Erro de Execução] Função '" + nome + "' terminou sem RETORNE.");
    }

    private static boolean ehValorResultado(ValorThz v) {
        return v instanceof ValorThz.Resultado;
    }

    /**
     * Retorna o gerenciador de idempotência e auditoria de reuso da sessão de
     * execução.
     */
    public RegistroIdempotencia getRegistroIdempotencia() {
        return registroIdempotencia;
    }

    /**
     * Executa uma operação com contratos formais: EXIGE na entrada, GARANTE na
     * saída e IDEMPOTÊNCIA se declarada.
     */
    public ValorThz executarOperacao(String nomeOperacao, Map<String, ValorThz> argumentos) {
        OperacaoResolvida alvo = listarOperacoesExecutaveis().stream()
                .filter(o -> o.operacao().nome().equals(nomeOperacao))
                .findFirst().orElse(null);
        if (alvo == null) {
            throw new ErroExecucao(
                    "[Erro de Execução] Operação '" + nomeOperacao + "' não encontrada ou sem corpo executável.");
        }

        boolean ehIdempotente = alvo.operacao().idempotente() || alvo.regra().idempotente();
        String chaveIdemp = alvo.operacao().chaveIdempotencia() != null ? alvo.operacao().chaveIdempotencia()
                : alvo.regra().chaveIdempotencia();

        if (ehIdempotente || (chaveIdemp != null && !chaveIdemp.isBlank())) {
            return registroIdempotencia.executarOuReutilizar(
                    alvo.operacao().nome(),
                    chaveIdemp,
                    argumentos,
                    () -> executarOperacaoInterna(alvo, argumentos),
                    emitir);
        }

        return executarOperacaoInterna(alvo, argumentos);
    }

    private ValorThz executarOperacaoInterna(OperacaoResolvida alvo, Map<String, ValorThz> argumentos) {
        String nomeOperacao = alvo.operacao().nome();
        boolean retornoResultado = alvo.operacao().tipoRetorno() != null
                && alvo.operacao().tipoRetorno().startsWith("RESULTADO");

        Escopo escopoGlobal = new Escopo();
        if (argumentos != null) {
            for (Map.Entry<String, ValorThz> e : argumentos.entrySet())
                escopoGlobal.definir(e.getKey(), e.getValue());
        }

        validarContratos(alvo.regra().clausulasEntrada(), escopoGlobal, "EXIGE");

        ValorThz retorno = null;
        try {
            executarComandos(alvo.operacao().corpo(), escopoGlobal);
        } catch (SinalFalhar s) {
            if (!retornoResultado) {
                throw new ErroExecucao(
                        "[Erro de Execução] FALHAR_COM exige operação com retorno RESULTADO[T,E]; operacao '"
                                + nomeOperacao + "' declara '" + alvo.operacao().tipoRetorno() + "'.");
            }
            return new ValorThz.Resultado(false, null, s.getValor());
        } catch (SinalRetorne s) {
            retorno = s.getValor();
        }

        Escopo escopoSaida = new Escopo(escopoGlobal);
        escopoSaida.definir("RESULTADO", retorno != null ? retorno : new ValorThz.Nulo());
        validarContratos(alvo.regra().clausulasSaida(), escopoSaida, "GARANTE");
        if (retornoResultado && !ehValorResultado(retorno)) {
            return new ValorThz.Resultado(true, retorno, null);
        }
        return retorno;

    }

    public ValorThz executarOperacao(String nomeOperacao) {
        return executarOperacao(nomeOperacao, Map.of());
    }

    public void executarProcedimento(String nome, Map<String, ValorThz> argumentos) {
        ProcedimentoAst proc = listarProcedimentos().stream().filter(p -> p.nome().equals(nome)).findFirst()
                .orElse(null);
        if (proc == null)
            throw new ErroExecucao("[Erro de Execução] Procedimento '" + nome + "' não encontrado.");

        boolean ehIdempotente = proc.idempotente();
        String chaveIdemp = proc.chaveIdempotencia();

        if (ehIdempotente || (chaveIdemp != null && !chaveIdemp.isBlank())) {
            registroIdempotencia.executarOuReutilizar(
                    proc.nome(),
                    chaveIdemp,
                    argumentos,
                    () -> {
                        executarProcedimentoInterno(proc, argumentos);
                        return new ValorThz.Nulo();
                    },

                    emitir);
            return;
        }

        executarProcedimentoInterno(proc, argumentos);
    }

    private void executarProcedimentoInterno(ProcedimentoAst proc, Map<String, ValorThz> argumentos) {
        String nome = proc.nome();
        Escopo escopo = new Escopo();
        Map<String, ValorThz> args = argumentos != null ? argumentos : Map.of();
        for (ParametroOperacaoAst p : proc.parametros()) {
            ValorThz v = args.get(p.nome());
            if (v == null)
                throw new ErroExecucao("[Erro de Execução] Parâmetro '" + p.nome()
                        + "' não fornecido para procedimento '" + nome + "'.");
            escopo.definir(p.nome(), v);
        }
        try {
            executarComandos(proc.corpo(), escopo);
        } catch (SinalFalhar s) {
            throw new ErroExecucao(
                    "[Erro de Execução] FALHAR_COM não permitido dentro de PROCEDIMENTO (sem canal RESULTADO).");
        } catch (SinalRetorne s) {
            if (s.getValor() != null) {
                throw new ErroExecucao(
                        "[Erro de Execução] RETORNE com valor não permitido dentro de PROCEDIMENTO; use RETORNE sem valor.");
            }
        }
    }

    public void executarProcedimento(String nome) {
        executarProcedimento(nome, Map.of());
    }

    // ---- Contratos formais (delegação) ----

    private void validarContratos(List<ClausulaContratoAst> clausulas, Escopo escopo, String natureza) {
        validadorContratos.validarContratos(clausulas, escopo, natureza);
    }

    // ---- Avaliação de expressões (Pattern Matching Switch Exaustivo) ----

    private ValorThz avaliar(ExprAst expr, Escopo escopo) {
        return switch (expr) {
            case ExprAst.LiteralInteiro li -> ValorThz.INTEIRO(li.valor());
            case ExprAst.LiteralDecimal ld -> ValorThz.DECIMAL(new DecimalFixo(ld.escalado(), ld.escala()));
            case ExprAst.LiteralTexto lt -> ValorThz.TEXTO(lt.valor());
            case ExprAst.LiteralLogico ll -> ValorThz.LOGICO(ll.valor());
            case ExprAst.Nulo _ -> ValorThz.NULO;
            case ExprAst.FatiaLiteral fl -> avaliarFatiaLiteral(fl, escopo);
            case ExprAst.CriarRegistro cr -> avaliarCriarRegistro(cr, escopo);
            case ExprAst.Indexacao idx -> avaliarIndexacao(idx, escopo);
            case ExprAst.Chamada ch -> avaliarChamada(ch, escopo);
            case ExprAst.AcessoCampo ac -> avaliarAcessoCampo(ac, escopo);
            case ExprAst.OpUnaria ou -> avaliarOpUnaria(ou, escopo);
            case ExprAst.OpBinaria ob -> avaliarOpBinaria(ob, escopo);
            case ExprAst.ConsultaTipada ct -> avaliarConsultaTipada(ct, escopo);
        };
    }

    private ValorThz avaliarConsultaTipada(ExprAst.ConsultaTipada ct, Escopo escopo) {
        ValorThz fonteVal = avaliar(ct.fonte(), escopo);
        if (!(fonteVal instanceof ValorThz.Fatia fatia)) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ct.linha() + ":" + ct.coluna() + "] CONSULTAR exige como fonte uma FATIA, recebido: " + fonteVal.classe());
        }

        List<ValorThz> resultado = new ArrayList<>();
        for (ValorThz item : fatia.elementos()) {
            if (ct.onde() == null) {
                resultado.add(item);
                continue;
            }

            Escopo subEscopo = new Escopo(escopo);
            if (item instanceof ValorThz.Registro reg) {
                for (Map.Entry<String, ValorThz> entry : reg.campos().entrySet()) {
                    subEscopo.definir(entry.getKey(), entry.getValue());
                }
            } else {
                subEscopo.definir("item", item);
                subEscopo.definir("it", item);
            }

            ValorThz cond = avaliar(ct.onde(), subEscopo);
            if (cond instanceof ValorThz.Logico l && l.valor()) {
                resultado.add(item);
            }
        }

        if (ct.campoOrdenacao() != null) {
            String campo = ct.campoOrdenacao();
            boolean asc = ct.asc();
            resultado.sort((a, b) -> {
                ValorThz valA = a instanceof ValorThz.Registro ra ? ra.campos().get(campo) : a;
                ValorThz valB = b instanceof ValorThz.Registro rb ? rb.campos().get(campo) : b;
                if (valA == null && valB == null) return 0;
                if (valA == null) return asc ? -1 : 1;
                if (valB == null) return asc ? 1 : -1;
                Integer cmp = ordemNumerica(valA, valB, ct);
                if (cmp != null) return asc ? cmp : -cmp;
                int strCmp = valA.formatar().compareTo(valB.formatar());
                return asc ? strCmp : -strCmp;
            });
        }

        int start = 0;
        if (ct.pular() != null) {
            ValorThz pularVal = avaliar(ct.pular(), escopo);
            if (pularVal instanceof ValorThz.Inteiro i) {
                start = Math.max(0, Math.min(i.valor().intValue(), resultado.size()));
            }
        }

        int end = resultado.size();
        if (ct.limite() != null) {
            ValorThz limiteVal = avaliar(ct.limite(), escopo);
            if (limiteVal instanceof ValorThz.Inteiro i) {
                end = Math.min(start + Math.max(0, i.valor().intValue()), resultado.size());
            }
        }

        List<ValorThz> slice = resultado.subList(start, end);
        return new ValorThz.Fatia(fatia.tipoInterno(), new ArrayList<>(slice));
    }

    private ValorThz avaliarFatiaLiteral(ExprAst.FatiaLiteral fl, Escopo escopo) {
        List<ValorThz> elementos = new ArrayList<>();
        for (ExprAst e : fl.elementos())
            elementos.add(avaliar(e, escopo));
        String tipoInterno = "TEXTO";
        if (!elementos.isEmpty()) {
            ValorThz primeiro = elementos.get(0);
            if (primeiro instanceof ValorThz.Registro r)
                tipoInterno = r.nomeEstrutura();
            else if (primeiro instanceof ValorThz.Inteiro)
                tipoInterno = "INTEIRO";
            else if (primeiro instanceof ValorThz.Decimal)
                tipoInterno = "DECIMAL";
            else if (primeiro instanceof ValorThz.Texto)
                tipoInterno = "TEXTO";
            else if (primeiro instanceof ValorThz.Data)
                tipoInterno = "DATA";
            else if (primeiro instanceof ValorThz.DataHora)
                tipoInterno = "DATA_HORA";
            else
                tipoInterno = primeiro.classe();
        }
        return new ValorThz.Fatia(tipoInterno, List.copyOf(elementos));
    }

    private ValorThz avaliarCriarRegistro(ExprAst.CriarRegistro cr, Escopo escopo) {
        EstruturaAst estrutura = ast.estruturas().stream().filter(e -> e.nome().equals(cr.nomeEstrutura())).findFirst()
                .orElse(null);
        if (estrutura == null)
            throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna() + "] Estrutura '"
                    + cr.nomeEstrutura() + "' não declarada.");
        if (cr.campos().size() != estrutura.campos().size()) {
            throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna() + "] CRIAR '"
                    + cr.nomeEstrutura() + "' exige " + estrutura.campos().size() + " campos, recebidos "
                    + cr.campos().size() + ".");
        }
        Map<String, ValorThz> campos = new HashMap<>();
        for (CampoEstruturaAst campo : estrutura.campos()) {
            ExprAst.CampoValor fornecido = cr.campos().stream().filter(c -> c.nome().equals(campo.nome())).findFirst()
                    .orElse(null);
            if (fornecido == null)
                throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna() + "] Campo '"
                        + campo.nome() + "' não fornecido em CRIAR '" + cr.nomeEstrutura() + "'.");
            campos.put(campo.nome(), avaliar(fornecido.valor(), escopo));
        }
        ValorThz registro = new ValorThz.Registro(cr.nomeEstrutura(), campos);
        validarInvariantes(registro, null);
        return registro;
    }

    private ValorThz avaliarIndexacao(ExprAst.Indexacao idx, Escopo escopo) {
        ValorThz alvo = avaliar(idx.alvo(), escopo);
        ValorThz indice = avaliar(idx.indice(), escopo);
        if (indice instanceof ValorThz.Texto t && alvo instanceof ValorThz.Registro r) {
            ValorThz val = r.campos().get(t.valor());
            if (val == null) {
                throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Campo '" + t.valor() + "' não existe na estrutura " + r.nomeEstrutura());
            }
            return val;
        }
        if (!(indice instanceof ValorThz.Inteiro))
            throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna()
                    + "] Índice deve ser INTEIRO, recebido " + indice.classe());
        BigInteger bv = ((ValorThz.Inteiro) indice).valor();
        long iLong;
        try {
            iLong = bv.longValueExact();
        } catch (ArithmeticException ex) {
            throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna()
                    + "] Índice fora de limites: '" + bv + "'.");
        }
        if (iLong < 0 || iLong > Integer.MAX_VALUE)
            throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna()
                    + "] Índice fora de limites: '" + bv + "'.");
        int i = (int) iLong;
        if (alvo instanceof ValorThz.Fatia f) {
            if (i >= f.elementos().size())
                throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice " + i
                        + " fora da fatia (tamanho " + f.elementos().size() + ").");
            return f.elementos().get(i);
        }
        if (alvo instanceof ValorThz.Texto t) {
            if (i >= t.valor().length())
                throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice " + i
                        + " fora do texto (tamanho " + t.valor().length() + ").");
            return ValorThz.TEXTO(String.valueOf(t.valor().charAt(i)));
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna()
                + "] Indexação exige FATIA ou TEXTO, recebido " + alvo.classe());
    }

    private ValorThz avaliarChamada(ExprAst.Chamada ch, Escopo escopo) {
        String nomeQualificado = String.join(".", ch.caminho());
        List<ValorThz> args = new ArrayList<>();
        for (ExprAst a : ch.argumentos())
            args.add(avaliar(a, escopo));
        if (BibliotecaPadrao.ehStdlib(nomeQualificado)) {
            try {
                return BibliotecaPadrao.executar(nomeQualificado, args, ch, this);
            } catch (ErroExecucao e) {
                throw e;
            } catch (Exception e) {
                throw new ErroExecucao(e.getMessage());
            }
        }

        // procedimento?
        if (ch.caminho().size() == 1) {
            String nome = ch.caminho().get(0);
            FuncaoAst funcao = ast.funcoes() != null
                    ? ast.funcoes().stream().filter(f -> f.nome().equals(nome)).findFirst().orElse(null)
                    : null;
            if (funcao != null) {
                if (args.size() != funcao.parametros().size())
                    throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                            + "] Função '" + funcao.nome() + "' exige " + funcao.parametros().size() + " arg(s), recebidos " + args.size() + ".");
                Map<String, ValorThz> mapa = new HashMap<>();
                for (int i = 0; i < funcao.parametros().size(); i++) mapa.put(funcao.parametros().get(i).nome(), args.get(i));
                return executarFuncao(nome, mapa);
            }
            ProcedimentoAst proc = ast.procedimentos() != null
                    ? ast.procedimentos().stream().filter(p -> p.nome().equals(nome)).findFirst().orElse(null)
                    : null;
            if (proc != null) {
                if (args.size() != proc.parametros().size())
                    throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                            + "] Procedimento '" + proc.nome() + "' exige " + proc.parametros().size()
                            + " arg(s), recebidos " + args.size() + ".");
                Escopo escopoChamada = new Escopo();
                for (int i = 0; i < proc.parametros().size(); i++)
                    escopoChamada.definir(proc.parametros().get(i).nome(), args.get(i));
                try {
                    executarComandos(proc.corpo(), escopoChamada);
                } catch (SinalRetorne s) {
                    if (s.getValor() != null)
                        throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                                + "] RETORNE com valor não permitido em PROCEDIMENTO.");
                    return ValorThz.NULO;
                } catch (SinalFalhar s) {
                    throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                            + "] FALHAR_COM não permitido em PROCEDIMENTO.");
                }
                return ValorThz.NULO;
            }
        }
        // operação de regra qualificada (Regra.Operacao)
        if (ch.caminho().size() == 2) {
            String nomeRegra = ch.caminho().get(0);
            String nomeOp = ch.caminho().get(1);
            for (RegraNegocioAst regra : ast.regras()) {
                if (regra.nome().equals(nomeRegra)) {
                    OperacaoAst op = regra.operacoes().stream().filter(o -> o.nome().equals(nomeOp)).findFirst()
                            .orElse(null);
                    if (op != null) {
                        if (args.size() != op.parametros().size())
                            throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                                    + "] Operação '" + op.nome() + "' exige " + op.parametros().size()
                                    + " arg(s), recebidos " + args.size() + ".");
                        Map<String, ValorThz> mapa = new HashMap<>();
                        for (int i = 0; i < op.parametros().size(); i++)
                            mapa.put(op.parametros().get(i).nome(), args.get(i));
                        ValorThz ret = executarOperacao(op.nome(), mapa);
                        return ret != null ? ret : ValorThz.NULO;
                    }
                }
            }
        }

        // operação de regra direta por nome simples
        if (ch.caminho().size() == 1) {
            for (RegraNegocioAst regra : ast.regras()) {
                OperacaoAst op = regra.operacoes().stream().filter(o -> o.nome().equals(ch.caminho().get(0)))
                        .findFirst().orElse(null);
                if (op != null) {
                    if (args.size() != op.parametros().size())
                        throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                                + "] Operação '" + op.nome() + "' exige " + op.parametros().size()
                                + " arg(s), recebidos " + args.size() + ".");
                    Map<String, ValorThz> mapa = new HashMap<>();
                    for (int i = 0; i < op.parametros().size(); i++)
                        mapa.put(op.parametros().get(i).nome(), args.get(i));
                    ValorThz ret = executarOperacao(op.nome(), mapa);
                    return ret != null ? ret : ValorThz.NULO;
                }
            }
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna()
                + "] Chamada desconhecida: '" + nomeQualificado + "'.");
    }

    private ValorThz avaliarAcessoCampo(ExprAst.AcessoCampo ac, Escopo escopo) {
        ValorThz base = escopo.resolver(ac.caminho().get(0));
        if (base == null) {
            if (ac.caminho().size() == 1) {
                String ident = ac.caminho().get(0);
                for (EnumeracaoAst en : ast.enumeracoes()) {
                    if (en.membros().contains(ident))
                        return new ValorThz.Enumerado(en.nome(), ident);
                }
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ac.linha() + ":" + ac.coluna()
                    + "] Identificador não declarado: '" + ac.caminho().get(0) + "'.");
        }
        ValorThz atual = base;
        for (int i = 1; i < ac.caminho().size(); i++) {
            String campo = ac.caminho().get(i);
            if (!(atual instanceof ValorThz.Registro reg)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ac.linha() + ":" + ac.coluna()
                        + "] Acesso a campo '" + campo + "' em valor que não é registro.");
            }
            ValorThz proximo = reg.campos().get(campo);
            if (proximo == null)
                throw new ErroExecucao("[Erro de Execução][Linha " + ac.linha() + ":" + ac.coluna() + "] Campo '"
                        + campo + "' inexistente em '" + reg.nomeEstrutura() + "'.");
            atual = proximo;
        }
        return atual;
    }

    private ValorThz avaliarOpUnaria(ExprAst.OpUnaria ou, Escopo escopo) {
        ValorThz operando = avaliar(ou.operando(), escopo);
        if ("NAO".equals(ou.operador())) {
            return ValorThz.LOGICO(!exigirLogico(operando, "operando do conectivo 'NAO'"));
        }
        if (operando instanceof ValorThz.Inteiro i)
            return ValorThz.INTEIRO(i.valor().negate());
        if (operando instanceof ValorThz.Decimal d)
            return ValorThz.DECIMAL(d.valor().negar());
        throw new ErroExecucao("[Erro de Execução][Linha " + ou.linha() + ":" + ou.coluna()
                + "] Negação aritmética exige valor numérico.");
    }

    private ValorThz avaliarOpBinaria(ExprAst.OpBinaria ob, Escopo escopo) {
        if ("E".equals(ob.operador())) {
            ValorThz esq = avaliar(ob.esquerda(), escopo);
            if (!exigirLogico(esq, "conectivo 'E'"))
                return ValorThz.LOGICO(false);
            return ValorThz.LOGICO(exigirLogico(avaliar(ob.direita(), escopo), "conectivo 'E'"));
        }
        if ("OU".equals(ob.operador())) {
            ValorThz esq = avaliar(ob.esquerda(), escopo);
            if (exigirLogico(esq, "conectivo 'OU'"))
                return ValorThz.LOGICO(true);
            return ValorThz.LOGICO(exigirLogico(avaliar(ob.direita(), escopo), "conectivo 'OU'"));
        }
        ValorThz esquerda = avaliar(ob.esquerda(), escopo);
        ValorThz direita = avaliar(ob.direita(), escopo);
        if (Set.of("=", "<>", "<", "<=", ">", ">=").contains(ob.operador())) {
            return ValorThz.LOGICO(comparar(esquerda, direita, ob.operador(), ob));
        }
        return aritmetica(esquerda, direita, ob.operador(), ob);
    }

    private boolean exigirLogico(ValorThz v, String contexto) {
        return ValorThzUtils.exigirLogico(v, contexto);
    }

    private boolean comparar(ValorThz a, ValorThz b, String operador, ExprAst ctx) {
        return ValorThzUtils.comparar(a, b, operador, ctx);
    }

    private Integer ordemNumerica(ValorThz x, ValorThz y, ExprAst ctx) {
        return ValorThzUtils.ordemNumerica(x, y, ctx);
    }

    private ValorThz aritmetica(ValorThz a, ValorThz b, String operador, ExprAst ctx) {
        return ValorThzUtils.aritmetica(a, b, operador, ctx);
    }

    public String formatar(ValorThz v) {
        return ValorThzUtils.formatar(v);
    }

    // ---- Execução de comandos (Pattern Matching Switch Exaustivo) ----

    private void executarComandos(List<ComandoAst> comandos, Escopo escopo) {
        if (comandos == null)
            return;
        for (ComandoAst cmd : comandos)
            executarComando(cmd, escopo);
    }

    private void executarComando(ComandoAst cmd, Escopo escopo) {
        if (debugListener != null) {
            debugListener.aoExecutarLinha(cmd.linha(), operacaoAtual, escopo);
        }
        switch (cmd) {
            case ComandoAst.DeclVariavel decl -> executarDeclVariavel(decl, escopo);
            case ComandoAst.Atribuicao atr -> executarAtribuicao(atr, escopo);
            case ComandoAst.Se se -> executarSe(se, escopo);
            case ComandoAst.Enquanto enq -> executarEnquanto(enq, escopo);
            case ComandoAst.Tente tente -> executarTente(tente, escopo);
            case ComandoAst.Para para -> executarPara(para, escopo);
            case ComandoAst.VetorizarPara vp -> executarVetorizarPara(vp, escopo);
            case ComandoAst.BlocoMemoria bm -> executarBlocoMemoria(bm, escopo);
            case ComandoAst.Exiba ex -> emitir.accept(formatar(avaliar(ex.expressao(), escopo)));
            case ComandoAst.Ler ler -> executarLer(ler, escopo);
            case ComandoAst.Chamada ch -> avaliar(ch.expressao(), escopo);
            case ComandoAst.Retorne ret ->
                throw new SinalRetorne(ret.expressao() != null ? avaliar(ret.expressao(), escopo) : null);
            case ComandoAst.FalharCom fc -> throw new SinalFalhar(avaliar(fc.expressao(), escopo));
            case ComandoAst.CasoResultado cr -> executarCasoResultado(cr, escopo);
        }
    }

    private void executarCasoResultado(ComandoAst.CasoResultado cr, Escopo escopo) {
        ValorThz alvo = avaliar(cr.alvo(), escopo);
        if (!(alvo instanceof ValorThz.Resultado res)) {
            throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna()
                    + "] CASO_RESULTADO exige valor RESULTADO; obtido " + formatar(alvo) + ".");
        }
        if (res.sucesso()) {
            if (cr.varSucesso() != null && cr.corpoSucesso() != null) {
                Escopo escopoSucesso = new Escopo(escopo);
                escopoSucesso.definir(cr.varSucesso(), res.valor());
                executarComandos(cr.corpoSucesso(), escopoSucesso);
            }
        } else {
            if (cr.varErro() != null && cr.corpoErro() != null) {
                Escopo escopoErro = new Escopo(escopo);
                escopoErro.definir(cr.varErro(), res.erro());
                executarComandos(cr.corpoErro(), escopoErro);
            }
        }
    }

    private void executarTente(ComandoAst.Tente tente, Escopo escopo) {
        try {
            executarComandos(tente.corpoTente(), new Escopo(escopo));
        } catch (SinalFalhar falha) {
            executarComandos(tente.corpoCapture(), new Escopo(escopo));
        }
    }

    private void executarDeclVariavel(ComandoAst.DeclVariavel decl, Escopo escopo) {
        if (escopo.resolver(decl.nome()) != null) {
            throw new ErroExecucao("[Erro de Execução][Linha " + decl.linha() + ":" + decl.coluna() + "] Variável '"
                    + decl.nome() + "' já declarada neste escopo.");
        }
        escopo.definir(decl.nome(), avaliar(decl.inicializacao(), escopo));
    }

    private void executarAtribuicao(ComandoAst.Atribuicao atr, Escopo escopo) {
        ValorThz base = escopo.resolver(atr.alvo().get(0));
        if (base == null)
            throw new ErroExecucao("[Erro de Execução][Linha " + atr.linha() + ":" + atr.coluna()
                    + "] Atribuição a identificador não declarado: '" + atr.alvo().get(0) + "'.");
        ValorThz valor = avaliar(atr.expressao(), escopo);
        if (atr.alvo().size() == 1) {
            if (!escopo.atualizar(atr.alvo().get(0), valor))
                throw new ErroExecucao("[Erro de Execução][Linha " + atr.linha() + ":" + atr.coluna()
                        + "] Atribuição a identificador não declarado: '" + atr.alvo().get(0) + "'.");
            validarInvariantes(valor, atr);
        } else {
            atribuirCampo(base, atr.alvo().subList(1, atr.alvo().size()), valor, atr);
            validarInvariantes(base, atr);
        }
    }

    private void executarSe(ComandoAst.Se se, Escopo escopo) {
        if (exigirLogico(avaliar(se.condicao(), escopo), "condição do 'SE'")) {
            executarComandos(se.entao(), new Escopo(escopo));
        } else {
            executarComandos(se.senao(), new Escopo(escopo));
        }
    }

    private void executarEnquanto(ComandoAst.Enquanto enq, Escopo escopo) {
        int iteracoes = 0;
        while (exigirLogico(avaliar(enq.condicao(), escopo), "condição do 'ENQUANTO'")) {
            if (++iteracoes > maxIteracoes)
                throw new ErroExecucao("[Erro de Execução][Linha " + enq.linha() + ":" + enq.coluna()
                        + "] Laço 'ENQUANTO' excedeu " + maxIteracoes + " iterações (guarda anti-loop).");
            executarComandos(enq.corpo(), new Escopo(escopo));
        }
    }

    private void executarPara(ComandoAst.Para para, Escopo escopo) {
        ValorThz inicio = avaliar(para.inicio(), escopo);
        ValorThz fim = avaliar(para.fim(), escopo);
        if (!(inicio instanceof ValorThz.Inteiro))
            throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna()
                    + "] 'PARA' exige início INTEIRO, recebido " + inicio.classe());
        if (!(fim instanceof ValorThz.Inteiro))
            throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna()
                    + "] 'PARA' exige fim INTEIRO, recebido " + fim.classe());
        BigInteger passo = BigInteger.ONE;
        if (para.passo() != null) {
            ValorThz v = avaliar(para.passo(), escopo);
            if (!(v instanceof ValorThz.Inteiro))
                throw new ErroExecucao(
                        "[Erro de Execução][Linha " + para.linha() + ":" + para.coluna() + "] 'PASSO' exige INTEIRO");
            passo = ((ValorThz.Inteiro) v).valor();
            if (passo.equals(BigInteger.ZERO))
                throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna()
                        + "] 'PASSO' não pode ser zero.");
        }
        int iter = 0;
        BigInteger cur = ((ValorThz.Inteiro) inicio).valor();
        BigInteger fimVal = ((ValorThz.Inteiro) fim).valor();
        boolean crescente = passo.signum() > 0;
        while (crescente ? cur.compareTo(fimVal) <= 0 : cur.compareTo(fimVal) >= 0) {
            if (++iter > maxIteracoes)
                throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna()
                        + "] Laço 'PARA' excedeu " + maxIteracoes + " iterações (guarda anti-loop).");
            Escopo esc = new Escopo(escopo);
            esc.definir(para.variavel(), ValorThz.INTEIRO(cur));
            executarComandos(para.corpo(), esc);
            cur = cur.add(passo);
        }
    }

    private void executarVetorizarPara(ComandoAst.VetorizarPara vp, Escopo escopo) {
        ValorThz fonte = escopo.resolver(vp.fonte().get(0));
        if (vp.fonte().size() > 1) {
            ExprAst.AcessoCampo dummy = new ExprAst.AcessoCampo(vp.fonte(), vp.linha(), vp.coluna());
            fonte = avaliar(dummy, escopo);
        }
        if (!(fonte instanceof ValorThz.Fatia f)) {
            throw new ErroExecucao("[Erro de Execução][Linha " + vp.linha() + ":" + vp.coluna()
                    + "] Fonte do 'VETORIZAR_PARA' deve ser uma fatia: " + String.join(".", vp.fonte()));
        }
        for (ValorThz elemento : f.elementos()) {
            Escopo escopoIteracao = new Escopo(escopo);
            escopoIteracao.definir(vp.variavel(), elemento);
            executarComandos(vp.corpo(), escopoIteracao);
        }
    }

    private void executarBlocoMemoria(ComandoAst.BlocoMemoria bm, Escopo escopo) {
        BlocoMemoria bloco = new BlocoMemoria(1);
        bloco.alocar(1024);
        try {
            executarComandos(bm.corpo(), new Escopo(escopo));
        } finally {
            bloco.liberarTudo();
        }
    }

    private void executarLer(ComandoAst.Ler ler, Escopo escopo) {
        String linha = lerEntrada.get();
        if (linha == null)
            throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                    + "] Entrada encerrada (EOF) em LER " + String.join(".", ler.alvo()));
        ValorThz base = escopo.resolver(ler.alvo().get(0));
        if (base == null)
            throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                    + "] LER alvo não declarado: '" + ler.alvo().get(0) + "'.");
        ValorThz novo;
        if (base instanceof ValorThz.Inteiro) {
            String s = linha.trim();
            if (!s.matches("^-?\\d+$"))
                throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                        + "] LER INTEIRO exige dígitos: '" + linha + "'");
            novo = ValorThz.INTEIRO(new BigInteger(s));
        } else if (base instanceof ValorThz.Decimal d) {
            try {
                novo = ValorThz.DECIMAL(DecimalFixo.deTexto(linha.trim(), d.valor().getEscala()));
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                        + "] LER DECIMAL inválido: " + e.getMessage());
            }
        } else if (base instanceof ValorThz.Data) {
            try {
                novo = ValorThz.DATA(DataThz.deTexto(linha.trim()));
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                        + "] LER DATA inválido: " + e.getMessage());
            }
        } else if (base instanceof ValorThz.DataHora) {
            try {
                novo = ValorThz.DATA_HORA(DataHoraThz.deTexto(linha.trim()));
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                        + "] LER DATA_HORA inválido: " + e.getMessage());
            }
        } else if (base instanceof ValorThz.Texto) {
            novo = ValorThz.TEXTO(linha);
        } else {
            String t = linha.trim().toUpperCase();
            if (base instanceof ValorThz.Logico)
                novo = ValorThz.LOGICO(t.equals("VERDADEIRO") || t.equals("TRUE") || t.equals("1"));
            else
                novo = ValorThz.TEXTO(linha);
        }
        if (ler.alvo().size() == 1) {
            if (!escopo.atualizar(ler.alvo().get(0), novo))
                throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna()
                        + "] LER alvo não declarado: '" + ler.alvo().get(0) + "'.");
        } else {
            atribuirCampo(base, ler.alvo().subList(1, ler.alvo().size()), novo, ler);
        }
    }

    // ---- Invariantes (delegação) ----

    public void validarInvariantes(ValorThz valor, ComandoAst cmd) {
        validadorContratos.validarInvariantes(valor, cmd);
    }

    public void validarInvariantes(ValorThz valor) {
        validadorContratos.validarInvariantes(valor);
    }

    private void atribuirCampo(ValorThz alvo, List<String> caminhoRestante, ValorThz valor, ComandoAst cmd) {
        if (caminhoRestante.isEmpty())
            throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna()
                    + "] Caminho de atribuição malformado.");
        String campoFinal = caminhoRestante.get(caminhoRestante.size() - 1);
        ValorThz container = alvo;
        for (int i = 0; i < caminhoRestante.size() - 1; i++) {
            if (!(container instanceof ValorThz.Registro reg))
                throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna()
                        + "] Caminho de atribuição inválido em '" + caminhoRestante.get(i) + "'.");
            ValorThz proximo = reg.campos().get(caminhoRestante.get(i));
            if (proximo == null)
                throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Campo '"
                        + caminhoRestante.get(i) + "' inexistente.");
            container = proximo;
        }
        if (!(container instanceof ValorThz.Registro reg))
            throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna()
                    + "] Atribuição a campo exige registro.");
        reg.campos().put(campoFinal, valor);
    }

    // ---- Utilidades numéricas (delegação) ----

    public static ValorThz valorThzDe(String tipoDado, Object bruto) {
        return ValorThzUtils.valorThzDe(tipoDado, bruto);
    }
}
