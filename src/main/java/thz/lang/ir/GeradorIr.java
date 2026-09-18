package thz.lang.ir;

import thz.lang.ast.*;
import thz.lang.simd.ResultadoValidacaoSimd;
import thz.lang.simd.ValidadorSimd;
import thz.lang.sintatico.ThzParser;

import java.util.*;

/**
 * Gerador de Representação Intermediária (THZ-IR/1) e Emissor de LLVM IR AOT (G5/G7).
 */
public final class GeradorIr {

    public static final String VERSAO_IR = "thz-ir/1";

    private GeradorIr() {}

    /**
     * Baixa a AST do THZ-LANG para a representação intermediária THZ-IR.
     */
    public static IrPrograma baixarParaIr(ProgramaAst ast) {
        if (ast == null) {
            throw new IllegalArgumentException("AST não pode ser nula para geração de IR.");
        }

        Map<String, String> meta = new LinkedHashMap<>();
        if (ast.metadados() != null) {
            if (ast.metadados().dominio() != null) meta.put("dominio", ast.metadados().dominio());
            if (ast.metadados().camada() != null) meta.put("camada", ast.metadados().camada());
            if (ast.metadados().sloLatencia() != null) meta.put("slo", ast.metadados().sloLatencia());
        }

        // Estruturas
        List<IrPrograma.IrEstrutura> estruturas = new ArrayList<>();
        if (ast.estruturas() != null) {
            for (EstruturaAst est : ast.estruturas()) {
                List<IrPrograma.IrCampo> campos = new ArrayList<>();
                for (CampoEstruturaAst c : est.campos()) {
                    campos.add(new IrPrograma.IrCampo(c.nome(), mapearTipoIr(c.tipo()), c.tipo()));
                }
                estruturas.add(new IrPrograma.IrEstrutura(est.nome(), est.layoutColunar(), List.copyOf(campos)));
            }
        }

        // Funções / Operações / Procedimentos
        List<IrPrograma.IrFuncao> funcoes = new ArrayList<>();

        if (ast.regras() != null) {
            for (RegraNegocioAst regra : ast.regras()) {
                if (regra.operacoes() != null) {
                    for (OperacaoAst op : regra.operacoes()) {
                        List<IrPrograma.IrParametro> params = new ArrayList<>();
                        for (ParametroOperacaoAst p : op.parametros()) {
                            params.add(new IrPrograma.IrParametro(p.nome(), mapearTipoIr(p.tipo())));
                        }
                        List<String> instrs = baixarComandosParaIr(op.corpo());
                        boolean idemp = op.idempotente() || regra.idempotente();
                        String chave = op.chaveIdempotencia() != null ? op.chaveIdempotencia() : regra.chaveIdempotencia();
                        funcoes.add(new IrPrograma.IrFuncao(
                                regra.nome() + "_" + op.nome(),
                                op.tipoRetorno() != null ? mapearTipoIr(op.tipoRetorno()) : "void",
                                List.copyOf(params),
                                List.copyOf(instrs),
                                idemp,
                                chave
                        ));
                    }
                }
            }
        }

        if (ast.procedimentos() != null) {
            for (ProcedimentoAst proc : ast.procedimentos()) {
                List<IrPrograma.IrParametro> params = new ArrayList<>();
                for (ParametroOperacaoAst p : proc.parametros()) {
                    params.add(new IrPrograma.IrParametro(p.nome(), mapearTipoIr(p.tipo())));
                }
                List<String> instrs = baixarComandosParaIr(proc.corpo());
                funcoes.add(new IrPrograma.IrFuncao(
                        proc.nome(),
                        "void",
                        List.copyOf(params),
                        List.copyOf(instrs),
                        proc.idempotente(),
                        proc.chaveIdempotencia()
                ));
            }
        }

        if (ast.funcoes() != null) {
            for (FuncaoAst funcao : ast.funcoes()) {
                List<IrPrograma.IrParametro> params = new ArrayList<>();
                for (ParametroOperacaoAst p : funcao.parametros()) {
                    params.add(new IrPrograma.IrParametro(p.nome(), mapearTipoIr(p.tipo())));
                }
                funcoes.add(new IrPrograma.IrFuncao(funcao.nome(), mapearTipoIr(funcao.tipoRetorno()),
                        List.copyOf(params), List.copyOf(baixarComandosParaIr(funcao.corpo())), false, null));
            }
        }

        // Loops SIMD
        List<IrPrograma.IrSimdLoop> loopsSimd = new ArrayList<>();
        List<ResultadoValidacaoSimd> simdVal = ValidadorSimd.analisarTudo(ast);
        for (ResultadoValidacaoSimd s : simdVal) {
            loopsSimd.add(new IrPrograma.IrSimdLoop(
                    s.loopIdentificador(),
                    s.variavel(),
                    String.join(".", s.fonte()),
                    s.passoSimd(),
                    s.vetorizavel()
            ));
        }

        return new IrPrograma(
                VERSAO_IR,
                ast.nome(),
                ast.versaoLinguagem() != null ? ast.versaoLinguagem() : thz.lang.version.ThzVersion.ATUAL.toString(),
                Map.copyOf(meta),
                List.copyOf(estruturas),
                List.copyOf(funcoes),
                List.copyOf(loopsSimd)
        );
    }

    private static List<String> baixarComandosParaIr(List<ComandoAst> comandos) {
        List<String> out = new ArrayList<>();
        if (comandos == null) return out;
        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.DeclVariavel d -> out.add("%" + d.nome() + " = alloca " + mapearTipoIr(d.tipoDado()) + " <- " + ThzParser.textoCanonicoDe(d.inicializacao()));
                case ComandoAst.Atribuicao a -> out.add("store " + ThzParser.textoCanonicoDe(a.expressao()) + " -> " + String.join(".", a.alvo()));
                case ComandoAst.Se s -> out.add("branch " + ThzParser.textoCanonicoDe(s.condicao()) + " ? then(" + (s.entao()!=null?s.entao().size():0) + ") : else(" + (s.senao()!=null?s.senao().size():0) + ")");
                case ComandoAst.Enquanto e -> out.add("loop while " + ThzParser.textoCanonicoDe(e.condicao()));
                case ComandoAst.Para p -> out.add("loop for " + p.variavel() + " in [" + ThzParser.textoCanonicoDe(p.inicio()) + ".." + ThzParser.textoCanonicoDe(p.fim()) + "]");
                case ComandoAst.VetorizarPara vp -> out.add("vector_loop " + vp.variavel() + " in " + String.join(".", vp.fonte()) + " step_simd " + vp.passoSimd());
                case ComandoAst.BlocoMemoria _ -> out.add("scoped_arena_alloc 1024");
                case ComandoAst.Exiba ex -> out.add("call @thz_exiba(" + ThzParser.textoCanonicoDe(ex.expressao()) + ")");
                case ComandoAst.Ler ler -> out.add("call @thz_ler -> " + String.join(".", ler.alvo()));
                case ComandoAst.Chamada ch -> out.add("call @" + ThzParser.textoCanonicoDe(ch.expressao()));
                case ComandoAst.Retorne r -> out.add("ret " + (r.expressao() != null ? ThzParser.textoCanonicoDe(r.expressao()) : "void"));
                case ComandoAst.FalharCom fc -> out.add("fail " + ThzParser.textoCanonicoDe(fc.expressao()));
                case ComandoAst.CasoResultado cr -> out.add("match_result " + ThzParser.textoCanonicoDe(cr.alvo()));
                case ComandoAst.Tente t -> {
                    out.add("try_begin capture " + t.tipoCaptura());
                    out.addAll(baixarComandosParaIr(t.corpoTente()));
                    out.add("catch_begin " + t.tipoCaptura());
                    out.addAll(baixarComandosParaIr(t.corpoCapture()));
                    out.add("try_end");
                }
            }
        }
        return out;
    }

    private static String mapearTipoIr(String tipoThz) {
        if (tipoThz == null) return "i64";
        String t = tipoThz.toUpperCase();
        if (t.contains("DECIMAL") || t.contains("MONETARIO")) return "i128_fixed";
        if (t.contains("INTEIRO64") || t.equals("INTEIRO")) return "i64";
        if (t.contains("INTEIRO32") || t.contains("NATURAL32")) return "i32";
        if (t.contains("TEXTO")) return "ptr_utf8";
        if (t.contains("LOGICO")) return "i1";
        if (t.contains("FATIA")) return "soa_vector";
        return "ptr";
    }

    public static String serializarIr(IrPrograma ir) {
        return serializarIrJson(ir);
    }

    public static String serializarIrJson(IrPrograma ir) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"versaoIr\": \"").append(ir.versaoIr()).append("\",\n");
        sb.append("  \"nomePrograma\": \"").append(ir.nomePrograma()).append("\",\n");
        sb.append("  \"versaoFonte\": \"").append(ir.versaoFonte()).append("\",\n");
        sb.append("  \"metadados\": {\n");
        int count = 0;
        for (Map.Entry<String, String> e : ir.metadados().entrySet()) {
            sb.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"").append(++count < ir.metadados().size() ? "," : "").append("\n");
        }
        sb.append("  },\n");

        sb.append("  \"estruturas\": [\n");
        for (int i = 0; i < ir.estruturas().size(); i++) {
            IrPrograma.IrEstrutura e = ir.estruturas().get(i);
            sb.append("    {\n");
            sb.append("      \"nome\": \"").append(e.nome()).append("\",\n");
            sb.append("      \"layoutColunar\": ").append(e.layoutColunar()).append(",\n");
            sb.append("      \"campos\": [\n");
            for (int j = 0; j < e.campos().size(); j++) {
                IrPrograma.IrCampo c = e.campos().get(j);
                sb.append("        {\"nome\": \"").append(c.nome()).append("\", \"tipoIr\": \"").append(c.tipoPrimitivoIr()).append("\", \"tipoOriginal\": \"").append(c.tipoOriginal()).append("\"}").append(j + 1 < e.campos().size() ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i + 1 < ir.estruturas().size() ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"funcoes\": [\n");
        for (int i = 0; i < ir.funcoes().size(); i++) {
            IrPrograma.IrFuncao f = ir.funcoes().get(i);
            sb.append("    {\n");
            sb.append("      \"nome\": \"").append(f.nome()).append("\",\n");
            sb.append("      \"retorno\": \"").append(f.tipoRetornoIr()).append("\",\n");
            sb.append("      \"idempotente\": ").append(f.idempotente()).append(",\n");
            if (f.chaveIdempotencia() != null) {
                sb.append("      \"chaveIdempotencia\": \"").append(f.chaveIdempotencia()).append("\",\n");
            }
            sb.append("      \"parametros\": [");
            for (int p = 0; p < f.parametros().size(); p++) {
                IrPrograma.IrParametro param = f.parametros().get(p);
                sb.append("{\"nome\": \"").append(param.nome()).append("\", \"tipo\": \"").append(param.tipoIr()).append("\"}").append(p + 1 < f.parametros().size() ? ", " : "");
            }
            sb.append("],\n");
            sb.append("      \"instrucoes\": [\n");
            for (int k = 0; k < f.instrucoes().size(); k++) {
                sb.append("        \"").append(f.instrucoes().get(k).replace("\"", "\\\"")).append("\"").append(k + 1 < f.instrucoes().size() ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i + 1 < ir.funcoes().size() ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"loopsSimd\": [\n");
        for (int i = 0; i < ir.loopsSimd().size(); i++) {
            IrPrograma.IrSimdLoop s = ir.loopsSimd().get(i);
            sb.append("    {\"contexto\": \"").append(s.contexto()).append("\", \"variavel\": \"").append(s.variavel()).append("\", \"fonte\": \"").append(s.fonte()).append("\", \"passoSimd\": ").append(s.passoSimd()).append(", \"vetorizavel\": ").append(s.vetorizavel()).append("}").append(i + 1 < ir.loopsSimd().size() ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Valida se a AST contém nós ou comandos sem suporte implementado pelo backend LLVM AOT.
     * Retorna uma lista de limitações encontradas (vazia se o programa for 100% suportado).
     */
    public static List<String> validarCapacidadeLlvm(ProgramaAst ast) {
        if (ast == null) return List.of();
        List<String> limitacoes = new ArrayList<>();

        if (ast.regras() != null) {
            for (RegraNegocioAst r : ast.regras()) {
                if (r.operacoes() != null) {
                    for (OperacaoAst op : r.operacoes()) {
                        verificarComandosNaoSuportados(op.corpo(), "regra '" + r.nome() + "', operação '" + op.nome() + "'", limitacoes);
                    }
                }
            }
        }

        if (ast.procedimentos() != null) {
            for (ProcedimentoAst p : ast.procedimentos()) {
                verificarComandosNaoSuportados(p.corpo(), "procedimento '" + p.nome() + "'", limitacoes);
            }
        }

        if (ast.funcoes() != null) {
            for (FuncaoAst f : ast.funcoes()) {
                verificarComandosNaoSuportados(f.corpo(), "função '" + f.nome() + "'", limitacoes);
            }
        }

        return List.copyOf(limitacoes);
    }

    private static void verificarComandosNaoSuportados(List<ComandoAst> comandos, String contexto, List<String> limitacoes) {
        if (comandos == null) return;
        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.Exiba _ -> {}
                case ComandoAst.Chamada _ -> {}
                case ComandoAst.Retorne _ -> {}
                case ComandoAst.DeclVariavel _ -> {}
                case ComandoAst.Atribuicao _ -> {}
                case ComandoAst.Se s -> {
                    verificarComandosNaoSuportados(s.entao(), contexto, limitacoes);
                    verificarComandosNaoSuportados(s.senao(), contexto, limitacoes);
                }
                case ComandoAst.Enquanto e -> {
                    verificarComandosNaoSuportados(e.corpo(), contexto, limitacoes);
                }
                case ComandoAst.Para p -> {
                    verificarComandosNaoSuportados(p.corpo(), contexto, limitacoes);
                }
                case ComandoAst.Tente _ -> limitacoes.add("Tratamento de exceções (TENTE/CAPTURE) em " + contexto + " não suportado pelo emissor AOT.");
                case ComandoAst.FalharCom _ -> limitacoes.add("Interrupção com falha explícita (FALHAR_COM) em " + contexto + " não suportado pelo emissor AOT.");
                case ComandoAst.CasoResultado _ -> limitacoes.add("Pattern matching (CASO_RESULTADO) em " + contexto + " não suportado pelo emissor AOT.");
                case ComandoAst.Ler _ -> limitacoes.add("Comando interativo de entrada (LER) em " + contexto + " não suportado pelo emissor AOT.");
                case ComandoAst.BlocoMemoria _ -> limitacoes.add("Alocação dinâmica aninhada de arena local em " + contexto + " não suportada diretamente no AOT.");
                case ComandoAst.VetorizarPara vp -> limitacoes.add("Lowering de laço vetorizado SIMD (VETORIZAR_PARA " + vp.variavel() + ") em " + contexto + " aguarda suporte a intrinsics no emissor LLVM.");
            }
        }
    }

    /**
     * Emite representação textual LLVM IR para o backend AOT Dual-OS em modo padrão (não-estrito).
     */
    public static String emitirLlvm(ProgramaAst ast) {
        return emitirLlvm(ast, false);
    }

    /**
     * Emite representação textual LLVM IR para o backend AOT Dual-OS.
     * Em modo estrito, rejeita a emissão lançando {@link ErroEmissaoBackendException} se houver
     * comandos ou construções sem lowering AOT suportado.
     */
    public static String emitirLlvm(ProgramaAst ast, boolean estrito) {
        if (estrito) {
            List<String> limitacoes = validarCapacidadeLlvm(ast);
            if (!limitacoes.isEmpty()) {
                throw new ErroEmissaoBackendException("Backend LLVM", "Construções sem lowering",
                        "Compilação para LLVM rejeitada em modo estrito devido a construções não suportadas pelo emissor AOT:\n - "
                                + String.join("\n - ", limitacoes));
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("; ModuleID = 'thz.").append(ast.nome()).append("'\n");
        sb.append("source_filename = \"").append(ast.nome()).append(".thz\"\n");
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            sb.append("target datalayout = \"e-m:w-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128\"\n");
            sb.append("target triple = \"x86_64-pc-windows-msvc\"\n\n");
        } else {
            sb.append("target datalayout = \"e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128\"\n");
            sb.append("target triple = \"x86_64-pc-linux-gnu\"\n\n");
        }

        sb.append("; Declarações de runtime THZ-LANG O(1) Arena & IO\n");
        sb.append("declare ptr @thz_arena_alloc(i64 %bytes)\n");
        sb.append("declare void @thz_arena_free_all(ptr %arena)\n");
        sb.append("declare void @thz_exiba_str(ptr %msg)\n");
        sb.append("declare void @thz_exiba_i64(i64 %val)\n");
        sb.append("declare void @thz_exiba_i32(i32 %val)\n");
        sb.append("declare void @thz_exiba_bool(i1 %val)\n");
        sb.append("declare void @thz_exiba_i128(i128 %val, i32 %scale)\n");
        sb.append("declare void @thz_renderizar_tela(ptr %titulo, ptr %conteudo)\n\n");

        // Identifica se é um módulo GUI ou de Formulário
        boolean isGuiModule = ast.nome().toLowerCase().contains("gui") ||
                ast.nome().toLowerCase().contains("tela") ||
                (ast.estruturas() != null && ast.estruturas().stream().anyMatch(e -> e.nome().toLowerCase().contains("form")));

        if (isGuiModule) {
            sb.append("; Declarações de GUI Nativa Win32 (Janela Real com Formulário)\n");
            sb.append("declare i32 @thz_gui_iniciar(ptr %titulo, ptr %nomeEstrutura)\n");
            sb.append("declare void @thz_gui_adicionar_campo(i32 %formIdx, ptr %rotulo, ptr %valorPadrao, ptr %tipo)\n");
            sb.append("declare void @thz_gui_set_operacao(i32 %formIdx, ptr %operacao)\n");
            sb.append("declare void @thz_gui_exibir(i32 %formIdx)\n");
            sb.append("declare void @thz_gui_loop_mensagens()\n\n");
        }

        List<String> stringsConstantes = new ArrayList<>();
        Map<String, String> mapaStringGlobal = new LinkedHashMap<>();

        // Banner principal
        String bannerStr = "[THZ-LANG ENGINE AOT v2.4] Executando modulo: " + ast.nome();
        adicionarStringConstante(bannerStr, stringsConstantes, mapaStringGlobal);

        // Para módulos GUI, registra strings do formulário e dos campos da ESTRUTURA
        String guiTitleStr = null;
        String guiNomeEstruturaStr = null;
        String guiOperacaoStr = null;
        EstruturaAst guiEstrutura = null;

        if (isGuiModule) {
            // Encontra a ESTRUTURA principal do formulário
            if (ast.estruturas() != null && !ast.estruturas().isEmpty()) {
                guiEstrutura = ast.estruturas().get(0);
            }

            // Titulo da janela
            guiTitleStr = ast.nome().replace("_", " ") + " — THZ-LANG";
            adicionarStringConstante(guiTitleStr, stringsConstantes, mapaStringGlobal);

            // Nome da estrutura
            guiNomeEstruturaStr = guiEstrutura != null ? guiEstrutura.nome() : "Formulario";
            adicionarStringConstante(guiNomeEstruturaStr, stringsConstantes, mapaStringGlobal);

            // Nome da operação alvo (procura a primeira OPERACAO que não seja MontarTela)
            guiOperacaoStr = "Salvar";
            if (ast.regras() != null) {
                for (RegraNegocioAst r : ast.regras()) {
                    if (r.operacoes() != null) {
                        for (OperacaoAst op : r.operacoes()) {
                            if (!op.nome().equalsIgnoreCase("MontarTela")) {
                                guiOperacaoStr = op.nome();
                                break;
                            }
                        }
                    }
                    if (!guiOperacaoStr.equals("Salvar")) break;
                }
            }
            adicionarStringConstante(guiOperacaoStr, stringsConstantes, mapaStringGlobal);

            // Strings para cada campo da ESTRUTURA
            if (guiEstrutura != null) {
                for (CampoEstruturaAst campo : guiEstrutura.campos()) {
                    adicionarStringConstante(campo.nome(), stringsConstantes, mapaStringGlobal);
                    // Tipo do campo para display
                    String tipoDisplay = campo.tipo() != null ? campo.tipo() : "TEXTO";
                    adicionarStringConstante(tipoDisplay, stringsConstantes, mapaStringGlobal);
                }
            }
        }

        // Coleta todas as strings dos procedimentos e regras
        if (ast.procedimentos() != null) {
            for (ProcedimentoAst p : ast.procedimentos()) {
                coletarStrings(p.corpo(), stringsConstantes, mapaStringGlobal);
            }
        }
        if (ast.regras() != null) {
            for (RegraNegocioAst r : ast.regras()) {
                if (r.operacoes() != null) {
                    for (OperacaoAst op : r.operacoes()) {
                        coletarStrings(op.corpo(), stringsConstantes, mapaStringGlobal);
                    }
                }
            }
        }

        // Emite Globais de String em LLVM IR
        for (Map.Entry<String, String> e : mapaStringGlobal.entrySet()) {
            String val = e.getKey();
            String varName = e.getValue();
            byte[] bytes = val.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int len = bytes.length + 1;
            sb.append(varName).append(" = private unnamed_addr constant [").append(len).append(" x i8] c\"");
            for (byte b : bytes) {
                if (b >= 32 && b <= 126 && b != '"' && b != '\\') {
                    sb.append((char) b);
                } else {
                    sb.append(String.format("\\%02X", b));
                }
            }
            sb.append("\\00\", align 1\n");
        }
        sb.append("\n");

        // Estruturas
        if (ast.estruturas() != null) {
            for (EstruturaAst est : ast.estruturas()) {
                sb.append("%struct.").append(est.nome()).append(" = type { ");
                for (int i = 0; i < est.campos().size(); i++) {
                    CampoEstruturaAst c = est.campos().get(i);
                    sb.append(mapearTipoLlvm(c.tipo()));
                    if (i + 1 < est.campos().size()) sb.append(", ");
                }
                sb.append(" }\n");
            }
            sb.append("\n");
        }

        // Funções puras declaradas no módulo (inclui a forma compacta `= expressão`).
        // A assinatura é preservada no LLVM para permitir chamadas tipadas pelo AOT.
        // Funções puras declaradas no módulo (inclui a forma compacta `= expressão`).
        if (ast.funcoes() != null) {
            for (FuncaoAst funcao : ast.funcoes()) {
                String tipoRet = mapearTipoLlvm(funcao.tipoRetorno());
                String llvmNome = funcao.nome().equals("main") ? "thz_fn_main" : funcao.nome();
                sb.append("define ").append(tipoRet).append(" @")
                  .append(llvmNome).append("(");
                for (int i = 0; i < funcao.parametros().size(); i++) {
                    ParametroOperacaoAst p = funcao.parametros().get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(mapearTipoLlvm(p.tipo())).append(" %").append(p.nome());
                }
                sb.append(") {\nentry:\n");
                boolean ehRetornoPuro = funcao.corpo() != null && funcao.corpo().size() == 1
                        && funcao.corpo().get(0) instanceof ComandoAst.Retorne;
                if (ehRetornoPuro) {
                    emitirRetornoLlvm(sb, funcao);
                } else {
                    EmissorAotContexto ctx = new EmissorAotContexto(sb, mapaStringGlobal, ast);
                    for (ParametroOperacaoAst p : funcao.parametros()) {
                        String ptrParam = "%var." + p.nome();
                        String tipoParam = mapearTipoLlvm(p.tipo());
                        sb.append("  ").append(ptrParam).append(" = alloca ").append(tipoParam).append("\n");
                        sb.append("  store ").append(tipoParam).append(" %").append(p.nome()).append(", ptr ").append(ptrParam).append("\n");
                        ctx.variaveis.put(p.nome(), new EmissorAotContexto.VariavelLocal(ptrParam, tipoParam));
                    }
                    boolean terminou = emitirComandos(ctx, funcao.corpo());
                    if (!terminou) {
                        emitirRetornoLlvm(sb, funcao);
                    }
                }
                sb.append("}\n\n");
            }
        }

        // Emite Funções para as Operações das Regras de Negócio
        if (ast.regras() != null) {
            for (RegraNegocioAst r : ast.regras()) {
                if (r.operacoes() != null) {
                    for (OperacaoAst op : r.operacoes()) {
                        String fnName = r.nome() + "_" + op.nome();
                        String tipoRet = op.tipoRetorno() != null ? mapearTipoLlvm(op.tipoRetorno()) : "void";
                        sb.append("define ").append(tipoRet).append(" @").append(fnName).append("(");
                        for (int i = 0; i < op.parametros().size(); i++) {
                            ParametroOperacaoAst p = op.parametros().get(i);
                            if (i > 0) sb.append(", ");
                            sb.append(mapearTipoLlvm(p.tipo())).append(" %").append(p.nome());
                        }
                        sb.append(") {\nentry:\n");
                        EmissorAotContexto ctx = new EmissorAotContexto(sb, mapaStringGlobal, ast);
                        for (ParametroOperacaoAst p : op.parametros()) {
                            String ptrParam = "%var." + p.nome();
                            String tipoParam = mapearTipoLlvm(p.tipo());
                            sb.append("  ").append(ptrParam).append(" = alloca ").append(tipoParam).append("\n");
                            sb.append("  store ").append(tipoParam).append(" %").append(p.nome()).append(", ptr ").append(ptrParam).append("\n");
                            ctx.variaveis.put(p.nome(), new EmissorAotContexto.VariavelLocal(ptrParam, tipoParam));
                        }
                        boolean terminou = emitirComandos(ctx, op.corpo());
                        if (!terminou) {
                            if (tipoRet.equals("void")) {
                                sb.append("  ret void\n");
                            } else {
                                sb.append("  ret ").append(tipoRet).append(" 0\n");
                            }
                        }
                        sb.append("}\n\n");
                    }
                }
            }
        }

        // Emite Funções dos Procedimentos (não-GUI)
        if (ast.procedimentos() != null) {
            for (ProcedimentoAst proc : ast.procedimentos()) {
                sb.append("define void @").append(proc.nome()).append("() {\n");
                sb.append("entry:\n");
                EmissorAotContexto ctx = new EmissorAotContexto(sb, mapaStringGlobal, ast);
                boolean terminou = emitirComandos(ctx, proc.corpo());

                if (!isGuiModule && proc.nome().equalsIgnoreCase("Principal")) {
                    if (ast.regras() != null) {
                        for (RegraNegocioAst r : ast.regras()) {
                            if (r.operacoes() != null) {
                                for (OperacaoAst op : r.operacoes()) {
                                    sb.append("  call void @").append(r.nome()).append("_").append(op.nome()).append("()\n");
                                }
                            }
                        }
                    }
                }

                if (!terminou) {
                    sb.append("  ret void\n");
                }
                sb.append("}\n\n");
            }
        }

        // Função Principal / Main Entry Point
        sb.append("define i32 @main() {\n");
        sb.append("entry:\n");
        sb.append("  %arena = call ptr @thz_arena_alloc(i64 1048576)\n");
        sb.append("  call void @thz_exiba_str(ptr ").append(mapaStringGlobal.get(bannerStr)).append(")\n");

        if (isGuiModule && guiEstrutura != null) {
            // GUI Path: criar janela real com formulário nativo
            sb.append("\n  ; === GUI Nativa: Criação de Janela com Formulário ===\n");
            sb.append("  %formIdx = call i32 @thz_gui_iniciar(ptr ").append(mapaStringGlobal.get(guiTitleStr))
              .append(", ptr ").append(mapaStringGlobal.get(guiNomeEstruturaStr)).append(")\n");

            // Adicionar campo para cada campo da ESTRUTURA (exceto 'titulo' que é metadata)
            for (CampoEstruturaAst campo : guiEstrutura.campos()) {
                if (campo.nome().equalsIgnoreCase("titulo")) continue;
                String tipoDisplay = campo.tipo() != null ? campo.tipo() : "TEXTO";
                sb.append("  call void @thz_gui_adicionar_campo(i32 %formIdx, ptr ")
                  .append(mapaStringGlobal.get(campo.nome()))
                  .append(", ptr ").append(mapaStringGlobal.get(campo.nome()))  // placeholder = field name
                  .append(", ptr ").append(mapaStringGlobal.get(tipoDisplay))
                  .append(")\n");
            }

            // Definir a operação alvo do botão
            sb.append("  call void @thz_gui_set_operacao(i32 %formIdx, ptr ").append(mapaStringGlobal.get(guiOperacaoStr)).append(")\n");

            // Exibir a janela
            sb.append("  call void @thz_gui_exibir(i32 %formIdx)\n");

            // Entrar no message loop (bloqueia até fechar a janela)
            sb.append("  call void @thz_gui_loop_mensagens()\n\n");
        } else {
            // Console Path: chamar procedimentos e operações normalmente
            boolean chamouAlgumaCoisa = false;
            if (ast.procedimentos() != null && !ast.procedimentos().isEmpty()) {
                for (ProcedimentoAst proc : ast.procedimentos()) {
                    if (proc.nome().equalsIgnoreCase("Principal")) {
                        sb.append("  call void @Principal()\n");
                        chamouAlgumaCoisa = true;
                    }
                }
            }

            if (!chamouAlgumaCoisa && ast.regras() != null) {
                for (RegraNegocioAst r : ast.regras()) {
                    if (r.operacoes() != null) {
                        for (OperacaoAst op : r.operacoes()) {
                            sb.append("  call void @").append(r.nome()).append("_").append(op.nome()).append("()\n");
                            chamouAlgumaCoisa = true;
                        }
                    }
                }
            }

            if (!chamouAlgumaCoisa && ast.funcoes() != null) {
                for (FuncaoAst f : ast.funcoes()) {
                    if (f.nome().equalsIgnoreCase("main") || f.nome().equalsIgnoreCase("Principal")) {
                        String llvmNome = f.nome().equals("main") ? "thz_fn_main" : f.nome();
                        String tipoRet = mapearTipoLlvm(f.tipoRetorno());
                        sb.append("  call ").append(tipoRet).append(" @").append(llvmNome).append("()\n");
                        chamouAlgumaCoisa = true;
                        break;
                    }
                }
            }
        }

        sb.append("  call void @thz_arena_free_all(ptr %arena)\n");
        sb.append("  ret i32 0\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static void adicionarStringConstante(String val, List<String> list, Map<String, String> map) {
        if (!map.containsKey(val)) {
            String name = "@.str." + list.size();
            list.add(val);
            map.put(val, name);
        }
    }

    private static void coletarStringsExpr(ExprAst expr, List<String> list, Map<String, String> map) {
        if (expr == null) return;
        switch (expr) {
            case ExprAst.LiteralTexto lt -> adicionarStringConstante(lt.valor(), list, map);
            case ExprAst.OpBinaria b -> {
                coletarStringsExpr(b.esquerda(), list, map);
                coletarStringsExpr(b.direita(), list, map);
            }
            case ExprAst.OpUnaria u -> coletarStringsExpr(u.operando(), list, map);
            case ExprAst.Chamada ch -> {
                if (ch.argumentos() != null) {
                    for (ExprAst arg : ch.argumentos()) coletarStringsExpr(arg, list, map);
                }
            }
            default -> {}
        }
    }

    private static void coletarStrings(List<ComandoAst> comandos, List<String> list, Map<String, String> map) {
        if (comandos == null) return;
        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.Exiba ex -> {
                    String text = ThzParser.textoCanonicoDe(ex.expressao());
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        text = text.substring(1, text.length() - 1);
                        adicionarStringConstante(text, list, map);
                    } else {
                        coletarStringsExpr(ex.expressao(), list, map);
                    }
                }
                case ComandoAst.Chamada ch -> {
                    String text = ThzParser.textoCanonicoDe(ch.expressao());
                    adicionarStringConstante(text, list, map);
                }
                case ComandoAst.DeclVariavel dv -> coletarStringsExpr(dv.inicializacao(), list, map);
                case ComandoAst.Atribuicao at -> coletarStringsExpr(at.expressao(), list, map);
                case ComandoAst.Retorne ret -> coletarStringsExpr(ret.expressao(), list, map);
                case ComandoAst.Se s -> {
                    coletarStringsExpr(s.condicao(), list, map);
                    coletarStrings(s.entao(), list, map);
                    coletarStrings(s.senao(), list, map);
                }
                case ComandoAst.Enquanto e -> {
                    coletarStringsExpr(e.condicao(), list, map);
                    coletarStrings(e.corpo(), list, map);
                }
                case ComandoAst.Para p -> {
                    coletarStringsExpr(p.inicio(), list, map);
                    coletarStringsExpr(p.fim(), list, map);
                    coletarStringsExpr(p.passo(), list, map);
                    coletarStrings(p.corpo(), list, map);
                }
                default -> {}
            }
        }
    }

    private static final class EmissorAotContexto {
        final StringBuilder sb;
        final Map<String, String> mapaStringGlobal;
        final ProgramaAst ast;
        final Map<String, VariavelLocal> variaveis = new LinkedHashMap<>();
        int regContador = 1;
        int labelContador = 1;

        record VariavelLocal(String ptrLlvm, String tipoLlvm) {}

        EmissorAotContexto(StringBuilder sb, Map<String, String> mapaStringGlobal, ProgramaAst ast) {
            this.sb = sb;
            this.mapaStringGlobal = mapaStringGlobal;
            this.ast = ast;
        }

        String novoReg() {
            return "%t." + (regContador++);
        }

        String novoLabel(String prefixo) {
            return prefixo + "." + (labelContador++);
        }
    }

    record ResultadoLlvm(String valor, String tipo) {}

    private static ResultadoLlvm avaliarExpr(ExprAst expr, EmissorAotContexto ctx) {
        if (expr == null) return new ResultadoLlvm("0", "i64");

        switch (expr) {
            case ExprAst.LiteralInteiro li -> {
                return new ResultadoLlvm(li.valor().toString(), "i64");
            }
            case ExprAst.LiteralDecimal ld -> {
                return new ResultadoLlvm(ld.escalado().toString(), "i128");
            }
            case ExprAst.LiteralLogico ll -> {
                return new ResultadoLlvm(ll.valor() ? "1" : "0", "i1");
            }
            case ExprAst.LiteralTexto lt -> {
                String gVar = ctx.mapaStringGlobal.get(lt.valor());
                return new ResultadoLlvm(gVar != null ? gVar : "null", "ptr");
            }
            case ExprAst.AcessoCampo ac -> {
                if (ac.caminho().size() == 1) {
                    String nome = ac.caminho().getFirst();
                    EmissorAotContexto.VariavelLocal var = ctx.variaveis.get(nome);
                    if (var != null) {
                        String reg = ctx.novoReg();
                        ctx.sb.append("  ").append(reg).append(" = load ").append(var.tipoLlvm())
                              .append(", ptr ").append(var.ptrLlvm()).append("\n");
                        return new ResultadoLlvm(reg, var.tipoLlvm());
                    }
                    return new ResultadoLlvm("%" + nome, "i64");
                }
                return new ResultadoLlvm("0", "i64");
            }
            case ExprAst.OpUnaria ou -> {
                ResultadoLlvm opVal = avaliarExpr(ou.operando(), ctx);
                if (ou.operador().equals("-")) {
                    String reg = ctx.novoReg();
                    ctx.sb.append("  ").append(reg).append(" = sub ").append(opVal.tipo())
                          .append(" 0, ").append(opVal.valor()).append("\n");
                    return new ResultadoLlvm(reg, opVal.tipo());
                } else if (ou.operador().equalsIgnoreCase("NAO") || ou.operador().equals("!")) {
                    String reg = ctx.novoReg();
                    ctx.sb.append("  ").append(reg).append(" = xor i1 ").append(opVal.valor()).append(", 1\n");
                    return new ResultadoLlvm(reg, "i1");
                }
                return opVal;
            }
            case ExprAst.OpBinaria ob -> {
                ResultadoLlvm esq = avaliarExpr(ob.esquerda(), ctx);
                ResultadoLlvm dir = avaliarExpr(ob.direita(), ctx);
                String op = ob.operador();

                if (op.equalsIgnoreCase("E") || op.equalsIgnoreCase("and")) {
                    String reg = ctx.novoReg();
                    ctx.sb.append("  ").append(reg).append(" = and i1 ").append(esq.valor()).append(", ").append(dir.valor()).append("\n");
                    return new ResultadoLlvm(reg, "i1");
                }
                if (op.equalsIgnoreCase("OU") || op.equalsIgnoreCase("or")) {
                    String reg = ctx.novoReg();
                    ctx.sb.append("  ").append(reg).append(" = or i1 ").append(esq.valor()).append(", ").append(dir.valor()).append("\n");
                    return new ResultadoLlvm(reg, "i1");
                }

                String tipoComum = esq.tipo();
                String valEsq = esq.valor();
                String valDir = dir.valor();
                if (esq.tipo().equals("i32") && dir.tipo().equals("i64")) {
                    String r = ctx.novoReg();
                    ctx.sb.append("  ").append(r).append(" = sext i32 ").append(valEsq).append(" to i64\n");
                    valEsq = r;
                    tipoComum = "i64";
                } else if (esq.tipo().equals("i64") && dir.tipo().equals("i32")) {
                    String r = ctx.novoReg();
                    ctx.sb.append("  ").append(r).append(" = sext i32 ").append(valDir).append(" to i64\n");
                    valDir = r;
                    tipoComum = "i64";
                }

                String icmp = switch (op) {
                    case ">" -> "icmp sgt";
                    case "<" -> "icmp slt";
                    case ">=" -> "icmp sge";
                    case "<=" -> "icmp sle";
                    case "==", "=" -> "icmp eq";
                    case "!=", "<>" -> "icmp ne";
                    default -> null;
                };
                if (icmp != null) {
                    String reg = ctx.novoReg();
                    ctx.sb.append("  ").append(reg).append(" = ").append(icmp).append(" ").append(tipoComum)
                          .append(" ").append(valEsq).append(", ").append(valDir).append("\n");
                    return new ResultadoLlvm(reg, "i1");
                }

                String arith = switch (op) {
                    case "+" -> "add";
                    case "-" -> "sub";
                    case "*" -> "mul";
                    case "/" -> "sdiv";
                    default -> "add";
                };
                String reg = ctx.novoReg();
                ctx.sb.append("  ").append(reg).append(" = ").append(arith).append(" ").append(tipoComum)
                      .append(" ").append(valEsq).append(", ").append(valDir).append("\n");
                return new ResultadoLlvm(reg, tipoComum);
            }
            case ExprAst.Chamada ch -> {
                String nomeFn = String.join(".", ch.caminho());
                String llvmFn = nomeFn.equals("main") ? "thz_fn_main" : nomeFn;
                String tipoRet = "i64";
                if (ctx.ast != null && ctx.ast.funcoes() != null) {
                    for (FuncaoAst f : ctx.ast.funcoes()) {
                        if (f.nome().equals(nomeFn)) {
                            tipoRet = mapearTipoLlvm(f.tipoRetorno());
                            break;
                        }
                    }
                }
                StringBuilder callArgs = new StringBuilder();
                if (ch.argumentos() != null) {
                    for (int i = 0; i < ch.argumentos().size(); i++) {
                        if (i > 0) callArgs.append(", ");
                        ResultadoLlvm argRes = avaliarExpr(ch.argumentos().get(i), ctx);
                        callArgs.append(argRes.tipo()).append(" ").append(argRes.valor());
                    }
                }
                String reg = ctx.novoReg();
                ctx.sb.append("  ").append(reg).append(" = call ").append(tipoRet).append(" @").append(llvmFn)
                      .append("(").append(callArgs).append(")\n");
                return new ResultadoLlvm(reg, tipoRet);
            }
            default -> {
                return new ResultadoLlvm("0", "i64");
            }
        }
    }

    private static boolean emitirComandos(EmissorAotContexto ctx, List<ComandoAst> comandos) {
        if (comandos == null) return false;
        boolean terminou = false;
        for (ComandoAst c : comandos) {
            if (terminou) break;
            terminou = emitirComando(ctx, c);
        }
        return terminou;
    }

    private static boolean emitirComando(EmissorAotContexto ctx, ComandoAst c) {
        switch (c) {
            case ComandoAst.DeclVariavel dv -> {
                String tipo = dv.tipoDado() != null ? mapearTipoLlvm(dv.tipoDado()) : null;
                ResultadoLlvm init = null;
                if (dv.inicializacao() != null) {
                    init = avaliarExpr(dv.inicializacao(), ctx);
                    if (tipo == null) {
                        tipo = init.tipo();
                    }
                } else if (tipo == null) {
                    tipo = "i64";
                }
                String ptrVar = "%var." + dv.nome() + "." + (ctx.regContador++);
                ctx.sb.append("  ").append(ptrVar).append(" = alloca ").append(tipo).append("\n");
                ctx.variaveis.put(dv.nome(), new EmissorAotContexto.VariavelLocal(ptrVar, tipo));
                if (init != null) {
                    String valInit = init.valor();
                    if (!init.tipo().equals(tipo) && init.tipo().equals("i32") && tipo.equals("i64")) {
                        String regConv = ctx.novoReg();
                        ctx.sb.append("  ").append(regConv).append(" = sext i32 ").append(valInit).append(" to i64\n");
                        valInit = regConv;
                    }
                    ctx.sb.append("  store ").append(tipo).append(" ").append(valInit).append(", ptr ").append(ptrVar).append("\n");
                }
                return false;
            }
            case ComandoAst.Atribuicao at -> {
                if (at.alvo().size() == 1) {
                    String nome = at.alvo().getFirst();
                    EmissorAotContexto.VariavelLocal var = ctx.variaveis.get(nome);
                    if (var != null) {
                        ResultadoLlvm val = avaliarExpr(at.expressao(), ctx);
                        String valFinal = val.valor();
                        if (!val.tipo().equals(var.tipoLlvm()) && val.tipo().equals("i32") && var.tipoLlvm().equals("i64")) {
                            String regConv = ctx.novoReg();
                            ctx.sb.append("  ").append(regConv).append(" = sext i32 ").append(valFinal).append(" to i64\n");
                            valFinal = regConv;
                        }
                        ctx.sb.append("  store ").append(var.tipoLlvm()).append(" ").append(valFinal).append(", ptr ").append(var.ptrLlvm()).append("\n");
                    }
                }
                return false;
            }
            case ComandoAst.Exiba ex -> {
                String text = ThzParser.textoCanonicoDe(ex.expressao());
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    String raw = text.substring(1, text.length() - 1);
                    String gVar = ctx.mapaStringGlobal.get(raw);
                    if (gVar != null) {
                        ctx.sb.append("  call void @thz_exiba_str(ptr ").append(gVar).append(")\n");
                    }
                    return false;
                }
                if (ex.expressao() instanceof ExprAst.OpBinaria ob && ob.operador().equals("+")) {
                    emitirExibaConcatenado(ctx, ob);
                    return false;
                }
                ResultadoLlvm res = avaliarExpr(ex.expressao(), ctx);
                switch (res.tipo()) {
                    case "i64" -> ctx.sb.append("  call void @thz_exiba_i64(i64 ").append(res.valor()).append(")\n");
                    case "i32" -> ctx.sb.append("  call void @thz_exiba_i32(i32 ").append(res.valor()).append(")\n");
                    case "i1" -> ctx.sb.append("  call void @thz_exiba_bool(i1 ").append(res.valor()).append(")\n");
                    case "i128" -> ctx.sb.append("  call void @thz_exiba_i128(i128 ").append(res.valor()).append(", i32 2)\n");
                    case "ptr" -> ctx.sb.append("  call void @thz_exiba_str(ptr ").append(res.valor()).append(")\n");
                    default -> ctx.sb.append("  call void @thz_exiba_i64(i64 ").append(res.valor()).append(")\n");
                }
                return false;
            }
            case ComandoAst.Se s -> {
                ResultadoLlvm cond = avaliarExpr(s.condicao(), ctx);
                String lblThen = ctx.novoLabel("se.entao");
                String lblElse = ctx.novoLabel("se.senao");
                String lblMerge = ctx.novoLabel("se.fim");

                boolean temElse = s.senao() != null && !s.senao().isEmpty();
                ctx.sb.append("  br i1 ").append(cond.valor()).append(", label %").append(lblThen)
                      .append(", label %").append(temElse ? lblElse : lblMerge).append("\n");

                ctx.sb.append(lblThen).append(":\n");
                boolean thenTerminou = emitirComandos(ctx, s.entao());
                if (!thenTerminou) {
                    ctx.sb.append("  br label %").append(lblMerge).append("\n");
                }

                boolean elseTerminou = false;
                if (temElse) {
                    ctx.sb.append(lblElse).append(":\n");
                    elseTerminou = emitirComandos(ctx, s.senao());
                    if (!elseTerminou) {
                        ctx.sb.append("  br label %").append(lblMerge).append("\n");
                    }
                }

                if (!thenTerminou || (temElse && !elseTerminou) || !temElse) {
                    ctx.sb.append(lblMerge).append(":\n");
                    return false;
                }
                return true;
            }
            case ComandoAst.Enquanto e -> {
                String lblCond = ctx.novoLabel("enq.cond");
                String lblCorpo = ctx.novoLabel("enq.corpo");
                String lblFim = ctx.novoLabel("enq.fim");

                ctx.sb.append("  br label %").append(lblCond).append("\n");
                ctx.sb.append(lblCond).append(":\n");
                ResultadoLlvm cond = avaliarExpr(e.condicao(), ctx);
                ctx.sb.append("  br i1 ").append(cond.valor()).append(", label %").append(lblCorpo).append(", label %").append(lblFim).append("\n");

                ctx.sb.append(lblCorpo).append(":\n");
                boolean corpoTerminou = emitirComandos(ctx, e.corpo());
                if (!corpoTerminou) {
                    ctx.sb.append("  br label %").append(lblCond).append("\n");
                }

                ctx.sb.append(lblFim).append(":\n");
                return false;
            }
            case ComandoAst.Para p -> {
                String ptrVar = "%var." + p.variavel() + "." + (ctx.regContador++);
                ctx.sb.append("  ").append(ptrVar).append(" = alloca i64\n");
                ResultadoLlvm ini = avaliarExpr(p.inicio(), ctx);
                ctx.sb.append("  store i64 ").append(ini.valor()).append(", ptr ").append(ptrVar).append("\n");
                ctx.variaveis.put(p.variavel(), new EmissorAotContexto.VariavelLocal(ptrVar, "i64"));

                String lblCond = ctx.novoLabel("para.cond");
                String lblCorpo = ctx.novoLabel("para.corpo");
                String lblFim = ctx.novoLabel("para.fim");

                ctx.sb.append("  br label %").append(lblCond).append("\n");
                ctx.sb.append(lblCond).append(":\n");
                String rAtual = ctx.novoReg();
                ctx.sb.append("  ").append(rAtual).append(" = load i64, ptr ").append(ptrVar).append("\n");
                ResultadoLlvm fim = avaliarExpr(p.fim(), ctx);
                String rCmp = ctx.novoReg();
                ctx.sb.append("  ").append(rCmp).append(" = icmp sle i64 ").append(rAtual).append(", ").append(fim.valor()).append("\n");
                ctx.sb.append("  br i1 ").append(rCmp).append(", label %").append(lblCorpo).append(", label %").append(lblFim).append("\n");

                ctx.sb.append(lblCorpo).append(":\n");
                boolean corpoTerminou = emitirComandos(ctx, p.corpo());
                if (!corpoTerminou) {
                    ResultadoLlvm passo = p.passo() != null ? avaliarExpr(p.passo(), ctx) : new ResultadoLlvm("1", "i64");
                    String rCarregado = ctx.novoReg();
                    ctx.sb.append("  ").append(rCarregado).append(" = load i64, ptr ").append(ptrVar).append("\n");
                    String rProx = ctx.novoReg();
                    ctx.sb.append("  ").append(rProx).append(" = add i64 ").append(rCarregado).append(", ").append(passo.valor()).append("\n");
                    ctx.sb.append("  store i64 ").append(rProx).append(", ptr ").append(ptrVar).append("\n");
                    ctx.sb.append("  br label %").append(lblCond).append("\n");
                }

                ctx.sb.append(lblFim).append(":\n");
                return false;
            }
            case ComandoAst.Retorne r -> {
                ResultadoLlvm res = avaliarExpr(r.expressao(), ctx);
                ctx.sb.append("  ret ").append(res.tipo()).append(" ").append(res.valor()).append("\n");
                return true;
            }
            case ComandoAst.Chamada ch -> {
                String fn = ThzParser.textoCanonicoDe(ch.expressao());
                if (fn.contains("(")) {
                    fn = fn.substring(0, fn.indexOf('('));
                }
                ctx.sb.append("  call void @").append(fn).append("()\n");
                return false;
            }
            default -> {
                ctx.sb.append("  ; AVISO AOT: comando ").append(c.getClass().getSimpleName())
                      .append(" ignorado pelo emissor AOT (lowering de semântica não implementado no backend LLVM)\n");
                return false;
            }
        }
    }

    private static void emitirExibaConcatenado(EmissorAotContexto ctx, ExprAst.OpBinaria soma) {
        List<ExprAst> partes = new ArrayList<>();
        coletarPartesSoma(soma, partes);
        for (ExprAst parte : partes) {
            String text = ThzParser.textoCanonicoDe(parte);
            if (text.startsWith("\"") && text.endsWith("\"")) {
                String raw = text.substring(1, text.length() - 1);
                String gVar = ctx.mapaStringGlobal.get(raw);
                if (gVar != null) {
                    ctx.sb.append("  call void @thz_exiba_str(ptr ").append(gVar).append(")\n");
                }
            } else {
                ResultadoLlvm res = avaliarExpr(parte, ctx);
                switch (res.tipo()) {
                    case "i64" -> ctx.sb.append("  call void @thz_exiba_i64(i64 ").append(res.valor()).append(")\n");
                    case "i32" -> ctx.sb.append("  call void @thz_exiba_i32(i32 ").append(res.valor()).append(")\n");
                    case "i1" -> ctx.sb.append("  call void @thz_exiba_bool(i1 ").append(res.valor()).append(")\n");
                    case "i128" -> ctx.sb.append("  call void @thz_exiba_i128(i128 ").append(res.valor()).append(", i32 2)\n");
                    case "ptr" -> ctx.sb.append("  call void @thz_exiba_str(ptr ").append(res.valor()).append(")\n");
                    default -> ctx.sb.append("  call void @thz_exiba_i64(i64 ").append(res.valor()).append(")\n");
                }
            }
        }
    }

    private static void coletarPartesSoma(ExprAst expr, List<ExprAst> partes) {
        if (expr instanceof ExprAst.OpBinaria b && b.operador().equals("+")) {
            coletarPartesSoma(b.esquerda(), partes);
            coletarPartesSoma(b.direita(), partes);
        } else {
            partes.add(expr);
        }
    }

    private static String mapearTipoLlvm(String tipo) {
        if (tipo == null) return "i64";
        String t = tipo.toUpperCase();
        if (t.contains("DECIMAL") || t.contains("MONETARIO")) return "i128";
        if (t.contains("INTEIRO64") || t.equals("INTEIRO")) return "i64";
        if (t.contains("INTEIRO32") || t.contains("NATURAL32")) return "i32";
        if (t.contains("TEXTO") || t.contains("UUID")) return "ptr";
        if (t.contains("LOGICO")) return "i1";
        if (t.contains("FATIA")) return "ptr";
        return "ptr";
    }

    // private static String valorRetornoLlvm(FuncaoAst funcao) {
    //     if (funcao.corpo() != null) {
    //         for (ComandoAst c : funcao.corpo()) {
    //             if (c instanceof ComandoAst.Retorne r) {
    //                 String valor = valorExpressaoLlvm(r.expressao(), funcao);
    //                 if (valor != null) return valor;
    //             }
    //         }
    //     }
    //     return "0";
    // }

    private static void emitirRetornoLlvm(StringBuilder sb, FuncaoAst funcao) {
        String tipo = mapearTipoLlvm(funcao.tipoRetorno());
        ExprAst expr = null;
        if (funcao.corpo() != null) {
            for (ComandoAst c : funcao.corpo()) if (c instanceof ComandoAst.Retorne r) { expr = r.expressao(); break; }
        }
        if (expr instanceof ExprAst.OpBinaria b && (tipo.equals("i32") || tipo.equals("i64") || tipo.equals("i128"))) {
            String esq = operandoLlvm(b.esquerda(), funcao), dir = operandoLlvm(b.direita(), funcao);
            String op = switch (b.operador()) { case "+" -> "add"; case "-" -> "sub"; case "*" -> "mul"; case "/" -> "sdiv"; default -> null; };
            if (esq != null && dir != null && op != null) {
                sb.append("  %ret = ").append(op).append(" ").append(tipo).append(" ").append(esq).append(", ").append(dir).append("\n");
                sb.append("  ret ").append(tipo).append(" %ret\n");
                return;
            }
        }
        sb.append("  ret ").append(tipo).append(" ").append(valorExpressaoLlvm(expr, funcao)).append("\n");
    }

    private static String operandoLlvm(ExprAst expr, FuncaoAst funcao) {
        if (expr instanceof ExprAst.LiteralInteiro i) return i.valor().toString();
        if (expr instanceof ExprAst.LiteralDecimal d) return d.escalado().toString();
        if (expr instanceof ExprAst.AcessoCampo a && a.caminho().size() == 1
                && funcao.parametros().stream().anyMatch(p -> p.nome().equals(a.caminho().getFirst()))) return "%" + a.caminho().getFirst();
        return null;
    }

    /** Avalia somente a sublinguagem constante, evitando emitir LLVM incorreto para efeitos/chamadas. */
    private static String avaliarConstanteLlvm(ExprAst expr) {
        if (expr instanceof ExprAst.LiteralInteiro i) return i.valor().toString();
        if (expr instanceof ExprAst.LiteralDecimal d) return d.escalado().toString();
        if (expr instanceof ExprAst.LiteralLogico b) return b.valor() ? "1" : "0";
        if (expr instanceof ExprAst.OpUnaria u) {
            String v = avaliarConstanteLlvm(u.operando());
            if (v != null && u.operador().equals("-")) return v.startsWith("-") ? v.substring(1) : "-" + v;
            return v;
        }
        if (expr instanceof ExprAst.OpBinaria b) {
            String a = avaliarConstanteLlvm(b.esquerda());
            String z = avaliarConstanteLlvm(b.direita());
            if (a == null || z == null) return null;
            try {
                java.math.BigInteger x = new java.math.BigInteger(a), y = new java.math.BigInteger(z);
                return switch (b.operador()) {
                    case "+" -> x.add(y).toString();
                    case "-" -> x.subtract(y).toString();
                    case "*" -> x.multiply(y).toString();
                    case "/" -> y.signum() == 0 ? null : x.divide(y).toString();
                    default -> null;
                };
            } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static String valorExpressaoLlvm(ExprAst expr, FuncaoAst funcao) {
        if (expr instanceof ExprAst.AcessoCampo a && a.caminho().size() == 1
                && funcao.parametros().stream().anyMatch(p -> p.nome().equals(a.caminho().getFirst()))) {
            return "%" + a.caminho().getFirst();
        }
        return avaliarConstanteLlvm(expr);
    }
}
