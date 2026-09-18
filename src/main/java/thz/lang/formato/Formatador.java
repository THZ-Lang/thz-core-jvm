package thz.lang.formato;

import thz.lang.ast.*;
import thz.lang.lexico.DialetoLinguagem;
import thz.lang.lexico.PalavrasReservadas;
import thz.lang.sintatico.ThzParser;
import java.util.ArrayList;
import java.util.List;

public final class Formatador {
    private static final String IND = "    ";
    private Formatador() {}
    
    private static String tipoCanonico(String tipo, DialetoLinguagem dialeto) {
        String base = tipo.replaceAll("\\s+", "").replace(",", ", ");
        if (dialeto == DialetoLinguagem.EN_US) {
            base = base.replace("TEXTO", "TEXT")
                       .replace("INTEIRO", "INTEGER")
                       .replace("MONETARIO", "MONETARY")
                       .replace("BOOLEANO", "BOOLEAN")
                       .replace("DATA_HORA", "DATETIME")
                       .replace("DATA", "DATE")
                       .replace("FATIA", "LIST")
                       .replace("MAPA", "MAP")
                       .replace("RESULTADO", "RESULT");
        }
        return base;
    }

    private static String linha(String v, int nivel) { return IND.repeat(nivel) + v; }
    private static String formatarExpr(ExprAst e) { return ThzParser.textoCanonicoDe(e); }

    private static List<String> formatarComandos(List<ComandoAst> comandos, int nivel, DialetoLinguagem dialeto) {
        List<String> out = new ArrayList<>();
        boolean en = (dialeto == DialetoLinguagem.EN_US);

        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.DeclVariavel d -> {
                    String kw = en ? "VARIABLE " : "VARIAVEL ";
                    String tipoStr = d.tipoDado() != null ? ": " + tipoCanonico(d.tipoDado(), dialeto) : "";
                    out.add(linha(kw + d.nome() + tipoStr + " <- " + formatarExpr(d.inicializacao()), nivel));
                }
                case ComandoAst.CasoResultado cr -> {
                    String kwMatch = en ? "MATCH_RESULT " : "ESCOLHA ";
                    String kwSuccess = en ? "SUCCESS(" : "CASO SUCESSO(";
                    String kwError = en ? "ERROR(" : "CASO FALHA(";
                    String kwEndMatch = en ? "END_MATCH" : "FIM_ESCOLHA";

                    out.add(linha(kwMatch + formatarExpr(cr.alvo()), nivel));
                    if (cr.varSucesso() != null) {
                        out.add(linha(kwSuccess + cr.varSucesso() + ") ->", nivel + 1));
                        out.addAll(formatarComandos(cr.corpoSucesso(), nivel + 2, dialeto));
                    }
                    if (cr.varErro() != null) {
                        out.add(linha(kwError + cr.varErro() + ") ->", nivel + 1));
                        out.addAll(formatarComandos(cr.corpoErro(), nivel + 2, dialeto));
                    }
                    out.add(linha(kwEndMatch, nivel));
                }
                case ComandoAst.Atribuicao a ->
                    out.add(linha(String.join(".", a.alvo()) + " <- " + formatarExpr(a.expressao()), nivel));
                case ComandoAst.Se s -> {
                    String kwSe = en ? "IF " : "SE ";
                    String kwSenao = en ? "ELSE" : "SENAO";
                    String kwFimSe = en ? "END_IF" : "FIM_SE";

                    out.add(linha(kwSe + formatarExpr(s.condicao()), nivel));
                    out.addAll(formatarComandos(s.entao(), nivel + 1, dialeto));
                    if (!s.senao().isEmpty()) {
                        out.add(linha(kwSenao, nivel));
                        out.addAll(formatarComandos(s.senao(), nivel + 1, dialeto));
                    }
                    out.add(linha(kwFimSe, nivel));
                }
                case ComandoAst.Enquanto e -> {
                    String kwEnq = en ? "WHILE " : "ENQUANTO ";
                    String kwFimEnq = en ? "END_WHILE" : "FIM_ENQUANTO";

                    out.add(linha(kwEnq + formatarExpr(e.condicao()), nivel));
                    out.addAll(formatarComandos(e.corpo(), nivel + 1, dialeto));
                    out.add(linha(kwFimEnq, nivel));
                }
                case ComandoAst.VetorizarPara v -> {
                    String kwVet = en ? "VECTORIZE_FOR " : "VETORIZAR_PARA ";
                    String kwEm = en ? " IN " : " EM ";
                    String kwPasso = en ? " SIMD_STEP " : " PASSO_SIMD ";
                    String kwFimPara = en ? "END_FOR" : "FIM_PARA";

                    String passo = v.passoSimd() != null ? kwPasso + v.passoSimd() : "";
                    out.add(linha(kwVet + v.variavel() + kwEm + String.join(".", v.fonte()) + passo, nivel));
                    out.addAll(formatarComandos(v.corpo(), nivel + 1, dialeto));
                    out.add(linha(kwFimPara, nivel));
                }
                case ComandoAst.Para p -> {
                    String kwPara = en ? "FOR " : "PARA ";
                    String kwDe = en ? " FROM " : " DE ";
                    String kwAte = en ? " TO " : " ATE ";
                    String kwPasso = en ? " STEP " : " PASSO ";
                    String kwFimPara = en ? "END_FOR" : "FIM_PARA";

                    String passo = p.passo() != null ? kwPasso + formatarExpr(p.passo()) : "";
                    out.add(linha(kwPara + p.variavel() + kwDe + formatarExpr(p.inicio()) + kwAte + formatarExpr(p.fim()) + passo, nivel));
                    out.addAll(formatarComandos(p.corpo(), nivel + 1, dialeto));
                    out.add(linha(kwFimPara, nivel));
                }
                case ComandoAst.BlocoMemoria b -> {
                    String kwBloco = en ? "USE_MEMORY_BLOCK " : "USAR_BLOCO_MEMORIA ";
                    String kwFimBloco = en ? "END_MEMORY_BLOCK" : "FIM_BLOCO_MEMORIA";

                    out.add(linha(kwBloco + b.nome(), nivel));
                    out.addAll(formatarComandos(b.corpo(), nivel + 1, dialeto));
                    out.add(linha(kwFimBloco, nivel));
                }
                case ComandoAst.Exiba e ->
                    out.add(linha((en ? "PRINT " : "EXIBA ") + formatarExpr(e.expressao()), nivel));
                case ComandoAst.Ler l ->
                    out.add(linha((en ? "READ " : "LER ") + String.join(".", l.alvo()), nivel));
                case ComandoAst.Chamada ch ->
                    out.add(linha(formatarExpr(ch.expressao()), nivel));
                case ComandoAst.Retorne r ->
                    out.add(linha(r.expressao() != null ? (en ? "RETURN " : "RETORNE ") + formatarExpr(r.expressao()) : (en ? "RETURN" : "RETORNE"), nivel));
                case ComandoAst.FalharCom f ->
                    out.add(linha((en ? "FAIL_WITH " : "FALHAR_COM ") + formatarExpr(f.expressao()), nivel));
                case ComandoAst.Tente t -> {
                    out.add(linha(en ? "TRY" : "TENTE", nivel));
                    out.addAll(formatarComandos(t.corpoTente(), nivel + 1, dialeto));
                    out.add(linha((en ? "CATCH " : "CAPTURE ") + t.tipoCaptura(), nivel));
                    out.addAll(formatarComandos(t.corpoCapture(), nivel + 1, dialeto));
                    out.add(linha(en ? "END_TRY" : "FIM_TENTE", nivel));
                }
            }
        }
        return out;
    }

    private static List<String> formatarComandosEnxutos(List<ComandoAst> comandos, int nivel) {
        List<String> out = new ArrayList<>();
        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.DeclVariavel d -> {
                    String tipo = d.tipoDado() == null ? "" : ": " + tipoCanonico(d.tipoDado(), DialetoLinguagem.PT_BR);
                    out.add(linha(d.nome() + tipo + " := " + formatarExpr(d.inicializacao()), nivel));
                }
                case ComandoAst.Atribuicao a -> out.add(linha(String.join(".", a.alvo()) + " = " + formatarExpr(a.expressao()), nivel));
                case ComandoAst.Se s -> {
                    out.add(linha("se " + formatarExpr(s.condicao()) + ":", nivel));
                    out.addAll(formatarComandosEnxutos(s.entao(), nivel + 1));
                    if (!s.senao().isEmpty()) {
                        out.add(linha("senao:", nivel));
                        out.addAll(formatarComandosEnxutos(s.senao(), nivel + 1));
                    }
                }
                case ComandoAst.Enquanto e -> {
                    out.add(linha("enquanto " + formatarExpr(e.condicao()) + ":", nivel));
                    out.addAll(formatarComandosEnxutos(e.corpo(), nivel + 1));
                }
                case ComandoAst.Para p -> {
                    String passo = p.passo() == null ? "" : " passo " + formatarExpr(p.passo());
                    out.add(linha("para " + p.variavel() + " de " + formatarExpr(p.inicio()) + " ate " + formatarExpr(p.fim()) + passo + ":", nivel));
                    out.addAll(formatarComandosEnxutos(p.corpo(), nivel + 1));
                }
                case ComandoAst.VetorizarPara v -> {
                    String passo = v.passoSimd() == null ? "" : " PASSO_SIMD " + v.passoSimd();
                    out.add(linha("VETORIZAR_PARA " + v.variavel() + " EM " + String.join(".", v.fonte()) + passo + ":", nivel));
                    out.addAll(formatarComandosEnxutos(v.corpo(), nivel + 1));
                }
                case ComandoAst.BlocoMemoria b -> {
                    out.add(linha("USAR_BLOCO_MEMORIA " + b.nome() + ":", nivel));
                    out.addAll(formatarComandosEnxutos(b.corpo(), nivel + 1));
                }
                case ComandoAst.Exiba e -> out.add(linha("exiba " + formatarExpr(e.expressao()), nivel));
                case ComandoAst.Ler l -> out.add(linha("LER " + String.join(".", l.alvo()), nivel));
                case ComandoAst.Chamada ch -> out.add(linha(formatarExpr(ch.expressao()), nivel));
                case ComandoAst.Retorne r -> out.add(linha(r.expressao() == null ? "retorne" : "retorne " + formatarExpr(r.expressao()), nivel));
                case ComandoAst.FalharCom f -> out.add(linha("FALHAR_COM " + formatarExpr(f.expressao()), nivel));
                case ComandoAst.Tente t -> {
                    out.add(linha("TENTE:", nivel));
                    out.addAll(formatarComandosEnxutos(t.corpoTente(), nivel + 1));
                    out.add(linha("CAPTURE " + t.tipoCaptura() + ":", nivel));
                    out.addAll(formatarComandosEnxutos(t.corpoCapture(), nivel + 1));
                }
                case ComandoAst.CasoResultado cr -> {
                    out.add(linha("ESCOLHA " + formatarExpr(cr.alvo()) + ":", nivel));
                    if (cr.varSucesso() != null) {
                        out.add(linha("CASO SUCESSO(" + cr.varSucesso() + "):", nivel + 1));
                        out.addAll(formatarComandosEnxutos(cr.corpoSucesso(), nivel + 2));
                    }
                    if (cr.varErro() != null) {
                        out.add(linha("CASO FALHA(" + cr.varErro() + "):", nivel + 1));
                        out.addAll(formatarComandosEnxutos(cr.corpoErro(), nivel + 2));
                    }
                }
            }
        }
        return out;
    }

    /** Formatação canônica compacta da THZ-LANG 4. */
    public static String formatarEnxuto(ProgramaAst ast) {
        List<String> out = new ArrayList<>();
        TipoModulo tipo = ast.tipoModulo() != null ? ast.tipoModulo() : TipoModulo.PROGRAMA;
        out.add(tipo.descricao().toLowerCase() + " " + ast.nome() + ":");
        int raiz = 1;

        for (ImportacaoAst imp : ast.importacoes() != null ? ast.importacoes() : List.<ImportacaoAst>of()) {
            String de = imp.caminho() == null ? "" : " DE \"" + imp.caminho() + "\"";
            out.add(linha("IMPORTAR " + imp.modulo() + de, raiz));
        }

        if (ast.metadados() != null) {
            out.add(linha("metadados:", raiz));
            var m = ast.metadados();
            if (m.dominio() != null) out.add(linha("DOMINIO: \"" + m.dominio() + "\"", raiz + 1));
            if (m.subdominio() != null) out.add(linha("SUBDOMINIO: \"" + m.subdominio() + "\"", raiz + 1));
            if (m.camada() != null) out.add(linha("CAMADA: \"" + m.camada() + "\"", raiz + 1));
            if (m.versao() != null) out.add(linha("VERSAO: \"" + m.versao() + "\"", raiz + 1));
            if (m.autor() != null) out.add(linha("AUTOR: \"" + m.autor() + "\"", raiz + 1));
            if (m.sloLatencia() != null) out.add(linha("SLO_LATENCIA_MAXIMA: \"" + m.sloLatencia() + "\"", raiz + 1));
            if (m.conformidade() != null && !m.conformidade().isEmpty()) {
                String lista = m.conformidade().stream().map(c -> "\"" + c + "\"").reduce((a,b) -> a + ", " + b).orElse("");
                out.add(linha("CONFORMIDADE: " + lista, raiz + 1));
            }
        }
        for (EstruturaAst est : ast.estruturas()) {
            String layout = est.layoutColunar() ? " LAYOUT_COLUNAR" : "";
            out.add(linha("estrutura " + est.nome() + layout + ":", raiz));
            for (CampoEstruturaAst campo : est.campos()) out.add(linha(campo.nome() + ": " + tipoCanonico(campo.tipo(), DialetoLinguagem.PT_BR), raiz + 1));
            for (InvarianteAst inv : est.invariantes()) out.add(linha("invariante " + inv.textoCanonico(), raiz + 1));
        }
        for (EnumeracaoAst enumeracao : ast.enumeracoes()) {
            out.add(linha("enumeracao " + enumeracao.nome() + ":", raiz));
            for (String membro : enumeracao.membros()) out.add(linha(membro, raiz + 1));
        }
        for (RegraNegocioAst regra : ast.regras()) {
            out.add(linha("regra " + regra.nome() + ":", raiz));
            if (regra.rastreioRequisito() != null) out.add(linha("RASTREIO_REQUISITO: \"" + regra.rastreioRequisito() + "\"", raiz + 1));
            for (ClausulaContratoAst c : regra.clausulasEntrada()) out.add(linha("exige " + c.textoCanonico(), raiz + 1));
            for (ClausulaContratoAst c : regra.clausulasSaida()) out.add(linha("garante " + c.textoCanonico(), raiz + 1));
            for (OperacaoAst op : regra.operacoes()) {
                String params = op.parametros().stream().map(p -> p.nome() + ": " + tipoCanonico(p.tipo(), DialetoLinguagem.PT_BR)).reduce((a,b) -> a + ", " + b).orElse("");
                out.add(linha("operacao " + op.nome() + "(" + params + ") -> " + tipoCanonico(op.tipoRetorno(), DialetoLinguagem.PT_BR) + ":", raiz + 1));
                out.addAll(formatarComandosEnxutos(op.corpo(), raiz + 2));
            }
        }
        for (ProcedimentoAst proc : ast.procedimentos() != null ? ast.procedimentos() : List.<ProcedimentoAst>of()) {
            String params = proc.parametros().stream().map(p -> p.nome() + ": " + tipoCanonico(p.tipo(), DialetoLinguagem.PT_BR)).reduce((a,b) -> a + ", " + b).orElse("");
            out.add(linha("procedimento " + proc.nome() + "(" + params + "):", raiz));
            out.addAll(formatarComandosEnxutos(proc.corpo(), raiz + 1));
        }
        for (FuncaoAst funcao : ast.funcoes() != null ? ast.funcoes() : List.<FuncaoAst>of()) {
            String params = funcao.parametros().stream().map(p -> p.nome() + ": " + tipoCanonico(p.tipo(), DialetoLinguagem.PT_BR)).reduce((a,b) -> a + ", " + b).orElse("");
            out.add(linha("funcao " + funcao.nome() + "(" + params + ") -> " + tipoCanonico(funcao.tipoRetorno(), DialetoLinguagem.PT_BR) + ":", raiz));
            out.addAll(formatarComandosEnxutos(funcao.corpo(), raiz + 1));
        }
        return String.join("\n", out) + "\n";
    }

    public static String formatar(ProgramaAst ast) {
        DialetoLinguagem d = ast.dialeto() != null ? ast.dialeto() : DialetoLinguagem.PT_BR;
        return formatar(ast, d);
    }

    public static String formatar(ProgramaAst ast, DialetoLinguagem dialeto) {
        List<String> out = new ArrayList<>();
        boolean en = (dialeto == DialetoLinguagem.EN_US);

        if (en) {
            out.add("LANGUAGE: en-US");
        } else {
            out.add("LINGUAGEM: pt-BR");
        }

        TipoModulo tipo = ast.tipoModulo() != null ? ast.tipoModulo() : TipoModulo.PROGRAMA;
        String descTipo = en ? PalavrasReservadas.traduzir(tipo.descricao(), DialetoLinguagem.EN_US) : tipo.descricao();
        out.add(descTipo + " " + ast.nome()); out.add("");

        if (ast.importacoes() != null && !ast.importacoes().isEmpty()) {
            for (ImportacaoAst imp : ast.importacoes()) {
                String kwImport = en ? "IMPORT " : "IMPORTAR ";
                String kwFrom = en ? " FROM \"" : " DE \"";
                String de = imp.caminho() != null ? kwFrom + imp.caminho() + "\"" : "";
                out.add(kwImport + imp.modulo() + de);
            }
            out.add("");
        }

        if (ast.metadados() != null) {
            out.add(en ? "ARCHITECTURE_METADATA" : "METADADOS_ARQUITETURA");
            var m = ast.metadados();
            if (m.dominio() != null) out.add(linha((en ? "DOMAIN: \"" : "DOMINIO: \"") + m.dominio() + "\"", 1));
            if (m.subdominio() != null) out.add(linha((en ? "SUBDOMAIN: \"" : "SUBDOMINIO: \"") + m.subdominio() + "\"", 1));
            if (m.camada() != null) out.add(linha((en ? "LAYER: \"" : "CAMADA: \"") + m.camada() + "\"", 1));
            if (m.versao() != null) out.add(linha((en ? "VERSION: \"" : "VERSAO: \"") + m.versao() + "\"", 1));
            if (m.autor() != null) out.add(linha((en ? "AUTHOR: \"" : "AUTOR: \"") + m.autor() + "\"", 1));
            if (m.sloLatencia() != null) out.add(linha((en ? "MAX_LATENCY_SLO: \"" : "SLO_LATENCIA_MAXIMA: \"") + m.sloLatencia() + "\"", 1));
            if (m.conformidade() != null && !m.conformidade().isEmpty()) {
                String lista = m.conformidade().stream().map(c -> "\"" + c + "\"").reduce((a,b)->a+", "+b).orElse("");
                out.add(linha((en ? "COMPLIANCE: " : "CONFORMIDADE: ") + lista, 1));
            }
            out.add(en ? "END_METADATA" : "FIM_METADADOS"); out.add("");
        }

        for (EstruturaAst est : ast.estruturas()) {
            String layout = est.layoutColunar() ? (en ? " COLUMNAR_LAYOUT" : " LAYOUT_COLUNAR") : "";
            out.add((en ? "STRUCTURE " : "ESTRUTURA ") + est.nome() + layout);
            for (CampoEstruturaAst campo : est.campos()) {
                out.add(linha(campo.nome() + " : " + tipoCanonico(campo.tipo(), dialeto), 1));
            }
            for (InvarianteAst inv : est.invariantes()) {
                out.add(linha((en ? "INVARIANT " : "INVARIANTE ") + inv.textoCanonico(), 1));
            }
            out.add(en ? "END_STRUCTURE" : "FIM_ESTRUTURA"); out.add("");
        }

        for (EnumeracaoAst enumeracao : ast.enumeracoes()) {
            out.add((en ? "ENUM " : "ENUMERACAO ") + enumeracao.nome());
            for (String mem : enumeracao.membros()) out.add(linha(mem, 1));
            out.add(en ? "END_ENUM" : "FIM_ENUMERACAO"); out.add("");
        }

        for (RegraNegocioAst regra : ast.regras()) {
            out.add((en ? "BUSINESS_RULE " : "REGRA_NEGOCIO ") + regra.nome());
            if (regra.identificador() != null) out.add(linha((en ? "RULE_ID: \"" : "IDENTIFICADOR_REGRA: \"") + regra.identificador() + "\"", 1));
            if (regra.rastreioRequisito() != null) out.add(linha((en ? "REQUIREMENT_TRACE: \"" : "RASTREIO_REQUISITO: \"") + regra.rastreioRequisito() + "\"", 1));
            if (regra.descricao() != null) out.add(linha((en ? "DESCRIPTION: \"" : "DESCRICAO: \"") + regra.descricao() + "\"", 1));
            if (regra.idempotente()) out.add(linha(en ? "IDEMPOTENT: TRUE" : "IDEMPOTENTE: VERDADEIRO", 1));
            if (regra.chaveIdempotencia() != null) out.add(linha((en ? "IDEMPOTENCY_KEY: \"" : "CHAVE_IDEMPOTENCIA: \"") + regra.chaveIdempotencia() + "\"", 1));

            if (!regra.clausulasEntrada().isEmpty()) {
                out.add(linha(en ? "INPUT_CONTRACT" : "CONTRATO_ENTRADA", 1));
                for (ClausulaContratoAst c : regra.clausulasEntrada()) {
                    out.add(linha((en ? "REQUIRES " : "EXIGE ") + c.textoCanonico(), 2));
                }
                out.add(linha(en ? "END_INPUT_CONTRACT" : "FIM_CONTRATO_ENTRADA", 1));
            }

            if (!regra.clausulasSaida().isEmpty()) {
                out.add(linha(en ? "OUTPUT_CONTRACT" : "CONTRATO_SAIDA", 1));
                for (ClausulaContratoAst c : regra.clausulasSaida()) {
                    out.add(linha((en ? "ENSURES " : "GARANTE ") + c.textoCanonico(), 2));
                }
                out.add(linha(en ? "END_OUTPUT_CONTRACT" : "FIM_CONTRATO_SAIDA", 1));
            }

            for (OperacaoAst op : regra.operacoes()) {
                String params = op.parametros().stream()
                        .map(p -> p.nome() + ": " + tipoCanonico(p.tipo(), dialeto))
                        .reduce((a, b) -> a + ", " + b).orElse("");
                String idempMod = op.idempotente() ? (en ? "IDEMPOTENT " : "IDEMPOTENTE ") : "";
                String kwOp = en ? "OPERATION " : "OPERACAO ";
                out.add(linha(kwOp + idempMod + op.nome() + "(" + params + ") : " + tipoCanonico(op.tipoRetorno(), dialeto), 1));
                if (!op.corpo().isEmpty()) {
                    out.add(linha(en ? "BEGIN" : "INICIO", 1));
                    out.addAll(formatarComandos(op.corpo(), 2, dialeto));
                    out.add(linha(en ? "END_OPERATION" : "FIM_OPERACAO", 1));
                }
            }
            out.add(en ? "END_BUSINESS_RULE" : "FIM_REGRA_NEGOCIO"); out.add("");
        }

        for (ProcedimentoAst proc : ast.procedimentos() != null ? ast.procedimentos() : List.<ProcedimentoAst>of()) {
            String params = proc.parametros().stream()
                    .map(p -> p.nome() + ": " + tipoCanonico(p.tipo(), dialeto))
                    .reduce((a, b) -> a + ", " + b).orElse("");
            String idempMod = proc.idempotente() ? (en ? "IDEMPOTENT " : "IDEMPOTENTE ") : "";
            out.add((en ? "PROCEDURE " : "PROCEDIMENTO ") + idempMod + proc.nome() + "(" + params + ")");
            if (!proc.corpo().isEmpty()) {
                out.add(linha(en ? "BEGIN" : "INICIO", 1));
                out.addAll(formatarComandos(proc.corpo(), 2, dialeto));
                out.add(linha(en ? "END" : "FIM", 1));
            }
            out.add("");
        }

        for (FuncaoAst funcao : ast.funcoes() != null ? ast.funcoes() : List.<FuncaoAst>of()) {
            String params = funcao.parametros().stream()
                    .map(p -> p.nome() + ": " + tipoCanonico(p.tipo(), dialeto))
                    .reduce((a, b) -> a + ", " + b).orElse("");
            String cabecalho = (en ? "FUNCTION " : "FUNCAO ") + funcao.nome() + "(" + params + "): "
                    + tipoCanonico(funcao.tipoRetorno(), dialeto);
            if (funcao.corpo().size() == 1 && funcao.corpo().getFirst() instanceof ComandoAst.Retorne r && r.expressao() != null) {
                out.add(cabecalho + " = " + formatarExpr(r.expressao()));
                out.add("");
                continue;
            }
            out.add(cabecalho);
            out.addAll(formatarComandos(funcao.corpo(), 1, dialeto));
            out.add(en ? "END_FUNCTION" : "FIM_FUNCAO");
            out.add("");
        }

        String terminador = en ? PalavrasReservadas.traduzir(tipo.terminadorPadrao(), DialetoLinguagem.EN_US) : tipo.terminadorPadrao();
        out.add(terminador); out.add("");
        return String.join("\n", out);
    }
}
