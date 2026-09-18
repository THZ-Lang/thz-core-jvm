package thz.lang.ast;

import thz.lang.lexico.DialetoLinguagem;
import java.util.List;

public record ProgramaAst(
        TipoModulo tipoModulo,
        String nome,
        String versaoLinguagem,
        List<ImportacaoAst> importacoes,
        MetadadosArquiteturaAst metadados,
        List<EstruturaAst> estruturas,
        List<EnumeracaoAst> enumeracoes,
        List<RegraNegocioAst> regras,
        List<ProcedimentoAst> procedimentos,
        DialetoLinguagem dialeto,
        List<String> blocosRust,
        List<FuncaoAst> funcoes) {

    public ProgramaAst(
            TipoModulo tipoModulo,
            String nome,
            String versaoLinguagem,
            List<ImportacaoAst> importacoes,
            MetadadosArquiteturaAst metadados,
            List<EstruturaAst> estruturas,
            List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras,
            List<ProcedimentoAst> procedimentos,
            DialetoLinguagem dialeto) {
        this(tipoModulo, nome, versaoLinguagem, importacoes, metadados, estruturas, enumeracoes, regras, procedimentos, dialeto, List.of(), List.of());
    }

    public ProgramaAst(
            TipoModulo tipoModulo,
            String nome,
            String versaoLinguagem,
            List<ImportacaoAst> importacoes,
            MetadadosArquiteturaAst metadados,
            List<EstruturaAst> estruturas,
            List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras,
            List<ProcedimentoAst> procedimentos) {
        this(tipoModulo, nome, versaoLinguagem, importacoes, metadados, estruturas, enumeracoes, regras, procedimentos, DialetoLinguagem.PT_BR, List.of(), List.of());
    }

    public ProgramaAst(TipoModulo tipoModulo, String nome, String versaoLinguagem, List<ImportacaoAst> importacoes,
            MetadadosArquiteturaAst metadados, List<EstruturaAst> estruturas, List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras, List<ProcedimentoAst> procedimentos, DialetoLinguagem dialeto,
            List<String> blocosRust) {
        this(tipoModulo, nome, versaoLinguagem, importacoes, metadados, estruturas, enumeracoes, regras,
                procedimentos, dialeto, blocosRust, List.of());
    }

    public ProgramaAst(
            TipoModulo tipoModulo,
            String nome,
            String versaoLinguagem,
            MetadadosArquiteturaAst metadados,
            List<EstruturaAst> estruturas,
            List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras,
            List<ProcedimentoAst> procedimentos) {
        this(tipoModulo, nome, versaoLinguagem, List.of(), metadados, estruturas, enumeracoes, regras, procedimentos, DialetoLinguagem.PT_BR);
    }

    public ProgramaAst(
            String nome,
            String versaoLinguagem,
            MetadadosArquiteturaAst metadados,
            List<EstruturaAst> estruturas,
            List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras,
            List<ProcedimentoAst> procedimentos) {
        this(TipoModulo.PROGRAMA, nome, versaoLinguagem, List.of(), metadados, estruturas, enumeracoes, regras, procedimentos, DialetoLinguagem.PT_BR);
    }
}
