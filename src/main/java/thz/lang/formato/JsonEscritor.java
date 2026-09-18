package thz.lang.formato;

import java.util.List;

import thz.lang.ast.ComandoAst;
import thz.lang.ast.EnumeracaoAst;
import thz.lang.ast.EstruturaAst;
import thz.lang.ast.ExprAst;
import thz.lang.ast.FuncaoAst;
import thz.lang.ast.MetadadosArquiteturaAst;
import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;

public final class JsonEscritor {
    private JsonEscritor() {
    }

    public static String paraJson(ProgramaAst p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"nome\": ").append(q(p.nome())).append(",\n");
        sb.append("  \"versaoLinguagem\": ").append(p.versaoLinguagem() == null ? "null" : q(p.versaoLinguagem()))
                .append(",\n");
        sb.append("  \"metadados\": ").append(metadadosJson(p.metadados())).append(",\n");
        sb.append("  \"estruturas\": ").append(lista(p.estruturas(), JsonEscritor::estruturaJson)).append(",\n");
        sb.append("  \"enumeracoes\": ").append(lista(p.enumeracoes(), JsonEscritor::enumeracaoJson)).append(",\n");
        sb.append("  \"regras\": ").append(lista(p.regras(), JsonEscritor::regraJson)).append(",\n");
        sb.append("  \"procedimentos\": ")
                .append(lista(p.procedimentos() == null ? List.of() : p.procedimentos(), JsonEscritor::procJson))
                .append(",\n  \"funcoes\": ")
                .append(lista(p.funcoes() == null ? List.of() : p.funcoes(), JsonEscritor::funcaoJson))
                .append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String metadadosJson(MetadadosArquiteturaAst m) {
        if (m == null)
            return "null";
        return "{\n    \"dominio\": " + q(m.dominio()) + ",\n    \"subdominio\": " + q(m.subdominio())
                + ",\n    \"camada\": " + q(m.camada()) + ",\n    \"versao\": " + q(m.versao()) + ",\n    \"autor\": "
                + q(m.autor()) + ",\n    \"sloLatencia\": " + q(m.sloLatencia()) + ",\n    \"conformidade\": "
                + listaStr(m.conformidade()) + "\n  }";
    }

    private static String estruturaJson(EstruturaAst e) {
        return "{\"nome\": " + q(e.nome()) + ", \"layoutColunar\": " + e.layoutColunar() + ", \"campos\": "
                + lista(e.campos(), c -> "{\"nome\": " + q(c.nome()) + ", \"tipo\": " + q(c.tipo()) + "}")
                + ", \"invariantes\": "
                + lista(e.invariantes(),
                        inv -> "{\"expressao\": " + exprJson(inv.expressao()) + ", \"textoCanonico\": "
                                + q(inv.textoCanonico()) + ", \"linha\": " + inv.linha() + ", \"coluna\": "
                                + inv.coluna() + "}")
                + "}";
    }

    private static String enumeracaoJson(EnumeracaoAst e) {
        return "{\"nome\": " + q(e.nome()) + ", \"membros\": " + listaStr(e.membros()) + "}";
    }

    private static String regraJson(RegraNegocioAst r) {
        return "{\"nome\": " + q(r.nome()) + ", \"identificador\": "
                + (r.identificador() == null ? "null" : q(r.identificador())) + ", \"rastreioRequisito\": "
                + (r.rastreioRequisito() == null ? "null" : q(r.rastreioRequisito())) + ", \"descricao\": "
                + (r.descricao() == null ? "null" : q(r.descricao())) + ", \"clausulasEntrada\": "
                + lista(r.clausulasEntrada(),
                        c -> "{\"tipoClausula\": " + q(c.tipoClausula()) + ", \"textoCanonico\": "
                                + q(c.textoCanonico()) + ", \"expressao\": " + exprJson(c.expressao()) + "}")
                + ", \"clausulasSaida\": "
                + lista(r.clausulasSaida(),
                        c -> "{\"tipoClausula\": " + q(c.tipoClausula()) + ", \"textoCanonico\": "
                                + q(c.textoCanonico()) + ", \"expressao\": " + exprJson(c.expressao()) + "}")
                + ", \"operacoes\": " + lista(r.operacoes(), JsonEscritor::opJson) + "}";
    }

    private static String opJson(OperacaoAst o) {
        return "{\"nome\": " + q(o.nome()) + ", \"parametros\": "
                + lista(o.parametros(), p -> "{\"nome\": " + q(p.nome()) + ", \"tipo\": " + q(p.tipo()) + "}")
                + ", \"tipoRetorno\": " + q(o.tipoRetorno()) + ", \"corpo\": " + lista(o.corpo(), JsonEscritor::cmdJson)
                + "}";
    }

    private static String procJson(ProcedimentoAst p) {
        return "{\"nome\": " + q(p.nome()) + ", \"parametros\": "
                + lista(p.parametros(), v -> "{\"nome\": " + q(v.nome()) + ", \"tipo\": " + q(v.tipo()) + "}")
                + ", \"corpo\": " + lista(p.corpo(), JsonEscritor::cmdJson) + "}";
    }

    private static String funcaoJson(FuncaoAst f) {
        return "{\"nome\": " + q(f.nome()) + ", \"parametros\": "
                + lista(f.parametros(), v -> "{\"nome\": " + q(v.nome()) + ", \"tipo\": " + q(v.tipo()) + "}")
                + ", \"tipoRetorno\": " + q(f.tipoRetorno()) + ", \"corpo\": " + lista(f.corpo(), JsonEscritor::cmdJson) + "}";
    }

    private static String cmdJson(ComandoAst c) {
        if (c instanceof ComandoAst.DeclVariavel d)
            return "{\"tipoComando\": \"DECL_VARIAVEL\", \"nome\": " + q(d.nome()) + ", \"tipoDado\": "
                    + q(d.tipoDado()) + ", \"inicializacao\": " + exprJson(d.inicializacao()) + ", \"linha\": "
                    + d.linha() + ", \"coluna\": " + d.coluna() + "}";
        if (c instanceof ComandoAst.Atribuicao a)
            return "{\"tipoComando\": \"ATRIBUICAO\", \"alvo\": " + listaStr(a.alvo()) + ", \"expressao\": "
                    + exprJson(a.expressao()) + "}";
        if (c instanceof ComandoAst.Se s)
            return "{\"tipoComando\": \"SE\", \"condicao\": " + exprJson(s.condicao()) + ", \"entao\": "
                    + lista(s.entao(), JsonEscritor::cmdJson) + ", \"senao\": "
                    + lista(s.senao(), JsonEscritor::cmdJson) + "}";
        if (c instanceof ComandoAst.Enquanto e)
            return "{\"tipoComando\": \"ENQUANTO\", \"condicao\": " + exprJson(e.condicao()) + ", \"corpo\": "
                    + lista(e.corpo(), JsonEscritor::cmdJson) + "}";
        if (c instanceof ComandoAst.Para p)
            return "{\"tipoComando\": \"PARA\", \"variavel\": " + q(p.variavel()) + ", \"inicio\": "
                    + exprJson(p.inicio()) + ", \"fim\": " + exprJson(p.fim()) + ", \"passo\": "
                    + (p.passo() == null ? "null" : exprJson(p.passo())) + ", \"corpo\": "
                    + lista(p.corpo(), JsonEscritor::cmdJson) + "}";
        if (c instanceof ComandoAst.VetorizarPara v)
            return "{\"tipoComando\": \"VETORIZAR_PARA\", \"variavel\": " + q(v.variavel()) + ", \"fonte\": "
                    + listaStr(v.fonte()) + ", \"passoSimd\": " + (v.passoSimd() == null ? "null" : v.passoSimd())
                    + "}";
        if (c instanceof ComandoAst.BlocoMemoria b)
            return "{\"tipoComando\": \"BLOCO_MEMORIA\", \"nome\": " + q(b.nome()) + ", \"corpo\": "
                    + lista(b.corpo(), JsonEscritor::cmdJson) + "}";
        if (c instanceof ComandoAst.Exiba e)
            return "{\"tipoComando\": \"EXIBA\", \"expressao\": " + exprJson(e.expressao()) + "}";
        if (c instanceof ComandoAst.Ler l)
            return "{\"tipoComando\": \"LER\", \"alvo\": " + listaStr(l.alvo()) + "}";
        if (c instanceof ComandoAst.Chamada ch)
            return "{\"tipoComando\": \"CHAMADA\", \"expressao\": " + exprJson(ch.expressao()) + "}";
        if (c instanceof ComandoAst.Retorne r)
            return "{\"tipoComando\": \"RETORNE\", \"expressao\": "
                    + (r.expressao() == null ? "null" : exprJson(r.expressao())) + "}";
        if (c instanceof ComandoAst.FalharCom f)
            return "{\"tipoComando\": \"FALHAR_COM\", \"expressao\": " + exprJson(f.expressao()) + "}";
        return "\"?\"";
    }

    private static String exprJson(ExprAst e) {
        if (e instanceof ExprAst.LiteralInteiro li)
            return "{\"tipo\": \"LITERAL_INTEIRO\", \"valor\": \"" + li.valor().toString() + "\", \"linha\": "
                    + li.linha() + ", \"coluna\": " + li.coluna() + "}";
        if (e instanceof ExprAst.LiteralDecimal ld)
            return "{\"tipo\": \"LITERAL_DECIMAL\", \"escalado\": \"" + ld.escalado().toString() + "\", \"escala\": "
                    + ld.escala() + ", \"linha\": " + ld.linha() + ", \"coluna\": " + ld.coluna() + "}";
        if (e instanceof ExprAst.LiteralTexto lt)
            return "{\"tipo\": \"LITERAL_TEXTO\", \"valor\": " + q(lt.valor()) + ", \"linha\": " + lt.linha()
                    + ", \"coluna\": " + lt.coluna() + "}";
        if (e instanceof ExprAst.LiteralLogico ll)
            return "{\"tipo\": \"LITERAL_LOGICO\", \"valor\": " + ll.valor() + ", \"linha\": " + ll.linha()
                    + ", \"coluna\": " + ll.coluna() + "}";
        if (e instanceof ExprAst.Nulo n)
            return "{\"tipo\": \"NULO\", \"linha\": " + n.linha() + ", \"coluna\": " + n.coluna() + "}";
        if (e instanceof ExprAst.AcessoCampo a)
            return "{\"tipo\": \"ACESSO\", \"caminho\": " + listaStr(a.caminho()) + ", \"linha\": " + a.linha()
                    + ", \"coluna\": " + a.coluna() + "}";
        if (e instanceof ExprAst.Chamada c)
            return "{\"tipo\": \"CHAMADA\", \"caminho\": " + listaStr(c.caminho()) + ", \"argumentos\": "
                    + lista(c.argumentos(), JsonEscritor::exprJson) + ", \"linha\": " + c.linha() + ", \"coluna\": "
                    + c.coluna() + "}";
        if (e instanceof ExprAst.Indexacao i)
            return "{\"tipo\": \"INDEXACAO\", \"alvo\": " + exprJson(i.alvo()) + ", \"indice\": " + exprJson(i.indice())
                    + ", \"linha\": " + i.linha() + ", \"coluna\": " + i.coluna() + "}";
        if (e instanceof ExprAst.FatiaLiteral f)
            return "{\"tipo\": \"FATIA_LITERAL\", \"elementos\": " + lista(f.elementos(), JsonEscritor::exprJson)
                    + ", \"linha\": " + f.linha() + ", \"coluna\": " + f.coluna() + "}";
        if (e instanceof ExprAst.CriarRegistro cr)
            return "{\"tipo\": \"CRIAR_REGISTRO\", \"nomeEstrutura\": " + q(cr.nomeEstrutura()) + ", \"campos\": "
                    + lista(cr.campos(),
                            cv -> "{\"nome\": " + q(cv.nome()) + ", \"valor\": " + exprJson(cv.valor()) + "}")
                    + ", \"linha\": " + cr.linha() + ", \"coluna\": " + cr.coluna() + "}";
        if (e instanceof ExprAst.OpBinaria o)
            return "{\"tipo\": \"OP_BINARIA\", \"operador\": " + q(o.operador()) + ", \"esquerda\": "
                    + exprJson(o.esquerda()) + ", \"direita\": " + exprJson(o.direita()) + ", \"linha\": " + o.linha()
                    + ", \"coluna\": " + o.coluna() + "}";
        if (e instanceof ExprAst.OpUnaria o)
            return "{\"tipo\": \"OP_UNARIA\", \"operador\": " + q(o.operador()) + ", \"operando\": "
                    + exprJson(o.operando()) + ", \"linha\": " + o.linha() + ", \"coluna\": " + o.coluna() + "}";
        return "null";
    }

    private static String q(String s) {
        if (s == null)
            return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static <T> String lista(List<T> l, java.util.function.Function<T, String> fn) {
        if (l == null)
            return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(fn.apply(l.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String listaStr(List<String> l) {
        if (l == null)
            return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(q(l.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}
