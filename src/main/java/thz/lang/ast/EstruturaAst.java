package thz.lang.ast;
import java.util.List;
public record EstruturaAst(String nome, boolean layoutColunar, List<CampoEstruturaAst> campos, List<InvarianteAst> invariantes) {}
