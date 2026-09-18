package thz.lang.dap;

import thz.lang.interpretador.Escopo;

/**
 * Interface de gancho (hook) para monitoramento e controle de execução do depurador.
 */
public interface ThzDebugListener {

    enum AcaoPasso { CONTINUAR, STEP_OVER, STEP_IN, STEP_OUT, PAUSAR }

    /**
     * Notificado antes da execução de um comando ou linha de código.
     * @param linha número da linha atual (1-indexada)
     * @param operacao nome da operação corrente
     * @param escopo escopo de variáveis visíveis
     * @return ação a ser tomada pelo interpretador
     */
    AcaoPasso aoExecutarLinha(int linha, String operacao, Escopo escopo);
}
