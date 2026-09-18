package thz.lang.otimizador;

import thz.lang.ast.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Passe de otimização estática na AST do THZ-LANG.
 * Realiza Constant Folding (dobra de constantes) e Dead Code Elimination (eliminação de código morto).
 */
public final class OtimizadorAst {

    public static ProgramaAst otimizar(ProgramaAst ast) {
        if (ast == null) return null;

        List<RegraNegocioAst> regrasOtimizadas = new ArrayList<>();
        if (ast.regras() != null) {
            for (RegraNegocioAst regra : ast.regras()) {
                regrasOtimizadas.add(otimizarRegra(regra));
            }
        }

        List<ProcedimentoAst> procedimentosOtimizados = new ArrayList<>();
        if (ast.procedimentos() != null) {
            for (ProcedimentoAst proc : ast.procedimentos()) {
                procedimentosOtimizados.add(otimizarProcedimento(proc));
            }
        }

        return new ProgramaAst(
                ast.tipoModulo(),
                ast.nome(),
                ast.versaoLinguagem(),
                ast.importacoes(),
                ast.metadados(),
                ast.estruturas(),
                ast.enumeracoes(),
                regrasOtimizadas,
                procedimentosOtimizados
        );
    }

    private static RegraNegocioAst otimizarRegra(RegraNegocioAst regra) {
        List<OperacaoAst> operacoesOtimizadas = new ArrayList<>();
        if (regra.operacoes() != null) {
            for (OperacaoAst op : regra.operacoes()) {
                List<ComandoAst> comandosOtimizados = otimizarComandos(op.corpo());
                operacoesOtimizadas.add(new OperacaoAst(
                        op.nome(),
                        op.parametros(),
                        op.tipoRetorno(),
                        comandosOtimizados,
                        op.idempotente(),
                        op.chaveIdempotencia()
                ));
            }
        }
        return new RegraNegocioAst(
                regra.nome(),
                regra.identificador(),
                regra.rastreioRequisito(),
                regra.descricao(),
                regra.clausulasEntrada(),
                regra.clausulasSaida(),
                operacoesOtimizadas,
                regra.idempotente(),
                regra.chaveIdempotencia()
        );
    }

    private static ProcedimentoAst otimizarProcedimento(ProcedimentoAst proc) {
        List<ComandoAst> comandosOtimizados = otimizarComandos(proc.corpo());
        return new ProcedimentoAst(
                proc.nome(),
                proc.parametros(),
                comandosOtimizados,
                proc.idempotente(),
                proc.chaveIdempotencia()
        );
    }

    public static List<ComandoAst> otimizarComandos(List<ComandoAst> comandos) {
        if (comandos == null) return List.of();
        List<ComandoAst> resultado = new ArrayList<>();

        for (ComandoAst cmd : comandos) {
            if (cmd instanceof ComandoAst.Se c) {
                ExprAst condOtimizada = otimizarExpressao(c.condicao());
                if (condOtimizada instanceof ExprAst.LiteralLogico b) {
                    if (b.valor()) {
                        resultado.addAll(otimizarComandos(c.entao()));
                    } else if (c.senao() != null) {
                        resultado.addAll(otimizarComandos(c.senao()));
                    }
                    continue;
                }
                resultado.add(new ComandoAst.Se(
                        condOtimizada,
                        otimizarComandos(c.entao()),
                        otimizarComandos(c.senao()),
                        c.linha(),
                        c.coluna()
                ));
            } else if (cmd instanceof ComandoAst.DeclVariavel d) {
                resultado.add(new ComandoAst.DeclVariavel(
                        d.nome(),
                        d.tipoDado(),
                        otimizarExpressao(d.inicializacao()),
                        d.linha(),
                        d.coluna()
                ));
            } else if (cmd instanceof ComandoAst.Atribuicao a) {
                resultado.add(new ComandoAst.Atribuicao(
                        a.alvo(),
                        otimizarExpressao(a.expressao()),
                        a.linha(),
                        a.coluna()
                ));
            } else if (cmd instanceof ComandoAst.Retorne r) {
                resultado.add(new ComandoAst.Retorne(
                        r.expressao() != null ? otimizarExpressao(r.expressao()) : null,
                        r.linha(),
                        r.coluna()
                ));
            } else if (cmd instanceof ComandoAst.Exiba e) {
                resultado.add(new ComandoAst.Exiba(
                        otimizarExpressao(e.expressao()),
                        e.linha(),
                        e.coluna()
                ));
            } else if (cmd instanceof ComandoAst.Enquanto e) {
                ExprAst condOtimizada = otimizarExpressao(e.condicao());
                if (condOtimizada instanceof ExprAst.LiteralLogico b && !b.valor()) {
                    continue;
                }
                resultado.add(new ComandoAst.Enquanto(
                        condOtimizada,
                        otimizarComandos(e.corpo()),
                        e.linha(),
                        e.coluna()
                ));
            } else if (cmd instanceof ComandoAst.Para p) {
                resultado.add(new ComandoAst.Para(
                        p.variavel(),
                        otimizarExpressao(p.inicio()),
                        otimizarExpressao(p.fim()),
                        otimizarExpressao(p.passo()),
                        otimizarComandos(p.corpo()),
                        p.linha(),
                        p.coluna()
                ));
            } else if (cmd instanceof ComandoAst.CasoResultado c) {
                resultado.add(new ComandoAst.CasoResultado(
                        otimizarExpressao(c.alvo()),
                        c.varSucesso(),
                        otimizarComandos(c.corpoSucesso()),
                        c.varErro(),
                        otimizarComandos(c.corpoErro()),
                        c.linha(),
                        c.coluna()
                ));
            } else {
                resultado.add(cmd);
            }
        }

        return resultado;
    }

    public static ExprAst otimizarExpressao(ExprAst expr) {
        if (expr == null) return null;

        if (expr instanceof ExprAst.OpBinaria b) {
            ExprAst esq = otimizarExpressao(b.esquerda());
            ExprAst dir = otimizarExpressao(b.direita());

            // Constant Folding de Aritmética Inteira
            if (esq instanceof ExprAst.LiteralInteiro e1 && dir instanceof ExprAst.LiteralInteiro d1) {
                BigInteger valEsq = e1.valor();
                BigInteger valDir = d1.valor();
                switch (b.operador()) {
                    case "+" -> { return new ExprAst.LiteralInteiro(valEsq.add(valDir), b.linha(), b.coluna()); }
                    case "-" -> { return new ExprAst.LiteralInteiro(valEsq.subtract(valDir), b.linha(), b.coluna()); }
                    case "*" -> { return new ExprAst.LiteralInteiro(valEsq.multiply(valDir), b.linha(), b.coluna()); }
                    case "/" -> {
                        if (!valDir.equals(BigInteger.ZERO)) return new ExprAst.LiteralInteiro(valEsq.divide(valDir), b.linha(), b.coluna());
                    }
                    case "==" -> { return new ExprAst.LiteralLogico(valEsq.equals(valDir), b.linha(), b.coluna()); }
                    case "!=" -> { return new ExprAst.LiteralLogico(!valEsq.equals(valDir), b.linha(), b.coluna()); }
                    case ">" -> { return new ExprAst.LiteralLogico(valEsq.compareTo(valDir) > 0, b.linha(), b.coluna()); }
                    case "<" -> { return new ExprAst.LiteralLogico(valEsq.compareTo(valDir) < 0, b.linha(), b.coluna()); }
                    case ">=" -> { return new ExprAst.LiteralLogico(valEsq.compareTo(valDir) >= 0, b.linha(), b.coluna()); }
                    case "<=" -> { return new ExprAst.LiteralLogico(valEsq.compareTo(valDir) <= 0, b.linha(), b.coluna()); }
                }
            }

            // Constant Folding de Lógica Booleana
            if (esq instanceof ExprAst.LiteralLogico e2 && dir instanceof ExprAst.LiteralLogico d2) {
                boolean valEsq = e2.valor();
                boolean valDir = d2.valor();
                switch (b.operador()) {
                    case "E" -> { return new ExprAst.LiteralLogico(valEsq && valDir, b.linha(), b.coluna()); }
                    case "OU" -> { return new ExprAst.LiteralLogico(valEsq || valDir, b.linha(), b.coluna()); }
                    case "==" -> { return new ExprAst.LiteralLogico(valEsq == valDir, b.linha(), b.coluna()); }
                    case "!=" -> { return new ExprAst.LiteralLogico(valEsq != valDir, b.linha(), b.coluna()); }
                }
            }

            return new ExprAst.OpBinaria(b.operador(), esq, dir, b.linha(), b.coluna());
        }

        if (expr instanceof ExprAst.OpUnaria u) {
            ExprAst operando = otimizarExpressao(u.operando());
            if (operando instanceof ExprAst.LiteralLogico b && u.operador().equalsIgnoreCase("NAO")) {
                return new ExprAst.LiteralLogico(!b.valor(), u.linha(), u.coluna());
            }
            if (operando instanceof ExprAst.LiteralInteiro i && u.operador().equals("-")) {
                return new ExprAst.LiteralInteiro(i.valor().negate(), u.linha(), u.coluna());
            }
            return new ExprAst.OpUnaria(u.operador(), operando, u.linha(), u.coluna());
        }

        return expr;
    }
}
