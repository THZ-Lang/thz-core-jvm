package thz.lang.semantico;

public record TipoThz(String nome, CategoriaTipo categoria, Integer escala, String moeda, String interno, String internoErro) {
    public TipoThz(String nome, CategoriaTipo categoria) { this(nome, categoria, null, null, null, null); }
}
