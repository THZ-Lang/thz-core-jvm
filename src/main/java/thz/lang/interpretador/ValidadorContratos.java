package thz.lang.interpretador;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import thz.lang.ast.ClausulaContratoAst;
import thz.lang.ast.ComandoAst;
import thz.lang.ast.ExprAst;
import thz.lang.ast.InvarianteAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.EstruturaAst;

/**
 * Validação de contratos formais (EXIGE/GARANTE) e invariantes de estruturas.
 * Extraído de InterpretadorThz para responsabilidade única.
 */
public class ValidadorContratos {

    private final ProgramaAst ast;
    private final BiFunction<ExprAst, Escopo, ValorThz> avaliador;

    public ValidadorContratos(ProgramaAst ast, BiFunction<ExprAst, Escopo, ValorThz> avaliador) {
        this.ast = ast;
        this.avaliador = avaliador;
    }

    public void validarContratos(List<ClausulaContratoAst> clausulas, Escopo escopo, String natureza) {
        if (clausulas == null)
            return;
        for (ClausulaContratoAst clausula : clausulas) {
            if (!avaliarClausulaUniversal(clausula.expressao(), escopo)) {
                throw new ErroContrato("[Violação de Contrato " + natureza + "][Linha " + clausula.linha() + ":"
                        + clausula.coluna() + "] Cláusula reprovada: " + clausula.textoCanonico());
            }
        }
    }

    public void validarInvariantes(ValorThz valor, ComandoAst cmd) {
        if (!(valor instanceof ValorThz.Registro reg))
            return;
        EstruturaAst estrutura = ast.estruturas().stream().filter(e -> e.nome().equals(reg.nomeEstrutura())).findFirst()
                .orElse(null);
        if (estrutura == null || estrutura.invariantes().isEmpty())
            return;
        Escopo escopoInv = new Escopo(null);
        for (var entry : reg.campos().entrySet()) {
            escopoInv.definir(entry.getKey(), entry.getValue());
        }
        for (InvarianteAst inv : estrutura.invariantes()) {
            ValorThz cond = avaliador.apply(inv.expressao(), escopoInv);
            if (!(cond instanceof ValorThz.Logico l) || !l.valor()) {
                String posicao;
                if (cmd != null)
                    posicao = "[Linha " + cmd.linha() + ":" + cmd.coluna() + "] ";
                else
                    posicao = "[Linha " + inv.linha() + ":" + inv.coluna() + "] ";
                throw new ErroContrato("[Violação de Invariante]" + posicao + "Estrutura '" + reg.nomeEstrutura()
                        + "' reprovou: " + inv.textoCanonico());
            }
        }
    }

    public void validarInvariantes(ValorThz valor) {
        if (!(valor instanceof ValorThz.Registro reg))
            return;
        EstruturaAst estrutura = ast.estruturas().stream().filter(e -> e.nome().equals(reg.nomeEstrutura())).findFirst()
                .orElse(null);
        if (estrutura == null || estrutura.invariantes().isEmpty())
            return;
        Escopo escopoInv = new Escopo(null);
        for (var entry : reg.campos().entrySet()) {
            escopoInv.definir(entry.getKey(), entry.getValue());
        }
        for (InvarianteAst inv : estrutura.invariantes()) {
            ValorThz cond = avaliador.apply(inv.expressao(), escopoInv);
            if (!(cond instanceof ValorThz.Logico l) || !l.valor()) {
                String posicao = "[Linha " + inv.linha() + ":" + inv.coluna() + "] ";
                throw new ErroContrato("[Violação de Invariante]" + posicao + "Estrutura '" + reg.nomeEstrutura()
                        + "' reprovou: " + inv.textoCanonico());
            }
        }
    }

    private boolean avaliarClausulaUniversal(ExprAst expr, Escopo escopo) {
        Set<String> raizes = coletarRaizesDeFatias(expr, escopo);
        return quantificar(expr, escopo, new ArrayList<>(raizes), 0);
    }

    private Set<String> coletarRaizesDeFatias(ExprAst expr, Escopo escopo) {
        Set<String> raizes = new HashSet<>();
        visitarRaizes(expr, escopo, raizes);
        return raizes;
    }

    private void visitarRaizes(ExprAst e, Escopo escopo, Set<String> raizes) {
        switch (e) {
            case ExprAst.AcessoCampo ac -> {
                if (!ac.caminho().isEmpty()) {
                    ValorThz base = escopo.resolver(ac.caminho().get(0));
                    if (base instanceof ValorThz.Fatia)
                        raizes.add(ac.caminho().get(0));
                }
            }
            case ExprAst.Chamada ch -> {
                for (ExprAst arg : ch.argumentos())
                    visitarRaizes(arg, escopo, raizes);
            }
            case ExprAst.Indexacao idx -> {
                visitarRaizes(idx.alvo(), escopo, raizes);
                visitarRaizes(idx.indice(), escopo, raizes);
            }
            case ExprAst.FatiaLiteral fl -> {
                for (ExprAst el : fl.elementos())
                    visitarRaizes(el, escopo, raizes);
            }
            case ExprAst.CriarRegistro cr -> {
                for (ExprAst.CampoValor c : cr.campos())
                    visitarRaizes(c.valor(), escopo, raizes);
            }
            case ExprAst.OpBinaria ob -> {
                visitarRaizes(ob.esquerda(), escopo, raizes);
                visitarRaizes(ob.direita(), escopo, raizes);
            }
            case ExprAst.OpUnaria ou -> visitarRaizes(ou.operando(), escopo, raizes);
            case ExprAst.ConsultaTipada ct -> visitarRaizes(ct.fonte(), escopo, raizes);
            case ExprAst.LiteralInteiro _,ExprAst.LiteralDecimal _,ExprAst.LiteralTexto _,ExprAst.LiteralLogico _,ExprAst.Nulo _ ->
                {
                }
        }
    }

    private boolean quantificar(ExprAst expr, Escopo escopo, List<String> raizes, int indice) {
        if (indice >= raizes.size()) {
            return ValorThzUtils.exigirLogico(avaliador.apply(expr, escopo), "cláusula de contrato");
        }
        String nome = raizes.get(indice);
        ValorThz valor = escopo.resolver(nome);
        if (!(valor instanceof ValorThz.Fatia fatia)) {
            return quantificar(expr, escopo, raizes, indice + 1);
        }
        for (ValorThz elemento : fatia.elementos()) {
            Escopo sub = new Escopo(escopo);
            sub.definir(nome, elemento);
            if (!quantificar(expr, sub, raizes, indice + 1)) {
                return false;
            }
        }
        return true;
    }
}
