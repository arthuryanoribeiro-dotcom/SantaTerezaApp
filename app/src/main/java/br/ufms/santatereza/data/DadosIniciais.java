package br.ufms.santatereza.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo inicial de matrizes.
 *
 * ATENÇÃO: o site oficial (fazendastatereza.com.br/matrizes) ainda não publica os dados
 * individuais dos animais. Os registros abaixo são ILUSTRATIVOS, criados apenas para
 * demonstrar o funcionamento do aplicativo, e o app exibe esse aviso ao usuário.
 * Para usar dados reais basta substituir esta lista.
 */
public final class DadosIniciais {

    private DadosIniciais() { }

    public static List<Matriz> matrizes() {
        List<Matriz> lista = new ArrayList<>();
        lista.add(new Matriz("ST Aurora", "STZ-1021", "2021-03-14", 520,
                "ST Imperador (FIV)", "ST Lua Nova", "FIV", 21.4, 12,
                "Matriz de excelente habilidade materna, com bezerros desmamados acima da média do rebanho. "
                        + "Temperamento dócil e ótima conformação de úbere."));
        lista.add(new Matriz("ST Bonança", "STZ-1034", "2020-11-02", 548,
                "ST Horizonte (FIV)", "ST Serena", "FIV", 18.9, 18,
                "Destaque em precocidade sexual: primeira cria aos 26 meses. Boa profundidade de costelas "
                        + "e aprumos corretos."));
        lista.add(new Matriz("ST Cigana", "STZ-1047", "2021-07-21", 497,
                "ST Valente (IA)", "ST Morena", "IA", 16.2, 23,
                "Vaca moderada, eficiente em pasto, com ótimo escore de mansidão. Filha de inseminação "
                        + "artificial com touro provado."));
        lista.add(new Matriz("ST Dália", "STZ-1058", "2019-09-09", 575,
                "ST Imperador (FIV)", "ST Pérola", "FIV", 24.8, 8,
                "Uma das doadoras de embriões do programa de FIV da fazenda. Alta fertilidade e "
                        + "progênie uniforme."));
        lista.add(new Matriz("ST Estrela", "STZ-1062", "2022-01-30", 468,
                "ST Guardião (TE)", "ST Aurora", "TE", 19.7, 15,
                "Novilha de futuro, filha da ST Aurora. Ótimo ganho de peso pós-desmama e "
                        + "caracterização racial marcante."));
        lista.add(new Matriz("ST Fortuna", "STZ-1075", "2020-05-17", 556,
                "ST Horizonte (FIV)", "ST Jóia", "FIV", 22.1, 11,
                "Matriz de grande estrutura, com excelente área de olho de lombo nas avaliações de "
                        + "carcaça. Muito mansa no manejo."));
        lista.add(new Matriz("ST Graça", "STZ-1083", "2021-10-05", 505,
                "ST Valente (IA)", "ST Serena", "IA", 17.5, 21,
                "Boa habilidade materna e longevidade esperada. Indicada para programas que buscam "
                        + "equilíbrio entre tamanho e eficiência."));
        lista.add(new Matriz("ST Harmonia", "STZ-1096", "2019-12-12", 588,
                "ST Guardião (TE)", "ST Dália", "TE", 25.6, 6,
                "Top 10% do rebanho no índice genético. Doadora ativa, com prenhezes confirmadas "
                        + "a cada ciclo de aspiração."));
        return lista;
    }
}
