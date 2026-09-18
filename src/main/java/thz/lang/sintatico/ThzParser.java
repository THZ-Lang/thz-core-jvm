package thz.lang.sintatico;

import thz.lang.ast.*;
import thz.lang.lexico.DialetoLinguagem;
import thz.lang.lexico.PalavrasReservadas;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ThzParser {

    private int current = 0;
    private final List<Token> tokens;
    private final List<String> errosSintaticos = new ArrayList<>();

    public List<String> errosSintaticos() {
        return errosSintaticos;
    }

    public void sincronizar() {
        advance();
        while (!isAtEnd()) {
            TokenType t = peek().type();
            if (t == TokenType.FIM_PROGRAMA || t == TokenType.FIM_BIBLIOTECA || t == TokenType.FIM_TELA ||
                t == TokenType.FIM_PIPELINE || t == TokenType.FIM_EXTENSAO || t == TokenType.FIM_FERRAMENTA ||
                t == TokenType.FIM_TESTE || t == TokenType.FIM_ESTRUTURA || t == TokenType.FIM_REGRA_NEGOCIO ||
                t == TokenType.PROCEDIMENTO || t == TokenType.FUNCAO || t == TokenType.REGRA_NEGOCIO || t == TokenType.ESTRUTURA ||
                t == TokenType.ENUMERACAO || t == TokenType.PIPELINE_DADOS) {
                return;
            }
            advance();
        }
    }

    private static final Map<String, String> RELACIONAIS = Map.of(
            "=", "=",
            "==", "=",
            "<>", "<>",
            "!=", "<>",
            "<", "<",
            "<=", "<=",
            ">", ">",
            ">=", ">="
    );

    private final DialetoLinguagem dialeto;

    public ThzParser(List<Token> tokens, DialetoLinguagem dialeto) {
        this.tokens = tokens;
        this.dialeto = dialeto != null ? dialeto : DialetoLinguagem.PT_BR;
    }

    public ThzParser(List<Token> tokens) {
        this(tokens, DialetoLinguagem.PT_BR);
    }

    public ProgramaAst parse() {
        String versaoLinguagem = null;
        MetadadosArquiteturaAst metadados = null;
        List<ImportacaoAst> importacoes = new ArrayList<>();
        List<EstruturaAst> estruturas = new ArrayList<>();
        List<EnumeracaoAst> enumeracoes = new ArrayList<>();
        List<RegraNegocioAst> regras = new ArrayList<>();
        List<ProcedimentoAst> procedimentos = new ArrayList<>();
        List<FuncaoAst> funcoes = new ArrayList<>();
        List<String> blocosRust = new ArrayList<>();

        // Pragma opcional de compatibilidade: VERSAO_LINGUAGEM "2.4"
        if (match(TokenType.VERSAO_LINGUAGEM)) {
            versaoLinguagem = consume(TokenType.STRING_LITERAL, "Esperada a versão da linguagem entre aspas após 'VERSAO_LINGUAGEM'.").value();
        }

        TipoModulo tipoModulo;
        TokenType terminadorEsperado;

        if (match(TokenType.PROGRAMA)) {
            if (match(TokenType.VISUAL)) {
                tipoModulo = TipoModulo.PROGRAMA_VISUAL;
            } else if (match(TokenType.NEGOCIO)) {
                tipoModulo = TipoModulo.PROGRAMA_NEGOCIO;
            } else if (match(TokenType.ARQUITETURA)) {
                tipoModulo = TipoModulo.PROGRAMA_ARQUITETURA;
            } else {
                tipoModulo = TipoModulo.PROGRAMA;
            }
            terminadorEsperado = TokenType.FIM_PROGRAMA;
        } else if (match(TokenType.BIBLIOTECA)) {
            tipoModulo = TipoModulo.BIBLIOTECA;
            terminadorEsperado = TokenType.FIM_BIBLIOTECA;
        } else if (match(TokenType.EXTENSAO)) {
            tipoModulo = TipoModulo.EXTENSAO;
            terminadorEsperado = TokenType.FIM_EXTENSAO;
        } else if (match(TokenType.FERRAMENTA)) {
            tipoModulo = TipoModulo.FERRAMENTA;
            terminadorEsperado = TokenType.FIM_FERRAMENTA;
        } else if (match(TokenType.TESTE)) {
            tipoModulo = TipoModulo.TESTE;
            terminadorEsperado = TokenType.FIM_TESTE;
        } else if (match(TokenType.TELA)) {
            tipoModulo = TipoModulo.TELA;
            terminadorEsperado = TokenType.FIM_TELA;
        } else if (match(TokenType.PIPELINE_DADOS)) {
            tipoModulo = TipoModulo.PIPELINE_DADOS;
            terminadorEsperado = TokenType.FIM_PIPELINE;
        } else {
            Token token = peek();
            throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] Esperada declaração de módulo no início do arquivo ('PROGRAMA', 'PROGRAMA VISUAL', 'PROGRAMA NEGOCIO', 'PROGRAMA ARQUITETURA', 'PIPELINE_DADOS', 'BIBLIOTECA', 'EXTENSAO', 'FERRAMENTA', 'TESTE' ou 'TELA'). (Encontrado: '" + token.value() + "')");
        }

        String nome = consumeIdentificador("Esperado o nome do módulo " + tipoModulo.descricao() + ".").value();
        boolean usaChaves = match(TokenType.ABRE_CHAVE);

        while ((usaChaves ? !check(TokenType.FECHA_CHAVE) : !check(terminadorEsperado)) && !isAtEnd()) {
            if (match(TokenType.IMPORTAR)) {
                importacoes.add(parseImportacao());
            } else if (match(TokenType.METADADOS_ARQUITETURA)) {
                metadados = parseMetadados();
            } else if (match(TokenType.BLOCO_NATIVO_RUST)) {
                blocosRust.add(parseBlocoNativoRust());
            } else if (match(TokenType.ESTRUTURA)) {
                estruturas.add(parseEstrutura());
            } else if (match(TokenType.ENUMERACAO)) {
                enumeracoes.add(parseEnumeracao());
            } else if (match(TokenType.REGRA_NEGOCIO)) {
                regras.add(parseRegraNegocio());
            } else if (match(TokenType.PROCEDIMENTO)) {
                procedimentos.add(parseProcedimento());
            } else if (match(TokenType.FUNCAO)) {
                funcoes.add(parseFuncao());
            } else {
                erroDeclaracaoInvalida(tipoModulo);
            }
        }

        if (usaChaves) {
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o bloco " + tipoModulo.descricao() + ".");
        } else {
            consume(terminadorEsperado, "Esperado '" + tipoModulo.terminadorPadrao() + "' encerrando o bloco " + tipoModulo.descricao() + ".");
        }

        return new ProgramaAst(tipoModulo, nome, versaoLinguagem, importacoes, metadados, estruturas, enumeracoes, regras, procedimentos, dialeto, blocosRust, funcoes);
    }

    private String parseBlocoNativoRust() {
        String codigo = "";
        if (check(TokenType.STRING_LITERAL)) {
            codigo = advance().value();
        }
        consume(TokenType.FIM_BLOCO_NATIVO, "Esperado 'FIM_BLOCO_NATIVO' para encerrar o bloco de código Rust.");
        return codigo;
    }

    private ImportacaoAst parseImportacao() {
        Token inicio = previous();
        String modulo = consumeIdentificador("Esperado nome do módulo após 'IMPORTAR'.").value();
        String caminho = null;
        if (match(TokenType.DE)) {
            caminho = consume(TokenType.STRING_LITERAL, "Esperado caminho do arquivo entre aspas após 'DE'.").value();
        }
        return new ImportacaoAst(modulo, caminho, inicio.line(), inicio.column());
    }

    private void erroDeclaracaoInvalida(TipoModulo tipoModulo) {
        Token token = peek();
        throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] Declaração não reconhecida no nível do módulo. Esperados 'IMPORTAR', 'METADADOS_ARQUITETURA', 'ESTRUTURA', 'ENUMERACAO', 'REGRA_NEGOCIO', 'PROCEDIMENTO', 'FUNCAO' ou '" + tipoModulo.terminadorPadrao() + "'. (Encontrado: '" + token.value() + "')");
    }

    /* ============================================================
     * METADADOS
     * ============================================================ */

    private MetadadosArquiteturaAst parseMetadados() {
        String dominio = null;
        String subdominio = null;
        String camada = null;
        String versao = null;
        String autor = null;
        String sloLatencia = null;
        List<String> conformidade = new ArrayList<>();
        boolean comChave = match(TokenType.ABRE_CHAVE);

        while ((comChave ? !check(TokenType.FECHA_CHAVE) : !check(TokenType.FIM_METADADOS)) && !isAtEnd()) {
            String chave = consumeIdentificador("Esperada chave de metadado.").value().toUpperCase();
            consume(TokenType.DOIS_PONTOS, "Esperado ':' após identificador de metadado.");

            if ("CONFORMIDADE".equals(chave) || "COMPLIANCE".equals(chave)) {
                conformidade.add(consume(TokenType.STRING_LITERAL, "Esperada regra de conformidade.").value());
                while (match(TokenType.VIRGULA)) {
                    conformidade.add(consume(TokenType.STRING_LITERAL, "Esperado próximo valor.").value());
                }
            } else {
                String valor = consume(TokenType.STRING_LITERAL, "Esperado valor textual do metadado.").value();
                if ("DOMINIO".equals(chave) || "DOMAIN".equals(chave)) dominio = valor;
                if ("SUBDOMINIO".equals(chave) || "SUBDOMAIN".equals(chave)) subdominio = valor;
                if ("CAMADA".equals(chave) || "LAYER".equals(chave)) camada = valor;
                if ("VERSAO".equals(chave) || "VERSION".equals(chave)) versao = valor;
                if ("AUTOR".equals(chave) || "AUTHOR".equals(chave)) autor = valor;
                if ("SLO_LATENCIA_MAXIMA".equals(chave) || "MAX_LATENCY_SLO".equals(chave)) sloLatencia = valor;
            }
        }
        if (comChave) {
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o bloco de metadados.");
        } else {
            consume(TokenType.FIM_METADADOS, "Esperado 'FIM_METADADOS'.");
        }
        return new MetadadosArquiteturaAst(dominio, subdominio, camada, versao, autor, sloLatencia, conformidade);
    }

    /* ============================================================
     * ESTRUTURA
     * ============================================================ */

    private EstruturaAst parseEstrutura() {
        String nome = consumeIdentificador("Esperado nome da estrutura.").value();
        boolean layoutColunar = false;
        if (match(TokenType.LAYOUT_COLUNAR)) {
            layoutColunar = true;
        }
        boolean comChave = match(TokenType.ABRE_CHAVE);

        List<CampoEstruturaAst> campos = new ArrayList<>();
        List<InvarianteAst> invariantes = new ArrayList<>();
        while ((comChave ? !check(TokenType.FECHA_CHAVE) : !check(TokenType.FIM_ESTRUTURA)) && !isAtEnd()) {
            if (check(TokenType.INVARIANTE)) {
                invariantes.add(parseInvariante());
                continue;
            }
            String campoNome = consumeIdentificador("Esperado nome do campo.").value();
            consume(TokenType.DOIS_PONTOS, "Esperado ':' após o nome do campo.");
            campos.add(new CampoEstruturaAst(campoNome, parseTipoDado()));
            match(TokenType.VIRGULA);
        }

        if (comChave) {
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando a declaração da estrutura '" + nome + "'.");
        } else {
            consume(TokenType.FIM_ESTRUTURA, "Esperado 'FIM_ESTRUTURA' encerrando a declaração da estrutura '" + nome + "'.");
        }

        return new EstruturaAst(nome, layoutColunar, campos, invariantes);
    }

    /**
     * ENUMERACAO Nome
     *     MEMBRO_A
     * FIM_ENUMERACAO
     */
    private EnumeracaoAst parseEnumeracao() {
        String nome = consumeIdentificador("Esperado nome da enumeração.").value();
        boolean comChave = match(TokenType.ABRE_CHAVE);
        List<String> membros = new ArrayList<>();
        while ((comChave ? !check(TokenType.FECHA_CHAVE) : !check(TokenType.FIM_ENUMERACAO)) && !isAtEnd()) {
            membros.add(consumeIdentificador("Esperado membro da enumeração.").value());
        }
        if (comChave) {
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando a enumeração '" + nome + "'.");
        } else {
            consume(TokenType.FIM_ENUMERACAO, "Esperado 'FIM_ENUMERACAO' encerrando a enumeração '" + nome + "'.");
        }
        return new EnumeracaoAst(nome, membros);
    }

    /** INVARIANTE <expressão> — cláusula viva dentro de ESTRUTURA. */
    private InvarianteAst parseInvariante() {
        Token inicio = consume(TokenType.INVARIANTE, "Esperado 'INVARIANTE'.");
        ExprAst expressao = parseExpressao();
        return new InvarianteAst(expressao, textoCanonicoDe(expressao), inicio.line(), inicio.column());
    }

    /**
     * Tipo de dado canônico: IDENTIFICADOR [(args)] [[T]]
     * Preserva verbatim (ex.: 'DECIMAL(12,4)', 'FATIA[ItemFatura]').
     */
    private String parseTipoDado() {
        String tipoRaw = consumeIdentificador("Esperado tipo do dado.").value();
        String tipo = switch (tipoRaw.toUpperCase()) {
            case "TEXT", "STRING", "STR" -> "TEXTO";
            case "INTEGER", "INT", "I64" -> "INTEIRO";
            case "INT32", "I32" -> "INTEIRO32";
            case "DEC", "DECIMAL" -> "DECIMAL";
            case "MONETARY", "MONEY" -> "MONETARIO";
            case "BOOLEAN", "BOOLEANO", "BOOL" -> "LOGICO";
            case "DATE" -> "DATA";
            case "DATETIME" -> "DATA_HORA";
            case "RESULT" -> "RESULTADO";
            case "LIST", "SLICE" -> "FATIA";
            case "MAP" -> "MAPA";
            default -> tipoRaw;
        };

        if (match(TokenType.ABRE_PARENTESE)) {
            StringBuilder args = new StringBuilder();
            while (!check(TokenType.FECHA_PARENTESE) && !isAtEnd()) {
                args.append(peek().value());
                advance();
            }
            consume(TokenType.FECHA_PARENTESE, "Esperado ')' após parâmetros do tipo.");
            tipo += "(" + args + ")";
        }
        if (match(TokenType.ABRE_COLCHETE)) {
            if ("RESULTADO".equals(tipo) || "RESULT".equals(tipo)) {
                // RESULTADO[T, E]: canal de sucesso e canal de erro.
                String sucesso = parseTipoDado();
                consume(TokenType.VIRGULA, "Esperado ',' separando os canais de RESULTADO[T, E].");
                String erro = parseTipoDado();
                consume(TokenType.FECHA_COLCHETE, "Esperado ']' após canais de RESULTADO.");
                return "RESULTADO[" + sucesso + ", " + erro + "]";
            }
            tipo += "[" + parseTipoDado() + "]";
            consume(TokenType.FECHA_COLCHETE, "Esperado ']' após tipo interno.");
        }
        return tipo;
    }

    /* ============================================================
     * REGRA DE NEGÓCIO
     * ============================================================ */

    private RegraNegocioAst parseRegraNegocio() {
        String nome = consumeIdentificador("Esperado nome da regra.").value();
        String identificador = null;
        String rastreioRequisito = null;
        String descricao = null;
        boolean idempotente = false;
        String chaveIdempotencia = null;
        List<ClausulaContratoAst> clausulasEntrada = new ArrayList<>();
        List<ClausulaContratoAst> clausulasSaida = new ArrayList<>();
        List<OperacaoAst> operacoes = new ArrayList<>();
        boolean comChave = match(TokenType.ABRE_CHAVE);

        while ((comChave ? !check(TokenType.FECHA_CHAVE) : !check(TokenType.FIM_REGRA_NEGOCIO)) && !isAtEnd()) {
            Token p = peek();
            String pVal = p.value().toUpperCase();
            if (("IDENTIFICADOR_REGRA".equals(pVal) || "RULE_ID".equals(pVal)) && p.type() == TokenType.IDENTIFICADOR) {
                advance();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'IDENTIFICADOR_REGRA'.");
                identificador = consume(TokenType.STRING_LITERAL, "Esperado ID da regra.").value();
            } else if (("RASTREIO_REQUISITO".equals(pVal) || "REQUIREMENT_TRACE".equals(pVal)) && p.type() == TokenType.IDENTIFICADOR) {
                advance();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'RASTREIO_REQUISITO'.");
                rastreioRequisito = consume(TokenType.STRING_LITERAL, "Esperado requisito de rastreio.").value();
            } else if (("DESCRICAO".equals(pVal) || "DESCRIPTION".equals(pVal)) && p.type() == TokenType.IDENTIFICADOR) {
                advance();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'DESCRICAO'.");
                descricao = consume(TokenType.STRING_LITERAL, "Esperada descrição.").value();
            } else if (match(TokenType.IDEMPOTENTE)) {
                idempotente = true;
                if (match(TokenType.DOIS_PONTOS)) {
                    if (match(TokenType.VERDADEIRO) || match(TokenType.STRING_LITERAL)) {
                        idempotente = true;
                    } else if (match(TokenType.FALSO)) {
                        idempotente = false;
                    }
                }
            } else if ("CHAVE_IDEMPOTENCIA".equals(pVal) || "IDEMPOTENCY_KEY".equals(pVal) || p.type() == TokenType.CHAVE_IDEMPOTENCIA) {
                advance();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'CHAVE_IDEMPOTENCIA'.");
                chaveIdempotencia = consume(TokenType.STRING_LITERAL, "Esperada chave de idempotência.").value();
            } else if (match(TokenType.CONTRATO_ENTRADA)) {
                while (!check(TokenType.FIM_CONTRATO_ENTRADA) && !isAtEnd()) {
                    clausulasEntrada.add(parseClausulaContrato(TokenType.EXIGE));
                }
                consume(TokenType.FIM_CONTRATO_ENTRADA, "Esperado 'FIM_CONTRATO_ENTRADA' encerrando o bloco de pré-condições.");
            } else if (match(TokenType.CONTRATO_SAIDA)) {
                while (!check(TokenType.FIM_CONTRATO_SAIDA) && !isAtEnd()) {
                    clausulasSaida.add(parseClausulaContrato(TokenType.GARANTE));
                }
                consume(TokenType.FIM_CONTRATO_SAIDA, "Esperado 'FIM_CONTRATO_SAIDA' encerrando o bloco de pós-condições.");
            } else if (check(TokenType.EXIGE)) {
                clausulasEntrada.add(parseClausulaContrato(TokenType.EXIGE));
            } else if (check(TokenType.GARANTE)) {
                clausulasSaida.add(parseClausulaContrato(TokenType.GARANTE));
            } else if (match(TokenType.OPERACAO) || match(TokenType.FUNCAO)) {
                operacoes.add(parseOperacao());
            } else {
                Token token = peek();
                throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] Elemento não reconhecido dentro de 'REGRA_NEGOCIO'. Esperados 'IDENTIFICADOR_REGRA', 'RASTREIO_REQUISITO', 'DESCRICAO', 'IDEMPOTENTE', 'CHAVE_IDEMPOTENCIA', 'CONTRATO_ENTRADA', 'CONTRATO_SAIDA', 'OPERACAO', 'FUNCAO' ou " + (comChave ? "'}'" : "'FIM_REGRA_NEGOCIO'") + ". (Encontrado: '" + token.value() + "')");
            }
        }

        if (comChave) {
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando a regra de negócio.");
        } else {
            consume(TokenType.FIM_REGRA_NEGOCIO, "Esperado 'FIM_REGRA_NEGOCIO'.");
        }
        return new RegraNegocioAst(nome, identificador, rastreioRequisito, descricao, clausulasEntrada, clausulasSaida, operacoes, idempotente, chaveIdempotencia);
    }

    private ClausulaContratoAst parseClausulaContrato(TokenType tipo) {
        Token inicio = consume(tipo, "Esperada cláusula '" + (tipo == TokenType.EXIGE ? "EXIGE" : "GARANTE") + "' dentro do bloco de contrato.");
        ExprAst expressao = parseExpressao();
        return new ClausulaContratoAst(inicio.value(), expressao, textoCanonicoDe(expressao), inicio.line(), inicio.column());
    }

    private OperacaoAst parseOperacao() {
        boolean idempotente = false;
        String chaveIdempotencia = null;
        if (match(TokenType.IDEMPOTENTE)) {
            idempotente = true;
        }

        String nome = consumeIdentificador("Esperado nome da operação.").value();
        List<ParametroOperacaoAst> parametros = new ArrayList<>();
        String tipoRetorno;

        consume(TokenType.ABRE_PARENTESE, "Esperado '(' na assinatura da operação.");
        if (!check(TokenType.FECHA_PARENTESE)) {
            do {
                String paramNome = consumeIdentificador("Esperado nome do parâmetro.").value();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do parâmetro.");
                String paramTipo = parseTipoDado();
                parametros.add(new ParametroOperacaoAst(paramNome, paramTipo));
            } while (match(TokenType.VIRGULA));
        }
        consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando os parâmetros.");
        consume(TokenType.DOIS_PONTOS, "Esperado ':' antes do tipo de retorno.");
        tipoRetorno = parseTipoDado();

        List<ComandoAst> corpo = new ArrayList<>();
        if (check(TokenType.OPERADOR_RELACIONAL) && "=".equals(peek().value())) {
            advance();
            ExprAst expressao = parseExpressao();
            corpo.add(new ComandoAst.Retorne(expressao, expressao.linha(), expressao.coluna()));
        } else if (match(TokenType.ABRE_CHAVE)) {
            corpo = parseBlocoComandos(TokenType.FECHA_CHAVE);
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o corpo da operação.");
        } else if (match(TokenType.INICIO)) {
            corpo = parseBlocoComandos(TokenType.FIM);
            consume(TokenType.FIM, "Esperado 'FIM' encerrando o corpo da operação.");
        }

        return new OperacaoAst(nome, parametros, tipoRetorno, corpo, idempotente, chaveIdempotencia);
    }

    private ProcedimentoAst parseProcedimento() {
        boolean idempotente = false;
        String chaveIdempotencia = null;
        if (match(TokenType.IDEMPOTENTE)) {
            idempotente = true;
        }

        String nome = consumeIdentificador("Esperado nome do procedimento.").value();
        List<ParametroOperacaoAst> parametros = new ArrayList<>();
        consume(TokenType.ABRE_PARENTESE, "Esperado '(' na assinatura do procedimento.");
        if (!check(TokenType.FECHA_PARENTESE)) {
            do {
                String paramNome = consumeIdentificador("Esperado nome do parâmetro.").value();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do parâmetro.");
                String paramTipo = parseTipoDado();
                parametros.add(new ParametroOperacaoAst(paramNome, paramTipo));
            } while (match(TokenType.VIRGULA));
        }
        consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando os parâmetros.");
        // Corpo opcional INICIO ... FIM ou { ... }
        List<ComandoAst> corpo = new ArrayList<>();
        if (match(TokenType.ABRE_CHAVE)) {
            corpo = parseBlocoComandos(TokenType.FECHA_CHAVE);
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o corpo do procedimento.");
        } else if (match(TokenType.INICIO)) {
            corpo = parseBlocoComandos(TokenType.FIM);
            consume(TokenType.FIM, "Esperado 'FIM' encerrando o corpo do procedimento.");
        }
        return new ProcedimentoAst(nome, parametros, corpo, idempotente, chaveIdempotencia);
    }

    private FuncaoAst parseFuncao() {
        String nome = consumeIdentificador("Esperado nome da função.").value();
        List<ParametroOperacaoAst> parametros = new ArrayList<>();
        consume(TokenType.ABRE_PARENTESE, "Esperado '(' na assinatura da função.");
        if (!check(TokenType.FECHA_PARENTESE)) {
            do {
                String paramNome = consumeIdentificador("Esperado nome do parâmetro.").value();
                consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do parâmetro.");
                parametros.add(new ParametroOperacaoAst(paramNome, parseTipoDado()));
            } while (match(TokenType.VIRGULA));
        }
        consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando os parâmetros.");
        consume(TokenType.DOIS_PONTOS, "Esperado ':' antes do tipo de retorno da função.");
        String tipoRetorno = parseTipoDado();
        if (check(TokenType.OPERADOR_RELACIONAL) && "=".equals(peek().value())) {
            advance();
            ExprAst expressao = parseExpressao();
            return new FuncaoAst(nome, parametros, tipoRetorno,
                    List.of(new ComandoAst.Retorne(expressao, expressao.linha(), expressao.coluna())));
        }
        List<ComandoAst> corpo;
        if (match(TokenType.ABRE_CHAVE)) {
            corpo = parseBlocoComandos(TokenType.FECHA_CHAVE);
            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o corpo da função.");
        } else {
            corpo = parseBlocoComandos(TokenType.FIM_FUNCAO);
            consume(TokenType.FIM_FUNCAO, "Esperado 'FIM_FUNCAO' encerrando o corpo da função.");
        }
        return new FuncaoAst(nome, parametros, tipoRetorno, corpo);
    }


    /* ============================================================
     * COMANDOS
     * ============================================================ */

    /** Consome comandos até qualquer um dos terminadores informados (sem consumi-los). */
    private List<ComandoAst> parseBlocoComandos(TokenType... terminadores) {
        List<ComandoAst> comandos = new ArrayList<>();
        while (!isAtEnd() && !containsCheck(terminadores)) {
            comandos.add(parseComando());
        }
        return comandos;
    }

    private boolean containsCheck(TokenType[] terminadores) {
        for (TokenType t : terminadores) {
            if (check(t)) return true;
        }
        return false;
    }

    private ComandoAst parseComando() {
        Token token = peek();

        switch (token.type()) {
            case VARIAVEL: {
                advance();
                String nome = consumeIdentificador("Esperado nome da variável.").value();
                String tipoDado = null;
                if (match(TokenType.DOIS_PONTOS)) {
                    tipoDado = parseTipoDado();
                }
                if (match(TokenType.SETA_ATRIBUICAO)) {
                    // aceita <- ou :=
                } else if (check(TokenType.OPERADOR_RELACIONAL) && "=".equals(peek().value())) {
                    advance();
                } else {
                    throw new RuntimeException("[Erro Sintático][Linha " + peek().line() + ":" + peek().column() + "] Esperado '<-', ':=' ou '=' na inicialização da variável '" + nome + "'.");
                }
                ExprAst inicializacao = parseExpressao();
                return new ComandoAst.DeclVariavel(nome, tipoDado, inicializacao, token.line(), token.column());
            }

            case CASO_RESULTADO: {
                advance();
                ExprAst alvo = parseExpressao();
                String varSucesso = null;
                List<ComandoAst> corpoSucesso = new ArrayList<>();
                String varErro = null;
                List<ComandoAst> corpoErro = new ArrayList<>();

                while (!check(TokenType.FIM_CASO) && !isAtEnd()) {
                    match(TokenType.CASO);
                    if (match(TokenType.SUCESSO)) {
                        consume(TokenType.ABRE_PARENTESE, "Esperado '(' após 'SUCESSO'.");
                        varSucesso = consumeIdentificador("Esperado identificador da variável de sucesso.").value();
                        consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando parâmetro de 'SUCESSO'.");
                        consume(TokenType.SETA_CASO, "Esperado '->' após SUCESSO(...).");
                        if (match(TokenType.INICIO)) {
                            corpoSucesso = parseBlocoComandos(TokenType.FIM);
                            consume(TokenType.FIM, "Esperado 'FIM' encerrando o bloco de SUCESSO.");
                        } else {
                            corpoSucesso = List.of(parseComando());
                        }
                    } else if (match(TokenType.ERRO)) {
                        consume(TokenType.ABRE_PARENTESE, "Esperado '(' após 'ERRO'.");
                        varErro = consumeIdentificador("Esperado identificador da variável de erro.").value();
                        consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando parâmetro de 'ERRO'.");
                        consume(TokenType.SETA_CASO, "Esperado '->' após ERRO(...).");
                        if (match(TokenType.INICIO)) {
                            corpoErro = parseBlocoComandos(TokenType.FIM);
                            consume(TokenType.FIM, "Esperado 'FIM' encerrando o bloco de ERRO.");
                        } else {
                            corpoErro = List.of(parseComando());
                        }
                    } else {
                        Token t = peek();
                        throw new RuntimeException("[Erro Sintático][Linha " + t.line() + ":" + t.column() + "] Esperado 'CASO SUCESSO', 'CASO FALHA' ou 'FIM_ESCOLHA' dentro de 'ESCOLHA'. (Encontrado: '" + t.value() + "')");
                    }
                }
                consume(TokenType.FIM_CASO, "Esperado 'FIM_CASO' encerrando 'CASO_RESULTADO'.");
                return new ComandoAst.CasoResultado(alvo, varSucesso, corpoSucesso, varErro, corpoErro, token.line(), token.column());
            }

            case TENTE: {
                advance();
                List<ComandoAst> corpoTente = parseBlocoComandos(TokenType.CAPTURE, TokenType.FIM_TENTE);
                consume(TokenType.CAPTURE, "Esperado 'CAPTURE' no bloco 'TENTE'.");
                String tipoCaptura = consumeIdentificador("Esperado tipo de erro após 'CAPTURE'.").value();
                List<ComandoAst> corpoCapture = parseBlocoComandos(TokenType.FIM_TENTE);
                consume(TokenType.FIM_TENTE, "Esperado 'FIM_TENTE' encerrando o bloco.");
                return new ComandoAst.Tente(corpoTente, tipoCaptura, corpoCapture, token.line(), token.column());
            }

            case SE: {
                advance();
                ExprAst condicao = parseExpressao();
                boolean comChave = match(TokenType.ABRE_CHAVE);
                if (!comChave) {
                    match(TokenType.ENTAO);
                }
                List<ComandoAst> entao;
                List<ComandoAst> senao = new ArrayList<>();
                if (comChave) {
                    entao = parseBlocoComandos(TokenType.FECHA_CHAVE);
                    consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o bloco 'se'.");
                    if (match(TokenType.SENAO)) {
                        if (match(TokenType.ABRE_CHAVE)) {
                            senao = parseBlocoComandos(TokenType.FECHA_CHAVE);
                            consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o bloco 'senao'.");
                        } else if (check(TokenType.SE)) {
                            senao = List.of(parseComando());
                        } else {
                            senao = parseBlocoComandos(TokenType.FIM_SE);
                            consume(TokenType.FIM_SE, "Esperado 'FIM_SE' encerrando o comando 'SE'.");
                        }
                    }
                } else {
                    entao = parseBlocoComandos(TokenType.FIM_SE, TokenType.SENAO);
                    if (match(TokenType.SENAO)) {
                        senao = parseBlocoComandos(TokenType.FIM_SE);
                    }
                    consume(TokenType.FIM_SE, "Esperado 'FIM_SE' encerrando o comando 'SE'.");
                }
                return new ComandoAst.Se(condicao, entao, senao, token.line(), token.column());
            }

            case ENQUANTO: {
                advance();
                ExprAst condicao = parseExpressao();
                boolean comChave = match(TokenType.ABRE_CHAVE);
                if (!comChave) {
                    match(TokenType.FACA);
                }
                List<ComandoAst> corpo;
                if (comChave) {
                    corpo = parseBlocoComandos(TokenType.FECHA_CHAVE);
                    consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o laço 'ENQUANTO'.");
                } else {
                    corpo = parseBlocoComandos(TokenType.FIM_ENQUANTO);
                    consume(TokenType.FIM_ENQUANTO, "Esperado 'FIM_ENQUANTO' encerrando o laço 'ENQUANTO'.");
                }
                return new ComandoAst.Enquanto(condicao, corpo, token.line(), token.column());
            }

            case VETORIZAR_PARA: {
                advance();
                String variavel = consumeIdentificador("Esperado nome da variável do laço 'VETORIZAR_PARA'.").value();
                consume(TokenType.EM, "Esperado 'EM' introduzindo a fonte de dados vetorizada.");
                List<String> fonte = parseAcessoCaminho();
                Integer passoSimd = null;
                if (match(TokenType.PASSO_SIMD)) {
                    Token valorPasso = consume(TokenType.NUMERO_LITERAL, "Esperado tamanho do bloco SIMD após PASSO_SIMD.");
                    try {
                        passoSimd = Integer.parseInt(valorPasso.value(), 10);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("[Erro Sintático][Linha " + valorPasso.line() + ":" + valorPasso.column() + "] PASSO_SIMD exige inteiro positivo.");
                    }
                    if (passoSimd <= 0) {
                        throw new RuntimeException("[Erro Sintático][Linha " + valorPasso.line() + ":" + valorPasso.column() + "] PASSO_SIMD exige inteiro positivo.");
                    }
                }
                List<ComandoAst> corpo = parseBlocoComandos(TokenType.FIM_PARA, TokenType.FIM_VETORIZAR);
                if (!match(TokenType.FIM_PARA) && !match(TokenType.FIM_VETORIZAR)) {
                    throw new RuntimeException("[Erro Sintático][Linha " + peek().line() + ":" + peek().column() + "] Esperado 'FIM_PARA' ou 'FIM_VETORIZAR' encerrando o laço vetorizado.");
                }
                return new ComandoAst.VetorizarPara(variavel, fonte, passoSimd, corpo, token.line(), token.column());
            }

            case USAR_BLOCO_MEMORIA: {
                advance();
                String nome = consumeIdentificador("Esperado nome do bloco de memória temporária.").value();
                boolean comChave = match(TokenType.ABRE_CHAVE);
                if (!comChave) {
                    match(TokenType.FACA);
                }
                List<ComandoAst> corpo;
                if (comChave) {
                    corpo = parseBlocoComandos(TokenType.FECHA_CHAVE);
                    consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o escopo de memória.");
                } else {
                    corpo = parseBlocoComandos(TokenType.FIM_BLOCO_MEMORIA);
                    consume(TokenType.FIM_BLOCO_MEMORIA, "Esperado 'FIM_BLOCO_MEMORIA' encerrando o escopo de memória.");
                }
                return new ComandoAst.BlocoMemoria(nome, corpo, token.line(), token.column());
            }

            case LER: {
                advance();
                List<String> alvo = parseAcessoCaminho();
                return new ComandoAst.Ler(alvo, token.line(), token.column());
            }

            case PARA: {
                advance();
                String variavel = consumeIdentificador("Esperado nome da variável do laço 'PARA'.").value();
                consume(TokenType.DE, "Esperado 'DE' após variável do laço 'PARA'.");
                ExprAst inicio = parseExpressao();
                consume(TokenType.ATE, "Esperado 'ATE' após início do intervalo do laço 'PARA'.");
                ExprAst fim = parseExpressao();
                ExprAst passo = null;
                if (match(TokenType.PASSO)) {
                    passo = parseExpressao();
                }
                boolean comChave = match(TokenType.ABRE_CHAVE);
                if (!comChave) {
                    match(TokenType.FACA);
                }
                List<ComandoAst> corpo;
                if (comChave) {
                    corpo = parseBlocoComandos(TokenType.FECHA_CHAVE);
                    consume(TokenType.FECHA_CHAVE, "Esperado '}' encerrando o laço 'PARA'.");
                } else {
                    corpo = parseBlocoComandos(TokenType.FIM_PARA);
                    consume(TokenType.FIM_PARA, "Esperado 'FIM_PARA' encerrando o laço 'PARA'.");
                }
                return new ComandoAst.Para(variavel, inicio, fim, passo, corpo, token.line(), token.column());
            }

            case EXIBA: {
                advance();
                return new ComandoAst.Exiba(parseExpressao(), token.line(), token.column());
            }

            case RETORNE: {
                advance();
                boolean podeTerValor = !check(TokenType.FIM) && !check(TokenType.FIM_SE)
                        && !check(TokenType.FIM_ENQUANTO) && !check(TokenType.FIM_PARA)
                        && !check(TokenType.FIM_BLOCO_MEMORIA) && !check(TokenType.FECHA_CHAVE);
                return new ComandoAst.Retorne(podeTerValor ? parseExpressao() : null, token.line(), token.column());
            }

            case FALHAR_COM: {
                advance();
                return new ComandoAst.FalharCom(parseExpressao(), token.line(), token.column());
            }

            case IDENTIFICADOR, TELA: {
                List<String> alvo = parseAcessoCaminho();
                if (check(TokenType.ABRE_PARENTESE)) {
                    // Chamada isolada como comando: TEXTO.comprimento(x) ou MATEMATICA.abs(-3)
                    advance(); // '('
                    List<ExprAst> args = new ArrayList<>();
                    if (!check(TokenType.FECHA_PARENTESE)) {
                        do {
                            args.add(parseExpressao());
                        } while (match(TokenType.VIRGULA));
                    }
                    consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando argumentos da chamada.");
                    ExprAst expr = new ExprAst.Chamada(alvo, args, token.line(), token.column());
                    while (check(TokenType.ABRE_COLCHETE)) {
                        Token inicioIdx = peek();
                        advance();
                        ExprAst indice = parseExpressao();
                        consume(TokenType.FECHA_COLCHETE, "Esperado ']' após índice.");
                        expr = new ExprAst.Indexacao(expr, indice, inicioIdx.line(), inicioIdx.column());
                    }
                    return new ComandoAst.Chamada(expr, token.line(), token.column());
                }
                // Declaração tipada direta: x : TIPO <- expr ou x : TIPO := expr ou x : TIPO = expr
                if (alvo.size() == 1 && match(TokenType.DOIS_PONTOS)) {
                    String tipoDado = parseTipoDado();
                    if (match(TokenType.SETA_ATRIBUICAO)) {
                        ExprAst inicializacao = parseExpressao();
                        return new ComandoAst.DeclVariavel(alvo.get(0), tipoDado, inicializacao, token.line(), token.column());
                    } else if (check(TokenType.OPERADOR_RELACIONAL) && "=".equals(peek().value())) {
                        advance();
                        ExprAst inicializacao = parseExpressao();
                        return new ComandoAst.DeclVariavel(alvo.get(0), tipoDado, inicializacao, token.line(), token.column());
                    }
                    throw new RuntimeException("[Erro Sintático][Linha " + peek().line() + ":" + peek().column() + "] Esperado '<-', ':=' ou '=' na declaração da variável '" + alvo.get(0) + "'.");
                }

                // Declaração inferida direta: x := expr
                if (alvo.size() == 1 && check(TokenType.SETA_ATRIBUICAO) && peek().value().equals(":=")) {
                    advance();
                    ExprAst expressao = parseExpressao();
                    return new ComandoAst.DeclVariavel(alvo.get(0), null, expressao, token.line(), token.column());
                }

                if (match(TokenType.SETA_ATRIBUICAO)) {
                    // <- ou :=
                } else if (check(TokenType.OPERADOR_RELACIONAL) && "=".equals(peek().value())) {
                    advance();
                } else {
                    consume(TokenType.SETA_ATRIBUICAO, "Esperado '<-', ':=' ou '=' na atribuição a '" + String.join(".", alvo) + "'.");
                }
                ExprAst expressao = parseExpressao();
                return new ComandoAst.Atribuicao(alvo, expressao, token.line(), token.column());
            }

            default:
                throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] Comando não reconhecido. Esperados 'VARIAVEL', 'SE', 'ENQUANTO', 'VETORIZAR_PARA', 'PARA', 'USAR_BLOCO_MEMORIA', 'EXIBA', 'LER', 'RETORNE', 'FALHAR_COM' ou atribuição/chamada. (Encontrado: '" + token.value() + "')");
        }
    }

    private List<String> parseAcessoCaminho() {
        List<String> caminho = new ArrayList<>();
        if (check(TokenType.TELA)) {
            caminho.add(advance().value());
        } else {
            caminho.add(consumeIdentificador("Esperado identificador.").value());
        }
        while (match(TokenType.PONTO)) {
            caminho.add(consumeIdentificador("Esperado campo após ponto de acesso.").value());
        }
        return caminho;
    }

    /* ============================================================
     * EXPRESSÕES — precedência OU < E < relacional < aditivo < mult < unário
     * ============================================================ */

    public ExprAst parseExpressao() {
        return parseExprOu();
    }

    private ExprAst parseExprOu() {
        ExprAst esquerda = parseExprE();
        while (ehLogico("OU")) {
            Token op = advance();
            ExprAst direita = parseExprE();
            esquerda = new ExprAst.OpBinaria("OU", esquerda, direita, op.line(), op.column());
        }
        return esquerda;
    }

    private ExprAst parseExprE() {
        ExprAst esquerda = parseExprRelacional();
        while (ehLogico("E")) {
            Token op = advance();
            ExprAst direita = parseExprRelacional();
            esquerda = new ExprAst.OpBinaria("E", esquerda, direita, op.line(), op.column());
        }
        return esquerda;
    }

    private boolean ehLogico(String palavra) {
        Token t = peek();
        return t.type() == TokenType.OPERADOR_LOGICO && palavra.equals(t.value());
    }

    private ExprAst parseExprRelacional() {
        ExprAst esquerda = parseExprAditiva();
        Token t = peek();
        if (t.type() == TokenType.OPERADOR_RELACIONAL && RELACIONAIS.containsKey(t.value())) {
            advance();
            ExprAst direita = parseExprAditiva();
            return new ExprAst.OpBinaria(RELACIONAIS.get(t.value()), esquerda, direita, t.line(), t.column());
        }
        return esquerda;
    }

    private ExprAst parseExprAditiva() {
        ExprAst esquerda = parseExprMultiplicativa();
        while (check(TokenType.OPERADOR_ARITMETICO) && ("+".equals(peek().value()) || "-".equals(peek().value()))) {
            Token op = advance();
            ExprAst direita = parseExprMultiplicativa();
            esquerda = new ExprAst.OpBinaria(op.value(), esquerda, direita, op.line(), op.column());
        }
        return esquerda;
    }

    private ExprAst parseExprMultiplicativa() {
        ExprAst esquerda = parseExprUnaria();
        while (check(TokenType.OPERADOR_ARITMETICO) && List.of("*", "/", "%").contains(peek().value())) {
            Token op = advance();
            ExprAst direita = parseExprUnaria();
            esquerda = new ExprAst.OpBinaria(op.value(), esquerda, direita, op.line(), op.column());
        }
        return esquerda;
    }

    private ExprAst parseExprUnaria() {
        Token t = peek();
        if (t.type() == TokenType.OPERADOR_ARITMETICO && "-".equals(t.value())) {
            advance();
            return new ExprAst.OpUnaria("-", parseExprUnaria(), t.line(), t.column());
        }
        if (t.type() == TokenType.OPERADOR_LOGICO && "NAO".equals(t.value())) {
            advance();
            return new ExprAst.OpUnaria("NAO", parseExprUnaria(), t.line(), t.column());
        }
        return parseExprPrimaria();
    }

    private ExprAst parseExprPrimaria() {
        Token t = peek();

        if (t.type() == TokenType.NUMERO_LITERAL) {
            advance();
            int indicePonto = t.value().indexOf('.');
            ExprAst base;
            if (indicePonto >= 0) {
                String parteInteira = t.value().substring(0, indicePonto);
                String parteFracionaria = t.value().substring(indicePonto + 1);
                BigInteger escalado = new BigInteger((parteInteira.isEmpty() ? "0" : parteInteira) + parteFracionaria);
                int escala = parteFracionaria.length();
                base = new ExprAst.LiteralDecimal(escalado, escala, t.line(), t.column());
            } else {
                base = new ExprAst.LiteralInteiro(new BigInteger(t.value()), t.line(), t.column());
            }
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.STRING_LITERAL) {
            advance();
            ExprAst base = new ExprAst.LiteralTexto(t.value(), t.line(), t.column());
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.VERDADEIRO || t.type() == TokenType.FALSO) {
            advance();
            ExprAst base = new ExprAst.LiteralLogico(t.type() == TokenType.VERDADEIRO, t.line(), t.column());
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.NULO) {
            advance();
            ExprAst base = new ExprAst.Nulo(t.line(), t.column());
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.ABRE_PARENTESE) {
            advance();
            ExprAst interna = parseExpressao();
            consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando expressão parentetizada.");
            return parsePosfixo(interna);
        }

        if (t.type() == TokenType.ABRE_COLCHETE) {
            advance();
            List<ExprAst> elementos = new ArrayList<>();
            if (!check(TokenType.FECHA_COLCHETE)) {
                do {
                    elementos.add(parseExpressao());
                } while (match(TokenType.VIRGULA));
            }
            consume(TokenType.FECHA_COLCHETE, "Esperado ']' fechando literal de fatia.");
            ExprAst base = new ExprAst.FatiaLiteral(elementos, t.line(), t.column());
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.CRIAR) {
            advance();
            String nomeEstrutura = consumeIdentificador("Esperado nome da estrutura após CRIAR.").value();
            consume(TokenType.ABRE_PARENTESE, "Esperado '(' após nome da estrutura em 'CRIAR'.");
            List<ExprAst.CampoValor> campos = new ArrayList<>();
            if (!check(TokenType.FECHA_PARENTESE)) {
                do {
                    String nomeCampo = consumeIdentificador("Esperado nome do campo em CRIAR.").value();
                    consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do campo em CRIAR.");
                    ExprAst valor = parseExpressao();
                    campos.add(new ExprAst.CampoValor(nomeCampo, valor));
                } while (match(TokenType.VIRGULA));
            }
            consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando 'CRIAR'.");
            ExprAst base = new ExprAst.CriarRegistro(nomeEstrutura, campos, t.line(), t.column());
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.CONSULTAR) {
            advance();
            if (check(TokenType.DE)) {
                advance();
            }
            ExprAst fonte = parseExprPrimaria();
            ExprAst onde = null;
            String campoOrdenacao = null;
            boolean asc = true;
            ExprAst limite = null;
            ExprAst pular = null;

            while (true) {
                if (check(TokenType.ONDE) && onde == null) {
                    advance();
                    onde = parseExpressao();
                } else if (check(TokenType.ORDENAR_POR) && campoOrdenacao == null) {
                    advance();
                    campoOrdenacao = consumeIdentificador("Esperado nome do campo para ORDENAR_POR.").value();
                    if (check(TokenType.DESC)) {
                        advance();
                        asc = false;
                    } else if (check(TokenType.ASC)) {
                        advance();
                        asc = true;
                    }
                } else if (check(TokenType.LIMITE) && limite == null) {
                    advance();
                    limite = parseExpressao();
                } else if (check(TokenType.PULAR) && pular == null) {
                    advance();
                    pular = parseExpressao();
                } else {
                    break;
                }
            }
            ExprAst base = new ExprAst.ConsultaTipada(fonte, onde, campoOrdenacao, asc, limite, pular, t.line(), t.column());
            return parsePosfixo(base);
        }

        if (t.type() == TokenType.IDENTIFICADOR || t.type() == TokenType.TELA) {
            List<String> caminho = parseAcessoCaminho();
            ExprAst base;
            if (check(TokenType.ABRE_PARENTESE)) {
                advance();
                List<ExprAst> args = new ArrayList<>();
                if (!check(TokenType.FECHA_PARENTESE)) {
                    do {
                        args.add(parseExpressao());
                    } while (match(TokenType.VIRGULA));
                }
                consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando argumentos da chamada.");
                base = new ExprAst.Chamada(caminho, args, t.line(), t.column());
            } else {
                base = new ExprAst.AcessoCampo(caminho, t.line(), t.column());
            }
            return parsePosfixo(base);
        }

        if (PalavrasReservadas.ehPalavraReservada(t.value())) {
            throw new RuntimeException("[Erro Sintático][Linha " + t.line() + ":" + t.column() + "] '" + t.value() + "' é palavra reservada e não pode iniciar uma expressão.");
        }

        throw new RuntimeException("[Erro Sintático][Linha " + t.line() + ":" + t.column() + "] Expressão inválida. (Encontrado: '" + t.value() + "')");
    }

    private ExprAst parsePosfixo(ExprAst base) {
        ExprAst cur = base;
        while (true) {
            if (check(TokenType.ABRE_COLCHETE)) {
                Token inicio = peek();
                advance();
                ExprAst indice = parseExpressao();
                consume(TokenType.FECHA_COLCHETE, "Esperado ']' após índice.");
                cur = new ExprAst.Indexacao(cur, indice, inicio.line(), inicio.column());
            } else if (check(TokenType.PONTO)) {
                Token ponto = advance();
                String campo = consumeIdentificador("Esperado nome do campo após '.'.").value();
                cur = new ExprAst.Indexacao(cur, new ExprAst.LiteralTexto(campo, ponto.line(), ponto.column()), ponto.line(), ponto.column());
            } else {
                break;
            }
        }
        return cur;
    }

    /* ============================================================
     * INFRAESTRUTURA DO CURSOR
     * ============================================================ */

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        Token token = peek();
        throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] " + message + " (Encontrado: '" + token.value() + "')");
    }

    /**
     * Política estrita de palavras reservadas: um nome declarado (programa,
     * estrutura, campo, regra, operação, parâmetro, variável) nunca pode
     * colidir com palavra reservada da linguagem.
     */
    private Token consumeIdentificador(String message) {
        Token token = peek();
        if (!check(TokenType.IDENTIFICADOR)) {
            if (PalavrasReservadas.ehPalavraReservada(token.value())) {
                throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] " + message + " '" + token.value() + "' é palavra reservada e não pode ser usada como identificador.");
            }
            throw new RuntimeException("[Erro Sintático][Linha " + token.line() + ":" + token.column() + "] " + message + " (Encontrado: '" + token.value() + "')");
        }
        return advance();
    }

    /** Renderização textual canônica de uma expressão (para docgen e auditoria) com checagem exaustiva de tipos. */
    public static String textoCanonicoDe(ExprAst expr) {
        return switch (expr) {
            case ExprAst.LiteralInteiro li -> li.valor().toString();
            case ExprAst.LiteralDecimal ld -> formatarEscalado(ld.escalado(), ld.escala());
            case ExprAst.LiteralTexto lt -> "\"" + lt.valor().replace("\n", "\\n") + "\"";
            case ExprAst.LiteralLogico ll -> ll.valor() ? "VERDADEIRO" : "FALSO";
            case ExprAst.Nulo _ -> "NULO";
            case ExprAst.AcessoCampo ac -> String.join(".", ac.caminho());
            case ExprAst.Chamada ch -> String.join(".", ch.caminho()) + "(" + ch.argumentos().stream().map(ThzParser::textoCanonicoDe).collect(Collectors.joining(", ")) + ")";
            case ExprAst.Indexacao idx -> textoCanonicoDe(idx.alvo()) + "[" + textoCanonicoDe(idx.indice()) + "]";
            case ExprAst.FatiaLiteral fl -> "[" + fl.elementos().stream().map(ThzParser::textoCanonicoDe).collect(Collectors.joining(", ")) + "]";
            case ExprAst.CriarRegistro cr -> "CRIAR " + cr.nomeEstrutura() + "(" + cr.campos().stream().map(c -> c.nome() + ": " + textoCanonicoDe(c.valor())).collect(Collectors.joining(", ")) + ")";
            case ExprAst.OpUnaria ou -> ("-".equals(ou.operador()) ? "-" : "NAO ") + textoCanonicoDe(ou.operando());
            case ExprAst.OpBinaria ob -> textoCanonicoDe(ob.esquerda()) + " " + ob.operador() + " " + textoCanonicoDe(ob.direita());
            case ExprAst.ConsultaTipada ct -> "CONSULTAR DE " + textoCanonicoDe(ct.fonte()) + (ct.onde() != null ? " ONDE " + textoCanonicoDe(ct.onde()) : "") + (ct.campoOrdenacao() != null ? " ORDENAR_POR " + ct.campoOrdenacao() + (ct.asc() ? " ASC" : " DESC") : "");
        };
    }


    private static String formatarEscalado(BigInteger escalado, int escala) {
        boolean negativo = escalado.compareTo(BigInteger.ZERO) < 0;
        BigInteger absoluto = negativo ? escalado.negate() : escalado;
        BigInteger divisor = BigInteger.TEN.pow(escala);
        BigInteger inteiro = absoluto.divide(divisor);
        BigInteger resto = absoluto.mod(divisor);
        String fracao = resto.toString();
        // padStart escala with '0'
        if (fracao.length() < escala) {
            fracao = "0".repeat(escala - fracao.length()) + fracao;
        }
        return (negativo ? "-" : "") + inteiro.toString() + "." + fracao;
    }
}
