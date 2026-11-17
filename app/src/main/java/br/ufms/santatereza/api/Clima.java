package br.ufms.santatereza.api;

/** Dados de clima já processados para exibição. */
public class Clima {
    public double temperatura;
    public int umidade;
    public double vento;
    public int codigo;
    public long obtidoEm;

    /** Converte o código WMO devolvido pela API Open-Meteo em texto em português. */
    public String descricao() {
        if (codigo == 0) return "Céu limpo";
        if (codigo <= 2) return "Parcialmente nublado";
        if (codigo == 3) return "Nublado";
        if (codigo == 45 || codigo == 48) return "Neblina";
        if (codigo >= 51 && codigo <= 57) return "Garoa";
        if (codigo >= 61 && codigo <= 67) return "Chuva";
        if (codigo >= 71 && codigo <= 77) return "Neve";
        if (codigo >= 80 && codigo <= 82) return "Pancadas de chuva";
        if (codigo >= 95) return "Tempestade";
        return "Tempo instável";
    }
}
