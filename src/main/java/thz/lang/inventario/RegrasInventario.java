package thz.lang.inventario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Regras puras da aplicação de referência, sem relógio ou persistência implícitos. */
public final class RegrasInventario {
    public static final String IDENTIDADE = "DadosObrigatoriosInventario";
    public record Revisao(int numero, LocalDate desde, LocalDate ateExclusivo) {
        public Revisao {
            if (numero < 1 || desde == null || (ateExclusivo != null && !ateExclusivo.isAfter(desde))) {
                throw new IllegalArgumentException("Revisão ou intervalo de vigência inválido.");
            }
        }
        boolean aplicavel(LocalDate data) {
            return !data.isBefore(desde) && (ateExclusivo == null || data.isBefore(ateExclusivo));
        }
    }
    private final List<Revisao> revisoes;

    public RegrasInventario() {
        this(List.of(new Revisao(1, LocalDate.of(2026, 1, 1), null)));
    }

    public RegrasInventario(List<Revisao> revisoes) {
        this.revisoes = List.copyOf(revisoes);
    }

    public Revisao selecionar(LocalDate data) {
        if (data == null) throw new IllegalArgumentException("Referência temporal obrigatória.");
        var aplicaveis = revisoes.stream().filter(r -> r.aplicavel(data)).toList();
        if (aplicaveis.size() != 1) {
            throw new IllegalArgumentException(aplicaveis.isEmpty()
                    ? "Nenhuma revisão vigente para " + data + "."
                    : "Vigência ambígua para " + data + ".");
        }
        return aplicaveis.getFirst();
    }

    public List<String> validar(String codigo, String descricao, String localizacao) {
        var erros = new ArrayList<String>();
        obrigatorio(erros, "codigo", codigo);
        obrigatorio(erros, "descricao", descricao);
        obrigatorio(erros, "localizacao", localizacao);
        return List.copyOf(erros);
    }

    private static void obrigatorio(List<String> erros, String campo, String valor) {
        if (valor == null || valor.isBlank()) erros.add("AUSENTE: " + campo + " obrigatório.");
    }
}
