package br.jao.screenmatch.principal;

import br.jao.screenmatch.model.DadosEpisodio;
import br.jao.screenmatch.model.DadosSerie;
import br.jao.screenmatch.model.DadosTemporada;
import br.jao.screenmatch.model.Episodio;
import br.jao.screenmatch.service.ConsumoApi;
import br.jao.screenmatch.service.ConverteDados;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();

    //"https://www.omdbapi.com/?t=gilmore+girls&apikey=6585022c"

    public void exibeMenu(){
        System.out.println("Digite o nome da série para a busca");
        var nomeSerie = leitura.nextLine();
        var nomeSerieReplaced = ENDERECO + nomeSerie.replace(" ", "+") + API_KEY;
        var json = consumo.obterDados(nomeSerieReplaced);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dados);

        List<DadosTemporada> temporadas = new ArrayList<>();

        for(int i = 1; i<=dados.totalTemporadas(); i++) {
            json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);
            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
        }

        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream())
                .collect(Collectors.toList());

        List<Episodio> episodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream()
                        .map(d -> new Episodio(t.numero(), d)))
                .collect(Collectors.toList());

        System.out.println("Qual função quer utilizar? \n" +
                "1 - Top 10 episodios\n" +
                "2 - Ver todos os episodios\n" +
                "3 - Ver a partir de determinado ano\n" +
                "4 - Pesquisar episodio\n" +
                "5 - Estatisticas da série");
        String op = leitura.nextLine();

        if (op.equals("1")) {
            System.out.println("\nTop 10 episódios");
            dadosEpisodios.stream()
                .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                .limit(10)
                .map(e -> e.titulo().toUpperCase())
                .forEach(System.out::println);
        }
        else if (op.equals("2")) {
            System.out.println("\nTodos os Episodios");
            episodios.forEach(System.out::println);
        }
        else if (op.equals("3")) {
            System.out.println("A partir de que ano? ");
            var ano = leitura.nextInt();
            leitura.nextLine();
            LocalDate dataBusca = LocalDate.of(ano,1,1);
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            episodios.stream()
                    .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca))
                    .forEach(e -> System.out.println(
                            "Temporada: " + e.getTemporada() +
                                    "    Episódio: " + e.getTitulo() +
                                    "    Data lançamento: " + e.getDataLancamento().format(formatador)
                    ));
        }
        else if (op.equals("4")) {
            System.out.println("Digite um trecho do titulo do episodio");
            var trechoTitulo = leitura.nextLine();
            Optional<Episodio> episodioBuscado = episodios.stream()
                .filter(e -> e.getTitulo().contains(trechoTitulo))
                .findFirst();
            if(episodioBuscado.isPresent()){
                System.out.println(episodioBuscado.get());
            } else {
                System.out.println("Episodio não encontrado");
            }
        }
        else if (op.equals("5")) {
            Map<Integer, Double> avaliacoesTemporada = episodios.stream()
                    .filter(e -> e.getAvaliacao() > 0.0)
                    .collect(Collectors.groupingBy(Episodio::getTemporada, Collectors.averagingDouble(Episodio::getAvaliacao)));
            System.out.println(avaliacoesTemporada);

            DoubleSummaryStatistics est = episodios.stream()
                    .filter(e -> e.getAvaliacao() > 0.0)
                    .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));
            System.out.println("Média: " + est.getAverage());
            System.out.println("Melhor episódio: " + est.getMax());
            System.out.println("Pior episódio: " + est.getMin());
            System.out.println("Quantidade: " + est.getCount());
        }
        else {
            System.out.println("Comando não encontrado");
        }
    }
}