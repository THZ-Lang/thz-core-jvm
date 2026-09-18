package thz.lang.interpretador;

import thz.lang.ast.ExprAst;
import thz.lang.inventario.InventarioServico;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adaptador JVM da aplicação de referência; não introduz palavras-chave. */
public final class BibliotecaInventario {
    private BibliotecaInventario() {}

    public static void registrar(Map<String, BibliotecaPadrao.FuncaoStdlib> m) {
        for (String acao : List.of("importar", "validar", "atualizar", "consultar", "historico")) {
            String nome = "INVENTARIO." + acao;
            BibliotecaPadrao.registrarPublico(m, nome, (args, ctx, interp) -> {
                int aridade = switch (acao) { case "atualizar" -> 6; case "consultar" -> 1; default -> 3; };
                StdlibHelper.exigirAridade(nome, args, aridade, ctx);
                if (interp == null) throw new ErroExecucao("Inventário exige contexto autenticado do host.");
                try {
                    var servico = interp.inventario();
                    return switch (acao) {
                        case "importar", "validar" -> relatorio(servico.importar(Path.of(texto(args, 0, ctx)),
                                LocalDate.parse(texto(args, 1, ctx)), texto(args, 2, ctx), acao.equals("validar")));
                        case "atualizar" -> ocorrencia(servico.atualizar(texto(args, 0, ctx), inteiro(args, 1, ctx),
                                texto(args, 2, ctx), texto(args, 3, ctx), LocalDate.parse(texto(args, 4, ctx)), texto(args, 5, ctx)));
                        case "consultar" -> item(servico.consultar(texto(args, 0, ctx)));
                        case "historico" -> ValorThz.FATIA(servico.historico(texto(args, 0, ctx),
                                Math.toIntExact(inteiro(args, 1, ctx)), Math.toIntExact(inteiro(args, 2, ctx)))
                                .stream().map(BibliotecaInventario::historico).toList());
                        default -> throw new IllegalStateException("Operação desconhecida.");
                    };
                } catch (Exception e) {
                    throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage());
                }
            });
        }
    }

    private static String texto(List<ValorThz> args, int i, ExprAst ctx) {
        StdlibHelper.exigirClasse("INVENTARIO", args.get(i), "TEXTO", ctx);
        return ((ValorThz.Texto) args.get(i)).valor();
    }
    private static long inteiro(List<ValorThz> args, int i, ExprAst ctx) {
        return StdlibHelper.comoInteiroArg(args.get(i), ctx).longValueExact();
    }
    private static ValorThz registro(Object... pares) {
        var campos = new LinkedHashMap<String, ValorThz>();
        for (int i = 0; i < pares.length; i += 2) {
            Object v = pares[i + 1];
            ValorThz valor = v == null ? ValorThz.NULO : v instanceof ValorThz thz ? thz
                    : v instanceof Boolean b ? ValorThz.LOGICO(b)
                    : v instanceof Number n ? ValorThz.INTEIRO(n.longValue()) : ValorThz.TEXTO(v.toString());
            campos.put((String) pares[i], valor);
        }
        return new ValorThz.Registro("REGISTRO", campos);
    }
    private static ValorThz ocorrencia(InventarioServico.Ocorrencia o) {
        return registro("linha", o.linha(), "codigo", o.codigo(), "estado", o.estado(),
                "detalhes", ValorThz.FATIA(o.detalhes().stream().map(s -> (ValorThz) ValorThz.TEXTO(s)).toList()));
    }
    private static ValorThz relatorio(InventarioServico.Relatorio r) {
        return registro("somenteValidar", r.somenteValidar(), "origem", r.origem(), "regra", r.regra(),
                "revisao", r.revisao(), "referencia", r.referencia(),
                "ocorrencias", ValorThz.FATIA(r.ocorrencias().stream().map(BibliotecaInventario::ocorrencia).toList()));
    }
    private static ValorThz item(InventarioServico.Item i) {
        return i == null ? ValorThz.NULO : registro("codigo", i.codigo(), "descricao", i.descricao(),
                "localizacao", i.localizacao(), "versao", i.versao());
    }
    private static ValorThz historico(InventarioServico.Historico h) {
        return registro("codigo", h.codigo(), "campo", h.campo(), "anterior", h.anterior(), "novo", h.novo(),
                "versao", h.versao(), "autor", h.autor(), "justificativa", h.justificativa(), "instante", h.instante(),
                "origem", h.origem(), "regra", h.regra(), "revisao", h.revisao(), "referencia", h.referencia(),
                "operacao", h.operacao(), "arquivado", h.arquivado());
    }
}
