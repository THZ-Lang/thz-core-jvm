package thz.lang.js;

import thz.lang.ast.*;
import thz.lang.sintatico.ThzParser;

/**
 * ThzJsEmitter — Emissor/Transpilador de programas THZ para JavaScript moderno ES2023.
 */
public final class ThzJsEmitter {

    private ThzJsEmitter() {}

    public static String emitir(ProgramaAst ast) {
        StringBuilder sb = new StringBuilder();
        sb.append("// ============================================================\n");
        sb.append("// THZ-LANG JavaScript ES2023 Target\n");
        sb.append("// Módulo: ").append(ast.nome()).append(" | Tipo: ").append(ast.tipoModulo() != null ? ast.tipoModulo().descricao() : "PROGRAMA").append("\n");
        sb.append("// ============================================================\n\n");

        // Runtime DecimalFixo embutido em JS (BigInt com precisão exata ISO/IEC 10967)
        sb.append("""
                class DecimalFixo {
                  constructor(bruto, escala = 2) {
                    this.bruto = typeof bruto === 'bigint' ? bruto : BigInt(bruto);
                    this.escala = escala;
                  }
                  static deTexto(texto, escala = 2) {
                    const limpo = String(texto).replace(/_/g, '');
                    const partes = limpo.split('.');
                    let inteira = partes[0] || '0';
                    let frac = partes[1] || '';
                    if (frac.length > escala) frac = frac.slice(0, escala);
                    while (frac.length < escala) frac += '0';
                    const sinal = inteira.startsWith('-') ? -1n : 1n;
                    if (inteira.startsWith('-')) inteira = inteira.slice(1);
                    return new DecimalFixo(sinal * (BigInt(inteira) * (10n ** BigInt(escala)) + BigInt(frac)), escala);
                  }
                  somar(outro) { return new DecimalFixo(this.bruto + outro.bruto, this.escala); }
                  subtrair(outro) { return new DecimalFixo(this.bruto - outro.bruto, this.escala); }
                  multiplicar(outro) { return new DecimalFixo((this.bruto * outro.bruto) / (10n ** BigInt(this.escala)), this.escala); }
                  dividir(outro) { return new DecimalFixo((this.bruto * (10n ** BigInt(this.escala))) / outro.bruto, this.escala); }
                  formatar() {
                    const divisor = 10n ** BigInt(this.escala);
                    const inteira = this.bruto / divisor;
                    let frac = (this.bruto % divisor).toString().replace('-', '');
                    while (frac.length < this.escala) frac = '0' + frac;
                    return `${inteira}.${frac}`;
                  }
                }

                function THZ_EXIBA(val) {
                  if (val && typeof val.formatar === 'function') console.log(val.formatar());
                  else console.log(val);
                }

                """);

        // Enumerações
        if (ast.enumeracoes() != null) {
            for (EnumeracaoAst en : ast.enumeracoes()) {
                sb.append("const ").append(en.nome()).append(" = Object.freeze({\n");
                for (String m : en.membros()) {
                    sb.append("  ").append(m).append(": '").append(m).append("',\n");
                }
                sb.append("});\n\n");
            }
        }

        // Estruturas
        if (ast.estruturas() != null) {
            for (EstruturaAst est : ast.estruturas()) {
                sb.append("class ").append(est.nome()).append(" {\n");
                sb.append("  constructor(init = {}) {\n");
                for (CampoEstruturaAst campo : est.campos()) {
                    sb.append("    this.").append(campo.nome()).append(" = init.").append(campo.nome()).append(" ?? null;\n");
                }
                sb.append("  }\n");
                sb.append("}\n\n");
            }
        }

        // Regras de negócio
        if (ast.regras() != null) {
            for (RegraNegocioAst regra : ast.regras()) {
                sb.append("class ").append(regra.nome()).append(" {\n");
                for (OperacaoAst op : regra.operacoes()) {
                    sb.append("  static ").append(op.nome()).append("(");
                    for (int i = 0; i < op.parametros().size(); i++) {
                        sb.append(op.parametros().get(i).nome());
                        if (i + 1 < op.parametros().size()) sb.append(", ");
                    }
                    sb.append(") {\n");
                    for (ComandoAst c : op.corpo()) {
                        sb.append(emitirComando(c, 4));
                    }
                    sb.append("  }\n\n");
                }
                sb.append("}\n\n");
            }
        }

        // Procedimentos
        if (ast.procedimentos() != null) {
            for (ProcedimentoAst proc : ast.procedimentos()) {
                sb.append("function ").append(proc.nome()).append("(");
                for (int i = 0; i < proc.parametros().size(); i++) {
                    sb.append(proc.parametros().get(i).nome());
                    if (i + 1 < proc.parametros().size()) sb.append(", ");
                }
                sb.append(") {\n");
                for (ComandoAst c : proc.corpo()) {
                    sb.append(emitirComando(c, 2));
                }
                sb.append("}\n\n");
            }
        }

        if (ast.funcoes() != null) {
            for (FuncaoAst funcao : ast.funcoes()) {
                sb.append("function ").append(funcao.nome()).append("(");
                for (int i = 0; i < funcao.parametros().size(); i++) {
                    sb.append(funcao.parametros().get(i).nome());
                    if (i + 1 < funcao.parametros().size()) sb.append(", ");
                }
                sb.append(") {\n");
                for (ComandoAst c : funcao.corpo()) sb.append(emitirComando(c, 2));
                sb.append("}\n\n");
            }
        }

        return sb.toString();
    }

    private static String emitirComando(ComandoAst cmd, int indent) {
        String pad = " ".repeat(indent);
        return switch (cmd) {
            case ComandoAst.DeclVariavel d -> pad + "let " + d.nome() + " = " + emitirExpr(d.inicializacao()) + ";\n";
            case ComandoAst.Atribuicao a -> pad + String.join(".", a.alvo()) + " = " + emitirExpr(a.expressao()) + ";\n";
            case ComandoAst.Exiba ex -> pad + "THZ_EXIBA(" + emitirExpr(ex.expressao()) + ");\n";
            case ComandoAst.Retorne r -> pad + "return " + (r.expressao() != null ? emitirExpr(r.expressao()) : "undefined") + ";\n";
            case ComandoAst.FalharCom fc -> pad + "throw new Error(" + emitirExpr(fc.expressao()) + ");\n";
            case ComandoAst.Se s -> {
                StringBuilder sb = new StringBuilder();
                sb.append(pad).append("if (").append(emitirExpr(s.condicao())).append(") {\n");
                for (ComandoAst c : s.entao()) sb.append(emitirComando(c, indent + 2));
                if (s.senao() != null && !s.senao().isEmpty()) {
                    sb.append(pad).append("} else {\n");
                    for (ComandoAst c : s.senao()) sb.append(emitirComando(c, indent + 2));
                }
                sb.append(pad).append("}\n");
                yield sb.toString();
            }
            case ComandoAst.Enquanto e -> {
                StringBuilder sb = new StringBuilder();
                sb.append(pad).append("while (").append(emitirExpr(e.condicao())).append(") {\n");
                for (ComandoAst c : e.corpo()) sb.append(emitirComando(c, indent + 2));
                sb.append(pad).append("}\n");
                yield sb.toString();
            }
            case ComandoAst.Para p -> {
                StringBuilder sb = new StringBuilder();
                String v = p.variavel();
                sb.append(pad).append("for (let ").append(v).append(" = ").append(emitirExpr(p.inicio()))
                        .append("; ").append(v).append(" <= ").append(emitirExpr(p.fim())).append("; ")
                        .append(v).append(" += ").append(p.passo() != null ? emitirExpr(p.passo()) : "1").append(") {\n");
                for (ComandoAst c : p.corpo()) sb.append(emitirComando(c, indent + 2));
                sb.append(pad).append("}\n");
                yield sb.toString();
            }
            case ComandoAst.VetorizarPara vp -> {
                StringBuilder sb = new StringBuilder();
                sb.append(pad).append("for (const ").append(vp.variavel()).append(" of ").append(String.join(".", vp.fonte())).append(") {\n");
                for (ComandoAst c : vp.corpo()) sb.append(emitirComando(c, indent + 2));
                sb.append(pad).append("}\n");
                yield sb.toString();
            }
            case ComandoAst.BlocoMemoria bm -> {
                StringBuilder sb = new StringBuilder();
                sb.append(pad).append("/* BLOCO_MEMORIA: ").append(bm.nome()).append(" */ {\n");
                for (ComandoAst c : bm.corpo()) sb.append(emitirComando(c, indent + 2));
                sb.append(pad).append(pad).append("}\n");
                yield sb.toString();
            }
            case ComandoAst.CasoResultado cr -> {
                StringBuilder sb = new StringBuilder();
                sb.append(pad).append("const _res_").append(Math.abs(cr.hashCode())).append(" = ").append(emitirExpr(cr.alvo())).append(";\n");
                if (cr.varSucesso() != null) {
                    sb.append(pad).append("if (_res_").append(Math.abs(cr.hashCode())).append(" && !_res_").append(Math.abs(cr.hashCode())).append(".erro) {\n");
                    sb.append(pad).append("  const ").append(cr.varSucesso()).append(" = _res_").append(Math.abs(cr.hashCode())).append(";\n");
                    for (ComandoAst c : cr.corpoSucesso()) sb.append(emitirComando(c, indent + 2));
                    sb.append(pad).append("}\n");
                }
                if (cr.varErro() != null) {
                    sb.append(pad).append("if (_res_").append(Math.abs(cr.hashCode())).append(" && _res_").append(Math.abs(cr.hashCode())).append(".erro) {\n");
                    sb.append(pad).append("  const ").append(cr.varErro()).append(" = _res_").append(Math.abs(cr.hashCode())).append(".erro;\n");
                    for (ComandoAst c : cr.corpoErro()) sb.append(emitirComando(c, indent + 2));
                    sb.append(pad).append("}\n");
                }
                yield sb.toString();
            }
            case ComandoAst.Tente t -> {
                StringBuilder sb = new StringBuilder();
                sb.append(pad).append("try {\n");
                for (ComandoAst c : t.corpoTente()) sb.append(emitirComando(c, indent + 2));
                sb.append(pad).append("} catch (_erro) {\n");
                for (ComandoAst c : t.corpoCapture()) sb.append(emitirComando(c, indent + 2));
                sb.append(pad).append("}\n");
                yield sb.toString();
            }
            case ComandoAst.Chamada ch -> pad + emitirExpr(ch.expressao()) + ";\n";
            case ComandoAst.Ler ler -> pad + String.join(".", ler.alvo()) + " = prompt();\n";
        };
    }

    private static String emitirExpr(ExprAst expr) {
        return switch (expr) {
            case ExprAst.LiteralInteiro li -> li.valor().toString();
            case ExprAst.LiteralDecimal ld -> "DecimalFixo.deTexto('" + ThzParser.textoCanonicoDe(ld) + "', " + ld.escala() + ")";
            case ExprAst.LiteralTexto lt -> "\"" + lt.valor().replace("\"", "\\\"") + "\"";
            case ExprAst.LiteralLogico ll -> ll.valor() ? "true" : "false";
            case ExprAst.Nulo _ -> "null";
            case ExprAst.AcessoCampo ac -> String.join(".", ac.caminho());
            case ExprAst.Chamada ch -> {
                StringBuilder sb = new StringBuilder(String.join(".", ch.caminho())).append("(");
                for (int i = 0; i < ch.argumentos().size(); i++) {
                    sb.append(emitirExpr(ch.argumentos().get(i)));
                    if (i + 1 < ch.argumentos().size()) sb.append(", ");
                }
                sb.append(")");
                yield sb.toString();
            }
            case ExprAst.CriarRegistro cr -> {
                StringBuilder sb = new StringBuilder("new ").append(cr.nomeEstrutura()).append("({");
                boolean prim = true;
                for (ExprAst.CampoValor cv : cr.campos()) {
                    if (!prim) sb.append(", ");
                    prim = false;
                    sb.append(cv.nome()).append(": ").append(emitirExpr(cv.valor()));
                }
                sb.append("})");
                yield sb.toString();
            }
            case ExprAst.FatiaLiteral fl -> {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < fl.elementos().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(emitirExpr(fl.elementos().get(i)));
                }
                sb.append("]");
                yield sb.toString();
            }
            case ExprAst.Indexacao idx -> emitirExpr(idx.alvo()) + "[" + emitirExpr(idx.indice()) + "]";
            case ExprAst.OpUnaria ou -> ("NAO".equals(ou.operador()) ? "!" : ou.operador()) + "(" + emitirExpr(ou.operando()) + ")";
            case ExprAst.OpBinaria ob -> {
                String op = switch (ob.operador()) {
                    case "=" -> "===";
                    case "<>" -> "!==";
                    case "E" -> "&&";
                    case "OU" -> "||";
                    default -> ob.operador();
                };
                yield "(" + emitirExpr(ob.esquerda()) + " " + op + " " + emitirExpr(ob.direita()) + ")";
            }
            case ExprAst.ConsultaTipada ct -> {
                StringBuilder sb = new StringBuilder("(");
                sb.append(emitirExpr(ct.fonte()));
                if (ct.onde() != null) {
                    sb.append(").filter(item => { with(item) { return ").append(emitirExpr(ct.onde())).append("; } })");
                } else {
                    sb.append(")");
                }
                if (ct.campoOrdenacao() != null) {
                    sb.append(".slice().sort((a, b) => ").append(ct.asc() ? "a." + ct.campoOrdenacao() + " > b." + ct.campoOrdenacao() + " ? 1 : -1" : "a." + ct.campoOrdenacao() + " < b." + ct.campoOrdenacao() + " ? 1 : -1").append(")");
                }
                if (ct.pular() != null || ct.limite() != null) {
                    String start = ct.pular() != null ? emitirExpr(ct.pular()) : "0";
                    String end = ct.limite() != null ? start + " + " + emitirExpr(ct.limite()) : "undefined";
                    sb.append(".slice(").append(start).append(", ").append(end).append(")");
                }
                yield sb.toString();
            }
        };
    }
}
