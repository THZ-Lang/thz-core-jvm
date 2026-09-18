package thz.lang.ast;
import java.util.List;
public record MetadadosArquiteturaAst(String dominio, String subdominio, String camada, String versao, String autor, String sloLatencia, List<String> conformidade) {}
