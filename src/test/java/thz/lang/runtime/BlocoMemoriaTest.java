package thz.lang.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlocoMemoriaTest {

    @Test
    void deveRejeitarSomaQueTransbordariaSemCorromperUtilizacao() {
        var bloco = new BlocoMemoria(1);
        bloco.alocar(1);
        assertThrows(RuntimeException.class, () -> bloco.alocar(Integer.MAX_VALUE));
        assertEquals(1, bloco.getUtilizacaoBytes());
    }

    @Test
    public void testCriacaoEAlocacaoBasica() {
        BlocoMemoria bloco = new BlocoMemoria(1); // 1 MB
        assertEquals(1, bloco.getTamanhoMb());
        assertEquals(1024 * 1024, bloco.getCapacidadeBytes());
        assertEquals(0, bloco.getUtilizacaoBytes());
        assertTrue(bloco.estaVazio());
        assertEquals(1024 * 1024, bloco.getEspacoLivreBytes());
        assertEquals(0.0, bloco.getPorcentagemUso());

        int offset1 = bloco.alocar(512);
        assertEquals(0, offset1);
        assertEquals(512, bloco.getUtilizacaoBytes());
        assertFalse(bloco.estaVazio());
        assertEquals((1024 * 1024) - 512, bloco.getEspacoLivreBytes());

        int offset2 = bloco.alocar(1024);
        assertEquals(512, offset2);
        assertEquals(1536, bloco.getUtilizacaoBytes());

        // Limpeza instantânea de todo o bloco
        bloco.liberarTudo();
        assertEquals(0, bloco.getUtilizacaoBytes());
        assertTrue(bloco.estaVazio());
        assertEquals(1024 * 1024, bloco.getEspacoLivreBytes());
    }

    @Test
    public void testLimiteDeCapacidadeExcedido() {
        BlocoMemoria bloco = new BlocoMemoria(1); // 1 MB = 1048576 B
        bloco.alocar(1048570);

        Exception ex = assertThrows(RuntimeException.class, () -> bloco.alocar(10));
        assertTrue(ex.getMessage().contains("Limite do bloco de memória temporária excedido"));
    }

    @Test
    public void testParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new BlocoMemoria(-1));
        BlocoMemoria bloco = new BlocoMemoria(1);
        assertThrows(IllegalArgumentException.class, () -> bloco.alocar(-50));
    }

    @Test
    public void testToStringLegivel() {
        BlocoMemoria bloco = new BlocoMemoria(2);
        bloco.alocar(1024);
        String s = bloco.toString();
        assertNotNull(s);
        assertTrue(s.contains("BlocoMemoria"));
    }

    @Test
    public void testAlocacaoZeroBytesEExataCapacidade() {
        BlocoMemoria bloco = new BlocoMemoria(1); // 1048576 bytes
        int pos0 = bloco.alocar(0);
        assertEquals(0, pos0);
        assertEquals(0, bloco.getUtilizacaoBytes());

        int posExata = bloco.alocar(1048576);
        assertEquals(0, posExata);
        assertEquals(1048576, bloco.getUtilizacaoBytes());
        assertEquals(0, bloco.getEspacoLivreBytes());
        assertEquals(100.0, bloco.getPorcentagemUso());

        assertThrows(RuntimeException.class, () -> bloco.alocar(1));
    }

    @Test
    public void testOverflowIntegerMaxSemCorromperEstadoVazio() {
        BlocoMemoria bloco = new BlocoMemoria(1);
        assertThrows(RuntimeException.class, () -> bloco.alocar(Integer.MAX_VALUE));
        assertEquals(0, bloco.getUtilizacaoBytes());
        assertTrue(bloco.estaVazio());
    }
}
