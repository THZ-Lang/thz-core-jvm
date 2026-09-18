package thz.lang.governanca.liberacao;

import thz.lang.ast.ProgramaAst;

import java.util.List;
import java.util.Map;

/**
 * Solicitação formal de liberação de binário para produção baseada em evidências.
 */
public record SolicitacaoLiberacao(
        String nomePrograma,
        String codigoFonte,
        ProgramaAst ast,
        List<CasoHomologacao> casosHomologacao,
        RegistroDadosReaisValidados dadosReais,
        String referenciaTemporal,
        Map<String, String> revisoesRegrasVigentes
) {}
